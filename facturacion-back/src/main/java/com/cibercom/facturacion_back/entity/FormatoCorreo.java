package com.cibercom.facturacion_back.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.cibercom.facturacion_back.converter.BooleanToStringConverter;

/**
 * Entidad para la configuración de formato de correo electrónico
 */
@Entity
@Table(name = "CONFIGURACION_FORMATO_CORREO")
public class FormatoCorreo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(name = "TIPO_FUENTE", nullable = false, length = 50)
    private String tipoFuente;
    
    @Column(name = "TAMANO_FUENTE", nullable = false)
    private Integer tamanoFuente;
    
    @Column(name = "ES_CURSIVA", nullable = false, length = 1)
    @Convert(converter = BooleanToStringConverter.class)
    private Boolean esCursiva;

    @Column(name = "ES_NEGRITA", nullable = false, length = 1)
    @Convert(converter = BooleanToStringConverter.class)
    private Boolean esNegrita;

    @Column(name = "ES_SUBRAYADO", nullable = false, length = 1)
    @Convert(converter = BooleanToStringConverter.class)
    private Boolean esSubrayado;
    
    @Column(name = "COLOR_TEXTO", nullable = false, length = 7)
    private String colorTexto;
    
    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;
    
    @Transient
    private LocalDateTime fechaModificacion;
    
    @Column(name = "ACTIVO", nullable = false, length = 1)
    @Convert(converter = BooleanToStringConverter.class)
    private Boolean activo;
    
    // Constructor por defecto
    public FormatoCorreo() {
        this.fechaCreacion = LocalDateTime.now();
        this.activo = true;
        this.esSubrayado = false;
        this.esNegrita = false;
    }
    
    // Constructor con parámetros
    public FormatoCorreo(String tipoFuente, Integer tamanoFuente, Boolean esCursiva, String colorTexto) {
        this();
        this.tipoFuente = tipoFuente;
        this.tamanoFuente = tamanoFuente;
        this.esCursiva = esCursiva;
        this.colorTexto = colorTexto;
    }
    
    // Método que se ejecuta antes de actualizar
    @PreUpdate
    public void preUpdate() {
        this.fechaModificacion = LocalDateTime.now();
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

    public Boolean getEsSubrayado() {
        return esSubrayado;
    }

    public void setEsSubrayado(Boolean esSubrayado) {
        this.esSubrayado = esSubrayado;
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
    
    @Override
    public String toString() {
        return "FormatoCorreo{" +
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