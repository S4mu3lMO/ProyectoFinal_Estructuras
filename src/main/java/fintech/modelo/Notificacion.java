package fintech.modelo;

import java.time.LocalDateTime;

/** Alerta o aviso dirigido a un usuario. */
public class Notificacion {

    private final String id;
    private final LocalDateTime fecha;
    private final TipoNotificacion tipo;
    private final String mensaje;
    private final Usuario theUsuario;
    private boolean despachada;
    private LocalDateTime fechaDespacho;

    public Notificacion(String id, LocalDateTime fecha, TipoNotificacion tipo, String mensaje, Usuario usuario) {
        this.id = id;
        this.fecha = fecha;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.theUsuario = usuario;
    }

    public void marcarDespachada(LocalDateTime cuando) {
        despachada = true;
        fechaDespacho = cuando;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Usuario getUsuario() {
        return theUsuario;
    }

    public boolean isDespachada() {
        return despachada;
    }

    public LocalDateTime getFechaDespacho() {
        return fechaDespacho;
    }

    @Override
    public String toString() {
        return "[" + tipo + "] " + mensaje;
    }
}
