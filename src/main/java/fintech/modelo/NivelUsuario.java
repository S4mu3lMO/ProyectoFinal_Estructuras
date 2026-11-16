package fintech.modelo;

/**
 * Niveles de fidelización según los puntos acumulados y sus ventajas.
 */
public enum NivelUsuario {
    //        min    max              comisión  bono  límite por operación  prioridad
    BRONCE("Bronce", 0, 500,             0.0100, 0.00, 5_000, 1),
    PLATA("Plata", 501, 1000,            0.0075, 0.05, 10_000, 2),
    ORO("Oro", 1001, 5000,               0.0050, 0.10, 25_000, 3),
    PLATINO("Platino", 5001, Integer.MAX_VALUE, 0.0000, 0.20, 100_000, 4);

    private final String etiqueta;
    private final int puntosMin;
    private final int puntosMax;
    private final double comision;
    private final double bonoPuntos;
    private final double limiteOperacion;
    private final int prioridad;

    NivelUsuario(String etiqueta, int puntosMin, int puntosMax, double comision, double bonoPuntos,
                 double limiteOperacion, int prioridad) {
        this.etiqueta = etiqueta;
        this.puntosMin = puntosMin;
        this.puntosMax = puntosMax;
        this.comision = comision;
        this.bonoPuntos = bonoPuntos;
        this.limiteOperacion = limiteOperacion;
        this.prioridad = prioridad;
    }

    public static NivelUsuario desdePuntos(int puntos) {
        for (NivelUsuario n : values()) if (puntos >= n.puntosMin && puntos <= n.puntosMax) return n;
        return BRONCE;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getPuntosMin() {
        return puntosMin;
    }

    public int getPuntosMax() {
        return puntosMax;
    }

    /** Porcentaje de comisión sobre retiros y transferencias a terceros. */
    public double getComision() {
        return comision;
    }

    /** Bono adicional de puntos (porcentaje) sobre cada transacción. */
    public double getBonoPuntos() {
        return bonoPuntos;
    }

    public double getLimiteOperacion() {
        return limiteOperacion;
    }

    /** Prioridad en la cola de operaciones programadas (mayor = se atiende primero en empate). */
    public int getPrioridad() {
        return prioridad;
    }

    public String ventajas() {
        return String.format("Comisión %.2f%% · Bono de puntos %d%% · Límite por operación $%,.2f · Prioridad %d",
                comision * 100, Math.round(bonoPuntos * 100), limiteOperacion, prioridad);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
