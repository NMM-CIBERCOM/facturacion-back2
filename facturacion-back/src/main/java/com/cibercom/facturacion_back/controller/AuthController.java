package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.LoginRequestDto;
import com.cibercom.facturacion_back.dto.LoginResponseDto;
import com.cibercom.facturacion_back.dto.UsuarioLoginDto;
import com.cibercom.facturacion_back.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * Controlador REST para la autenticación de usuarios
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174", "https://redcibercom.cloud", "http://redcibercom.cloud"}, allowCredentials = "true")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Endpoint para el login de usuarios
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest, HttpServletRequest request) {
        try {
            logger.info("Intento de login para usuario: {}", loginRequest.getUsuario());
            
            Map<String, Object> resultado = usuarioService.autenticarUsuario(
                loginRequest.getUsuario(), 
                loginRequest.getPassword()
            );
            
            if (resultado == null) {
                logger.warn("Login: servicio devolvió null");
                return ResponseEntity.status(500).body(new LoginResponseDto(false, "Error interno del servidor"));
            }
            
            Object successObj = resultado.get("success");
            boolean success = Boolean.TRUE.equals(successObj);
            String message = resultado.get("message") != null ? resultado.get("message").toString() : "Error de autenticación";
            
            if (success) {
                Object usuarioObj = resultado.get("usuario");
                if (!(usuarioObj instanceof UsuarioLoginDto)) {
                    logger.error("Login: usuario no es UsuarioLoginDto (tipo: {})", usuarioObj != null ? usuarioObj.getClass().getName() : "null");
                    return ResponseEntity.status(500).body(new LoginResponseDto(false, "Error interno del servidor"));
                }
                UsuarioLoginDto usuario = (UsuarioLoginDto) usuarioObj;
                String token = resultado.get("token") != null ? resultado.get("token").toString() : null;
                
                LoginResponseDto response = new LoginResponseDto(true, message, token, usuario);
                logger.info("Login exitoso para usuario: {}", loginRequest.getUsuario());
                return ResponseEntity.ok(response);
                
            } else {
                LoginResponseDto response = new LoginResponseDto(false, message);
                logger.warn("Login fallido para usuario: {} - {}", loginRequest.getUsuario(), message);
                return ResponseEntity.status(401).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error durante el login [{}]: {} - Revisar logs de Tomcat para detalles (p. ej. conexión Oracle)", 
                e.getClass().getSimpleName(), e.getMessage(), e);
            // Si el cliente envía X-Debug-Login: true, devolver el mensaje real para diagnóstico (sin stack trace)
            boolean debug = "true".equalsIgnoreCase(request.getHeader("X-Debug-Login"));
            String msg = debug && e.getMessage() != null
                ? ("Error interno: " + e.getMessage())
                : "Error interno del servidor";
            return ResponseEntity.status(500).body(new LoginResponseDto(false, msg));
        }
    }

    /**
     * Health check del controlador de autenticación
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("Health check solicitado para AuthController");
        
        Map<String, Object> response = Map.of(
            "success", true,
            "message", "AuthController funcionando correctamente",
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(response);
    }
}