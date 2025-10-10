package com.cibercom.facturacion_back.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para la petición de login
 */
public class LoginRequestDto {
    
    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String usuario;
    
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
    
    // Constructores
    public LoginRequestDto() {}
    
    public LoginRequestDto(String usuario, String password) {
        this.usuario = usuario;
        this.password = password;
    }
    
    // Getters y Setters
    public String getUsuario() {
        return usuario;
    }
    
    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    @Override
    public String toString() {
        return "LoginRequestDto{" +
                "usuario='" + usuario + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}