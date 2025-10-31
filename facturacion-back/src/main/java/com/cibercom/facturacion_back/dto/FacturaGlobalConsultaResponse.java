package com.cibercom.facturacion_back.dto;

import java.util.List;

/**
 * Respuesta para consulta global.
 */
public class FacturaGlobalConsultaResponse {
    private List<FacturaGlobalRegistro> registros;
    private List<FacturaGlobalFacturaDTO> facturasAgregadas;
    private int totalFacturas;
    private int totalTickets;
    private int totalCartaPorte;

    public List<FacturaGlobalRegistro> getRegistros() { return registros; }
    public void setRegistros(List<FacturaGlobalRegistro> registros) { this.registros = registros; }

    public List<FacturaGlobalFacturaDTO> getFacturasAgregadas() { return facturasAgregadas; }
    public void setFacturasAgregadas(List<FacturaGlobalFacturaDTO> facturasAgregadas) { this.facturasAgregadas = facturasAgregadas; }

    public int getTotalFacturas() { return totalFacturas; }
    public void setTotalFacturas(int totalFacturas) { this.totalFacturas = totalFacturas; }

    public int getTotalTickets() { return totalTickets; }
    public void setTotalTickets(int totalTickets) { this.totalTickets = totalTickets; }

    public int getTotalCartaPorte() { return totalCartaPorte; }
    public void setTotalCartaPorte(int totalCartaPorte) { this.totalCartaPorte = totalCartaPorte; }
}