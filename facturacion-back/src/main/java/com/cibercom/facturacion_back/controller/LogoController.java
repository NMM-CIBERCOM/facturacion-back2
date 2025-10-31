package com.cibercom.facturacion_back.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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

    // Nuevo endpoint para servir el PNG proporcionado por el usuario
    @GetMapping(value = "/cibercom-png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> obtenerLogoCibercomPng() {
        try {
            // Ruta principal proporcionada por el usuario
            Path logoPathPrincipal = Paths.get(
                    "C:/workspace/Repositories/FacturacionCibercom/facturacion-cibercom/public/images/Logo Cibercom.png");

            // Rutas alternativas relativas por si se ejecuta desde otro directorio
            Path[] rutasAlternativas = new Path[] {
                    Paths.get("../facturacion-cibercom/public/images/Logo Cibercom.png"),
                    Paths.get("../../facturacion-cibercom/public/images/Logo Cibercom.png"),
                    Paths.get("../../../facturacion-cibercom/public/images/Logo Cibercom.png"),
                    Paths.get("public/images/Logo Cibercom.png"),
                    Paths.get("src/main/resources/static/images/Logo Cibercom.png")
            };

            Path rutaEncontrada = null;

            if (Files.exists(logoPathPrincipal)) {
                rutaEncontrada = logoPathPrincipal;
                logger.info("Logo PNG encontrado en ruta principal: {}", logoPathPrincipal.toAbsolutePath());
            } else {
                for (Path alternativa : rutasAlternativas) {
                    if (Files.exists(alternativa)) {
                        rutaEncontrada = alternativa;
                        logger.info("Logo PNG encontrado en ruta alternativa: {}", alternativa.toAbsolutePath());
                        break;
                    }
                }
            }

            if (rutaEncontrada != null) {
                byte[] logoBytes = Files.readAllBytes(rutaEncontrada);
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(logoBytes);
            } else {
                logger.warn("No se encontró el archivo PNG del logo en las rutas configuradas");
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            logger.error("Error leyendo el archivo PNG del logo: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check solicitado para LogoController");
        return ResponseEntity.ok("LogoService funcionando correctamente");
    }

    // Endpoint para subir favicon
    @PostMapping("/favicon/upload")
    public ResponseEntity<Map<String, Object>> subirFavicon(@RequestBody Map<String, String> request) {
        logger.info("Subiendo nuevo favicon");
        
        try {
            String faviconBase64 = request.get("faviconBase64");
            if (faviconBase64 == null || faviconBase64.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("exitoso", false);
                errorResponse.put("error", "Datos de favicon no proporcionados");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Decodificar Base64
            byte[] faviconBytes;
            try {
                // Remover el prefijo "data:image/xxx;base64," si existe
                if (faviconBase64.contains(",")) {
                    faviconBase64 = faviconBase64.split(",")[1];
                }
                faviconBytes = Base64.getDecoder().decode(faviconBase64);
            } catch (IllegalArgumentException e) {
                logger.error("Error decodificando Base64 del favicon: {}", e.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("exitoso", false);
                errorResponse.put("error", "Datos de favicon inválidos");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Determinar extensión basándose en el tipo MIME
            String extension = "png"; // Por defecto
            String dataPart = request.get("faviconBase64");
            if (dataPart != null && dataPart.startsWith("data:")) {
                String mimeType = dataPart.split(";")[0].split("/")[1];
                extension = mimeType.toLowerCase();
                if (extension.equals("svg+xml")) {
                    extension = "svg";
                }
            }

            // Guardar el favicon en diferentes ubicaciones posibles
            String fileName = "favicon." + extension;
            Path[] rutasArchivo = new Path[] {
                Paths.get("H:/Cibercom/facturacion-cibercom/public/images/" + fileName),
                Paths.get("C:/workspace/Repositories/FacturacionCibercom/facturacion-cibercom/public/images/" + fileName),
                Paths.get("../facturacion-cibercom/public/images/" + fileName),
                Paths.get("../../facturacion-cibercom/public/images/" + fileName),
                Paths.get("../../../facturacion-cibercom/public/images/" + fileName),
                Paths.get("public/images/" + fileName)
            };

            boolean guardadoExitoso = false;
            for (Path rutaArchivo : rutasArchivo) {
                try {
                    Path parentDir = rutaArchivo.getParent();
                    if (parentDir != null && !Files.exists(parentDir)) {
                        Files.createDirectories(parentDir);
                    }
                    
                    Files.write(rutaArchivo, faviconBytes);
                    logger.info("Favicon guardado exitosamente en: {}", rutaArchivo.toAbsolutePath());
                    guardadoExitoso = true;
                    break;
                } catch (IOException e) {
                    logger.warn("No se pudo guardar en {}: {}", rutaArchivo, e.getMessage());
                }
            }

            if (!guardadoExitoso) {
                logger.error("No se pudo guardar el favicon en ninguna ubicación");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("exitoso", false);
                errorResponse.put("error", "Error guardando el archivo de favicon");
                return ResponseEntity.internalServerError().body(errorResponse);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("exitoso", true);
            response.put("mensaje", "Favicon actualizado exitosamente");
            response.put("faviconUrl", "/images/" + fileName);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error subiendo favicon", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("exitoso", false);
            errorResponse.put("error", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Endpoint para obtener el favicon
    @GetMapping(value = "/favicon", produces = "image/*")
    public ResponseEntity<byte[]> obtenerFavicon() {
        try {
            Path[] rutasFavicon = new Path[] {
                Paths.get("H:/Cibercom/facturacion-cibercom/public/images/favicon.svg"),
                Paths.get("H:/Cibercom/facturacion-cibercom/public/images/favicon.png"),
                Paths.get("H:/Cibercom/facturacion-cibercom/public/images/favicon.ico"),
                Paths.get("C:/workspace/Repositories/FacturacionCibercom/facturacion-cibercom/public/images/favicon.svg"),
                Paths.get("C:/workspace/Repositories/FacturacionCibercom/facturacion-cibercom/public/images/favicon.png"),
                Paths.get("C:/workspace/Repositories/FacturacionCibercom/facturacion-cibercom/public/images/favicon.ico"),
                Paths.get("../facturacion-cibercom/public/images/favicon.svg"),
                Paths.get("../facturacion-cibercom/public/images/favicon.png"),
                Paths.get("../facturacion-cibercom/public/images/favicon.ico"),
                Paths.get("../../facturacion-cibercom/public/images/favicon.svg"),
                Paths.get("../../facturacion-cibercom/public/images/favicon.png"),
                Paths.get("public/images/favicon.svg"),
                Paths.get("public/images/favicon.png")
            };

            for (Path rutaFavicon : rutasFavicon) {
                if (Files.exists(rutaFavicon)) {
                    byte[] faviconBytes = Files.readAllBytes(rutaFavicon);
                    
                    // Determinar content type basándose en la extensión
                    String contentType = "image/png";
                    String fileName = rutaFavicon.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".svg")) {
                        contentType = "image/svg+xml";
                    } else if (fileName.endsWith(".ico")) {
                        contentType = "image/x-icon";
                    } else if (fileName.endsWith(".png")) {
                        contentType = "image/png";
                    }
                    
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .body(faviconBytes);
                }
            }

            logger.warn("No se encontró ningún archivo de favicon");
            return ResponseEntity.notFound().build();

        } catch (IOException e) {
            logger.error("Error leyendo archivo de favicon: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}