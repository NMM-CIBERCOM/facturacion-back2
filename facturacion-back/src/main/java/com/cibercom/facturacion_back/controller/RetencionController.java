package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.RetencionRequest;
import com.cibercom.facturacion_back.dto.RetencionResponse;
import com.cibercom.facturacion_back.service.RetencionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/retenciones")
@CrossOrigin(origins = "*")
public class RetencionController {

    private static final Logger logger = LoggerFactory.getLogger(RetencionController.class);

    @Autowired
    private RetencionService retencionService;

    @PostMapping("/registrar")
    public ResponseEntity<RetencionResponse> registrarRetencion(@Valid @RequestBody RetencionRequest request) {
        logger.info("Registrando retención para RFC receptor: {}", request != null ? request.getRfcReceptor() : "null");
        RetencionResponse resultado = retencionService.registrarRetencion(request);
        if (resultado.isSuccess()) {
            return ResponseEntity.ok(resultado);
        }
        return ResponseEntity.badRequest().body(resultado);
    }

    @PostMapping("/enviar-correo")
    public ResponseEntity<RetencionResponse> enviarRetencionPorCorreo(@RequestBody com.cibercom.facturacion_back.dto.RetencionEnvioRequest request) {
        logger.info("Enviando retención {} por correo a {}", request != null ? request.getUuidRetencion() : "null", request != null ? request.getCorreoReceptor() : "null");
        RetencionResponse resultado = retencionService.enviarRetencionPorCorreo(request);
        if (resultado.isSuccess()) {
            return ResponseEntity.ok(resultado);
        }
        return ResponseEntity.badRequest().body(resultado);
    }
}

