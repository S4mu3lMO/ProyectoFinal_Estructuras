package fintech.servicios;

import fintech.deteccion.DetectorFraude;
import fintech.modelo.BeneficioCanjeado;
import fintech.modelo.Usuario;

/**
 * Fachada del sistema: crea y conecta todos los módulos y expone las
 * operaciones compuestas que involucran a varios de ellos.
 */
public class PlataformaFintech {

    private final Reloj reloj;
    private final GeneradorId ids = new GeneradorId();
    private final GestorUsuarios usuarios;
    private final GestorBilleteras billeteras;
    private final GestorNotificaciones notificaciones;
    private final GestorRecompensas recompensas;
    private final DetectorFraude detector;
    private final MotorTransacciones motor;
    private final GestorProgramadas programadas;
    private final ServicioAnalitica analitica;
    private final AnalizadorRelaciones relaciones;
    private final ComparadorRendimiento rendimiento = new ComparadorRendimiento();

    public PlataformaFintech() {
        this(new Reloj());
    }

    public PlataformaFintech(Reloj reloj) {
        this.reloj = reloj;
        usuarios = new GestorUsuarios(reloj);
        billeteras = new GestorBilleteras(ids, reloj);
        notificaciones = new GestorNotificaciones(ids, reloj);
        recompensas = new GestorRecompensas(usuarios, notificaciones, ids, reloj);
        detector = new DetectorFraude(notificaciones, ids, reloj);
        motor = new MotorTransacciones(billeteras, recompensas, notificaciones, detector, ids, reloj);
        programadas = new GestorProgramadas(usuarios, billeteras, motor, notificaciones, ids, reloj);
        analitica = new ServicioAnalitica(motor, usuarios);
        relaciones = new AnalizadorRelaciones(motor, usuarios, billeteras);
    }

    /** Elimina un usuario con todas sus validaciones de consistencia. */
    public Usuario eliminarUsuario(String id) {
        Usuario u = usuarios.eliminar(id);   // valida saldo cero
        programadas.cancelarDeUsuario(u);
        billeteras.eliminarDeUsuario(u);
        return u;
    }

    public BeneficioCanjeado canjear(String idUsuario, String codigoBeneficio) {
        return recompensas.canjear(usuarios.obtener(idUsuario), codigoBeneficio);
    }

    /** Avanza el reloj y procesa lo que corresponda. */
    public int avanzarTiempo(int dias, int horas) {
        return programadas.simularHasta(reloj.ahora().plusDays(dias).plusHours(horas));
    }

    public Reloj getReloj() {
        return reloj;
    }

    public GestorUsuarios getUsuarios() {
        return usuarios;
    }

    public GestorBilleteras getBilleteras() {
        return billeteras;
    }

    public GestorNotificaciones getNotificaciones() {
        return notificaciones;
    }

    public GestorRecompensas getRecompensas() {
        return recompensas;
    }

    public DetectorFraude getDetector() {
        return detector;
    }

    public MotorTransacciones getMotor() {
        return motor;
    }

    public GestorProgramadas getProgramadas() {
        return programadas;
    }

    public ServicioAnalitica getAnalitica() {
        return analitica;
    }

    public AnalizadorRelaciones getRelaciones() {
        return relaciones;
    }

    public ComparadorRendimiento getRendimiento() {
        return rendimiento;
    }
}
