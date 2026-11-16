package fintech.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Pila (LIFO) enlazada.
 *
 * Uso en el sistema: cada usuario tiene una pila de operaciones reversibles
 * (historial de "deshacer"). La operación más reciente queda en la cima, que es
 * justamente la primera candidata a revertirse. También se usa una pila auxiliar
 * para revertir una operación específica sin perder el orden de las demás.
 *
 * Complejidades: apilar O(1), desapilar O(1), cima O(1).
 */
public class Pila<T> implements Iterable<T> {

    private static final class Nodo<T> {
        final T dato;
        final Nodo<T> debajo;

        Nodo(T dato, Nodo<T> debajo) {
            this.dato = dato;
            this.debajo = debajo;
        }
    }

    private Nodo<T> cima;
    private int tamano;

    public void apilar(T dato) {
        cima = new Nodo<>(dato, cima);
        tamano++;
    }

    public T desapilar() {
        if (cima == null) throw new NoSuchElementException("Pila vacía");
        T d = cima.dato;
        cima = cima.debajo;
        tamano--;
        return d;
    }

    public T cima() {
        if (cima == null) throw new NoSuchElementException("Pila vacía");
        return cima.dato;
    }

    public boolean estaVacia() {
        return cima == null;
    }

    public int tamano() {
        return tamano;
    }

    /** Recorre desde la cima hacia el fondo. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Nodo<T> actual = cima;

            public boolean hasNext() {
                return actual != null;
            }

            public T next() {
                if (actual == null) throw new NoSuchElementException();
                T d = actual.dato;
                actual = actual.debajo;
                return d;
            }
        };
    }
}
