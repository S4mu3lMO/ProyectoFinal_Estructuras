package fintech.modelo;

import fintech.estructuras.ListaEnlazada;

import java.time.LocalDateTime;

/** Registro del historial especial de auditoría: un patrón inusual detectado. */
public class EventoAuditoria {

    private final String id;
    private final LocalDateTime fecha;
    private final Transaccion theTransaccion;
    private final Usuario theUsuario;
    private final int puntaje;
    private final NivelRiesgo nivelRiesgo;
    private final ListaEnlazada<String> lstHallazgo;
    private final boolean bloqueada;

    public EventoAuditoria(String id, LocalDateTime fecha, Transaccion transaccion, Usuario usuario,
                           int puntaje, NivelRiesgo nivelRiesgo, ListaEnlazada<String> hallazgos, boolean bloqueada) {
        this.id = id;
        this.fecha = fecha;
        this.theTransaccion = transaccion;
        this.theUsuario = usuario;
        this.puntaje = puntaje;
        this.nivelRiesgo = nivelRiesgo;
        this.lstHallazgo = hallazgos;
        this.bloqueada = bloqueada;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public Transaccion getTransaccion() {
        return theTransaccion;
    }

    public Usuario getUsuario() {
        return theUsuario;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public NivelRiesgo getNivelRiesgo() {
        return nivelRiesgo;
    }

    public ListaEnlazada<String> getHallazgos() {
        return lstHallazgo;
    }

    public boolean isBloqueada() {
        return bloqueada;
    }

    public String resumenHallazgos() {
        StringBuilder sb = new StringBuilder();
        for (String h : lstHallazgo) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(h);
        }
        return sb.toString();
    }
}
