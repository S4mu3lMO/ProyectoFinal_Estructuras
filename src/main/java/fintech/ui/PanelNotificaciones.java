package fintech.ui;

import fintech.modelo.Notificacion;
import fintech.modelo.TipoNotificacion;
import fintech.modelo.Usuario;
import fintech.servicios.GestorNotificaciones;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Cola de notificaciones pendientes e historial reciente por usuario. */
public class PanelNotificaciones extends JPanel implements Refrescable {

    private final VentanaPrincipal v;
    private final JCheckBox chkAuto = new JCheckBox("Despacho automático (al generarse)");
    private final JLabel lblContadores = UI.etiqueta("");
    private final DefaultTableModel modeloCola = UI.modelo("Turno", "ID", "Generada", "Usuario", "Tipo", "Mensaje");
    private final JComboBox<Usuario> cmbUsuario = VentanaPrincipal.comboUsuarios();
    private final JComboBox<Object> cmbTipo = new JComboBox<>();
    private final DefaultTableModel modeloHist = UI.modelo("ID", "Generada", "Despachada", "Tipo", "Mensaje");
    private boolean cargando;

    public PanelNotificaciones(VentanaPrincipal v) {
        super(new BorderLayout(10, 10));
        this.v = v;
        setBackground(UI.FONDO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chkAuto.setOpaque(false);
        chkAuto.setFont(UI.FUENTE);
        cmbTipo.addItem("Todos los tipos");
        for (TipoNotificacion t : TipoNotificacion.values()) cmbTipo.addItem(t);

        JPanel cola = new JPanel(new BorderLayout(4, 4));
        cola.setOpaque(false);
        cola.add(UI.fila(chkAuto, UI.botonPrimario("Despachar siguiente", () -> {
            Notificacion n = gestor().despacharSiguiente();
            v.refrescarTodo();
            if (n == null) UI.info(this, "No hay notificaciones pendientes");
        }), UI.boton("Despachar todas", () -> {
            int n = gestor().despacharTodas();
            v.refrescarTodo();
            UI.info(this, n + " notificaciones despachadas");
        }), lblContadores), BorderLayout.NORTH);
        JTable tCola = UI.tabla(modeloCola);
        UI.anchos(tCola, 45, 85, 115, 140, 170, 520);
        cola.add(UI.scroll(tCola), BorderLayout.CENTER);

        JPanel hist = new JPanel(new BorderLayout(4, 4));
        hist.setOpaque(false);
        hist.add(UI.fila(UI.etiqueta("Usuario:"), cmbUsuario, UI.etiqueta("Tipo:"), cmbTipo), BorderLayout.NORTH);
        JTable tHist = UI.tabla(modeloHist);
        UI.anchos(tHist, 85, 115, 115, 170, 600);
        hist.add(UI.scroll(tHist), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UI.seccion("Cola FIFO de notificaciones pendientes de despacho", cola),
                UI.seccion("Historial de notificaciones recientes del usuario (últimas " + Usuario.MAX_NOTIFICACIONES_RECIENTES + ")", hist));
        split.setResizeWeight(0.4);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        chkAuto.addActionListener(e -> {
            gestor().setDespachoAutomatico(chkAuto.isSelected());
            if (chkAuto.isSelected()) gestor().despacharTodas();
            v.refrescarTodo();
        });
        cmbUsuario.addActionListener(e -> {
            if (!cargando) refrescar();
        });
        cmbTipo.addActionListener(e -> refrescar());
    }

    private GestorNotificaciones gestor() {
        return v.getPlataforma().getNotificaciones();
    }

    @Override
    public void refrescar() {
        cargando = true;
        v.llenarUsuarios(cmbUsuario);
        cargando = false;
        chkAuto.setSelected(gestor().isDespachoAutomatico());
        lblContadores.setText("   En cola: " + gestor().getPendientes().tamano() + " · Generadas: " + gestor().getTotalGeneradas()
                + " · Despachadas: " + gestor().getTotalDespachadas());
        modeloCola.setRowCount(0);
        int i = 1;
        for (Notificacion n : gestor().getPendientes())
            modeloCola.addRow(new Object[]{i++, n.getId(), UI.fecha(n.getFecha()), n.getUsuario().getNombre(), n.getTipo(), n.getMensaje()});
        modeloHist.setRowCount(0);
        Usuario u = (Usuario) cmbUsuario.getSelectedItem();
        Object filtro = cmbTipo.getSelectedItem();
        if (u != null) {
            for (Notificacion n : u.notificacionesRecientes().inverso()) {
                if (filtro instanceof TipoNotificacion && n.getTipo() != filtro) continue;
                modeloHist.addRow(new Object[]{n.getId(), UI.fecha(n.getFecha()), UI.fecha(n.getFechaDespacho()), n.getTipo(), n.getMensaje()});
            }
        }
    }
}
