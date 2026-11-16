package fintech.ui;

import fintech.modelo.Billetera;
import fintech.modelo.EstadoBilletera;
import fintech.modelo.TipoBilletera;
import fintech.modelo.Transaccion;
import fintech.modelo.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Creación y administración de múltiples billeteras por usuario. */
public class PanelBilleteras extends JPanel implements Refrescable {

    private final VentanaPrincipal v;
    private final JComboBox<Usuario> cmbUsuario = VentanaPrincipal.comboUsuarios();
    private final JTextField txtNombre = UI.campo(16);
    private final JComboBox<TipoBilletera> cmbTipo = new JComboBox<>(TipoBilletera.values());
    private final JTextField txtAlerta = UI.campo(8);
    private final JComboBox<EstadoBilletera> cmbEstado = new JComboBox<>(EstadoBilletera.values());
    private final JTextField txtCodigo = UI.campo(10);
    private final JCheckBox chkTodas = new JCheckBox("Ver billeteras de todos los usuarios");
    private final DefaultTableModel modelo = UI.modelo("Código", "Nombre", "Tipo", "Usuario", "Saldo", "Estado", "Movimientos", "Alerta saldo <", "Creada");
    private final JTable tabla = UI.tabla(modelo);
    private final DefaultTableModel modeloHist = UI.modelo("ID", "Fecha", "Tipo", "Valor", "Origen", "Destino", "Estado", "Puntos", "Descripción");
    private final JTable tablaHist = UI.tabla(modeloHist);
    private final JLabel lblHist = UI.etiqueta("Seleccione una billetera para ver su historial");
    private boolean cargando;

    public PanelBilleteras(VentanaPrincipal v) {
        super(new BorderLayout(10, 10));
        this.v = v;
        setBackground(UI.FONDO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtAlerta.setText("100");
        chkTodas.setOpaque(false);

        JPanel form = UI.formulario("Usuario:", cmbUsuario, "Nombre de la billetera:", txtNombre, "Tipo:", cmbTipo,
                "Alerta de saldo bajo ($):", txtAlerta, "Estado:", cmbEstado);
        JPanel botones = new JPanel(new GridLayout(0, 1, 4, 4));
        botones.setOpaque(false);
        botones.add(UI.botonPrimario("Crear billetera", this::crear));
        botones.add(UI.boton("Guardar cambios (nombre/tipo/alerta)", this::modificar));
        botones.add(UI.boton("Aplicar estado", this::cambiarEstado));
        botones.add(UI.boton("Eliminar billetera (saldo 0)", this::eliminar));
        JPanel izq = new JPanel(new BorderLayout(6, 6));
        izq.setOpaque(false);
        izq.add(form, BorderLayout.NORTH);
        izq.add(UI.arriba(botones), BorderLayout.CENTER);
        izq.add(UI.fila(UI.etiqueta("Código:"), txtCodigo, UI.boton("Localizar", this::localizar)), BorderLayout.SOUTH);
        add(UI.seccion("Billetera", izq), BorderLayout.WEST);

        JPanel arriba = new JPanel(new BorderLayout());
        arriba.setOpaque(false);
        arriba.add(chkTodas, BorderLayout.NORTH);
        UI.anchos(tabla, 70, 130, 100, 130, 90, 80, 80, 90, 120);
        arriba.add(UI.scroll(tabla), BorderLayout.CENTER);
        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        abajo.add(lblHist, BorderLayout.NORTH);
        UI.anchos(tablaHist, 80, 120, 140, 90, 70, 70, 80, 50, 220);
        abajo.add(UI.scroll(tablaHist), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, UI.seccion("Billeteras", arriba),
                UI.seccion("Historial de la billetera (lista enlazada por billetera)", abajo));
        split.setResizeWeight(0.5);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        cmbUsuario.addActionListener(e -> {
            if (!cargando) refrescar();
        });
        chkTodas.addActionListener(e -> refrescar());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarSeleccionada();
        });
    }

    private Billetera seleccionada() {
        int r = tabla.getSelectedRow();
        if (r < 0) return null;
        return v.getPlataforma().getBilleteras().buscar((String) modelo.getValueAt(tabla.convertRowIndexToModel(r), 0));
    }

    private Billetera requerirSeleccion() {
        Billetera b = seleccionada();
        if (b == null) throw new fintech.servicios.ValidacionException("Seleccione una billetera en la tabla");
        return b;
    }

    private void crear() {
        UI.ejecutar(this, () -> {
            Usuario u = (Usuario) cmbUsuario.getSelectedItem();
            Billetera b = v.getPlataforma().getBilleteras().crear(u, txtNombre.getText(), (TipoBilletera) cmbTipo.getSelectedItem());
            b.setSaldoMinimoAlerta(UI.leerValor(txtAlerta.getText()));
            txtNombre.setText("");
            v.refrescarTodo();
            UI.info(this, "Billetera creada con código " + b.getCodigo());
        });
    }

    private void modificar() {
        UI.ejecutar(this, () -> {
            Billetera b = requerirSeleccion();
            v.getPlataforma().getBilleteras().modificar(b.getCodigo(), txtNombre.getText(), (TipoBilletera) cmbTipo.getSelectedItem());
            b.setSaldoMinimoAlerta(UI.leerValor(txtAlerta.getText()));
            v.refrescarTodo();
        });
    }

    private void cambiarEstado() {
        UI.ejecutar(this, () -> {
            Billetera b = requerirSeleccion();
            v.getPlataforma().getBilleteras().cambiarEstado(b.getCodigo(), (EstadoBilletera) cmbEstado.getSelectedItem());
            v.refrescarTodo();
        });
    }

    private void eliminar() {
        UI.ejecutar(this, () -> {
            Billetera b = requerirSeleccion();
            v.getPlataforma().getBilleteras().eliminar(b.getCodigo());
            v.refrescarTodo();
        });
    }

    private void localizar() {
        UI.ejecutar(this, () -> {
            Billetera b = v.getPlataforma().getBilleteras().obtener(txtCodigo.getText());
            chkTodas.setSelected(true);
            refrescar();
            for (int i = 0; i < tabla.getRowCount(); i++) {
                if (b.getCodigo().equals(tabla.getValueAt(i, 0))) {
                    tabla.setRowSelectionInterval(i, i);
                    tabla.scrollRectToVisible(tabla.getCellRect(i, 0, true));
                }
            }
        });
    }

    private void mostrarSeleccionada() {
        Billetera b = seleccionada();
        modeloHist.setRowCount(0);
        if (b == null) {
            lblHist.setText("Seleccione una billetera para ver su historial");
            return;
        }
        txtNombre.setText(b.getNombre());
        cmbTipo.setSelectedItem(b.getTipo());
        cmbEstado.setSelectedItem(b.getEstado());
        txtAlerta.setText(String.valueOf(b.getSaldoMinimoAlerta()));
        lblHist.setText(b.getCodigo() + " · " + b.getNombre() + " · saldo " + UI.dinero(b.getSaldo()) + " · " + b.getTransacciones().tamano() + " movimientos (más recientes primero)");
        for (Transaccion t : b.getTransacciones().inverso()) {
            modeloHist.addRow(new Object[]{t.getId(), UI.fecha(t.getFecha()), t.getTipo(), UI.dinero(t.getValor()),
                    t.codigoOrigen(), t.codigoDestino(), t.getEstado(), t.getPuntosGenerados(), t.getDescripcion()});
        }
    }

    @Override
    public void refrescar() {
        cargando = true;
        v.llenarUsuarios(cmbUsuario);
        cargando = false;
        Usuario u = (Usuario) cmbUsuario.getSelectedItem();
        String sel = seleccionada() == null ? null : seleccionada().getCodigo();
        modelo.setRowCount(0);
        Iterable<Billetera> lista = chkTodas.isSelected() || u == null ? v.getPlataforma().getBilleteras().todas() : u.getBilleteras();
        for (Billetera b : lista) {
            modelo.addRow(new Object[]{b.getCodigo(), b.getNombre(), b.getTipo(), b.getUsuario().getNombre(), UI.dinero(b.getSaldo()),
                    b.getEstado(), b.getTransacciones().tamano(), UI.dinero(b.getSaldoMinimoAlerta()), UI.fecha(b.getFechaCreacion())});
        }
        if (sel != null) {
            for (int i = 0; i < tabla.getRowCount(); i++) if (sel.equals(tabla.getValueAt(i, 0))) tabla.setRowSelectionInterval(i, i);
        }
        mostrarSeleccionada();
    }
}
