package com.cibercom.facturacion_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FacturaRequest {
    
    @NotBlank(message = "El nombre del emisor es obligatorio")
    private String nombreEmisor;
    
    @NotBlank(message = "El RFC del emisor es obligatorio")
    private String rfcEmisor;
    
    @NotBlank(message = "El código postal del emisor es obligatorio")
    private String codigoPostalEmisor;
    
    @NotBlank(message = "El régimen fiscal del emisor es obligatorio")
    private String regimenFiscalEmisor;
    
    @NotBlank(message = "El nombre del receptor es obligatorio")
    private String nombreReceptor;
    
    @NotBlank(message = "El RFC del receptor es obligatorio")
    private String rfcReceptor;
    
    @NotBlank(message = "El código postal del receptor es obligatorio")
    private String codigoPostalReceptor;
    
    @NotBlank(message = "El régimen fiscal del receptor es obligatorio")
    private String regimenFiscalReceptor;
    
    @NotNull(message = "La lista de conceptos es obligatoria")
    private List<Concepto> conceptos;
    
    @NotBlank(message = "El método de pago es obligatorio")
    private String metodoPago;
    
    @NotBlank(message = "La forma de pago es obligatoria")
    private String formaPago;
    
    @NotBlank(message = "El uso CFDI es obligatorio")
    private String usoCFDI;
    
    @Data
    public static class Concepto {
        @NotBlank(message = "La descripción del concepto es obligatoria")
        private String descripcion;
        
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
        private BigDecimal cantidad;
        
        @NotBlank(message = "La unidad es obligatoria")
        private String unidad;
        
        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
        private BigDecimal precioUnitario;
        
        @NotNull(message = "El importe es obligatorio")
        @DecimalMin(value = "0.01", message = "El importe debe ser mayor a 0")
        private BigDecimal importe;
    }
} 