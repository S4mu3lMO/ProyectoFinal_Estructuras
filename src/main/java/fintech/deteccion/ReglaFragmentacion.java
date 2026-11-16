package fintech.deteccion;

import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.TablaHash;
import fintech.modelo.TipoTransaccion;
import fintech.modelo.Transaccion;

import java.time.LocalDateTime;

/**
 * Uso simultáneo de varias billeteras del mismo usuario para fragmentar
 * montos (salidas desde muchas billeteras distintas en poco tiempo).
 */
public class ReglaFragmentacion implements ReglaDeteccion {

    private final int ventanaMinutos;
    private final int umbralBilleteras;

    public ReglaFragmentacion(int ventanaMinutos, int umbralBilleteras) {
        this.ventanaMinutos = ventanaMinutos;
        this.umbralBilleteras = umbralBilleteras;
    }

    public String getNombre() {
        return "Fragmentación de montos";
    }

    public int getPeso() {
        return 3;
    }

    public String evaluar(Transaccion c, ListaEnlazada<Transaccion> historial) {
        if (!c.getTipo().esSalida() || c.getBilleteraOrigen() == null
                || c.getTipo() == TipoTransaccion.TRANSFERENCIA_PROPIA) return null;
        LocalDateTime limite = c.getFecha().minusMinutes(ventanaMinutos);
        TablaHash<String, Boolean> distintas = new TablaHash<>();
        distintas.poner(c.getBilleteraOrigen().getCodigo(), true);
        double total = c.getValor();
        for (Transaccion t : historial.inverso()) {
            if (t.getFecha().isBefore(limite)) break;
            if (ReglaDeteccion.ejecutadaPor(t, c.getUsuario()) && t.getTipo().esSalida() && t.getBilleteraOrigen() != null
                    && t.getTipo() != TipoTransaccion.TRANSFERENCIA_PROPIA) {
                distintas.poner(t.getBilleteraOrigen().getCodigo(), true);
                total += t.getValor();
            }
        }
        return distintas.tamano() >= umbralBilleteras
                ? String.format("Salidas desde %d billeteras en %d min (total $%,.2f)", distintas.tamano(), ventanaMinutos, total)
                : null;
    }
}
