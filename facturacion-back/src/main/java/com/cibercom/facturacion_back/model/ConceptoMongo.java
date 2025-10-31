package com.cibercom.facturacion_back.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "conceptos")
public class ConceptoMongo {

    @Id
    private String id;

    @Field("uuidFactura")
    private String uuidFactura; // opcional, para vincular si existe

    @Field("idFactura")
    private Long idFactura; // opcional, para vincular si existe

    @Field("skuClaveSat")
    private String skuClaveSat;

    @Field("descripcion")
    private String descripcion;

    @Field("unidadMedida")
    private String unidadMedida;

    @Field("valorUnitario")
    private BigDecimal valorUnitario;

    @Field("cantidad")
    private BigDecimal cantidad;

    @Field("descuento")
    private BigDecimal descuento;

    @Field("tasaIva")
    private BigDecimal tasaIva;

    @Field("iva")
    private BigDecimal iva;

    @Field("tasaIeps")
    private BigDecimal tasaIeps;

    @Field("ieps")
    private BigDecimal ieps;

    @Field("noPedimento")
    private String noPedimento;

    @Field("fechaCreacion")
    private LocalDateTime fechaCreacion;

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUuidFactura() { return uuidFactura; }
    public void setUuidFactura(String uuidFactura) { this.uuidFactura = uuidFactura; }

    public Long getIdFactura() { return idFactura; }
    public void setIdFactura(Long idFactura) { this.idFactura = idFactura; }

    public String getSkuClaveSat() { return skuClaveSat; }
    public void setSkuClaveSat(String skuClaveSat) { this.skuClaveSat = skuClaveSat; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public BigDecimal getTasaIva() { return tasaIva; }
    public void setTasaIva(BigDecimal tasaIva) { this.tasaIva = tasaIva; }

    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }

    public BigDecimal getTasaIeps() { return tasaIeps; }
    public void setTasaIeps(BigDecimal tasaIeps) { this.tasaIeps = tasaIeps; }

    public BigDecimal getIeps() { return ieps; }
    public void setIeps(BigDecimal ieps) { this.ieps = ieps; }

    public String getNoPedimento() { return noPedimento; }
    public void setNoPedimento(String noPedimento) { this.noPedimento = noPedimento; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}