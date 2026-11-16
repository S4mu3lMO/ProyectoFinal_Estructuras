package fintech.ui;

import fintech.estructuras.ArbolAVL;
import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Beneficio;
import fintech.modelo.BeneficioCanjeado;
import fintech.modelo.NivelUsuario;
import fintech.modelo.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Puntos, niveles, canje de beneficios y consultas sobre el árbol AVL de puntos. */
public class PanelRecompensas extends JPanel implements Refrescable {

    private final VentanaPrincipal v;
    private final JComboBox<Usuario> cmbUsuario = VentanaPrincipal.comboUsuarios();
    private final JLabel lblNivel = new JLabel();
    private final JLabel lblPuntos = UI.etiqueta("");
    private final JLabel lblVentajas = UI.etiqueta("");
    private final JProgressBar barra = new JProgressBar(0, 100);
    private final DefaultTableModel modeloCatalogo = UI.modelo("Código", "Beneficio", "Descripción", "Costo (pts)", "Nivel mínimo");
    private final JTable tablaCatalogo = UI.tabla(modeloCatalogo);
    private final DefaultTableModel modeloCanjes = UI.modelo("Código canje", "Fecha", "Beneficio", "Puntos usados");
    private final DefaultTableModel modeloArbol = UI.modelo("#", "Usuario", "Nivel", "Puntos acumulados");
    private final JTextField txtMin = UI.campo(6);
    private final JTextField txtMax = UI.campo(6);
    private final JComboBox<NivelUsuario> cmbNivel = new JComboBox<>(NivelUsuario.values());
    private final JLabel lblArbol = UI.etiqueta("");
    private ListaEnlazada<Usuario> consultaArbol;
    private String tituloConsulta = "Ranking (recorrido inverso del AVL)";
    private boolean cargando;

    public PanelRecompensas(VentanaPrincipal v) {
        super(new BorderLayout(10, 10));
        this.v = v;
        setBackground(UI.FONDO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtMin.setText("0");
        txtMax.setText("1000");

        // ---- tarjeta del usuario
        lblNivel.setFont(UI.FUENTE_TITULO);
        lblNivel.setForeground(UI.PRIMARIO);
        barra.setStringPainted(true);
        JPanel tarjeta = new JPanel(new GridLayout(0, 1, 2, 2));
        tarjeta.setOpaque(false);
        tarjeta.add(UI.fila(UI.etiqueta("Usuario:"), cmbUsuario));
        tarjeta.add(lblNivel);
        tarjeta.add(lblPuntos);
        tarjeta.add(lblVentajas);
        tarjeta.add(barra);

        JPanel catalogo = new JPanel(new BorderLayout(4, 4));
        catalogo.setOpaque(false);
        UI.anchos(tablaCatalogo, 60, 170, 260, 80, 90);
        catalogo.add(UI.scroll(tablaCatalogo), BorderLayout.CENTER);
        catalogo.add(UI.fila(UI.botonPrimario("Canjear beneficio seleccionado", this::canjear)), BorderLayout.SOUTH);

        JTextArea politica = new JTextArea(
                "Política de puntos: Recarga 1 pt / $100 · Retiro 2 pts / $100 · Transferencia 3 pts / $100 · "
                        + "Operación programada exitosa: +10 pts de bono · Bono adicional por nivel: Plata 5%, Oro 10%, Platino 20%.\n"
                        + "Niveles: Bronce 0–500 · Plata 501–1000 · Oro 1001–5000 · Platino > 5000 (según puntos acumulados). "
                        + "El canje descuenta puntos disponibles; una reversión descuenta los puntos que generó la operación.");
        politica.setLineWrap(true);
        politica.setWrapStyleWord(true);
        politica.setEditable(false);
        politica.setOpaque(false);
        politica.setFont(UI.FUENTE.deriveFont(12f));

        JPanel izq = new JPanel(new BorderLayout(8, 8));
        izq.setOpaque(false);
        izq.add(UI.seccion("Nivel y puntos", tarjeta), BorderLayout.NORTH);
        JSplitPane splitIzq = new JSplitPane(JSplitPane.VERTICAL_SPLIT, UI.seccion("Catálogo de beneficios", catalogo),
                UI.seccion("Beneficios canjeados por el usuario (lista)", UI.scroll(UI.tabla(modeloCanjes))));
        splitIzq.setResizeWeight(0.55);
        splitIzq.setBorder(null);
        izq.add(splitIzq, BorderLayout.CENTER);
        izq.add(politica, BorderLayout.SOUTH);

        // ---- árbol AVL
        JPanel arbol = new JPanel(new BorderLayout(4, 4));
        arbol.setOpaque(false);
        JPanel controles = new JPanel(new GridLayout(0, 1, 2, 2));
        controles.setOpaque(false);
        controles.add(UI.fila(UI.boton("Top 10", () -> {
            consultaArbol = null;
            tituloConsulta = "Ranking (recorrido inverso del AVL)";
            refrescar();
        }), UI.boton("Todos en orden", () -> {
            consultaArbol = v.getPlataforma().getUsuarios().getArbolPuntos().enOrden();
            tituloConsulta = "Recorrido en orden (ascendente)";
            refrescar();
        })));
        controles.add(UI.fila(UI.etiqueta("Rango de puntos:"), txtMin, UI.etiqueta("a"), txtMax, UI.boton("Buscar", this::buscarRango)));
        controles.add(UI.fila(UI.etiqueta("Nivel:"), cmbNivel, UI.boton("Usuarios del nivel", () -> {
            NivelUsuario n = (NivelUsuario) cmbNivel.getSelectedItem();
            consultaArbol = v.getPlataforma().getUsuarios().porNivel(n);
            tituloConsulta = "Nivel " + n + " (rango " + n.getPuntosMin() + "–" + (n.getPuntosMax() == Integer.MAX_VALUE ? "∞" : n.getPuntosMax()) + ")";
            refrescar();
        })));
        controles.add(lblArbol);
        arbol.add(controles, BorderLayout.NORTH);
        JTable tablaArbol = UI.tabla(modeloArbol);
        UI.anchos(tablaArbol, 30, 170, 70, 110);
        arbol.add(UI.scroll(tablaArbol), BorderLayout.CENTER);
        JPanel der = UI.seccion("Árbol AVL de usuarios por puntos", arbol);
        der.setPreferredSize(new Dimension(470, 400));

        add(izq, BorderLayout.CENTER);
        add(der, BorderLayout.EAST);

        cmbUsuario.addActionListener(e -> {
            if (!cargando) refrescar();
        });
    }

    private void buscarRango() {
        UI.ejecutar(this, () -> {
            int min = UI.leerEntero(txtMin.getText(), "Mínimo"), max = UI.leerEntero(txtMax.getText(), "Máximo");
            consultaArbol = v.getPlataforma().getUsuarios().porRangoDePuntos(min, max);
            tituloConsulta = "Rango [" + min + ", " + max + "]";
            refrescar();
        });
    }

    private void canjear() {
        UI.ejecutar(this, () -> {
            Usuario u = (Usuario) cmbUsuario.getSelectedItem();
            int r = tablaCatalogo.getSelectedRow();
            if (u == null || r < 0) throw new fintech.servicios.ValidacionException("Seleccione un usuario y un beneficio");
            String codigo = (String) modeloCatalogo.getValueAt(tablaCatalogo.convertRowIndexToModel(r), 0);
            BeneficioCanjeado bc = v.getPlataforma().canjear(u.getId(), codigo);
            v.refrescarTodo();
            UI.info(this, "Canje exitoso: " + bc.getBeneficio().getNombre() + "\nCódigo: " + bc.getCodigoCanje());
        });
    }

    @Override
    public void refrescar() {
        cargando = true;
        v.llenarUsuarios(cmbUsuario);
        cargando = false;
        Usuario u = (Usuario) cmbUsuario.getSelectedItem();
        modeloCanjes.setRowCount(0);
        if (u != null) {
            NivelUsuario n = u.getNivel();
            lblNivel.setText("Nivel " + n.getEtiqueta());
            lblPuntos.setText("Puntos acumulados: " + u.getPuntosAcumulados() + "   ·   Disponibles para canje: " + u.getPuntosDisponibles());
            lblVentajas.setText(n.ventajas());
            if (n == NivelUsuario.PLATINO) {
                barra.setValue(100);
                barra.setString("Nivel máximo alcanzado");
            } else {
                int rango = n.getPuntosMax() - n.getPuntosMin() + 1;
                int avance = (int) Math.round(100.0 * (u.getPuntosAcumulados() - n.getPuntosMin()) / rango);
                barra.setValue(avance);
                barra.setString(avance + "% hacia " + NivelUsuario.values()[n.ordinal() + 1] + " (faltan "
                        + (n.getPuntosMax() + 1 - u.getPuntosAcumulados()) + " pts)");
            }
            for (BeneficioCanjeado bc : u.getBeneficiosCanjeados().inverso())
                modeloCanjes.addRow(new Object[]{bc.getCodigoCanje(), UI.fecha(bc.getFecha()), bc.getBeneficio().getNombre(), bc.getPuntosUsados()});
        }
        if (modeloCatalogo.getRowCount() == 0) {
            for (Beneficio b : v.getPlataforma().getRecompensas().getCatalogo())
                modeloCatalogo.addRow(new Object[]{b.getCodigo(), b.getNombre(), b.getDescripcion(), b.getCostoPuntos(), b.getNivelMinimo()});
        }
        ListaEnlazada<Usuario> l = consultaArbol != null ? consultaArbol : v.getPlataforma().getUsuarios().ranking(10);
        if (consultaArbol != null && tituloConsulta.startsWith("Recorrido")) l = v.getPlataforma().getUsuarios().getArbolPuntos().enOrden();
        modeloArbol.setRowCount(0);
        int i = 1;
        for (Usuario x : l) modeloArbol.addRow(new Object[]{i++, x.getNombre(), x.getNivel(), x.getPuntosAcumulados()});
        ArbolAVL<Integer, Usuario> a = v.getPlataforma().getUsuarios().getArbolPuntos();
        lblArbol.setText("<html><b>" + tituloConsulta + "</b>: " + l.tamano() + " usuarios · altura del árbol " + a.altura()
                + " · " + a.cantidadClaves() + " claves distintas</html>");
    }
}
