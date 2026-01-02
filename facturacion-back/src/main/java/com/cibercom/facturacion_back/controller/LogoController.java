package com.cibercom.facturacion_back.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.cibercom.facturacion_back.service.CorreoService;
import com.cibercom.facturacion_back.dto.ConfiguracionCorreoResponseDto;

@RestController
@RequestMapping("/api/logos")
@CrossOrigin(origins = "*")
public class LogoController {

    private static final Logger logger = LoggerFactory.getLogger(LogoController.class);

    @Autowired
    private CorreoService correoService;

    // Ruta del archivo donde persistimos el logo activo (data URI)
    // CRÍTICO: Usar ruta absoluta dentro del directorio de Tomcat para persistencia
    private Path getLogoBase64Path() {
        // Intentar usar variable de entorno o propiedad del sistema
        String tomcatBase = System.getProperty("catalina.base");
        if (tomcatBase != null && !tomcatBase.isEmpty()) {
            // Si estamos en Tomcat, guardar en conf/ dentro de Tomcat
            return Paths.get(tomcatBase, "conf", "logo-base64.txt");
        }
        
        // Fallback: usar directorio de trabajo actual/config
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isEmpty()) {
            return Paths.get(userDir, "config", "logo-base64.txt");
        }
        
        // Último fallback: ruta relativa
        return Paths.get("config", "logo-base64.txt");
    }

    private String leerLogoBase64Activo() {
        try {
            Path p = getLogoBase64Path();
            if (Files.exists(p)) {
                String content = Files.readString(p, StandardCharsets.UTF_8);
                if (content != null && !content.trim().isEmpty()) {
                    return content.trim();
                }
            }
        } catch (IOException e) {
            logger.warn("No se pudo leer logo activo: {}", e.getMessage());
        }
        return null;
    }

    private Path guardarLogoBase64Activo(String dataUri) throws IOException {
        Path p = getLogoBase64Path();
        if (p.getParent() != null && !Files.exists(p.getParent())) {
            Files.createDirectories(p.getParent());
            logger.info("Directorio creado: {}", p.getParent().toAbsolutePath());
        }
        Files.writeString(p, dataUri != null ? dataUri.trim() : "", StandardCharsets.UTF_8);
        logger.info("Logo activo guardado en {} ({} bytes)", p.toAbsolutePath(), 
                   dataUri != null ? dataUri.length() : 0);
        return p;
    }

    @GetMapping("/configuracion")
    public ResponseEntity<Map<String, Object>> obtenerConfiguracionLogos() {
        logger.info("Obteniendo configuración de logos");

        try {
            Map<String, Object> response = new HashMap<>();

            String logoUrl = "/images/cibercom-logo.svg";

            String logoBase64 = null;
            try {
                // Si existe logo activo persistido, preferirlo
                String activo = leerLogoBase64Activo();
                if (activo != null) {
                    logoBase64 = activo;
                    logger.info("Usando logoBase64 activo persistido ({} chars)", activo.length());
                } else {
                    // CRÍTICO: Intentar cargar desde classpath (recursos del WAR) primero
                    try {
                        java.io.InputStream logoStream = getClass().getClassLoader()
                                .getResourceAsStream("static/images/cibercom-logo.svg");
                        if (logoStream != null) {
                            byte[] logoBytes = logoStream.readAllBytes();
                            logoStream.close();
                            logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
                            logger.info("Logo SVG cargado desde classpath (recursos del WAR), tamaño: {} bytes", logoBytes.length);
                        }
                    } catch (Exception e) {
                        logger.warn("No se pudo cargar logo SVG desde classpath: {}", e.getMessage());
                    }
                    
                    // Fallback: Intentar desde rutas del sistema de archivos (solo si no está en classpath)
                    if (logoBase64 == null) {
                        Path[] rutasAlternativas = {
                                Paths.get("src/main/resources/static/images/cibercom-logo.svg"),
                                Paths.get("static/images/cibercom-logo.svg"),
                                Paths.get("images/cibercom-logo.svg"),
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
                }
            } catch (IOException e) {
                logger.error("Error leyendo archivo de logo: {}", e.getMessage());
            }

            Map<String, String> customColors = new HashMap<>();
            // Determinar color primario desde configuración de formato de correo
            String colorPrimario = "#2563eb"; // fallback
            try {
                ConfiguracionCorreoResponseDto cfg = correoService.obtenerConfiguracionMensajes();
                String colorCfg = (cfg != null && cfg.getFormatoCorreo() != null) ? cfg.getFormatoCorreo().getColorTexto() : null;
                if (colorCfg != null) {
                    colorCfg = colorCfg.trim();
                    if (colorCfg.startsWith("#") && (colorCfg.length() == 7 || colorCfg.length() == 9)) {
                        colorPrimario = colorCfg;
                    } else {
                        logger.warn("ColorTexto inválido recibido: {}. Usando fallback.", colorCfg);
                    }
                } else {
                    logger.info("Configuración de formato de correo sin colorTexto. Usando fallback.");
                }
            } catch (Exception e) {
                logger.warn("No se pudo obtener color de formato de correo: {}", e.getMessage());
            }

            customColors.put("primary", colorPrimario);
            customColors.put("secondary", "#64748b");
            customColors.put("accent", "#059669");

            response.put("exitoso", true);
            response.put("logoUrl", logoUrl);
            response.put("logoBase64", logoBase64);
            response.put("customColors", customColors);

            logger.info("Configuración de logos enviada exitosamente (primary: {})", colorPrimario);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error obteniendo configuración de logos", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("exitoso", false);
            errorResponse.put("error", "Error interno del servidor: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Nuevo: Persistir logo activo en backend
    @PostMapping("/guardar")
    public ResponseEntity<Map<String, Object>> guardarLogo(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String logoBase64 = request != null ? request.get("logoBase64") : null;
            if (logoBase64 == null || logoBase64.trim().isEmpty()) {
                response.put("exitoso", false);
                response.put("mensaje", "logoBase64 vacío o no proporcionado");
                logger.warn("Intento de guardar logo vacío rechazado");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validar que sea un data URI válido
            String trimmed = logoBase64.trim();
            if (!trimmed.startsWith("data:image/")) {
                logger.warn("Logo recibido no es un data URI válido, agregando prefijo si es necesario");
                // Si es solo base64, intentar agregar prefijo PNG
                if (!trimmed.contains(",")) {
                    trimmed = "data:image/png;base64," + trimmed;
                }
            }
            
            Path logoPath = guardarLogoBase64Activo(trimmed);
            logger.info("Logo guardado exitosamente en: {} (tamaño: {} chars)", 
                       logoPath.toAbsolutePath(), trimmed.length());
            
            // Verificar que se guardó correctamente
            String verificado = leerLogoBase64Activo();
            if (verificado == null || verificado.trim().isEmpty()) {
                logger.error("Logo guardado pero no se pudo leer después de guardar");
                response.put("exitoso", false);
                response.put("mensaje", "Logo guardado pero no se pudo verificar");
                return ResponseEntity.internalServerError().body(response);
            }
            
            response.put("exitoso", true);
            response.put("mensaje", "Logo guardado correctamente en: " + logoPath.toAbsolutePath());
            response.put("ruta", logoPath.toAbsolutePath().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error guardando logo activo: {}", e.getMessage(), e);
            response.put("exitoso", false);
            response.put("mensaje", "Error interno: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Nuevo: Obtener logo activo persistido
    @GetMapping("/activo")
    public ResponseEntity<Map<String, Object>> obtenerLogoActivo() {
        Map<String, Object> response = new HashMap<>();
        try {
            String base64 = leerLogoBase64Activo();
            response.put("exitoso", true);
            response.put("logoBase64", base64);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error obteniendo logo activo: {}", e.getMessage());
            response.put("exitoso", false);
            response.put("mensaje", "Error interno: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Endpoint de imagen PNG: primero intenta devolver el logo activo si es PNG
    @GetMapping(value = "/cibercom-png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> obtenerLogoCibercomPng() {
        try {
            // Preferir logo activo persistido si es PNG
            String activo = leerLogoBase64Activo();
            if (activo != null && !activo.isBlank()) {
                String lower = activo.toLowerCase();
                String raw = activo;
                if (activo.contains(",")) {
                    raw = activo.split(",", 2)[1];
                }
                // Solo servimos si parece PNG para evitar incompatibilidades
                if (lower.startsWith("data:image/png;base64,")) {
                    byte[] decoded = Base64.getDecoder().decode(raw);
                    logger.info("Logo PNG servido desde logo activo persistido ({} bytes)", decoded.length);
                    return ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_PNG)
                            .body(decoded);
                }
            }

            // CRÍTICO: Intentar cargar desde classpath (recursos del WAR) primero
            try {
                java.io.InputStream logoStream = getClass().getClassLoader()
                        .getResourceAsStream("static/images/Logo Cibercom.png");
                if (logoStream != null) {
                    byte[] logoBytes = logoStream.readAllBytes();
                    logoStream.close();
                    logger.info("Logo PNG cargado desde classpath (recursos del WAR): {} bytes", logoBytes.length);
                    return ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_PNG)
                            .body(logoBytes);
                }
            } catch (Exception e) {
                logger.warn("No se pudo cargar logo desde classpath: {}", e.getMessage());
            }

            // Fallback: Intentar desde rutas del sistema de archivos (solo si no está en classpath)
            Path[] rutasAlternativas = new Path[] {
                    Paths.get("src/main/resources/static/images/Logo Cibercom.png"),
                    Paths.get("static/images/Logo Cibercom.png"),
                    Paths.get("images/Logo Cibercom.png"),
                    Paths.get("../facturacion-cibercom/public/images/Logo Cibercom.png"),
                    Paths.get("../../facturacion-cibercom/public/images/Logo Cibercom.png"),
                    Paths.get("public/images/Logo Cibercom.png")
            };

            for (Path alternativa : rutasAlternativas) {
                if (Files.exists(alternativa)) {
                    byte[] logoBytes = Files.readAllBytes(alternativa);
                    logger.info("Logo PNG encontrado en ruta alternativa: {} ({} bytes)", 
                               alternativa.toAbsolutePath(), logoBytes.length);
                    return ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_PNG)
                            .body(logoBytes);
                }
            }

            logger.warn("No se encontró el archivo PNG del logo en ninguna ruta configurada");
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            logger.error("Error leyendo el archivo PNG del logo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check solicitado para LogoController");
        return ResponseEntity.ok("LogoService funcionando correctamente");
    }
}