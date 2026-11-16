package fintech.servicios;

import fintech.estructuras.ArbolAVL;
import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.TablaHash;
import fintech.modelo.NivelUsuario;
import fintech.modelo.Usuario;

import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * Registro, modificación, búsqueda y eliminación de usuarios.
 *
 * Estructuras:
 *  - TablaHash id -> Usuario: búsqueda por identificación en O(1).
 *  - ArbolAVL puntos -> Usuario: ranking, niveles y búsqueda por rango de puntos.
 */
public class GestorUsuarios {

    private static final Pattern PATRON_ID = Pattern.compile("[0-9A-Za-z]{4,15}");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PATRON_TEL = Pattern.compile("[0-9+ ]{7,15}");

    private final TablaHash<String, Usuario> tablaUsuarios = new TablaHash<>();
    private final ArbolAVL<Integer, Usuario> arbolPuntos = new ArbolAVL<>();
    private final Reloj reloj;

    public GestorUsuarios(Reloj reloj) {
        this.reloj = reloj;
    }

    public Usuario registrar(String id, String nombre, String correo, String telefono) {
        id = limpiar(id);
        validarDatos(id, nombre, correo, telefono);
        if (tablaUsuarios.contieneClave(id)) throw new ValidacionException("Ya existe un usuario con identificación " + id);
        Usuario u = new Usuario(id, nombre.trim(), correo.trim(), telefono.trim(), reloj.ahora());
        tablaUsuarios.poner(id, u);
        arbolPuntos.insertar(0, u);
        return u;
    }

    public Usuario modificar(String id, String nombre, String correo, String telefono) {
        Usuario u = obtener(id);
        validarDatos(u.getId(), nombre, correo, telefono);
        u.actualizarDatos(nombre.trim(), correo.trim(), telefono.trim());
        return u;
    }

    /** Busca por identificación; retorna null si no existe. O(1) promedio. */
    public Usuario buscar(String id) {
        return tablaUsuarios.obtener(limpiar(id));
    }

    /** Igual que buscar, pero lanza excepción si no existe. */
    public Usuario obtener(String id) {
        Usuario u = buscar(id);
        if (u == null) throw new ValidacionException("No existe un usuario con identificación " + limpiar(id));
        return u;
    }

    public ListaEnlazada<Usuario> buscarPorNombre(String texto) {
        String t = texto == null ? "" : texto.trim().toLowerCase();
        ListaEnlazada<Usuario> r = tablaUsuarios.valores().filtrar(u -> u.getNombre().toLowerCase().contains(t));
        r.ordenar(Comparator.comparing(Usuario::getNombre));
        return r;
    }

    /**
     * Elimina un usuario. Solo se permite si su saldo total es cero
     * (consistencia: no se puede "perder" dinero de la plataforma).
     */
    public Usuario eliminar(String id) {
        Usuario u = obtener(id);
        if (u.saldoTotal() > 0) {
            throw new ValidacionException(String.format("El usuario tiene saldo ($%,.2f). Debe retirarlo antes de eliminarlo.", u.saldoTotal()));
        }
        tablaUsuarios.eliminar(u.getId());
        arbolPuntos.eliminar(u.getPuntosAcumulados(), u);
        return u;
    }

    /** Reubica al usuario en el árbol después de un cambio de puntos. O(log n). */
    public void actualizarPosicion(Usuario u, int puntosAnteriores) {
        if (puntosAnteriores == u.getPuntosAcumulados()) return;
        if (arbolPuntos.eliminar(puntosAnteriores, u)) arbolPuntos.insertar(u.getPuntosAcumulados(), u);
    }

    public ListaEnlazada<Usuario> todos() {
        ListaEnlazada<Usuario> r = tablaUsuarios.valores();
        r.ordenar(Comparator.comparing(Usuario::getNombre));
        return r;
    }

    /** Usuarios con puntos en [min, max] en orden ascendente. O(log n + k). */
    public ListaEnlazada<Usuario> porRangoDePuntos(int min, int max) {
        if (min > max) throw new ValidacionException("El mínimo no puede ser mayor que el máximo");
        return arbolPuntos.rango(min, max);
    }

    public ListaEnlazada<Usuario> porNivel(NivelUsuario nivel) {
        return arbolPuntos.rango(nivel.getPuntosMin(), nivel.getPuntosMax());
    }

    /** Los n usuarios con más puntos (recorrido inverso del AVL). */
    public ListaEnlazada<Usuario> ranking(int n) {
        return arbolPuntos.mayores(n);
    }

    public ArbolAVL<Integer, Usuario> getArbolPuntos() {
        return arbolPuntos;
    }

    public TablaHash<String, Usuario> getTablaUsuarios() {
        return tablaUsuarios;
    }

    public int cantidad() {
        return tablaUsuarios.tamano();
    }

    private void validarDatos(String id, String nombre, String correo, String telefono) {
        if (id == null || !PATRON_ID.matcher(id).matches())
            throw new ValidacionException("La identificación debe tener entre 4 y 15 caracteres alfanuméricos");
        if (nombre == null || nombre.trim().length() < 3)
            throw new ValidacionException("El nombre debe tener al menos 3 caracteres");
        if (correo == null || !PATRON_CORREO.matcher(correo.trim()).matches())
            throw new ValidacionException("Correo electrónico inválido");
        if (telefono == null || !PATRON_TEL.matcher(telefono.trim()).matches())
            throw new ValidacionException("Teléfono inválido (7 a 15 dígitos)");
    }

    private static String limpiar(String id) {
        return id == null ? "" : id.trim();
    }
}
