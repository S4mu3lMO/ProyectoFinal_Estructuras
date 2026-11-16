package fintech.modelo;

/**
 * Tipos de transacción con su factor de puntos (puntos por cada 100 unidades).
 */
public enum TipoTransaccion {
    RECARGA("Recarga", 1, true),
    RETIRO("Retiro", 2, true),
    TRANSFERENCIA_PROPIA("Transferencia propia", 3, true),
    TRANSFERENCIA_EXTERNA("Transferencia a tercero", 3, true),
    REVERSION("Reversión", 0, false);

    private final String etiqueta;
    private final int puntosPorCada100;
    private final boolean reversible;

    TipoTransaccion(String etiqueta, int puntosPorCada100, boolean reversible) {
        this.etiqueta = etiqueta;
        this.puntosPorCada100 = puntosPorCada100;
        this.reversible = reversible;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getPuntosPorCada100() {
        return puntosPorCada100;
    }

    public boolean esReversible() {
        return reversible;
    }

    public boolean esTransferencia() {
        return this == TRANSFERENCIA_PROPIA || this == TRANSFERENCIA_EXTERNA;
    }

    /** Operaciones que sacan dinero de una billetera del usuario. */
    public boolean esSalida() {
        return this == RETIRO || esTransferencia();
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
