package fintech.modelo;

public enum TipoNotificacion {
    SALDO_BAJO("Saldo bajo"),
    PROGRAMADA_PROXIMA("Operación programada próxima"),
    PROGRAMADA_EJECUTADA("Operación programada ejecutada"),
    OPERACION_RECHAZADA("Operación rechazada"),
    ASCENSO_NIVEL("Ascenso de nivel"),
    DESCENSO_NIVEL("Descenso de nivel"),
    CANJE_BENEFICIO("Canje de beneficio"),
    REVERSION("Reversión de operación"),
    ALERTA_SEGURIDAD("Alerta de seguridad");

    private final String etiqueta;

    TipoNotificacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
