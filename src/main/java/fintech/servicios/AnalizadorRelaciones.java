package fintech.servicios;

import fintech.estructuras.Grafo;
import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Billetera;
import fintech.modelo.Usuario;

import java.util.Comparator;

/**
 * Análisis de relaciones de transacción usando los grafos de usuarios y de
 * billeteras: rutas frecuentes, ciclos, relaciones recíprocas, caminos entre
 * usuarios y principales emisores/receptores.
 */
public class AnalizadorRelaciones {

    private final MotorTransacciones motor;
    private final GestorUsuarios usuarios;
    private final GestorBilleteras billeteras;

    public AnalizadorRelaciones(MotorTransacciones motor, GestorUsuarios usuarios, GestorBilleteras billeteras) {
        this.motor = motor;
        this.usuarios = usuarios;
        this.billeteras = billeteras;
    }

    public Grafo<String> grafoUsuarios() {
        return motor.getGrafoUsuarios();
    }

    public Grafo<String> grafoBilleteras() {
        return motor.getGrafoBilleteras();
    }

    /** Nombre legible de un vértice del grafo de usuarios. */
    public String nombre(String idUsuario) {
        Usuario u = usuarios.buscar(idUsuario);
        return u == null ? idUsuario + " (eliminado)" : u.getNombre();
    }

    public String nombreBilletera(String codigo) {
        Billetera b = billeteras.buscar(codigo);
        return b == null ? codigo : codigo + " " + b.getNombre() + " (" + b.getUsuario().getNombre() + ")";
    }

    public String describirRuta(ListaEnlazada<String> ruta) {
        StringBuilder sb = new StringBuilder();
        for (String v : ruta) {
            if (sb.length() > 0) sb.append(" → ");
            sb.append(nombre(v));
        }
        return sb.toString();
    }

    public ListaEnlazada<ListaEnlazada<String>> ciclosEntreUsuarios() {
        return grafoUsuarios().detectarCiclos(50);
    }

    public ListaEnlazada<Grafo.Ruta<String>> rutasFrecuentes(int minFrecuencia) {
        return grafoUsuarios().rutasFrecuentes(minFrecuencia);
    }

    public ListaEnlazada<Grafo.Arista<String>> relacionesReciprocas() {
        return grafoUsuarios().relacionesReciprocas();
    }

    public ListaEnlazada<String> rutaEntre(String idOrigen, String idDestino) {
        usuarios.obtener(idOrigen);
        usuarios.obtener(idDestino);
        return grafoUsuarios().rutaMasCorta(idOrigen, idDestino);
    }

    /** Billeteras a las que puede llegar (directa o indirectamente) el dinero que sale de una billetera. */
    public ListaEnlazada<String> billeterasAlcanzables(String codigo) {
        billeteras.obtener(codigo);
        return grafoBilleteras().alcanzables(codigo.trim().toUpperCase());
    }

    /** Aristas más fuertes (más transferencias) del grafo de billeteras. */
    public ListaEnlazada<Grafo.Arista<String>> relacionesBilleteras(int k) {
        ListaEnlazada<Grafo.Arista<String>> r = grafoBilleteras().aristas();
        r.ordenar(Comparator.comparingInt((Grafo.Arista<String> a) -> a.getCantidad()).reversed());
        ListaEnlazada<Grafo.Arista<String>> top = new ListaEnlazada<>();
        for (Grafo.Arista<String> a : r) {
            if (top.tamano() >= k) break;
            top.agregar(a);
        }
        return top;
    }

    /** Patrón de interacción por usuario: grado de salida/entrada y montos enviados/recibidos. */
    public ListaEnlazada<ItemReporte> patronesInteraccion() {
        ListaEnlazada<ItemReporte> r = new ListaEnlazada<>();
        Grafo<String> g = grafoUsuarios();
        for (String v : g.vertices()) {
            double enviado = 0, recibido = 0;
            int nEnv = 0;
            for (Grafo.Arista<String> a : g.aristasSalientes(v)) {
                enviado += a.getMontoTotal();
                nEnv += a.getCantidad();
            }
            for (Grafo.Arista<String> a : g.aristasEntrantes(v)) recibido += a.getMontoTotal();
            String rol = enviado > recibido * 1.5 ? "Emisor neto" : recibido > enviado * 1.5 ? "Receptor neto" : "Equilibrado";
            r.agregar(new ItemReporte(nombre(v), nEnv, enviado,
                    String.format("envía a %d, recibe de %d, recibido $%,.2f · %s", g.gradoSalida(v), g.gradoEntrada(v), recibido, rol)));
        }
        r.ordenar(Comparator.comparingDouble(ItemReporte::getMonto).reversed());
        return r;
    }
}
