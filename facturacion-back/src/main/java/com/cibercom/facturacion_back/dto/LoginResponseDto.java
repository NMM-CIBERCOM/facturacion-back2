package com.cibercom.facturacion_back.dto;

/**
 * DTO para la respuesta de login
 */
public class LoginResponseDto {
    
    private boolean success;
    private String message;
    private String token;
    private UsuarioLoginDto usuario;
    
    // Constructores
    public LoginResponseDto() {}
    
    public LoginResponseDto(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public LoginResponseDto(boolean success, String message, String token, UsuarioLoginDto usuario) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.usuario = usuario;
    }
    
    // Getters y Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public UsuarioLoginDto getUsuario() {
        return usuario;
    }
    
    public void setUsuario(UsuarioLoginDto usuario) {
        this.usuario = usuario;
    }
}