package fintech.deteccion;

import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Transaccion;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Cambio brusco en la frecuencia de transacciones: las operaciones de las
 * últimas 24 horas superan varias veces el promedio diario histórico.
 */
public class ReglaCambioFrecuencia implements ReglaDeteccion {

    private final double factor;
    private final int minimoUltimoDia;
    private final int diasHistoricos;

    public ReglaCambioFrecuencia(double factor, int minimoUltimoDia, int diasHistoricos) {
        this.factor = factor;
        this.minimoUltimoDia = minimoUltimoDia;
        this.diasHistoricos = diasHistoricos;
    }

    public String getNombre() {
        return "Cambio brusco de frecuencia";
    }

    public int getPeso() {
        return 2;
    }

    public String evaluar(Transaccion c, ListaEnlazada<Transaccion> historial) {
        LocalDateTime hace24h = c.getFecha().minusHours(24);
        LocalDateTime inicioHist = c.getFecha().minusDays(diasHistoricos);
        int ultimoDia = 1, previas = 0;
        LocalDateTime masAntigua = null;
        for (Transaccion t : historial.inverso()) {
            if (t.getFecha().isBefore(inicioHist)) break;
            if (!ReglaDeteccion.ejecutadaPor(t, c.getUsuario())) continue;
            if (!t.getFecha().isBefore(hace24h)) ultimoDia++;
            else {
                previas++;
                masAntigua = t.getFecha();
            }
        }
        if (masAntigua == null || ultimoDia < minimoUltimoDia) return null;
        double dias = Math.max(1.0, Duration.between(masAntigua, hace24h).toHours() / 24.0);
        if (dias < 3) return null; // historial insuficiente para comparar
        double promedioDiario = previas / dias;
        if (ultimoDia > factor * Math.max(promedioDiario, 0.5)) {
            return String.format("%d operaciones en 24 h vs. promedio de %.1f/día", ultimoDia, promedioDiario);
        }
        return null;
    }
}
