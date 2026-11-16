package fintech.modelo;

import java.time.LocalDateTime;

public enum Frecuencia {
    UNICA("Única"), DIARIA("Diaria"), SEMANAL("Semanal"), MENSUAL("Mensual");

    private final String etiqueta;

    Frecuencia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Fecha de la siguiente ejecución, o null si la operación no se repite. */
    public LocalDateTime siguiente(LocalDateTime fecha) {
        switch (this) {
            case DIARIA: return fecha.plusDays(1);
            case SEMANAL: return fecha.plusWeeks(1);
            case MENSUAL: return fecha.plusMonths(1);
            default: return null;
        }
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
