package fintech.ui;

import fintech.servicios.ValidacionException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Utilidades de presentación compartidas por todos los paneles. */
public final class UI {

    public static final Color PRIMARIO = new Color(0x1F, 0x4E, 0x79);
    public static final Color PRIMARIO_CLARO = new Color(0xDD, 0xEB, 0xF7);
    public static final Color FONDO = new Color(0xF5, 0xF7, 0xFA);
    public static final Color EXITO = new Color(0x2E, 0x7D, 0x32);
    public static final Color ALERTA = new Color(0xC6, 0x28, 0x28);
    public static final Color ADVERTENCIA = new Color(0xEF, 0x8F, 0x00);
    public static final Font FUENTE = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FUENTE_NEGRITA = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FUENTE_TITULO = new Font("SansSerif", Font.BOLD, 18);

    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private UI() {
    }

    public static String dinero(double v) {
        return String.format("$%,.2f", v);
    }

    public static String fecha(LocalDateTime f) {
        return f == null ? "" : f.format(FECHA_HORA);
    }

    public static LocalDateTime leerFecha(String texto) {
        try {
            return LocalDateTime.parse(texto.trim(), FECHA_HORA);
        } catch (DateTimeParseException e) {
            throw new ValidacionException("Fecha inválida '" + texto + "'. Use el formato yyyy-MM-dd HH:mm");
        }
    }

    public static double leerValor(String texto) {
        try {
            double v = Double.parseDouble(texto.trim().replace(",", ""));
            if (v <= 0) throw new ValidacionException("El valor debe ser mayor que cero");
            return v;
        } catch (NumberFormatException e) {
            throw new ValidacionException("Valor numérico inválido: '" + texto + "'");
        }
    }

    public static int leerEntero(String texto, String campo) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new ValidacionException(campo + " debe ser un número entero");
        }
    }

    /** Ejecuta una acción mostrando los errores de validación como diálogo. */
    public static boolean ejecutar(Component padre, Runnable accion) {
        try {
            accion.run();
            return true;
        } catch (ValidacionException | IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(padre, e.getMessage(), "Operación no válida", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    public static void info(Component padre, String msg) {
        JOptionPane.showMessageDialog(padre, msg, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    public static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                // permite ordenar numéricamente las columnas con números
                if (getRowCount() > 0 && getValueAt(0, c) != null) return getValueAt(0, c).getClass();
                return Object.class;
            }
        };
    }

    public static JTable tabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setFont(FUENTE);
        t.setRowHeight(24);
        t.setAutoCreateRowSorter(true);
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setFont(FUENTE_NEGRITA);
        t.getTableHeader().setBackground(PRIMARIO_CLARO);
        t.setGridColor(new Color(0xE0, 0xE4, 0xEA));
        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(SwingConstants.RIGHT);
        t.setDefaultRenderer(Number.class, derecha);
        return t;
    }

    /** Coloca el componente arriba, sin estirarlo verticalmente. */
    public static JPanel arriba(Component c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.NORTH);
        return p;
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.getViewport().setBackground(Color.WHITE);
        return s;
    }

    public static JPanel seccion(String titulo, Component contenido) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        TitledBorder b = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0xC5, 0xD3, 0xE3)), titulo);
        b.setTitleFont(FUENTE_NEGRITA);
        b.setTitleColor(PRIMARIO);
        p.setBorder(BorderFactory.createCompoundBorder(b, new EmptyBorder(6, 6, 6, 6)));
        p.add(contenido, BorderLayout.CENTER);
        return p;
    }

    public static JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.setFont(FUENTE);
        b.setFocusPainted(false);
        b.addActionListener(e -> accion.run());
        return b;
    }

    public static JButton botonPrimario(String texto, Runnable accion) {
        JButton b = boton(texto, accion);
        b.setBackground(PRIMARIO);
        b.setForeground(Color.WHITE);
        b.setFont(FUENTE_NEGRITA);
        return b;
    }

    /** Formulario de dos columnas (etiqueta: campo). */
    public static JPanel formulario(Object... etiquetasYCampos) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < etiquetasYCampos.length; i += 2) {
            g.gridy = i / 2;
            g.gridx = 0;
            g.weightx = 0;
            g.fill = GridBagConstraints.NONE;
            JLabel l = new JLabel(String.valueOf(etiquetasYCampos[i]));
            l.setFont(FUENTE);
            p.add(l, g);
            g.gridx = 1;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            p.add((Component) etiquetasYCampos[i + 1], g);
        }
        return p;
    }

    public static JPanel fila(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        p.setOpaque(false);
        for (Component c : comps) p.add(c);
        return p;
    }

    public static JTextField campo(int columnas) {
        JTextField f = new JTextField(columnas);
        f.setFont(FUENTE);
        return f;
    }

    public static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(FUENTE);
        return l;
    }

    public static JTextArea areaTexto() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        a.setLineWrap(false);
        return a;
    }

    public static JPanel panelBase() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(FONDO);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    /** Fija el ancho preferido de las columnas de una tabla. */
    public static void anchos(JTable t, int... anchos) {
        for (int i = 0; i < anchos.length && i < t.getColumnCount(); i++) t.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
    }
}
