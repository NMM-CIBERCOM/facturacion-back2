package com.cibercom.facturacion_back.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class PasswordUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * Codifica una contraseña en Base64
     */
    public String encodeBase64(String password) {
        try {
            return Base64.getEncoder().encodeToString(password.getBytes());
        } catch (Exception e) {
            logger.error("Error codificando contraseña en Base64: {}", e.getMessage());
            throw new RuntimeException("Error al codificar contraseña", e);
        }
    }
    
    /**
     * Decodifica una contraseña desde Base64
     */
    public String decodeBase64(String encodedPassword) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedPassword);
            return new String(decodedBytes);
        } catch (Exception e) {
            logger.error("Error decodificando contraseña desde Base64: {}", e.getMessage());
            throw new RuntimeException("Error al decodificar contraseña", e);
        }
    }
    
    /**
     * Codifica una contraseña usando BCrypt (más seguro que Base64)
     */
    public String encodeBCrypt(String password) {
        try {
            return passwordEncoder.encode(password);
        } catch (Exception e) {
            logger.error("Error codificando contraseña con BCrypt: {}", e.getMessage());
            throw new RuntimeException("Error al codificar contraseña", e);
        }
    }
    
    /**
     * Verifica si una contraseña coincide con su hash BCrypt
     */
    public boolean matchesBCrypt(String rawPassword, String encodedPassword) {
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (Exception e) {
            logger.error("Error verificando contraseña con BCrypt: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica si una contraseña coincide con su versión Base64
     */
    public boolean matchesBase64(String rawPassword, String encodedPassword) {
        try {
            String decodedPassword = decodeBase64(encodedPassword);
            return rawPassword.equals(decodedPassword);
        } catch (Exception e) {
            logger.error("Error verificando contraseña con Base64: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Determina si una contraseña está codificada en Base64
     */
    public boolean isBase64Encoded(String password) {
        try {
            Base64.getDecoder().decode(password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Determina si una contraseña está codificada con BCrypt
     */
    public boolean isBCryptEncoded(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
