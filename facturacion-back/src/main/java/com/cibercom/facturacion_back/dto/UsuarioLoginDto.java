package com.cibercom.facturacion_back.dto;

/**
 * DTO para los datos del usuario en la respuesta de login
 */
public class UsuarioLoginDto {
    
    private String noUsuario;
    private String nombreEmpleado;
    private String nombrePerfil;
    private Integer idPerfil;
    private String estatusUsuario;
    private Integer idDfi;
    private Integer idEstacionamiento;
    private String modificaUbicacion;
    
    // Constructores
    public UsuarioLoginDto() {}
    
    public UsuarioLoginDto(String noUsuario, String nombreEmpleado, String nombrePerfil, 
                          Integer idPerfil, String estatusUsuario, Integer idDfi, 
                          Integer idEstacionamiento, String modificaUbicacion) {
        this.noUsuario = noUsuario;
        this.nombreEmpleado = nombreEmpleado;
        this.nombrePerfil = nombrePerfil;
        this.idPerfil = idPerfil;
        this.estatusUsuario = estatusUsuario;
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
    
    public String getNombrePerfil() {
        return nombrePerfil;
    }
    
    public void setNombrePerfil(String nombrePerfil) {
        this.nombrePerfil = nombrePerfil;
    }
    
    public Integer getIdPerfil() {
        return idPerfil;
    }
    
    public void setIdPerfil(Integer idPerfil) {
        this.idPerfil = idPerfil;
    }
    
    public String getEstatusUsuario() {
        return estatusUsuario;
    }
    
    public void setEstatusUsuario(String estatusUsuario) {
        this.estatusUsuario = estatusUsuario;
    }
    
    public Integer getIdDfi() {
        return idDfi;
    }
    
    public void setIdDfi(Integer idDfi) {
        this.idDfi = idDfi;
    }
    
    public Integer getIdEstacionamiento() {
        return idEstacionamiento;
    }
    
    public void setIdEstacionamiento(Integer idEstacionamiento) {
        this.idEstacionamiento = idEstacionamiento;
    }
    
    public String getModificaUbicacion() {
        return modificaUbicacion;
    }
    
    public void setModificaUbicacion(String modificaUbicacion) {
        this.modificaUbicacion = modificaUbicacion;
    }
}