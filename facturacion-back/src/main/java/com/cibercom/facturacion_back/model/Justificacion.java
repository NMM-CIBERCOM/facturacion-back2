package com.cibercom.facturacion_back.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "JUSTIFICACION")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Justificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "DESCRIPCION", length = 255, nullable = false)
    private String descripcion;

    // Getters y Setters explícitos para robustez cuando Lombok no está activo
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}