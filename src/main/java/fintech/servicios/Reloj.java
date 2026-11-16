package fintech.servicios;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Reloj simulado de la plataforma. Permite "adelantar el tiempo" para simular
 * la ejecución automática de operaciones programadas y generar datos de
 * prueba con fechas realistas.
 */
public class Reloj {

    private LocalDateTime actual;

    public Reloj() {
        this(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
    }

    public Reloj(LocalDateTime inicio) {
        this.actual = inicio;
    }

    public LocalDateTime ahora() {
        return actual;
    }

    public void establecer(LocalDateTime fecha) {
        this.actual = fecha;
    }

    public void avanzar(Duration d) {
        actual = actual.plus(d);
    }

    public void avanzarMinutos(long m) {
        actual = actual.plusMinutes(m);
    }

    public void avanzarHoras(long h) {
        actual = actual.plusHours(h);
    }

    public void avanzarDias(long d) {
        actual = actual.plusDays(d);
    }
}
