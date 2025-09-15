package com.cibercom.facturacion_back.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestPdfController {

    private static final Logger logger = LoggerFactory.getLogger(TestPdfController.class);

    @GetMapping("/pdf-logo")
    public ResponseEntity<byte[]> generarPDFConLogo() {
        logger.info("Generando PDF de prueba con logo");

        try {
            String logoBase64 = obtenerLogoBase64();

            String htmlContent = generarHTMLSimple(logoBase64);

            byte[] pdfBytes = htmlContent.getBytes("UTF-8");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.setContentDispositionFormData("inline", "test-logo.html");

            logger.info("PDF de prueba generado exitosamente con logo de {} caracteres",
                    logoBase64 != null ? logoBase64.length() : 0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            logger.error("Error generando PDF de prueba", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String obtenerLogoBase64() {
        try {
            Path logoPath = Paths.get(
                    "C:\\workspace\\Repositories\\FacturacionCibercom\\facturacion-cibercom\\public\\images\\cibercom-logo.svg");
            if (Files.exists(logoPath)) {
                byte[] logoBytes = Files.readAllBytes(logoPath);
                String base64 = Base64.getEncoder().encodeToString(logoBytes);
                logger.info("Logo leído exitosamente: {} bytes, base64: {} caracteres", logoBytes.length,
                        base64.length());
                return base64;
            } else {
                logger.warn("Archivo de logo no encontrado en: {}", logoPath.toAbsolutePath());
                Path[] rutasAlternativas = {
                        Paths.get("../facturacion-cibercom/public/images/cibercom-logo.svg"),
                        Paths.get("../../facturacion-cibercom/public/images/cibercom-logo.svg")
                };

                for (Path rutaAlternativa : rutasAlternativas) {
                    if (Files.exists(rutaAlternativa)) {
                        byte[] logoBytes = Files.readAllBytes(rutaAlternativa);
                        String base64 = Base64.getEncoder().encodeToString(logoBytes);
                        logger.info("Logo leído desde ruta alternativa: {}, {} bytes", rutaAlternativa.toAbsolutePath(),
                                logoBytes.length);
                        return base64;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error leyendo archivo de logo: {}", e.getMessage());
        }

        String logoSvgPrueba = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 240 80\"><rect width=\"240\" height=\"80\" fill=\"#2563eb\"/><text x=\"120\" y=\"45\" text-anchor=\"middle\" fill=\"white\" font-family=\"Arial\" font-size=\"16\">CIBERCOM LOGO</text></svg>";
        return Base64.getEncoder().encodeToString(logoSvgPrueba.getBytes());
    }

    private String generarHTMLSimple(String logoBase64) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>Prueba Logo PDF</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }\n");
        html.append(
                ".container { background: white; padding: 40px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
        html.append(
                ".logo { max-width: 300px; max-height: 120px; border: 2px solid #ddd; padding: 10px; margin: 20px 0; }\n");
        html.append(".info { background: #e3f2fd; padding: 15px; border-radius: 4px; margin: 20px 0; }\n");
        html.append("h1 { color: #2563eb; text-align: center; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        html.append("<div class='container'>\n");
        html.append("<h1>🧪 PRUEBA DE LOGO EN PDF</h1>\n");

        if (logoBase64 != null && !logoBase64.isEmpty()) {
            html.append("<div class='info'>✅ Logo cargado exitosamente (Base64: ").append(logoBase64.length())
                    .append(" caracteres)</div>\n");
            html.append("<div style='text-align: center;'>\n");
            html.append("<img src='data:image/svg+xml;base64,").append(logoBase64)
                    .append("' alt='Logo Cibercom' class='logo' />\n");
            html.append("</div>\n");
        } else {
            html.append("<div class='info'>❌ No se pudo cargar el logo</div>\n");
        }

        html.append("<div class='info'>\n");
        html.append("<strong>Información técnica:</strong><br>\n");
        html.append("• Formato: SVG en Base64<br>\n");
        html.append("• Tamaño Base64: ").append(logoBase64 != null ? logoBase64.length() : 0)
                .append(" caracteres<br>\n");
        html.append("• Fecha: ").append(java.time.LocalDateTime.now()).append("\n");
        html.append("</div>\n");

        html.append("</div>\n");
        html.append("</body>\n</html>");

        return html.toString();
    }
}