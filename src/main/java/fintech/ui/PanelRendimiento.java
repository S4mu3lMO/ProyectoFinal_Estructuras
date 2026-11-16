package fintech.ui;

import fintech.estructuras.ListaEnlazada;
import fintech.servicios.ComparadorRendimiento.Resultado;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Comparación del rendimiento de distintas estructuras en operaciones similares. */
public class PanelRendimiento extends JPanel implements Refrescable {

    private final VentanaPrincipal v;
    private final JSpinner spN = new JSpinner(new SpinnerNumberModel(20_000, 1_000, 200_000, 1_000));
    private final DefaultTableModel modelo = UI.modelo("Escenario", "Estructura", "n", "Tiempo (ms)", "Complejidad", "Verificación");
    private final JTable tabla = UI.tabla(modelo);
    private final GraficoBarras grafico = new GraficoBarras();
    private final JLabel lblEstado = UI.etiqueta("Presione «Ejecutar comparación». Cada caso se mide después de una corrida de calentamiento.");
    private final JButton btnEjecutar;
    private ListaEnlazada<Resultado> ultimos = new ListaEnlazada<>();

    public PanelRendimiento(VentanaPrincipal v) {
        super(new BorderLayout(10, 10));
        this.v = v;
        setBackground(UI.FONDO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        btnEjecutar = UI.botonPrimario("Ejecutar comparación", this::ejecutar);
        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(UI.fila(UI.etiqueta("Cantidad de registros n:"), spN, btnEjecutar), BorderLayout.NORTH);
        norte.add(lblEstado, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        UI.anchos(tabla, 260, 240, 70, 90, 110, 260);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0)
                graficar((String) modelo.getValueAt(tabla.convertRowIndexToModel(tabla.getSelectedRow()), 0));
        });
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, UI.seccion("Resultados", UI.scroll(tabla)),
                UI.seccion("Comparación del escenario seleccionado", grafico));
        split.setResizeWeight(0.62);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    private void ejecutar() {
        int n = (Integer) spN.getValue();
        btnEjecutar.setEnabled(false);
        lblEstado.setText("Ejecutando con n = " + n + "…");
        new SwingWorker<ListaEnlazada<Resultado>, Void>() {
            @Override
            protected ListaEnlazada<Resultado> doInBackground() {
                return v.getPlataforma().getRendimiento().ejecutar(n, 42);
            }

            @Override
            protected void done() {
                btnEjecutar.setEnabled(true);
                try {
                    ultimos = get();
                } catch (Exception ex) {
                    lblEstado.setText("Error: " + ex.getMessage());
                    return;
                }
                modelo.setRowCount(0);
                for (Resultado r : ultimos)
                    modelo.addRow(new Object[]{r.getEscenario(), r.getEstructura(), r.getN(),
                            Double.valueOf(String.format(java.util.Locale.US, "%.3f", r.getMilisegundos())), r.getComplejidad(), r.getVerificacion()});
                lblEstado.setText("Listo. Seleccione una fila para ver el gráfico de su escenario. (Los tiempos varían según el equipo.)");
                if (!ultimos.estaVacia()) graficar(ultimos.primero().getEscenario());
            }
        }.execute();
    }

    private void graficar(String escenario) {
        grafico.limpiar();
        grafico.setTitulo(escenario + " — ms", false);
        for (Resultado r : ultimos) if (r.getEscenario().equals(escenario)) grafico.agregar(r.getEstructura(), r.getMilisegundos());
        grafico.setTitulo(escenario + " (milisegundos)", false);
        grafico.setFormato("%,.3f ms");
        grafico.repaint();
    }

    @Override
    public void refrescar() {
        // los resultados solo cambian al ejecutar la comparación
    }
}
