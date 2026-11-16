package fintech.modelo;

/**
 * Datos básicos de cualquier persona registrada en la plataforma.
 */
public abstract class Persona {

    protected String id;          // número de identificación (clave única)
    protected String nombre;
    protected String correo;
    protected String telefono;

    protected Persona(String id, String nombre, String correo, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
    }

    public void actualizarDatos(String nombre, String correo, String telefono) {
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
    }

    /** Descripción del tipo de persona (polimorfismo). */
    public abstract String tipoPersona();

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    @Override
    public String toString() {
        return nombre + " (" + id + ")";
    }
}
