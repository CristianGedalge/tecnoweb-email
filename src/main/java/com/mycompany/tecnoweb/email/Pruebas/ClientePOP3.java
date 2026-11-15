package com.mycompany.tecnoweb.email.Pruebas;

import com.sun.mail.pop3.POP3Folder;
import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

/** Recibe correos sin eliminarlos del servidor. */
public final class ClientePOP3 implements AutoCloseable {
    private final Store store;
    private final POP3Folder bandeja;

    public ClientePOP3(Properties config) throws MessagingException {
        Properties opciones = opcionesConexion(config, "pop3");
        opciones.setProperty("mail.pop3.disabletop", "true");
        store = Session.getInstance(opciones).getStore("pop3");
        POP3Folder abierta = null;
        try {
            store.connect(config.getProperty("pop3.host"),
                    Integer.parseInt(config.getProperty("pop3.port", "110")),
                    config.getProperty("pop3.usuario"), config.getProperty("pop3.password"));
            abierta = (POP3Folder) store.getFolder("INBOX");
            abierta.open(Folder.READ_ONLY);
            bandeja = abierta;
        } catch (MessagingException | RuntimeException error) {
            if (abierta != null && abierta.isOpen()) {
                try { abierta.close(false); } catch (MessagingException cierre) { error.addSuppressed(cierre); }
            }
            try { store.close(); } catch (MessagingException cierre) { error.addSuppressed(cierre); }
            throw error;
        }
    }

    public Message[] leerMensajes() throws MessagingException {
        return bandeja.getMessages();
    }

    public String obtenerId(Message mensaje) throws MessagingException {
        String uid = bandeja.getUID(mensaje);
        if (uid == null || uid.isBlank()) {
            throw new MessagingException("El servidor POP3 debe soportar UIDL para evitar respuestas duplicadas.");
        }
        return uid;
    }

    static Properties opcionesConexion(Properties config, String protocolo) {
        Properties opciones = new Properties();
        String prefijo = "mail." + protocolo + ".";
        String seguridad = config.getProperty(protocolo + ".seguridad", "none").trim();
        if (!seguridad.equals("none") && !seguridad.equals("starttls") && !seguridad.equals("ssl")) {
            throw new IllegalArgumentException(protocolo + ".seguridad debe ser none, starttls o ssl.");
        }
        opciones.setProperty(prefijo + "ssl.enable", String.valueOf(seguridad.equals("ssl")));
        opciones.setProperty(prefijo + "ssl.checkserveridentity", "true");
        opciones.setProperty(prefijo + "starttls.enable", String.valueOf(seguridad.equals("starttls")));
        opciones.setProperty(prefijo + "starttls.required", String.valueOf(seguridad.equals("starttls")));
        opciones.setProperty(prefijo + "connectiontimeout", "10000");
        opciones.setProperty(prefijo + "timeout", "20000");
        opciones.setProperty(prefijo + "writetimeout", "20000");
        return opciones;
    }

    @Override
    public void close() throws MessagingException {
        try {
            if (bandeja.isOpen()) bandeja.close(false);
        } finally {
            store.close();
        }
    }
}
