package fintech.modelo;

import fintech.estructuras.ListaEnlazada;

/** Contrato para quien puede recibir notificaciones de la plataforma. */
public interface Notificable {

    void recibirNotificacion(Notificacion notificacion);

    ListaEnlazada<Notificacion> notificacionesRecientes();
}
