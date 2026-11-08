package com.clickobs.service;

import com.clickobs.model.Solapamiento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Payload agregado que se le entrega a la IA (seccion de salvaguardas del documento): solo periodo, montos por
 * herramienta, servicios solapados y el ahorro ya calculado por el backend. Nunca telemetria cruda ni datos de clientes.
 */
public record PayloadIA(String periodo, Map<String, BigDecimal> costoPorHerramienta,
                        List<Solapamiento> solapamientos, BigDecimal ahorroEstimado) {

    /** Representacion JSON del payload (es lo que viajaria hacia OpenRouter en la version con IA real). */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{\"periodo\":\"").append(esc(periodo)).append("\",\"costoPorHerramienta\":{");
        boolean primero = true;
        for (Map.Entry<String, BigDecimal> e : costoPorHerramienta.entrySet()) {
            sb.append(primero ? "" : ",").append('"').append(esc(e.getKey())).append("\":").append(e.getValue().toPlainString());
            primero = false;
        }
        sb.append("},\"solapamientos\":[");
        for (int i = 0; i < solapamientos.size(); i++) {
            Solapamiento s = solapamientos.get(i);
            sb.append(i == 0 ? "" : ",").append("{\"servicio\":\"").append(esc(s.servicio())).append("\",\"coberturas\":[");
            for (int j = 0; j < s.coberturas().size(); j++) {
                Solapamiento.Cobertura c = s.coberturas().get(j);
                sb.append(j == 0 ? "" : ",").append("{\"herramienta\":\"").append(esc(c.herramienta()))
                  .append("\",\"costoUsd\":").append(c.costoUsd().toPlainString()).append('}');
            }
            sb.append("]}");
        }
        return sb.append("],\"ahorroEstimadoUsd\":").append(ahorroEstimado.toPlainString()).append('}').toString();
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
