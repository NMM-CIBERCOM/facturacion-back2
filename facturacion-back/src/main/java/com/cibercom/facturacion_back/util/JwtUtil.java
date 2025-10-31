package com.cibercom.facturacion_back.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${jwt.secret:cibercom-facturacion-secret-key-2024}")
    private String secret;
    
    @Value("${jwt.expiration:900000}") // 15 minutos en milisegundos
    private long expiration;
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Genera un token JWT para un usuario
     */
    public String generateToken(String username, String role, Integer idPerfil) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);
        claims.put("idPerfil", idPerfil);
        claims.put("issuedAt", new Date());
        
        return createToken(claims, username);
    }
    
    /**
     * Crea el token JWT con los claims especificados
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Extrae el nombre de usuario del token
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    /**
     * Extrae la fecha de expiración del token
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    /**
     * Extrae un claim específico del token
     */
    public <T> T getClaimFromToken(String token, ClaimsResolver<T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.resolve(claims);
    }
    
    /**
     * Extrae todos los claims del token
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Verifica si el token ha expirado
     */
    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            logger.warn("Error verificando expiración del token: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * Valida el token JWT
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = getUsernameFromToken(token);
            return (username.equals(tokenUsername) && !isTokenExpired(token));
        } catch (Exception e) {
            logger.warn("Error validando token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extrae el rol del token
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return (String) claims.get("role");
        } catch (Exception e) {
            logger.warn("Error extrayendo rol del token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrae el ID del perfil del token
     */
    public Integer getIdPerfilFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return (Integer) claims.get("idPerfil");
        } catch (Exception e) {
            logger.warn("Error extrayendo ID perfil del token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Interfaz funcional para resolver claims
     */
    @FunctionalInterface
    public interface ClaimsResolver<T> {
        T resolve(Claims claims);
    }
}
