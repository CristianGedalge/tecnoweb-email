package com.mycompany.tecnoweb.email.Conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Gestiona la conexion JDBC hacia PostgreSQL (esquema talento).
 */
public final class ConexionDB {
    private static String url = "jdbc:postgresql://localhost:5432/talento?currentSchema=talento,public";
    private static String usuario = "postgres";
    private static String password = "postgres";

    private ConexionDB() {
    }

    public static void configurar(Properties config) {
        if (config == null) return;
        String cfgUrl = config.getProperty("db.url");
        String cfgUser = config.getProperty("db.usuario");
        String cfgPass = config.getProperty("db.password");
        if (cfgUrl != null && !cfgUrl.isBlank()) url = cfgUrl.trim();
        if (cfgUser != null && !cfgUser.isBlank()) usuario = cfgUser.trim();
        if (cfgPass != null) password = cfgPass.trim();
    }

    public static Connection conectar() throws SQLException {
        Connection conexion = DriverManager.getConnection(url, usuario, password);
        try (Statement stmt = conexion.createStatement()) {
            stmt.execute("SET search_path = talento, public");
        }
        return conexion;
    }
}
