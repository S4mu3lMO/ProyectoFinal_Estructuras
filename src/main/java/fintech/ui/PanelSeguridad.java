package fintech.ui;

import fintech.deteccion.DetectorFraude;
import fintech.deteccion.ReglaDeteccion;
import fintech.estructuras.ListaEnlazada;
import fintech.modelo.Billetera;
import fintech.modelo.EventoAuditoria;
import fintech.modelo.NivelRiesgo;
import fintech.modelo.Transaccion;
import fintech.modelo.Usuario;
import fintech.servicios.MotorTransacciones;
import fintech.servicios.ValidacionException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Consulta del historial de auditoría y de las reglas de detección de comportamiento inusual. */
public class PanelSeguridad extends JPanel implements Refrescable {

    private final VentanaPrincipal v;
    private final JComboBox<Object> cmbUsuario = new JComboBox<>();
    private final JComboBox<NivelRiesgo> cmbNivel = new JComboBox<>(NivelRiesgo.values());
    private final JCheckBox chkBloquear = new JCheckBox("Bloquear operaciones con riesgo CRÍTICO");
    private final DefaultTableModel modelo = UI.modelo("ID", "Fecha", "Usuario", "Transacción", "Tipo", "Valor", "Puntaje", "Nivel", "Bloqueada", "Hallazgos");
    private final JTable tabla = UI.tabla(modelo);
    private final DefaultTableModel modeloReglas = UI.modelo("Regla", "Peso");
    private boolean cargando;

    public PanelSeguridad(VentanaPrincipal v) {
        super(new BorderLayout(10, 10));
        this.v = v;
        setBackground(UI.FONDO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chkBloquear.setOpaque(false);
        chkBloquear.setFont(UI.FUENTE);
        cmbNivel.setSelectedItem(NivelRiesgo.BAJO);

        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.setOpaque(false);
        centro.add(UI.fila(UI.etiqueta("Usuario:"), cmbUsuario, UI.etiqueta("Nivel mínimo:"), cmbNivel,
                UI.boton("Consultar", this::refrescar), chkBloquear), BorderLayout.NORTH);
        UI.anchos(tabla, 75, 115, 130, 80, 140, 80, 55, 70, 70, 520);
        centro.add(UI.scroll(tabla), BorderLayout.CENTER);
        add(UI.seccion("Historial especial de auditoría (eventos detectados)", centro), BorderLayout.CENTER);

        JPanel der = new JPanel(new BorderLayout(6, 6));
        der.setOpaque(false);
        JTable tReglas = UI.tabla(modeloReglas);
        UI.anchos(tReglas, 230, 50);
        JScrollPane sp = UI.scroll(tReglas);
        sp.setPreferredSize(new Dimension(300, 190));
        der.add(UI.seccion("Reglas activas", sp), BorderLayout.NORTH);
        JTextArea escala = new JTextArea("Puntaje = suma de pesos de las reglas activadas.\n"
                + "0 Ninguno · 1–2 Bajo · 3–4 Medio · 5–6 Alto · ≥7 Crítico.\n\n"
                + "Ante un hallazgo: se marca la transacción con su nivel de riesgo, "
                + "se envía una alerta de seguridad al usuario y se registra el evento aquí. "
                + "Las de riesgo crítico pueden bloquearse preventivamente.\n\n"
                + "Las reglas de ventana de tiempo recorren el historial del usuario desde el final "
                + "(lista doblemente enlazada) y se detienen al salir de la ventana.");
        escala.setLineWrap(true);
        escala.setWrapStyleWord(true);
        escala.setEditable(false);
        escala.setOpaque(false);
        escala.setFont(UI.FUENTE.deriveFont(12f));
        der.add(escala, BorderLayout.CENTER);
        der.add(UI.seccion("Demostración", UI.fila(UI.botonPrimario("Simular escenario sospechoso", this::simular))), BorderLayout.SOUTH);
        der.setPreferredSize(new Dimension(320, 400));
        add(der, BorderLayout.EAST);

        cmbUsuario.addActionListener(e -> {
            if (!cargando) refrescar();
        });
        cmbNivel.addActionListener(e -> refrescar());
        chkBloquear.addActionListener(e -> detector().setBloquearRiesgoCritico(chkBloquear.isSelected()));
    }

    private DetectorFraude detector() {
        return v.getPlataforma().getDetector();
    }

    /**
     * Ejecuta, con el usuario seleccionado, varias transferencias seguidas al
     * mismo destino y luego una de valor muy alto, avanzando el reloj minuto a minuto.
     */
    private void simular() {
        UI.ejecutar(this, () -> {
            Object sel = cmbUsuario.getSelectedItem();
            if (!(sel instanceof Usuario)) throw new ValidacionException("Seleccione un usuario concreto en el filtro");
            Usuario u = (Usuario) sel;
            Billetera origen = null;
            for (Billetera b : u.getBilleteras()) if (b.estaActiva() && (origen == null || b.getSaldo() > origen.getSaldo())) origen = b;
            if (origen == null || origen.getSaldo() < 200) throw new ValidacionException("El usuario necesita una billetera activa con saldo ≥ $200");
            Billetera destino = null;
            for (Billetera b : v.getPlataforma().getBilleteras().todas())
                if (b.getUsuario() != u && b.estaActiva()) {
                    destino = b;
                    break;
                }
            if (destino == null) throw new ValidacionException("Se necesita una billetera de otro usuario");
            MotorTransacciones m = v.getPlataforma().getMotor();
            StringBuilder sb = new StringBuilder("Escenario ejecutado (" + origen.getCodigo() + " → " + destino.getCodigo() + "):\n");
            double pequeno = Math.max(10, Math.min(60, origen.getSaldo() / 20));
            for (int i = 0; i < 4; i++) {
                v.getPlataforma().getReloj().avanzarMinutos(1);
                Transaccion t = m.transferir(origen.getCodigo(), destino.getCodigo(), pequeno, "Prueba de ráfaga " + (i + 1));
                sb.append(String.format("%s  %s  riesgo %s%n", t.getId(), UI.dinero(t.getValor()), t.getNivelRiesgo()));
            }
            v.getPlataforma().getReloj().avanzarMinutos(1);
            double alto = Math.min(origen.getSaldo() * 0.8, u.getNivel().getLimiteOperacion());
            Transaccion t = m.transferir(origen.getCodigo(), destino.getCodigo(), Math.round(alto), "Envío de alto valor");
            sb.append(String.format("%s  %s  riesgo %s  %s%n", t.getId(), UI.dinero(t.getValor()), t.getNivelRiesgo(),
                    t.esExitosa() ? "" : "→ " + t.getMotivoRechazo()));
            v.refrescarTodo();
            JOptionPane.showMessageDialog(this, sb.toString(), "Detección de comportamiento inusual", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void refrescar() {
        cargando = true;
        Object sel = cmbUsuario.getSelectedItem();
        cmbUsuario.removeAllItems();
        cmbUsuario.addItem("Todos los usuarios");
        for (Usuario u : v.getPlataforma().getUsuarios().todos()) cmbUsuario.addItem(u);
        if (sel != null) cmbUsuario.setSelectedItem(sel);
        if (cmbUsuario.getSelectedIndex() < 0) cmbUsuario.setSelectedIndex(0);
        cargando = false;
        chkBloquear.setSelected(detector().isBloquearRiesgoCritico());

        Object filtro = cmbUsuario.getSelectedItem();
        NivelRiesgo min = (NivelRiesgo) cmbNivel.getSelectedItem();
        ListaEnlazada<EventoAuditoria> eventos = filtro instanceof Usuario
                ? detector().auditoriaDeUsuario(((Usuario) filtro).getId()) : detector().getAuditoria();
        modelo.setRowCount(0);
        for (EventoAuditoria e : eventos.inverso()) {
            if (e.getNivelRiesgo().ordinal() < min.ordinal()) continue;
            Transaccion t = e.getTransaccion();
            modelo.addRow(new Object[]{e.getId(), UI.fecha(e.getFecha()), e.getUsuario().getNombre(), t.getId(), t.getTipo(),
                    UI.dinero(t.getValor()), e.getPuntaje(), e.getNivelRiesgo(), e.isBloqueada() ? "Sí" : "No", e.resumenHallazgos()});
        }
        modeloReglas.setRowCount(0);
        for (ReglaDeteccion r : detector().getReglas()) modeloReglas.addRow(new Object[]{r.getNombre(), r.getPeso()});
    }
}
