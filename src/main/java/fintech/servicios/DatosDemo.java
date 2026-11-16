package fintech.servicios;

import fintech.modelo.Billetera;
import fintech.modelo.Frecuencia;
import fintech.modelo.TipoBilletera;
import fintech.modelo.TipoOperacionProgramada;
import fintech.modelo.Usuario;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Random;

/**
 * Genera un escenario de demostración: usuarios, billeteras, un mes de
 * historial con patrones de transferencia (rutas frecuentes, ciclos,
 * relaciones recíprocas), operaciones programadas y algunos casos sospechosos.
 */
public final class DatosDemo {

    private DatosDemo() {
    }

    /** Avanza el reloj (procesando programadas) solo si la fecha es futura. */
    private static void avanzarA(GestorProgramadas gp, Reloj reloj, LocalDateTime fecha) {
        if (fecha.isAfter(reloj.ahora())) gp.simularHasta(fecha);
    }

    public static void cargar(PlataformaFintech p) {
        Reloj reloj = p.getReloj();
        LocalDateTime fin = reloj.ahora().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime inicio = fin.minusDays(30).withHour(8).withMinute(0);
        reloj.establecer(inicio);
        boolean auto = p.getNotificaciones().isDespachoAutomatico();
        p.getNotificaciones().setDespachoAutomatico(true);

        GestorUsuarios gu = p.getUsuarios();
        GestorBilleteras gb = p.getBilleteras();
        MotorTransacciones m = p.getMotor();

        Usuario ana = gu.registrar("1001", "Ana Gómez", "ana.gomez@correo.com", "3001112233");
        Usuario carlos = gu.registrar("1002", "Carlos Pérez", "carlos.perez@correo.com", "3002223344");
        Usuario laura = gu.registrar("1003", "Laura Martínez", "laura.m@correo.com", "3003334455");
        Usuario andres = gu.registrar("1004", "Andrés Rodríguez", "andres.r@correo.com", "3004445566");
        Usuario vale = gu.registrar("1005", "Valentina Torres", "vale.torres@correo.com", "3005556677");
        Usuario santi = gu.registrar("1006", "Santiago Ramírez", "santi.ramirez@correo.com", "3006667788");
        Usuario gym = gu.registrar("9001", "Gimnasio Vital SAS", "pagos@gimnasiovital.co", "6014445566");
        Usuario arr = gu.registrar("9002", "Arrendamientos Centro", "cobros@arrcentro.co", "6017778899");

        Billetera anaG = gb.crear(ana, "Día a día", TipoBilletera.GASTOS_DIARIOS);
        Billetera anaA = gb.crear(ana, "Ahorro viaje", TipoBilletera.AHORRO);
        Billetera anaC = gb.crear(ana, "Compras online", TipoBilletera.COMPRAS);
        Billetera carG = gb.crear(carlos, "Principal", TipoBilletera.GASTOS_DIARIOS);
        Billetera carT = gb.crear(carlos, "Transporte", TipoBilletera.TRANSPORTE);
        Billetera lauG = gb.crear(laura, "Gastos", TipoBilletera.GASTOS_DIARIOS);
        Billetera lauI = gb.crear(laura, "Inversión", TipoBilletera.INVERSION);
        Billetera andG = gb.crear(andres, "Nómina", TipoBilletera.GASTOS_DIARIOS);
        Billetera andS = gb.crear(andres, "Suscripciones", TipoBilletera.SUSCRIPCIONES);
        Billetera valG = gb.crear(vale, "Cuenta diaria", TipoBilletera.GASTOS_DIARIOS);
        Billetera valC = gb.crear(vale, "Compras", TipoBilletera.COMPRAS);
        Billetera sanG = gb.crear(santi, "Personal", TipoBilletera.GASTOS_DIARIOS);
        Billetera sanC = gb.crear(santi, "Compras", TipoBilletera.COMPRAS);
        Billetera sanT = gb.crear(santi, "Transporte", TipoBilletera.TRANSPORTE);
        Billetera gymR = gb.crear(gym, "Recaudo", TipoBilletera.GASTOS_DIARIOS);
        Billetera arrR = gb.crear(arr, "Recaudo arriendos", TipoBilletera.INVERSION);

        // saldos iniciales
        for (Billetera b : new Billetera[]{anaG, carG, lauG, andG, valG, sanG}) m.recargar(b.getCodigo(), 2_800, "Saldo inicial");
        m.recargar(anaA.getCodigo(), 1_500, "Saldo inicial");
        m.recargar(sanC.getCodigo(), 900, "Saldo inicial");
        m.recargar(sanT.getCodigo(), 400, "Saldo inicial");
        m.recargar(carT.getCodigo(), 150, "Saldo inicial");

        // operaciones programadas
        GestorProgramadas gp = p.getProgramadas();
        gp.programar(ana.getId(), TipoOperacionProgramada.AHORRO_AUTOMATICO, anaG.getCodigo(), anaA.getCodigo(), 120,
                inicio.plusDays(2).withHour(7), Frecuencia.SEMANAL, -1, "Ahorro semanal para viaje");
        gp.programar(andres.getId(), TipoOperacionProgramada.PAGO_RECURRENTE, andG.getCodigo(), arrR.getCodigo(), 1_100,
                inicio.plusDays(4).withHour(9), Frecuencia.MENSUAL, 12, "Canon de arrendamiento");
        gp.programar(vale.getId(), TipoOperacionProgramada.PAGO_RECURRENTE, valG.getCodigo(), gymR.getCodigo(), 85,
                inicio.plusDays(1).withHour(10), Frecuencia.MENSUAL, -1, "Mensualidad gimnasio");
        gp.programar(carlos.getId(), TipoOperacionProgramada.RECARGA_AUTOMATICA, null, carT.getCodigo(), 40,
                inicio.plusDays(1).withHour(6), Frecuencia.SEMANAL, 8, "Recarga tarjeta de transporte");
        gp.programar(laura.getId(), TipoOperacionProgramada.TRANSFERENCIA_PERIODICA, lauG.getCodigo(), lauI.getCodigo(), 250,
                inicio.plusDays(3).withHour(8), Frecuencia.SEMANAL, -1, "Aporte a inversión");

        Random rnd = new Random(2026);
        Billetera[][] rutasFrecuentes = {
                {anaG, carG}, {carG, lauG}, {lauG, anaG},          // ciclo Ana -> Carlos -> Laura -> Ana
                {andG, valG}, {valG, andG},                        // relación recíproca Andrés <-> Valentina
                {sanG, anaG}, {valC, sanC}, {carG, valG}
        };
        for (int dia = 0; dia < 28; dia++) {
            LocalDateTime base = inicio.plusDays(dia);
            avanzarA(gp, reloj, base.withHour(7));
            // movimientos de alto volumen (nómina e inversión) para que haya usuarios en distintos niveles
            try {
                if (dia % 3 == 0) {
                    avanzarA(gp, reloj, base.withHour(8).withMinute(5));
                    m.recargar(lauG.getCodigo(), 4_500, "Pago de honorarios");
                    avanzarA(gp, reloj, base.withHour(8).withMinute(20));
                    m.transferir(lauG.getCodigo(), lauI.getCodigo(), 3_500, "Inversión");
                }
                if (dia % 7 == 1) {
                    avanzarA(gp, reloj, base.withHour(8).withMinute(10));
                    m.recargar(andG.getCodigo(), 4_800, "Nómina");
                    m.recargar(valG.getCodigo(), 4_000, "Nómina");
                    avanzarA(gp, reloj, base.withHour(8).withMinute(25));
                    m.transferir(valG.getCodigo(), valC.getCodigo(), 2_500, "Presupuesto de compras");
                }
            } catch (ValidacionException ignorada) {
                // si algún dato no es válido se omite
            }
            int nOps = 3 + rnd.nextInt(4);
            int[] horas = new int[nOps];
            for (int i = 0; i < nOps; i++) horas[i] = 8 * 60 + 30 + rnd.nextInt(11 * 60 + 30); // entre 8:30 y 20:00
            Arrays.sort(horas);
            for (int i = 0; i < nOps; i++) {
                LocalDateTime cuando = base.withHour(0).plusMinutes(horas[i]);
                avanzarA(gp, reloj, cuando); // ejecuta a tiempo las programadas que venzan antes
                int tipo = rnd.nextInt(10);
                try {
                    if (tipo < 5) {
                        Billetera[] par = rutasFrecuentes[rnd.nextInt(rutasFrecuentes.length)];
                        m.transferir(par[0].getCodigo(), par[1].getCodigo(), 20 + rnd.nextInt(180), "Pago entre amigos");
                    } else if (tipo < 7) {
                        Billetera b = new Billetera[]{anaG, carG, lauG, andG, valG, sanG}[rnd.nextInt(6)];
                        m.recargar(b.getCodigo(), 100 + rnd.nextInt(600), "Recarga desde banco");
                    } else if (tipo < 9) {
                        Billetera b = new Billetera[]{anaC, valC, sanC, carT, sanT}[rnd.nextInt(5)];
                        Billetera desde = b.getUsuario().getBilleteras().primero();
                        if (desde != b) m.transferir(desde.getCodigo(), b.getCodigo(), 30 + rnd.nextInt(120), "Mover a bolsillo");
                    } else {
                        Billetera b = new Billetera[]{anaG, carG, lauG, andG, valG}[rnd.nextInt(5)];
                        m.retirar(b.getCodigo(), 40 + rnd.nextInt(160), "Retiro en cajero");
                    }
                } catch (ValidacionException ignorada) {
                    // datos aleatorios: si una operación no es válida simplemente se omite
                }
            }
        }

        // ---- casos especiales en los últimos días
        LocalDateTime d29 = inicio.plusDays(28);
        avanzarA(gp, reloj, d29.withHour(9));
        reloj.establecer(d29.withHour(9).withMinute(15));
        m.transferir(lauG.getCodigo(), valG.getCodigo(), 75, "Pago almuerzo");
        reloj.avanzarMinutos(1);
        var duplicada = m.transferir(lauG.getCodigo(), valG.getCodigo(), 75, "Pago almuerzo (duplicado)");
        reloj.avanzarMinutos(10);
        m.revertirPorId(duplicada.getId(), "Operación duplicada");

        // operaciones rechazadas (saldo insuficiente y límite del nivel)
        reloj.avanzarMinutos(20);
        m.retirar(carT.getCodigo(), 4_900, "Retiro sin fondos suficientes");
        reloj.avanzarMinutos(2);
        m.recargar(sanG.getCodigo(), 7_500, "Recarga que supera el límite Bronce");

        reloj.establecer(d29.withHour(11));
        m.recargar(anaC.getCodigo(), 300, "Recarga mal aplicada");
        reloj.avanzarMinutos(5);
        m.revertirUltima(ana);

        // fragmentación: Santiago saca dinero de 3 billeteras en pocos minutos
        reloj.establecer(d29.withHour(15).withMinute(10));
        m.retirar(sanG.getCodigo(), 300, "Retiro");
        reloj.avanzarMinutos(3);
        m.retirar(sanC.getCodigo(), 250, "Retiro");
        reloj.avanzarMinutos(4);
        m.retirar(sanT.getCodigo(), 200, "Retiro");

        // ráfaga nocturna de Carlos hacia la misma billetera
        LocalDateTime noche = d29.plusDays(1).withHour(2).withMinute(30);
        avanzarA(gp, reloj, noche.minusMinutes(1));
        for (int i = 0; i < 4; i++) {
            if (noche.plusMinutes(i).isAfter(reloj.ahora())) reloj.establecer(noche.plusMinutes(i));
            m.transferir(carG.getCodigo(), valC.getCodigo(), 150 + 10 * i, "Transferencia urgente");
        }
        // envío de alto valor al mismo destino: ráfaga + destino repetido + monto inusual = riesgo CRÍTICO (se bloquea)
        reloj.avanzarMinutos(1);
        m.transferir(carG.getCodigo(), valC.getCodigo(), 1_500, "Transferencia urgente de alto valor");

        // dejar programadas próximas para ver la alerta previa
        avanzarA(gp, reloj, fin);
        gp.programar(santi.getId(), TipoOperacionProgramada.PAGO_RECURRENTE, sanG.getCodigo(), gymR.getCodigo(), 85,
                reloj.ahora().plusHours(6), Frecuencia.MENSUAL, -1, "Mensualidad gimnasio");
        gp.programar(carlos.getId(), TipoOperacionProgramada.TRANSFERENCIA_PERIODICA, carG.getCodigo(), lauG.getCodigo(), 60,
                reloj.ahora().plusDays(2), Frecuencia.SEMANAL, 4, "Cuota compartida");

        p.getNotificaciones().setDespachoAutomatico(auto);
    }
}
