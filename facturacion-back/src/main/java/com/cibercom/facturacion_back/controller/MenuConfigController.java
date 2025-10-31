package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.MenuConfigDto;
import com.cibercom.facturacion_back.service.MenuConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la configuración de menús
 * Solo accesible para administradores
 */
@RestController
@RequestMapping("/api/menu-config")
@CrossOrigin(origins = "http://localhost:5173")
public class MenuConfigController {

    private static final Logger logger = LoggerFactory.getLogger(MenuConfigController.class);

    @Autowired
    private MenuConfigService menuConfigService;

    /**
     * Obtiene la configuración de menús para un perfil específico
     */
    @GetMapping("/perfil/{idPerfil}")
    public ResponseEntity<List<MenuConfigDto>> obtenerConfiguracionPorPerfil(@PathVariable Integer idPerfil) {
        try {
            logger.info("Obteniendo configuración de menús para perfil: {}", idPerfil);
            List<MenuConfigDto> configuraciones = menuConfigService.obtenerPestañasPorPerfil(idPerfil);
            logger.info("Devolviendo {} configuraciones de pestañas para perfil {}", configuraciones.size(), idPerfil);
            return ResponseEntity.ok(configuraciones);
        } catch (Exception e) {
            logger.error("Error al obtener configuración de menús: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene todas las configuraciones de menús
     */
    @GetMapping("/todas")
    public ResponseEntity<List<MenuConfigDto>> obtenerTodasLasConfiguraciones() {
        try {
            logger.info("Obteniendo todas las configuraciones de menús");
            List<MenuConfigDto> configuraciones = menuConfigService.obtenerTodasLasConfiguraciones();
            return ResponseEntity.ok(configuraciones);
        } catch (Exception e) {
            logger.error("Error al obtener todas las configuraciones: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Actualiza la visibilidad de un menú
     */
    @PutMapping("/visibilidad/{idConfig}")
    public ResponseEntity<Map<String, Object>> actualizarVisibilidad(
            @PathVariable Long idConfig,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        
        try {
            Boolean isVisible = (Boolean) request.get("isVisible");
            logger.info("Actualizando visibilidad del menú {} a {} por usuario {}", idConfig, isVisible, usuario);
            
            Map<String, Object> response = menuConfigService.actualizarVisibilidadMenu(idConfig, isVisible, usuario);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al actualizar visibilidad del menú: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "Error interno del servidor"
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Actualiza el orden de un menú
     */
    @PutMapping("/orden/{idConfig}")
    public ResponseEntity<Map<String, Object>> actualizarOrden(
            @PathVariable Long idConfig,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        
        try {
            Integer orden = (Integer) request.get("orden");
            logger.info("Actualizando orden del menú {} a {} por usuario {}", idConfig, orden, usuario);
            
            Map<String, Object> response = menuConfigService.actualizarOrdenMenu(idConfig, orden, usuario);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al actualizar orden del menú: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "Error interno del servidor"
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Obtiene los perfiles disponibles
     */
    @GetMapping("/perfiles")
    public ResponseEntity<List<Map<String, Object>>> obtenerPerfiles() {
        try {
            logger.info("Obteniendo perfiles disponibles");
            List<Map<String, Object>> perfiles = menuConfigService.obtenerPerfiles();
            return ResponseEntity.ok(perfiles);
        } catch (Exception e) {
            logger.error("Error al obtener perfiles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check del controlador
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("Health check solicitado para MenuConfigController");
        
        Map<String, Object> response = Map.of(
            "success", true,
            "message", "MenuConfigController funcionando correctamente",
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(response);
    }
}
