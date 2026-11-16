package fintech.modelo;

/** Beneficio del catálogo que puede canjearse con puntos. */
public class Beneficio {

    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final int costoPuntos;
    private final NivelUsuario nivelMinimo;

    public Beneficio(String codigo, String nombre, String descripcion, int costoPuntos, NivelUsuario nivelMinimo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.costoPuntos = costoPuntos;
        this.nivelMinimo = nivelMinimo;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getCostoPuntos() {
        return costoPuntos;
    }

    public NivelUsuario getNivelMinimo() {
        return nivelMinimo;
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + costoPuntos + " pts)";
    }
}
