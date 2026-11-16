package fintech.modelo;

import java.time.LocalDateTime;

/**
 * Movimiento automático que debe ejecutarse en una fecha futura.
 * Se almacena en una cola de prioridad ordenada por fecha de ejecución.
 */
public class OperacionProgramada {

    private final String id;
    private final long secuencia;                 // desempate estable (orden de creación)
    private final Usuario theUsuario;
    private final TipoOperacionProgramada tipo;
    private final Billetera theBilleteraOrigen;   // null en recarga automática
    private final Billetera theBilleteraDestino;
    private final double valor;
    private LocalDateTime fechaEjecucion;
    private final Frecuencia frecuencia;
    private int repeticionesRestantes;            // -1 = indefinidas
    private final String descripcion;
    private boolean activa = true;
    private boolean alertaPreviaEnviada;
    private int ejecucionesExitosas;
    private int ejecucionesFallidas;
    private int prioridadNivel;                   // copia de la prioridad del nivel al entrar a la cola

    /** Fija la prioridad (según el nivel actual del usuario) antes de insertar en la cola. */
    public void fijarPrioridad() {
        this.prioridadNivel = theUsuario.getNivel().getPrioridad();
    }

    public int getPrioridadNivel() {
        return prioridadNivel;
    }

    public OperacionProgramada(String id, long secuencia, Usuario usuario, TipoOperacionProgramada tipo,
                               Billetera origen, Billetera destino, double valor, LocalDateTime fechaEjecucion,
                               Frecuencia frecuencia, int repeticiones, String descripcion) {
        this.id = id;
        this.secuencia = secuencia;
        this.theUsuario = usuario;
        this.tipo = tipo;
        this.theBilleteraOrigen = origen;
        this.theBilleteraDestino = destino;
        this.valor = valor;
        this.fechaEjecucion = fechaEjecucion;
        this.frecuencia = frecuencia;
        this.repeticionesRestantes = frecuencia == Frecuencia.UNICA ? 1 : repeticiones;
        this.descripcion = descripcion == null ? "" : descripcion;
    }

    /**
     * Avanza a la siguiente ocurrencia. Retorna false si ya no debe repetirse.
     */
    public boolean reprogramar() {
        if (repeticionesRestantes > 0) repeticionesRestantes--;
        LocalDateTime sig = frecuencia.siguiente(fechaEjecucion);
        if (sig == null || repeticionesRestantes == 0) {
            activa = false;
            return false;
        }
        fechaEjecucion = sig;
        alertaPreviaEnviada = false;
        return true;
    }

    public void registrarResultado(boolean exito) {
        if (exito) ejecucionesExitosas++; else ejecucionesFallidas++;
    }

    public void cancelar() {
        activa = false;
    }

    // ------------------------------------------------------------ getters
    public String getId() {
        return id;
    }

    public long getSecuencia() {
        return secuencia;
    }

    public Usuario getUsuario() {
        return theUsuario;
    }

    public TipoOperacionProgramada getTipo() {
        return tipo;
    }

    public Billetera getBilleteraOrigen() {
        return theBilleteraOrigen;
    }

    public Billetera getBilleteraDestino() {
        return theBilleteraDestino;
    }

    public double getValor() {
        return valor;
    }

    public LocalDateTime getFechaEjecucion() {
        return fechaEjecucion;
    }

    public Frecuencia getFrecuencia() {
        return frecuencia;
    }

    public int getRepeticionesRestantes() {
        return repeticionesRestantes;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActiva() {
        return activa;
    }

    public boolean isAlertaPreviaEnviada() {
        return alertaPreviaEnviada;
    }

    public void setAlertaPreviaEnviada(boolean v) {
        this.alertaPreviaEnviada = v;
    }

    public int getEjecucionesExitosas() {
        return ejecucionesExitosas;
    }

    public int getEjecucionesFallidas() {
        return ejecucionesFallidas;
    }

    @Override
    public String toString() {
        return id + " " + tipo + " $" + String.format("%,.0f", valor) + " @ " + fechaEjecucion;
    }
}
