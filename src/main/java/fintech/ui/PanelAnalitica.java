package fintech.ui;

import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Transaccion;
import fintech.servicios.ItemReporte;
import fintech.servicios.ServicioAnalitica;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;

/** Consultas e informes de uso de la plataforma. */
public class PanelAnalitica extends JPanel implements Refrescable {

    private final VentanaPrincipal v;
    private final JTextField txtDesde = UI.campo(13);
    private final JTextField txtHasta = UI.campo(13);
    private final JSpinner spTop = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
    private final DefaultTableModel modelo = UI.modelo("#", "Concepto", "Cantidad", "Monto", "Detalle");
    private final JTable tabla = UI.tabla(modelo);
    private final GraficoBarras grafico = new GraficoBarras();
    private final JLabel lblExplicacion = UI.etiqueta(" ");
    private Runnable ultimoReporte = this::resumen;

    public PanelAnalitica(VentanaPrincipal v) {
        super(new BorderLayout(10, 10));
        this.v = v;
        setBackground(UI.FONDO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        LocalDateTime ahora = v.getPlataforma().getReloj().ahora();
        txtDesde.setText(UI.fecha(ahora.minusDays(7)));
        txtHasta.setText(UI.fecha(ahora));

        JPanel botones = new JPanel(new GridLayout(0, 1, 4, 4));
        botones.setOpaque(false);
        botones.add(boton("Resumen general", this::resumen));
        botones.add(boton("Billeteras con mayor uso", () -> items(analitica().billeterasMayorUso(top()),
                "Top-K con TablaHash (conteo) + ColaPrioridad de tamaño K", false)));
        botones.add(boton("Usuarios con más transferencias", () -> items(analitica().usuariosMasTransferencias(top()),
                "Conteo en TablaHash y top-K con montículo", false)));
        botones.add(boton("Categorías de billetera más activas", () -> items(analitica().categoriasMasActivas(),
                "Movimientos por tipo de billetera (TablaHash)", false)));
        botones.add(boton("Frecuencia por tipo de transacción", () -> items(analitica().frecuenciaPorTipo(),
                "Frecuencia y monto por tipo (TablaHash)", false)));
        botones.add(boton("Transacciones de mayor valor", this::mayorValor));
        botones.add(boton("Monto movilizado en el rango", this::montoRango));
        botones.add(boton("Usuario con mayor actividad en el rango", this::mayorActividad));
        botones.add(boton("Patrones de interacción (grafo)", () -> items(v.getPlataforma().getRelaciones().patronesInteraccion(),
                "Grado de salida/entrada y montos en el grafo de usuarios", true)));

        JPanel izq = new JPanel(new BorderLayout(6, 6));
        izq.setOpaque(false);
        izq.add(UI.formulario("Desde:", txtDesde, "Hasta:", txtHasta, "Top K:", spTop), BorderLayout.NORTH);
        izq.add(UI.arriba(botones), BorderLayout.CENTER);
        add(UI.seccion("Informes", izq), BorderLayout.WEST);

        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.setOpaque(false);
        lblExplicacion.setFont(UI.FUENTE_NEGRITA);
        lblExplicacion.setForeground(UI.PRIMARIO);
        centro.add(lblExplicacion, BorderLayout.NORTH);
        UI.anchos(tabla, 35, 280, 80, 110, 380);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, UI.scroll(tabla), grafico);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        centro.add(split, BorderLayout.CENTER);
        add(UI.seccion("Resultado", centro), BorderLayout.CENTER);
    }

    private JButton boton(String texto, Runnable r) {
        return UI.boton(texto, () -> UI.ejecutar(this, () -> {
            ultimoReporte = r;
            r.run();
        }));
    }

    private ServicioAnalitica analitica() {
        return v.getPlataforma().getAnalitica();
    }

    private int top() {
        return (Integer) spTop.getValue();
    }

    private void resumen() {
        items(analitica().resumenGeneral(), "Indicadores generales de la plataforma", true);
    }

    private void items(ListaEnlazada<ItemReporte> l, String explicacion, boolean graficarMonto) {
        lblExplicacion.setText(explicacion);
        modelo.setRowCount(0);
        grafico.limpiar();
        grafico.setTitulo(graficarMonto ? "Monto" : "Cantidad de movimientos", graficarMonto);
        int i = 1;
        for (ItemReporte it : l) {
            modelo.addRow(new Object[]{i++, it.getEtiqueta(), it.getCantidad(), UI.dinero(it.getMonto()), it.getDetalle()});
            grafico.agregar(it.getEtiqueta(), graficarMonto ? it.getMonto() : it.getCantidad());
        }
    }

    private void mayorValor() {
        ListaEnlazada<Transaccion> l = analitica().transaccionesMayorValor(top());
        lblExplicacion.setText("Recorrido inverso del ArbolAVL por valor: O(log n + k)");
        modelo.setRowCount(0);
        grafico.limpiar();
        grafico.setTitulo("Valor de la transacción", true);
        int i = 1;
        for (Transaccion t : l) {
            String etq = t.getId() + " · " + t.getTipo() + " · " + t.getUsuario().getNombre();
            modelo.addRow(new Object[]{i++, etq, 1, UI.dinero(t.getValor()),
                    UI.fecha(t.getFecha()) + " · " + t.codigoOrigen() + " → " + t.codigoDestino()});
            grafico.agregar(t.getId(), t.getValor());
        }
    }

    private void montoRango() {
        LocalDateTime d = UI.leerFecha(txtDesde.getText()), h = UI.leerFecha(txtHasta.getText());
        ItemReporte r = analitica().montoTotalEnRango(d, h);
        ListaEnlazada<ItemReporte> l = new ListaEnlazada<>();
        l.agregar(r);
        items(l, "Consulta de rango en el ArbolAVL por fecha: " + UI.fecha(d) + " a " + UI.fecha(h), true);
        // detalle diario como gráfico
        grafico.limpiar();
        grafico.setTitulo("Monto movilizado por día", true);
        LocalDateTime dia = d.toLocalDate().atStartOfDay();
        int dias = 0;
        while (!dia.isAfter(h) && dias < 12) {
            LocalDateTime fin = dia.plusDays(1).minusSeconds(1);
            ItemReporte x = analitica().montoTotalEnRango(dia.isBefore(d) ? d : dia, fin.isAfter(h) ? h : fin);
            grafico.agregar(dia.toLocalDate().toString(), x.getMonto());
            dia = dia.plusDays(1);
            dias++;
        }
    }

    private void mayorActividad() {
        LocalDateTime d = UI.leerFecha(txtDesde.getText()), h = UI.leerFecha(txtHasta.getText());
        ListaEnlazada<ItemReporte> l = analitica().actividadPorUsuario(d, h, top());
        ItemReporte mayor = analitica().usuarioMayorActividad(d, h);
        items(l, mayor == null ? "Sin actividad en el período"
                : "Mayor actividad: " + mayor.getEtiqueta() + " con " + mayor.getCantidad() + " operaciones (rango del AVL por fecha + conteo hash)", false);
    }

    @Override
    public void refrescar() {
        try {
            ultimoReporte.run();
        } catch (RuntimeException ignorada) {
            // si el rango quedó inválido no se recalcula automáticamente
        }
    }
}
