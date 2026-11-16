package fintech.ui;

import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Billetera;
import fintech.modelo.Usuario;
import fintech.servicios.DatosDemo;
import fintech.servicios.PlataformaFintech;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Ventana principal: encabezado con el reloj simulado y pestañas por módulo. */
public class VentanaPrincipal extends JFrame {

    private PlataformaFintech plataforma;
    private final JTabbedPane pestanas = new JTabbedPane();
    private final ListaEnlazada<Refrescable> lstPanel = new ListaEnlazada<>();
    private final JLabel lblReloj = new JLabel();
    private final JLabel lblEstado = new JLabel();
    private boolean refrescando;

    public VentanaPrincipal(PlataformaFintech plataforma) {
        super("Billeteras Digitales — Plataforma Fintech (Estructuras de Datos)");
        this.plataforma = plataforma;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1360, 820);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
        construir();
        refrescarTodo();
    }

    private void construir() {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(UI.FONDO);
        raiz.add(encabezado(), BorderLayout.NORTH);

        pestanas.setFont(UI.FUENTE_NEGRITA);
        agregar("Usuarios", new PanelUsuarios(this));
        agregar("Billeteras", new PanelBilleteras(this));
        agregar("Operaciones", new PanelOperaciones(this));
        agregar("Programadas", new PanelProgramadas(this));
        agregar("Recompensas y niveles", new PanelRecompensas(this));
        agregar("Notificaciones", new PanelNotificaciones(this));
        agregar("Analítica", new PanelAnalitica(this));
        agregar("Grafo de transferencias", new PanelGrafo(this));
        agregar("Seguridad y auditoría", new PanelSeguridad(this));
        agregar("Rendimiento", new PanelRendimiento(this));
        pestanas.addChangeListener(e -> refrescarTodo());
        raiz.add(pestanas, BorderLayout.CENTER);

        lblEstado.setFont(UI.FUENTE);
        lblEstado.setBorder(new EmptyBorder(4, 10, 4, 10));
        raiz.add(lblEstado, BorderLayout.SOUTH);
        setContentPane(raiz);
        setJMenuBar(menu());
    }

    private void agregar(String titulo, JComponent panel) {
        pestanas.addTab(titulo, panel);
        lstPanel.agregar((Refrescable) panel);
    }

    private JComponent encabezado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UI.PRIMARIO);
        p.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel titulo = new JLabel("Plataforma Fintech · Billeteras Digitales");
        titulo.setFont(UI.FUENTE_TITULO);
        titulo.setForeground(Color.WHITE);
        p.add(titulo, BorderLayout.WEST);

        lblReloj.setForeground(Color.WHITE);
        lblReloj.setFont(UI.FUENTE_NEGRITA);
        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        der.setOpaque(false);
        der.add(lblReloj);
        der.add(UI.boton("+1 hora", () -> avanzar(0, 1)));
        der.add(UI.boton("+1 día", () -> avanzar(1, 0)));
        der.add(UI.boton("+7 días", () -> avanzar(7, 0)));
        p.add(der, BorderLayout.EAST);
        return p;
    }

    private void avanzar(int dias, int horas) {
        UI.ejecutar(this, () -> {
            int n = plataforma.avanzarTiempo(dias, horas);
            refrescarTodo();
            lblEstado.setText(lblEstado.getText() + "   ·   Simulación: " + n + " operaciones programadas ejecutadas");
        });
    }

    private JMenuBar menu() {
        JMenuBar mb = new JMenuBar();
        JMenu datos = new JMenu("Datos");
        JMenuItem demo = new JMenuItem("Reiniciar con datos de demostración");
        demo.addActionListener(e -> reiniciar(true));
        JMenuItem vacio = new JMenuItem("Reiniciar plataforma vacía");
        vacio.addActionListener(e -> reiniciar(false));
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> System.exit(0));
        datos.add(demo);
        datos.add(vacio);
        datos.addSeparator();
        datos.add(salir);
        JMenu ayuda = new JMenu("Ayuda");
        JMenuItem acerca = new JMenuItem("Acerca de");
        acerca.addActionListener(e -> UI.info(this,
                "Proyecto Final — Estructuras de Datos\n"
                        + "Plataforma Fintech de Gestión de Billeteras Digitales y Analítica de Transacciones\n\n"
                        + "Estructuras propias: ListaEnlazada, Pila, Cola, ColaPrioridad (heap),\n"
                        + "ArbolAVL, TablaHash y Grafo dirigido ponderado.\n\n"
                        + "El reloj del encabezado es simulado: use +1 hora / +1 día / +7 días\n"
                        + "para ejecutar automáticamente las operaciones programadas."));
        ayuda.add(acerca);
        mb.add(datos);
        mb.add(ayuda);
        return mb;
    }

    private void reiniciar(boolean conDemo) {
        int r = JOptionPane.showConfirmDialog(this, "Se perderán los datos actuales. ¿Continuar?", "Reiniciar", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        PlataformaFintech nueva = new PlataformaFintech();
        if (conDemo) DatosDemo.cargar(nueva);
        dispose();
        new VentanaPrincipal(nueva).setVisible(true);
    }

    /** Vuelve a pintar el panel visible y el encabezado. */
    public void refrescarTodo() {
        if (refrescando) return;
        refrescando = true;
        try {
            Component visible = pestanas.getSelectedComponent();
            if (visible instanceof Refrescable) ((Refrescable) visible).refrescar();
            lblReloj.setText("Fecha simulada: " + UI.fecha(plataforma.getReloj().ahora()) + "   ");
            lblEstado.setText(String.format("Usuarios: %d   ·   Billeteras: %d   ·   Transacciones: %d   ·   Programadas pendientes: %d   ·   Notificaciones en cola: %d   ·   Eventos de auditoría: %d",
                    plataforma.getUsuarios().cantidad(), plataforma.getBilleteras().cantidad(),
                    plataforma.getMotor().getTransacciones().tamano(), plataforma.getProgramadas().pendientesEnOrden().tamano(),
                    plataforma.getNotificaciones().getPendientes().tamano(), plataforma.getDetector().getAuditoria().tamano()));
        } finally {
            refrescando = false;
        }
    }

    public PlataformaFintech getPlataforma() {
        return plataforma;
    }

    // ------------------------------------------------------------ utilidades de combos
    public void llenarUsuarios(JComboBox<Usuario> combo) {
        Object sel = combo.getSelectedItem();
        combo.removeAllItems();
        for (Usuario u : plataforma.getUsuarios().todos()) combo.addItem(u);
        if (sel != null) combo.setSelectedItem(sel);
        if (combo.getSelectedIndex() < 0 && combo.getItemCount() > 0) combo.setSelectedIndex(0);
    }

    /** Llena con las billeteras de un usuario, o todas si u es null. */
    public void llenarBilleteras(JComboBox<Billetera> combo, Usuario u) {
        Object sel = combo.getSelectedItem();
        combo.removeAllItems();
        ListaEnlazada<Billetera> l = u == null ? plataforma.getBilleteras().todas() : u.getBilleteras();
        for (Billetera b : l) combo.addItem(b);
        if (sel != null) combo.setSelectedItem(sel);
        if (combo.getSelectedIndex() < 0 && combo.getItemCount() > 0) combo.setSelectedIndex(0);
    }

    public static JComboBox<Billetera> comboBilleteras() {
        JComboBox<Billetera> c = new JComboBox<>();
        c.setFont(UI.FUENTE);
        c.setPreferredSize(new Dimension(300, 26));
        c.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Billetera) {
                    Billetera b = (Billetera) value;
                    setText(b.getCodigo() + " · " + b.getNombre() + " · " + b.getUsuario().getNombre() + " · " + UI.dinero(b.getSaldo()));
                }
                return this;
            }
        });
        return c;
    }

    public static JComboBox<Usuario> comboUsuarios() {
        JComboBox<Usuario> c = new JComboBox<>();
        c.setFont(UI.FUENTE);
        c.setPreferredSize(new Dimension(230, 26));
        return c;
    }
}
