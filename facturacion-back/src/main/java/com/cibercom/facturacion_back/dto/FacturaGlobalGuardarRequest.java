package com.cibercom.facturacion_back.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

/**
 * Request para guardar una factura global con sus relaciones a facturas hijas.
 */
public class FacturaGlobalGuardarRequest {
    private String periodo;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    
    private String codigoTienda;
    private String serie;
    private String folio;
    private Double importe;
    private Double subtotal;
    private List<String> facturasHijasUuid; // UUIDs de las facturas hijas a relacionar
    private Integer totalFacturas;
    private Integer totalTickets;
    private Integer totalCartaPorte;
    private String descripcion;

    // Getters y Setters
    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getCodigoTienda() { return codigoTienda; }
    public void setCodigoTienda(String codigoTienda) { this.codigoTienda = codigoTienda; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public Double getImporte() { return importe; }
    public void setImporte(Double importe) { this.importe = importe; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public List<String> getFacturasHijasUuid() { return facturasHijasUuid; }
    public void setFacturasHijasUuid(List<String> facturasHijasUuid) { this.facturasHijasUuid = facturasHijasUuid; }

    public Integer getTotalFacturas() { return totalFacturas; }
    public void setTotalFacturas(Integer totalFacturas) { this.totalFacturas = totalFacturas; }

    public Integer getTotalTickets() { return totalTickets; }
    public void setTotalTickets(Integer totalTickets) { this.totalTickets = totalTickets; }

    public Integer getTotalCartaPorte() { return totalCartaPorte; }
    public void setTotalCartaPorte(Integer totalCartaPorte) { this.totalCartaPorte = totalCartaPorte; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}


