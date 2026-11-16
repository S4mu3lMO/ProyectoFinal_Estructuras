package fintech.servicios;

import fintech.estructuras.TablaHash;

/** Genera identificadores consecutivos por prefijo (TX-000001, BIL-0001, ...). */
public class GeneradorId {

    private final TablaHash<String, Long> contadores = new TablaHash<>();

    public String siguiente(String prefijo, int digitos) {
        long n = contadores.obtenerOPorDefecto(prefijo, 0L) + 1;
        contadores.poner(prefijo, n);
        return prefijo + "-" + String.format("%0" + digitos + "d", n);
    }

    public long siguienteNumero(String prefijo) {
        long n = contadores.obtenerOPorDefecto(prefijo, 0L) + 1;
        contadores.poner(prefijo, n);
        return n;
    }
}
