package fintech.ui;

import fintech.estructuras.Grafo;
import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.TablaHash;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.function.Function;

/** Dibuja un grafo dirigido con los vértices en círculo y aristas curvas con grosor según su frecuencia. */
public class VistaGrafo extends JPanel {

    private Grafo<String> grafo;
    private Function<String, String> etiqueta = s -> s;
    private ListaEnlazada<String> resaltado = new ListaEnlazada<>();
    private String titulo = "";

    public VistaGrafo() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(640, 560));
    }

    public void mostrar(Grafo<String> g, Function<String, String> etiqueta, String titulo) {
        this.grafo = g;
        this.etiqueta = etiqueta;
        this.titulo = titulo;
        repaint();
    }

    public void resaltar(ListaEnlazada<String> ruta) {
        this.resaltado = ruta == null ? new ListaEnlazada<>() : ruta;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(UI.FUENTE_NEGRITA);
        g.setColor(UI.PRIMARIO);
        g.drawString(titulo, 12, 20);
        if (grafo == null || grafo.numVertices() == 0) {
            g.setColor(Color.GRAY);
            g.drawString("Aún no hay transferencias registradas", 12, 44);
            return;
        }
        ListaEnlazada<String> vs = grafo.vertices();
        vs.ordenar(String::compareTo);
        int n = vs.tamano();
        double cx = getWidth() / 2.0, cy = getHeight() / 2.0 + 10;
        double radio = Math.min(getWidth(), getHeight()) / 2.0 - 70;
        TablaHash<String, Point2D> pos = new TablaHash<>();
        int i = 0;
        for (String v : vs) {
            double ang = -Math.PI / 2 + 2 * Math.PI * i++ / n;
            pos.poner(v, new Point2D.Double(cx + radio * Math.cos(ang), cy + radio * Math.sin(ang)));
        }
        int maxCant = 1;
        for (Grafo.Arista<String> a : grafo.aristas()) maxCant = Math.max(maxCant, a.getCantidad());

        // aristas
        for (Grafo.Arista<String> a : grafo.aristas()) {
            boolean enRuta = estaEnRuta(a.getOrigen(), a.getDestino());
            float grosor = 1f + 5f * a.getCantidad() / maxCant;
            g.setStroke(new BasicStroke(enRuta ? grosor + 2.5f : grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(enRuta ? UI.ALERTA : new Color(0x2F, 0x6E, 0xBA, 120));
            dibujarArista(g, pos.obtener(a.getOrigen()), pos.obtener(a.getDestino()), a.getCantidad(), enRuta);
        }
        // vértices
        g.setStroke(new BasicStroke(2f));
        for (String v : vs) {
            Point2D p = pos.obtener(v);
            boolean enRuta = resaltado.contiene(v);
            int r = 22;
            g.setColor(enRuta ? new Color(0xFF, 0xE0, 0xE0) : UI.PRIMARIO_CLARO);
            g.fillOval((int) p.getX() - r, (int) p.getY() - r, 2 * r, 2 * r);
            g.setColor(enRuta ? UI.ALERTA : UI.PRIMARIO);
            g.drawOval((int) p.getX() - r, (int) p.getY() - r, 2 * r, 2 * r);
            g.setFont(UI.FUENTE.deriveFont(Font.BOLD, 11f));
            String id = v.length() > 8 ? v.substring(v.length() - 4) : v;
            FontMetrics fm = g.getFontMetrics();
            g.drawString(id, (int) p.getX() - fm.stringWidth(id) / 2, (int) p.getY() + 4);
            String et = etiqueta.apply(v);
            if (et.length() > 24) et = et.substring(0, 23) + "…";
            g.setFont(UI.FUENTE.deriveFont(11.5f));
            fm = g.getFontMetrics();
            double dy = p.getY() < cy ? -r - 6 : r + 15;
            g.setColor(Color.DARK_GRAY);
            g.drawString(et, (int) p.getX() - fm.stringWidth(et) / 2, (int) (p.getY() + dy));
        }
        g.setFont(UI.FUENTE.deriveFont(11f));
        g.setColor(Color.GRAY);
        g.drawString(grafo.numVertices() + " vértices · " + grafo.numAristas() + " aristas · grosor = número de transferencias", 12, getHeight() - 10);
    }

    private boolean estaEnRuta(String o, String d) {
        String prev = null;
        for (String x : resaltado) {
            if (prev != null && prev.equals(o) && x.equals(d)) return true;
            prev = x;
        }
        return false;
    }

    private void dibujarArista(Graphics2D g, Point2D a, Point2D b, int cantidad, boolean enRuta) {
        double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
        double len = Math.max(1, Math.hypot(dx, dy));
        double ux = dx / len, uy = dy / len;
        // acortar para no entrar en los círculos
        Point2D ini = new Point2D.Double(a.getX() + ux * 24, a.getY() + uy * 24);
        Point2D fin = new Point2D.Double(b.getX() - ux * 26, b.getY() - uy * 26);
        // punto de control desplazado a la derecha: separa A->B de B->A
        double off = 28;
        Point2D ctrl = new Point2D.Double((ini.getX() + fin.getX()) / 2 - uy * off, (ini.getY() + fin.getY()) / 2 + ux * off);
        g.draw(new QuadCurve2D.Double(ini.getX(), ini.getY(), ctrl.getX(), ctrl.getY(), fin.getX(), fin.getY()));
        // punta de flecha
        double ax = fin.getX() - ctrl.getX(), ay = fin.getY() - ctrl.getY();
        double al = Math.max(1, Math.hypot(ax, ay));
        ax /= al;
        ay /= al;
        int s = 10;
        Polygon flecha = new Polygon();
        flecha.addPoint((int) fin.getX(), (int) fin.getY());
        flecha.addPoint((int) (fin.getX() - ax * s - ay * s * 0.5), (int) (fin.getY() - ay * s + ax * s * 0.5));
        flecha.addPoint((int) (fin.getX() - ax * s + ay * s * 0.5), (int) (fin.getY() - ay * s - ax * s * 0.5));
        g.fill(flecha);
        // etiqueta con la cantidad
        Font f = g.getFont();
        g.setFont(UI.FUENTE.deriveFont(Font.BOLD, 10.5f));
        Color c = g.getColor();
        g.setColor(enRuta ? UI.ALERTA : new Color(0x1F, 0x4E, 0x79));
        double mx = 0.25 * ini.getX() + 0.5 * ctrl.getX() + 0.25 * fin.getX();
        double my = 0.25 * ini.getY() + 0.5 * ctrl.getY() + 0.25 * fin.getY();
        String txt = String.valueOf(cantidad);
        int tw = g.getFontMetrics().stringWidth(txt);
        g.setColor(Color.WHITE);
        g.fillRoundRect((int) mx - tw / 2 - 3, (int) my - 9, tw + 6, 13, 6, 6);
        g.setColor(enRuta ? UI.ALERTA : new Color(0x1F, 0x4E, 0x79));
        g.drawString(txt, (int) mx - tw / 2, (int) my + 2);
        g.setColor(c);
        g.setFont(f);
    }
}
