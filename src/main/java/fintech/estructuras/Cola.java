package fintech.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Cola (FIFO) enlazada con referencias a frente y final.
 *
 * Uso en el sistema: notificaciones pendientes de despacho. Las alertas se
 * atienden en el mismo orden en que se generaron (equidad, sin inanición).
 *
 * Complejidades: encolar O(1), desencolar O(1), frente O(1).
 */
public class Cola<T> implements Iterable<T> {

    private static final class Nodo<T> {
        final T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> frente;
    private Nodo<T> fin;
    private int tamano;

    public void encolar(T dato) {
        Nodo<T> n = new Nodo<>(dato);
        if (fin == null) {
            frente = fin = n;
        } else {
            fin.siguiente = n;
            fin = n;
        }
        tamano++;
    }

    public T desencolar() {
        if (frente == null) throw new NoSuchElementException("Cola vacía");
        T d = frente.dato;
        frente = frente.siguiente;
        if (frente == null) fin = null;
        tamano--;
        return d;
    }

    public T frente() {
        if (frente == null) throw new NoSuchElementException("Cola vacía");
        return frente.dato;
    }

    public boolean estaVacia() {
        return frente == null;
    }

    public int tamano() {
        return tamano;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Nodo<T> actual = frente;

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
}
