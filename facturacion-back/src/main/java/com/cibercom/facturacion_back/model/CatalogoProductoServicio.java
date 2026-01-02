package com.cibercom.facturacion_back.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CATALOGOS_PRODUCTOS_SERVICIOS", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"ID_USUARIO", "CLAVE_PROD_SERV"}))
public class CatalogoProductoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ID_USUARIO", nullable = false)
    private Long idUsuario;

    @Column(name = "CLAVE_PROD_SERV", nullable = false, length = 20)
    private String claveProdServ;

    @Column(name = "CANTIDAD", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad;

    @Column(name = "UNIDAD", nullable = false, length = 10)
    private String unidad;

    @Column(name = "CLAVE_UNIDAD", length = 10)
    private String claveUnidad;

    @Column(name = "DESCRIPCION", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "OBJETO_IMPUESTO", nullable = false, length = 2)
    private String objetoImpuesto;

    @Column(name = "VALOR_UNITARIO", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "IMPORTE", nullable = false, precision = 18, scale = 2)
    private BigDecimal importe;

    @Column(name = "TASA_IVA", precision = 5, scale = 2)
    private BigDecimal tasaIva;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "FECHA_ACTUALIZACION", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "ACTIVO", nullable = false, length = 1)
    private String activo;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (activo == null) {
            activo = "1";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getClaveProdServ() {
        return claveProdServ;
    }

    public void setClaveProdServ(String claveProdServ) {
        this.claveProdServ = claveProdServ;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getClaveUnidad() {
        return claveUnidad;
    }

    public void setClaveUnidad(String claveUnidad) {
        this.claveUnidad = claveUnidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getObjetoImpuesto() {
        return objetoImpuesto;
    }

    public void setObjetoImpuesto(String objetoImpuesto) {
        this.objetoImpuesto = objetoImpuesto;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public BigDecimal getTasaIva() {
        return tasaIva;
    }

    public void setTasaIva(BigDecimal tasaIva) {
        this.tasaIva = tasaIva;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getActivo() {
        return activo;
    }

    public void setActivo(String activo) {
        this.activo = activo;
    }

    public boolean isActivo() {
        return "1".equals(activo);
    }

    public void setActivo(boolean activo) {
        this.activo = activo ? "1" : "0";
    }
}

