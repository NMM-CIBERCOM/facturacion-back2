package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.CartaPorteSaveRequest;
import com.cibercom.facturacion_back.service.CartaPorteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/carta-porte")
@CrossOrigin(origins = "*")
public class CartaPorteController {

    private static final Logger logger = LoggerFactory.getLogger(CartaPorteController.class);

    @Autowired
    private CartaPorteService cartaPorteService;

    @PostMapping("/guardar")
    public ResponseEntity<?> guardar(@RequestBody CartaPorteSaveRequest request) {
        try {
            logger.info("Guardando carta porte con RFC: {}", request.getRfcCompleto());
            
            Long idGenerado = cartaPorteService.guardar(request);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", true);
            response.put("id", idGenerado);
            response.put("message", "Carta porte guardada exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error guardando carta porte: {}", e.getMessage(), e);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}