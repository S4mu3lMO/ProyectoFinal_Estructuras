package fintech.servicios;

import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.TablaHash;
import fintech.modelo.Billetera;
import fintech.modelo.EstadoBilletera;
import fintech.modelo.TipoBilletera;
import fintech.modelo.Usuario;

import java.util.Comparator;

/**
 * Creación y administración de billeteras.
 * Estructura principal: TablaHash código -> Billetera (localización O(1)).
 */
public class GestorBilleteras {

    public static final int MAX_BILLETERAS_POR_USUARIO = 10;

    private final TablaHash<String, Billetera> tablaBilleteras = new TablaHash<>();
    private final GeneradorId ids;
    private final Reloj reloj;

    public GestorBilleteras(GeneradorId ids, Reloj reloj) {
        this.ids = ids;
        this.reloj = reloj;
    }

    public Billetera crear(Usuario usuario, String nombre, TipoBilletera tipo) {
        if (usuario == null) throw new ValidacionException("Usuario inexistente");
        if (nombre == null || nombre.trim().isEmpty()) throw new ValidacionException("La billetera debe tener un nombre");
        if (tipo == null) throw new ValidacionException("Debe indicar el tipo de billetera");
        if (usuario.getBilleteras().tamano() >= MAX_BILLETERAS_POR_USUARIO)
            throw new ValidacionException("Se alcanzó el máximo de " + MAX_BILLETERAS_POR_USUARIO + " billeteras por usuario");
        String n = nombre.trim();
        if (usuario.getBilleteras().buscar(b -> b.getNombre().equalsIgnoreCase(n)) != null)
            throw new ValidacionException("El usuario ya tiene una billetera llamada '" + n + "'");
        Billetera b = new Billetera(ids.siguiente("BIL", 4), n, tipo, usuario, reloj.ahora());
        tablaBilleteras.poner(b.getCodigo(), b);
        usuario.agregarBilletera(b);
        return b;
    }

    public Billetera buscar(String codigo) {
        return codigo == null ? null : tablaBilleteras.obtener(codigo.trim().toUpperCase());
    }

    public Billetera obtener(String codigo) {
        Billetera b = buscar(codigo);
        if (b == null) throw new ValidacionException("No existe la billetera " + codigo);
        return b;
    }

    public void modificar(String codigo, String nombre, TipoBilletera tipo) {
        Billetera b = obtener(codigo);
        if (nombre == null || nombre.trim().isEmpty()) throw new ValidacionException("La billetera debe tener un nombre");
        b.setNombre(nombre.trim());
        if (tipo != null) b.setTipo(tipo);
    }

    public void cambiarEstado(String codigo, EstadoBilletera estado) {
        Billetera b = obtener(codigo);
        if (estado == EstadoBilletera.CERRADA && b.getSaldo() > 0)
            throw new ValidacionException("No se puede cerrar una billetera con saldo");
        b.setEstado(estado);
    }

    public void eliminar(String codigo) {
        Billetera b = obtener(codigo);
        if (b.getSaldo() > 0) throw new ValidacionException("No se puede eliminar una billetera con saldo");
        tablaBilleteras.eliminar(b.getCodigo());
        b.getUsuario().quitarBilletera(b);
    }

    /** Quita del índice todas las billeteras de un usuario (al eliminar el usuario). */
    public void eliminarDeUsuario(Usuario u) {
        for (Billetera b : u.getBilleteras()) tablaBilleteras.eliminar(b.getCodigo());
    }

    public ListaEnlazada<Billetera> todas() {
        ListaEnlazada<Billetera> r = tablaBilleteras.valores();
        r.ordenar(Comparator.comparing(Billetera::getCodigo));
        return r;
    }

    public int cantidad() {
        return tablaBilleteras.tamano();
    }

    public TablaHash<String, Billetera> getTablaBilleteras() {
        return tablaBilleteras;
    }
}
