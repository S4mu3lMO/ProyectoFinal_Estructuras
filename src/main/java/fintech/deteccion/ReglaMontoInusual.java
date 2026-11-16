package fintech.deteccion;

import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Transaccion;

/** Retiros o envíos por valores muy superiores al promedio histórico del usuario. */
public class ReglaMontoInusual implements ReglaDeteccion {

    private final double factor;
    private final int historialMinimo;

    public ReglaMontoInusual(double factor, int historialMinimo) {
        this.factor = factor;
        this.historialMinimo = historialMinimo;
    }

    public String getNombre() {
        return "Monto inusualmente alto";
    }

    public int getPeso() {
        return 3;
    }

    public String evaluar(Transaccion c, ListaEnlazada<Transaccion> historial) {
        if (!c.getTipo().esSalida()) return null;
        double suma = 0;
        int n = 0;
        for (Transaccion t : historial) {
            if (ReglaDeteccion.ejecutadaPor(t, c.getUsuario()) && t.getTipo().esSalida()) {
                suma += t.getValor();
                n++;
            }
        }
        if (n < historialMinimo) return null;
        double promedio = suma / n;
        if (c.getValor() > promedio * factor) {
            return String.format("Valor $%,.2f supera %.1fx el promedio ($%,.2f)", c.getValor(), factor, promedio);
        }
        return null;
    }
}
