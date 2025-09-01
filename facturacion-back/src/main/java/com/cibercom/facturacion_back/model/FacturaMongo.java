package com.cibercom.facturacion_back.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "facturas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaMongo {

    @Id
    private String id;
    
    @Field("uuid")
    private String uuid;
    
    @Field("xmlContent")
    private String xmlContent;
    
    @Field("fechaGeneracion")
    private LocalDateTime fechaGeneracion;
    
    @Field("fechaTimbrado")
    private LocalDateTime fechaTimbrado;
    
    // Datos del Emisor
    @Field("emisor")
    private Map<String, Object> emisor;
    
    // Datos del Receptor
    @Field("receptor")
    private Map<String, Object> receptor;
    
    // Datos de la Factura
    @Field("codigoFacturacion")
    private String codigoFacturacion;
    
    @Field("tienda")
    private String tienda;
    
    @Field("fechaFactura")
    private LocalDateTime fechaFactura;
    
    @Field("terminal")
    private String terminal;
    
    @Field("boleta")
    private String boleta;
    
    @Field("medioPago")
    private String medioPago;

    @Field("formaPago")
    private String formaPago;
    
    @Field("iepsDesglosado")
    private Boolean iepsDesglosado;
    
    // Totales
    @Field("subtotal")
    private BigDecimal subtotal;
    
    @Field("iva")
    private BigDecimal iva;
    
    @Field("ieps")
    private BigDecimal ieps;
    
    @Field("total")
    private BigDecimal total;
    
    // Estado y Control
    @Field("estado")
    private String estado; // Código del estado según SAT (66, 0, 1, 2, 3, 4, 99, 67)
    
    @Field("estadoDescripcion")
    private String estadoDescripcion; // Descripción del estado
    
    @Field("serie")
    private String serie;
    
    @Field("folio")
    private String folio;
    
    @Field("cadenaOriginal")
    private String cadenaOriginal;
    
    @Field("selloDigital")
    private String selloDigital;
    
    @Field("certificado")
    private String certificado;
    

    
    @Field("fechaCreacion")
    private LocalDateTime fechaCreacion;
    
    @Field("fechaModificacion")
    private LocalDateTime fechaModificacion;
    
    // Métodos para facilitar el acceso a datos del emisor
    public String getEmisorRfc() {
        return emisor != null ? (String) emisor.get("rfc") : null;
    }

    public String getEmisorRazonSocial() {
        return emisor != null ? (String) emisor.get("razonSocial") : null;
    }

    public String getEmisorNombre() {
        return emisor != null ? (String) emisor.get("nombre") : null;
    }

    // Métodos para facilitar el acceso a datos del receptor
    public String getReceptorRfc() {
        return receptor != null ? (String) receptor.get("rfc") : null;
    }

    public String getReceptorRazonSocial() {
        return receptor != null ? (String) receptor.get("razonSocial") : null;
    }
    
    public String getReceptorNombre() {
        return receptor != null ? (String) receptor.get("nombre") : null;
    }
}
