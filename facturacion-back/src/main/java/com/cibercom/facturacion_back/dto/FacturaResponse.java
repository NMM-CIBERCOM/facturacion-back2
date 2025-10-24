package com.cibercom.facturacion_back.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FacturaResponse {
    
    private boolean exitoso;
    private String mensaje;
    private LocalDateTime timestamp;
    private String uuid;
    private String xmlTimbrado;
    private DatosFactura datosFactura;
    private String errores;
    
    // Métodos explícitos para compatibilidad
    // Manual builder to replace Lombok builder when annotation processing fails
    public static FacturaResponseBuilder builder() {
        return new FacturaResponseBuilder();
    }
    
    public static class FacturaResponseBuilder {
        private final FacturaResponse resp = new FacturaResponse();
    
        public FacturaResponseBuilder exitoso(boolean exitoso) { resp.exitoso = exitoso; return this; }
        public FacturaResponseBuilder mensaje(String mensaje) { resp.mensaje = mensaje; return this; }
        public FacturaResponseBuilder timestamp(java.time.LocalDateTime timestamp) { resp.timestamp = timestamp; return this; }
        public FacturaResponseBuilder uuid(String uuid) { resp.uuid = uuid; return this; }
        public FacturaResponseBuilder xmlTimbrado(String xmlTimbrado) { resp.xmlTimbrado = xmlTimbrado; return this; }
        public FacturaResponseBuilder datosFactura(DatosFactura datosFactura) { resp.datosFactura = datosFactura; return this; }
        public FacturaResponseBuilder errores(String errores) { resp.errores = errores; return this; }
    
        public FacturaResponse build() { return resp; }
    }
    
    // Manual builder for DatosFactura
    public static class DatosFacturaBuilder {
        private final DatosFactura d = new DatosFactura();
    
        public DatosFacturaBuilder folioFiscal(String folioFiscal) { d.folioFiscal = folioFiscal; return this; }
        public DatosFacturaBuilder serie(String serie) { d.serie = serie; return this; }
        public DatosFacturaBuilder folio(String folio) { d.folio = folio; return this; }
        public DatosFacturaBuilder fechaTimbrado(java.time.LocalDateTime fechaTimbrado) { d.fechaTimbrado = fechaTimbrado; return this; }
        public DatosFacturaBuilder subtotal(java.math.BigDecimal subtotal) { d.subtotal = subtotal; return this; }
        public DatosFacturaBuilder iva(java.math.BigDecimal iva) { d.iva = iva; return this; }
        public DatosFacturaBuilder total(java.math.BigDecimal total) { d.total = total; return this; }
        public DatosFacturaBuilder cadenaOriginal(String cadenaOriginal) { d.cadenaOriginal = cadenaOriginal; return this; }
        public DatosFacturaBuilder selloDigital(String selloDigital) { d.selloDigital = selloDigital; return this; }
        public DatosFacturaBuilder certificado(String certificado) { d.certificado = certificado; return this; }
    
        public DatosFactura build() { return d; }
    }
    
    // Provide builder() for DatosFactura
    public static DatosFacturaBuilder builderDatos() {
        return new DatosFacturaBuilder();
    }
    
    public boolean isExitoso() {
        return this.exitoso;
    }
    
    public String getMensaje() {
        return this.mensaje;
    }
    
    public String getXmlTimbrado() {
        return this.xmlTimbrado;
    }
    
    public String getErrores() {
        return this.errores;
    }

    // Getters explícitos adicionales para garantizar la serialización JSON
    public String getUuid() { return this.uuid; }
    public LocalDateTime getTimestamp() { return this.timestamp; }
    public DatosFactura getDatosFactura() { return this.datosFactura; }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DatosFactura {
        private String folioFiscal;
        private String serie;
        private String folio;
        private java.time.LocalDateTime fechaTimbrado;
        private java.math.BigDecimal subtotal;
        private java.math.BigDecimal iva;
        private java.math.BigDecimal total;
        private String cadenaOriginal;
        private String selloDigital;
        private String certificado;

        // Manual builder method for DatosFactura
        public static FacturaResponse.DatosFacturaBuilder builder() {
            return new FacturaResponse.DatosFacturaBuilder();
        }

        // Getters explícitos para garantizar la serialización JSON
        public String getFolioFiscal() { return folioFiscal; }
        public String getSerie() { return serie; }
        public String getFolio() { return folio; }
        public java.time.LocalDateTime getFechaTimbrado() { return fechaTimbrado; }
        public java.math.BigDecimal getSubtotal() { return subtotal; }
        public java.math.BigDecimal getIva() { return iva; }
        public java.math.BigDecimal getTotal() { return total; }
        public String getCadenaOriginal() { return cadenaOriginal; }
        public String getSelloDigital() { return selloDigital; }
        public String getCertificado() { return certificado; }
    }
}