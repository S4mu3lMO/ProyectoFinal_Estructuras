package fintech.deteccion;

import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.TablaHash;
import fintech.modelo.EventoAuditoria;
import fintech.modelo.NivelRiesgo;
import fintech.modelo.TipoNotificacion;
import fintech.modelo.Transaccion;
import fintech.modelo.Usuario;
import fintech.servicios.GeneradorId;
import fintech.servicios.GestorNotificaciones;
import fintech.servicios.Reloj;

import java.time.LocalDateTime;

/**
 * Módulo de detección de comportamiento financiero inusual.
 *
 * Aplica un conjunto de reglas (lista) a cada transacción antes de
 * ejecutarla, suma sus pesos para obtener un nivel de riesgo y, si hay
 * hallazgos: marca la transacción, genera una alerta de seguridad y registra
 * el evento en el historial especial de auditoría (lista global + tabla hash
 * por usuario para consultas rápidas).
 */
public class DetectorFraude {

    private final ListaEnlazada<ReglaDeteccion> lstRegla = new ListaEnlazada<>();
    private final ListaEnlazada<EventoAuditoria> lstAuditoria = new ListaEnlazada<>();
    private final TablaHash<String, ListaEnlazada<EventoAuditoria>> auditoriaPorUsuario = new TablaHash<>();
    private final GestorNotificaciones notificaciones;
    private final GeneradorId ids;
    private final Reloj reloj;
    private boolean bloquearRiesgoCritico = true;

    public DetectorFraude(GestorNotificaciones notificaciones, GeneradorId ids, Reloj reloj) {
        this.notificaciones = notificaciones;
        this.ids = ids;
        this.reloj = reloj;
        lstRegla.agregar(new ReglaRafagaTransferencias(5, 3));
        lstRegla.agregar(new ReglaMontoInusual(3.0, 5));
        lstRegla.agregar(new ReglaDestinoRepetido(30, 3));
        lstRegla.agregar(new ReglaFragmentacion(15, 3));
        lstRegla.agregar(new ReglaCambioFrecuencia(3.0, 6, 30));
        lstRegla.agregar(new ReglaHorarioInusual(15, 0.03));
    }

    public ResultadoDeteccion evaluar(Transaccion candidata) {
        ListaEnlazada<String> hallazgos = new ListaEnlazada<>();
        int puntaje = 0;
        ListaEnlazada<Transaccion> historial = candidata.getUsuario().getTransacciones();
        for (ReglaDeteccion r : lstRegla) {
            String h = r.evaluar(candidata, historial);
            if (h != null) {
                hallazgos.agregar(r.getNombre() + ": " + h);
                puntaje += r.getPeso();
            }
        }
        return new ResultadoDeteccion(hallazgos, puntaje);
    }

    /** ¿Debe bloquearse la operación por su nivel de riesgo? */
    public boolean debeBloquear(ResultadoDeteccion r) {
        return bloquearRiesgoCritico && r.getNivel() == NivelRiesgo.CRITICO;
    }

    /** Registra el evento en auditoría y emite la alerta de seguridad. */
    public EventoAuditoria registrar(Transaccion t, ResultadoDeteccion r, boolean bloqueada) {
        Usuario u = t.getUsuario();
        EventoAuditoria e = new EventoAuditoria(ids.siguiente("AUD", 5), reloj.ahora(), t, u,
                r.getPuntaje(), r.getNivel(), r.getHallazgos(), bloqueada);
        lstAuditoria.agregarAlFinal(e);
        ListaEnlazada<EventoAuditoria> l = auditoriaPorUsuario.obtener(u.getId());
        if (l == null) {
            l = new ListaEnlazada<>();
            auditoriaPorUsuario.poner(u.getId(), l);
        }
        l.agregarAlFinal(e);
        notificaciones.notificar(u, TipoNotificacion.ALERTA_SEGURIDAD,
                "Riesgo " + r.getNivel() + " en " + t.getId() + (bloqueada ? " (BLOQUEADA)" : "") + ": " + e.resumenHallazgos());
        return e;
    }

    // ------------------------------------------------------------ consultas
    public ListaEnlazada<EventoAuditoria> getAuditoria() {
        return lstAuditoria;
    }

    public ListaEnlazada<EventoAuditoria> auditoriaDeUsuario(String idUsuario) {
        ListaEnlazada<EventoAuditoria> l = auditoriaPorUsuario.obtener(idUsuario);
        return l == null ? new ListaEnlazada<>() : l;
    }

    public ListaEnlazada<EventoAuditoria> porNivelMinimo(NivelRiesgo minimo) {
        return lstAuditoria.filtrar(e -> e.getNivelRiesgo().ordinal() >= minimo.ordinal());
    }

    public ListaEnlazada<EventoAuditoria> enRango(LocalDateTime desde, LocalDateTime hasta) {
        return lstAuditoria.filtrar(e -> !e.getFecha().isBefore(desde) && !e.getFecha().isAfter(hasta));
    }

    public ListaEnlazada<ReglaDeteccion> getReglas() {
        return lstRegla;
    }

    public boolean isBloquearRiesgoCritico() {
        return bloquearRiesgoCritico;
    }

    public void setBloquearRiesgoCritico(boolean v) {
        this.bloquearRiesgoCritico = v;
    }
}
