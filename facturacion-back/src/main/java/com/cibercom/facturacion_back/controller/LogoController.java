package com.cibercom.facturacion_back.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/logos")
@CrossOrigin(origins = "*")
public class LogoController {

    private static final Logger logger = LoggerFactory.getLogger(LogoController.class);

    @GetMapping("/configuracion")
    public ResponseEntity<Map<String, Object>> obtenerConfiguracionLogos() {
        logger.info("Obteniendo configuración de logos");

        try {
            Map<String, Object> response = new HashMap<>();

            String logoUrl = "/images/cibercom-logo.svg";

            String logoBase64 = null;
            try {
                Path logoPath = Paths.get(
                        "C:\\workspace\\Repositories\\FacturacionCibercom\\facturacion-cibercom\\public\\images\\cibercom-logo.svg");
                if (Files.exists(logoPath)) {
                    byte[] logoBytes = Files.readAllBytes(logoPath);
                    logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
                    logger.info("Logo SVG leído exitosamente, tamaño: {} bytes", logoBytes.length);
                } else {
                    logger.warn("Archivo de logo no encontrado en: {}", logoPath.toAbsolutePath());
                    Path[] rutasAlternativas = {
                            Paths.get("../facturacion-cibercom/public/images/cibercom-logo.svg"),
                            Paths.get("../../facturacion-cibercom/public/images/cibercom-logo.svg"),
                            Paths.get("../../../facturacion-cibercom/public/images/cibercom-logo.svg")
                    };

                    for (Path rutaAlternativa : rutasAlternativas) {
                        if (Files.exists(rutaAlternativa)) {
                            byte[] logoBytes = Files.readAllBytes(rutaAlternativa);
                            logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
                            logger.info("Logo SVG leído desde ruta alternativa: {}, tamaño: {} bytes",
                                    rutaAlternativa.toAbsolutePath(), logoBytes.length);
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error leyendo archivo de logo: {}", e.getMessage());
            }

            Map<String, String> customColors = new HashMap<>();
            customColors.put("primary", "#2563eb");
            customColors.put("secondary", "#64748b");
            customColors.put("accent", "#059669");

            response.put("exitoso", true);
            response.put("logoUrl", logoUrl);
            response.put("logoBase64", logoBase64);
            response.put("customColors", customColors);

            logger.info("Configuración de logos enviada exitosamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error obteniendo configuración de logos", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("exitoso", false);
            errorResponse.put("error", "Error interno del servidor: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check solicitado para LogoController");
        return ResponseEntity.ok("LogoService funcionando correctamente");
    }
}