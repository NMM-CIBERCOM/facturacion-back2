package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.TiendaDto;
import com.cibercom.facturacion_back.service.TiendaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de tiendas
 */
@RestController
@RequestMapping("/api/tiendas")
@CrossOrigin(origins = "*")
public class TiendaController {

    private static final Logger logger = LoggerFactory.getLogger(TiendaController.class);

    @Autowired
    private TiendaService tiendaService;

    /**
     * Crear una nueva tienda
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearTienda(@Valid @RequestBody TiendaDto tiendaDto) {
        logger.info("Solicitud para crear nueva tienda: {}", tiendaDto.getCodigoTienda());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TiendaDto tiendaCreada = tiendaService.crearTienda(tiendaDto);
            
            response.put("success", true);
            response.put("message", "Tienda creada exitosamente");
            response.put("data", tiendaCreada);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Error al crear tienda: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error al crear la tienda: " + e.getMessage());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Actualizar una tienda existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizarTienda(
            @PathVariable Long id, 
            @Valid @RequestBody TiendaDto tiendaDto) {
        
        logger.info("Solicitud para actualizar tienda con ID: {}", id);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TiendaDto tiendaActualizada = tiendaService.actualizarTienda(id, tiendaDto);
            
            response.put("success", true);
            response.put("message", "Tienda actualizada exitosamente");
            response.put("data", tiendaActualizada);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al actualizar tienda con ID {}: {}", id, e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error al actualizar la tienda: " + e.getMessage());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Listar todas las tiendas con filtros opcionales
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarTiendas(
            @RequestParam(required = false) String estadoTienda,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String zona,
            @RequestParam(required = false) String tipoTienda,
            @RequestParam(required = false) String busqueda) {
        
        logger.info("Solicitud para listar tiendas con filtros");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<TiendaDto> tiendas = tiendaService.listarTiendas(
                    estadoTienda, region, zona, tipoTienda, busqueda);
            
            response.put("success", true);
            response.put("message", "Tiendas obtenidas exitosamente");
            response.put("data", tiendas);
            response.put("total", tiendas.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al listar tiendas: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error al obtener las tiendas: " + e.getMessage());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtener una tienda por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerTiendaPorId(@PathVariable Long id) {
        logger.info("Solicitud para obtener tienda con ID: {}", id);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TiendaDto tienda = tiendaService.obtenerTiendaPorId(id);
            
            response.put("success", true);
            response.put("message", "Tienda obtenida exitosamente");
            response.put("data", tienda);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al obtener tienda con ID {}: {}", id, e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error al obtener la tienda: " + e.getMessage());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Obtener una tienda por código
     */
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Map<String, Object>> obtenerTiendaPorCodigo(@PathVariable String codigo) {
        logger.info("Solicitud para obtener tienda con código: {}", codigo);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TiendaDto tienda = tiendaService.obtenerTiendaPorCodigo(codigo);
            
            response.put("success", true);
            response.put("message", "Tienda obtenida exitosamente");
            response.put("data", tienda);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al obtener tienda con código {}: {}", codigo, e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error al obtener la tienda: " + e.getMessage());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Eliminar una tienda (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminarTienda(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "sistema") String usuarioModificacion) {
        
        logger.info("Solicitud para eliminar tienda con ID: {}", id);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            tiendaService.eliminarTienda(id, usuarioModificacion);
            
            response.put("success", true);
            response.put("message", "Tienda eliminada exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al eliminar tienda con ID {}: {}", id, e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error al eliminar la tienda: " + e.getMessage());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Eliminar una tienda permanentemente
     */
    @DeleteMapping("/{id}/permanente")
    public ResponseEntity<Map<String, Object>> eliminarTiendaPermanente(@PathVariable Long id) {
        logger.info("Solicitud para eliminar permanentemente tienda con ID: {}", id);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            tiendaService.eliminarTiendaPermanente(id);
            
            response.put("success", true);
            response.put("message", "Tienda eliminada permanentemente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al eliminar permanentemente tienda con ID {}: {}", id, e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error al eliminar la tienda: " + e.getMessage());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Obtener estadísticas de tiendas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        logger.info("Solicitud para obtener estadísticas de tiendas");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> estadisticas = tiendaService.obtenerEstadisticas();
            
            response.put("success", true);
            response.put("message", "Estadísticas obtenidas exitosamente");
            response.put("data", estadisticas);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error al obtener las estadísticas: " + e.getMessage());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtener tiendas activas
     */
    @GetMapping("/activas")
    public ResponseEntity<Map<String, Object>> obtenerTiendasActivas() {
        logger.info("Solicitud para obtener tiendas activas");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<TiendaDto> tiendas = tiendaService.obtenerTiendasActivas();
            
            response.put("success", true);
            response.put("message", "Tiendas activas obtenidas exitosamente");
            response.put("data", tiendas);
            response.put("total", tiendas.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al obtener tiendas activas: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error al obtener las tiendas activas: " + e.getMessage());
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Health check del controlador
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("Health check solicitado para TiendaController");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "TiendaController funcionando correctamente");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}