package com.cibercom.facturacion_back.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * Request para consulta global de Facturas, Tickets y Carta Porte.
 */
public class FacturaGlobalConsultaRequest {
    /** Periodo: "DIA" o "SEMANA". */
    private String periodo;
    /** Fecha ancla (yyyy-MM-dd). Se usa para el día o calcular semana. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    /** Código de tienda (opcional). */
    private String codigoTienda;
    /** Terminal (opcional). */
    private Integer terminalId;
    /** Incluir registros de Carta Porte (opcional, por defecto true). */
    private Boolean incluirCartaPorte = Boolean.TRUE;

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getCodigoTienda() { return codigoTienda; }
    public void setCodigoTienda(String codigoTienda) { this.codigoTienda = codigoTienda; }

    public Integer getTerminalId() { return terminalId; }
    public void setTerminalId(Integer terminalId) { this.terminalId = terminalId; }

    public Boolean getIncluirCartaPorte() { return incluirCartaPorte; }
    public void setIncluirCartaPorte(Boolean incluirCartaPorte) { this.incluirCartaPorte = incluirCartaPorte; }
}