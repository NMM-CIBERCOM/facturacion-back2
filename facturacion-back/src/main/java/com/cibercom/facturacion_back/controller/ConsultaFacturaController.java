package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.ConsultaFacturaRequest;
import com.cibercom.facturacion_back.dto.ConsultaFacturaResponse;
import com.cibercom.facturacion_back.service.ConsultaFacturaService;
import com.cibercom.facturacion_back.dao.ConsultaFacturaDAO;
import com.cibercom.facturacion_back.dto.CancelFacturaRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/consulta-facturas")
@CrossOrigin(origins = "*")
public class ConsultaFacturaController {

    private static final Logger logger = LoggerFactory.getLogger(ConsultaFacturaController.class);

    @Autowired
    private ConsultaFacturaService consultaFacturaService;
    @Autowired
    private ConsultaFacturaDAO consultaFacturaDAO;

    @PostMapping("/buscar")
    public ResponseEntity<ConsultaFacturaResponse> consultarFacturas(
            @Valid @RequestBody ConsultaFacturaRequest request) {

        logger.info("Recibida solicitud de consulta de facturas: {}", request);

        try {
            ConsultaFacturaResponse response = consultaFacturaService.consultarFacturas(request);

            logger.info("Respuesta del servicio: exitoso={}, mensaje={}", response.isExitoso(), response.getMensaje());

            if (response.isExitoso()) {
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Consulta fallida: {}", response.getMensaje());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Error interno del servidor al consultar facturas", e);
            ConsultaFacturaResponse errorResponse = ConsultaFacturaResponse.error(
                    "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    public static class CancelCallback {
        public String uuid;
        public String status;
    }

    @PostMapping("/cancelacion/callback")
    public ResponseEntity<ConsultaFacturaResponse> pacCallback(@RequestBody CancelCallback cb) {
        if (cb == null || cb.uuid == null || cb.uuid.isBlank() || cb.status == null) {
            return ResponseEntity.badRequest().body(ConsultaFacturaResponse.error("Callback inválido"));
        }
        boolean ok = consultaFacturaDAO.actualizarEstado(cb.uuid, cb.status.toUpperCase());
        if (ok)
            return ResponseEntity.ok(ConsultaFacturaResponse.exito(java.util.Collections.emptyList()));
        return ResponseEntity.badRequest().body(ConsultaFacturaResponse.error("No se actualizó estado en BD"));
    }

    @PostMapping("/cancelar")
    public ResponseEntity<ConsultaFacturaResponse> cancelarFactura(
            @RequestBody CancelFacturaRequest request) {
        logger.info("Recibida solicitud de cancelación: uuid={}, usuario={}", request.getUuid(), request.getUsuario());
        try {
            ConsultaFacturaResponse response = consultaFacturaService.cancelarFactura(request);
            if (response.isExitoso()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Error interno al cancelar factura", e);
            return ResponseEntity.internalServerError().body(
                    ConsultaFacturaResponse.error("Error interno del servidor: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check solicitado");
        return ResponseEntity.ok("ConsultaFacturaService funcionando correctamente");
    }
}
