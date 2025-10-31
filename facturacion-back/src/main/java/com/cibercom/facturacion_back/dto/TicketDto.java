package com.cibercom.facturacion_back.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO de datos de ticket devueltos por la consulta.
 */
@Data
public class TicketDto {
    private Long idTicket;
    private Integer tiendaId;
    private String codigoTienda;
    private Integer terminalId;
    private java.time.LocalDate fecha;
    private Integer folio;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;
    private String formaPago;
    private String rfcCliente;
    private String nombreCliente;
    private Integer status;
    private Long idFactura;

    // Getters/Setters explícitos para robustez cuando Lombok no esté activo
    public Long getIdTicket() { return idTicket; }
    public void setIdTicket(Long idTicket) { this.idTicket = idTicket; }

    public Integer getTiendaId() { return tiendaId; }
    public void setTiendaId(Integer tiendaId) { this.tiendaId = tiendaId; }

    public String getCodigoTienda() { return codigoTienda; }
    public void setCodigoTienda(String codigoTienda) { this.codigoTienda = codigoTienda; }

    public Integer getTerminalId() { return terminalId; }
    public void setTerminalId(Integer terminalId) { this.terminalId = terminalId; }

    public java.time.LocalDate getFecha() { return fecha; }
    public void setFecha(java.time.LocalDate fecha) { this.fecha = fecha; }

    public Integer getFolio() { return folio; }
    public void setFolio(Integer folio) { this.folio = folio; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public String getRfcCliente() { return rfcCliente; }
    public void setRfcCliente(String rfcCliente) { this.rfcCliente = rfcCliente; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Long getIdFactura() { return idFactura; }
    public void setIdFactura(Long idFactura) { this.idFactura = idFactura; }
}