package com.cibercom.facturacion_back.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cibercom.facturacion_back.dto.FormatoCorreoDto;
import com.cibercom.facturacion_back.service.FormatoCorreoService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador para la configuración de formato de correos electrónicos
 */
@RestController
@RequestMapping("/api/formato-correo")
@CrossOrigin(origins = "*")
public class FormatoCorreoController {
    
    private static final Logger logger = LoggerFactory.getLogger(FormatoCorreoController.class);
    
    @Autowired
    private FormatoCorreoService formatoCorreoService;
    
    /**
     * Obtiene la configuración de formato activa
     * 
     * @return ResponseEntity con la configuración activa
     */
    @GetMapping("/activa")
    public ResponseEntity<FormatoCorreoDto> obtenerConfiguracionActiva() {
        try {
            FormatoCorreoDto configuracion = formatoCorreoService.obtenerConfiguracionActiva();
            return ResponseEntity.ok(configuracion);
        } catch (Exception e) {
            logger.error("Error al obtener configuración de formato activa: {}", e.getMessage(), e);
            
            // Retornar configuración por defecto en caso de error
            FormatoCorreoDto configuracionDefault = new FormatoCorreoDto();
            configuracionDefault.setTipoFuente("Arial");
            configuracionDefault.setTamanoFuente(14);
            configuracionDefault.setEsCursiva(false);
            configuracionDefault.setColorTexto("#000000");
            configuracionDefault.setActivo(true);
            
            return ResponseEntity.ok(configuracionDefault);
        }
    }
    
    /**
     * Obtiene todas las configuraciones de formato
     * 
     * @return ResponseEntity con la lista de configuraciones
     */
    @GetMapping
    public ResponseEntity<List<FormatoCorreoDto>> obtenerTodasConfiguraciones() {
        try {
            List<FormatoCorreoDto> configuraciones = formatoCorreoService.obtenerTodasConfiguraciones();
            return ResponseEntity.ok(configuraciones);
        } catch (Exception e) {
            logger.error("Error al obtener todas las configuraciones: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Guarda una nueva configuración de formato
     * 
     * @param configuracion Configuración a guardar
     * @return ResponseEntity con la configuración guardada
     */
    @PostMapping
    public ResponseEntity<FormatoCorreoDto> guardarConfiguracion(
            @RequestBody FormatoCorreoDto configuracion) {
        try {
            FormatoCorreoDto configuracionGuardada = formatoCorreoService.guardarConfiguracion(configuracion);
            return ResponseEntity.ok(configuracionGuardada);
        } catch (Exception e) {
            logger.error("Error al guardar configuración de formato: {}", e.getMessage(), e);
            // Fallback: devolver la configuración recibida con valores por defecto si la BD falla
            FormatoCorreoDto fallback = configuracion;
            if (fallback.getTipoFuente() == null || fallback.getTipoFuente().trim().isEmpty()) fallback.setTipoFuente("Arial");
            if (fallback.getTamanoFuente() == null || fallback.getTamanoFuente() <= 0) fallback.setTamanoFuente(14);
            if (fallback.getColorTexto() == null || fallback.getColorTexto().trim().isEmpty()) fallback.setColorTexto("#000000");
            if (fallback.getEsCursiva() == null) fallback.setEsCursiva(false);
            if (fallback.getEsSubrayado() == null) fallback.setEsSubrayado(false);
            if (fallback.getActivo() == null) fallback.setActivo(true);
            return ResponseEntity.ok(fallback);
        }
    }
    
    /**
     * Actualiza una configuración de formato existente
     * 
     * @param id ID de la configuración
     * @param configuracion Configuración actualizada
     * @return ResponseEntity con la configuración actualizada
     */
    @PutMapping("/{id}")
    public ResponseEntity<FormatoCorreoDto> actualizarConfiguracion(
            @PathVariable Long id,
            @RequestBody FormatoCorreoDto configuracion) {
        try {
            configuracion.setId(id);
            FormatoCorreoDto configuracionActualizada = formatoCorreoService.actualizarConfiguracion(configuracion);
            return ResponseEntity.ok(configuracionActualizada);
        } catch (Exception e) {
            logger.error("Error al actualizar configuración de formato: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Elimina una configuración de formato
     * 
     * @param id ID de la configuración a eliminar
     * @return ResponseEntity con el resultado
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminarConfiguracion(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            formatoCorreoService.eliminarConfiguracion(id);
            response.put("success", true);
            response.put("message", "Configuración eliminada exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al eliminar configuración de formato: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error al eliminar la configuración: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Genera estilos CSS para aplicar al correo
     * 
     * @param id ID de la configuración
     * @return ResponseEntity con los estilos CSS
     */
    @GetMapping("/{id}/estilos")
    public ResponseEntity<Map<String, String>> generarEstilos(@PathVariable Long id) {
        try {
            String estilos = formatoCorreoService.generarEstilosCSS(id);
            Map<String, String> response = new HashMap<>();
            response.put("estilos", estilos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al generar estilos CSS: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}