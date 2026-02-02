package com.cibercom.facturacion_back.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*")
public class IndexController {

    /**
     * Redirige la ruta raíz al index.html del frontend
     */
    @GetMapping
    public RedirectView index() {
        return new RedirectView("/index.html");
    }

    /**
     * Endpoint alternativo para verificar el estado del API
     */
    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> apiStatus() {
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

