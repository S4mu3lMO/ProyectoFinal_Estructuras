package fintech.modelo;

import java.time.LocalDateTime;

/** Registro de un canje de beneficio realizado por un usuario. */
public class BeneficioCanjeado {

    private final String codigoCanje;
    private final Beneficio theBeneficio;
    private final LocalDateTime fecha;
    private final int puntosUsados;

    public BeneficioCanjeado(String codigoCanje, Beneficio beneficio, LocalDateTime fecha, int puntosUsados) {
        this.codigoCanje = codigoCanje;
        this.theBeneficio = beneficio;
        this.fecha = fecha;
        this.puntosUsados = puntosUsados;
    }

    public String getCodigoCanje() {
        return codigoCanje;
    }

    public Beneficio getBeneficio() {
        return theBeneficio;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public int getPuntosUsados() {
        return puntosUsados;
    }
}
