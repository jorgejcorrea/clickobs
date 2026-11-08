package com.clickobs.service;

import com.clickobs.model.ComparacionFila;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exporta la comparacion a CSV (RF-17, version prototipo). Separador ';' y UTF-8 con BOM para que Excel en
 * configuracion regional argentina lo abra correctamente. La exportacion a PDF/Excel nativo queda para Construccion.
 */
public class ExportadorCsv {

    public void exportar(List<ComparacionFila> filas, Path destino) throws IOException {
        try (Writer w = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            w.write('\uFEFF');
            w.write("Herramienta;Servicio;Tribu;Periodo;Cantidad usada;Unidad;Costo (USD)\r\n");
            for (ComparacionFila f : filas) {
                w.write(String.join(";", campo(f.herramienta()), campo(f.servicio()), campo(f.tribu()), campo(f.periodo()),
                        f.cantidadUsada() == null ? "" : f.cantidadUsada().toPlainString().replace('.', ','),
                        campo(f.unidad()), f.costoUsd() == null ? "" : f.costoUsd().toPlainString().replace('.', ',')));
                w.write("\r\n");
            }
        }
    }

    private static String campo(String s) {
        return s == null ? "" : s.replace(";", ",");
    }
}
