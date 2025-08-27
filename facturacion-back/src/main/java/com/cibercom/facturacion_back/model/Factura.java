package com.cibercom.facturacion_back.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "FACTURAS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Factura {
    
    @Id
    @Column(name = "UUID", length = 36)
    private String uuid;
    
    @Column(name = "XML_CONTENT", columnDefinition = "CLOB")
    private String xmlContent;
    
    @Column(name = "FECHA_GENERACION")
    private LocalDateTime fechaGeneracion;
    
    @Column(name = "FECHA_TIMBRADO")
    private LocalDateTime fechaTimbrado;
    
    // Datos del Emisor
    @Column(name = "EMISOR_RFC", length = 13)
    private String emisorRfc;
    
    @Column(name = "EMISOR_RAZON_SOCIAL", length = 200)
    private String emisorRazonSocial;
    
    @Column(name = "EMISOR_NOMBRE", length = 100)
    private String emisorNombre;
    
    @Column(name = "EMISOR_PATERNO", length = 100)
    private String emisorPaterno;
    
    @Column(name = "EMISOR_MATERNO", length = 100)
    private String emisorMaterno;
    
    @Column(name = "EMISOR_CORREO", length = 200)
    private String emisorCorreo;
    
    @Column(name = "EMISOR_PAIS", length = 50)
    private String emisorPais;
    
    @Column(name = "EMISOR_DOMICILIO_FISCAL", length = 500)
    private String emisorDomicilioFiscal;
    
    @Column(name = "EMISOR_REGIMEN_FISCAL", length = 10)
    private String emisorRegimenFiscal;
    
    // Datos del Receptor
    @Column(name = "RECEPTOR_RFC", length = 13)
    private String receptorRfc;
    
    @Column(name = "RECEPTOR_RAZON_SOCIAL", length = 200)
    private String receptorRazonSocial;
    
    @Column(name = "RECEPTOR_NOMBRE", length = 100)
    private String receptorNombre;
    
    @Column(name = "RECEPTOR_PATERNO", length = 100)
    private String receptorPaterno;
    
    @Column(name = "RECEPTOR_MATERNO", length = 100)
    private String receptorMaterno;
    
    @Column(name = "RECEPTOR_CORREO", length = 200)
    private String receptorCorreo;
    
    @Column(name = "RECEPTOR_PAIS", length = 50)
    private String receptorPais;
    
    @Column(name = "RECEPTOR_DOMICILIO_FISCAL", length = 500)
    private String receptorDomicilioFiscal;
    
    @Column(name = "RECEPTOR_REGIMEN_FISCAL", length = 10)
    private String receptorRegimenFiscal;
    
    @Column(name = "RECEPTOR_USO_CFDI", length = 10)
    private String receptorUsoCfdi;
    
    // Datos de la Factura
    @Column(name = "CODIGO_FACTURACION", length = 50)
    private String codigoFacturacion;
    
    @Column(name = "TIENDA", length = 100)
    private String tienda;
    
    @Column(name = "FECHA_FACTURA")
    private LocalDateTime fechaFactura;
    
    @Column(name = "TERMINAL", length = 50)
    private String terminal;
    
    @Column(name = "BOLETA", length = 50)
    private String boleta;
    
    @Column(name = "MEDIO_PAGO", length = 10)
    private String medioPago;
    
    @Column(name = "FORMA_PAGO", length = 10)
    private String formaPago;
    
    @Column(name = "IEPS_DESGLOSADO")
    private Boolean iepsDesglosado;
    
    // Totales
    @Column(name = "SUBTOTAL", precision = 15, scale = 2)
    private BigDecimal subtotal;
    
    @Column(name = "IVA", precision = 15, scale = 2)
    private BigDecimal iva;
    
    @Column(name = "IEPS", precision = 15, scale = 2)
    private BigDecimal ieps;
    
    @Column(name = "TOTAL", precision = 15, scale = 2)
    private BigDecimal total;
    
    // Estado y Control
    @Column(name = "ESTADO", length = 20)
    private String estado; // GENERADA, TIMBRADA, CANCELADA
    
    @Column(name = "SERIE", length = 10)
    private String serie;
    
    @Column(name = "FOLIO", length = 20)
    private String folio;
    
    @Column(name = "CADENA_ORIGINAL", columnDefinition = "CLOB")
    private String cadenaOriginal;
    
    @Column(name = "SELLO_DIGITAL", columnDefinition = "CLOB")
    private String selloDigital;
    
    @Column(name = "CERTIFICADO", columnDefinition = "CLOB")
    private String certificado;
    

    
    @Column(name = "FECHA_CREACION")
    private LocalDateTime fechaCreacion;
    
    @Column(name = "FECHA_MODIFICACION")
    private LocalDateTime fechaModificacion;
    
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaModificacion = LocalDateTime.now();
        if (fechaGeneracion == null) {
            fechaGeneracion = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
} 