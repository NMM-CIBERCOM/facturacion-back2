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

    public String getEmisorRfc() {
        return emisor != null ? (String) emisor.get("rfc") : null;
    }

    public String getEmisorRazonSocial() {
        return emisor != null ? (String) emisor.get("razonSocial") : null;
    }

    public String getEmisorNombre() {
        return emisor != null ? (String) emisor.get("nombre") : null;
    }

    public String getReceptorRfc() {
        return receptor != null ? (String) receptor.get("rfc") : null;
    }

    public String getReceptorRazonSocial() {
        return receptor != null ? (String) receptor.get("razonSocial") : null;
    }

    public String getReceptorNombre() {
        return receptor != null ? (String) receptor.get("nombre") : null;
    }

    // Getters explícitos para compatibilidad si Lombok falla
    public java.util.Map<String, Object> getReceptor() { return receptor; }
    public java.util.Map<String, Object> getEmisor() { return emisor; }
    public String getEstado() { return estado; }
    public String getUuid() { return uuid; }
    public String getXmlContent() { return xmlContent; }
    public java.time.LocalDateTime getFechaTimbrado() { return fechaTimbrado; }
    public java.math.BigDecimal getSubtotal() { return subtotal; }
    public java.math.BigDecimal getIva() { return iva; }
    public java.math.BigDecimal getTotal() { return total; }
    public String getCadenaOriginal() { return cadenaOriginal; }
    public String getSelloDigital() { return selloDigital; }
    public String getCertificado() { return certificado; }
    public String getSerie() { return serie; }
    public String getFolio() { return folio; }
    public java.time.LocalDateTime getFechaGeneracion() { return fechaGeneracion; }

    // Explicit setters to ensure compatibility when Lombok processing is unavailable
    public void setUuid(String uuid) { this.uuid = uuid; }
    public void setXmlContent(String xmlContent) { this.xmlContent = xmlContent; }
    public void setFechaGeneracion(java.time.LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
    public void setFechaTimbrado(java.time.LocalDateTime fechaTimbrado) { this.fechaTimbrado = fechaTimbrado; }
    public void setSubtotal(java.math.BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setIva(java.math.BigDecimal iva) { this.iva = iva; }
    public void setTotal(java.math.BigDecimal total) { this.total = total; }
    public void setCadenaOriginal(String cadenaOriginal) { this.cadenaOriginal = cadenaOriginal; }
    public void setSelloDigital(String selloDigital) { this.selloDigital = selloDigital; }
    public void setCertificado(String certificado) { this.certificado = certificado; }
    public void setSerie(String serie) { this.serie = serie; }
    public void setFolio(String folio) { this.folio = folio; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setEstadoDescripcion(String estadoDescripcion) { this.estadoDescripcion = estadoDescripcion; }
    // Manual builder to replace Lombok builder when annotation processing fails
    public static FacturaMongoBuilder builder() {
        return new FacturaMongoBuilder();
    }

    public static class FacturaMongoBuilder {
        private final FacturaMongo f = new FacturaMongo();

        public FacturaMongoBuilder id(String id) { f.id = id; return this; }
        public FacturaMongoBuilder uuid(String uuid) { f.uuid = uuid; return this; }
        public FacturaMongoBuilder xmlContent(String xmlContent) { f.xmlContent = xmlContent; return this; }
        public FacturaMongoBuilder fechaGeneracion(java.time.LocalDateTime fechaGeneracion) { f.fechaGeneracion = fechaGeneracion; return this; }
        public FacturaMongoBuilder fechaTimbrado(java.time.LocalDateTime fechaTimbrado) { f.fechaTimbrado = fechaTimbrado; return this; }

        public FacturaMongoBuilder emisor(java.util.Map<String, Object> emisor) { f.emisor = emisor; return this; }
        public FacturaMongoBuilder receptor(java.util.Map<String, Object> receptor) { f.receptor = receptor; return this; }

        public FacturaMongoBuilder codigoFacturacion(String codigoFacturacion) { f.codigoFacturacion = codigoFacturacion; return this; }
        public FacturaMongoBuilder tienda(String tienda) { f.tienda = tienda; return this; }
        public FacturaMongoBuilder fechaFactura(java.time.LocalDateTime fechaFactura) { f.fechaFactura = fechaFactura; return this; }
        public FacturaMongoBuilder terminal(String terminal) { f.terminal = terminal; return this; }
        public FacturaMongoBuilder boleta(String boleta) { f.boleta = boleta; return this; }
        public FacturaMongoBuilder medioPago(String medioPago) { f.medioPago = medioPago; return this; }
        public FacturaMongoBuilder formaPago(String formaPago) { f.formaPago = formaPago; return this; }
        public FacturaMongoBuilder iepsDesglosado(Boolean iepsDesglosado) { f.iepsDesglosado = iepsDesglosado; return this; }

        public FacturaMongoBuilder subtotal(java.math.BigDecimal subtotal) { f.subtotal = subtotal; return this; }
        public FacturaMongoBuilder iva(java.math.BigDecimal iva) { f.iva = iva; return this; }
        public FacturaMongoBuilder ieps(java.math.BigDecimal ieps) { f.ieps = ieps; return this; }
        public FacturaMongoBuilder total(java.math.BigDecimal total) { f.total = total; return this; }

        public FacturaMongoBuilder estado(String estado) { f.estado = estado; return this; }
        public FacturaMongoBuilder estadoDescripcion(String estadoDescripcion) { f.estadoDescripcion = estadoDescripcion; return this; }
        public FacturaMongoBuilder serie(String serie) { f.serie = serie; return this; }
        public FacturaMongoBuilder folio(String folio) { f.folio = folio; return this; }
        public FacturaMongoBuilder cadenaOriginal(String cadenaOriginal) { f.cadenaOriginal = cadenaOriginal; return this; }
        public FacturaMongoBuilder selloDigital(String selloDigital) { f.selloDigital = selloDigital; return this; }
        public FacturaMongoBuilder certificado(String certificado) { f.certificado = certificado; return this; }

        public FacturaMongoBuilder fechaCreacion(java.time.LocalDateTime fechaCreacion) { f.fechaCreacion = fechaCreacion; return this; }
        public FacturaMongoBuilder fechaModificacion(java.time.LocalDateTime fechaModificacion) { f.fechaModificacion = fechaModificacion; return this; }

        public FacturaMongo build() { return f; }
    }
}
