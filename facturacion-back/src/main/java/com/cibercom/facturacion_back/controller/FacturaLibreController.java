package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.FacturaLibreSaveRequest;
import com.cibercom.facturacion_back.service.FacturaLibreService;
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
@RequestMapping("/api/factura-libre")
@CrossOrigin(origins = "*")
public class FacturaLibreController {

    private static final Logger logger = LoggerFactory.getLogger(FacturaLibreController.class);

    @Autowired
    private FacturaLibreService facturaLibreService;

    @PostMapping("/guardar")
    public ResponseEntity<?> guardar(@RequestBody FacturaLibreSaveRequest request) {
        try {
            logger.info("Guardando factura libre con RFC: {}", request.getRfc());
            
            String idGenerado = facturaLibreService.guardar(request);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", true);
            response.put("uuid", idGenerado);
            response.put("message", "Factura libre guardada exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación guardando factura libre: {}", e.getMessage());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Error guardando factura libre: {}", e.getMessage(), e);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", false);
            response.put("error", "Error interno del servidor: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}