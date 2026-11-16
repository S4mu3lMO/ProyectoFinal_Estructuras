package fintech.deteccion;

import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Transaccion;

import java.time.LocalDateTime;

/** Múltiples transferencias consecutivas en un tiempo muy corto. */
public class ReglaRafagaTransferencias implements ReglaDeteccion {

    private final int ventanaMinutos;
    private final int umbral;

    public ReglaRafagaTransferencias(int ventanaMinutos, int umbral) {
        this.ventanaMinutos = ventanaMinutos;
        this.umbral = umbral;
    }

    public String getNombre() {
        return "Ráfaga de transferencias";
    }

    public int getPeso() {
        return 2;
    }

    public String evaluar(Transaccion c, ListaEnlazada<Transaccion> historial) {
        if (!c.getTipo().esTransferencia()) return null;
        LocalDateTime limite = c.getFecha().minusMinutes(ventanaMinutos);
        int conteo = 1;
        // recorrido inverso: solo se visitan las operaciones dentro de la ventana
        for (Transaccion t : historial.inverso()) {
            if (t.getFecha().isBefore(limite)) break;
            if (ReglaDeteccion.ejecutadaPor(t, c.getUsuario()) && t.getTipo().esTransferencia()) conteo++;
        }
        return conteo >= umbral ? conteo + " transferencias en " + ventanaMinutos + " min" : null;
    }
}
