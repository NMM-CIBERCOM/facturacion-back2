package com.cibercom.facturacion_back.dto;

import java.sql.Timestamp;
import java.math.BigDecimal;

/**
 * DTO para consulta de empleados
 */
public class EmpleadoConsultaDTO {
    
    private Integer id;
    private String contrasena;
    private String correo;
    private String rfc;
    private String curp;
    private String nombre;
    private String noUsuario;
    private String nombreEmpleado;
    private String nombrePerfil;
    private String estatusUsuario;
    private Timestamp fechaAlta;
    private Timestamp fechaMod;
    private Integer idDfi;
    private Integer idEstacionamiento;
    private Integer idPerfil;
    private String modificaUbicacion;
    private String password;
    private String usuarioMod;
    private BigDecimal salarioBase;

    // Constructor por defecto
    public EmpleadoConsultaDTO() {}

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getRfc() {
        return rfc;
    }

    public void setRfc(String rfc) {
        this.rfc = rfc;
    }

    public String getCurp() {
        return curp;
    }

    public void setCurp(String curp) {
        this.curp = curp;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

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

    public String getEstatusUsuario() {
        return estatusUsuario;
    }

    public void setEstatusUsuario(String estatusUsuario) {
        this.estatusUsuario = estatusUsuario;
    }

    public Timestamp getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(Timestamp fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public Timestamp getFechaMod() {
        return fechaMod;
    }

    public void setFechaMod(Timestamp fechaMod) {
        this.fechaMod = fechaMod;
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

    public Integer getIdPerfil() {
        return idPerfil;
    }

    public void setIdPerfil(Integer idPerfil) {
        this.idPerfil = idPerfil;
    }

    public String getModificaUbicacion() {
        return modificaUbicacion;
    }

    public void setModificaUbicacion(String modificaUbicacion) {
        this.modificaUbicacion = modificaUbicacion;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsuarioMod() {
        return usuarioMod;
    }

    public void setUsuarioMod(String usuarioMod) {
        this.usuarioMod = usuarioMod;
    }

    public BigDecimal getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(BigDecimal salarioBase) {
        this.salarioBase = salarioBase;
    }

    @Override
    public String toString() {
        return "EmpleadoConsultaDTO{" +
                "id=" + id +
                ", contrasena='" + contrasena + '\'' +
                ", correo='" + correo + '\'' +
                ", rfc='" + rfc + '\'' +
                ", curp='" + curp + '\'' +
                ", nombre='" + nombre + '\'' +
                ", noUsuario='" + noUsuario + '\'' +
                ", nombreEmpleado='" + nombreEmpleado + '\'' +
                ", nombrePerfil='" + nombrePerfil + '\'' +
                ", estatusUsuario='" + estatusUsuario + '\'' +
                ", fechaAlta=" + fechaAlta +
                ", fechaMod=" + fechaMod +
                ", idDfi=" + idDfi +
                ", idEstacionamiento=" + idEstacionamiento +
                ", idPerfil=" + idPerfil +
                ", modificaUbicacion='" + modificaUbicacion + '\'' +
                ", password='" + password + '\'' +
                ", usuarioMod='" + usuarioMod + '\'' +
                ", salarioBase=" + salarioBase +
                '}';
    }
}