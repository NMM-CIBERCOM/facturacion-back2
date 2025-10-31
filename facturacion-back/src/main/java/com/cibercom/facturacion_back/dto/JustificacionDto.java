package com.cibercom.facturacion_back.dto;

import lombok.Data;

@Data
public class JustificacionDto {
    private Long id;
    private String descripcion;

    // Getters y Setters explícitos para robustez cuando Lombok no está activo
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}