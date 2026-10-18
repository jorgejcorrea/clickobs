package com.clickobs.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formato de numeros para pantalla (estilo argentino: 3.200,00). */
final class Formato {

    private Formato() { }

    static String usd(BigDecimal valor) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(valor);
    }
}
