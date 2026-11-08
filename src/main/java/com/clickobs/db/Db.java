package com.clickobs.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Acceso a la base de datos MySQL por JDBC (RI-02).
 * La configuracion se lee de db.properties (carpeta de ejecucion) y puede
 * sobreescribirse con las variables de entorno CLICKOBS_DB_URL, CLICKOBS_DB_USER
 * y CLICKOBS_DB_PASSWORD. Las credenciales nunca se guardan en el codigo (RNF-03).
 */
public final class Db {

    private static Properties config;

    private Db() { }

    public static Connection getConnection() throws SQLException {
        Properties p = config();
        String url = p.getProperty("db.url");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                "Falta configurar la base de datos. Copie db.properties.example como db.properties "
                + "o defina CLICKOBS_DB_URL, CLICKOBS_DB_USER y CLICKOBS_DB_PASSWORD.");
        }
        return DriverManager.getConnection(url, p.getProperty("db.user"), p.getProperty("db.password"));
    }

    private static synchronized Properties config() {
        if (config != null) {
            return config;
        }
        Properties p = new Properties();
        Path archivo = Path.of("db.properties");
        if (Files.exists(archivo)) {
            try (InputStream in = Files.newInputStream(archivo)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer db.properties: " + e.getMessage(), e);
            }
        }
        sobreescribir(p, "db.url", "CLICKOBS_DB_URL");
        sobreescribir(p, "db.user", "CLICKOBS_DB_USER");
        sobreescribir(p, "db.password", "CLICKOBS_DB_PASSWORD");
        config = p;
        return config;
    }

    private static void sobreescribir(Properties p, String clave, String variableEntorno) {
        String valor = System.getenv(variableEntorno);
        if (valor != null && !valor.isBlank()) {
            p.setProperty(clave, valor);
        }
    }
}
