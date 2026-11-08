package com.clickobs.service;

import com.clickobs.model.Solapamiento;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Implementacion simulada de la IA para el prototipo (no hay llamadas externas). Redacta la narrativa a partir del
 * payload agregado. En Construccion se reemplaza por un cliente HTTP hacia OpenRouter.
 */
public class IASimulada implements ClienteIA {

    private static final Locale AR = Locale.forLanguageTag("es-AR");

    @Override
    public String redactarRecomendacion(PayloadIA p) {
        if (p.solapamientos().isEmpty()) {
            return "Periodo " + p.periodo() + ": no se detectaron servicios monitoreados por mas de una herramienta. "
                 + "No se sugiere retirar ninguna cobertura.";
        }
        StringBuilder sb = new StringBuilder("Periodo " + p.periodo() + ": se detectaron " + p.solapamientos().size()
                + " servicio(s) con cobertura duplicada. ");
        for (Solapamiento s : p.solapamientos()) {
            Solapamiento.Cobertura menor = s.menorCosto();
            Solapamiento.Cobertura mayor = s.mayorCosto();
            sb.append("El servicio '").append(s.servicio()).append("' esta cubierto por ").append(mayor.herramienta())
              .append(" (USD ").append(usd(mayor.costoUsd())).append(") y ").append(menor.herramienta())
              .append(" (USD ").append(usd(menor.costoUsd())).append("); se sugiere evaluar el retiro de ")
              .append(menor.herramienta()).append(" para ese servicio. ");
        }
        sb.append("Ahorro estimado (cota inferior): USD ").append(usd(p.ahorroEstimado())).append(". La decision es del CTO.");
        return sb.toString();
    }

    @Override
    public String nombreModelo() {
        return "simulado-local";
    }

    private static String usd(BigDecimal v) {
        NumberFormat nf = NumberFormat.getNumberInstance(AR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }
}
