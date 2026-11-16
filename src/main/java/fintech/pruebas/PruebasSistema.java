package fintech.pruebas;

import fintech.deteccion.ReglaHorarioInusual;
import fintech.estructuras.ArbolAVL;
import fintech.estructuras.Cola;
import fintech.estructuras.ColaPrioridad;
import fintech.estructuras.Grafo;
import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.Pila;
import fintech.estructuras.TablaHash;
import fintech.modelo.*;
import fintech.servicios.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;

/**
 * Casos de prueba del sistema (sin dependencias externas).
 * Ejecutar: java -cp out fintech.pruebas.PruebasSistema
 */
public class PruebasSistema {

    private static int aprobadas, fallidas;
    private static final StringBuilder reporte = new StringBuilder();

    // ================================================================ mini framework
    private interface Prueba {
        void ejecutar() throws Exception;
    }

    private static void caso(String id, String descripcion, Prueba p) {
        try {
            p.ejecutar();
            aprobadas++;
            linea(String.format("[OK]    %-4s %s", id, descripcion));
        } catch (Throwable e) {
            fallidas++;
            linea(String.format("[FALLA] %-4s %s%n        -> %s", id, descripcion, e));
        }
    }

    private static void linea(String s) {
        System.out.println(s);
        reporte.append(s).append('\n');
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static void igual(Object esperado, Object real, String mensaje) {
        if (!Objects.equals(esperado, real)) throw new AssertionError(mensaje + " (esperado " + esperado + ", obtenido " + real + ")");
    }

    private static void igualD(double esperado, double real, String mensaje) {
        if (Math.abs(esperado - real) > 0.005) throw new AssertionError(mensaje + " (esperado " + esperado + ", obtenido " + real + ")");
    }

    private static String lanza(Runnable r) {
        try {
            r.run();
        } catch (ValidacionException e) {
            return e.getMessage();
        }
        throw new AssertionError("Se esperaba ValidacionException");
    }

    // ================================================================ escenario base
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 3, 2, 9, 0);

    private static class Esc {
        final PlataformaFintech p = new PlataformaFintech(new Reloj(T0));
        final Usuario ana, beto;
        final Billetera anaG, anaA, anaC, betoG;

        Esc() {
            ana = p.getUsuarios().registrar("1001", "Ana Prueba", "ana@test.co", "3001234567");
            beto = p.getUsuarios().registrar("2002", "Beto Prueba", "beto@test.co", "3007654321");
            anaG = p.getBilleteras().crear(ana, "Gastos", TipoBilletera.GASTOS_DIARIOS);
            anaA = p.getBilleteras().crear(ana, "Ahorro", TipoBilletera.AHORRO);
            anaC = p.getBilleteras().crear(ana, "Compras", TipoBilletera.COMPRAS);
            betoG = p.getBilleteras().crear(beto, "Principal", TipoBilletera.GASTOS_DIARIOS);
        }

        MotorTransacciones m() {
            return p.getMotor();
        }

        void min(int minutos) {
            p.getReloj().avanzarMinutos(minutos);
        }
    }

    // ================================================================ pruebas
    public static void main(String[] args) throws Exception {
        linea("==== PRUEBAS DE ESTRUCTURAS DE DATOS ====");

        caso("E01", "ListaEnlazada: inserción, eliminación, recorrido inverso y merge sort estable", () -> {
            ListaEnlazada<Integer> l = new ListaEnlazada<>();
            for (int i = 0; i < 10; i++) l.agregarAlFinal(i);
            l.agregarAlInicio(-1);
            igual(11, l.tamano(), "tamaño");
            igual(-1, l.primero(), "primero");
            igual(9, l.eliminarUltimo(), "eliminarUltimo");
            verificar(l.eliminar(5) && !l.contiene(5), "eliminar(5)");
            StringBuilder inv = new StringBuilder();
            for (int x : l.inverso()) inv.append(x);
            igual("87643210-1", inv.toString(), "recorrido inverso");
            ListaEnlazada<String> s = new ListaEnlazada<>();
            for (String x : new String[]{"b2", "a1", "b1", "a2", "c1"}) s.agregar(x);
            s.ordenar(Comparator.comparing(x -> x.charAt(0)));
            igual("[a1, a2, b2, b1, c1]", s.toString(), "orden estable por letra");
            igual("c1", s.ultimo(), "cola tras ordenar");
        });

        caso("E02", "Pila: orden LIFO", () -> {
            Pila<String> p = new Pila<>();
            p.apilar("A");
            p.apilar("B");
            p.apilar("C");
            igual("C", p.cima(), "cima");
            igual("C", p.desapilar(), "desapilar 1");
            igual("B", p.desapilar(), "desapilar 2");
            igual(1, p.tamano(), "tamaño");
        });

        caso("E03", "Cola: orden FIFO", () -> {
            Cola<Integer> c = new Cola<>();
            for (int i = 1; i <= 5; i++) c.encolar(i);
            igual(1, c.desencolar(), "primero en salir");
            igual(2, c.frente(), "frente");
            igual(4, c.tamano(), "tamaño");
        });

        caso("E04", "ColaPrioridad: 5.000 valores aleatorios salen ordenados; eliminarSi conserva el montículo", () -> {
            ColaPrioridad<Integer> h = new ColaPrioridad<>(Integer::compare);
            Random r = new Random(1);
            for (int i = 0; i < 5000; i++) h.insertar(r.nextInt(100_000));
            h.insertar(-7);
            verificar(h.eliminarSi(x -> x == -7) == -7, "eliminarSi");
            int prev = Integer.MIN_VALUE, n = 0;
            while (!h.estaVacia()) {
                int x = h.extraer();
                verificar(x >= prev, "orden del heap");
                prev = x;
                n++;
            }
            igual(5000, n, "cantidad extraída");
        });

        caso("E05", "TablaHash: 20.000 claves, redimensionamiento, reemplazo y eliminación", () -> {
            TablaHash<String, Integer> t = new TablaHash<>();
            for (int i = 0; i < 20_000; i++) t.poner("K" + i, i);
            igual(20_000, t.tamano(), "tamaño");
            verificar(t.capacidad() >= 20_000 / 0.75, "redimensionó");
            igual(12345, t.obtener("K12345"), "obtener");
            t.poner("K1", 99);
            igual(99, t.obtener("K1"), "reemplazo");
            igual(20_000, t.tamano(), "reemplazo no cambia tamaño");
            igual(500, t.eliminar("K500"), "eliminar");
            verificar(!t.contieneClave("K500"), "ya no existe");
        });

        caso("E06", "ArbolAVL: claves insertadas en orden quedan balanceadas; rango, top-k y eliminación", () -> {
            ArbolAVL<Integer, String> a = new ArbolAVL<>();
            int n = 10_000;
            for (int i = 0; i < n; i++) a.insertar(i, "v" + i);
            a.insertar(50, "dup");
            verificar(a.esValido(), "propiedad AVL");
            verificar(a.altura() <= 1.45 * (Math.log(n) / Math.log(2)), "altura O(log n): " + a.altura());
            igual(11, a.rango(45, 54).tamano(), "rango con duplicado");
            igual("v9999", a.mayores(3).primero(), "mayor");
            verificar(a.eliminar(50, "dup") && a.eliminar(50, "v50"), "eliminar valores");
            verificar(a.buscar(50).estaVacia() && a.esValido(), "nodo eliminado y árbol válido");
            for (int i = 0; i < n; i += 2) a.eliminar(i, "v" + i);
            verificar(a.esValido(), "válido tras eliminar la mitad");
        });

        caso("E07", "Grafo: BFS, ciclos, recíprocas y descuento de aristas", () -> {
            Grafo<String> g = new Grafo<>();
            g.registrarArista("A", "B", 10);
            g.registrarArista("B", "C", 10);
            g.registrarArista("C", "A", 10);
            g.registrarArista("C", "D", 5);
            g.registrarArista("D", "C", 5);
            igual("[A, B, C, D]", g.rutaMasCorta("A", "D").toString(), "ruta BFS");
            ListaEnlazada<ListaEnlazada<String>> ciclos = g.detectarCiclos(10);
            verificar(ciclos.tamano() >= 2, "ciclos A-B-C y C-D: " + ciclos);
            igual(1, g.relacionesReciprocas().tamano(), "una relación recíproca");
            g.descontarArista("D", "C", 5);
            verificar(g.obtenerArista("D", "C") == null, "arista eliminada al quedar en 0");
            verificar(g.rutaMasCorta("D", "A").estaVacia(), "sin ruta D->A");
        });

        linea("");
        linea("==== PRUEBAS FUNCIONALES ====");

        caso("F01", "Registrar usuario y buscarlo por identificación (tabla hash)", () -> {
            Esc e = new Esc();
            Usuario u = e.p.getUsuarios().buscar("1001");
            igual("Ana Prueba", u.getNombre(), "nombre");
            igual(NivelUsuario.BRONCE, u.getNivel(), "nivel inicial");
            verificar(e.p.getUsuarios().buscar("9999") == null, "inexistente = null");
        });

        caso("F02", "Validaciones de registro: id duplicado, correo y teléfono inválidos", () -> {
            Esc e = new Esc();
            verificar(lanza(() -> e.p.getUsuarios().registrar("1001", "Otra", "o@x.co", "3000000000")).contains("Ya existe"), "duplicado");
            verificar(lanza(() -> e.p.getUsuarios().registrar("3003", "Otra", "correo-malo", "3000000000")).contains("Correo"), "correo");
            verificar(lanza(() -> e.p.getUsuarios().registrar("3003", "Otra", "o@x.co", "12")).contains("Teléfono"), "teléfono");
        });

        caso("F03", "Modificar datos de un usuario", () -> {
            Esc e = new Esc();
            e.p.getUsuarios().modificar("1001", "Ana María Prueba", "anam@test.co", "3110000000");
            igual("anam@test.co", e.p.getUsuarios().obtener("1001").getCorreo(), "correo modificado");
        });

        caso("F04", "Eliminar usuario: con saldo se rechaza; sin saldo se elimina junto con sus billeteras", () -> {
            Esc e = new Esc();
            e.m().recargar(e.betoG.getCodigo(), 100, "");
            verificar(lanza(() -> e.p.eliminarUsuario("2002")).contains("saldo"), "con saldo");
            e.m().retirar(e.betoG.getCodigo(), 99, "");
            e.m().retirar(e.betoG.getCodigo(), 0.01, ""); // comisión dejó residuo
            e.p.eliminarUsuario("2002");
            verificar(e.p.getUsuarios().buscar("2002") == null, "usuario eliminado");
            verificar(e.p.getBilleteras().buscar(e.betoG.getCodigo()) == null, "billetera eliminada del índice");
        });

        caso("F05", "Múltiples billeteras por usuario; nombre repetido rechazado", () -> {
            Esc e = new Esc();
            igual(3, e.ana.getBilleteras().tamano(), "billeteras de Ana");
            verificar(lanza(() -> e.p.getBilleteras().crear(e.ana, "ahorro", TipoBilletera.AHORRO)).contains("ya tiene"), "duplicada");
            igual(e.anaA, e.p.getBilleteras().obtener(e.anaA.getCodigo().toLowerCase()), "búsqueda por código (hash)");
        });

        caso("F06", "Recarga: actualiza saldo, historial y puntos (1 pt / $100)", () -> {
            Esc e = new Esc();
            Transaccion t = e.m().recargar(e.anaG.getCodigo(), 1_250, "nómina");
            igual(EstadoTransaccion.EXITOSA, t.getEstado(), "estado");
            igualD(1_250, e.anaG.getSaldo(), "saldo");
            igual(12, t.getPuntosGenerados(), "puntos");
            igual(12, e.ana.getPuntosAcumulados(), "puntos del usuario");
            igual(1, e.anaG.getTransacciones().tamano(), "historial billetera");
            igual(1, e.ana.getTransacciones().tamano(), "historial usuario");
        });

        caso("F07", "Retiro: comisión del nivel (Bronce 1 %), 2 pts / $100; saldo insuficiente se rechaza y alerta", () -> {
            Esc e = new Esc();
            e.m().recargar(e.anaG.getCodigo(), 1_000, "");
            Transaccion t = e.m().retirar(e.anaG.getCodigo(), 500, "");
            igualD(5, t.getComision(), "comisión");
            igualD(495, e.anaG.getSaldo(), "saldo tras retiro");
            igual(10, t.getPuntosGenerados(), "puntos retiro");
            Transaccion r = e.m().retirar(e.anaG.getCodigo(), 600, "");
            igual(EstadoTransaccion.RECHAZADA, r.getEstado(), "rechazada");
            igualD(495, e.anaG.getSaldo(), "saldo intacto");
            verificar(e.ana.notificacionesRecientes().ultimo().getTipo() == TipoNotificacion.OPERACION_RECHAZADA, "alerta de rechazo");
        });

        caso("F08", "Transferencias propia (sin comisión) y a tercero (con comisión); grafo actualizado", () -> {
            Esc e = new Esc();
            e.m().recargar(e.anaG.getCodigo(), 2_000, "");
            Transaccion p = e.m().transferir(e.anaG.getCodigo(), e.anaA.getCodigo(), 300, "");
            igual(TipoTransaccion.TRANSFERENCIA_PROPIA, p.getTipo(), "tipo propia");
            igualD(0, p.getComision(), "sin comisión");
            Transaccion x = e.m().transferir(e.anaG.getCodigo(), e.betoG.getCodigo(), 400, "");
            igual(TipoTransaccion.TRANSFERENCIA_EXTERNA, x.getTipo(), "tipo externa");
            igualD(4, x.getComision(), "comisión 1%");
            igualD(2_000 - 300 - 404, e.anaG.getSaldo(), "saldo origen");
            igualD(400, e.betoG.getSaldo(), "saldo destino");
            igual(12, x.getPuntosGenerados(), "3 pts / $100");
            igual(1, e.m().getGrafoUsuarios().obtenerArista("1001", "2002").getCantidad(), "arista usuarios");
            igual(2, e.m().getGrafoBilleteras().numAristas(), "aristas billeteras");
            verificar(e.beto.getTransacciones().contiene(x), "el receptor ve la transferencia en su historial");
        });

        caso("F09", "Límite por operación del nivel (Bronce $5.000)", () -> {
            Esc e = new Esc();
            Transaccion t = e.m().recargar(e.anaG.getCodigo(), 5_001, "");
            igual(EstadoTransaccion.RECHAZADA, t.getEstado(), "rechazada");
            verificar(t.getMotivoRechazo().contains("límite"), "motivo");
        });

        caso("F10", "Billetera bloqueada no puede operar", () -> {
            Esc e = new Esc();
            e.m().recargar(e.anaG.getCodigo(), 100, "");
            e.p.getBilleteras().cambiarEstado(e.anaG.getCodigo(), EstadoBilletera.BLOQUEADA);
            igual(EstadoTransaccion.RECHAZADA, e.m().retirar(e.anaG.getCodigo(), 10, "").getEstado(), "rechazada");
            verificar(lanza(() -> e.p.getBilleteras().cambiarEstado(e.anaG.getCodigo(), EstadoBilletera.CERRADA)).contains("saldo"),
                    "no se cierra con saldo");
        });

        caso("F11", "Valores inválidos (cero, negativo, misma billetera, inexistente)", () -> {
            Esc e = new Esc();
            lanza(() -> e.m().recargar(e.anaG.getCodigo(), 0, ""));
            lanza(() -> e.m().recargar(e.anaG.getCodigo(), -50, ""));
            lanza(() -> e.m().transferir(e.anaG.getCodigo(), e.anaG.getCodigo(), 10, ""));
            lanza(() -> e.m().recargar("BIL-9999", 10, ""));
            igual(0, e.m().getTransacciones().tamano(), "no se registró nada");
        });

        caso("F12", "Deshacer última operación (pila): restaura saldos, historial y puntos", () -> {
            Esc e = new Esc();
            e.m().recargar(e.anaG.getCodigo(), 1_000, "");
            Transaccion t = e.m().transferir(e.anaG.getCodigo(), e.betoG.getCodigo(), 500, "por error");
            int puntosAntes = e.ana.getPuntosAcumulados();
            e.min(5);
            Transaccion rev = e.m().revertirUltima(e.ana);
            igual(EstadoTransaccion.REVERTIDA, t.getEstado(), "original revertida");
            igual(TipoTransaccion.REVERSION, rev.getTipo(), "movimiento de reversión");
            igualD(1_000, e.anaG.getSaldo(), "saldo origen restaurado (incluye comisión)");
            igualD(0, e.betoG.getSaldo(), "saldo destino restaurado");
            igual(puntosAntes - t.getPuntosGenerados(), e.ana.getPuntosAcumulados(), "puntos recalculados");
            verificar(e.m().getGrafoUsuarios().obtenerArista("1001", "2002") == null, "arista del grafo descontada");
            igual(t, e.ana.getPilaReversibles().cima().getTipo() == TipoTransaccion.RECARGA ? t : null, "la cima vuelve a ser la recarga");
        });

        caso("F13", "Revertir una operación específica por id usando pila auxiliar (orden preservado)", () -> {
            Esc e = new Esc();
            Transaccion t1 = e.m().recargar(e.anaG.getCodigo(), 300, "");
            Transaccion t2 = e.m().recargar(e.anaG.getCodigo(), 200, "duplicada");
            Transaccion t3 = e.m().transferir(e.anaG.getCodigo(), e.anaA.getCodigo(), 100, "");
            e.m().revertirPorId(t2.getId(), "Operación duplicada");
            ListaEnlazada<String> pila = new ListaEnlazada<>();
            for (Transaccion t : e.ana.getPilaReversibles()) pila.agregar(t.getId());
            igual("[" + t3.getId() + ", " + t1.getId() + "]", pila.toString(), "pila sin t2 y en el mismo orden");
            igualD(300, e.ana.saldoTotal(), "saldo total del usuario");
        });

        caso("F14", "Reversiones no permitidas: destino sin fondos, doble reversión y plazo vencido", () -> {
            Esc e = new Esc();
            e.m().recargar(e.anaG.getCodigo(), 1_000, "");
            Transaccion t = e.m().transferir(e.anaG.getCodigo(), e.betoG.getCodigo(), 500, "");
            e.m().retirar(e.betoG.getCodigo(), 450, "Beto gastó el dinero");
            verificar(lanza(() -> e.m().revertirPorId(t.getId(), "")).contains("saldo suficiente"), "destino sin fondos");
            Transaccion r = e.m().recargar(e.anaA.getCodigo(), 100, "");
            e.m().revertirPorId(r.getId(), "");
            verificar(lanza(() -> e.m().revertirPorId(r.getId(), "")).contains("no es reversible"), "doble reversión");
            Transaccion v = e.m().recargar(e.anaC.getCodigo(), 100, "");
            e.p.getReloj().avanzarHoras(73);
            verificar(lanza(() -> e.m().revertirPorId(v.getId(), "")).contains("plazo"), "plazo de 72 h");
        });

        caso("F15", "Cola de prioridad de programadas: fecha y, en empate, mayor nivel primero", () -> {
            Esc e = new Esc();
            for (int i = 0; i < 3; i++) { // 3 ciclos x 200 pts = 600 pts: Ana sube a Plata
                e.min(60);
                e.m().recargar(e.anaG.getCodigo(), 5_000, "");
                e.m().transferir(e.anaG.getCodigo(), e.anaA.getCodigo(), 5_000, "");
            }
            igual(NivelUsuario.PLATA, e.ana.getNivel(), "Ana en Plata");
            e.m().recargar(e.betoG.getCodigo(), 1_000, "");
            GestorProgramadas gp = e.p.getProgramadas();
            LocalDateTime f = T0.plusDays(1);
            OperacionProgramada b = gp.programar("2002", TipoOperacionProgramada.PAGO_RECURRENTE, e.betoG.getCodigo(), e.anaG.getCodigo(), 10, f, Frecuencia.UNICA, 1, "beto");
            OperacionProgramada a = gp.programar("1001", TipoOperacionProgramada.AHORRO_AUTOMATICO, e.anaG.getCodigo(), e.anaA.getCodigo(), 10, f, Frecuencia.UNICA, 1, "ana");
            OperacionProgramada c = gp.programar("1001", TipoOperacionProgramada.AHORRO_AUTOMATICO, e.anaG.getCodigo(), e.anaA.getCodigo(), 10, f.minusHours(2), Frecuencia.UNICA, 1, "temprana");
            ListaEnlazada<OperacionProgramada> orden = gp.pendientesEnOrden();
            igual(c, orden.obtener(0), "la más temprana primero");
            igual(a, orden.obtener(1), "en empate, Plata antes que Bronce");
            igual(b, orden.obtener(2), "Bronce al final");
        });

        caso("F16", "Simulación: ahorro semanal x3 se ejecuta 3 veces con bono y alerta previa", () -> {
            Esc e = new Esc();
            e.m().recargar(e.anaG.getCodigo(), 1_000, "");
            int puntos = e.ana.getPuntosAcumulados();
            e.p.getProgramadas().programar("1001", TipoOperacionProgramada.AHORRO_AUTOMATICO, e.anaG.getCodigo(), e.anaA.getCodigo(),
                    100, T0.plusDays(2), Frecuencia.SEMANAL, 3, "ahorro");
            int n = e.p.getProgramadas().simularDias(30);
            igual(3, n, "ejecuciones");
            igualD(300, e.anaA.getSaldo(), "saldo ahorro");
            igual(puntos + 3 * (3 + GestorRecompensas.BONO_PROGRAMADA), e.ana.getPuntosAcumulados(), "puntos con bono");
            igual(0, e.p.getProgramadas().pendientesEnOrden().tamano(), "sin pendientes");
            verificar(e.ana.notificacionesRecientes().contar(x -> x.getTipo() == TipoNotificacion.PROGRAMADA_PROXIMA) == 3, "3 alertas previas");
        });

        caso("F17", "Programada sin fondos: queda como fallida y se notifica", () -> {
            Esc e = new Esc();
            e.m().recargar(e.anaG.getCodigo(), 50, "");
            OperacionProgramada op = e.p.getProgramadas().programar("1001", TipoOperacionProgramada.PAGO_RECURRENTE, e.anaG.getCodigo(),
                    e.betoG.getCodigo(), 100, T0.plusHours(3), Frecuencia.UNICA, 1, "");
            e.p.getProgramadas().simularDias(1);
            igual(1, op.getEjecucionesFallidas(), "fallida");
            igualD(0, e.betoG.getSaldo(), "no se movió dinero");
        });

        caso("F18", "Niveles: ascenso Bronce→Plata→Oro→Platino, árbol AVL reubica y busca por rango", () -> {
            Esc e = new Esc();
            ListaEnlazada<NivelUsuario> niveles = new ListaEnlazada<>();
            niveles.agregar(e.ana.getNivel());
            int ascensos = 0;
            for (int i = 0; i < 60; i++) {
                e.min(60);
                e.m().recargar(e.anaG.getCodigo(), 5_000, "");
                e.m().transferir(e.anaG.getCodigo(), e.anaA.getCodigo(), 5_000, "");
                if (e.ana.getNivel() != niveles.ultimo()) {
                    niveles.agregar(e.ana.getNivel());
                    NivelUsuario nuevo = e.ana.getNivel();
                    if (e.ana.notificacionesRecientes().buscar(n -> n.getTipo() == TipoNotificacion.ASCENSO_NIVEL
                            && n.getMensaje().contains(" a " + nuevo + ".")) != null) ascensos++;
                }
            }
            igual("[Bronce, Plata, Oro, Platino]", niveles.toString(), "secuencia de niveles");
            igual(3, ascensos, "cada ascenso se notificó");
            igual(e.ana, e.p.getUsuarios().ranking(1).primero(), "primera en el ranking");
            verificar(e.p.getUsuarios().porNivel(NivelUsuario.PLATINO).contiene(e.ana), "consulta por nivel");
            verificar(e.p.getUsuarios().porRangoDePuntos(0, 10).contiene(e.beto), "Beto en rango 0-10");
            verificar(e.p.getUsuarios().getArbolPuntos().esValido(), "árbol válido");
        });

        caso("F19", "Canje de beneficios: descuenta disponibles (no el nivel); valida nivel y puntos", () -> {
            Esc e = new Esc();
            for (int i = 0; i < 4; i++) {
                e.min(60);
                e.m().recargar(e.anaG.getCodigo(), 5_000, "");
            }
            int acumulados = e.ana.getPuntosAcumulados();
            BeneficioCanjeado bc = e.p.canjear("1001", "BEN-01");
            igual(acumulados - 150, e.ana.getPuntosDisponibles(), "disponibles");
            igual(acumulados, e.ana.getPuntosAcumulados(), "acumulados intactos");
            igual(1, e.ana.getBeneficiosCanjeados().tamano(), "lista de canjes");
            verificar(bc.getCodigoCanje().startsWith("CNJ"), "código de canje");
            verificar(lanza(() -> e.p.canjear("1001", "BEN-06")).contains("requiere nivel"), "nivel insuficiente");
            verificar(lanza(() -> e.p.canjear("2002", "BEN-01")).contains("insuficientes"), "puntos insuficientes");
        });

        caso("F20", "Notificaciones: cola FIFO con despacho manual e historial acotado a 30", () -> {
            Esc e = new Esc();
            GestorNotificaciones gn = e.p.getNotificaciones();
            gn.setDespachoAutomatico(false);
            for (int i = 0; i < 35; i++) gn.notificar(e.ana, TipoNotificacion.SALDO_BAJO, "aviso " + i);
            igual(35, gn.getPendientes().tamano(), "en cola");
            igual("aviso 0", gn.despacharSiguiente().getMensaje(), "FIFO");
            gn.despacharTodas();
            igual(30, e.ana.notificacionesRecientes().tamano(), "historial acotado");
            igual("aviso 5", e.ana.notificacionesRecientes().primero().getMensaje(), "se descartan las más antiguas");
        });

        caso("F21", "Alerta automática de saldo bajo", () -> {
            Esc e = new Esc();
            e.m().recargar(e.anaG.getCodigo(), 300, "");
            e.m().retirar(e.anaG.getCodigo(), 250, "");
            verificar(e.ana.notificacionesRecientes().contar(n -> n.getTipo() == TipoNotificacion.SALDO_BAJO) == 1, "alerta");
        });

        caso("F22", "Analítica: mayor valor (AVL), monto en rango sin revertidas, mayor actividad, frecuencia", () -> {
            Esc e = new Esc();
            e.m().recargar(e.anaG.getCodigo(), 1_000, "");
            e.min(10);
            e.m().recargar(e.betoG.getCodigo(), 3_000, "");
            e.min(10);
            Transaccion r = e.m().recargar(e.anaA.getCodigo(), 4_000, "mal aplicada");
            e.min(10);
            e.m().revertirPorId(r.getId(), "");
            e.min(10);
            e.m().transferir(e.anaG.getCodigo(), e.anaC.getCodigo(), 200, "");
            ListaEnlazada<Transaccion> top = e.p.getAnalitica().transaccionesMayorValor(2);
            igualD(3_000, top.obtener(0).getValor(), "mayor vigente (la revertida salió del AVL)");
            igualD(1_000, top.obtener(1).getValor(), "segunda");
            ItemReporte monto = e.p.getAnalitica().montoTotalEnRango(T0, T0.plusHours(1));
            igualD(4_200, monto.getMonto(), "monto movilizado vigente");
            igual("Ana Prueba (1001)", e.p.getAnalitica().usuarioMayorActividad(T0, T0.plusHours(1)).getEtiqueta(), "mayor actividad");
            int total = 0;
            for (ItemReporte it : e.p.getAnalitica().frecuenciaPorTipo()) total += it.getCantidad();
            igual(e.m().getTransacciones().tamano(), total, "frecuencias suman el total");
        });

        caso("F23", "Detección: ráfaga + destino repetido (MEDIO) y luego monto inusual → CRÍTICO bloqueada", () -> {
            Esc e = new Esc();
            e.m().recargar(e.anaG.getCodigo(), 4_000, "");
            for (int i = 0; i < 5; i++) {
                e.p.getReloj().avanzarHoras(5);
                e.m().transferir(e.anaG.getCodigo(), e.anaC.getCodigo(), 50, "historial normal");
            }
            e.p.getReloj().avanzarHoras(5);
            Transaccion t = null;
            for (int i = 0; i < 3; i++) {
                e.min(1);
                t = e.m().transferir(e.anaG.getCodigo(), e.betoG.getCodigo(), 40, "ráfaga");
            }
            igual(NivelRiesgo.MEDIO, t.getNivelRiesgo(), "riesgo medio");
            verificar(t.esExitosa(), "se ejecuta pero queda marcada");
            e.min(1);
            Transaccion grande = e.m().transferir(e.anaG.getCodigo(), e.betoG.getCodigo(), 1_000, "alto valor");
            igual(NivelRiesgo.CRITICO, grande.getNivelRiesgo(), "riesgo crítico");
            igual(EstadoTransaccion.RECHAZADA, grande.getEstado(), "bloqueada");
            ListaEnlazada<EventoAuditoria> aud = e.p.getDetector().auditoriaDeUsuario("1001");
            verificar(aud.tamano() >= 2 && aud.ultimo().isBloqueada(), "eventos en auditoría");
            verificar(e.ana.notificacionesRecientes().contar(n -> n.getTipo() == TipoNotificacion.ALERTA_SEGURIDAD) >= 2, "alertas de seguridad");
        });

        caso("F24", "Detección: fragmentación (salidas desde 3 billeteras en 15 min)", () -> {
            Esc e = new Esc();
            e.m().recargar(e.anaG.getCodigo(), 500, "");
            e.m().recargar(e.anaA.getCodigo(), 500, "");
            e.m().recargar(e.anaC.getCodigo(), 500, "");
            e.min(30);
            e.m().retirar(e.anaG.getCodigo(), 100, "");
            e.min(3);
            e.m().retirar(e.anaA.getCodigo(), 100, "");
            e.min(3);
            Transaccion t = e.m().retirar(e.anaC.getCodigo(), 100, "");
            verificar(t.getNivelRiesgo() != NivelRiesgo.NINGUNO, "marcada");
            verificar(e.p.getDetector().getAuditoria().ultimo().resumenHallazgos().contains("Fragmentación"), "hallazgo");
        });

        caso("F25", "Detección: horario no habitual (regla aislada)", () -> {
            Esc e = new Esc();
            for (int i = 0; i < 20; i++) {
                e.p.getReloj().establecer(T0.plusDays(i).withHour(10));
                e.m().recargar(e.anaG.getCodigo(), 10, "");
            }
            e.p.getReloj().establecer(T0.plusDays(21).withHour(3));
            Transaccion candidata = new Transaccion("X", e.p.getReloj().ahora(), TipoTransaccion.RETIRO, 5, e.anaG, null, e.ana, "");
            String h = new ReglaHorarioInusual(15, 0.03).evaluar(candidata, e.ana.getTransacciones());
            verificar(h != null && h.contains("03:00"), "hallazgo: " + h);
        });

        caso("F26", "Grafo de usuarios: ciclo de dinero y ruta frecuente en escenario controlado", () -> {
            Esc e = new Esc();
            Usuario c = e.p.getUsuarios().registrar("3003", "Caro Prueba", "caro@test.co", "3009998877");
            Billetera cG = e.p.getBilleteras().crear(c, "Principal", TipoBilletera.GASTOS_DIARIOS);
            e.m().recargar(e.anaG.getCodigo(), 3_000, "");
            for (int i = 0; i < 3; i++) {
                e.p.getReloj().avanzarHoras(2);
                e.m().transferir(e.anaG.getCodigo(), e.betoG.getCodigo(), 100, "");
                e.p.getReloj().avanzarHoras(2);
                e.m().transferir(e.betoG.getCodigo(), cG.getCodigo(), 50, "");
                e.p.getReloj().avanzarHoras(2);
                e.m().transferir(cG.getCodigo(), e.anaG.getCodigo(), 20, "");
            }
            ListaEnlazada<ListaEnlazada<String>> ciclos = e.p.getRelaciones().ciclosEntreUsuarios();
            igual(1, ciclos.tamano(), "un ciclo");
            igual(4, ciclos.primero().tamano(), "ciclo de 3 usuarios (cerrado)");
            Grafo.Ruta<String> mejor = e.p.getRelaciones().rutasFrecuentes(3).primero();
            igual(3, mejor.getFrecuencia(), "frecuencia 3");
            igual("[1001, 2002, 3003]", e.p.getRelaciones().rutaEntre("1001", "3003").toString(), "BFS");
        });

        caso("F27", "Comparador de rendimiento: todas las estructuras dan el mismo resultado", () -> {
            ListaEnlazada<ComparadorRendimiento.Resultado> r = new ComparadorRendimiento().ejecutar(5_000, 7);
            String busqueda = null, top = null;
            for (ComparadorRendimiento.Resultado x : r) {
                if (x.getEscenario().startsWith("Búsqueda")) {
                    if (busqueda == null) busqueda = x.getVerificacion();
                    igual(busqueda, x.getVerificacion(), "búsqueda " + x.getEstructura());
                }
                if (x.getEscenario().startsWith("Top-10")) {
                    if (top == null) top = x.getVerificacion();
                    igual(top, x.getVerificacion(), "top " + x.getEstructura());
                }
            }
            verificar(r.tamano() == 17, "17 mediciones");
        });

        caso("F28", "Datos de demostración: conservación del dinero (saldos = recargas − retiros − comisiones)", () -> {
            PlataformaFintech p = new PlataformaFintech();
            DatosDemo.cargar(p);
            double entradas = 0, salidas = 0;
            for (Transaccion t : p.getMotor().getTransacciones()) {
                if (t.getEstado() != EstadoTransaccion.EXITOSA) continue;
                if (t.getTipo() == TipoTransaccion.RECARGA) entradas += t.getValor();
                if (t.getTipo() == TipoTransaccion.RETIRO) salidas += t.getValor();
            }
            double saldos = 0;
            for (Billetera b : p.getBilleteras().todas()) saldos += b.getSaldo();
            igualD(entradas - salidas - p.getMotor().getComisionesRecaudadas(), saldos, "conservación");
            verificar(p.getUsuarios().getArbolPuntos().esValido(), "árbol válido");
            verificar(p.getDetector().getAuditoria().tamano() > 0, "hay eventos de auditoría");
            verificar(p.getRelaciones().ciclosEntreUsuarios().tamano() > 0, "hay ciclos");
        });

        linea("");
        linea(String.format("RESULTADO: %d aprobadas, %d fallidas, %d en total", aprobadas, fallidas, aprobadas + fallidas));
        if (args.length > 0) java.nio.file.Files.writeString(java.nio.file.Path.of(args[0]), reporte.toString());
        if (fallidas > 0) System.exit(1);
    }
}
