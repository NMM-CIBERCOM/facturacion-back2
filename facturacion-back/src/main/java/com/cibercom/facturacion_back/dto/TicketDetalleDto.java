package com.cibercom.facturacion_back.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para detalle de tickets (tabla TICKETS_DETALLE).
 */
@Data
public class TicketDetalleDto {
    private Long idDetalle;
    private Long idTicket;
    private Long productoId;
    private String descripcion;
    private BigDecimal cantidad;
    private String unidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuento;
    private BigDecimal subtotal;
    private BigDecimal ivaPorcentaje;
    private BigDecimal ivaImporte;
    private BigDecimal iepsPorcentaje;
    private BigDecimal iepsImporte;
    private BigDecimal total;

    public Long getIdDetalle() { return idDetalle; }
    public void setIdDetalle(Long idDetalle) { this.idDetalle = idDetalle; }

    public Long getIdTicket() { return idTicket; }
    public void setIdTicket(Long idTicket) { this.idTicket = idTicket; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getIvaPorcentaje() { return ivaPorcentaje; }
    public void setIvaPorcentaje(BigDecimal ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }

    public BigDecimal getIvaImporte() { return ivaImporte; }
    public void setIvaImporte(BigDecimal ivaImporte) { this.ivaImporte = ivaImporte; }

    public BigDecimal getIepsPorcentaje() { return iepsPorcentaje; }
    public void setIepsPorcentaje(BigDecimal iepsPorcentaje) { this.iepsPorcentaje = iepsPorcentaje; }

    public BigDecimal getIepsImporte() { return iepsImporte; }
    public void setIepsImporte(BigDecimal iepsImporte) { this.iepsImporte = iepsImporte; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}