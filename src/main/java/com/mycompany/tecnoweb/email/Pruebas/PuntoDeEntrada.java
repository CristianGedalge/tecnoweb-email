package com.mycompany.tecnoweb.email.Pruebas;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.mail.Message;

/** Ejecuta el ciclo POP3 -> comando -> respuesta SMTP. */
public final class PuntoDeEntrada {
    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        Path archivoConfig = Path.of("correo.properties");
        if (!Files.isRegularFile(archivoConfig)) {
            throw new IllegalArgumentException("Falta correo.properties. Copia correo.properties.example y completa tu cuenta.");
        }
        try (Reader lector = Files.newBufferedReader(archivoConfig, StandardCharsets.UTF_8)) {
            config.load(lector);
        }
        for (String clave : new String[]{"pop3.host", "pop3.usuario", "pop3.password", "smtp.host", "smtp.emisor"}) {
            exigir(config, clave);
        }
        if (Boolean.parseBoolean(config.getProperty("smtp.auth", "false"))) {
            exigir(config, "smtp.usuario");
            exigir(config, "smtp.password");
        }
        int intervalo = Integer.parseInt(config.getProperty("intervalo.segundos", "10"));
        if (intervalo <= 0) throw new IllegalArgumentException("intervalo.segundos debe ser positivo.");
        boolean unaVez = args.length == 1 && args[0].equals("--una-vez");
        if (args.length > 0 && !unaVez) throw new IllegalArgumentException("Uso: sin argumentos o --una-vez");
        Path historial = Path.of(config.getProperty("procesados.archivo", ".correos-procesados"));
        Set<String> procesados = Files.exists(historial)
                ? new HashSet<>(Files.readAllLines(historial, StandardCharsets.UTF_8)) : new HashSet<>();
        String cuenta = config.getProperty("pop3.host") + ":" + config.getProperty("pop3.port", "110")
                + ":" + config.getProperty("pop3.usuario") + ":";
        ClienteSMTP smtp = new ClienteSMTP(config);
        ProcesadorComandos procesador = new ProcesadorComandos();
        System.out.println("Sistema de correo iniciado. Ctrl+C para detener. Los mensajes se conservan en el buzon.");
        do {
            try (ClientePOP3 pop3 = new ClientePOP3(config)) {
                Message[] mensajes = pop3.leerMensajes();
                System.out.println("Mensajes en el buzon: " + mensajes.length);
                for (Message mensaje : mensajes) {
                    try {
                        String id = Base64.getEncoder().encodeToString(
                                (cuenta + pop3.obtenerId(mensaje)).getBytes(StandardCharsets.UTF_8));
                        if (procesados.contains(id)) continue;
                        if (!procesador.esAutomatico(mensaje)) {
                            String emisor = procesador.obtenerCorreoEmisor(mensaje);
                            if (!emisor.equalsIgnoreCase(config.getProperty("smtp.emisor"))) {
                                smtp.enviarCorreo(emisor, "Respuesta del sistema de correo",
                                        procesador.procesarComando(procesador.getCuerpo(mensaje)));
                                System.out.println("Respuesta enviada a " + emisor);
                            }
                        }
                        Files.writeString(historial, id + System.lineSeparator(), StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        procesados.add(id);
                    } catch (Exception error) {
                        System.err.println("No se completo un mensaje: " + error.getMessage());
                        if (unaVez) throw error;
                    }
                }
            } catch (Exception error) {
                System.err.println("Error de correo: " + error.getMessage());
                if (unaVez) throw error;
            }
            if (!unaVez) Thread.sleep(intervalo * 1000L);
        } while (!unaVez);
    }

    private static void exigir(Properties config, String clave) {
        String valor = config.getProperty(clave);
        if (valor == null || valor.isBlank() || valor.contains("TU_USUARIO") || valor.contains("TU_CONTRASENA")) {
            throw new IllegalArgumentException("Completa " + clave + " en correo.properties.");
        }
    }
}
