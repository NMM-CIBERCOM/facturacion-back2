package com.cibercom.facturacion_back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para los perfiles de usuario
 */
public class PerfilDto {

    @JsonProperty("idPerfil")
    private Integer idPerfil;

    @JsonProperty("nombrePerfil")
    private String nombrePerfil;

    @JsonProperty("descripcion")
    private String descripcion;

    // Constructores
    public PerfilDto() {}

    public PerfilDto(Integer idPerfil, String nombrePerfil, String descripcion) {
        this.idPerfil = idPerfil;
        this.nombrePerfil = nombrePerfil;
        this.descripcion = descripcion;
    }

    // Getters y Setters
    public Integer getIdPerfil() {
        return idPerfil;
    }

    public void setIdPerfil(Integer idPerfil) {
        this.idPerfil = idPerfil;
    }

    public String getNombrePerfil() {
        return nombrePerfil;
    }

    public void setNombrePerfil(String nombrePerfil) {
        this.nombrePerfil = nombrePerfil;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return "PerfilDto{" +
                "idPerfil=" + idPerfil +
                ", nombrePerfil='" + nombrePerfil + '\'' +
                ", descripcion='" + descripcion + '\'' +
                '}';
    }
}