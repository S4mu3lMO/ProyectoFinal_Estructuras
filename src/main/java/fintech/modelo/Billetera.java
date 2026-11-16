package fintech.modelo;

import fintech.estructuras.ListaEnlazada;

import java.time.LocalDateTime;

/**
 * Billetera digital de un usuario. Mantiene su saldo y su propio historial
 * de transacciones (lista enlazada).
 */
public class Billetera {

    public static final double SALDO_MINIMO_ALERTA_POR_DEFECTO = 100;

    private final String codigo;
    private String nombre;
    private TipoBilletera tipo;
    private double saldo;
    private EstadoBilletera estado = EstadoBilletera.ACTIVA;
    private final LocalDateTime fechaCreacion;
    private double saldoMinimoAlerta = SALDO_MINIMO_ALERTA_POR_DEFECTO;
    private final Usuario theUsuario;
    private final ListaEnlazada<Transaccion> lstTransaccion = new ListaEnlazada<>();

    public Billetera(String codigo, String nombre, TipoBilletera tipo, Usuario theUsuario, LocalDateTime fechaCreacion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.theUsuario = theUsuario;
        this.fechaCreacion = fechaCreacion;
    }

    public void depositar(double valor) {
        if (valor <= 0) throw new IllegalArgumentException("El valor a depositar debe ser positivo");
        saldo = redondear(saldo + valor);
    }

    public void debitar(double valor) {
        if (valor <= 0) throw new IllegalArgumentException("El valor a debitar debe ser positivo");
        if (valor > saldo + 1e-9) throw new IllegalStateException("Saldo insuficiente en " + codigo);
        saldo = redondear(saldo - valor);
    }

    public boolean tieneSaldo(double valor) {
        return saldo + 1e-9 >= valor;
    }

    public boolean estaActiva() {
        return estado == EstadoBilletera.ACTIVA;
    }

    public boolean saldoBajo() {
        return saldo < saldoMinimoAlerta;
    }

    public void registrarTransaccion(Transaccion t) {
        lstTransaccion.agregarAlFinal(t);
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ------------------------------------------------------------ getters / setters
    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoBilletera getTipo() {
        return tipo;
    }

    public void setTipo(TipoBilletera tipo) {
        this.tipo = tipo;
    }

    public double getSaldo() {
        return saldo;
    }

    public EstadoBilletera getEstado() {
        return estado;
    }

    public void setEstado(EstadoBilletera estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public double getSaldoMinimoAlerta() {
        return saldoMinimoAlerta;
    }

    public void setSaldoMinimoAlerta(double v) {
        this.saldoMinimoAlerta = v;
    }

    public Usuario getUsuario() {
        return theUsuario;
    }

    public ListaEnlazada<Transaccion> getTransacciones() {
        return lstTransaccion;
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre + " [" + tipo + "]";
    }
}
