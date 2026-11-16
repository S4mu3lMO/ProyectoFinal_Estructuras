package fintech.modelo;

import java.time.LocalDateTime;

/**
 * Movimiento financiero. Registra identificador, fecha, tipo, valor,
 * billetera origen y destino, estado, puntos generados y nivel de riesgo.
 * También guarda los saldos previos (estado anterior) para poder revertirla.
 */
public class Transaccion {

    private final String id;
    private final LocalDateTime fecha;
    private final TipoTransaccion tipo;
    private final double valor;
    private double comision;
    private final Billetera theBilleteraOrigen;   // null en recargas
    private final Billetera theBilleteraDestino;  // null en retiros
    private final Usuario theUsuario;             // quien origina la operación
    private EstadoTransaccion estado;
    private int puntosGenerados;
    private NivelRiesgo nivelRiesgo = NivelRiesgo.NINGUNO;
    private String descripcion;
    private String motivoRechazo;
    private boolean programada;
    private String idTransaccionRelacionada;      // en una reversión: id de la original; en la original: id de la reversión

    // estado anterior (para restauración / auditoría)
    private double saldoOrigenAntes;
    private double saldoDestinoAntes;

    public Transaccion(String id, LocalDateTime fecha, TipoTransaccion tipo, double valor,
                       Billetera origen, Billetera destino, Usuario usuario, String descripcion) {
        this.id = id;
        this.fecha = fecha;
        this.tipo = tipo;
        this.valor = valor;
        this.theBilleteraOrigen = origen;
        this.theBilleteraDestino = destino;
        this.theUsuario = usuario;
        this.descripcion = descripcion == null ? "" : descripcion;
        this.estado = EstadoTransaccion.EXITOSA;
        if (origen != null) saldoOrigenAntes = origen.getSaldo();
        if (destino != null) saldoDestinoAntes = destino.getSaldo();
    }

    public boolean esExitosa() {
        return estado == EstadoTransaccion.EXITOSA;
    }

    /** Puede revertirse si es de un tipo reversible y sigue vigente. */
    public boolean esReversible() {
        return tipo.esReversible() && estado == EstadoTransaccion.EXITOSA;
    }

    public void rechazar(String motivo) {
        estado = EstadoTransaccion.RECHAZADA;
        motivoRechazo = motivo;
        puntosGenerados = 0;
    }

    public void marcarRevertida(String idReversion) {
        estado = EstadoTransaccion.REVERTIDA;
        idTransaccionRelacionada = idReversion;
    }

    /** Usuario dueño de la billetera destino (si existe). */
    public Usuario usuarioDestino() {
        return theBilleteraDestino == null ? null : theBilleteraDestino.getUsuario();
    }

    // ------------------------------------------------------------ getters / setters
    public String getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public double getValor() {
        return valor;
    }

    public double getComision() {
        return comision;
    }

    public void setComision(double comision) {
        this.comision = comision;
    }

    public Billetera getBilleteraOrigen() {
        return theBilleteraOrigen;
    }

    public Billetera getBilleteraDestino() {
        return theBilleteraDestino;
    }

    public Usuario getUsuario() {
        return theUsuario;
    }

    public EstadoTransaccion getEstado() {
        return estado;
    }

    public int getPuntosGenerados() {
        return puntosGenerados;
    }

    public void setPuntosGenerados(int p) {
        this.puntosGenerados = p;
    }

    public NivelRiesgo getNivelRiesgo() {
        return nivelRiesgo;
    }

    public void setNivelRiesgo(NivelRiesgo nivelRiesgo) {
        this.nivelRiesgo = nivelRiesgo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public boolean isProgramada() {
        return programada;
    }

    public void setProgramada(boolean programada) {
        this.programada = programada;
    }

    public String getIdTransaccionRelacionada() {
        return idTransaccionRelacionada;
    }

    public void setIdTransaccionRelacionada(String id) {
        this.idTransaccionRelacionada = id;
    }

    public double getSaldoOrigenAntes() {
        return saldoOrigenAntes;
    }

    public double getSaldoDestinoAntes() {
        return saldoDestinoAntes;
    }

    public String codigoOrigen() {
        return theBilleteraOrigen == null ? "EXTERNO" : theBilleteraOrigen.getCodigo();
    }

    public String codigoDestino() {
        return theBilleteraDestino == null ? "EXTERNO" : theBilleteraDestino.getCodigo();
    }

    @Override
    public String toString() {
        return String.format("%s %s $%,.0f %s->%s [%s]", id, tipo, valor, codigoOrigen(), codigoDestino(), estado);
    }
}
