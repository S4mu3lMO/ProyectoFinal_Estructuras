package fintech.modelo;

import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.Pila;

import java.time.LocalDateTime;

/**
 * Usuario de la plataforma. Agrupa sus billeteras, su historial de
 * transacciones (lista), su pila de operaciones reversibles, sus beneficios
 * canjeados y sus notificaciones recientes.
 *
 * Se manejan dos contadores de puntos:
 *  - puntosAcumulados: históricos, determinan el nivel (solo bajan por reversiones);
 *  - puntosDisponibles: saldo canjeable por beneficios.
 */
public class Usuario extends Persona implements Notificable {

    public static final int MAX_NOTIFICACIONES_RECIENTES = 30;

    private final LocalDateTime fechaRegistro;
    private int puntosAcumulados;
    private int puntosDisponibles;
    private NivelUsuario nivel = NivelUsuario.BRONCE;

    private final ListaEnlazada<Billetera> lstBilletera = new ListaEnlazada<>();
    private final ListaEnlazada<Transaccion> lstTransaccion = new ListaEnlazada<>();
    private final Pila<Transaccion> pilaReversibles = new Pila<>();
    private final ListaEnlazada<BeneficioCanjeado> lstBeneficioCanjeado = new ListaEnlazada<>();
    private final ListaEnlazada<Notificacion> lstNotificacion = new ListaEnlazada<>();

    public Usuario(String id, String nombre, String correo, String telefono, LocalDateTime fechaRegistro) {
        super(id, nombre, correo, telefono);
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public String tipoPersona() {
        return "Usuario " + nivel.getEtiqueta();
    }

    // ------------------------------------------------------------ billeteras
    public void agregarBilletera(Billetera b) {
        lstBilletera.agregarAlFinal(b);
    }

    public boolean quitarBilletera(Billetera b) {
        return lstBilletera.eliminar(b);
    }

    public double saldoTotal() {
        double s = 0;
        for (Billetera b : lstBilletera) s += b.getSaldo();
        return s;
    }

    // ------------------------------------------------------------ historial
    public void registrarTransaccion(Transaccion t) {
        lstTransaccion.agregarAlFinal(t);
    }

    // ------------------------------------------------------------ puntos
    /** Suma (o resta, si es negativo) puntos. Retorna true si cambió de nivel. */
    public boolean ajustarPuntos(int delta) {
        puntosAcumulados = Math.max(0, puntosAcumulados + delta);
        puntosDisponibles = Math.max(0, puntosDisponibles + delta);
        NivelUsuario nuevo = NivelUsuario.desdePuntos(puntosAcumulados);
        boolean cambio = nuevo != nivel;
        nivel = nuevo;
        return cambio;
    }

    /** Descuenta puntos canjeables (no afecta el nivel). */
    public void gastarPuntos(int puntos) {
        if (puntos > puntosDisponibles) throw new IllegalStateException("Puntos insuficientes");
        puntosDisponibles -= puntos;
    }

    public void registrarCanje(BeneficioCanjeado bc) {
        lstBeneficioCanjeado.agregarAlFinal(bc);
    }

    // ------------------------------------------------------------ notificaciones
    @Override
    public void recibirNotificacion(Notificacion n) {
        lstNotificacion.agregarAlFinal(n);
        // historial acotado: se descartan las más antiguas
        while (lstNotificacion.tamano() > MAX_NOTIFICACIONES_RECIENTES) lstNotificacion.eliminarPrimero();
    }

    @Override
    public ListaEnlazada<Notificacion> notificacionesRecientes() {
        return lstNotificacion;
    }

    // ------------------------------------------------------------ getters
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public int getPuntosAcumulados() {
        return puntosAcumulados;
    }

    public int getPuntosDisponibles() {
        return puntosDisponibles;
    }

    public NivelUsuario getNivel() {
        return nivel;
    }

    public ListaEnlazada<Billetera> getBilleteras() {
        return lstBilletera;
    }

    public ListaEnlazada<Transaccion> getTransacciones() {
        return lstTransaccion;
    }

    public Pila<Transaccion> getPilaReversibles() {
        return pilaReversibles;
    }

    public ListaEnlazada<BeneficioCanjeado> getBeneficiosCanjeados() {
        return lstBeneficioCanjeado;
    }
}
