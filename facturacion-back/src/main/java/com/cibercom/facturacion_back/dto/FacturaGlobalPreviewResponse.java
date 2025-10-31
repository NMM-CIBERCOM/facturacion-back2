package com.cibercom.facturacion_back.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta de preview de factura global con montos agregados por tasa.
 */
@Data
public class FacturaGlobalPreviewResponse {
    private String fecha;
    private String codigoTienda;
    private Integer ticketsSeleccionados;

    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;

    private List<MontoPorTasa> porTasa;

    @Data
    public static class MontoPorTasa {
        /** Tasa en fracción (ej. 0.16, 0.08, 0.00). */
        private BigDecimal tasa;
        private BigDecimal base;
        private BigDecimal iva;
        private BigDecimal total;
    }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getCodigoTienda() { return codigoTienda; }
    public void setCodigoTienda(String codigoTienda) { this.codigoTienda = codigoTienda; }
    public Integer getTicketsSeleccionados() { return ticketsSeleccionados; }
    public void setTicketsSeleccionados(Integer ticketsSeleccionados) { this.ticketsSeleccionados = ticketsSeleccionados; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public List<MontoPorTasa> getPorTasa() { return porTasa; }
    public void setPorTasa(List<MontoPorTasa> porTasa) { this.porTasa = porTasa; }
}