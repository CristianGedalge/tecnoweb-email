# Plan de Implementación — Tecnoweb Email (Selección de Personal)

> Documento de planificación y especificación de comandos.
> Fecha: 16 de noviembre de 2026.

## 1. Objetivo y Alcance

Implementar los **ocho casos de uso (CU1–CU8)** del sistema de selección de personal operados íntegramente mediante **correo electrónico (POP3 / SMTP)** siguiendo el protocolo de comandos en el **Asunto (`Subject`)** exigido en la materia:

```text
COMANDO["parametro1","parametro2",...]
```

El sistema comparte el esquema de base de datos PostgreSQL con el proyecto web (`tecnoweb-api` / `tecnoweb-app`), operando sobre las **34 tablas** correspondientes al flujo de selección empresarial y evaluación de candidatos (más **3 vistas** de reportes).

---

## 2. Arquitectura en 3 Capas (`Dato`, `Negocio`, `Presentacion`)

El proyecto sigue la arquitectura de **3 capas de TecnoWeb (UAGRM)** dentro del paquete `com.mycompany.tecnoweb.email`:

```text
com.mycompany.tecnoweb.email/
│
├── Conexion/
│   └── ConexionDB.java                <-- Conexión JDBC a PostgreSQL
│
├── Dato/                              <-- 1. CAPA DE DATOS (Consultas SQL con PreparedStatement)
│   ├── DPersona.java                  <-- CU1: tabla persona
│   ├── DUsuario.java                  <-- CU1: tablas usuario y rol
│   ├── DConsentimiento.java           <-- CU2: tablas consentimiento y parametro_sistema
│   ├── DDocumento.java                <-- CU2: tablas archivo, tipo_documento, requisito_documento, documento_postulacion, invitacion_postulacion
│   ├── DEmpresa.java                  <-- CU3: tablas empresa y contacto_empresa
│   ├── DCargo.java                    <-- CU3: tablas proceso, solicitud_empresa, dimension_evaluada y perfil_requerido
│   ├── DPrueba.java                   <-- CU3: tablas prueba, proceso_prueba, rubrica_criterio, pregunta y opcion_pregunta
│   ├── DPostulacion.java              <-- CU4: tablas etapa, proceso_etapa, postulacion, postulacion_etapa e historial_postulacion_etapa
│   ├── DEvaluacion.java               <-- CU5: tablas intento_prueba, respuesta_intento, calificacion_criterio, resultado_prueba y resultado_dimension
│   ├── DRanking.java                  <-- CU6: tabla ranking_resultado, búsqueda aritmética y matriz comparativa
│   ├── DInforme.java                  <-- CU7: tablas informe_candidato e informe_final
│   └── DReporte.java                  <-- CU8: vistas v_embudo_proceso, v_bandeja_tareas, v_puntaje_postulacion y estadísticas
│
├── Negocio/                           <-- 2. CAPA DE NEGOCIO (Validaciones, Reglas y Transacciones de los 8 CU)
│   ├── NUsuario.java                  <-- CU1: Gestión de usuarios, personas y roles
│   ├── NCandidato.java                <-- CU2: Gestión de candidatos, privacidad, consentimientos y documentos
│   ├── NEmpresaCargo.java             <-- CU3: Empresas solicitantes, perfiles de cargo e instrumentos de evaluación
│   ├── NPostulacion.java              <-- CU4: Clasificar candidatos, asignar planes y trazabilidad
│   ├── NEvaluacion.java               <-- CU5: Ejecutar pruebas, consolidar resultados y calificar candidatos
│   ├── NRanking.java                  <-- CU6: Búsqueda aritmética, ranking y matriz comparativa
│   ├── NInforme.java                  <-- CU7: Informes individuales, recomendaciones ejecutivas y despacho
│   └── NReporte.java                  <-- CU8: Reportes y estadísticas
│
├── Presentacion/                      <-- 3. CAPA DE PRESENTACIÓN (Interfaz por Correo Electrónico)
│   ├── ClientePOP3.java               <-- Recepción de mensajes POP3, UIDL y lectura de adjuntos MIME
│   ├── ClienteSMTP.java               <-- Envío de respuestas por SMTP (texto/HTML y adjuntos PDF/CSV)
│   ├── AnalizadorComandos.java        <-- Parser de sintaxis COMANDO["p1","p2"], validación de comillas y parámetros
│   ├── ProcesadorComandos.java        <-- Enrutador entre la entrada del correo y la capa de Negocio (N...)
│   └── FormateadorRespuesta.java      <-- Construcción de tablas de respuesta, mensajes de Éxito/Error y Ayuda
│
└── PuntoDeEntrada.java                <-- Clase principal (Main) del ciclo de escucha de correo
```

### Flujo de una petición por las 3 capas
1. **`Presentacion`**: `ClientePOP3` lee el correo entrante $\rightarrow$ `AnalizadorComandos` valida el formato `COMANDO["p1","p2"]` en el `Asunto` $\rightarrow$ `ProcesadorComandos` invoca el método correspondiente en `Negocio`.
2. **`Negocio`**: La clase `N...` del Caso de Uso valida reglas de negocio, permisos y parámetros, e invoca a una o más clases de `Dato`.
3. **`Dato`**: La clase `D...` ejecuta las consultas `SELECT` / `INSERT` / `UPDATE` parametrizadas sobre PostgreSQL mediante `ConexionDB`.
4. **Respuesta en `Presentacion`**: `FormateadorRespuesta` estructura el resultado (`"Éxito: ..."` / listado en tabla / `"Error: [Descripción]"`) y `ClienteSMTP` envía el correo al remitente.

---

## 3. Distribución de las Tablas de la Base de Datos

De las **43 tablas** del esquema general en PostgreSQL, el sistema de correo utiliza **34 tablas** (y **3 vistas**), organizadas así:

### 3.1. Tablas Catálogo (8 tablas — Pre-cargadas por `CatalogoSeeder`, solo lectura)
1. `rol` (`ADMIN`, `AYUDANTE`, `EGRESADO`, `CANDIDATO_EXTERNO`, `EMPRESA`)
2. `etapa` (`INSCRIPCION`, `REQUISITOS`, `ANALISIS_CV`, `EVALUACION_TECNICA`, `EVALUACION_PSICOLOGICA`, `HABILIDADES_BLANDAS`, `ENTREVISTA`, `INVESTIGACION_ANTECEDENTES`, `SELECCION`, `INFORME`)
3. `dimension_evaluada` (16 dimensiones de personalidad y perfil)
4. `prueba` (`PPA`, `CV_ANALISIS`, `TEST_PERSONALIDAD`, `TEST_INTELIGENCIA`, `TEST_INTELIGENCIA_EMOCIONAL`, `TEST_LECTURA_COMPRENSIVA`, `TEST_APTITUD_NUMERICA`, `TEST_HABILIDADES_BLANDAS`, `ENTREVISTA`)
5. `rubrica_criterio` (`Formación`, `Experiencia`, `Habilidades`, `Presentación`)
6. `tipo_documento` (`HISTORICO_ACADEMICO`, `CEDULA_IDENTIDAD`, `FOTOGRAFIA`, `CURRICULUM_VITAE`)
7. `requisito_documento`
8. `parametro_sistema`

### 3.2. Tablas Principales (12 tablas — Operadas directamente por comandos)
9. `persona` (**CU1**)
10. `usuario` (**CU1**)
11. `consentimiento` (**CU2**)
12. `documento_postulacion` (**CU2**)
13. `empresa` (**CU3**)
14. `solicitud_empresa` (**CU3**)
15. `perfil_requerido` (**CU3**)
16. `pregunta` (**CU3**)
17. `postulacion` (**CU4**)
18. `postulacion_etapa` (**CU4**)
19. `intento_prueba` (**CU5**)
20. `informe_final` (**CU7**)

### 3.3. Tablas Hijas / Automáticas (14 tablas — Llenadas en transacción por `Dato`/`Negocio`)
21. `archivo` (metadatos de adjuntos en `INSDOC` y `GENINF`)
22. `contacto_empresa` (se registra junto con `empresa` en `INSEMP`)
23. `proceso` (se crea junto con `solicitud_empresa` en `INSCAR`)
24. `proceso_etapa` (se inicializa al crear el proceso en `INSCAR`)
25. `proceso_prueba` (se configura con `ASIPRU`)
26. `opcion_pregunta` (se inserta junto con `pregunta` en `INSPRE`)
27. `invitacion_postulacion` (se registra en `INSINV`)
28. `historial_postulacion_etapa` (se registra automáticamente en `INSPOS` y `ASIETA`)
29. `respuesta_intento` (se registra al procesar `RESPRU`)
30. `calificacion_criterio` (se registra al procesar `CALRUB`)
31. `resultado_prueba` (se consolida al finalizar `RESPRU` o `CALRUB`)
32. `resultado_dimension` (se calcula al finalizar `RESPRU`)
33. `ranking_resultado` (se genera al ejecutar `GENRAN`)
34. `informe_candidato` (se registra con `INSINF`)

### 3.4. Vistas de Reportes (CU8)
- `v_embudo_proceso`
- `v_bandeja_tareas`
- `v_puntaje_postulacion`

*(Las 9 tablas restantes —`facultad`, `carrera`, `area_preferencia`, `ayudante_carrera`, `egresado`, `convocatoria_talento`, `antecedente_laboral`, `investigacion_antecedentes`, `solicitud_informacion`— se conservan en la BD para el portal web universitario).*

---

## 4. Especificación de Comandos por Caso de Uso (40 Comandos)

Convención:
- **Nombre del comando:** 6 letras en mayúsculas (`VERBO` de 3 letras + `ENTIDAD` de 3 letras).
  - `LIS` = Listar / Consultar
  - `INS` = Insertar / Registrar
  - `MOD` = Modificar / Actualizar
  - `ELI` = Eliminar / Desactivar
  - `ASI` = Asignar, `INI` = Iniciar, `RES` = Responder, `CAL` = Calificar, `BUS` = Buscar, `GEN` = Generar, `ENV` = Enviar.
- **Parámetros:** Entre corchetes `[...]`, cada parámetro entre comillas dobles `"..."` y separados por coma `,`. Para listar todo se utiliza `["*"]`.

---

### CU1. Gestión de Usuarios, Personas y Roles
**Clases:** `NUsuario` $\rightarrow$ `DPersona`, `DUsuario`
**Tablas:** `rol`, `usuario`, `persona`

| # | Comando | Descripción | Sintaxis en el Asunto (`Subject`) | Ejemplo |
|---|---|---|---|---|
| 1 | **`LISROL`** | Lista los roles disponibles en el sistema. | `LISROL["*"]` | `LISROL["*"]` |
| 2 | **`LISPER`** | Lista las personas registradas (todas o filtradas por CI). | `LISPER["*"]` o `LISPER["ci"]` | `LISPER["*"]` |
| 3 | **`INSPER`** | Registra una nueva persona en la tabla `persona`. | `INSPER["ci","complemento","departamento","nombres","apellidos","fecha_nac","celular"]` | `INSPER["4715292","","SC","Juan Carlos","Perez Seras","2000-05-12","71055123"]` |
| 4 | **`MODPER`** | Modifica los datos de una persona existente. | `MODPER["id_persona","ci","nombres","apellidos","celular"]` | `MODPER["1","4715292","Juan Carlos","Perez Seras","71099999"]` |
| 5 | **`LISUSU`** | Lista los usuarios con su rol, correo y estado. | `LISUSU["*"]` | `LISUSU["*"]` |
| 6 | **`INSUSU`** | Crea una cuenta de usuario vinculada a una persona y un rol. | `INSUSU["id_persona","codigo_rol","correo","password"]` | `INSUSU["1","CANDIDATO_EXTERNO","juanperez@uagrm.edu.bo","123456"]` |
| 7 | **`MODUSU`** | Actualiza el rol o correo de un usuario. | `MODUSU["id_usuario","codigo_rol","correo"]` | `MODUSU["2","AYUDANTE","juanperez@uagrm.edu.bo"]` |
| 8 | **`ELIUSU`** | Desactiva una cuenta de usuario (`estado = INACTIVO`). | `ELIUSU["id_usuario"]` | `ELIUSU["2"]` |

---

### CU2. Gestión de Candidatos, Privacidad y Consentimiento
**Clases:** `NCandidato` $\rightarrow$ `DConsentimiento`, `DDocumento`
**Tablas:** `consentimiento`, `parametro_sistema`, `tipo_documento`, `requisito_documento`, `archivo`, `documento_postulacion`, `invitacion_postulacion`

| # | Comando | Descripción | Sintaxis en el Asunto (`Subject`) | Ejemplo |
|---|---|---|---|---|
| 9 | **`LISCON`** | Lista los consentimientos registrados de una persona. | `LISCON["id_persona"]` | `LISCON["1"]` |
| 10 | **`INSCON`** | Registra la aceptación de consentimiento de privacidad. | `INSCON["id_persona","tipo_consentimiento","aceptado"]` | `INSCON["1","TRATAMIENTO_DATOS","SI"]` |
| 11 | **`ELICON`** | Revoca un consentimiento registrando el evento de revocación. | `ELICON["id_consentimiento"]` | `ELICON["1"]` |
| 12 | **`LISDOC`** | Lista los documentos requeridos y presentados de una postulación. | `LISDOC["id_postulacion"]` | `LISDOC["25"]` |
| 13 | **`INSDOC`** | Registra el archivo adjunto del correo en `archivo` y `documento_postulacion`. | `INSDOC["id_postulacion","codigo_tipo_doc"]` *(con adjunto)* | `INSDOC["25","CURRICULUM_VITAE"]` |
| 14 | **`INSINV`** | Crea y envía una invitación a un candidato externo para un proceso. | `INSINV["id_proceso","correo_candidato","nombre_candidato"]` | `INSINV["8","juanperez@uagrm.edu.bo","Juan Carlos Perez"]` |

---

### CU3. Empresas Solicitantes, Perfiles de Cargo e Instrumentos de Evaluación
**Clases:** `NEmpresaCargo` $\rightarrow$ `DEmpresa`, `DCargo`, `DPrueba`
**Tablas:** `empresa`, `contacto_empresa`, `proceso`, `solicitud_empresa`, `dimension_evaluada`, `perfil_requerido`, `prueba`, `proceso_prueba`, `rubrica_criterio`, `pregunta`, `opcion_pregunta`

| # | Comando | Descripción | Sintaxis en el Asunto (`Subject`) | Ejemplo |
|---|---|---|---|---|
| 15 | **`LISEMP`** | Lista las empresas registradas y su contacto principal. | `LISEMP["*"]` | `LISEMP["*"]` |
| 16 | **`INSEMP`** | Inserta una empresa y su contacto principal en `empresa` y `contacto_empresa`. | `INSEMP["nit","razon_social","rubro","nombre_contacto","cargo_contacto","correo_contacto","telefono"]` | `INSEMP["1020304055","Banco Ganadero","Banca","Maria Lopez","Jefe RRHH","rrhh@bg.com.bo","33445566"]` |
| 17 | **`MODEMP`** | Modifica los datos de una empresa y su contacto. | `MODEMP["id_empresa","razon_social","rubro","correo_contacto","telefono"]` | `MODEMP["1","Banco Ganadero SA","Finanzas","talento@bg.com.bo","33445577"]` |
| 18 | **`LISCAR`** | Lista los cargos/procesos solicitados por las empresas. | `LISCAR["*"]` | `LISCAR["*"]` |
| 19 | **`INSCAR`** | Crea el `proceso`, la `solicitud_empresa` y sus etapas iniciales. | `INSCAR["id_empresa","codigo_proceso","cargo","funciones","vacantes","top_n"]` | `INSCAR["1","EMP-2026-001","Analista de Sistemas","Desarrollo Java y SQL","2","5"]` |
| 20 | **`INSREQ`** | Define el nivel requerido de una dimensión para el perfil del cargo (`perfil_requerido`). | `INSREQ["id_proceso","codigo_dimension","nivel_esperado"]` | `INSREQ["8","CAPACIDAD_ESCOLAR","8"]` |
| 21 | **`LISPRU`** | Lista las pruebas configuradas y sus pesos para un proceso. | `LISPRU["id_proceso"]` | `LISPRU["8"]` |
| 22 | **`ASIPRU`** | Asigna una prueba del catálogo al proceso con su ponderación (`proceso_prueba`). | `ASIPRU["id_proceso","codigo_prueba","peso","puntaje_minimo","obligatoria"]` | `ASIPRU["8","TEST_INTELIGENCIA","60","70","SI"]` |
| 23 | **`INSPRE`** | Inserta una pregunta con sus opciones en `pregunta` y `opcion_pregunta`. | `INSPRE["codigo_prueba","codigo_dimension","enunciado","opciones","opcion_correcta","puntaje"]` | `INSPRE["TEST_INTELIGENCIA","CAPACIDAD_ESCOLAR","Serie 2,4,8,?","A:10|B:12|C:16","C","20"]` |

---

### CU4. Clasificar Candidatos, Asignar Planes y Trazabilidad
**Clases:** `NPostulacion` $\rightarrow$ `DPostulacion`
**Tablas:** `etapa`, `proceso_etapa`, `postulacion`, `postulacion_etapa`, `historial_postulacion_etapa`

| # | Comando | Descripción | Sintaxis en el Asunto (`Subject`) | Ejemplo |
|---|---|---|---|---|
| 24 | **`LISPLA`** | Muestra el plan de etapas configurado para un proceso. | `LISPLA["id_proceso"]` | `LISPLA["8"]` |
| 25 | **`LISPOS`** | Lista los candidatos postulados a un proceso y su etapa actual. | `LISPOS["id_proceso"]` | `LISPOS["8"]` |
| 26 | **`INSPOS`** | Registra la postulación de una persona a un proceso e inicializa sus etapas e historial. | `INSPOS["id_proceso","id_persona"]` | `INSPOS["8","1"]` |
| 27 | **`ASIETA`** | Actualiza el estado de una etapa, asigna evaluador y guarda la trazabilidad en `historial_postulacion_etapa`. | `ASIETA["id_postulacion","codigo_etapa","estado_etapa","id_evaluador","observacion"]` | `ASIETA["25","REQUISITOS","APROBADA","2","Cumple todos los documentos"]` |
| 28 | **`LISHIS`** | Consulta la bitácora completa de cambios de etapa de una postulación. | `LISHIS["id_postulacion"]` | `LISHIS["25"]` |

---

### CU5. Ejecutar Pruebas, Consolidar Resultados y Calificar Candidatos
**Clases:** `NEvaluacion` $\rightarrow$ `DEvaluacion`
**Tablas:** `intento_prueba`, `respuesta_intento`, `calificacion_criterio`, `resultado_prueba`, `resultado_dimension`

| # | Comando | Descripción | Sintaxis en el Asunto (`Subject`) | Ejemplo |
|---|---|---|---|---|
| 29 | **`INIPRU`** | Crea un `intento_prueba` abierto y envía el cuestionario por correo al candidato. | `INIPRU["id_postulacion","codigo_prueba"]` | `INIPRU["25","TEST_INTELIGENCIA"]` |
| 30 | **`RESPRU`** | Registra las respuestas del candidato, cierra el intento y calcula notas en `resultado_prueba` y `resultado_dimension`. | `RESPRU["id_intento","respuestas"]` | `RESPRU["40","1:C|2:A|3:B|4:C|5:A"]` |
| 31 | **`CALRUB`** | Califica un criterio de rúbrica manual (CV o Entrevista) y actualiza `resultado_prueba`. | `CALRUB["id_postulacion","codigo_prueba","nombre_criterio","puntaje","observacion"]` | `CALRUB["25","ENTREVISTA","Formación","9","Excelente base tecnica"]` |
| 32 | **`LISRES`** | Lista las calificaciones por prueba, por dimensión y el promedio ponderado del candidato. | `LISRES["id_postulacion"]` | `LISRES["25"]` |

---

### CU6. Búsqueda Aritmética, Ranking y Matriz Comparativa
**Clases:** `NRanking` $\rightarrow$ `DRanking`
**Tablas:** `ranking_resultado`, `perfil_requerido`, `resultado_dimension`, `resultado_prueba`, `postulacion`

| # | Comando | Descripción | Sintaxis en el Asunto (`Subject`) | Ejemplo |
|---|---|---|---|---|
| 33 | **`BUSCAN`** | Búsqueda aritmética de candidatos que cumplan puntaje mínimo y porcentaje de afinidad. | `BUSCAN["id_proceso","puntaje_min","afinidad_min"]` | `BUSCAN["8","70","80"]` |
| 34 | **`GENRAN`** | Calcula las posiciones finales, persiste en `ranking_resultado` y retorna el Top N del proceso. | `GENRAN["id_proceso"]` | `GENRAN["8"]` |
| 35 | **`GENMAT`** | Genera la matriz comparativa de candidatos (filas) vs. dimensiones/pruebas y afinidad (columnas). | `GENMAT["id_proceso"]` | `GENMAT["8"]` |

**Fórmula de Afinidad con el Perfil Requerido (escala 0 a 10):**
$$\text{Afinidad (\%)} = 100 \times \left(1 - \frac{\sum |V_{\text{obtenido}} - V_{\text{requerido}}|}{10 \times N_{\text{dimensiones}}}\right)$$

---

### CU7. Informes Individuales, Recomendaciones Ejecutivas y Despacho
**Clases:** `NInforme` $\rightarrow$ `DInforme`
**Tablas:** `informe_candidato`, `informe_final`, `archivo`, `contacto_empresa`

| # | Comando | Descripción | Sintaxis en el Asunto (`Subject`) | Ejemplo |
|---|---|---|---|---|
| 36 | **`INSINF`** | Registra el informe individual y recomendación ejecutiva de un candidato (`informe_candidato`). | `INSINF["id_postulacion","recomendacion","fortalezas","debilidades","conclusion"]` | `INSINF["25","RECOMENDADO","Solido en SQL","Poca experiencia liderando","Apto para el cargo"]` |
| 37 | **`GENINF`** | Consolida el `informe_final` del proceso, genera el documento PDF (`archivo`) y lo adjunta en la respuesta. | `GENINF["id_proceso","resumen_ejecutivo"]` | `GENINF["8","Se presenta la terna final evaluada"]` |
| 38 | **`ENVINF`** | Envía por SMTP el informe final en PDF al correo del `contacto_empresa` autorizado. | `ENVINF["id_proceso"]` | `ENVINF["8"]` |

---

### CU8. Reportes y Estadísticas
**Clases:** `NReporte` $\rightarrow$ `DReporte`
**Vistas y Tablas:** `v_embudo_proceso`, `v_bandeja_tareas`, `v_puntaje_postulacion`, `proceso`, `postulacion`

| # | Comando | Descripción | Sintaxis en el Asunto (`Subject`) | Ejemplo |
|---|---|---|---|---|
| 39 | **`GENREP`** | Genera el reporte solicitado (`"EMBUDO"`, `"TAREAS"`, `"PUNTAJES"` o `"GENERAL"`) en texto y adjunta CSV. | `GENREP["tipo_reporte","id_proceso"]` | `GENREP["EMBUDO","8"]` |
| 40 | **`AYUDA`** | Devuelve el manual completo de comandos y su formato de uso. | `AYUDA["*"]` | `AYUDA["*"]` |

---

## 5. Formato Estándar de Respuestas

1. **Consultas / Listados (`LIS...`, `BUSCAN`, `GENRAN`, `GENMAT`, `GENREP`):**
   - Devuelve una tabla estructurada con los registros obtenidos y el total de filas.
2. **Inserciones / Modificaciones / Eliminaciones (`INS...`, `MOD...`, `ELI...`, `ASI...`, `CAL...`):**
   - En caso exitoso: `"Éxito: [Descripción de la operación realizada] (ID: X)"`.
   - En caso de error de sintaxis, parámetros incompletos, falta de comillas o violación de restricciones SQL: `"Error: [Descripción clara del fallo]"`.

---

## 6. Orden de Implementación

1. **Etapa 1 — Infraestructura y Capa de Presentación + Conexion:**
   - Agregar dependencia `org.postgresql:postgresql` en `pom.xml`.
   - Implementar `ConexionDB.java`.
   - Crear `AnalizadorComandos.java`, `FormateadorRespuesta.java` y adaptar `ClientePOP3`, `ClienteSMTP` y `ProcesadorComandos` dentro de `Presentacion/`.
2. **Etapa 2 — CU1 (`DPersona`, `DUsuario`, `NUsuario`):**
   - Implementar `LISROL`, `LISPER`, `INSPER`, `MODPER`, `LISUSU`, `INSUSU`, `MODUSU`, `ELIUSU`.
3. **Etapa 3 — CU2 (`DConsentimiento`, `DDocumento`, `NCandidato`):**
   - Implementar `LISCON`, `INSCON`, `ELICON`, `LISDOC`, `INSDOC`, `INSINV`.
4. **Etapa 4 — CU3 (`DEmpresa`, `DCargo`, `DPrueba`, `NEmpresaCargo`):**
   - Implementar `LISEMP`, `INSEMP`, `MODEMP`, `LISCAR`, `INSCAR`, `INSREQ`, `LISPRU`, `ASIPRU`, `INSPRE`.
5. **Etapa 5 — CU4 (`DPostulacion`, `NPostulacion`):**
   - Implementar `LISPLA`, `LISPOS`, `INSPOS`, `ASIETA`, `LISHIS`.
6. **Etapa 6 — CU5 (`DEvaluacion`, `NEvaluacion`):**
   - Implementar `INIPRU`, `RESPRU`, `CALRUB`, `LISRES`.
7. **Etapa 7 — CU6 (`DRanking`, `NRanking`):**
   - Implementar `BUSCAN`, `GENRAN`, `GENMAT`.
8. **Etapa 8 — CU7 y CU8 (`DInforme`, `NInforme`, `DReporte`, `NReporte`):**
   - Implementar `INSINF`, `GENINF`, `ENVINF`, `GENREP` y `AYUDA`.
