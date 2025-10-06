package com.cibercom.facturacion_back.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * DTO para la configuración de formato de correo electrónico
 */
public class FormatoCorreoDto {
    
    private Long id;
    private String tipoFuente;
    @JsonAlias({"tamanoFuente","tamañoFuente"})
    private Integer tamanoFuente;
    private Boolean esCursiva;
    private Boolean esNegrita;
    private String colorTexto;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private Boolean activo;
    private Boolean esSubrayado;
    
    // Constructor por defecto
    public FormatoCorreoDto() {
        this.tipoFuente = "Arial";
        this.tamanoFuente = 14;
        this.esCursiva = false;
        this.esNegrita = false;
        this.colorTexto = "#000000";
        this.activo = true;
        this.esSubrayado = false;
    }
    
    // Constructor con parámetros
    public FormatoCorreoDto(String tipoFuente, Integer tamanoFuente, Boolean esCursiva, String colorTexto) {
        this.tipoFuente = tipoFuente;
        this.tamanoFuente = tamanoFuente;
        this.esCursiva = esCursiva;
        this.colorTexto = colorTexto;
        this.activo = true;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTipoFuente() {
        return tipoFuente;
    }
    
    public void setTipoFuente(String tipoFuente) {
        this.tipoFuente = tipoFuente;
    }
    
    public Integer getTamanoFuente() {
        return tamanoFuente;
    }
    
    public void setTamanoFuente(Integer tamanoFuente) {
        this.tamanoFuente = tamanoFuente;
    }
    
    public Boolean getEsCursiva() {
        return esCursiva;
    }
    
    public void setEsCursiva(Boolean esCursiva) {
        this.esCursiva = esCursiva;
    }
    
    public Boolean getEsNegrita() {
        return esNegrita;
    }
    
    public void setEsNegrita(Boolean esNegrita) {
        this.esNegrita = esNegrita;
    }
    
    public String getColorTexto() {
        return colorTexto;
    }
    
    public void setColorTexto(String colorTexto) {
        this.colorTexto = colorTexto;
    }
    
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }
    
    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
    
    public Boolean getActivo() {
        return activo;
    }
    
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getEsSubrayado() {
        return esSubrayado;
    }

    public void setEsSubrayado(Boolean esSubrayado) {
        this.esSubrayado = esSubrayado;
    }
    
    @Override
    public String toString() {
        return "FormatoCorreoDto{" +
                "id=" + id +
                ", tipoFuente='" + tipoFuente + '\'' +
                ", tamanoFuente=" + tamanoFuente +
                ", esCursiva=" + esCursiva +
                ", esNegrita=" + esNegrita +
                ", esSubrayado=" + esSubrayado +
                ", colorTexto='" + colorTexto + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                ", fechaModificacion=" + fechaModificacion +
                ", activo=" + activo +
                '}';
    }
}