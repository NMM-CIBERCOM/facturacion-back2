package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.service.UsuarioService;
import com.cibercom.facturacion_back.dto.UsuarioRegistroDto;
import com.cibercom.facturacion_back.dto.PerfilDto;
import com.cibercom.facturacion_back.dto.EmpleadoConsultaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador REST para la gestión de usuarios
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Registra un nuevo usuario
     */
    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registrarUsuario(@RequestBody UsuarioRegistroDto usuario) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Registrando nuevo usuario: {}", usuario.getNoUsuario());
            
            Map<String, Object> resultado = usuarioService.registrarUsuario(usuario);
            
            if ((Boolean) resultado.get("success")) {
                response.put("success", true);
                response.put("message", "Usuario registrado exitosamente");
                response.put("usuario", resultado.get("usuario"));
            } else {
                response.put("success", false);
                response.put("message", resultado.get("message"));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al registrar usuario: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Obtiene todos los perfiles disponibles
     */
    @GetMapping("/perfiles")
    public ResponseEntity<List<PerfilDto>> obtenerPerfiles() {
        try {
            logger.info("Obteniendo lista de perfiles");
            
            List<PerfilDto> perfiles = usuarioService.obtenerPerfiles();
            return ResponseEntity.ok(perfiles);
            
        } catch (Exception e) {
            logger.error("Error al obtener perfiles: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    /**
     * Consulta todos los empleados
     */
    @GetMapping("/empleados")
    public ResponseEntity<List<EmpleadoConsultaDTO>> consultarEmpleados(
            @RequestParam(required = false) String noUsuario,
            @RequestParam(required = false) String nombreEmpleado,
            @RequestParam(required = false) String idPerfil) {
        try {
            List<EmpleadoConsultaDTO> empleados;
            
            if ((noUsuario != null && !noUsuario.trim().isEmpty()) ||
                (nombreEmpleado != null && !nombreEmpleado.trim().isEmpty()) ||
                (idPerfil != null && !idPerfil.trim().isEmpty())) {
                logger.info("Consultando empleados específicos con filtros - noUsuario: {}, nombreEmpleado: {}, idPerfil: {}", 
                           noUsuario, nombreEmpleado, idPerfil);
                empleados = usuarioService.consultarEmpleadosEspecificos(noUsuario, nombreEmpleado, idPerfil);
            } else {
                logger.info("Consultando todos los empleados");
                empleados = usuarioService.consultarEmpleados();
            }
            
            return ResponseEntity.ok(empleados);
            
        } catch (Exception e) {
            logger.error("Error al consultar empleados: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    /**
     * Actualiza el perfil de un usuario
     */
    @PutMapping("/actualizar-perfil")
    public ResponseEntity<Map<String, Object>> actualizarPerfil(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String noUsuario = (String) request.get("noUsuario");
            Integer idPerfil = (Integer) request.get("idPerfil");
            String usuarioMod = (String) request.get("usuarioMod");
            
            logger.info("Actualizando perfil del usuario: {} al perfil: {}", noUsuario, idPerfil);
            
            Map<String, Object> resultado = usuarioService.actualizarPerfil(noUsuario, idPerfil, usuarioMod);
            
            response.put("success", resultado.get("success"));
            response.put("message", resultado.get("message"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al actualizar perfil: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Elimina (desactiva) un usuario
     */
    @DeleteMapping("/eliminar")
    public ResponseEntity<Map<String, Object>> eliminarUsuario(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String noUsuario = (String) request.get("noUsuario");
            String usuarioMod = (String) request.get("usuarioMod");
            
            logger.info("Eliminando usuario: {}", noUsuario);
            
            Map<String, Object> resultado = usuarioService.eliminarUsuario(noUsuario, usuarioMod);
            
            response.put("success", resultado.get("success"));
            response.put("message", resultado.get("message"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al eliminar usuario: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Health check del controlador
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("Health check solicitado para UsuarioController");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "UsuarioController funcionando correctamente");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}