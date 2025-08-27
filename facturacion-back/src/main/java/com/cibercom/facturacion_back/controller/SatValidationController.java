package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.SatValidationRequest;
import com.cibercom.facturacion_back.dto.SatValidationResponse;
import com.cibercom.facturacion_back.service.SatValidationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sat")
@CrossOrigin(origins = "*")
public class SatValidationController {

    @Autowired
    private SatValidationService satValidationService;

    /**
     * Endpoint para validar datos de facturación con el SAT
     * POST /api/sat/validar
     */
    @PostMapping("/validar")
    public ResponseEntity<SatValidationResponse> validarDatosSat(
            @Valid @RequestBody SatValidationRequest request) {
        
        SatValidationResponse response = satValidationService.validarDatosSat(request);
        
        if (response.isValido()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Endpoint de salud para verificar que el servicio esté funcionando
     * GET /api/sat/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Servicio de validación SAT funcionando correctamente");
    }

    /**
     * Endpoint para obtener información sobre los regímenes fiscales válidos
     * GET /api/sat/regimenes
     */
    @GetMapping("/regimenes")
    public ResponseEntity<String[]> obtenerRegimenesFiscales() {
        return ResponseEntity.ok(SatValidationRequest.REGIMENES_FISICA);
    }
} 