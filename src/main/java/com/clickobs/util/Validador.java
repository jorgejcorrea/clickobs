package com.clickobs.util;

import com.clickobs.excepcion.MontoInvalidoException;
import com.clickobs.excepcion.PeriodoInvalidoException;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Validacion de los datos que escribe el usuario. Todo texto de la interfaz pasa por aca antes de llegar al dominio. */
public final class Validador {

    private static final Pattern PERIODO = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("10000000");

    private Validador() { }

    /** Devuelve el periodo sin espacios si cumple AAAA-MM; si no, lanza PeriodoInvalidoException. */
    public static String periodo(String texto) throws PeriodoInvalidoException {
        if (texto == null || !PERIODO.matcher(texto.trim()).matches()) {
            throw new PeriodoInvalidoException(texto);
        }
        return texto.trim();
    }

    /**
     * Convierte el texto a un monto en USD. Acepta "3500", "3500.50" y "3.500,50" (formato argentino).
     * El try-catch captura NumberFormatException (texto no numerico) y lo traduce a un mensaje claro.
     */
    public static BigDecimal monto(String texto) throws MontoInvalidoException {
        if (texto == null || texto.isBlank()) {
            throw new MontoInvalidoException("Ingrese un monto.");
        }
        String limpio = texto.trim().replace(" ", "");
        if (limpio.contains(",")) {
            limpio = limpio.replace(".", "").replace(',', '.');
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw new MontoInvalidoException("\"" + texto.trim() + "\" no es un numero valido. Use solo digitos, por ejemplo 3500 o 3500,50.", e);
        }
        if (valor.signum() <= 0) {
            throw new MontoInvalidoException("El monto debe ser mayor que cero.");
        }
        if (valor.compareTo(MONTO_MAXIMO) > 0) {
            throw new MontoInvalidoException("El monto no puede superar USD 10.000.000.");
        }
        return valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
