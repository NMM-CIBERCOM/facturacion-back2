package com.cibercom.facturacion_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SatValidationRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}$", 
             message = "El RFC debe tener un formato válido")
    private String rfc;
    
    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^[0-9]{5}$", 
             message = "El código postal debe tener 5 dígitos")
    private String codigoPostal;
    
    @NotBlank(message = "El régimen fiscal es obligatorio")
    private String regimenFiscal;
    
    // Para persona física, el régimen fiscal debe ser uno de estos valores
    public static final String[] REGIMENES_FISICA = {
        "601", // General de Ley Personas Morales
        "603", // Personas Morales con Fines no Lucrativos
        "605", // Sueldos y Salarios e Ingresos Asimilados a Salarios
        "606", // Arrendamiento
        "608", // Demás ingresos
        "609", // Consolación
        "610", // Residentes en el Extranjero sin Establecimiento Permanente en México
        "611", // Ingresos por Dividendos (socios y accionistas)
        "612", // Personas Físicas con Actividades Empresariales y Profesionales
        "614", // Ingresos por intereses
        "616", // Sin obligaciones fiscales
        "620", // Sociedades Cooperativas de Producción que optan por diferir sus ingresos
        "621", // Incorporación Fiscal
        "622", // Actividades Agrícolas, Ganaderas, Silvícolas y Pesqueras
        "623", // Opcional para Grupos de Sociedades
        "624", // Coordinados
        "625", // Régimen de las Actividades Empresariales con ingresos a través de Plataformas Tecnológicas
        "626"  // Régimen Simplificado de Confianza
    };
} 