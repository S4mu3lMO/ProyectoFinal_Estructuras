package fintech.estructuras;

/**
 * Tabla hash con encadenamiento separado y redimensionamiento automático.
 *
 * Uso en el sistema: acceso a usuarios por identificación, billeteras por
 * código, transacciones por id, beneficios por código y contadores de los
 * reportes de analítica.
 *
 * Justificación: estas búsquedas ocurren en cada operación financiera; con
 * una buena función hash y factor de carga ≤ 0.75 el costo esperado es O(1),
 * frente a O(n) de una lista.
 *
 * Complejidades (promedio): poner O(1), obtener O(1), eliminar O(1).
 * Peor caso O(n) si todas las claves colisionan.
 */
public class TablaHash<K, V> {

    public static final class Entrada<K, V> {
        private final K clave;
        private V valor;
        private Entrada<K, V> siguiente;

        Entrada(K clave, V valor, Entrada<K, V> siguiente) {
            this.clave = clave;
            this.valor = valor;
            this.siguiente = siguiente;
        }

        public K getClave() {
            return clave;
        }

        public V getValor() {
            return valor;
        }
    }

    private static final double FACTOR_CARGA_MAX = 0.75;

    private Entrada<K, V>[] cubetas;
    private int tamano;

    public TablaHash() {
        this(16);
    }

    @SuppressWarnings("unchecked")
    public TablaHash(int capacidadInicial) {
        int cap = 1;
        while (cap < Math.max(4, capacidadInicial)) cap <<= 1;
        cubetas = (Entrada<K, V>[]) new Entrada[cap];
    }

    private int indice(Object clave, int longitud) {
        int h = (clave == null) ? 0 : clave.hashCode();
        h ^= (h >>> 16); // dispersar bits altos
        return h & (longitud - 1);
    }

    /** Inserta o reemplaza. Retorna el valor anterior o null. */
    public V poner(K clave, V valor) {
        int i = indice(clave, cubetas.length);
        for (Entrada<K, V> e = cubetas[i]; e != null; e = e.siguiente) {
            if (iguales(e.clave, clave)) {
                V ant = e.valor;
                e.valor = valor;
                return ant;
            }
        }
        cubetas[i] = new Entrada<>(clave, valor, cubetas[i]);
        tamano++;
        if ((double) tamano / cubetas.length > FACTOR_CARGA_MAX) redimensionar();
        return null;
    }

    public V obtener(K clave) {
        int i = indice(clave, cubetas.length);
        for (Entrada<K, V> e = cubetas[i]; e != null; e = e.siguiente) {
            if (iguales(e.clave, clave)) return e.valor;
        }
        return null;
    }

    public V obtenerOPorDefecto(K clave, V porDefecto) {
        V v = obtener(clave);
        return v == null ? porDefecto : v;
    }

    public boolean contieneClave(K clave) {
        int i = indice(clave, cubetas.length);
        for (Entrada<K, V> e = cubetas[i]; e != null; e = e.siguiente) {
            if (iguales(e.clave, clave)) return true;
        }
        return false;
    }

    public V eliminar(K clave) {
        int i = indice(clave, cubetas.length);
        Entrada<K, V> prev = null;
        for (Entrada<K, V> e = cubetas[i]; e != null; prev = e, e = e.siguiente) {
            if (iguales(e.clave, clave)) {
                if (prev == null) cubetas[i] = e.siguiente; else prev.siguiente = e.siguiente;
                tamano--;
                return e.valor;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void redimensionar() {
        Entrada<K, V>[] viejas = cubetas;
        cubetas = (Entrada<K, V>[]) new Entrada[viejas.length * 2];
        for (Entrada<K, V> cab : viejas) {
            for (Entrada<K, V> e = cab; e != null; ) {
                Entrada<K, V> sig = e.siguiente;
                int i = indice(e.clave, cubetas.length);
                e.siguiente = cubetas[i];
                cubetas[i] = e;
                e = sig;
            }
        }
    }

    private static boolean iguales(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public int capacidad() {
        return cubetas.length;
    }

    /** Número de cubetas con más de un elemento (indicador de colisiones). */
    public int cubetasConColision() {
        int c = 0;
        for (Entrada<K, V> e : cubetas) if (e != null && e.siguiente != null) c++;
        return c;
    }

    public ListaEnlazada<K> claves() {
        ListaEnlazada<K> r = new ListaEnlazada<>();
        for (Entrada<K, V> cab : cubetas) for (Entrada<K, V> e = cab; e != null; e = e.siguiente) r.agregarAlFinal(e.clave);
        return r;
    }

    public ListaEnlazada<V> valores() {
        ListaEnlazada<V> r = new ListaEnlazada<>();
        for (Entrada<K, V> cab : cubetas) for (Entrada<K, V> e = cab; e != null; e = e.siguiente) r.agregarAlFinal(e.valor);
        return r;
    }

    public ListaEnlazada<Entrada<K, V>> entradas() {
        ListaEnlazada<Entrada<K, V>> r = new ListaEnlazada<>();
        for (Entrada<K, V> cab : cubetas) for (Entrada<K, V> e = cab; e != null; e = e.siguiente) r.agregarAlFinal(e);
        return r;
    }
}
