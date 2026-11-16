package fintech.ui;

import fintech.modelo.Billetera;
import fintech.modelo.Frecuencia;
import fintech.modelo.OperacionProgramada;
import fintech.modelo.TipoOperacionProgramada;
import fintech.modelo.Usuario;
import fintech.servicios.GestorProgramadas;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.temporal.ChronoUnit;

/** Programación de operaciones futuras y simulación de su ejecución automática. */
public class PanelProgramadas extends JPanel implements Refrescable {

    private final VentanaPrincipal v;
    private final JComboBox<Usuario> cmbUsuario = VentanaPrincipal.comboUsuarios();
    private final JComboBox<TipoOperacionProgramada> cmbTipo = new JComboBox<>(TipoOperacionProgramada.values());
    private final JComboBox<Billetera> cmbOrigen = VentanaPrincipal.comboBilleteras();
    private final JComboBox<Billetera> cmbDestino = VentanaPrincipal.comboBilleteras();
    private final JTextField txtValor = UI.campo(10);
    private final JTextField txtFecha = UI.campo(14);
    private final JComboBox<Frecuencia> cmbFrecuencia = new JComboBox<>(Frecuencia.values());
    private final JTextField txtRepeticiones = UI.campo(5);
    private final JTextField txtDescripcion = UI.campo(16);
    private final JSpinner spDias = new JSpinner(new SpinnerNumberModel(7, 1, 365, 1));
    private final DefaultTableModel modelo = UI.modelo("Orden", "ID", "Ejecución", "Usuario (nivel)", "Tipo", "Origen", "Destino",
            "Valor", "Frecuencia", "Restantes", "Éxitos / fallos", "Descripción");
    private final JTable tabla = UI.tabla(modelo);
    private final JTextArea txtBitacora = UI.areaTexto();
    private boolean cargando;

    public PanelProgramadas(VentanaPrincipal v) {
        super(new BorderLayout(10, 10));
        this.v = v;
        setBackground(UI.FONDO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtRepeticiones.setText("-1");
        txtRepeticiones.setToolTipText("-1 = indefinidas");

        JPanel form = UI.formulario("Usuario:", cmbUsuario, "Tipo:", cmbTipo, "Billetera origen:", cmbOrigen,
                "Billetera destino:", cmbDestino, "Valor ($):", txtValor, "Fecha (yyyy-MM-dd HH:mm):", txtFecha,
                "Frecuencia:", cmbFrecuencia, "Repeticiones (-1 = sin fin):", txtRepeticiones, "Descripción:", txtDescripcion);
        JPanel botones = new JPanel(new GridLayout(0, 1, 4, 4));
        botones.setOpaque(false);
        botones.add(UI.botonPrimario("Programar operación", this::programar));
        botones.add(UI.boton("Cancelar operación seleccionada", this::cancelar));
        botones.add(UI.boton("Procesar vencidas (hora actual)", () -> UI.ejecutar(this, () -> {
            int n = gestor().procesarPendientes();
            v.refrescarTodo();
            UI.info(this, n + " operaciones ejecutadas");
        })));
        JPanel sim = UI.fila(UI.etiqueta("Simular"), spDias, UI.etiqueta("días"), UI.botonPrimario("Ejecutar simulación", this::simular));
        JPanel izq = new JPanel(new BorderLayout(6, 6));
        izq.setOpaque(false);
        izq.add(form, BorderLayout.NORTH);
        izq.add(UI.arriba(botones), BorderLayout.CENTER);
        izq.add(UI.seccion("Simulación automática", sim), BorderLayout.SOUTH);
        add(UI.seccion("Programar movimiento", izq), BorderLayout.WEST);

        UI.anchos(tabla, 45, 75, 115, 150, 140, 70, 70, 80, 70, 65, 80, 180);
        JScrollPane spBit = UI.scroll(txtBitacora);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UI.seccion("Cola de prioridad (montículo): fecha de ejecución → prioridad del nivel → orden de creación", UI.scroll(tabla)),
                UI.seccion("Bitácora de ejecución", spBit));
        split.setResizeWeight(0.62);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        cmbUsuario.addActionListener(e -> {
            if (!cargando) refrescar();
        });
        cmbTipo.addActionListener(e -> cmbOrigen.setEnabled(((TipoOperacionProgramada) cmbTipo.getSelectedItem()).requiereOrigen()));
    }

    private GestorProgramadas gestor() {
        return v.getPlataforma().getProgramadas();
    }

    private void programar() {
        UI.ejecutar(this, () -> {
            Usuario u = (Usuario) cmbUsuario.getSelectedItem();
            if (u == null) throw new fintech.servicios.ValidacionException("Seleccione un usuario");
            TipoOperacionProgramada tipo = (TipoOperacionProgramada) cmbTipo.getSelectedItem();
            Billetera o = (Billetera) cmbOrigen.getSelectedItem();
            Billetera d = (Billetera) cmbDestino.getSelectedItem();
            OperacionProgramada op = gestor().programar(u.getId(), tipo, tipo.requiereOrigen() && o != null ? o.getCodigo() : null,
                    d == null ? null : d.getCodigo(), UI.leerValor(txtValor.getText()), UI.leerFecha(txtFecha.getText()),
                    (Frecuencia) cmbFrecuencia.getSelectedItem(), UI.leerEntero(txtRepeticiones.getText(), "Repeticiones"),
                    txtDescripcion.getText());
            v.refrescarTodo();
            UI.info(this, "Operación " + op.getId() + " programada para " + UI.fecha(op.getFechaEjecucion()));
        });
    }

    private void cancelar() {
        UI.ejecutar(this, () -> {
            int r = tabla.getSelectedRow();
            if (r < 0) throw new fintech.servicios.ValidacionException("Seleccione una operación de la tabla");
            gestor().cancelar((String) modelo.getValueAt(tabla.convertRowIndexToModel(r), 1));
            v.refrescarTodo();
        });
    }

    private void simular() {
        UI.ejecutar(this, () -> {
            int n = gestor().simularDias((Integer) spDias.getValue());
            v.refrescarTodo();
            UI.info(this, "Simulación terminada: " + n + " operaciones ejecutadas. Nueva fecha: " + UI.fecha(v.getPlataforma().getReloj().ahora()));
        });
    }

    @Override
    public void refrescar() {
        cargando = true;
        v.llenarUsuarios(cmbUsuario);
        Usuario u = (Usuario) cmbUsuario.getSelectedItem();
        v.llenarBilleteras(cmbOrigen, u);
        v.llenarBilleteras(cmbDestino, null);
        cargando = false;
        if (txtFecha.getText().isBlank())
            txtFecha.setText(UI.fecha(v.getPlataforma().getReloj().ahora().plusHours(1).truncatedTo(ChronoUnit.HOURS)));
        modelo.setRowCount(0);
        int i = 1;
        for (OperacionProgramada op : gestor().pendientesEnOrden()) {
            modelo.addRow(new Object[]{i++, op.getId(), UI.fecha(op.getFechaEjecucion()),
                    op.getUsuario().getNombre() + " (" + op.getUsuario().getNivel() + ")", op.getTipo(),
                    op.getBilleteraOrigen() == null ? "EXTERNO" : op.getBilleteraOrigen().getCodigo(),
                    op.getBilleteraDestino().getCodigo(), UI.dinero(op.getValor()), op.getFrecuencia(),
                    op.getRepeticionesRestantes() < 0 ? "∞" : String.valueOf(op.getRepeticionesRestantes()),
                    op.getEjecucionesExitosas() + " / " + op.getEjecucionesFallidas(), op.getDescripcion()});
        }
        StringBuilder sb = new StringBuilder();
        for (String linea : gestor().getBitacora().inverso()) sb.append(linea).append('\n');
        txtBitacora.setText(sb.toString());
        txtBitacora.setCaretPosition(0);
    }
}
