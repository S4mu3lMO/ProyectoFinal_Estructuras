package fintech.modelo;

public enum TipoOperacionProgramada {
    TRANSFERENCIA_PERIODICA("Transferencia periódica"),
    RECARGA_AUTOMATICA("Recarga automática"),
    PAGO_RECURRENTE("Pago recurrente"),
    AHORRO_AUTOMATICO("Ahorro automático");

    private final String etiqueta;

    TipoOperacionProgramada(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** La recarga automática no tiene billetera origen (el dinero entra desde fuera). */
    public boolean requiereOrigen() {
        return this != RECARGA_AUTOMATICA;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
