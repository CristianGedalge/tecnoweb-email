package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Capa de Datos: Modela archivo, tipo_documento, requisito_documento,
 * documento_postulacion e invitacion_postulacion (CU2).
 */
public class DDocumento {
    private long id;
    private long postulacionId;
    private int tipoDocumentoId;
    private String codigoTipoDoc;
    private long archivoId;
    private String nombreOriginal;
    private String claveAlmacenamiento;
    private String mimeType;
    private long tamanoBytes;
    private String estado;
    private boolean esVigente;
    private String observacion;

    public DDocumento() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPostulacionId() { return postulacionId; }
    public void setPostulacionId(long postulacionId) { this.postulacionId = postulacionId; }

    public int getTipoDocumentoId() { return tipoDocumentoId; }
    public void setTipoDocumentoId(int tipoDocumentoId) { this.tipoDocumentoId = tipoDocumentoId; }

    public String getCodigoTipoDoc() { return codigoTipoDoc; }
    public void setCodigoTipoDoc(String codigoTipoDoc) { this.codigoTipoDoc = codigoTipoDoc; }

    public long getArchivoId() { return archivoId; }
    public void setArchivoId(long archivoId) { this.archivoId = archivoId; }

    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public String getClaveAlmacenamiento() { return claveAlmacenamiento; }
    public void setClaveAlmacenamiento(String claveAlmacenamiento) { this.claveAlmacenamiento = claveAlmacenamiento; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public boolean isEsVigente() { return esVigente; }
    public void setEsVigente(boolean esVigente) { this.esVigente = esVigente; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public long insertar() throws SQLException {
        return registrarDocumento(this.postulacionId, this.codigoTipoDoc,
                this.nombreOriginal, this.claveAlmacenamiento, this.mimeType, this.tamanoBytes);
    }

    public List<String[]> listarDocumentosPostulacion(String filtroPostulacionId) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ID_DOC", "POSTULACION_ID", "TIPO_DOCUMENTO", "ARCHIVO", "ESTADO", "VIGENTE"});

        boolean todos = filtroPostulacionId == null || filtroPostulacionId.isBlank() || filtroPostulacionId.equals("*");
        String sql = "SELECT dp.id, dp.postulacion_id, td.codigo, a.nombre_original, dp.estado, "
                + "CASE WHEN dp.es_vigente THEN 'SI' ELSE 'NO' END "
                + "FROM documento_postulacion dp "
                + "JOIN tipo_documento td ON td.id = dp.tipo_documento_id "
                + "JOIN archivo a ON a.id = dp.archivo_id "
                + (todos ? "" : "WHERE dp.postulacion_id = ? ")
                + "ORDER BY dp.id DESC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                ps.setLong(1, Long.parseLong(filtroPostulacionId.trim()));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        String.valueOf(rs.getLong(2)),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6)
                    });
                }
            }
        }
        return filas;
    }

    public long registrarDocumento(long postulacionId, String codigoTipoDoc, String nombreArchivo,
                                   String rutaAlmacenamiento, String mimeType, long tamanoBytes) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                int tipoDocId;
                try (PreparedStatement psTipo = conn.prepareStatement(
                        "SELECT id FROM tipo_documento WHERE UPPER(codigo) = UPPER(?)")) {
                    psTipo.setString(1, codigoTipoDoc.trim());
                    try (ResultSet rs = psTipo.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Tipo de documento no valido: " + codigoTipoDoc);
                        }
                        tipoDocId = rs.getInt(1);
                    }
                }

                try (PreparedStatement psUpd = conn.prepareStatement(
                        "UPDATE documento_postulacion SET es_vigente = FALSE WHERE postulacion_id = ? AND tipo_documento_id = ? AND es_vigente = TRUE")) {
                    psUpd.setLong(1, postulacionId);
                    psUpd.setInt(2, tipoDocId);
                    psUpd.executeUpdate();
                }

                String claveUnica = "postulacion_" + postulacionId + "/" + UUID.randomUUID() + "_" + nombreArchivo;
                String sha256 = sha256Hex(claveUnica + ":" + tamanoBytes);

                long arcId;
                try (PreparedStatement psArc = conn.prepareStatement(
                        "INSERT INTO archivo (nombre_original, clave_almacenamiento, mime_type, tamano_bytes, hash_sha256) "
                                + "VALUES (?, ?, ?, ?, ?) RETURNING id")) {
                    psArc.setString(1, nombreArchivo);
                    psArc.setString(2, rutaAlmacenamiento != null && !rutaAlmacenamiento.isBlank() ? rutaAlmacenamiento : claveUnica);
                    psArc.setString(3, mimeType != null && !mimeType.isBlank() ? mimeType : "application/pdf");
                    psArc.setLong(4, Math.max(1L, tamanoBytes));
                    psArc.setString(5, sha256);
                    try (ResultSet rs = psArc.executeQuery()) {
                        rs.next();
                        arcId = rs.getLong(1);
                    }
                }

                long docPostId;
                try (PreparedStatement psDoc = conn.prepareStatement(
                        "INSERT INTO documento_postulacion (postulacion_id, tipo_documento_id, archivo_id, estado, es_vigente) "
                                + "VALUES (?, ?, ?, 'VALIDADO', TRUE) RETURNING id")) {
                    psDoc.setLong(1, postulacionId);
                    psDoc.setInt(2, tipoDocId);
                    psDoc.setLong(3, arcId);
                    try (ResultSet rs = psDoc.executeQuery()) {
                        rs.next();
                        docPostId = rs.getLong(1);
                    }
                }

                conn.commit();
                this.id = docPostId;
                this.archivoId = arcId;
                return docPostId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public long registrarInvitacion(long postulacionId, String tokenPlano) throws SQLException {
        String hash = sha256Hex(tokenPlano);
        String sql = "INSERT INTO invitacion_postulacion (postulacion_id, token_hash, estado, enviada_en, expira_en) "
                + "VALUES (?, ?, 'ENVIADA', now(), now() + interval '7 days') "
                + "ON CONFLICT (postulacion_id) DO UPDATE SET token_hash = EXCLUDED.token_hash, "
                + "reenvios = invitacion_postulacion.reenvios + 1, enviada_en = now(), expira_en = now() + interval '7 days' "
                + "RETURNING id";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postulacionId);
            ps.setString(2, hash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
                throw new SQLException("No se pudo registrar la invitacion.");
            }
        }
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return "0".repeat(64);
        }
    }
}
