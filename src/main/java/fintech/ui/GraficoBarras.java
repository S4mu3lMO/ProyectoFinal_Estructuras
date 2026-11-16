package fintech.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Gráfico de barras horizontales muy simple, dibujado con Java2D. */
public class GraficoBarras extends JPanel {

    private final List<String> etiquetas = new ArrayList<>();
    private final List<Double> valores = new ArrayList<>();
    private String titulo = "";
    private boolean esDinero;
    private String formato = "%,.0f";

    /** Formato de los valores numéricos (cuando no son dinero). */
    public void setFormato(String formato) {
        this.formato = formato;
    }

    public GraficoBarras() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(380, 300));
    }

    public void limpiar() {
        etiquetas.clear();
        valores.clear();
        repaint();
    }

    public void setTitulo(String titulo, boolean esDinero) {
        this.titulo = titulo;
        this.esDinero = esDinero;
    }

    public void agregar(String etiqueta, double valor) {
        etiquetas.add(etiqueta);
        valores.add(valor);
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
        g.drawString(titulo, 10, 20);
        if (valores.isEmpty()) return;
        double max = 0;
        for (double v : valores) max = Math.max(max, v);
        if (max <= 0) max = 1;
        int n = Math.min(valores.size(), 12);
        int top = 34, alto = Math.max(14, Math.min(26, (getHeight() - top - 10) / n - 6));
        int anchoEtiqueta = Math.min(170, getWidth() / 3);
        int anchoMax = getWidth() - anchoEtiqueta - 110;
        g.setFont(UI.FUENTE.deriveFont(12f));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < n; i++) {
            int y = top + i * (alto + 6);
            String et = etiquetas.get(i);
            while (fm.stringWidth(et) > anchoEtiqueta - 8 && et.length() > 4) et = et.substring(0, et.length() - 2);
            if (!et.equals(etiquetas.get(i))) et += "…";
            g.setColor(Color.DARK_GRAY);
            g.drawString(et, 8, y + alto - 5);
            int w = (int) Math.round(anchoMax * valores.get(i) / max);
            g.setColor(new Color(0x2F, 0x6E, 0xBA));
            g.fillRoundRect(anchoEtiqueta, y, Math.max(2, w), alto, 6, 6);
            g.setColor(Color.DARK_GRAY);
            String txt = esDinero ? UI.dinero(valores.get(i)) : String.format(formato, valores.get(i));
            g.drawString(txt, anchoEtiqueta + Math.max(2, w) + 6, y + alto - 5);
        }
    }
}
