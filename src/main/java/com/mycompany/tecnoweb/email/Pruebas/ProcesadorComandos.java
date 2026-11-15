package com.mycompany.tecnoweb.email.Pruebas;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;

/** Comandos de ejemplo sin base de datos ni clases de carpinteria. */
public final class ProcesadorComandos {
    private static final Pattern COMANDO = Pattern.compile("^([A-Z]+)\\[([^\\]]*)\\]$", Pattern.DOTALL);

    public String procesarComando(String cuerpo) {
        Matcher coincidencia = COMANDO.matcher(cuerpo == null ? "" : cuerpo.trim());
        if (!coincidencia.matches()) return ayuda();
        return switch (coincidencia.group(1)) {
            case "AYUDA" -> ayuda();
            case "PING" -> "PONG: el sistema de correo funciona.";
            case "ECO" -> coincidencia.group(2);
            default -> "Comando desconocido.\n\n" + ayuda();
        };
    }

    public String getCuerpo(Part parte) throws MessagingException, IOException {
        if (Part.ATTACHMENT.equalsIgnoreCase(parte.getDisposition())) return "";
        if (parte.isMimeType("text/plain")) return (String) parte.getContent();
        if (parte.isMimeType("multipart/*")) {
            Multipart contenido = (Multipart) parte.getContent();
            for (int i = 0; i < contenido.getCount(); i++) {
                String texto = getCuerpo(contenido.getBodyPart(i));
                if (!texto.isBlank()) return texto;
            }
        }
        return "";
    }

    public String obtenerCorreoEmisor(Message mensaje) throws MessagingException {
        if (mensaje.getFrom() == null || mensaje.getFrom().length == 0
                || !(mensaje.getFrom()[0] instanceof InternetAddress emisor)) {
            throw new MessagingException("El mensaje no tiene un remitente valido.");
        }
        emisor.validate();
        return emisor.getAddress();
    }

    public boolean esAutomatico(Message mensaje) throws MessagingException {
        String[] valores = mensaje.getHeader("Auto-Submitted");
        if (valores != null) {
            for (String valor : valores) {
                if (!valor.trim().equalsIgnoreCase("no")) return true;
            }
        }
        return mensaje.isMimeType("multipart/report");
    }

    private String ayuda() {
        return "Escribe un comando en MAYUSCULAS en el cuerpo como texto sin formato:\n"
                + "AYUDA[] - Lista los comandos.\n"
                + "PING[] - Comprueba la recepcion y la respuesta.\n"
                + "ECO[Hola] - Responde con el texto indicado.\n"
                + "Envia un solo comando por mensaje.";
    }
}
