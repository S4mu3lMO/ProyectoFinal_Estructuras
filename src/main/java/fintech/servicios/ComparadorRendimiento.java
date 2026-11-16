package fintech.servicios;

import fintech.estructuras.ArbolAVL;
import fintech.estructuras.ColaPrioridad;
import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.Pila;
import fintech.estructuras.TablaHash;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Compara el rendimiento de distintas estructuras en operaciones similares
 * (requisito adicional del proyecto). Cada escenario se ejecuta una vez de
 * calentamiento y luego se mide con System.nanoTime().
 */
public class ComparadorRendimiento {

    /** Fila de resultados. */
    public static class Resultado {
        private final String escenario;
        private final String estructura;
        private final int n;
        private final double milisegundos;
        private final String complejidad;
        private final String verificacion;

        public Resultado(String escenario, String estructura, int n, double milisegundos, String complejidad, String verificacion) {
            this.escenario = escenario;
            this.estructura = estructura;
            this.n = n;
            this.milisegundos = milisegundos;
            this.complejidad = complejidad;
            this.verificacion = verificacion;
        }

        public String getEscenario() {
            return escenario;
        }

        public String getEstructura() {
            return estructura;
        }

        public int getN() {
            return n;
        }

        public double getMilisegundos() {
            return milisegundos;
        }

        public String getComplejidad() {
            return complejidad;
        }

        public String getVerificacion() {
            return verificacion;
        }

        @Override
        public String toString() {
            return String.format("%-38s %-28s n=%-7d %10.3f ms  %-14s %s", escenario, estructura, n, milisegundos, complejidad, verificacion);
        }
    }

    private static final class Registro {
        final String id;
        final double valor;

        Registro(String id, double valor) {
            this.id = id;
            this.valor = valor;
        }
    }

    public ListaEnlazada<Resultado> ejecutar(int n, long semilla) {
        if (n < 100 || n > 500_000) throw new ValidacionException("n debe estar entre 100 y 500.000");
        Random rnd = new Random(semilla);
        Registro[] datos = new Registro[n];
        for (int i = 0; i < n; i++) datos[i] = new Registro("ID" + i, Math.round(rnd.nextDouble() * 1_000_000) / 100.0);
        int consultas = Math.min(2_000, n);
        String[] claves = new String[consultas];
        for (int i = 0; i < consultas; i++) claves[i] = "ID" + rnd.nextInt(n);

        ListaEnlazada<Resultado> r = new ListaEnlazada<>();

        // ---------------------------------------------------------------- 1. inserción
        String esc = "Inserción de n registros";
        r.agregar(medir(esc, "ListaEnlazada", n, "O(1) c/u", () -> {
            ListaEnlazada<Registro> l = new ListaEnlazada<>();
            for (Registro d : datos) l.agregarAlFinal(d);
            return l.tamano() + " elementos";
        }));
        r.agregar(medir(esc, "TablaHash", n, "O(1) prom.", () -> {
            TablaHash<String, Registro> t = new TablaHash<>();
            for (Registro d : datos) t.poner(d.id, d);
            return t.tamano() + " elementos";
        }));
        r.agregar(medir(esc, "ArbolAVL (por valor)", n, "O(log n)", () -> {
            ArbolAVL<Double, Registro> a = new ArbolAVL<>();
            for (Registro d : datos) a.insertar(d.valor, d);
            return "altura " + a.altura();
        }));
        r.agregar(medir(esc, "ColaPrioridad (heap)", n, "O(log n)", () -> {
            ColaPrioridad<Registro> h = new ColaPrioridad<>(Comparator.comparingDouble(x -> x.valor));
            for (Registro d : datos) h.insertar(d);
            return h.tamano() + " elementos";
        }));

        // estructuras ya construidas para las consultas
        ListaEnlazada<Registro> lista = new ListaEnlazada<>();
        TablaHash<String, Registro> tabla = new TablaHash<>();
        ArbolAVL<String, Registro> arbolId = new ArbolAVL<>();
        ArbolAVL<Double, Registro> arbolValor = new ArbolAVL<>();
        HashMap<String, Registro> hashJava = new HashMap<>();
        for (Registro d : datos) {
            lista.agregarAlFinal(d);
            tabla.poner(d.id, d);
            arbolId.insertar(d.id, d);
            arbolValor.insertar(d.valor, d);
            hashJava.put(d.id, d);
        }

        // ---------------------------------------------------------------- 2. búsqueda por clave
        esc = "Búsqueda por id (" + consultas + " consultas)";
        r.agregar(medir(esc, "ListaEnlazada (lineal)", n, "O(n)", () -> {
            int hits = 0;
            for (String k : claves) if (lista.buscar(x -> x.id.equals(k)) != null) hits++;
            return hits + " encontrados";
        }));
        r.agregar(medir(esc, "TablaHash propia", n, "O(1) prom.", () -> {
            int hits = 0;
            for (String k : claves) if (tabla.obtener(k) != null) hits++;
            return hits + " encontrados";
        }));
        r.agregar(medir(esc, "ArbolAVL (por id)", n, "O(log n)", () -> {
            int hits = 0;
            for (String k : claves) if (!arbolId.buscar(k).estaVacia()) hits++;
            return hits + " encontrados";
        }));
        r.agregar(medir(esc, "java.util.HashMap (referencia)", n, "O(1) prom.", () -> {
            int hits = 0;
            for (String k : claves) if (hashJava.get(k) != null) hits++;
            return hits + " encontrados";
        }));

        // ---------------------------------------------------------------- 3. top-10 de mayor valor
        esc = "Top-10 transacciones de mayor valor";
        r.agregar(medir(esc, "ListaEnlazada + merge sort", n, "O(n log n)", () -> {
            ListaEnlazada<Registro> c = lista.copia();
            c.ordenar(Comparator.comparingDouble((Registro x) -> x.valor).reversed());
            return String.format("máx $%,.2f", c.primero().valor);
        }));
        r.agregar(medir(esc, "ColaPrioridad (heap de 10)", n, "O(n log k)", () -> {
            ColaPrioridad<Registro> h = new ColaPrioridad<>(Comparator.comparingDouble(x -> x.valor));
            for (Registro d : lista) {
                if (h.tamano() < 10) h.insertar(d);
                else if (d.valor > h.verMinimo().valor) {
                    h.extraer();
                    h.insertar(d);
                }
            }
            double max = 0;
            while (!h.estaVacia()) max = h.extraer().valor;
            return String.format("máx $%,.2f", max);
        }));
        r.agregar(medir(esc, "ArbolAVL (ya construido)", n, "O(log n + k)", () ->
                String.format("máx $%,.2f", arbolValor.mayores(10).primero().valor)));

        // ---------------------------------------------------------------- 4. consulta por rango
        esc = "Consulta por rango (≈5% de los datos)";
        double a = 4_000, b = 4_500;
        r.agregar(medir(esc, "ListaEnlazada (filtrar)", n, "O(n)", () ->
                lista.filtrar(x -> x.valor >= a && x.valor <= b).tamano() + " en rango"));
        r.agregar(medir(esc, "ArbolAVL (rango)", n, "O(log n + k)", () ->
                arbolValor.rango(a, b).tamano() + " en rango"));

        // ---------------------------------------------------------------- 5. siguiente operación programada
        int extracciones = Math.min(1_000, n);
        esc = "Extraer la más próxima (" + extracciones + " veces)";
        r.agregar(medir(esc, "ListaEnlazada (buscar mínimo)", n, "O(n) c/u", () -> {
            ListaEnlazada<Registro> c = lista.copia();
            double ultimo = 0;
            for (int i = 0; i < extracciones; i++) {
                Registro min = null;
                for (Registro x : c) if (min == null || x.valor < min.valor) min = x;
                c.eliminar(min);
                ultimo = min.valor;
            }
            return String.format("último $%,.2f", ultimo);
        }));
        r.agregar(medir(esc, "ColaPrioridad (heap)", n, "O(log n) c/u", () -> {
            ColaPrioridad<Registro> h = new ColaPrioridad<>(Comparator.comparingDouble(x -> x.valor));
            for (Registro d : datos) h.insertar(d);
            double ultimo = 0;
            for (int i = 0; i < extracciones; i++) ultimo = h.extraer().valor;
            return String.format("último $%,.2f (incluye construir)", ultimo);
        }));

        // ---------------------------------------------------------------- 6. deshacer
        esc = "Apilar y deshacer n operaciones";
        r.agregar(medir(esc, "Pila", n, "O(1) c/u", () -> {
            Pila<Registro> p = new Pila<>();
            for (Registro d : datos) p.apilar(d);
            int c = 0;
            while (!p.estaVacia()) {
                p.desapilar();
                c++;
            }
            return c + " deshechas";
        }));
        r.agregar(medir(esc, "ListaEnlazada (quitar del inicio)", n, "O(1) c/u", () -> {
            ListaEnlazada<Registro> l = new ListaEnlazada<>();
            for (Registro d : datos) l.agregarAlInicio(d);
            int c = 0;
            while (!l.estaVacia()) {
                l.eliminarPrimero();
                c++;
            }
            return c + " deshechas";
        }));
        return r;
    }

    private Resultado medir(String escenario, String estructura, int n, String complejidad, Supplier<String> tarea) {
        tarea.get(); // calentamiento (JIT)
        long ini = System.nanoTime();
        String verif = tarea.get();
        double ms = (System.nanoTime() - ini) / 1_000_000.0;
        return new Resultado(escenario, estructura, n, ms, complejidad, verif);
    }
}
