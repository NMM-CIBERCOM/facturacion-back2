package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.PerfilDto;
import com.cibercom.facturacion_back.service.MenuConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador REST para la gestión de configuraciones de menú
 */
@RestController
@RequestMapping("/api/menu-config")
@CrossOrigin(origins = "*")
public class MenuConfigController {

    private static final Logger logger = LoggerFactory.getLogger(MenuConfigController.class);

    @Autowired
    private MenuConfigService menuConfigService;

    /**
     * Obtiene todos los perfiles disponibles
     */
    @GetMapping("/perfiles")
    public ResponseEntity<?> obtenerPerfiles() {
        try {
            logger.info("Obteniendo lista de perfiles para configuración de menú");
            
            List<PerfilDto> perfiles = menuConfigService.obtenerPerfiles();
            return ResponseEntity.ok(perfiles);
            
        } catch (Exception e) {
            logger.error("Error al obtener perfiles: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al cargar los perfiles: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Obtiene las configuraciones de menú para un perfil específico
     * Retorna tanto pestañas principales como pantallas específicas
     */
    @GetMapping("/perfil/{idPerfil}")
    public ResponseEntity<?> obtenerConfiguracionesPorPerfil(@PathVariable Integer idPerfil) {
        try {
            logger.info("Obteniendo configuraciones de menú para perfil: {}", idPerfil);
            
            List<Map<String, Object>> configuraciones = menuConfigService.obtenerConfiguracionesPorPerfil(idPerfil);
            return ResponseEntity.ok(configuraciones);
            
        } catch (Exception e) {
            logger.error("Error al obtener configuraciones para perfil {}: {}", idPerfil, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al cargar configuraciones: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Obtiene solo las pantallas específicas (con MENU_PATH) para un perfil
     */
    @GetMapping("/pantallas/{idPerfil}")
    public ResponseEntity<?> obtenerPantallasPorPerfil(@PathVariable Integer idPerfil) {
        try {
            logger.info("Obteniendo pantallas para perfil: {}", idPerfil);
            
            List<Map<String, Object>> pantallas = menuConfigService.obtenerPantallasPorPerfil(idPerfil);
            return ResponseEntity.ok(pantallas);
            
        } catch (Exception e) {
            logger.error("Error al obtener pantallas para perfil {}: {}", idPerfil, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al cargar pantallas: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Actualiza la visibilidad de una configuración de menú
     */
    @PutMapping("/visibilidad/{idConfig}")
    public ResponseEntity<Map<String, Object>> actualizarVisibilidad(
            @PathVariable Integer idConfig,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "admin") String usuarioMod) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Object isVisibleObj = request.get("isVisible");
            Boolean isVisible = null;
            
            // Manejar diferentes tipos de entrada (Boolean, String, Integer)
            if (isVisibleObj instanceof Boolean) {
                isVisible = (Boolean) isVisibleObj;
            } else if (isVisibleObj instanceof String) {
                String str = (String) isVisibleObj;
                isVisible = "true".equalsIgnoreCase(str) || "1".equals(str);
            } else if (isVisibleObj instanceof Integer) {
                isVisible = ((Integer) isVisibleObj) == 1;
            } else if (isVisibleObj instanceof Number) {
                isVisible = ((Number) isVisibleObj).intValue() == 1;
            }
            
            if (isVisible == null) {
                response.put("success", false);
                response.put("message", "El campo 'isVisible' es requerido y debe ser un valor booleano");
                return ResponseEntity.badRequest().body(response);
            }
            
            logger.info("Actualizando visibilidad de config {} a {} (usuario: {})", idConfig, isVisible, usuarioMod);
            
            response = menuConfigService.actualizarVisibilidad(idConfig, isVisible, usuarioMod);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al actualizar visibilidad de config {}: {}", idConfig, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al actualizar visibilidad: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Actualiza la visibilidad de una pantalla específica
     */
    @PutMapping("/pantalla-visibilidad/{idConfig}")
    public ResponseEntity<Map<String, Object>> actualizarVisibilidadPantalla(
            @PathVariable Integer idConfig,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "admin") String usuarioMod) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Object isVisibleObj = request.get("isVisible");
            Boolean isVisible = null;
            
            // Manejar diferentes tipos de entrada (Boolean, String, Integer)
            if (isVisibleObj instanceof Boolean) {
                isVisible = (Boolean) isVisibleObj;
            } else if (isVisibleObj instanceof String) {
                String str = (String) isVisibleObj;
                isVisible = "true".equalsIgnoreCase(str) || "1".equals(str);
            } else if (isVisibleObj instanceof Integer) {
                isVisible = ((Integer) isVisibleObj) == 1;
            } else if (isVisibleObj instanceof Number) {
                isVisible = ((Number) isVisibleObj).intValue() == 1;
            }
            
            if (isVisible == null) {
                response.put("success", false);
                response.put("message", "El campo 'isVisible' es requerido y debe ser un valor booleano");
                return ResponseEntity.badRequest().body(response);
            }
            
            logger.info("Actualizando visibilidad de pantalla {} a {} (usuario: {})", idConfig, isVisible, usuarioMod);
            
            response = menuConfigService.actualizarVisibilidadPantalla(idConfig, isVisible, usuarioMod);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al actualizar visibilidad de pantalla {}: {}", idConfig, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al actualizar visibilidad: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Health check del controlador
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("Health check solicitado para MenuConfigController");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "MenuConfigController funcionando correctamente");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}

