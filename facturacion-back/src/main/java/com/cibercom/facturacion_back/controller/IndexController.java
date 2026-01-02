package com.cibercom.facturacion_back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*")
public class IndexController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> index() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Facturacion Backend API está funcionando correctamente");
        response.put("version", "0.0.1-SNAPSHOT");
        response.put("endpoints", Map.of(
            "health", "/api/factura/health",
            "api", "/api/**",
            "documentation", "Ver ENDPOINTS_API.md para más información"
        ));
        return ResponseEntity.ok(response);
    }
}

