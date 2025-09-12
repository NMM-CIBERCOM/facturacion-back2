package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.service.ITextPdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/itext")
@CrossOrigin(origins = "*")
public class ITextPdfController {
    
    private static final Logger logger = LoggerFactory.getLogger(ITextPdfController.class);
    
    @Autowired
    private ITextPdfService iTextPdfService;
    
    @GetMapping("/test-logo")
    public ResponseEntity<Map<String, Object>> testLogo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Buscar el logo en diferentes ubicaciones
            String[] posiblesPaths = {
                "src/main/resources/static/images/Logo Cibercom.png",
                "src/main/resources/static/images/logo.png",
                "public/images/Logo Cibercom.png"
            };
            
            String logoEncontrado = null;
            byte[] logoBytes = null;
            
            for (String pathStr : posiblesPaths) {
                Path logoPath = Paths.get(pathStr);
                if (Files.exists(logoPath)) {
                    logoEncontrado = pathStr;
                    logoBytes = Files.readAllBytes(logoPath);
                    break;
                }
            }
            
            if (logoEncontrado != null) {
                String logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
                
                response.put("exitoso", true);
                response.put("mensaje", "Logo encontrado exitosamente");
                response.put("logoPath", logoEncontrado);
                response.put("logoSize", logoBytes.length);
                response.put("logoBase64", logoBase64.substring(0, Math.min(100, logoBase64.length())) + "...");
                
                logger.info("Logo encontrado en: {} - Tamaño: {} bytes", logoEncontrado, logoBytes.length);
            } else {
                response.put("exitoso", false);
                response.put("mensaje", "Logo no encontrado en ninguna ubicación");
                response.put("pathsBuscados", posiblesPaths);
                
                logger.warn("Logo no encontrado en ninguna de las ubicaciones: {}", String.join(", ", posiblesPaths));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al buscar logo: ", e);
            response.put("exitoso", false);
            response.put("mensaje", "Error al buscar logo: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/generar-pdf")
    public ResponseEntity<byte[]> generarPdf(@RequestBody Map<String, Object> request) {
        try {
            logger.info("Recibida solicitud de generación de PDF con iText: {}", request);
            
            // Extraer datos de la solicitud
            Map<String, Object> facturaData = (Map<String, Object>) request.get("facturaData");
            Map<String, Object> logoConfig = (Map<String, Object>) request.get("logoConfig");
            
            if (facturaData == null) {
                // Crear datos de prueba
                facturaData = crearDatosPrueba();
            }
            
            if (logoConfig == null) {
                // Crear configuración de logo por defecto
                logoConfig = crearLogoConfigPorDefecto();
            }
            
            // Generar el PDF
            byte[] pdfBytes = iTextPdfService.generarPdfConLogo(request, logoConfig);
            
            // Configurar headers para la respuesta
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "factura-itext.pdf");
            headers.setContentLength(pdfBytes.length);
            
            logger.info("PDF generado exitosamente con iText. Tamaño: {} bytes", pdfBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
                
        } catch (IOException e) {
            logger.error("Error generando PDF con iText: ", e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("Error inesperado generando PDF: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/generar-pdf-prueba")
    public ResponseEntity<byte[]> generarPdfPrueba() {
        try {
            logger.info("Generando PDF de prueba con iText");
            
            // Crear datos de prueba
            Map<String, Object> request = new HashMap<>();
            request.put("facturaData", crearDatosPrueba());
            request.put("logoConfig", crearLogoConfigPorDefecto());
            
            // Generar el PDF
            byte[] pdfBytes = iTextPdfService.generarPdfConLogo(request, crearLogoConfigPorDefecto());
            
            // Configurar headers para la respuesta
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "factura-prueba-itext.pdf");
            headers.setContentLength(pdfBytes.length);
            
            logger.info("PDF de prueba generado exitosamente con iText. Tamaño: {} bytes", pdfBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
                
        } catch (Exception e) {
            logger.error("Error generando PDF de prueba: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private Map<String, Object> crearDatosPrueba() {
        Map<String, Object> facturaData = new HashMap<>();
        facturaData.put("uuid", "TEST-UUID-" + System.currentTimeMillis());
        facturaData.put("serie", "TEST");
        facturaData.put("folio", "001");
        facturaData.put("nombreEmisor", "Empresa de Prueba");
        facturaData.put("nombreReceptor", "Cliente de Prueba");
        facturaData.put("rfcEmisor", "TEST123456789");
        facturaData.put("rfcReceptor", "XEXX010101000");
        facturaData.put("fechaEmision", java.time.Instant.now().toString());
        facturaData.put("importe", 1000.0);
        facturaData.put("subtotal", 862.07);
        facturaData.put("iva", 137.93);
        facturaData.put("metodoPago", "PUE");
        facturaData.put("formaPago", "01");
        facturaData.put("usoCFDI", "G01");
        
        return facturaData;
    }
    
    private Map<String, Object> crearLogoConfigPorDefecto() {
        Map<String, Object> logoConfig = new HashMap<>();
        logoConfig.put("logoUrl", "/images/Logo Cibercom.png");
        
        Map<String, String> customColors = new HashMap<>();
        customColors.put("primary", "#1d4ed8");
        customColors.put("primaryDark", "#1e40af");
        customColors.put("secondary", "#3b82f6");
        customColors.put("secondaryDark", "#2563eb");
        customColors.put("accent", "#06b6d4");
        customColors.put("accentDark", "#0891b2");
        customColors.put("background", "#ffffff");
        customColors.put("surface", "#f9fafb");
        customColors.put("text", "#1f2937");
        customColors.put("textSecondary", "#6b7280");
        
        logoConfig.put("customColors", customColors);
        
        return logoConfig;
    }
}