package com.cibercom.facturacion_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registro unificado para resultados de consulta global.
 */
public class FacturaGlobalRegistro {
    private String tipo; // FACTURA, TICKET, CARTA_PORTE
    private String tienda; // puede ser null para carta porte
    private String terminal; // opcional (tickets)
    private LocalDate fecha;
    private String folio; // serie-folio (factura), folio (ticket), numeroSerie (carta porte)
    private String uuid; // solo para factura
    private BigDecimal total;
    private String estado; // estatus

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTienda() { return tienda; }
    public void setTienda(String tienda) { this.tienda = tienda; }

    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}