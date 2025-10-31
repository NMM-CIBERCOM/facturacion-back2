package com.cibercom.facturacion_back.dto;

import lombok.Data;

/**
 * Request para preview de factura global (por día y tienda).
 */
@Data
public class FacturaGlobalPreviewRequest {
    /** Fecha del periodo (YYYY-MM-DD). */
    private String fecha;
    /** Código de tienda (ej. S001). */
    private String codigoTienda;
    /** Terminal opcional para acotar (si aplica). */
    private Integer terminalId;
    /** Excluir tickets ya facturados (ID_FACTURA no nulo). */
    private Boolean excluirFacturados = Boolean.TRUE;

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getCodigoTienda() { return codigoTienda; }
    public void setCodigoTienda(String codigoTienda) { this.codigoTienda = codigoTienda; }
    public Integer getTerminalId() { return terminalId; }
    public void setTerminalId(Integer terminalId) { this.terminalId = terminalId; }
    public Boolean getExcluirFacturados() { return excluirFacturados; }
    public void setExcluirFacturados(Boolean excluirFacturados) { this.excluirFacturados = excluirFacturados; }
}