package fintech.estructuras;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Lista doblemente enlazada genérica.
 *
 * Uso en el sistema: historial de transacciones por usuario y por billetera,
 * beneficios canjeados, notificaciones recientes, auditoría y resultados de
 * consultas por período.
 *
 * Justificación: los historiales crecen por el final (O(1)), se recorren en
 * orden cronológico o inverso (más recientes primero) y no requieren acceso
 * aleatorio. El doble enlace permite recorrer hacia atrás para las reglas de
 * detección de fraude, que solo analizan las últimas operaciones.
 *
 * Complejidades: agregar al inicio/final O(1), eliminar primero/último O(1),
 * búsqueda O(n), acceso por índice O(n), ordenar (merge sort) O(n log n).
 */
public class ListaEnlazada<T> implements Iterable<T> {

    private static final class Nodo<T> {
        T dato;
        Nodo<T> anterior;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    public void agregarAlFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cola == null) {
            cabeza = cola = nuevo;
        } else {
            nuevo.anterior = cola;
            cola.siguiente = nuevo;
            cola = nuevo;
        }
        tamano++;
    }

    /** Alias de agregarAlFinal. */
    public void agregar(T dato) {
        agregarAlFinal(dato);
    }

    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = cola = nuevo;
        } else {
            nuevo.siguiente = cabeza;
            cabeza.anterior = nuevo;
            cabeza = nuevo;
        }
        tamano++;
    }

    public T obtener(int indice) {
        verificarIndice(indice);
        Nodo<T> actual;
        if (indice < tamano / 2) {
            actual = cabeza;
            for (int i = 0; i < indice; i++) actual = actual.siguiente;
        } else {
            actual = cola;
            for (int i = tamano - 1; i > indice; i--) actual = actual.anterior;
        }
        return actual.dato;
    }

    public T primero() {
        if (cabeza == null) throw new NoSuchElementException("Lista vacía");
        return cabeza.dato;
    }

    public T ultimo() {
        if (cola == null) throw new NoSuchElementException("Lista vacía");
        return cola.dato;
    }

    public T eliminarPrimero() {
        if (cabeza == null) throw new NoSuchElementException("Lista vacía");
        Nodo<T> n = cabeza;
        desenlazar(n);
        return n.dato;
    }

    public T eliminarUltimo() {
        if (cola == null) throw new NoSuchElementException("Lista vacía");
        Nodo<T> n = cola;
        desenlazar(n);
        return n.dato;
    }

    /** Elimina la primera aparición del elemento (según equals). */
    public boolean eliminar(T dato) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (n.dato == null ? dato == null : n.dato.equals(dato)) {
                desenlazar(n);
                return true;
            }
        }
        return false;
    }

    /** Elimina todos los elementos que cumplan la condición; retorna cuántos eliminó. */
    public int eliminarSi(Predicate<T> condicion) {
        int eliminados = 0;
        Nodo<T> n = cabeza;
        while (n != null) {
            Nodo<T> sig = n.siguiente;
            if (condicion.test(n.dato)) {
                desenlazar(n);
                eliminados++;
            }
            n = sig;
        }
        return eliminados;
    }

    private void desenlazar(Nodo<T> n) {
        if (n.anterior == null) cabeza = n.siguiente; else n.anterior.siguiente = n.siguiente;
        if (n.siguiente == null) cola = n.anterior; else n.siguiente.anterior = n.anterior;
        n.anterior = n.siguiente = null;
        tamano--;
    }

    public T buscar(Predicate<T> condicion) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (condicion.test(n.dato)) return n.dato;
        }
        return null;
    }

    public boolean contiene(T dato) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (n.dato == null ? dato == null : n.dato.equals(dato)) return true;
        }
        return false;
    }

    public ListaEnlazada<T> filtrar(Predicate<T> condicion) {
        ListaEnlazada<T> r = new ListaEnlazada<>();
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (condicion.test(n.dato)) r.agregarAlFinal(n.dato);
        }
        return r;
    }

    public int contar(Predicate<T> condicion) {
        int c = 0;
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) if (condicion.test(n.dato)) c++;
        return c;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public void limpiar() {
        cabeza = cola = null;
        tamano = 0;
    }

    /** Copia superficial de la lista. */
    public ListaEnlazada<T> copia() {
        ListaEnlazada<T> r = new ListaEnlazada<>();
        for (T t : this) r.agregarAlFinal(t);
        return r;
    }

    /** Ordena la lista en sitio usando merge sort sobre los nodos: O(n log n), estable. */
    public void ordenar(Comparator<? super T> comparador) {
        if (tamano < 2) return;
        cabeza = mergeSort(cabeza, comparador);
        // reconstruir enlaces "anterior" y la cola
        Nodo<T> prev = null;
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            n.anterior = prev;
            prev = n;
        }
        cola = prev;
    }

    private Nodo<T> mergeSort(Nodo<T> inicio, Comparator<? super T> c) {
        if (inicio == null || inicio.siguiente == null) return inicio;
        // dividir con puntero lento / rápido
        Nodo<T> lento = inicio, rapido = inicio.siguiente;
        while (rapido != null && rapido.siguiente != null) {
            lento = lento.siguiente;
            rapido = rapido.siguiente.siguiente;
        }
        Nodo<T> mitad = lento.siguiente;
        lento.siguiente = null;
        return mezclar(mergeSort(inicio, c), mergeSort(mitad, c), c);
    }

    private Nodo<T> mezclar(Nodo<T> a, Nodo<T> b, Comparator<? super T> c) {
        Nodo<T> centinela = new Nodo<>(null);
        Nodo<T> t = centinela;
        while (a != null && b != null) {
            if (c.compare(a.dato, b.dato) <= 0) {
                t.siguiente = a;
                a = a.siguiente;
            } else {
                t.siguiente = b;
                b = b.siguiente;
            }
            t = t.siguiente;
        }
        t.siguiente = (a != null) ? a : b;
        return centinela.siguiente;
    }

    /** Iterador desde el último elemento hacia el primero (más recientes primero). */
    public Iterable<T> inverso() {
        return () -> new Iterator<T>() {
            Nodo<T> actual = cola;

            public boolean hasNext() {
                return actual != null;
            }

            public T next() {
                if (actual == null) throw new NoSuchElementException();
                T d = actual.dato;
                actual = actual.anterior;
                return d;
            }
        };
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Nodo<T> actual = cabeza;

            public boolean hasNext() {
                return actual != null;
            }

            public T next() {
                if (actual == null) throw new NoSuchElementException();
                T d = actual.dato;
                actual = actual.siguiente;
                return d;
            }
        };
    }

    /** Conversión a java.util.List, solo para presentar datos en la interfaz gráfica. */
    public List<T> aListaJava() {
        List<T> r = new ArrayList<>(tamano);
        for (T t : this) r.add(t);
        return r;
    }

    private void verificarIndice(int i) {
        if (i < 0 || i >= tamano) throw new IndexOutOfBoundsException("Índice " + i + ", tamaño " + tamano);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            sb.append(n.dato);
            if (n.siguiente != null) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}
