package fintech.deteccion;

import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Transaccion;

/**
 * Actividad en horarios no habituales: el usuario nunca (o casi nunca) opera
 * en esa franja horaria según su historial.
 */
public class ReglaHorarioInusual implements ReglaDeteccion {

    private final int historialMinimo;
    private final double proporcionMinima;

    public ReglaHorarioInusual(int historialMinimo, double proporcionMinima) {
        this.historialMinimo = historialMinimo;
        this.proporcionMinima = proporcionMinima;
    }

    public String getNombre() {
        return "Horario no habitual";
    }

    public int getPeso() {
        return 1;
    }

    public String evaluar(Transaccion c, ListaEnlazada<Transaccion> historial) {
        int hora = c.getFecha().getHour();
        int total = 0, enFranja = 0;
        for (Transaccion t : historial) {
            if (!ReglaDeteccion.ejecutadaPor(t, c.getUsuario())) continue;
            total++;
            int d = Math.abs(t.getFecha().getHour() - hora);
            if (Math.min(d, 24 - d) <= 2) enFranja++; // franja de ±2 horas (circular)
        }
        if (total < historialMinimo) return null;
        double proporcion = (double) enFranja / total;
        return proporcion < proporcionMinima
                ? String.format("Operación a las %02d:%02d; solo %.0f%% del historial en esa franja",
                hora, c.getFecha().getMinute(), proporcion * 100)
                : null;
    }
}
