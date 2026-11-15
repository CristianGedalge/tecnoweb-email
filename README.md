# Tecnoweb Email

Proyecto Maven con Java 21. Solo contiene las cuatro clases de correo en
`src/main/java/com/mycompany/tecnoweb/email/Pruebas`: PuntoDeEntrada,
ClientePOP3, ClienteSMTP y ProcesadorComandos. No necesita PostgreSQL ni
las clases de carpinteria. JavaMail interpreta POP3, SMTP y MIME.

## Ejecutar desde VS Code

Abre la carpeta que contiene este pom.xml y ejecuta en PowerShell:

```powershell
Copy-Item correo.properties.example correo.properties
code correo.properties
```

Completa usuario, password y emisor. Revisa los servidores y puertos.
Los valores de ejemplo corresponden al servidor del proyecto original,
con POP3 110 y SMTP 25 sin TLS. Si tu cuenta usa TLS, configura seguridad
`ssl` o `starttls` y sus puertos. Si requiere autenticacion SMTP, establece
`smtp.auth=true` y completa smtp.usuario y smtp.password. No implementa OAuth2.

Despues de guardar la configuracion:

```powershell
mvn compile exec:java
```

Para revisar solo una vez:

```powershell
mvn compile exec:java "-Dexec.args=--una-vez"
```

Envia desde otra cuenta un correo al emisor configurado, con uno de estos
comandos en el cuerpo como texto sin formato:

- `PING[]`: responde PONG.
- `AYUDA[]`: lista los comandos.
- `ECO[Hola mundo]`: responde con el texto indicado.

Se ejecuta PuntoDeEntrada. Ctrl+C detiene el ciclo de revision.

## Comportamiento

- Consulta el buzon cada 10 segundos (configurable).
- No elimina mensajes. Guarda sus UID en `.correos-procesados` para evitar
  responderlos nuevamente tras reiniciar. Requiere UIDL en el servidor POP3.
- Procesa tambien mensajes antiguos en la primera ejecucion; usa un buzon de pruebas.
- Si SMTP falla, reintenta en el siguiente ciclo sin marcar el mensaje como respondido.
- Omite mensajes automaticos identificados con Auto-Submitted, reportes MIME
  y mensajes enviados desde su propia cuenta.
- Lee texto sin formato, incluso multipartes y codificado. Ignora adjuntos.
  Para cuerpos exclusivamente HTML devuelve la ayuda.
- No implementa operaciones de negocio ni autorizacion: los comandos devuelven texto.
- Ejecuta una sola instancia. Si se interrumpe entre enviar y guardar el UID,
  puede repetir la respuesta. Conserva el historial al reiniciar.

Agrega tus propios comandos en el switch de ProcesadorComandos.
Las credenciales y el historial estan excluidos de Git.
