package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.PagoComplementoEnvioRequest;
import com.cibercom.facturacion_back.dto.PagoComplementoRequest;
import com.cibercom.facturacion_back.dto.PagoComplementoResponse;
import com.cibercom.facturacion_back.dto.PagoFacturaLookupResponse;
import com.cibercom.facturacion_back.service.PagoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@Profile("oracle")
@RequestMapping("/api/pagos")
public class PagoController {

    private static final Logger logger = LoggerFactory.getLogger(PagoController.class);

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping("/factura/{uuid}")
    public ResponseEntity<PagoFacturaLookupResponse> buscarFacturaPorUuid(@PathVariable String uuid) {
        PagoFacturaLookupResponse response = new PagoFacturaLookupResponse();
        response.setUuid(uuid);

        if (uuid == null || uuid.trim().isEmpty()) {
            response.setSuccess(false);
            response.setMessage("UUID inválido.");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Long> idOpt = pagoService.buscarFacturaIdPorUuid(uuid.trim());
        if (idOpt.isPresent()) {
            response.setSuccess(true);
            response.setFacturaId(idOpt.get());
            response.setMessage("Factura localizada.");
            return ResponseEntity.ok(response);
        }

        response.setSuccess(false);
        response.setMessage("No se encontró FACTURA_ID para el UUID especificado.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complemento")
    public ResponseEntity<PagoComplementoResponse> registrarComplemento(
            @RequestBody PagoComplementoRequest request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        logger.info("Registrando complemento de pagos para UUID: {}", request != null ? request.getFacturaUuid() : "null");
        Long usuarioId = parseUsuario(usuarioStr);
        PagoComplementoResponse resultado = pagoService.registrarComplemento(request, usuarioId);
        if (resultado.isSuccess()) {
            return ResponseEntity.ok(resultado);
        }
        if (resultado.getPagosInsertados() > 0) {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(resultado);
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

    @PostMapping("/complemento/enviar-correo")
    public ResponseEntity<PagoComplementoResponse> enviarComplementoCorreo(@RequestBody PagoComplementoEnvioRequest request) {
        logger.info("Enviando complemento de pago {} por correo a {}", request != null ? request.getUuidComplemento() : "null", request != null ? request.getCorreoReceptor() : "null");
        PagoComplementoResponse resultado = pagoService.enviarComplementoPorCorreo(request);
        if (resultado.isSuccess()) {
            return ResponseEntity.ok(resultado);
        }
        return ResponseEntity.badRequest().body(resultado);
    }

    @PostMapping("/complemento/preview-pdf")
    public ResponseEntity<byte[]> previewPDFComplemento(
            @RequestBody PagoComplementoRequest request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        logger.info("Recibida solicitud de vista previa PDF de complemento: {}", request != null ? request.getFacturaUuid() : "null");

        try {
            // El logoConfig se obtiene dentro de generarPdfPreviewComplemento usando obtenerLogoConfig()
            byte[] pdfBytes = pagoService.generarPdfPreviewComplemento(request, null);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "preview-complemento-pago.pdf");

            logger.info("PDF de vista previa de complemento generado exitosamente. Tamaño: {} bytes", pdfBytes.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            logger.error("Error generando PDF de vista previa de complemento", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

