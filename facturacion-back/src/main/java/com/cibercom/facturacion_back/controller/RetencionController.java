package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.RetencionRequest;
import com.cibercom.facturacion_back.dto.RetencionResponse;
import com.cibercom.facturacion_back.service.RetencionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@Profile("oracle")
@RequestMapping("/api/retenciones")
@CrossOrigin(origins = "*")
public class RetencionController {

    private static final Logger logger = LoggerFactory.getLogger(RetencionController.class);

    @Autowired
    private RetencionService retencionService;

    @PostMapping("/registrar")
    public ResponseEntity<RetencionResponse> registrarRetencion(
            @Valid @RequestBody RetencionRequest request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        logger.info("Registrando retención para RFC receptor: {}", request != null ? request.getRfcReceptor() : "null");
        Long usuarioId = parseUsuario(usuarioStr);
        RetencionResponse resultado = retencionService.registrarRetencion(request, usuarioId);
        if (resultado.isSuccess()) {
            return ResponseEntity.ok(resultado);
        }
        return ResponseEntity.badRequest().body(resultado);
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

    @PostMapping("/enviar-correo")
    public ResponseEntity<RetencionResponse> enviarRetencionPorCorreo(@RequestBody com.cibercom.facturacion_back.dto.RetencionEnvioRequest request) {
        logger.info("Enviando retención {} por correo a {}", request != null ? request.getUuidRetencion() : "null", request != null ? request.getCorreoReceptor() : "null");
        RetencionResponse resultado = retencionService.enviarRetencionPorCorreo(request);
        if (resultado.isSuccess()) {
            return ResponseEntity.ok(resultado);
        }
        return ResponseEntity.badRequest().body(resultado);
    }

    @PostMapping("/preview-pdf")
    public ResponseEntity<byte[]> previewPDF(
            @Valid @RequestBody RetencionRequest request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        logger.info("Recibida solicitud de vista previa PDF de retención: {}", request);

        try {
            // El logoConfig se obtiene dentro de generarPdfPreview usando obtenerLogoConfig()
            byte[] pdfBytes = retencionService.generarPdfPreview(request, null);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "preview-retencion.pdf");

            logger.info("PDF de vista previa de retención generado exitosamente. Tamaño: {} bytes", pdfBytes.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            logger.error("Error generando PDF de vista previa de retención", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

