package com.cibercom.facturacion_back.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RetencionResponse {
    private boolean success;
    private String message;
    private Long idRetencion;
    private Long facturaId;
    private List<String> errors;
    
    // UUID del CFDI de retención generado por el PAC
    private String uuidRetencion;
    private String xmlTimbrado;
    
    // Datos de la factura generada
    private String serieRetencion;
    private String folioRetencion;
    private String fechaTimbrado;
    
    // Datos del receptor
    private String correoReceptor;
    private String rfcReceptor;
    private String rfcEmisor;
    
    // Montos
    private BigDecimal montoRetenido;
    private BigDecimal baseRetencion;
}

