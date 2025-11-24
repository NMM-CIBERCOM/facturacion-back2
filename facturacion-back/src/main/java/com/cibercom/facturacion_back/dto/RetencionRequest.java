package com.cibercom.facturacion_back.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RetencionRequest {
    // Datos del emisor (se obtienen automáticamente del sistema)
    private String rfcEmisor;
    private String nombreEmisor;
    
    // Datos del receptor
    private String rfcReceptor;
    private String razonSocial; // Para persona moral
    private String nombre; // Para persona física
    private String paterno; // Para persona física
    private String materno; // Para persona física
    private String tipoPersona; // "moral" o "fisica"
    
    // Información de la retención
    private String tipoRetencion; // ISR_SERVICIOS, ISR_ARRENDAMIENTO, IVA, etc.
    private BigDecimal montoBase;
    private BigDecimal isrRetenido;
    private BigDecimal ivaRetenido;
    private BigDecimal montoRetenido; // Total retenido (ISR + IVA)
    
    // Período
    private String periodoMes; // "01" a "12"
    private String periodoAnio; // "2024", etc.
    
    // Fecha de pago
    private String fechaPago; // ISO date string
    
    // Concepto
    private String concepto;
    
    // Correo del receptor
    private String correoReceptor;
    
    // Usuario que registra
    private String usuarioRegistro;
}

