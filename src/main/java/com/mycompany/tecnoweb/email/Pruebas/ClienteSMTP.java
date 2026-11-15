package com.mycompany.tecnoweb.email.Pruebas;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/** Envia respuestas y propaga los rechazos del servidor SMTP. */
public final class ClienteSMTP {
    private final Properties config;
    private final Session sesion;

    public ClienteSMTP(Properties config) {
        this.config = config;
        Properties opciones = ClientePOP3.opcionesConexion(config, "smtp");
        opciones.setProperty("mail.smtp.auth", config.getProperty("smtp.auth", "false"));
        sesion = Session.getInstance(opciones);
    }

    public void enviarCorreo(String destinatario, String asunto, String cuerpo) throws MessagingException {
        MimeMessage mensaje = new MimeMessage(sesion);
        mensaje.setFrom(new InternetAddress(config.getProperty("smtp.emisor"), true));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario, true));
        mensaje.setSubject(asunto, StandardCharsets.UTF_8.name());
        mensaje.setText(cuerpo, StandardCharsets.UTF_8.name());
        mensaje.setSentDate(new Date());
        mensaje.setHeader("Auto-Submitted", "auto-replied");
        mensaje.saveChanges();
        boolean autenticar = Boolean.parseBoolean(config.getProperty("smtp.auth", "false"));
        try (Transport transporte = sesion.getTransport("smtp")) {
            transporte.connect(config.getProperty("smtp.host"),
                    Integer.parseInt(config.getProperty("smtp.port", "25")),
                    autenticar ? config.getProperty("smtp.usuario") : null,
                    autenticar ? config.getProperty("smtp.password") : null);
            transporte.sendMessage(mensaje, mensaje.getAllRecipients());
        }
    }
}
