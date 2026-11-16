# Tecnoweb Email

Sistema de selección de personal operado íntegramente mediante **correo electrónico (POP3 / SMTP)** y conectado a **PostgreSQL**. Permite gestionar usuarios, candidatos, consentimientos, empresas, cargos, pruebas de evaluación, ranking, informes en PDF y reportes estadísticos en CSV enviando comandos en el asunto del correo.

> Para consultar la lista completa de comandos disponibles por caso de uso, revisa [COMANDOS.md](COMANDOS.md).

---

## Requisitos

- **Java 21** (JDK 21)
- **Maven**
- **PostgreSQL 14 o superior**

---

## Cómo ejecutar el proyecto

### 1. Crear la base de datos

Crea la base de datos `talento` en PostgreSQL y carga el esquema inicial incluido en el proyecto:

```powershell
createdb -U postgres talento
psql -U postgres -d talento -v ON_ERROR_STOP=1 -f sql/talento_schema.sql
```

*(También puedes crear la base de datos `talento` desde **pgAdmin 4** y ejecutar el archivo `sql/talento_schema.sql`).*

### 2. Configurar credenciales

Copia el archivo de ejemplo y completa tus datos de correo (POP3/SMTP) y de PostgreSQL:

```powershell
Copy-Item correo.properties.example correo.properties
```

Edita `correo.properties`:
- `pop3.*` y `smtp.*`: servidor, puerto, usuario, contraseña y correo emisor.
- `db.url`, `db.usuario` y `db.password`: conexión a tu base de datos `talento`.

### 3. Compilar y ejecutar

Iniciar el servicio de escucha continua (revisa el buzón cada 10 segundos):

```powershell
mvn compile exec:java
```

*(Presiona `Ctrl + C` para detenerlo).*

#### Opciones adicionales

- **Revisar el buzón una sola vez y salir:**

  ```powershell
  mvn compile exec:java "-Dexec.args=--una-vez"
  ```

- **Probar un comando directamente en consola sin enviar correo:**

  ```powershell
  mvn compile exec:java "-Dexec.args=--probar AYUDA[*]"
  ```

---

## Uso rápido por correo

Envía un correo a la cuenta configurada colocando el comando en el **Asunto** con el formato `COMANDO["param1","param2",...]`. Por ejemplo:

- `AYUDA["*"]` — Devuelve la lista de todos los comandos.
- `LISPER["*"]` — Lista las personas registradas.
- `INSPER["4715292","","SC","Juan Carlos","Perez Seras","2000-05-12","71055123"]` — Registra una persona.
