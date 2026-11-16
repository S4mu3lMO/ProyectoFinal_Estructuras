package fintech.servicios;

/** Error de negocio por datos inválidos o reglas no cumplidas. */
public class ValidacionException extends RuntimeException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
