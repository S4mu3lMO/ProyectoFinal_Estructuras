package fintech.estructuras;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Cola de prioridad implementada como montículo binario (heap) sobre un arreglo.
 * El elemento "menor" según el comparador está siempre en la raíz.
 *
 * Uso en el sistema: operaciones programadas ordenadas por fecha/hora de
 * ejecución (y, en empate, por nivel del usuario); también para obtener el
 * top-K en los reportes de analítica.
 *
 * Complejidades: insertar O(log n), extraer O(log n), ver mínimo O(1),
 * eliminar por condición O(n).
 */
public class ColaPrioridad<T> implements Iterable<T> {

    private Object[] heap;
    private int tamano;
    private final Comparator<? super T> comparador;

    public ColaPrioridad(Comparator<? super T> comparador) {
        this.comparador = comparador;
        this.heap = new Object[16];
    }

    public void insertar(T dato) {
        if (tamano == heap.length) heap = Arrays.copyOf(heap, heap.length * 2);
        heap[tamano] = dato;
        subir(tamano);
        tamano++;
    }

    public T verMinimo() {
        if (tamano == 0) throw new NoSuchElementException("Cola de prioridad vacía");
        return elem(0);
    }

    public T extraer() {
        if (tamano == 0) throw new NoSuchElementException("Cola de prioridad vacía");
        T raiz = elem(0);
        tamano--;
        heap[0] = heap[tamano];
        heap[tamano] = null;
        if (tamano > 0) bajar(0);
        return raiz;
    }

    /** Elimina el primer elemento que cumpla la condición. O(n). */
    public T eliminarSi(Predicate<T> condicion) {
        for (int i = 0; i < tamano; i++) {
            T e = elem(i);
            if (condicion.test(e)) {
                tamano--;
                heap[i] = heap[tamano];
                heap[tamano] = null;
                if (i < tamano) {
                    bajar(i);
                    subir(i);
                }
                return e;
            }
        }
        return null;
    }

    public T buscar(Predicate<T> condicion) {
        for (int i = 0; i < tamano; i++) if (condicion.test(elem(i))) return elem(i);
        return null;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public int tamano() {
        return tamano;
    }

    /** Devuelve los elementos en orden de prioridad sin modificar el montículo. O(n log n). */
    public ListaEnlazada<T> enOrden() {
        ColaPrioridad<T> copia = new ColaPrioridad<>(comparador);
        copia.heap = Arrays.copyOf(heap, Math.max(heap.length, 1));
        copia.tamano = tamano;
        ListaEnlazada<T> r = new ListaEnlazada<>();
        while (!copia.estaVacia()) r.agregarAlFinal(copia.extraer());
        return r;
    }

    private void subir(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (comparador.compare(elem(i), elem(padre)) >= 0) break;
            intercambiar(i, padre);
            i = padre;
        }
    }

    private void bajar(int i) {
        while (true) {
            int izq = 2 * i + 1, der = izq + 1, menor = i;
            if (izq < tamano && comparador.compare(elem(izq), elem(menor)) < 0) menor = izq;
            if (der < tamano && comparador.compare(elem(der), elem(menor)) < 0) menor = der;
            if (menor == i) return;
            intercambiar(i, menor);
            i = menor;
        }
    }

    private void intercambiar(int a, int b) {
        Object t = heap[a];
        heap[a] = heap[b];
        heap[b] = t;
    }

    @SuppressWarnings("unchecked")
    private T elem(int i) {
        return (T) heap[i];
    }

    /** Recorrido en orden del arreglo (no en orden de prioridad). */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int i = 0;

            public boolean hasNext() {
                return i < tamano;
            }

            public T next() {
                if (i >= tamano) throw new NoSuchElementException();
                return elem(i++);
            }
        };
    }
}
