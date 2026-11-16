package fintech.ui;

import fintech.estructuras.Grafo;
import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Usuario;
import fintech.servicios.AnalizadorRelaciones;

import javax.swing.*;
import java.awt.*;

/** Visualización y análisis de los grafos de transferencias. */
public class PanelGrafo extends JPanel implements Refrescable {

    private final VentanaPrincipal v;
    private final VistaGrafo vista = new VistaGrafo();
    private final JRadioButton rbUsuarios = new JRadioButton("Grafo de usuarios", true);
    private final JRadioButton rbBilleteras = new JRadioButton("Grafo de billeteras");
    private final JSpinner spFrecuencia = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
    private final JComboBox<Usuario> cmbA = VentanaPrincipal.comboUsuarios();
    private final JComboBox<Usuario> cmbB = VentanaPrincipal.comboUsuarios();
    private final JTextField txtBilletera = UI.campo(9);
    private final DefaultListModel<String> modeloRes = new DefaultListModel<>();
    private final JList<String> lstRes = new JList<>(modeloRes);
    private final JLabel lblRes = UI.etiqueta("Resultados");
    private final ListaEnlazada<ListaEnlazada<String>> rutasMostradas = new ListaEnlazada<>();

    public PanelGrafo(VentanaPrincipal v) {
        super(new BorderLayout(10, 10));
        this.v = v;
        setBackground(UI.FONDO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbUsuarios);
        bg.add(rbBilleteras);
        rbUsuarios.setOpaque(false);
        rbBilleteras.setOpaque(false);
        rbUsuarios.addActionListener(e -> refrescar());
        rbBilleteras.addActionListener(e -> refrescar());

        add(UI.seccion("Grafo dirigido ponderado (listas de adyacencia sobre tablas hash)", vista), BorderLayout.CENTER);

        JPanel controles = new JPanel(new GridLayout(0, 1, 3, 3));
        controles.setOpaque(false);
        controles.add(UI.fila(rbUsuarios, rbBilleteras));
        controles.add(UI.fila(UI.etiqueta("Frecuencia mínima:"), spFrecuencia, UI.boton("Rutas frecuentes", this::rutasFrecuentes)));
        controles.add(UI.fila(UI.boton("Detectar ciclos (DFS)", this::ciclos), UI.boton("Relaciones recíprocas", this::reciprocas)));
        controles.add(UI.fila(UI.etiqueta("De:"), cmbA));
        controles.add(UI.fila(UI.etiqueta("A:"), cmbB, UI.boton("Ruta más corta (BFS)", this::ruta)));
        controles.add(UI.fila(UI.etiqueta("Billetera:"), txtBilletera, UI.boton("Alcanzables (DFS)", this::alcanzables)));
        controles.add(UI.fila(UI.boton("Relaciones más fuertes entre billeteras", this::relacionesBilleteras)));
        controles.add(UI.fila(UI.boton("Quitar resaltado", () -> {
            vista.resaltar(null);
            lstRes.clearSelection();
        })));

        lstRes.setFont(UI.FUENTE.deriveFont(12f));
        lstRes.addListSelectionListener(e -> {
            int i = lstRes.getSelectedIndex();
            if (!e.getValueIsAdjusting() && i >= 0 && i < rutasMostradas.tamano()) vista.resaltar(rutasMostradas.obtener(i));
        });
        JPanel res = new JPanel(new BorderLayout(4, 4));
        res.setOpaque(false);
        lblRes.setFont(UI.FUENTE_NEGRITA);
        res.add(lblRes, BorderLayout.NORTH);
        res.add(UI.scroll(lstRes), BorderLayout.CENTER);

        JPanel der = new JPanel(new BorderLayout(6, 6));
        der.setOpaque(false);
        der.add(UI.seccion("Análisis", controles), BorderLayout.NORTH);
        der.add(UI.seccion("Resultados (seleccione uno para resaltarlo)", res), BorderLayout.CENTER);
        der.setPreferredSize(new Dimension(500, 400));
        add(der, BorderLayout.EAST);
    }

    private AnalizadorRelaciones rel() {
        return v.getPlataforma().getRelaciones();
    }

    private void limpiarResultados(String titulo) {
        modeloRes.clear();
        rutasMostradas.limpiar();
        lblRes.setText(titulo);
        vista.resaltar(null);
    }

    private void agregarResultado(String texto, ListaEnlazada<String> ruta) {
        modeloRes.addElement(texto);
        rutasMostradas.agregar(ruta);
    }

    private String nombreVertice(String v) {
        return rbUsuarios.isSelected() ? rel().nombre(v) : rel().nombreBilletera(v);
    }

    private String describir(ListaEnlazada<String> ruta) {
        StringBuilder sb = new StringBuilder();
        for (String x : ruta) {
            if (sb.length() > 0) sb.append(" → ");
            sb.append(rbUsuarios.isSelected() ? rel().nombre(x) : x);
        }
        return sb.toString();
    }

    private Grafo<String> grafoActual() {
        return rbUsuarios.isSelected() ? rel().grafoUsuarios() : rel().grafoBilleteras();
    }

    private void rutasFrecuentes() {
        int min = (Integer) spFrecuencia.getValue();
        ListaEnlazada<Grafo.Ruta<String>> rutas = grafoActual().rutasFrecuentes(min);
        limpiarResultados(rutas.tamano() + " rutas con frecuencia ≥ " + min + " (frecuencia = arista más débil)");
        for (Grafo.Ruta<String> r : rutas)
            agregarResultado(r.getFrecuencia() + "×  " + describir(r.getVertices()) + "  (" + UI.dinero(r.getMonto()) + ")", r.getVertices());
    }

    private void ciclos() {
        ListaEnlazada<ListaEnlazada<String>> c = grafoActual().detectarCiclos(50);
        limpiarResultados(c.tamano() + " ciclos detectados (el dinero regresa a su origen)");
        for (ListaEnlazada<String> ciclo : c) agregarResultado(describir(ciclo), ciclo);
    }

    private void reciprocas() {
        ListaEnlazada<Grafo.Arista<String>> r = grafoActual().relacionesReciprocas();
        limpiarResultados(r.tamano() + " pares con transferencias en ambos sentidos");
        for (Grafo.Arista<String> a : r) {
            Grafo.Arista<String> inv = grafoActual().obtenerArista(a.getDestino(), a.getOrigen());
            ListaEnlazada<String> ruta = new ListaEnlazada<>();
            ruta.agregar(a.getOrigen());
            ruta.agregar(a.getDestino());
            ruta.agregar(a.getOrigen());
            agregarResultado(nombreVertice(a.getOrigen()) + " ⇄ " + nombreVertice(a.getDestino()) + "  ("
                    + a.getCantidad() + " / " + inv.getCantidad() + " transferencias)", ruta);
        }
    }

    private void ruta() {
        UI.ejecutar(this, () -> {
            Usuario a = (Usuario) cmbA.getSelectedItem(), b = (Usuario) cmbB.getSelectedItem();
            if (a == null || b == null) return;
            rbUsuarios.setSelected(true);
            refrescar();
            ListaEnlazada<String> r = rel().rutaEntre(a.getId(), b.getId());
            limpiarResultados(r.estaVacia() ? "No existe ruta de dinero de " + a.getNombre() + " a " + b.getNombre()
                    : "Ruta con menos intermediarios (" + (r.tamano() - 1) + " saltos)");
            if (!r.estaVacia()) {
                agregarResultado(describir(r), r);
                lstRes.setSelectedIndex(0);
            }
        });
    }

    private void alcanzables() {
        UI.ejecutar(this, () -> {
            ListaEnlazada<String> l = rel().billeterasAlcanzables(txtBilletera.getText());
            rbBilleteras.setSelected(true);
            refrescar();
            String origen = txtBilletera.getText().trim().toUpperCase();
            limpiarResultados(l.tamano() + " billeteras alcanzables desde " + origen);
            for (String x : l) {
                ListaEnlazada<String> ruta = rel().grafoBilleteras().rutaMasCorta(origen, x);
                agregarResultado(rel().nombreBilletera(x) + "   vía " + describir(ruta), ruta);
            }
        });
    }

    private void relacionesBilleteras() {
        rbBilleteras.setSelected(true);
        refrescar();
        ListaEnlazada<Grafo.Arista<String>> l = rel().relacionesBilleteras(20);
        limpiarResultados("Aristas con más transferencias entre billeteras");
        for (Grafo.Arista<String> a : l) {
            ListaEnlazada<String> r = new ListaEnlazada<>();
            r.agregar(a.getOrigen());
            r.agregar(a.getDestino());
            agregarResultado(a.getCantidad() + "×  " + rel().nombreBilletera(a.getOrigen()) + " → " + rel().nombreBilletera(a.getDestino())
                    + "  (" + UI.dinero(a.getMontoTotal()) + ")", r);
        }
    }

    @Override
    public void refrescar() {
        v.llenarUsuarios(cmbA);
        v.llenarUsuarios(cmbB);
        if (rbUsuarios.isSelected()) vista.mostrar(rel().grafoUsuarios(), rel()::nombre, "Transferencias entre usuarios");
        else vista.mostrar(rel().grafoBilleteras(), x -> {
            var b = v.getPlataforma().getBilleteras().buscar(x);
            return b == null ? x : b.getUsuario().getNombre().split(" ")[0] + " · " + b.getNombre();
        }, "Transferencias entre billeteras");
    }
}
