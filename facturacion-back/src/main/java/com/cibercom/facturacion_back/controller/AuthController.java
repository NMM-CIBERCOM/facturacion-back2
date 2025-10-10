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

import jakarta.validation.Valid;
import java.util.Map;

/**
 * Controlador REST para la autenticación de usuarios
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Endpoint para el login de usuarios
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        try {
            logger.info("Intento de login para usuario: {}", loginRequest.getUsuario());
            
            Map<String, Object> resultado = usuarioService.autenticarUsuario(
                loginRequest.getUsuario(), 
                loginRequest.getPassword()
            );
            
            boolean success = (Boolean) resultado.get("success");
            String message = (String) resultado.get("message");
            
            if (success) {
                UsuarioLoginDto usuario = (UsuarioLoginDto) resultado.get("usuario");
                String token = (String) resultado.get("token"); // Puede ser null si no usas JWT
                
                LoginResponseDto response = new LoginResponseDto(success, message, token, usuario);
                logger.info("Login exitoso para usuario: {}", loginRequest.getUsuario());
                return ResponseEntity.ok(response);
                
            } else {
                LoginResponseDto response = new LoginResponseDto(success, message);
                logger.warn("Login fallido para usuario: {} - {}", loginRequest.getUsuario(), message);
                return ResponseEntity.status(401).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error durante el login: {}", e.getMessage(), e);
            LoginResponseDto response = new LoginResponseDto(false, "Error interno del servidor");
            return ResponseEntity.status(500).body(response);
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