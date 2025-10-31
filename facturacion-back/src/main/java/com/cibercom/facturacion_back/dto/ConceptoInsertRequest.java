package com.cibercom.facturacion_back.dto;

import java.math.BigDecimal;

/**
 * Request para agregar un concepto a una factura.
 * Se recomienda enviar el UUID de la factura; si el ID_FACTURA está disponible,
 * puede enviarse directamente.
 */
public class ConceptoInsertRequest {
    private String uuidFactura; // preferido
    private Long idFactura;     // opcional

    private String skuClaveSat;
    private String descripcion;
    private String unidadMedida;
    private BigDecimal valorUnitario;
    private BigDecimal cantidad;
    private BigDecimal descuento;
    private BigDecimal tasaIva;
    private BigDecimal iva;
    private BigDecimal tasaIeps;
    private BigDecimal ieps;
    private String noPedimento;

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
}