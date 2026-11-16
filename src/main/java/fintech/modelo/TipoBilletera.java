package fintech.modelo;

/** Propósito de una billetera (categoría usada en la analítica). */
public enum TipoBilletera {
    AHORRO("Ahorro"),
    GASTOS_DIARIOS("Gastos diarios"),
    COMPRAS("Compras"),
    TRANSPORTE("Transporte"),
    INVERSION("Inversión"),
    SUSCRIPCIONES("Suscripciones");

    private final String etiqueta;

    TipoBilletera(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
