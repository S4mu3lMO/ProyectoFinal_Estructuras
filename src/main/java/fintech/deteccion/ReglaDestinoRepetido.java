package fintech.deteccion;

import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Transaccion;

import java.time.LocalDateTime;

/** Movimientos repetitivos hacia un mismo destino en intervalos reducidos. */
public class ReglaDestinoRepetido implements ReglaDeteccion {

    private final int ventanaMinutos;
    private final int umbral;

    public ReglaDestinoRepetido(int ventanaMinutos, int umbral) {
        this.ventanaMinutos = ventanaMinutos;
        this.umbral = umbral;
    }

    public String getNombre() {
        return "Destino repetido";
    }

    public int getPeso() {
        return 2;
    }

    public String evaluar(Transaccion c, ListaEnlazada<Transaccion> historial) {
        if (!c.getTipo().esTransferencia() || c.getBilleteraDestino() == null) return null;
        LocalDateTime limite = c.getFecha().minusMinutes(ventanaMinutos);
        int conteo = 1;
        for (Transaccion t : historial.inverso()) {
            if (t.getFecha().isBefore(limite)) break;
            if (ReglaDeteccion.ejecutadaPor(t, c.getUsuario()) && t.getBilleteraDestino() == c.getBilleteraDestino()) conteo++;
        }
        return conteo >= umbral
                ? conteo + " envíos a " + c.getBilleteraDestino().getCodigo() + " en " + ventanaMinutos + " min"
                : null;
    }
}
