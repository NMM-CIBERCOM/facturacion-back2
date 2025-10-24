package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.JustificacionDto;
import com.cibercom.facturacion_back.service.JustificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/justificaciones")
@CrossOrigin(origins = "*")
public class JustificacionController {
    private static final Logger logger = LoggerFactory.getLogger(JustificacionController.class);
    private final JustificacionService service;

    public JustificacionController(JustificacionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listar() {
        logger.info("Solicitud para listar justificaciones");
        Map<String, Object> response = new HashMap<>();
        List<JustificacionDto> list = service.listarTodas();
        response.put("success", true);
        response.put("message", "Justificaciones obtenidas exitosamente");
        response.put("data", list);
        response.put("total", list.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@RequestBody Map<String, String> body) {
        logger.info("Solicitud para crear justificación");
        String descripcion = body.get("descripcion");
        JustificacionDto creada = service.crear(descripcion);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Justificación creada exitosamente");
        response.put("data", creada);
        return ResponseEntity.ok(response);
    }
}