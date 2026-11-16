package fintech.servicios;

import fintech.estructuras.ColaPrioridad;
import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.TablaHash;
import fintech.modelo.Billetera;
import fintech.modelo.Frecuencia;
import fintech.modelo.OperacionProgramada;
import fintech.modelo.TipoNotificacion;
import fintech.modelo.TipoOperacionProgramada;
import fintech.modelo.TipoTransaccion;
import fintech.modelo.Transaccion;
import fintech.modelo.Usuario;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * Programación y ejecución automática de operaciones futuras.
 *
 * Estructura: ColaPrioridad (montículo binario) ordenada por
 *   1) fecha/hora de ejecución (la más próxima primero),
 *   2) prioridad del nivel del usuario (Platino antes que Bronce en empate),
 *   3) orden de creación.
 * Procesar las vencidas cuesta O(k log n) en lugar de recorrer todas.
 */
public class GestorProgramadas {

    public static final int HORAS_ALERTA_PREVIA = 24;
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static final Comparator<OperacionProgramada> COMPARADOR =
            Comparator.comparing(OperacionProgramada::getFechaEjecucion)
                    .thenComparing((OperacionProgramada o) -> -o.getPrioridadNivel())
                    .thenComparingLong(OperacionProgramada::getSecuencia);

    private final ColaPrioridad<OperacionProgramada> colaProgramadas = new ColaPrioridad<>(COMPARADOR);
    private final TablaHash<String, OperacionProgramada> tablaProgramadas = new TablaHash<>();
    private final ListaEnlazada<String> lstBitacora = new ListaEnlazada<>();

    private final GestorUsuarios usuarios;
    private final GestorBilleteras billeteras;
    private final MotorTransacciones motor;
    private final GestorNotificaciones notificaciones;
    private final GeneradorId ids;
    private final Reloj reloj;

    public GestorProgramadas(GestorUsuarios usuarios, GestorBilleteras billeteras, MotorTransacciones motor,
                             GestorNotificaciones notificaciones, GeneradorId ids, Reloj reloj) {
        this.usuarios = usuarios;
        this.billeteras = billeteras;
        this.motor = motor;
        this.notificaciones = notificaciones;
        this.ids = ids;
        this.reloj = reloj;
    }

    public OperacionProgramada programar(String idUsuario, TipoOperacionProgramada tipo, String codigoOrigen,
                                         String codigoDestino, double valor, LocalDateTime fecha,
                                         Frecuencia frecuencia, int repeticiones, String descripcion) {
        Usuario u = usuarios.obtener(idUsuario);
        if (tipo == null) throw new ValidacionException("Debe indicar el tipo de operación");
        if (frecuencia == null) throw new ValidacionException("Debe indicar la frecuencia");
        if (Double.isNaN(valor) || valor <= 0) throw new ValidacionException("El valor debe ser mayor que cero");
        if (fecha == null || !fecha.isAfter(reloj.ahora()))
            throw new ValidacionException("La fecha de ejecución debe ser futura (ahora: " + reloj.ahora().format(F) + ")");
        if (frecuencia != Frecuencia.UNICA && repeticiones == 0)
            throw new ValidacionException("Las repeticiones deben ser mayores que 0 (o -1 para indefinidas)");

        Billetera origen = null;
        if (tipo.requiereOrigen()) {
            origen = billeteras.obtener(codigoOrigen);
            if (origen.getUsuario() != u) throw new ValidacionException("La billetera origen no pertenece al usuario");
        }
        Billetera destino = billeteras.obtener(codigoDestino);
        if (origen == destino) throw new ValidacionException("Origen y destino no pueden ser la misma billetera");
        if ((tipo == TipoOperacionProgramada.AHORRO_AUTOMATICO || tipo == TipoOperacionProgramada.RECARGA_AUTOMATICA)
                && destino.getUsuario() != u)
            throw new ValidacionException(tipo + " debe tener como destino una billetera propia");
        if (valor > u.getNivel().getLimiteOperacion())
            throw new ValidacionException("El valor supera el límite por operación del nivel " + u.getNivel());

        OperacionProgramada op = new OperacionProgramada(ids.siguiente("PRG", 5), ids.siguienteNumero("SEQ-PRG"), u, tipo,
                origen, destino, valor, fecha, frecuencia, repeticiones, descripcion);
        op.fijarPrioridad();
        colaProgramadas.insertar(op);
        tablaProgramadas.poner(op.getId(), op);
        revisarProximas();
        return op;
    }

    public OperacionProgramada cancelar(String id) {
        OperacionProgramada op = tablaProgramadas.obtener(id);
        if (op == null || !op.isActiva()) throw new ValidacionException("No existe una operación programada activa con id " + id);
        op.cancelar();
        colaProgramadas.eliminarSi(o -> o == op);
        bitacora("Cancelada " + op.getId());
        return op;
    }

    /** Cancela todas las programadas de un usuario (al eliminarlo). */
    public int cancelarDeUsuario(Usuario u) {
        int c = 0;
        OperacionProgramada op;
        while ((op = colaProgramadas.eliminarSi(o -> o.getUsuario() == u)) != null) {
            op.cancelar();
            c++;
        }
        return c;
    }

    /** Envía alerta para las operaciones que se ejecutarán en las próximas 24 horas. */
    public int revisarProximas() {
        LocalDateTime limite = reloj.ahora().plusHours(HORAS_ALERTA_PREVIA);
        int c = 0;
        for (OperacionProgramada op : colaProgramadas) {
            if (op.isActiva() && !op.isAlertaPreviaEnviada() && !op.getFechaEjecucion().isAfter(limite)) {
                op.setAlertaPreviaEnviada(true);
                notificaciones.notificar(op.getUsuario(), TipoNotificacion.PROGRAMADA_PROXIMA,
                        op.getTipo() + String.format(" por $%,.2f", op.getValor()) + " se ejecutará el " + op.getFechaEjecucion().format(F));
                c++;
            }
        }
        return c;
    }

    /** Ejecuta todas las operaciones cuya fecha ya llegó, en orden de prioridad. */
    public int procesarPendientes() {
        int ejecutadas = 0;
        while (!colaProgramadas.estaVacia() && !colaProgramadas.verMinimo().getFechaEjecucion().isAfter(reloj.ahora())) {
            OperacionProgramada op = colaProgramadas.extraer();
            if (!op.isActiva()) continue;
            ejecutarUna(op);
            ejecutadas++;
            if (op.reprogramar()) {
                op.fijarPrioridad(); // el usuario pudo haber cambiado de nivel
                colaProgramadas.insertar(op);
            }
            else bitacora("Finalizada " + op.getId() + " (sin más repeticiones)");
        }
        return ejecutadas;
    }

    private void ejecutarUna(OperacionProgramada op) {
        Billetera o = op.getBilleteraOrigen(), d = op.getBilleteraDestino();
        TipoTransaccion tipo;
        if (op.getTipo() == TipoOperacionProgramada.RECARGA_AUTOMATICA) tipo = TipoTransaccion.RECARGA;
        else tipo = o.getUsuario() == d.getUsuario() ? TipoTransaccion.TRANSFERENCIA_PROPIA : TipoTransaccion.TRANSFERENCIA_EXTERNA;
        String desc = op.getTipo() + " " + op.getId() + (op.getDescripcion().isEmpty() ? "" : " - " + op.getDescripcion());
        Transaccion t;
        try {
            t = motor.ejecutar(tipo, o, d, op.getValor(), desc, true);
        } catch (ValidacionException ex) {
            op.registrarResultado(false);
            bitacora(reloj.ahora().format(F) + " " + op.getId() + " ERROR: " + ex.getMessage());
            notificaciones.notificar(op.getUsuario(), TipoNotificacion.OPERACION_RECHAZADA, op.getId() + ": " + ex.getMessage());
            return;
        }
        op.registrarResultado(t.esExitosa());
        bitacora(String.format("%s %s -> %s %s $%,.2f [%s]%s", reloj.ahora().format(F), op.getId(), t.getId(), op.getTipo(),
                op.getValor(), t.getEstado(), t.esExitosa() ? " +" + t.getPuntosGenerados() + " pts" : " " + t.getMotivoRechazo()));
        if (t.esExitosa()) {
            notificaciones.notificar(op.getUsuario(), TipoNotificacion.PROGRAMADA_EJECUTADA,
                    op.getTipo() + " " + op.getId() + String.format(" ejecutada por $%,.2f (+%d pts, incluye bono de %d)",
                            op.getValor(), t.getPuntosGenerados(), GestorRecompensas.BONO_PROGRAMADA));
        }
    }

    /**
     * Simula el paso del tiempo hasta la fecha indicada: va saltando de una
     * operación a la siguiente (la cima de la cola), emitiendo las alertas
     * previas y ejecutando cada operación en su fecha.
     */
    public int simularHasta(LocalDateTime fin) {
        if (fin.isBefore(reloj.ahora())) throw new ValidacionException("La fecha final debe ser posterior a la actual");
        int total = 0;
        while (!colaProgramadas.estaVacia() && !colaProgramadas.verMinimo().getFechaEjecucion().isAfter(fin)) {
            LocalDateTime proxima = colaProgramadas.verMinimo().getFechaEjecucion();
            LocalDateTime alerta = proxima.minusHours(HORAS_ALERTA_PREVIA);
            if (alerta.isAfter(reloj.ahora())) reloj.establecer(alerta);
            revisarProximas();
            if (proxima.isAfter(reloj.ahora())) reloj.establecer(proxima);
            total += procesarPendientes();
        }
        reloj.establecer(fin);
        revisarProximas();
        return total;
    }

    public int simularDias(int dias) {
        if (dias <= 0) throw new ValidacionException("Los días a simular deben ser positivos");
        return simularHasta(reloj.ahora().plusDays(dias));
    }

    /** Operaciones activas en el orden en que se ejecutarán. */
    public ListaEnlazada<OperacionProgramada> pendientesEnOrden() {
        return colaProgramadas.enOrden().filtrar(OperacionProgramada::isActiva);
    }

    public OperacionProgramada buscar(String id) {
        return tablaProgramadas.obtener(id);
    }

    public ListaEnlazada<String> getBitacora() {
        return lstBitacora;
    }

    public int cantidadPendientes() {
        return colaProgramadas.tamano();
    }

    private void bitacora(String linea) {
        lstBitacora.agregarAlFinal(linea);
    }
}
