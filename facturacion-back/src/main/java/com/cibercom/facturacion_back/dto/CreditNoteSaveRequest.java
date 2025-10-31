package com.cibercom.facturacion_back.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Petición para guardar una nota de crédito en FACTURAS y NOTAS_CREDITO.
 */
@Data
public class CreditNoteSaveRequest {
    // Relación con factura origen
    private String uuidFacturaOrig;
    private String serieFacturaOrig; // opcional, se puede resolver por UUID
    private String folioFacturaOrig; // opcional, se puede resolver por UUID

    // Datos de la nota de crédito
    private String uuidNc;           // UUID de la nota de crédito (folio fiscal)
    private String serieNc;          // Serie de la NC
    private String folioNc;          // Folio de la NC
    private LocalDateTime fechaEmision; // opcional, default ahora
    private String usoCfdi;          // Ej: G02
    private String regimenFiscal;    // Ej: 601
    private String motivo;           // Texto/clave de motivo SAT
    private String concepto;         // Descripción del concepto principal
    private BigDecimal cantidad;     // Cantidad del concepto
    private String unidad;           // Unidad, ej: E48
    private BigDecimal precioUnitario; // Precio unitario

    // Importes
    private BigDecimal subtotal;
    private BigDecimal ivaImporte;
    private BigDecimal ivaPorcentaje;   // opcional; si no viene, se usa config
    private BigDecimal iepsImporte;     // opcional
    private BigDecimal iepsPorcentaje;  // opcional
    private BigDecimal total;

    // CFDI / SAT
    private String xmlContent;       // XML timbrado o generado
    private String selloDigital;     // opcional
    private String estatusSat;       // opcional
    private Integer codeSat;         // opcional

    // Emisor/Receptor básicos para FACTURAS
    private String rfcEmisor;
    private String rfcReceptor;
    private String formaPago;        // Ej: 01
    private String metodoPago;       // Ej: PUE

    // Explicit getters and setters to ensure compatibility when Lombok processing is unavailable
    // Relación con factura origen
    public String getUuidFacturaOrig() { return uuidFacturaOrig; }
    public void setUuidFacturaOrig(String uuidFacturaOrig) { this.uuidFacturaOrig = uuidFacturaOrig; }
    public String getSerieFacturaOrig() { return serieFacturaOrig; }
    public void setSerieFacturaOrig(String serieFacturaOrig) { this.serieFacturaOrig = serieFacturaOrig; }
    public String getFolioFacturaOrig() { return folioFacturaOrig; }
    public void setFolioFacturaOrig(String folioFacturaOrig) { this.folioFacturaOrig = folioFacturaOrig; }

    // Datos de la nota de crédito
    public String getUuidNc() { return uuidNc; }
    public void setUuidNc(String uuidNc) { this.uuidNc = uuidNc; }
    public String getSerieNc() { return serieNc; }
    public void setSerieNc(String serieNc) { this.serieNc = serieNc; }
    public String getFolioNc() { return folioNc; }
    public void setFolioNc(String folioNc) { this.folioNc = folioNc; }
    public java.time.LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(java.time.LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }
    public String getUsoCfdi() { return usoCfdi; }
    public void setUsoCfdi(String usoCfdi) { this.usoCfdi = usoCfdi; }
    public String getRegimenFiscal() { return regimenFiscal; }
    public void setRegimenFiscal(String regimenFiscal) { this.regimenFiscal = regimenFiscal; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public java.math.BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(java.math.BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public java.math.BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(java.math.BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    // Importes
    public java.math.BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(java.math.BigDecimal subtotal) { this.subtotal = subtotal; }
    public java.math.BigDecimal getIvaImporte() { return ivaImporte; }
    public void setIvaImporte(java.math.BigDecimal ivaImporte) { this.ivaImporte = ivaImporte; }
    public java.math.BigDecimal getIvaPorcentaje() { return ivaPorcentaje; }
    public void setIvaPorcentaje(java.math.BigDecimal ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }
    public java.math.BigDecimal getIepsImporte() { return iepsImporte; }
    public void setIepsImporte(java.math.BigDecimal iepsImporte) { this.iepsImporte = iepsImporte; }
    public java.math.BigDecimal getIepsPorcentaje() { return iepsPorcentaje; }
    public void setIepsPorcentaje(java.math.BigDecimal iepsPorcentaje) { this.iepsPorcentaje = iepsPorcentaje; }
    public java.math.BigDecimal getTotal() { return total; }
    public void setTotal(java.math.BigDecimal total) { this.total = total; }

    // CFDI / SAT
    public String getXmlContent() { return xmlContent; }
    public void setXmlContent(String xmlContent) { this.xmlContent = xmlContent; }
    public String getSelloDigital() { return selloDigital; }
    public void setSelloDigital(String selloDigital) { this.selloDigital = selloDigital; }
    public String getEstatusSat() { return estatusSat; }
    public void setEstatusSat(String estatusSat) { this.estatusSat = estatusSat; }
    public Integer getCodeSat() { return codeSat; }
    public void setCodeSat(Integer codeSat) { this.codeSat = codeSat; }

    // Emisor/Receptor básicos para FACTURAS
    public String getRfcEmisor() { return rfcEmisor; }
    public void setRfcEmisor(String rfcEmisor) { this.rfcEmisor = rfcEmisor; }
    public String getRfcReceptor() { return rfcReceptor; }
    public void setRfcReceptor(String rfcReceptor) { this.rfcReceptor = rfcReceptor; }
    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
}