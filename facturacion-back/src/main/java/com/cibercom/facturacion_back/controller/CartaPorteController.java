package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.CartaPorteSaveRequest;
import com.cibercom.facturacion_back.service.CartaPorteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Profile("oracle")
@RequestMapping("/api/carta-porte")
@CrossOrigin(origins = "*")
public class CartaPorteController {

    private static final Logger logger = LoggerFactory.getLogger(CartaPorteController.class);

    @Autowired
    private CartaPorteService cartaPorteService;

    @PostMapping("/guardar")
    public ResponseEntity<?> guardar(
            @RequestBody CartaPorteSaveRequest request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        try {
            logger.info("Guardando carta porte con RFC: {}", request.getRfcCompleto());
            Long usuarioId = parseUsuario(usuarioStr);
            
            // DEBUG: Verificar remolques en el request
            if (request.getComplemento() != null && 
                request.getComplemento().getMercancias() != null &&
                request.getComplemento().getMercancias().getAutotransporte() != null) {
                var autotransporte = request.getComplemento().getMercancias().getAutotransporte();
                var remolques = autotransporte.getRemolques();
                logger.info("DEBUG - Remolques recibidos: {}", remolques == null ? "null" : remolques.size());
                if (remolques != null) {
                    for (int i = 0; i < remolques.size(); i++) {
                        var r = remolques.get(i);
                        logger.info("DEBUG - Remolque[{}]: subTipoRem={}, placa={}", 
                            i, 
                            r == null ? "null" : r.getSubTipoRem(),
                            r == null ? "null" : r.getPlaca());
                    }
                }
            }
            
            CartaPorteService.SaveResult result = cartaPorteService.guardar(request, usuarioId);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", true);
            response.put("id", result.getCartaPorteId());
            response.put("uuid", result.getUuidTimbrado());
            response.put("xmlTimbrado", result.getPacResponse() != null ? result.getPacResponse().getXmlTimbrado() : null);
            response.put("message", result.getPacResponse() != null && Boolean.TRUE.equals(result.getPacResponse().getOk())
                    ? "Carta porte timbrada exitosamente"
                    : "Carta porte guardada");
            response.put("pacResponse", result.getPacResponse());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error guardando carta porte: {}", e.getMessage(), e);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    private Long parseUsuario(String usuarioStr) {
        if (usuarioStr == null || usuarioStr.trim().isEmpty() || "0".equals(usuarioStr.trim())) {
            return null;
        }
        try {
            return Long.parseLong(usuarioStr.trim());
        } catch (NumberFormatException e) {
            logger.warn("Valor de usuario no numérico recibido: '{}', se usará null", usuarioStr);
            return null;
        }
    }

    @PostMapping("/preview-xml")
    public ResponseEntity<?> preview(@RequestBody CartaPorteSaveRequest request) {
        try {
            String xml = cartaPorteService.renderXml(request);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", true);
            response.put("xml", xml);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generando XML de vista previa: {}", e.getMessage(), e);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/preview-pdf")
    public ResponseEntity<byte[]> previewPDF(
            @RequestBody CartaPorteSaveRequest request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        logger.info("Recibida solicitud de vista previa PDF de carta porte");

        try {
            // El logoConfig se obtiene dentro de generarPdfPreview usando obtenerLogoConfig()
            byte[] pdfBytes = cartaPorteService.generarPdfPreview(request, null);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "preview-carta-porte.pdf");

            logger.info("PDF de vista previa de carta porte generado exitosamente. Tamaño: {} bytes", pdfBytes.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            logger.error("Error generando PDF de vista previa de carta porte", e);
            return ResponseEntity.internalServerError().build();
        }
    }

}
