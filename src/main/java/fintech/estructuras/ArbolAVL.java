package fintech.estructuras;

import java.util.function.BiConsumer;

/**
 * Árbol AVL (árbol binario de búsqueda auto-balanceado) que funciona como
 * "multimapa ordenado": cada clave puede tener varios valores asociados
 * (por ejemplo, varios usuarios con los mismos puntos).
 *
 * Uso en el sistema:
 *  - usuarios ordenados por puntos acumulados (ranking, niveles, rango de puntos);
 *  - transacciones ordenadas por valor (top-K de mayor valor);
 *  - transacciones ordenadas por fecha (monto movilizado en un rango de tiempo).
 *
 * Justificación: a diferencia de un BST simple, el AVL garantiza altura
 * O(log n) aunque las claves lleguen ordenadas (las fechas siempre llegan en
 * orden creciente, lo que degeneraría un BST en una lista).
 *
 * Complejidades: insertar O(log n), eliminar O(log n), buscar O(log n),
 * rango [a,b] O(log n + k), recorrido en orden O(n).
 */
public class ArbolAVL<K extends Comparable<? super K>, V> {

    private final class Nodo {
        K clave;
        ListaEnlazada<V> valores = new ListaEnlazada<>();
        Nodo izq, der;
        int altura = 1;

        Nodo(K clave, V valor) {
            this.clave = clave;
            valores.agregarAlFinal(valor);
        }
    }

    private Nodo raiz;
    private int totalValores;
    private int totalClaves;

    // ------------------------------------------------------------ inserción
    public void insertar(K clave, V valor) {
        raiz = insertar(raiz, clave, valor);
        totalValores++;
    }

    private Nodo insertar(Nodo n, K clave, V valor) {
        if (n == null) {
            totalClaves++;
            return new Nodo(clave, valor);
        }
        int c = clave.compareTo(n.clave);
        if (c < 0) n.izq = insertar(n.izq, clave, valor);
        else if (c > 0) n.der = insertar(n.der, clave, valor);
        else {
            n.valores.agregarAlFinal(valor);
            return n;
        }
        return balancear(n);
    }

    // ------------------------------------------------------------ eliminación
    /** Elimina un valor concreto asociado a la clave. Si la clave queda sin valores, se elimina el nodo. */
    public boolean eliminar(K clave, V valor) {
        Nodo n = buscarNodo(clave);
        if (n == null || !n.valores.eliminar(valor)) return false;
        totalValores--;
        if (n.valores.estaVacia()) {
            raiz = eliminarNodo(raiz, clave);
            totalClaves--;
        }
        return true;
    }

    private Nodo eliminarNodo(Nodo n, K clave) {
        if (n == null) return null;
        int c = clave.compareTo(n.clave);
        if (c < 0) n.izq = eliminarNodo(n.izq, clave);
        else if (c > 0) n.der = eliminarNodo(n.der, clave);
        else {
            if (n.izq == null) return n.der;
            if (n.der == null) return n.izq;
            Nodo sucesor = n.der;
            while (sucesor.izq != null) sucesor = sucesor.izq;
            n.clave = sucesor.clave;
            n.valores = sucesor.valores;
            n.der = eliminarMinimo(n.der);
        }
        return balancear(n);
    }

    private Nodo eliminarMinimo(Nodo n) {
        if (n.izq == null) return n.der;
        n.izq = eliminarMinimo(n.izq);
        return balancear(n);
    }

    // ------------------------------------------------------------ consultas
    private Nodo buscarNodo(K clave) {
        Nodo n = raiz;
        while (n != null) {
            int c = clave.compareTo(n.clave);
            if (c == 0) return n;
            n = c < 0 ? n.izq : n.der;
        }
        return null;
    }

    public ListaEnlazada<V> buscar(K clave) {
        Nodo n = buscarNodo(clave);
        return n == null ? new ListaEnlazada<>() : n.valores.copia();
    }

    /** Valores cuyas claves están en [min, max], en orden ascendente. O(log n + k). */
    public ListaEnlazada<V> rango(K min, K max) {
        ListaEnlazada<V> r = new ListaEnlazada<>();
        rango(raiz, min, max, r);
        return r;
    }

    private void rango(Nodo n, K min, K max, ListaEnlazada<V> r) {
        if (n == null) return;
        int cMin = min.compareTo(n.clave);
        int cMax = max.compareTo(n.clave);
        if (cMin < 0) rango(n.izq, min, max, r);          // puede haber claves válidas a la izquierda
        if (cMin <= 0 && cMax >= 0) for (V v : n.valores) r.agregarAlFinal(v);
        if (cMax > 0) rango(n.der, min, max, r);          // puede haber claves válidas a la derecha
    }

    /** Todos los valores en orden ascendente de clave. */
    public ListaEnlazada<V> enOrden() {
        ListaEnlazada<V> r = new ListaEnlazada<>();
        enOrden(raiz, r);
        return r;
    }

    private void enOrden(Nodo n, ListaEnlazada<V> r) {
        if (n == null) return;
        enOrden(n.izq, r);
        for (V v : n.valores) r.agregarAlFinal(v);
        enOrden(n.der, r);
    }

    /** Recorre en orden ascendente entregando (clave, valor). */
    public void recorrer(BiConsumer<K, V> accion) {
        recorrer(raiz, accion);
    }

    private void recorrer(Nodo n, BiConsumer<K, V> accion) {
        if (n == null) return;
        recorrer(n.izq, accion);
        for (V v : n.valores) accion.accept(n.clave, v);
        recorrer(n.der, accion);
    }

    /** Los k valores con mayor clave (recorrido en orden inverso con corte temprano). O(log n + k). */
    public ListaEnlazada<V> mayores(int k) {
        ListaEnlazada<V> r = new ListaEnlazada<>();
        mayores(raiz, k, r);
        return r;
    }

    private void mayores(Nodo n, int k, ListaEnlazada<V> r) {
        if (n == null || r.tamano() >= k) return;
        mayores(n.der, k, r);
        for (V v : n.valores) {
            if (r.tamano() >= k) return;
            r.agregarAlFinal(v);
        }
        mayores(n.izq, k, r);
    }

    public K claveMinima() {
        if (raiz == null) return null;
        Nodo n = raiz;
        while (n.izq != null) n = n.izq;
        return n.clave;
    }

    public K claveMaxima() {
        if (raiz == null) return null;
        Nodo n = raiz;
        while (n.der != null) n = n.der;
        return n.clave;
    }

    public int tamano() {
        return totalValores;
    }

    public int cantidadClaves() {
        return totalClaves;
    }

    public boolean estaVacio() {
        return raiz == null;
    }

    public int altura() {
        return h(raiz);
    }

    // ------------------------------------------------------------ balanceo
    private int h(Nodo n) {
        return n == null ? 0 : n.altura;
    }

    private void actualizar(Nodo n) {
        n.altura = 1 + Math.max(h(n.izq), h(n.der));
    }

    private int factor(Nodo n) {
        return h(n.izq) - h(n.der);
    }

    private Nodo rotarDerecha(Nodo y) {
        Nodo x = y.izq;
        y.izq = x.der;
        x.der = y;
        actualizar(y);
        actualizar(x);
        return x;
    }

    private Nodo rotarIzquierda(Nodo x) {
        Nodo y = x.der;
        x.der = y.izq;
        y.izq = x;
        actualizar(x);
        actualizar(y);
        return y;
    }

    private Nodo balancear(Nodo n) {
        actualizar(n);
        int f = factor(n);
        if (f > 1) {
            if (factor(n.izq) < 0) n.izq = rotarIzquierda(n.izq); // caso izquierda-derecha
            return rotarDerecha(n);
        }
        if (f < -1) {
            if (factor(n.der) > 0) n.der = rotarDerecha(n.der);   // caso derecha-izquierda
            return rotarIzquierda(n);
        }
        return n;
    }

    /** Verifica la propiedad AVL y de orden (usado en pruebas). */
    public boolean esValido() {
        return valido(raiz, null, null) >= 0;
    }

    private int valido(Nodo n, K min, K max) {
        if (n == null) return 0;
        if (min != null && n.clave.compareTo(min) <= 0) return -1;
        if (max != null && n.clave.compareTo(max) >= 0) return -1;
        int hi = valido(n.izq, min, n.clave);
        int hd = valido(n.der, n.clave, max);
        if (hi < 0 || hd < 0 || Math.abs(hi - hd) > 1) return -1;
        return 1 + Math.max(hi, hd);
    }
}
