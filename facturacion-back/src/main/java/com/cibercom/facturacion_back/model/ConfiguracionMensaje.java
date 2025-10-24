package com.cibercom.facturacion_back.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Date;

@Entity
@Table(name = "CONFIGURACION_MENSAJES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionMensaje {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "config_msg_seq")
    @SequenceGenerator(name = "config_msg_seq", sequenceName = "SEQ_CONFIGURACION_MENSAJES", allocationSize = 1)
    @Column(name = "ID_CONFIGURACION")
    private Long idConfiguracion;
    
    @Column(name = "MENSAJE_SELECCIONADO", nullable = false, length = 20)
    private String mensajeSeleccionado;
    
    @Column(name = "TIPO_MENSAJE", nullable = false, length = 50)
    private String tipoMensaje;
    
    @Column(name = "ASUNTO_PERSONALIZADO", length = 200)
    private String asuntoPersonalizado;
    
    @Column(name = "MENSAJE_PERSONALIZADO", columnDefinition = "CLOB")
    private String mensajePersonalizado;
    
    // Campos de formato de correo - nombres corregidos según la tabla Oracle
    @Column(name = "TIPO_FUENTE", length = 50)
    private String tipoFuente;
    
    @Column(name = "TAMANO_FUENTE")
    private Integer tamanoFuente;
    
    @Column(name = "ES_CURSIVA", length = 1)
    private String esCursiva;
    
    @Column(name = "ES_SUBRAYADO", length = 1)
    private String esSubrayado;

    @Column(name = "COLOR_TEXTO", length = 10)
    private String colorTexto;
    
    @Column(name = "ACTIVO", nullable = false, length = 1)
    private String activo;
    
    @Column(name = "FECHA_CREACION", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;
    
    @Column(name = "FECHA_MODIFICACION")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaModificacion;
    
    @Column(name = "USUARIO_CREACION", length = 50)
    private String usuarioCreacion;
    
    @Column(name = "USUARIO_MODIFICACION", length = 50)
    private String usuarioModificacion;
    
    @PrePersist
    protected void onCreate() {
        fechaCreacion = new Date();
        if (activo == null) {
            activo = "S";
        }
        // Valores por defecto para campos de formato
        if (tipoFuente == null) {
            tipoFuente = "Arial";
        }
        if (tamanoFuente == null) {
            tamanoFuente = 14;
        }
        if (esCursiva == null) {
            esCursiva = "N";
        }
        if (esSubrayado == null) {
            esSubrayado = "N";
        }
        if (colorTexto == null) {
            colorTexto = "#000000";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = new Date();
    }

    // Getters explícitos para compatibilidad si falla Lombok
    public Long getIdConfiguracion() { return idConfiguracion; }
    public String getTipoFuente() { return tipoFuente; }
    public Integer getTamanoFuente() { return tamanoFuente; }
    public String getEsCursiva() { return esCursiva; }
    public String getEsSubrayado() { return esSubrayado; }
    public String getColorTexto() { return colorTexto; }
    public String getMensajeSeleccionado() { return mensajeSeleccionado; }
    public String getTipoMensaje() { return tipoMensaje; }
    public String getAsuntoPersonalizado() { return asuntoPersonalizado; }
    public String getMensajePersonalizado() { return mensajePersonalizado; }
    public String getActivo() { return activo; }
    public String getUsuarioCreacion() { return usuarioCreacion; }

    // Setters explícitos usados por servicios
    public void setMensajeSeleccionado(String mensajeSeleccionado) { this.mensajeSeleccionado = mensajeSeleccionado; }
    public void setTipoMensaje(String tipoMensaje) { this.tipoMensaje = tipoMensaje; }
    public void setActivo(String activo) { this.activo = activo; }
    public void setUsuarioCreacion(String usuarioCreacion) { this.usuarioCreacion = usuarioCreacion; }
    public void setAsuntoPersonalizado(String asuntoPersonalizado) { this.asuntoPersonalizado = asuntoPersonalizado; }
    public void setMensajePersonalizado(String mensajePersonalizado) { this.mensajePersonalizado = mensajePersonalizado; }

    public void setTipoFuente(String tipoFuente) { this.tipoFuente = tipoFuente; }
    public void setTamanoFuente(Integer tamanoFuente) { this.tamanoFuente = tamanoFuente; }
    public void setEsCursiva(String esCursiva) { this.esCursiva = esCursiva; }
    public void setEsSubrayado(String esSubrayado) { this.esSubrayado = esSubrayado; }
    public void setColorTexto(String colorTexto) { this.colorTexto = colorTexto; }
}