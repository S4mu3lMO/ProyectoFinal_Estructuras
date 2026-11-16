package fintech.servicios;

import fintech.estructuras.ListaEnlazada;
import fintech.estructuras.TablaHash;
import fintech.modelo.Beneficio;
import fintech.modelo.BeneficioCanjeado;
import fintech.modelo.NivelUsuario;
import fintech.modelo.TipoNotificacion;
import fintech.modelo.TipoTransaccion;
import fintech.modelo.Usuario;

/**
 * Política de puntos, niveles y canje de beneficios.
 *
 * Política: Recarga 1 punto por cada 100, Retiro 2, Transferencia 3,
 * operación programada exitosa: bono adicional fijo. Además, cada nivel
 * agrega un bono porcentual sobre los puntos base.
 */
public class GestorRecompensas {

    public static final int BONO_PROGRAMADA = 10;

    private final ListaEnlazada<Beneficio> lstBeneficio = new ListaEnlazada<>();
    private final TablaHash<String, Beneficio> tablaBeneficios = new TablaHash<>();
    private final GestorUsuarios usuarios;
    private final GestorNotificaciones notificaciones;
    private final GeneradorId ids;
    private final Reloj reloj;

    public GestorRecompensas(GestorUsuarios usuarios, GestorNotificaciones notificaciones, GeneradorId ids, Reloj reloj) {
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.ids = ids;
        this.reloj = reloj;
        cargarCatalogo();
    }

    private void cargarCatalogo() {
        agregarBeneficio(new Beneficio("BEN-01", "Bono de transporte", "Recarga de $20 para transporte", 150, NivelUsuario.BRONCE));
        agregarBeneficio(new Beneficio("BEN-02", "Cero comisión por 1 mes", "Retiros sin comisión por 30 días", 300, NivelUsuario.BRONCE));
        agregarBeneficio(new Beneficio("BEN-03", "Cashback 5%", "Devolución del 5% en compras de la semana", 450, NivelUsuario.PLATA));
        agregarBeneficio(new Beneficio("BEN-04", "Tasa preferencial de ahorro", "Rendimiento extra en billetera de ahorro", 800, NivelUsuario.ORO));
        agregarBeneficio(new Beneficio("BEN-05", "Asesoría financiera", "Sesión personalizada con un asesor", 1200, NivelUsuario.ORO));
        agregarBeneficio(new Beneficio("BEN-06", "Acceso sala VIP", "Ingreso a sala VIP en aeropuertos", 2500, NivelUsuario.PLATINO));
    }

    public void agregarBeneficio(Beneficio b) {
        if (tablaBeneficios.contieneClave(b.getCodigo())) throw new ValidacionException("Beneficio duplicado " + b.getCodigo());
        lstBeneficio.agregarAlFinal(b);
        tablaBeneficios.poner(b.getCodigo(), b);
    }

    /** Puntos que genera una operación según tipo, valor, nivel del usuario y si fue programada. */
    public int calcularPuntos(TipoTransaccion tipo, double valor, NivelUsuario nivel, boolean programada) {
        int base = (int) (valor / 100) * tipo.getPuntosPorCada100();
        int bonoNivel = (int) Math.round(base * nivel.getBonoPuntos());
        int bonoProgramada = programada ? BONO_PROGRAMADA : 0;
        return base + bonoNivel + bonoProgramada;
    }

    /**
     * Suma (o resta si es negativo) puntos, reubica al usuario en el árbol y
     * notifica si cambió de nivel.
     */
    public void ajustarPuntos(Usuario u, int delta) {
        if (delta == 0) return;
        int antes = u.getPuntosAcumulados();
        NivelUsuario nivelAntes = u.getNivel();
        boolean cambio = u.ajustarPuntos(delta);
        usuarios.actualizarPosicion(u, antes);
        if (cambio) {
            boolean sube = u.getNivel().ordinal() > nivelAntes.ordinal();
            notificaciones.notificar(u, sube ? TipoNotificacion.ASCENSO_NIVEL : TipoNotificacion.DESCENSO_NIVEL,
                    (sube ? "¡Felicitaciones! Subiste" : "Bajaste") + " de nivel " + nivelAntes + " a " + u.getNivel()
                            + ". " + u.getNivel().ventajas());
        }
    }

    public BeneficioCanjeado canjear(Usuario u, String codigoBeneficio) {
        Beneficio b = tablaBeneficios.obtener(codigoBeneficio);
        if (b == null) throw new ValidacionException("No existe el beneficio " + codigoBeneficio);
        if (u.getNivel().ordinal() < b.getNivelMinimo().ordinal())
            throw new ValidacionException("El beneficio requiere nivel " + b.getNivelMinimo() + " (nivel actual: " + u.getNivel() + ")");
        if (u.getPuntosDisponibles() < b.getCostoPuntos())
            throw new ValidacionException("Puntos insuficientes: tiene " + u.getPuntosDisponibles() + ", requiere " + b.getCostoPuntos());
        u.gastarPuntos(b.getCostoPuntos());
        BeneficioCanjeado bc = new BeneficioCanjeado(ids.siguiente("CNJ", 5), b, reloj.ahora(), b.getCostoPuntos());
        u.registrarCanje(bc);
        notificaciones.notificar(u, TipoNotificacion.CANJE_BENEFICIO,
                "Canjeaste '" + b.getNombre() + "' por " + b.getCostoPuntos() + " puntos. Código: " + bc.getCodigoCanje());
        return bc;
    }

    public ListaEnlazada<Beneficio> getCatalogo() {
        return lstBeneficio;
    }

    public Beneficio buscarBeneficio(String codigo) {
        return tablaBeneficios.obtener(codigo);
    }
}
