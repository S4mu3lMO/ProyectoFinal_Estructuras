package fintech.estructuras;

import java.util.Comparator;

/**
 * Grafo dirigido y ponderado representado con listas de adyacencia sobre
 * tablas hash (vértice -> (vecino -> arista)).
 *
 * Uso en el sistema:
 *  - grafo de usuarios: arista A -> B cuando A transfiere a B;
 *  - grafo de billeteras: arista X -> Y cuando sale dinero de X hacia Y.
 * Cada arista acumula la cantidad de transferencias y el monto total, lo que
 * permite detectar rutas frecuentes, ciclos de dinero, relaciones recíprocas
 * y caminos entre usuarios.
 *
 * Justificación: las transferencias son relaciones muchos-a-muchos entre
 * entidades; el grafo es el modelo natural. Usar tablas hash para la
 * adyacencia da acceso O(1) a la arista (A,B) al registrar cada transferencia.
 *
 * Complejidades: agregar/actualizar arista O(1) promedio, BFS/DFS O(V + E).
 */
public class Grafo<V> {

    /** Arista dirigida con peso acumulado. */
    public static final class Arista<V> {
        private final V origen;
        private final V destino;
        private int cantidad;
        private double montoTotal;

        Arista(V origen, V destino) {
            this.origen = origen;
            this.destino = destino;
        }

        public V getOrigen() {
            return origen;
        }

        public V getDestino() {
            return destino;
        }

        public int getCantidad() {
            return cantidad;
        }

        public double getMontoTotal() {
            return montoTotal;
        }

        @Override
        public String toString() {
            return origen + " -> " + destino + " (" + cantidad + " transf., $" + String.format("%,.0f", montoTotal) + ")";
        }
    }

    /** Ruta (secuencia de vértices) con su frecuencia y monto. */
    public static final class Ruta<V> {
        private final ListaEnlazada<V> vertices;
        private final int frecuencia;
        private final double monto;

        public Ruta(ListaEnlazada<V> vertices, int frecuencia, double monto) {
            this.vertices = vertices;
            this.frecuencia = frecuencia;
            this.monto = monto;
        }

        public ListaEnlazada<V> getVertices() {
            return vertices;
        }

        public int getFrecuencia() {
            return frecuencia;
        }

        public double getMonto() {
            return monto;
        }
    }

    private final TablaHash<V, TablaHash<V, Arista<V>>> salientes = new TablaHash<>();
    private final TablaHash<V, TablaHash<V, Arista<V>>> entrantes = new TablaHash<>();
    private int numAristas;

    // ------------------------------------------------------------ construcción
    public void agregarVertice(V v) {
        if (!salientes.contieneClave(v)) {
            salientes.poner(v, new TablaHash<>());
            entrantes.poner(v, new TablaHash<>());
        }
    }

    public boolean contieneVertice(V v) {
        return salientes.contieneClave(v);
    }

    /** Registra una transferencia origen -> destino (crea la arista si no existe). */
    public void registrarArista(V origen, V destino, double monto) {
        agregarVertice(origen);
        agregarVertice(destino);
        TablaHash<V, Arista<V>> ady = salientes.obtener(origen);
        Arista<V> a = ady.obtener(destino);
        if (a == null) {
            a = new Arista<>(origen, destino);
            ady.poner(destino, a);
            entrantes.obtener(destino).poner(origen, a);
            numAristas++;
        }
        a.cantidad++;
        a.montoTotal += monto;
    }

    /** Descuenta una transferencia (por ejemplo, al revertirla). Elimina la arista si queda en cero. */
    public void descontarArista(V origen, V destino, double monto) {
        TablaHash<V, Arista<V>> ady = salientes.obtener(origen);
        if (ady == null) return;
        Arista<V> a = ady.obtener(destino);
        if (a == null) return;
        a.cantidad--;
        a.montoTotal -= monto;
        if (a.cantidad <= 0) {
            ady.eliminar(destino);
            entrantes.obtener(destino).eliminar(origen);
            numAristas--;
        }
    }

    public void eliminarVertice(V v) {
        TablaHash<V, Arista<V>> sal = salientes.obtener(v);
        if (sal == null) return;
        for (V d : sal.claves()) {
            entrantes.obtener(d).eliminar(v);
            numAristas--;
        }
        for (V o : entrantes.obtener(v).claves()) {
            salientes.obtener(o).eliminar(v);
            numAristas--;
        }
        salientes.eliminar(v);
        entrantes.eliminar(v);
    }

    // ------------------------------------------------------------ consultas básicas
    public Arista<V> obtenerArista(V origen, V destino) {
        TablaHash<V, Arista<V>> ady = salientes.obtener(origen);
        return ady == null ? null : ady.obtener(destino);
    }

    public ListaEnlazada<V> vertices() {
        return salientes.claves();
    }

    public ListaEnlazada<Arista<V>> aristasSalientes(V v) {
        TablaHash<V, Arista<V>> ady = salientes.obtener(v);
        return ady == null ? new ListaEnlazada<>() : ady.valores();
    }

    public ListaEnlazada<Arista<V>> aristasEntrantes(V v) {
        TablaHash<V, Arista<V>> ady = entrantes.obtener(v);
        return ady == null ? new ListaEnlazada<>() : ady.valores();
    }

    public ListaEnlazada<Arista<V>> aristas() {
        ListaEnlazada<Arista<V>> r = new ListaEnlazada<>();
        for (TablaHash<V, Arista<V>> ady : salientes.valores()) for (Arista<V> a : ady.valores()) r.agregarAlFinal(a);
        return r;
    }

    public int gradoSalida(V v) {
        TablaHash<V, Arista<V>> ady = salientes.obtener(v);
        return ady == null ? 0 : ady.tamano();
    }

    public int gradoEntrada(V v) {
        TablaHash<V, Arista<V>> ady = entrantes.obtener(v);
        return ady == null ? 0 : ady.tamano();
    }

    public int numVertices() {
        return salientes.tamano();
    }

    public int numAristas() {
        return numAristas;
    }

    // ------------------------------------------------------------ recorridos
    /** Camino con menos saltos entre dos vértices (BFS con una Cola). Lista vacía si no existe. */
    public ListaEnlazada<V> rutaMasCorta(V origen, V destino) {
        ListaEnlazada<V> ruta = new ListaEnlazada<>();
        if (!contieneVertice(origen) || !contieneVertice(destino)) return ruta;
        TablaHash<V, V> padre = new TablaHash<>();
        Cola<V> cola = new Cola<>();
        cola.encolar(origen);
        padre.poner(origen, origen);
        while (!cola.estaVacia()) {
            V u = cola.desencolar();
            if (u.equals(destino)) break;
            for (V w : salientes.obtener(u).claves()) {
                if (!padre.contieneClave(w)) {
                    padre.poner(w, u);
                    cola.encolar(w);
                }
            }
        }
        if (!padre.contieneClave(destino)) return ruta;
        for (V v = destino; ; v = padre.obtener(v)) {
            ruta.agregarAlInicio(v);
            if (v.equals(origen)) break;
        }
        return ruta;
    }

    /** Vértices alcanzables desde v siguiendo el flujo del dinero (DFS con una Pila). */
    public ListaEnlazada<V> alcanzables(V v) {
        ListaEnlazada<V> r = new ListaEnlazada<>();
        if (!contieneVertice(v)) return r;
        TablaHash<V, Boolean> visitado = new TablaHash<>();
        Pila<V> pila = new Pila<>();
        pila.apilar(v);
        visitado.poner(v, true);
        while (!pila.estaVacia()) {
            V u = pila.desapilar();
            if (!u.equals(v)) r.agregarAlFinal(u);
            for (V w : salientes.obtener(u).claves()) {
                if (!visitado.contieneClave(w)) {
                    visitado.poner(w, true);
                    pila.apilar(w);
                }
            }
        }
        return r;
    }

    /**
     * Detecta ciclos dirigidos (el dinero vuelve a su origen) con DFS de tres colores.
     * Cada arista de retroceso produce un ciclo; los ciclos se normalizan para no repetirlos.
     */
    public ListaEnlazada<ListaEnlazada<V>> detectarCiclos(int maximo) {
        ListaEnlazada<ListaEnlazada<V>> ciclos = new ListaEnlazada<>();
        TablaHash<V, Integer> color = new TablaHash<>(); // 0 blanco, 1 gris, 2 negro
        TablaHash<String, Boolean> vistos = new TablaHash<>();
        ListaEnlazada<V> camino = new ListaEnlazada<>();
        for (V v : vertices()) {
            if (color.obtenerOPorDefecto(v, 0) == 0) dfsCiclos(v, color, camino, ciclos, vistos, maximo);
            if (ciclos.tamano() >= maximo) break;
        }
        return ciclos;
    }

    private void dfsCiclos(V u, TablaHash<V, Integer> color, ListaEnlazada<V> camino,
                           ListaEnlazada<ListaEnlazada<V>> ciclos, TablaHash<String, Boolean> vistos, int maximo) {
        color.poner(u, 1);
        camino.agregarAlFinal(u);
        for (V w : salientes.obtener(u).claves()) {
            if (ciclos.tamano() >= maximo) break;
            int c = color.obtenerOPorDefecto(w, 0);
            if (c == 0) {
                dfsCiclos(w, color, camino, ciclos, vistos, maximo);
            } else if (c == 1) {
                // arista de retroceso: el ciclo es el tramo del camino desde w hasta u
                ListaEnlazada<V> ciclo = new ListaEnlazada<>();
                boolean dentro = false;
                for (V x : camino) {
                    if (x.equals(w)) dentro = true;
                    if (dentro) ciclo.agregarAlFinal(x);
                }
                String clave = normalizar(ciclo);
                if (!vistos.contieneClave(clave)) {
                    vistos.poner(clave, true);
                    ciclo.agregarAlFinal(w); // cerrar el ciclo para mostrarlo
                    ciclos.agregarAlFinal(ciclo);
                }
            }
        }
        camino.eliminarUltimo();
        color.poner(u, 2);
    }

    private String normalizar(ListaEnlazada<V> ciclo) {
        // rotación que empieza en el menor toString, para identificar ciclos iguales
        int n = ciclo.tamano(), inicio = 0;
        String menor = null;
        for (int i = 0; i < n; i++) {
            String s = String.valueOf(ciclo.obtener(i));
            if (menor == null || s.compareTo(menor) < 0) {
                menor = s;
                inicio = i;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(ciclo.obtener((inicio + i) % n)).append('|');
        return sb.toString();
    }

    /**
     * Rutas frecuentes de dos saltos (A -> B -> C) cuyas aristas tienen al menos
     * {@code minFrecuencia} transferencias, junto con las aristas directas frecuentes.
     * La frecuencia de una ruta es la mínima de sus aristas (cuello de botella).
     */
    public ListaEnlazada<Ruta<V>> rutasFrecuentes(int minFrecuencia) {
        ListaEnlazada<Ruta<V>> r = new ListaEnlazada<>();
        for (Arista<V> a : aristas()) {
            if (a.cantidad < minFrecuencia) continue;
            ListaEnlazada<V> directa = new ListaEnlazada<>();
            directa.agregarAlFinal(a.origen);
            directa.agregarAlFinal(a.destino);
            r.agregarAlFinal(new Ruta<>(directa, a.cantidad, a.montoTotal));
            for (Arista<V> b : aristasSalientes(a.destino)) {
                if (b.cantidad < minFrecuencia || b.destino.equals(a.origen)) continue;
                ListaEnlazada<V> dos = new ListaEnlazada<>();
                dos.agregarAlFinal(a.origen);
                dos.agregarAlFinal(a.destino);
                dos.agregarAlFinal(b.destino);
                r.agregarAlFinal(new Ruta<>(dos, Math.min(a.cantidad, b.cantidad), Math.min(a.montoTotal, b.montoTotal)));
            }
        }
        r.ordenar(Comparator.comparingInt((Ruta<V> x) -> x.frecuencia).reversed()
                .thenComparing(Comparator.comparingDouble((Ruta<V> x) -> x.monto).reversed()));
        return r;
    }

    /** Pares de vértices con transferencias en ambos sentidos (A -> B y B -> A). */
    public ListaEnlazada<Arista<V>> relacionesReciprocas() {
        ListaEnlazada<Arista<V>> r = new ListaEnlazada<>();
        for (Arista<V> a : aristas()) {
            Arista<V> inversa = obtenerArista(a.destino, a.origen);
            if (inversa != null && String.valueOf(a.origen).compareTo(String.valueOf(a.destino)) < 0) r.agregarAlFinal(a);
        }
        return r;
    }
}
