package com.cibercom.facturacion_back.dto;

import lombok.Data;

/**
 * Filtros de búsqueda para tickets.
 */
@Data
public class TicketSearchRequest {
    /** Código de tienda (ej. "S001"). */
    private String codigoTienda;
    /** Identificador de terminal (opcional). */
    private Integer terminalId;
    /** Fecha del ticket en formato YYYY-MM-DD (opcional). */
    private String fecha;
    /** Folio del ticket/boleta (opcional). */
    private Integer folio;

    // Getters/Setters explícitos para robustez cuando Lombok no esté activo
    public String getCodigoTienda() { return codigoTienda; }
    public void setCodigoTienda(String codigoTienda) { this.codigoTienda = codigoTienda; }

    public Integer getTerminalId() { return terminalId; }
    public void setTerminalId(Integer terminalId) { this.terminalId = terminalId; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public Integer getFolio() { return folio; }
    public void setFolio(Integer folio) { this.folio = folio; }
}