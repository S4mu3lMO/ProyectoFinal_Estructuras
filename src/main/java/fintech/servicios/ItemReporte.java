package fintech.servicios;

/** Fila genérica de un reporte: etiqueta, cantidad, monto y detalle. */
public class ItemReporte {

    private final String etiqueta;
    private int cantidad;
    private double monto;
    private String detalle;

    public ItemReporte(String etiqueta, int cantidad, double monto, String detalle) {
        this.etiqueta = etiqueta;
        this.cantidad = cantidad;
        this.monto = monto;
        this.detalle = detalle == null ? "" : detalle;
    }

    public void sumar(double valor) {
        cantidad++;
        monto += valor;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getMonto() {
        return monto;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    @Override
    public String toString() {
        return String.format("%s: %d ($%,.2f) %s", etiqueta, cantidad, monto, detalle);
    }
}
