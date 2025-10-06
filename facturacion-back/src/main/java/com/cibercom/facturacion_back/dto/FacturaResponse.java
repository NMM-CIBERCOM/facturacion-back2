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
    public static FacturaResponseBuilder builder() {
        return new FacturaResponseBuilder();
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
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DatosFactura {
        private String folioFiscal;
        private String serie;
        private String folio;
        private LocalDateTime fechaTimbrado;
        private BigDecimal subtotal;
        private BigDecimal iva;
        private BigDecimal total;
        private String cadenaOriginal;
        private String selloDigital;
        private String certificado;
    }
}