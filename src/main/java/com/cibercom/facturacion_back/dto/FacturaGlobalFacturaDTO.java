package com.cibercom.facturacion_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO que representa una factura con sus tickets y detalles.
 */
public class FacturaGlobalFacturaDTO {
    private String uuid;
    private String serie;
    private String folio;
    private LocalDate fechaEmision;
    private BigDecimal importe;
    private String estatusFacturacion;
    private String estatusSat;
    private String tienda;
    private List<FacturaGlobalTicketDTO> tickets;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }

    public String getEstatusFacturacion() { return estatusFacturacion; }
    public void setEstatusFacturacion(String estatusFacturacion) { this.estatusFacturacion = estatusFacturacion; }

    public String getEstatusSat() { return estatusSat; }
    public void setEstatusSat(String estatusSat) { this.estatusSat = estatusSat; }

    public String getTienda() { return tienda; }
    public void setTienda(String tienda) { this.tienda = tienda; }

    public List<FacturaGlobalTicketDTO> getTickets() { return tickets; }
    public void setTickets(List<FacturaGlobalTicketDTO> tickets) { this.tickets = tickets; }
}