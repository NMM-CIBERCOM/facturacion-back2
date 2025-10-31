package com.cibercom.facturacion_back.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador de prueba para diagnosticar problemas de autenticación
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    /**
     * Endpoint simple para probar conectividad
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Backend funcionando correctamente");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para probar login simple
     */
    @PostMapping("/simple-login")
    public ResponseEntity<Map<String, Object>> simpleLogin(@RequestBody Map<String, String> loginData) {
        Map<String, Object> response = new HashMap<>();
        
        String usuario = loginData.get("usuario");
        String password = loginData.get("password");
        
        try {
            // Verificación simple
            if (usuario != null && password != null) {
                response.put("success", true);
                response.put("message", "Datos recibidos correctamente");
                response.put("usuario", usuario);
                response.put("password_length", password.length());
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Usuario o contraseña faltantes");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Endpoint para probar respuesta JSON
     */
    @PostMapping("/json-test")
    public ResponseEntity<Map<String, Object>> jsonTest(@RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "JSON recibido correctamente");
        response.put("received_data", data);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
