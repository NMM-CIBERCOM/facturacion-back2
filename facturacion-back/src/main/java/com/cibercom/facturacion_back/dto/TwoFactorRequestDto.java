package com.cibercom.facturacion_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para la solicitud de autenticación en dos pasos
 */
public class TwoFactorRequestDto {
    
    @NotBlank(message = "El código de autenticación es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El código debe tener 6 dígitos")
    private String code;
    
    @NotBlank(message = "El token de sesión es obligatorio")
    private String sessionToken;
    
    // Constructores
    public TwoFactorRequestDto() {}
    
    public TwoFactorRequestDto(String code, String sessionToken) {
        this.code = code;
        this.sessionToken = sessionToken;
    }
    
    // Getters y Setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getSessionToken() {
        return sessionToken;
    }
    
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    
    @Override
    public String toString() {
        return "TwoFactorRequestDto{" +
                "code='[PROTECTED]'" +
                ", sessionToken='[PROTECTED]'" +
                '}';
    }
}
