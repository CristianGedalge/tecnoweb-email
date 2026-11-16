package com.mycompany.tecnoweb.email.Negocio;

import com.mycompany.tecnoweb.email.Dato.DInforme;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de Negocio para CU7: Informes Individuales, Recomendaciones Ejecutivas y Despacho.
 */
public class NInforme {
    private final DInforme dInforme = new DInforme();

    public String registrarInformeIndividual(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 5,
                "INSINF[\"id_postulacion\",\"recomendacion\",\"fortalezas\",\"debilidades\",\"conclusion\"]");
        long idPostulacion = NUsuario.parsearLong(params.get(0), "id_postulacion");
        dInforme.setPostulacionId(idPostulacion);
        dInforme.setEtiqueta(NUsuario.exigirTexto(params.get(1), "recomendacion"));
        dInforme.setFortalezas(NUsuario.exigirTexto(params.get(2), "fortalezas"));
        dInforme.setDebilidades(NUsuario.exigirTexto(params.get(3), "debilidades"));
        dInforme.setConclusion(NUsuario.exigirTexto(params.get(4), "conclusion"));

        dInforme.guardarInformeIndividual();
        return "Éxito: Informe individual y dictamen registrados para la postulacion ID " + idPostulacion + ".";
    }

    public ResultadoInforme emitirInformeFinal(List<String> params, Path directorioBase) throws SQLException, IOException {
        NUsuario.validarCantidad(params, 2, "GENINF[\"id_proceso\",\"resumen_ejecutivo\"]");
        long idProceso = NUsuario.parsearLong(params.get(0), "id_proceso");
        String resumen = NUsuario.exigirTexto(params.get(1), "resumen_ejecutivo");

        dInforme.setProcesoId(idProceso);
        dInforme.setResumen(resumen);

        List<String[]> candidatos = dInforme.listarCandidatosDeInforme(idProceso);
        Files.createDirectories(directorioBase);
        String nombrePdf = "informe_ejecutivo_proceso_" + idProceso + ".pdf";
        Path archivoPdf = directorioBase.resolve(nombrePdf);

        byte[] bytesPdf = construirPdfBasico(idProceso, resumen, candidatos);
        Files.write(archivoPdf, bytesPdf);

        long idInforme = dInforme.emitirInformeEjecutivo(
                idProceso, resumen, nombrePdf, archivoPdf.toAbsolutePath().toString(), bytesPdf.length);

        String mensaje = "Éxito: Informe ejecutivo final emitido (ID_INFORME: " + idInforme + ").\n"
                + "Resumen Ejecutivo: " + resumen + "\n"
                + "Se adjunta el documento PDF generado (" + nombrePdf + ").";
        return new ResultadoInforme(mensaje, candidatos, archivoPdf, null, null);
    }

    public ResultadoInforme prepararDespachoEmpresa(List<String> params, Path directorioBase) throws SQLException, IOException {
        NUsuario.validarCantidad(params, 1, "ENVINF[\"id_proceso\"]");
        long idProceso = NUsuario.parsearLong(params.get(0), "id_proceso");
        dInforme.setProcesoId(idProceso);

        String[] datos = dInforme.obtenerDatosDespacho(idProceso);
        String codigoProc = datos[0];
        String nombreProc = datos[1];
        String correoEmpresa = datos[2];
        String resumen = datos[3];
        String rutaExistente = datos[4];

        List<String[]> candidatos = dInforme.listarCandidatosDeInforme(idProceso);
        Path archivoPdf = null;
        if (rutaExistente != null && !rutaExistente.isBlank() && Files.isRegularFile(Path.of(rutaExistente))) {
            archivoPdf = Path.of(rutaExistente);
        } else {
            Files.createDirectories(directorioBase);
            archivoPdf = directorioBase.resolve("informe_ejecutivo_proceso_" + idProceso + ".pdf");
            Files.write(archivoPdf, construirPdfBasico(idProceso, resumen, candidatos));
        }

        String mensajeRemitente = "Éxito: Informe del proceso " + codigoProc + " (" + nombreProc + ") "
                + (correoEmpresa.isBlank() ? "listo para despacho." : "despachado al contacto empresarial: " + correoEmpresa);
        String cuerpoEmpresa = "Estimado cliente,\n\nSe remite el Informe Ejecutivo Final del proceso "
                + codigoProc + " - " + nombreProc + ".\n\nResumen: " + resumen + "\n";

        return new ResultadoInforme(mensajeRemitente, candidatos, archivoPdf,
                correoEmpresa.isBlank() ? null : correoEmpresa, cuerpoEmpresa);
    }

    private byte[] construirPdfBasico(long idProceso, String resumen, List<String[]> candidatos) {
        StringBuilder texto = new StringBuilder();
        texto.append("BT /F1 12 Tf 50 760 Td (INFORME EJECUTIVO DE SELECCION - PROCESO ID ")
             .append(idProceso).append(") Tj 0 -24 Td (Resumen: ")
             .append(escaparPdf(resumen)).append(") Tj ");
        for (int i = 1; i < candidatos.size(); i++) {
            String[] c = candidatos.get(i);
            String linea = c[0] + " - " + c[1] + " | Nota: " + c[2] + " | " + c[3];
            texto.append("0 -18 Td (").append(escaparPdf(linea)).append(") Tj ");
        }
        texto.append("ET");
        String stream = texto.toString();

        String pdf = "%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                + "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n"
                + "4 0 obj << /Length " + stream.length() + " >> stream\n"
                + stream + "\nendstream endobj\n"
                + "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
                + "xref\n0 6\n0000000000 65535 f \n"
                + "trailer << /Size 6 /Root 1 0 R >>\nstartxref\n0\n%%EOF\n";
        return pdf.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String escaparPdf(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", " ");
    }

    public record ResultadoInforme(
            String mensajeCabecera,
            List<String[]> tablaCandidatos,
            Path archivoAdjunto,
            String correoEmpresaAdicional,
            String cuerpoEmpresaAdicional
    ) {}
}
