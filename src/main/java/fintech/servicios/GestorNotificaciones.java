package fintech.servicios;

import fintech.estructuras.Cola;
import fintech.modelo.Notificacion;
import fintech.modelo.TipoNotificacion;
import fintech.modelo.Usuario;

/**
 * Genera alertas y las despacha a los usuarios.
 *
 * Estructura: Cola FIFO de notificaciones pendientes. Las alertas se atienden
 * en el orden exacto en que se generaron. Al despacharlas pasan al historial
 * reciente del usuario (lista acotada).
 */
public class GestorNotificaciones {

    private final Cola<Notificacion> colaPendientes = new Cola<>();
    private final GeneradorId ids;
    private final Reloj reloj;
    private boolean despachoAutomatico = true;
    private int totalGeneradas;
    private int totalDespachadas;

    public GestorNotificaciones(GeneradorId ids, Reloj reloj) {
        this.ids = ids;
        this.reloj = reloj;
    }

    public Notificacion notificar(Usuario usuario, TipoNotificacion tipo, String mensaje) {
        if (usuario == null) return null;
        Notificacion n = new Notificacion(ids.siguiente("NOT", 6), reloj.ahora(), tipo, mensaje, usuario);
        colaPendientes.encolar(n);
        totalGeneradas++;
        if (despachoAutomatico) despacharTodas();
        return n;
    }

    /** Despacha la notificación más antigua pendiente. */
    public Notificacion despacharSiguiente() {
        if (colaPendientes.estaVacia()) return null;
        Notificacion n = colaPendientes.desencolar();
        n.marcarDespachada(reloj.ahora());
        n.getUsuario().recibirNotificacion(n);
        totalDespachadas++;
        return n;
    }

    public int despacharTodas() {
        int c = 0;
        while (!colaPendientes.estaVacia()) {
            despacharSiguiente();
            c++;
        }
        return c;
    }

    public Cola<Notificacion> getPendientes() {
        return colaPendientes;
    }

    public boolean isDespachoAutomatico() {
        return despachoAutomatico;
    }

    public void setDespachoAutomatico(boolean v) {
        this.despachoAutomatico = v;
    }

    public int getTotalGeneradas() {
        return totalGeneradas;
    }

    public int getTotalDespachadas() {
        return totalDespachadas;
    }
}
