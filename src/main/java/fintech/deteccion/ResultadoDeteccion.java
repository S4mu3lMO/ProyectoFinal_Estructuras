package fintech.deteccion;

import fintech.estructuras.ListaEnlazada;
import fintech.modelo.NivelRiesgo;

/** Resultado de evaluar todas las reglas sobre una transacción. */
public class ResultadoDeteccion {

    private final ListaEnlazada<String> hallazgos;
    private final int puntaje;
    private final NivelRiesgo nivel;

    public ResultadoDeteccion(ListaEnlazada<String> hallazgos, int puntaje) {
        this.hallazgos = hallazgos;
        this.puntaje = puntaje;
        this.nivel = NivelRiesgo.desdePuntaje(puntaje);
    }

    public ListaEnlazada<String> getHallazgos() {
        return hallazgos;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public NivelRiesgo getNivel() {
        return nivel;
    }

    public boolean hayRiesgo() {
        return puntaje > 0;
    }
}
