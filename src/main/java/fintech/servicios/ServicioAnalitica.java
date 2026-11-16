package fintech.servicios;

import fintech.estructuras.ColaPrioridad;
import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.TablaHash;
import fintech.modelo.Billetera;
import fintech.modelo.EstadoTransaccion;
import fintech.modelo.TipoBilletera;
import fintech.modelo.TipoTransaccion;
import fintech.modelo.Transaccion;
import fintech.modelo.Usuario;

import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * Consultas e informes de uso.
 *
 * Patrón general: contar con una TablaHash (O(n)) y luego extraer el top-K con
 * un montículo de tamaño K (O(n log K)), en vez de ordenar todo (O(n log n)).
 * Las consultas por fecha usan el AVL por fecha y el top por valor el AVL por valor.
 */
public class ServicioAnalitica {

    private static final Comparator<ItemReporte> POR_CANTIDAD =
            Comparator.comparingInt(ItemReporte::getCantidad).thenComparingDouble(ItemReporte::getMonto);

    private final MotorTransacciones motor;
    private final GestorUsuarios usuarios;

    public ServicioAnalitica(MotorTransacciones motor, GestorUsuarios usuarios) {
        this.motor = motor;
        this.usuarios = usuarios;
    }

    /** Transacción aplicada (exitosa o luego revertida), sin contar reversiones ni rechazadas. */
    private static boolean aplicada(Transaccion t) {
        return t.getTipo() != TipoTransaccion.REVERSION && t.getEstado() != EstadoTransaccion.RECHAZADA;
    }

    /** Solo movimientos vigentes (exitosos y no revertidos). */
    private static boolean vigente(Transaccion t) {
        return t.getTipo() != TipoTransaccion.REVERSION && t.getEstado() == EstadoTransaccion.EXITOSA;
    }

    // ------------------------------------------------------------ billeteras con mayor uso
    public ListaEnlazada<ItemReporte> billeterasMayorUso(int k) {
        TablaHash<String, ItemReporte> conteo = new TablaHash<>();
        for (Transaccion t : motor.getTransacciones()) {
            if (!aplicada(t)) continue;
            if (t.getBilleteraOrigen() != null) contar(conteo, etiqueta(t.getBilleteraOrigen()), t.getValor());
            if (t.getBilleteraDestino() != null) contar(conteo, etiqueta(t.getBilleteraDestino()), t.getValor());
        }
        return topK(conteo, k, POR_CANTIDAD);
    }

    // ------------------------------------------------------------ usuarios con más transferencias
    public ListaEnlazada<ItemReporte> usuariosMasTransferencias(int k) {
        TablaHash<String, ItemReporte> conteo = new TablaHash<>();
        for (Transaccion t : motor.getTransacciones()) {
            if (aplicada(t) && t.getTipo().esTransferencia()) contar(conteo, t.getUsuario().toString(), t.getValor());
        }
        return topK(conteo, k, POR_CANTIDAD);
    }

    // ------------------------------------------------------------ categorías más activas
    public ListaEnlazada<ItemReporte> categoriasMasActivas() {
        TablaHash<String, ItemReporte> conteo = new TablaHash<>();
        for (TipoBilletera tb : TipoBilletera.values()) conteo.poner(tb.getEtiqueta(), new ItemReporte(tb.getEtiqueta(), 0, 0, ""));
        for (Transaccion t : motor.getTransacciones()) {
            if (!aplicada(t)) continue;
            if (t.getBilleteraOrigen() != null) contar(conteo, t.getBilleteraOrigen().getTipo().getEtiqueta(), t.getValor());
            if (t.getBilleteraDestino() != null) contar(conteo, t.getBilleteraDestino().getTipo().getEtiqueta(), t.getValor());
        }
        return topK(conteo, conteo.tamano(), POR_CANTIDAD);
    }

    // ------------------------------------------------------------ frecuencia por tipo
    public ListaEnlazada<ItemReporte> frecuenciaPorTipo() {
        TablaHash<String, ItemReporte> conteo = new TablaHash<>();
        for (TipoTransaccion tt : TipoTransaccion.values()) conteo.poner(tt.getEtiqueta(), new ItemReporte(tt.getEtiqueta(), 0, 0, ""));
        conteo.poner("Rechazadas", new ItemReporte("Rechazadas", 0, 0, "operaciones no aplicadas"));
        for (Transaccion t : motor.getTransacciones()) {
            if (t.getEstado() == EstadoTransaccion.RECHAZADA) contar(conteo, "Rechazadas", t.getValor());
            else contar(conteo, t.getTipo().getEtiqueta(), t.getValor());
        }
        return topK(conteo, conteo.tamano(), POR_CANTIDAD);
    }

    // ------------------------------------------------------------ monto en rango de tiempo
    /** Monto total movilizado (operaciones vigentes) en [desde, hasta], usando el AVL por fecha. */
    public ItemReporte montoTotalEnRango(LocalDateTime desde, LocalDateTime hasta) {
        ListaEnlazada<Transaccion> enRango = motor.enPeriodo(desde, hasta);
        ItemReporte r = new ItemReporte("Monto movilizado", 0, 0, "");
        for (Transaccion t : enRango) if (vigente(t)) r.sumar(t.getValor());
        r.setDetalle(enRango.tamano() + " registros en el período (incluye rechazadas/reversiones no sumadas)");
        return r;
    }

    // ------------------------------------------------------------ usuario con mayor actividad
    public ItemReporte usuarioMayorActividad(LocalDateTime desde, LocalDateTime hasta) {
        TablaHash<String, ItemReporte> conteo = new TablaHash<>();
        for (Transaccion t : motor.enPeriodo(desde, hasta)) if (aplicada(t)) contar(conteo, t.getUsuario().toString(), t.getValor());
        ListaEnlazada<ItemReporte> top = topK(conteo, 1, POR_CANTIDAD);
        return top.estaVacia() ? null : top.primero();
    }

    public ListaEnlazada<ItemReporte> actividadPorUsuario(LocalDateTime desde, LocalDateTime hasta, int k) {
        TablaHash<String, ItemReporte> conteo = new TablaHash<>();
        for (Transaccion t : motor.enPeriodo(desde, hasta)) if (aplicada(t)) contar(conteo, t.getUsuario().toString(), t.getValor());
        return topK(conteo, k, POR_CANTIDAD);
    }

    // ------------------------------------------------------------ mayor valor (estructura ordenada)
    public ListaEnlazada<Transaccion> transaccionesMayorValor(int k) {
        return motor.mayoresValores(k);
    }

    // ------------------------------------------------------------ resumen
    public ListaEnlazada<ItemReporte> resumenGeneral() {
        ListaEnlazada<ItemReporte> r = new ListaEnlazada<>();
        int exitosas = 0, rechazadas = 0, revertidas = 0;
        double movilizado = 0, saldoTotal = 0;
        for (Transaccion t : motor.getTransacciones()) {
            if (t.getEstado() == EstadoTransaccion.RECHAZADA) rechazadas++;
            else if (t.getEstado() == EstadoTransaccion.REVERTIDA) revertidas++;
            else if (t.getTipo() != TipoTransaccion.REVERSION) {
                exitosas++;
                movilizado += t.getValor();
            }
        }
        for (Usuario u : usuarios.todos()) saldoTotal += u.saldoTotal();
        r.agregar(new ItemReporte("Usuarios registrados", usuarios.cantidad(), 0, ""));
        r.agregar(new ItemReporte("Transacciones vigentes", exitosas, movilizado, "monto = total movilizado"));
        r.agregar(new ItemReporte("Transacciones revertidas", revertidas, 0, ""));
        r.agregar(new ItemReporte("Transacciones rechazadas", rechazadas, 0, ""));
        r.agregar(new ItemReporte("Saldo total en billeteras", 0, saldoTotal, ""));
        r.agregar(new ItemReporte("Comisiones recaudadas", 0, motor.getComisionesRecaudadas(), ""));
        return r;
    }

    // ------------------------------------------------------------ utilidades
    private static String etiqueta(Billetera b) {
        return b.getCodigo() + " " + b.getNombre() + " (" + b.getUsuario().getNombre() + ")";
    }

    private static void contar(TablaHash<String, ItemReporte> conteo, String clave, double valor) {
        ItemReporte it = conteo.obtener(clave);
        if (it == null) {
            it = new ItemReporte(clave, 0, 0, "");
            conteo.poner(clave, it);
        }
        it.sumar(valor);
    }

    /**
     * Top-K con un montículo mínimo de tamaño K: se mantiene en la cima el
     * "peor" de los K mejores y se reemplaza cuando llega uno mayor.
     * Resultado en orden descendente.
     */
    static ListaEnlazada<ItemReporte> topK(TablaHash<String, ItemReporte> conteo, int k, Comparator<ItemReporte> cmp) {
        ColaPrioridad<ItemReporte> heap = new ColaPrioridad<>(cmp);
        for (ItemReporte it : conteo.valores()) {
            if (heap.tamano() < k) heap.insertar(it);
            else if (k > 0 && cmp.compare(it, heap.verMinimo()) > 0) {
                heap.extraer();
                heap.insertar(it);
            }
        }
        ListaEnlazada<ItemReporte> r = new ListaEnlazada<>();
        while (!heap.estaVacia()) r.agregarAlInicio(heap.extraer());
        return r;
    }
}
