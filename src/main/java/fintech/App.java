package fintech;

import fintech.servicios.DatosDemo;
import fintech.servicios.PlataformaFintech;
import fintech.ui.UI;
import fintech.ui.VentanaPrincipal;

import javax.swing.*;
import java.awt.*;

/**
 * Punto de entrada de la aplicación gráfica.
 * Inicia la plataforma con datos de demostración (puede reiniciarse vacía desde el menú Datos).
 */
public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
                UIManager.put("control", UI.FONDO);
                UIManager.put("nimbusBase", UI.PRIMARIO);
                UIManager.put("nimbusFocus", new Color(0x5B, 0x9B, 0xD5));
                UIManager.put("nimbusSelectionBackground", new Color(0x2F, 0x6E, 0xBA));
            } catch (Exception ignorada) {
                // se usa el aspecto por defecto
            }
            PlataformaFintech plataforma = new PlataformaFintech();
            boolean vacia = args.length > 0 && "--vacia".equals(args[0]);
            if (!vacia) DatosDemo.cargar(plataforma);
            new VentanaPrincipal(plataforma).setVisible(true);
        });
    }
}
