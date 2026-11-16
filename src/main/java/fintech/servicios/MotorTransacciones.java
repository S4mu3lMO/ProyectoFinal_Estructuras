package fintech.servicios;

import fintech.deteccion.DetectorFraude;
import fintech.deteccion.ResultadoDeteccion;
import fintech.estructuras.ArbolAVL;
import fintech.estructuras.Grafo;
import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.Pila;
import fintech.estructuras.TablaHash;
import fintech.modelo.Billetera;
import fintech.modelo.EstadoBilletera;
import fintech.modelo.NivelRiesgo;
import fintech.modelo.TipoNotificacion;
import fintech.modelo.TipoTransaccion;
import fintech.modelo.Transaccion;
import fintech.modelo.Usuario;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Motor de operaciones financieras: recargas, retiros, transferencias y
 * reversiones.
 *
 * Índices que mantiene:
 *  - ListaEnlazada global (orden cronológico);
 *  - TablaHash id -> Transaccion (búsqueda O(1) para revertir);
 *  - ArbolAVL valor -> Transaccion (top de mayor valor);
 *  - ArbolAVL fecha -> Transaccion (consultas por rango de tiempo);
 *  - Grafo de usuarios y grafo de billeteras (relaciones de transferencia);
 *  - Pila de reversibles por usuario (deshacer).
 */
public class MotorTransacciones {

    private final ListaEnlazada<Transaccion> lstTransaccion = new ListaEnlazada<>();
    private final TablaHash<String, Transaccion> tablaTransacciones = new TablaHash<>();
    private final ArbolAVL<Double, Transaccion> arbolPorValor = new ArbolAVL<>();
    private final ArbolAVL<LocalDateTime, Transaccion> arbolPorFecha = new ArbolAVL<>();
    private final Grafo<String> grafoUsuarios = new Grafo<>();
    private final Grafo<String> grafoBilleteras = new Grafo<>();

    private final GestorBilleteras billeteras;
    private final GestorRecompensas recompensas;
    private final GestorNotificaciones notificaciones;
    private final DetectorFraude detector;
    private final GeneradorId ids;
    private final Reloj reloj;

    private double comisionesRecaudadas;
    private Duration ventanaReversion = Duration.ofHours(72);

    public MotorTransacciones(GestorBilleteras billeteras, GestorRecompensas recompensas,
                              GestorNotificaciones notificaciones, DetectorFraude detector,
                              GeneradorId ids, Reloj reloj) {
        this.billeteras = billeteras;
        this.recompensas = recompensas;
        this.notificaciones = notificaciones;
        this.detector = detector;
        this.ids = ids;
        this.reloj = reloj;
    }

    // ================================================================ operaciones públicas
    public Transaccion recargar(String codigoDestino, double valor, String descripcion) {
        Billetera destino = billeteras.obtener(codigoDestino);
        return ejecutar(TipoTransaccion.RECARGA, null, destino, valor, descripcion, false);
    }

    public Transaccion retirar(String codigoOrigen, double valor, String descripcion) {
        Billetera origen = billeteras.obtener(codigoOrigen);
        return ejecutar(TipoTransaccion.RETIRO, origen, null, valor, descripcion, false);
    }

    public Transaccion transferir(String codigoOrigen, String codigoDestino, double valor, String descripcion) {
        Billetera origen = billeteras.obtener(codigoOrigen);
        Billetera destino = billeteras.obtener(codigoDestino);
        if (origen == destino) throw new ValidacionException("La billetera origen y destino no pueden ser la misma");
        TipoTransaccion tipo = origen.getUsuario() == destino.getUsuario()
                ? TipoTransaccion.TRANSFERENCIA_PROPIA : TipoTransaccion.TRANSFERENCIA_EXTERNA;
        return ejecutar(tipo, origen, destino, valor, descripcion, false);
    }

    /**
     * Núcleo común de todas las operaciones. Las validaciones de forma
     * (valor no numérico, billetera inexistente) lanzan excepción; las de
     * negocio (saldo, límite, estado, riesgo) registran la transacción como
     * RECHAZADA y generan una alerta.
     */
    public Transaccion ejecutar(TipoTransaccion tipo, Billetera origen, Billetera destino, double valor,
                                String descripcion, boolean programada) {
        if (Double.isNaN(valor) || Double.isInfinite(valor) || valor <= 0)
            throw new ValidacionException("El valor debe ser un número mayor que cero");
        if (tipo == TipoTransaccion.REVERSION) throw new ValidacionException("Use revertir() para reversiones");
        if (tipo.esSalida() && origen == null) throw new ValidacionException("La operación requiere billetera origen");
        if ((tipo == TipoTransaccion.RECARGA || tipo.esTransferencia()) && destino == null)
            throw new ValidacionException("La operación requiere billetera destino");
        if (origen != null && origen == destino) throw new ValidacionException("La billetera origen y destino no pueden ser la misma");

        valor = Math.round(valor * 100.0) / 100.0;
        Usuario u = origen != null ? origen.getUsuario() : destino.getUsuario();
        Transaccion t = new Transaccion(ids.siguiente("TX", 6), reloj.ahora(), tipo, valor, origen, destino, u, descripcion);
        t.setProgramada(programada);

        double comision = (tipo == TipoTransaccion.RETIRO || tipo == TipoTransaccion.TRANSFERENCIA_EXTERNA)
                ? Math.round(valor * u.getNivel().getComision() * 100.0) / 100.0 : 0;

        // ---------- validaciones de negocio
        String motivo = null;
        if (origen != null && !origen.estaActiva()) motivo = "La billetera origen " + origen.getCodigo() + " está " + origen.getEstado();
        else if (destino != null && !destino.estaActiva()) motivo = "La billetera destino " + destino.getCodigo() + " está " + destino.getEstado();
        else if (valor > u.getNivel().getLimiteOperacion())
            motivo = String.format("Supera el límite por operación del nivel %s ($%,.2f)", u.getNivel(), u.getNivel().getLimiteOperacion());
        else if (origen != null && !origen.tieneSaldo(valor + comision))
            motivo = String.format("Saldo insuficiente en %s (saldo $%,.2f, requerido $%,.2f)", origen.getCodigo(), origen.getSaldo(), valor + comision);

        if (motivo != null) return rechazar(t, motivo);

        // ---------- detección de comportamiento inusual (antes de aplicar)
        ResultadoDeteccion riesgo = detector.evaluar(t);
        t.setNivelRiesgo(riesgo.getNivel());
        if (riesgo.hayRiesgo()) {
            boolean bloquear = detector.debeBloquear(riesgo);
            detector.registrar(t, riesgo, bloquear);
            if (bloquear) return rechazar(t, "Bloqueada preventivamente por riesgo " + NivelRiesgo.CRITICO);
        }

        // ---------- aplicar movimiento
        if (origen != null) origen.debitar(valor + comision);
        if (destino != null) destino.depositar(valor);
        t.setComision(comision);
        comisionesRecaudadas += comision;

        int puntos = recompensas.calcularPuntos(tipo, valor, u.getNivel(), programada);
        t.setPuntosGenerados(puntos);

        registrarEnIndices(t);
        u.getPilaReversibles().apilar(t);
        if (tipo.esTransferencia()) grafoBilleteras.registrarArista(origen.getCodigo(), destino.getCodigo(), valor);
        if (tipo == TipoTransaccion.TRANSFERENCIA_EXTERNA)
            grafoUsuarios.registrarArista(u.getId(), destino.getUsuario().getId(), valor);
        arbolPorValor.insertar(valor, t);

        recompensas.ajustarPuntos(u, puntos);

        if (origen != null && origen.saldoBajo())
            notificaciones.notificar(u, TipoNotificacion.SALDO_BAJO,
                    String.format("Saldo bajo en %s: $%,.2f", origen.getCodigo(), origen.getSaldo()));
        return t;
    }

    private Transaccion rechazar(Transaccion t, String motivo) {
        t.rechazar(motivo);
        registrarEnIndices(t);
        notificaciones.notificar(t.getUsuario(), TipoNotificacion.OPERACION_RECHAZADA,
                t.getTipo() + String.format(" por $%,.2f rechazada: ", t.getValor()) + motivo);
        return t;
    }

    private void registrarEnIndices(Transaccion t) {
        lstTransaccion.agregarAlFinal(t);
        tablaTransacciones.poner(t.getId(), t);
        arbolPorFecha.insertar(t.getFecha(), t);
        t.getUsuario().registrarTransaccion(t);
        if (t.getBilleteraOrigen() != null) t.getBilleteraOrigen().registrarTransaccion(t);
        if (t.getBilleteraDestino() != null) {
            t.getBilleteraDestino().registrarTransaccion(t);
            Usuario receptor = t.getBilleteraDestino().getUsuario();
            if (receptor != t.getUsuario()) receptor.registrarTransaccion(t); // el receptor también la ve en su historial
        }
    }

    // ================================================================ reversión (pila)
    /** Revierte la operación reversible más reciente del usuario (cima de su pila). */
    public Transaccion revertirUltima(Usuario u) {
        Pila<Transaccion> pila = u.getPilaReversibles();
        while (!pila.estaVacia() && !pila.cima().esReversible()) pila.desapilar(); // limpiar entradas obsoletas
        if (pila.estaVacia()) throw new ValidacionException("El usuario no tiene operaciones reversibles");
        Transaccion t = pila.cima();
        validarReversion(t);
        pila.desapilar();
        return aplicarReversion(t, "Deshacer última operación");
    }

    /**
     * Revierte una operación concreta por id. Se busca en la pila del usuario
     * usando una pila auxiliar para no alterar el orden de las demás.
     */
    public Transaccion revertirPorId(String idTransaccion, String motivo) {
        Transaccion t = tablaTransacciones.obtener(idTransaccion == null ? "" : idTransaccion.trim().toUpperCase());
        if (t == null) throw new ValidacionException("No existe la transacción " + idTransaccion);
        validarReversion(t);
        Pila<Transaccion> pila = t.getUsuario().getPilaReversibles();
        Pila<Transaccion> auxiliar = new Pila<>();
        boolean encontrada = false;
        while (!pila.estaVacia()) {
            Transaccion x = pila.desapilar();
            if (x == t) {
                encontrada = true;
                break;
            }
            auxiliar.apilar(x);
        }
        while (!auxiliar.estaVacia()) pila.apilar(auxiliar.desapilar());
        if (!encontrada) throw new ValidacionException("La transacción no está en la pila de operaciones reversibles");
        return aplicarReversion(t, motivo == null || motivo.isBlank() ? "Reversión solicitada" : motivo);
    }

    private void validarReversion(Transaccion t) {
        if (!t.esReversible()) throw new ValidacionException("La transacción " + t.getId() + " no es reversible (estado " + t.getEstado() + ", tipo " + t.getTipo() + ")");
        if (Duration.between(t.getFecha(), reloj.ahora()).compareTo(ventanaReversion) > 0)
            throw new ValidacionException("Venció el plazo de reversión (" + ventanaReversion.toHours() + " h)");
        Billetera d = t.getBilleteraDestino(), o = t.getBilleteraOrigen();
        if (d != null && !d.tieneSaldo(t.getValor()))
            throw new ValidacionException(String.format("La billetera destino %s ya no tiene saldo suficiente ($%,.2f) para revertir", d.getCodigo(), d.getSaldo()));
        if ((d != null && d.getEstado() == EstadoBilletera.CERRADA) || (o != null && o.getEstado() == EstadoBilletera.CERRADA))
            throw new ValidacionException("No se puede revertir: hay una billetera cerrada");
    }

    /** Restaura saldos, historial, puntos y grafo. */
    private Transaccion aplicarReversion(Transaccion t, String motivo) {
        Billetera origen = t.getBilleteraOrigen(), destino = t.getBilleteraDestino();
        if (destino != null) destino.debitar(t.getValor());
        if (origen != null) origen.depositar(t.getValor() + t.getComision());
        comisionesRecaudadas -= t.getComision();

        Transaccion rev = new Transaccion(ids.siguiente("TX", 6), reloj.ahora(), TipoTransaccion.REVERSION, t.getValor(),
                destino, origen, t.getUsuario(), motivo + " de " + t.getId());
        rev.setIdTransaccionRelacionada(t.getId());
        t.marcarRevertida(rev.getId());
        registrarEnIndices(rev);

        arbolPorValor.eliminar(t.getValor(), t);
        if (t.getTipo().esTransferencia()) grafoBilleteras.descontarArista(origen.getCodigo(), destino.getCodigo(), t.getValor());
        if (t.getTipo() == TipoTransaccion.TRANSFERENCIA_EXTERNA)
            grafoUsuarios.descontarArista(t.getUsuario().getId(), destino.getUsuario().getId(), t.getValor());

        // recalcular puntos: se descuentan los que generó la operación original
        recompensas.ajustarPuntos(t.getUsuario(), -t.getPuntosGenerados());

        notificaciones.notificar(t.getUsuario(), TipoNotificacion.REVERSION,
                String.format("Se revirtió %s (%s por $%,.2f). Puntos descontados: %d", t.getId(), t.getTipo(), t.getValor(), t.getPuntosGenerados()));
        if (destino != null && destino.getUsuario() != t.getUsuario())
            notificaciones.notificar(destino.getUsuario(), TipoNotificacion.REVERSION,
                    String.format("Se revirtió la transferencia %s recibida por $%,.2f", t.getId(), t.getValor()));
        return rev;
    }

    // ================================================================ consultas
    public Transaccion buscar(String id) {
        return id == null ? null : tablaTransacciones.obtener(id.trim().toUpperCase());
    }

    public ListaEnlazada<Transaccion> enPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        if (desde.isAfter(hasta)) throw new ValidacionException("La fecha inicial es posterior a la final");
        return arbolPorFecha.rango(desde, hasta);
    }

    public ListaEnlazada<Transaccion> mayoresValores(int k) {
        return arbolPorValor.mayores(k);
    }

    public ListaEnlazada<Transaccion> getTransacciones() {
        return lstTransaccion;
    }

    public TablaHash<String, Transaccion> getTablaTransacciones() {
        return tablaTransacciones;
    }

    public ArbolAVL<Double, Transaccion> getArbolPorValor() {
        return arbolPorValor;
    }

    public ArbolAVL<LocalDateTime, Transaccion> getArbolPorFecha() {
        return arbolPorFecha;
    }

    public Grafo<String> getGrafoUsuarios() {
        return grafoUsuarios;
    }

    public Grafo<String> getGrafoBilleteras() {
        return grafoBilleteras;
    }

    public double getComisionesRecaudadas() {
        return comisionesRecaudadas;
    }

    public Duration getVentanaReversion() {
        return ventanaReversion;
    }

    public void setVentanaReversion(Duration d) {
        this.ventanaReversion = d;
    }
}
