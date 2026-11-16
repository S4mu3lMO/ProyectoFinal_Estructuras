package fintech.ui;

import fintech.modelo.Billetera;
import fintech.modelo.NivelRiesgo;
import fintech.modelo.Transaccion;
import fintech.modelo.Usuario;
import fintech.servicios.MotorTransacciones;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Recargas, retiros, transferencias, historial y reversión de operaciones. */
public class PanelOperaciones extends JPanel implements Refrescable {

    private static final String RECARGA = "Recargar saldo", RETIRO = "Retirar saldo", TRANSFERENCIA = "Transferir";

    private final VentanaPrincipal v;
    private final JComboBox<String> cmbOperacion = new JComboBox<>(new String[]{RECARGA, RETIRO, TRANSFERENCIA});
    private final JComboBox<Usuario> cmbUsuario = VentanaPrincipal.comboUsuarios();
    private final JComboBox<Billetera> cmbPropia = VentanaPrincipal.comboBilleteras();
    private final JComboBox<Billetera> cmbDestino = VentanaPrincipal.comboBilleteras();
    private final JTextField txtValor = UI.campo(10);
    private final JTextField txtDescripcion = UI.campo(18);
    private final JLabel lblSaldo = UI.etiqueta(" ");
    private final DefaultTableModel modelo = UI.modelo("ID", "Fecha", "Tipo", "Valor", "Comisión", "Origen", "Destino",
            "Estado", "Puntos", "Riesgo", "Descripción / motivo");
    private final JTable tabla = UI.tabla(modelo);
    private final DefaultListModel<String> modeloPila = new DefaultListModel<>();
    private boolean cargando;

    public PanelOperaciones(VentanaPrincipal v) {
        super(new BorderLayout(10, 10));
        this.v = v;
        setBackground(UI.FONDO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = UI.formulario("Operación:", cmbOperacion, "Usuario:", cmbUsuario, "Billetera del usuario:", cmbPropia,
                "Billetera destino:", cmbDestino, "Valor ($):", txtValor, "Descripción:", txtDescripcion, "", lblSaldo);
        JPanel botones = new JPanel(new GridLayout(0, 1, 4, 4));
        botones.setOpaque(false);
        botones.add(UI.botonPrimario("Ejecutar operación", this::ejecutar));
        botones.add(UI.boton("Deshacer última operación (pila)", this::deshacer));
        botones.add(UI.boton("Revertir transacción seleccionada", this::revertirSeleccionada));
        JPanel izq = new JPanel(new BorderLayout(6, 6));
        izq.setOpaque(false);
        izq.add(form, BorderLayout.NORTH);
        izq.add(UI.arriba(botones), BorderLayout.CENTER);
        JList<String> lstPila = new JList<>(modeloPila);
        lstPila.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane sp = UI.scroll(lstPila);
        sp.setPreferredSize(new Dimension(380, 200));
        izq.add(UI.seccion("Pila de operaciones reversibles (cima arriba)", sp), BorderLayout.SOUTH);
        add(UI.seccion("Nueva operación", izq), BorderLayout.WEST);

        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UI.anchos(tabla, 85, 120, 150, 90, 70, 75, 75, 90, 55, 70, 330);
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                if (!sel) {
                    String estado = String.valueOf(t.getValueAt(r, 7));
                    String riesgo = String.valueOf(t.getValueAt(r, 9));
                    comp.setForeground(Color.BLACK);
                    if ("RECHAZADA".equals(estado)) comp.setForeground(UI.ALERTA);
                    else if ("REVERTIDA".equals(estado)) comp.setForeground(Color.GRAY);
                    if (!NivelRiesgo.NINGUNO.name().equals(riesgo) && c == 9) comp.setForeground(UI.ADVERTENCIA);
                }
                return comp;
            }
        });
        add(UI.seccion("Historial de movimientos del usuario (lista enlazada, más recientes primero)", UI.scroll(tabla)), BorderLayout.CENTER);

        cmbUsuario.addActionListener(e -> {
            if (!cargando) refrescar();
        });
        cmbPropia.addActionListener(e -> actualizarSaldo());
        cmbOperacion.addActionListener(e -> cmbDestino.setEnabled(TRANSFERENCIA.equals(cmbOperacion.getSelectedItem())));
        cmbDestino.setEnabled(false);
    }

    private MotorTransacciones motor() {
        return v.getPlataforma().getMotor();
    }

    private void ejecutar() {
        UI.ejecutar(this, () -> {
            Billetera propia = (Billetera) cmbPropia.getSelectedItem();
            if (propia == null) throw new fintech.servicios.ValidacionException("El usuario no tiene billeteras");
            double valor = UI.leerValor(txtValor.getText());
            String op = (String) cmbOperacion.getSelectedItem();
            Transaccion t;
            if (RECARGA.equals(op)) t = motor().recargar(propia.getCodigo(), valor, txtDescripcion.getText());
            else if (RETIRO.equals(op)) t = motor().retirar(propia.getCodigo(), valor, txtDescripcion.getText());
            else {
                Billetera d = (Billetera) cmbDestino.getSelectedItem();
                if (d == null) throw new fintech.servicios.ValidacionException("Seleccione la billetera destino");
                t = motor().transferir(propia.getCodigo(), d.getCodigo(), valor, txtDescripcion.getText());
            }
            v.refrescarTodo();
            mostrarResultado(t);
        });
    }

    private void mostrarResultado(Transaccion t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.getId()).append(" · ").append(t.getTipo()).append(" por ").append(UI.dinero(t.getValor())).append("\n");
        sb.append("Estado: ").append(t.getEstado()).append("\n");
        if (t.esExitosa()) {
            sb.append("Puntos generados: ").append(t.getPuntosGenerados()).append("\n");
            if (t.getComision() > 0) sb.append("Comisión: ").append(UI.dinero(t.getComision())).append("\n");
        } else {
            sb.append("Motivo: ").append(t.getMotivoRechazo()).append("\n");
        }
        if (t.getNivelRiesgo() != NivelRiesgo.NINGUNO)
            sb.append("Nivel de riesgo: ").append(t.getNivelRiesgo()).append(" (ver pestaña Seguridad y auditoría)\n");
        JOptionPane.showMessageDialog(this, sb.toString(), "Resultado de la operación",
                t.esExitosa() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    private void deshacer() {
        UI.ejecutar(this, () -> {
            Usuario u = (Usuario) cmbUsuario.getSelectedItem();
            if (u == null) return;
            Transaccion rev = motor().revertirUltima(u);
            v.refrescarTodo();
            UI.info(this, "Se deshizo " + rev.getIdTransaccionRelacionada() + ". Movimiento de reversión: " + rev.getId());
        });
    }

    private void revertirSeleccionada() {
        UI.ejecutar(this, () -> {
            int r = tabla.getSelectedRow();
            if (r < 0) throw new fintech.servicios.ValidacionException("Seleccione una transacción del historial");
            String id = (String) modelo.getValueAt(tabla.convertRowIndexToModel(r), 0);
            String motivo = JOptionPane.showInputDialog(this, "Motivo de la reversión de " + id + ":", "Transferencia realizada por error");
            if (motivo == null) return;
            Transaccion rev = motor().revertirPorId(id, motivo);
            v.refrescarTodo();
            UI.info(this, "Transacción " + id + " revertida (" + rev.getId() + "). Saldos, historial y puntos actualizados.");
        });
    }

    private void actualizarSaldo() {
        Billetera b = (Billetera) cmbPropia.getSelectedItem();
        lblSaldo.setText(b == null ? " " : "Saldo actual: " + UI.dinero(b.getSaldo()) + " · " + b.getEstado());
    }

    @Override
    public void refrescar() {
        cargando = true;
        v.llenarUsuarios(cmbUsuario);
        Usuario u = (Usuario) cmbUsuario.getSelectedItem();
        v.llenarBilleteras(cmbPropia, u);
        v.llenarBilleteras(cmbDestino, null);
        cargando = false;
        actualizarSaldo();
        modelo.setRowCount(0);
        modeloPila.clear();
        if (u == null) return;
        for (Transaccion t : u.getTransacciones().inverso()) {
            String desc = t.getMotivoRechazo() != null ? t.getMotivoRechazo() : t.getDescripcion();
            if (t.getUsuario() != u) desc = "Recibida de " + t.getUsuario().getNombre() + " · " + desc;
            modelo.addRow(new Object[]{t.getId(), UI.fecha(t.getFecha()), t.getTipo(), UI.dinero(t.getValor()), UI.dinero(t.getComision()),
                    t.codigoOrigen(), t.codigoDestino(), t.getEstado(), t.getUsuario() == u ? t.getPuntosGenerados() : 0,
                    t.getNivelRiesgo(), desc});
        }
        for (Transaccion t : u.getPilaReversibles()) {
            modeloPila.addElement(String.format("%s %-22s %12s %s", t.getId(), t.getTipo(), UI.dinero(t.getValor()),
                    t.esReversible() ? "" : "(" + t.getEstado() + ")"));
        }
    }
}
