package ar.fiuba.is2.hotel.dominio.reserva;

import java.security.SecureRandom;

/** Genera códigos de reserva legibles, sin caracteres ambiguos (RF-09). Ej.: AUR-7KQ2MX */
public class GeneradorDeCodigo {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LARGO = 6;
    private final SecureRandom random = new SecureRandom();
    private final String prefijo;

    public GeneradorDeCodigo(String prefijo) {
        this.prefijo = prefijo;
    }

    public String nuevo() {
        StringBuilder sb = new StringBuilder(prefijo).append('-');
        for (int i = 0; i < LARGO; i++) {
            sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
