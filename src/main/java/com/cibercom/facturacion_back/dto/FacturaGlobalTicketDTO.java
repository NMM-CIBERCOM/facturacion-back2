package com.cibercom.facturacion_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO que representa un ticket con sus detalles asociados.
 */
public class FacturaGlobalTicketDTO {
    private Long idTicket;
    private LocalDate fecha;
    private Integer folio;
    private BigDecimal total;
    private String formaPago;
    private java.util.List<TicketDetalleDto> detalles;

    public Long getIdTicket() { return idTicket; }
    public void setIdTicket(Long idTicket) { this.idTicket = idTicket; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Integer getFolio() { return folio; }
    public void setFolio(Integer folio) { this.folio = folio; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public List<TicketDetalleDto> getDetalles() { return detalles; }
    public void setDetalles(List<TicketDetalleDto> detalles) { this.detalles = detalles; }
}