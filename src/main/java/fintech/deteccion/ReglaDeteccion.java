package fintech.deteccion;

import fintech.estructuras.ListaEnlazada;
import fintech.modelo.EstadoTransaccion;
import fintech.modelo.TipoTransaccion;
import fintech.modelo.Transaccion;
import fintech.modelo.Usuario;

/**
 * Regla de detección de comportamiento financiero inusual (patrón Estrategia).
 * Cada regla analiza la transacción candidata contra el historial del usuario.
 */
public interface ReglaDeteccion {

    String getNombre();

    /** Puntos de riesgo que aporta la regla cuando se activa. */
    int getPeso();

    /**
     * @param candidata transacción que se va a ejecutar (aún no está en el historial)
     * @param historial historial del usuario que origina la operación
     * @return descripción del hallazgo, o null si la regla no se activa
     */
    String evaluar(Transaccion candidata, ListaEnlazada<Transaccion> historial);

    /** Operación ejecutada (no rechazada, no reversión) originada por el usuario. */
    static boolean ejecutadaPor(Transaccion t, Usuario u) {
        return t.getUsuario() == u && t.getTipo() != TipoTransaccion.REVERSION && t.getEstado() != EstadoTransaccion.RECHAZADA;
    }
}
