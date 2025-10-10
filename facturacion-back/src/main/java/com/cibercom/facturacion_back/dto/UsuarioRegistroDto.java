package com.cibercom.facturacion_back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para el registro de usuarios
 */
public class UsuarioRegistroDto {

    @JsonProperty("noUsuario")
    private String noUsuario;

    @JsonProperty("nombreEmpleado")
    private String nombreEmpleado;

    @JsonProperty("password")
    private String password;

    @JsonProperty("estatusUsuario")
    private String estatusUsuario;

    @JsonProperty("idPerfil")
    private Integer idPerfil;

    @JsonProperty("usuarioMod")
    private String usuarioMod;

    @JsonProperty("idDfi")
    private String idDfi;

    @JsonProperty("idEstacionamiento")
    private String idEstacionamiento;

    @JsonProperty("modificaUbicacion")
    private String modificaUbicacion;

    // Constructores
    public UsuarioRegistroDto() {}

    public UsuarioRegistroDto(String noUsuario, String nombreEmpleado, String password, 
                             String estatusUsuario, Integer idPerfil, String usuarioMod, 
                             String idDfi, String idEstacionamiento, String modificaUbicacion) {
        this.noUsuario = noUsuario;
        this.nombreEmpleado = nombreEmpleado;
        this.password = password;
        this.estatusUsuario = estatusUsuario;
        this.idPerfil = idPerfil;
        this.usuarioMod = usuarioMod;
        this.idDfi = idDfi;
        this.idEstacionamiento = idEstacionamiento;
        this.modificaUbicacion = modificaUbicacion;
    }

    // Getters y Setters
    public String getNoUsuario() {
        return noUsuario;
    }

    public void setNoUsuario(String noUsuario) {
        this.noUsuario = noUsuario;
    }

    public String getNombreEmpleado() {
        return nombreEmpleado;
    }

    public void setNombreEmpleado(String nombreEmpleado) {
        this.nombreEmpleado = nombreEmpleado;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEstatusUsuario() {
        return estatusUsuario;
    }

    public void setEstatusUsuario(String estatusUsuario) {
        this.estatusUsuario = estatusUsuario;
    }

    public Integer getIdPerfil() {
        return idPerfil;
    }

    public void setIdPerfil(Integer idPerfil) {
        this.idPerfil = idPerfil;
    }

    public String getUsuarioMod() {
        return usuarioMod;
    }

    public void setUsuarioMod(String usuarioMod) {
        this.usuarioMod = usuarioMod;
    }

    public String getIdDfi() {
        return idDfi;
    }

    public void setIdDfi(String idDfi) {
        this.idDfi = idDfi;
    }

    public String getIdEstacionamiento() {
        return idEstacionamiento;
    }

    public void setIdEstacionamiento(String idEstacionamiento) {
        this.idEstacionamiento = idEstacionamiento;
    }

    public String getModificaUbicacion() {
        return modificaUbicacion;
    }

    public void setModificaUbicacion(String modificaUbicacion) {
        this.modificaUbicacion = modificaUbicacion;
    }

    @Override
    public String toString() {
        return "UsuarioRegistroDto{" +
                "noUsuario='" + noUsuario + '\'' +
                ", nombreEmpleado='" + nombreEmpleado + '\'' +
                ", estatusUsuario='" + estatusUsuario + '\'' +
                ", idPerfil=" + idPerfil +
                ", usuarioMod='" + usuarioMod + '\'' +
                ", idDfi='" + idDfi + '\'' +
                ", idEstacionamiento='" + idEstacionamiento + '\'' +
                ", modificaUbicacion='" + modificaUbicacion + '\'' +
                '}';
    }
}