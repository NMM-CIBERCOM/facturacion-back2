package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.NominaSaveRequest;
import com.cibercom.facturacion_back.dto.NominaSaveResponse;
import com.cibercom.facturacion_back.service.NominaService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("oracle")
@RequestMapping("/api/nominas")
public class NominaController {

    private final NominaService nominaService;
    private static final Logger logger = LoggerFactory.getLogger(NominaController.class);

    public NominaController(NominaService nominaService) {
        this.nominaService = nominaService;
    }

    @PostMapping("/guardar")
    public ResponseEntity<NominaSaveResponse> guardar(
            @RequestBody NominaSaveRequest request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        Long usuarioId = parseUsuario(usuarioStr);
        NominaSaveResponse resp = nominaService.guardar(request, usuarioId);
        if (resp.isOk()) return ResponseEntity.ok(resp);
        // Log de error para facilitar diagnóstico en consola
        logger.error("Fallo guardando nómina: {} | errores: {}", resp.getMessage(), resp.getErrors());
        return ResponseEntity.badRequest().body(resp);
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

    @GetMapping("/historial")
    public ResponseEntity<java.util.List<com.cibercom.facturacion_back.dto.NominaHistorialDTO>> historial(@RequestParam("idEmpleado") String idEmpleado) {
        java.util.List<com.cibercom.facturacion_back.dto.NominaHistorialDTO> lista = nominaService.consultarHistorial(idEmpleado);
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/preview-pdf")
    public ResponseEntity<byte[]> previewPDF(
            @RequestBody com.cibercom.facturacion_back.dto.NominaSaveRequest request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        logger.info("Recibida solicitud de vista previa PDF de nómina: {}", request);

        try {
            // El logoConfig se obtiene dentro de generarPdfPreview usando obtenerLogoConfig()
            byte[] pdfBytes = nominaService.generarPdfPreview(request, null);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "preview-nomina.pdf");

            logger.info("PDF de vista previa de nómina generado exitosamente. Tamaño: {} bytes", pdfBytes.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            logger.error("Error generando PDF de vista previa de nómina", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}