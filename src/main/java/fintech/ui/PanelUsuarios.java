package fintech.ui;

import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Usuario;
import fintech.servicios.GestorUsuarios;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Registro, modificación, búsqueda y eliminación de usuarios. */
public class PanelUsuarios extends JPanel implements Refrescable {

    private final VentanaPrincipal v;
    private final JTextField txtId = UI.campo(14);
    private final JTextField txtNombre = UI.campo(20);
    private final JTextField txtCorreo = UI.campo(20);
    private final JTextField txtTelefono = UI.campo(14);
    private final JTextField txtFiltro = UI.campo(16);
    private final DefaultTableModel modelo = UI.modelo("ID", "Nombre", "Correo", "Teléfono", "Nivel",
            "Pts acumulados", "Pts disponibles", "Billeteras", "Saldo total", "Registro");
    private final JTable tabla = UI.tabla(modelo);
    private ListaEnlazada<Usuario> mostrados;

    public PanelUsuarios(VentanaPrincipal v) {
        super(new BorderLayout(10, 10));
        this.v = v;
        setBackground(UI.FONDO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = UI.formulario("Identificación:", txtId, "Nombre completo:", txtNombre,
                "Correo:", txtCorreo, "Teléfono:", txtTelefono);
        JPanel botones = new JPanel(new GridLayout(0, 1, 4, 4));
        botones.setOpaque(false);
        botones.add(UI.botonPrimario("Registrar", this::registrar));
        botones.add(UI.boton("Modificar", this::modificar));
        botones.add(UI.boton("Buscar por ID (tabla hash)", this::buscar));
        botones.add(UI.boton("Eliminar", this::eliminar));
        botones.add(UI.boton("Limpiar", this::limpiar));
        JPanel izq = new JPanel(new BorderLayout(6, 6));
        izq.setOpaque(false);
        izq.add(form, BorderLayout.NORTH);
        izq.add(UI.arriba(botones), BorderLayout.CENTER);
        JTextArea nota = new JTextArea("La búsqueda por identificación usa la TablaHash (O(1) promedio).\n"
                + "La eliminación solo se permite con saldo total en cero y cancela\nsus operaciones programadas.");
        nota.setEditable(false);
        nota.setOpaque(false);
        nota.setFont(UI.FUENTE.deriveFont(12f));
        izq.add(nota, BorderLayout.SOUTH);
        add(UI.seccion("Datos del usuario", izq), BorderLayout.WEST);

        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.setOpaque(false);
        centro.add(UI.fila(UI.etiqueta("Buscar por nombre:"), txtFiltro, UI.boton("Filtrar", this::refrescar),
                UI.boton("Ver todos", () -> {
                    txtFiltro.setText("");
                    refrescar();
                })), BorderLayout.NORTH);
        UI.anchos(tabla, 60, 150, 180, 100, 60, 90, 90, 70, 100, 120);
        centro.add(UI.scroll(tabla), BorderLayout.CENTER);
        add(UI.seccion("Usuarios registrados", centro), BorderLayout.CENTER);

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || tabla.getSelectedRow() < 0) return;
            Usuario u = v.getPlataforma().getUsuarios().buscar((String) modelo.getValueAt(tabla.convertRowIndexToModel(tabla.getSelectedRow()), 0));
            if (u != null) cargar(u);
        });
    }

    private GestorUsuarios gestor() {
        return v.getPlataforma().getUsuarios();
    }

    private void registrar() {
        UI.ejecutar(this, () -> {
            Usuario u = gestor().registrar(txtId.getText(), txtNombre.getText(), txtCorreo.getText(), txtTelefono.getText());
            UI.info(this, "Usuario " + u.getNombre() + " registrado. Ahora puede crearle billeteras.");
            limpiar();
            v.refrescarTodo();
        });
    }

    private void modificar() {
        UI.ejecutar(this, () -> {
            gestor().modificar(txtId.getText(), txtNombre.getText(), txtCorreo.getText(), txtTelefono.getText());
            UI.info(this, "Datos actualizados");
            v.refrescarTodo();
        });
    }

    private void buscar() {
        UI.ejecutar(this, () -> {
            Usuario u = gestor().obtener(txtId.getText());
            cargar(u);
            for (int i = 0; i < tabla.getRowCount(); i++) {
                if (u.getId().equals(tabla.getValueAt(i, 0))) {
                    tabla.setRowSelectionInterval(i, i);
                    tabla.scrollRectToVisible(tabla.getCellRect(i, 0, true));
                }
            }
        });
    }

    private void eliminar() {
        UI.ejecutar(this, () -> {
            Usuario u = gestor().obtener(txtId.getText());
            int r = JOptionPane.showConfirmDialog(this, "¿Eliminar a " + u.getNombre() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
            v.getPlataforma().eliminarUsuario(u.getId());
            limpiar();
            v.refrescarTodo();
        });
    }

    private void cargar(Usuario u) {
        txtId.setText(u.getId());
        txtNombre.setText(u.getNombre());
        txtCorreo.setText(u.getCorreo());
        txtTelefono.setText(u.getTelefono());
    }

    private void limpiar() {
        txtId.setText("");
        txtNombre.setText("");
        txtCorreo.setText("");
        txtTelefono.setText("");
        tabla.clearSelection();
    }

    @Override
    public void refrescar() {
        mostrados = txtFiltro.getText().isBlank() ? gestor().todos() : gestor().buscarPorNombre(txtFiltro.getText());
        modelo.setRowCount(0);
        for (Usuario u : mostrados) {
            modelo.addRow(new Object[]{u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getNivel(),
                    u.getPuntosAcumulados(), u.getPuntosDisponibles(), u.getBilleteras().tamano(),
                    UI.dinero(u.saldoTotal()), UI.fecha(u.getFechaRegistro())});
        }
    }
}
