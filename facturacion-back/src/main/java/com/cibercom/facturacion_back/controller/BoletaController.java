package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.BoletaDto;
import com.cibercom.facturacion_back.service.BoletaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/boletas")
@CrossOrigin(origins = "*")
public class BoletaController {
    private static final Logger logger = LoggerFactory.getLogger(BoletaController.class);
    private final BoletaService boletaService;

    public BoletaController(BoletaService boletaService) {
        this.boletaService = boletaService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearBoleta(@RequestBody BoletaDto dto) {
        logger.info("Solicitud para crear boleta");
        Map<String, Object> response = new HashMap<>();
        try {
            BoletaDto creada = boletaService.crearBoleta(dto);
            response.put("success", true);
            response.put("message", "Boleta creada exitosamente");
            response.put("data", creada);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al crear boleta: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al crear la boleta: " + e.getMessage());
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}