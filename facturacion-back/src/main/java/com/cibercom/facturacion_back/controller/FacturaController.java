package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.FacturaRequest;
import com.cibercom.facturacion_back.dto.FacturaResponse;
import com.cibercom.facturacion_back.dto.ConsultaFacturaRequest;
import com.cibercom.facturacion_back.dto.ConsultaFacturaResponse;
import com.cibercom.facturacion_back.service.FacturaService;
import com.cibercom.facturacion_back.service.ConsultaFacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/factura")
@CrossOrigin(origins = "*")
public class FacturaController {
    
    private static final Logger logger = LoggerFactory.getLogger(FacturaController.class);
    
    @Autowired
    private FacturaService facturaService;
    
    @Autowired
    private ConsultaFacturaService consultaFacturaService;
    
    /**
     * Endpoint para generar y timbrar una factura
     */
    @PostMapping("/generar")
    public ResponseEntity<FacturaResponse> generarFactura(
            @Valid @RequestBody FacturaRequest request) {
        
        logger.info("Recibida solicitud de generación de factura: {}", request);
        
        try {
            FacturaResponse response = facturaService.generarYTimbrarFactura(request);
            
            logger.info("Respuesta del servicio: exitoso={}, mensaje={}", response.isExitoso(), response.getMensaje());
            
            if (response.isExitoso()) {
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Generación de factura fallida: {}", response.getMensaje());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error interno del servidor al generar factura", e);
            FacturaResponse errorResponse = FacturaResponse.builder()
                .exitoso(false)
                .mensaje("Error interno del servidor: " + e.getMessage())
                .timestamp(java.time.LocalDateTime.now())
                .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Endpoint para consultar todas las facturas (sin filtros)
     */
    @GetMapping("/consultar-por-empresa")
    public ResponseEntity<ConsultaFacturaResponse> consultarTodasFacturas() {
        logger.info("Consultando todas las facturas");
        
        try {
            // Crear un request con un campo válido para pasar la validación
            ConsultaFacturaRequest request = new ConsultaFacturaRequest();
            request.setRfcReceptor("TODAS"); // Valor especial para consultar todas
            
            ConsultaFacturaResponse response = consultaFacturaService.consultarFacturas(request);
            
            if (response.isExitoso()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error al consultar todas las facturas", e);
            return ResponseEntity.internalServerError().body(
                ConsultaFacturaResponse.error("Error interno del servidor: " + e.getMessage())
            );
        }
    }
    
    /**
     * Endpoint de prueba para verificar que el servicio esté funcionando
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check solicitado para FacturaController");
        return ResponseEntity.ok("FacturaService funcionando correctamente");
    }
}
