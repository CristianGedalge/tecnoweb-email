package com.mycompany.tecnoweb.email.Presentacion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Capa de Presentacion: Analiza y valida la sintaxis COMANDO["p1","p2",...]
 * enviada en el Asunto o Cuerpo del correo.
 */
public final class AnalizadorComandos {
    private static final Pattern PATRON_GENERAL = Pattern.compile(
            "^\\s*([A-Za-z_]+)\\s*\\[(.*)\\]\\s*$", Pattern.DOTALL);

    public record ComandoParseado(String nombre, List<String> parametros) {}

    public ComandoParseado analizar(String asunto, String cuerpo) {
        String origen = seleccionarEntrada(asunto, cuerpo);
        if (origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("El mensaje no contiene ningun comando en el Asunto ni en el Cuerpo.");
        }

        Matcher matcher = PATRON_GENERAL.matcher(origen.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Sintaxis invalida. Use el formato COMANDO[\"param1\",\"param2\"] en el Asunto. Envie AYUDA[\"*\"] para ver los comandos.");
        }

        String comando = matcher.group(1).trim().toUpperCase();
        String interior = matcher.group(2).trim();
        List<String> parametros = parsearParametros(comando, interior);
        return new ComandoParseado(comando, parametros);
    }

    private String seleccionarEntrada(String asunto, String cuerpo) {
        if (asunto != null && asunto.contains("[") && asunto.contains("]")) {
            String limpio = limpiarPrefijosAsunto(asunto.trim());
            if (PATRON_GENERAL.matcher(limpio).matches()) {
                return limpio;
            }
        }
        if (cuerpo != null && !cuerpo.isBlank()) {
            for (String linea : cuerpo.split("\\R")) {
                String l = linea.trim();
                if (!l.isEmpty() && l.contains("[") && l.contains("]")) {
                    return l;
                }
            }
        }
        return asunto != null && !asunto.isBlank() ? limpiarPrefijosAsunto(asunto.trim()) : cuerpo;
    }

    private String limpiarPrefijosAsunto(String asunto) {
        String res = asunto.trim();
        while (res.toUpperCase().startsWith("RE:") || res.toUpperCase().startsWith("FW:") || res.toUpperCase().startsWith("RV:")) {
            res = res.substring(3).trim();
        }
        return res;
    }

    private List<String> parsearParametros(String comando, String interiorRaw) {
        List<String> resultado = new ArrayList<>();
        String interior = interiorRaw
                .replace('\u201C', '"')
                .replace('\u201D', '"')
                .replace('\u201E', '"')
                .trim();

        if (interior.isEmpty() || ((comando.equals("AYUDA") || comando.equals("HELP") || comando.equals("PING")) && interior.equals("*"))) {
            if (comando.equals("AYUDA") || comando.equals("HELP") || comando.equals("PING")) {
                resultado.add("*");
                return resultado;
            }
            return resultado;
        }

        // Caso especial ECO[texto sin comillas]
        if (comando.equals("ECO") && !interior.startsWith("\"")) {
            resultado.add(interior);
            return resultado;
        }

        int i = 0;
        int n = interior.length();
        while (i < n) {
            while (i < n && Character.isWhitespace(interior.charAt(i))) i++;
            if (i >= n) break;

            if (interior.charAt(i) != '"') {
                throw new IllegalArgumentException(
                        "Faltan comillas dobles en los parametros de " + comando + ". Formato: " + comando + "[\"param1\",\"param2\"]");
            }
            i++; // saltar comilla de apertura
            StringBuilder valor = new StringBuilder();
            boolean cerrado = false;
            while (i < n) {
                char c = interior.charAt(i);
                if (c == '\\' && i + 1 < n) {
                    valor.append(interior.charAt(i + 1));
                    i += 2;
                } else if (c == '"') {
                    cerrado = true;
                    i++; // saltar comilla de cierre
                    break;
                } else {
                    valor.append(c);
                    i++;
                }
            }
            if (!cerrado) {
                throw new IllegalArgumentException(
                        "Comillas sin cerrar en los parametros del comando " + comando + ".");
            }
            resultado.add(valor.toString());

            while (i < n && Character.isWhitespace(interior.charAt(i))) i++;
            if (i < n) {
                if (interior.charAt(i) != ',') {
                    throw new IllegalArgumentException(
                            "Los parametros deben separarse con coma (,) en el comando " + comando + ".");
                }
                i++; // saltar coma
                while (i < n && Character.isWhitespace(interior.charAt(i))) i++;
                if (i == n) {
                    throw new IllegalArgumentException("Coma final sin parametro adicional en " + comando + ".");
                }
            }
        }
        return resultado;
    }
}
