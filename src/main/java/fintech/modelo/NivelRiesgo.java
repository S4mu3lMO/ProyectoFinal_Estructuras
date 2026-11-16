package fintech.modelo;

/** Nivel de riesgo asignado por el detector de comportamiento inusual. */
public enum NivelRiesgo {
    NINGUNO, BAJO, MEDIO, ALTO, CRITICO;

    /** Convierte el puntaje acumulado por las reglas en un nivel. */
    public static NivelRiesgo desdePuntaje(int puntaje) {
        if (puntaje <= 0) return NINGUNO;
        if (puntaje <= 2) return BAJO;
        if (puntaje <= 4) return MEDIO;
        if (puntaje <= 6) return ALTO;
        return CRITICO;
    }
}
