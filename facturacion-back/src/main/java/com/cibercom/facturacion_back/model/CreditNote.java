package com.cibercom.facturacion_back.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CREDIT_NOTES", indexes = {
        @Index(name = "idx_credit_note_fecha", columnList = "FECHA_EMISION"),
        @Index(name = "idx_credit_note_estado", columnList = "ESTATUS"),
        @Index(name = "idx_credit_note_unique_key", columnList = "UNIQUE_KEY", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditNote {

    @Id
    @Column(name = "UUID_NC", length = 36)
    private String uuidNc;

    @Column(name = "FECHA_EMISION")
    private LocalDateTime fechaEmision;

    @Column(name = "SERIE", length = 10)
    private String serie;

    @Column(name = "FOLIO", length = 20)
    private String folio;

    @Column(name = "ESTATUS", length = 30)
    private String estatus;

    @Column(name = "XML_BASE64", columnDefinition = "CLOB")
    private String xmlBase64;

    @Column(name = "HTML_BASE64", columnDefinition = "CLOB")
    private String htmlBase64;

    @Column(name = "TOTAL", precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "SUBTOTAL", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "IVA", precision = 15, scale = 2)
    private BigDecimal iva;

    @Column(name = "TIPO_COMPROBANTE", length = 5)
    private String tipoComprobante; // Debe ser "E" para egreso

    @Column(name = "USO_CFDI", length = 5)
    private String usoCfdi; // Ej. G02

    @Column(name = "TIPO_RELACION", length = 5)
    private String tipoRelacion; // Siempre "01"

    @Column(name = "RFC_EMISOR", length = 13)
    private String rfcEmisor;

    @Column(name = "RFC_RECEPTOR", length = 13)
    private String rfcReceptor;

    @Column(name = "UNIQUE_KEY", length = 255, unique = true)
    private String uniqueKey; // Para idempotencia (periodo + UUIDs relacionados ordenados)

    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CreditNoteLink> links = new ArrayList<>();

    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CreditNoteItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (fechaEmision == null) {
            fechaEmision = LocalDateTime.now();
        }
        if (tipoRelacion == null || tipoRelacion.isBlank()) {
            tipoRelacion = "01";
        }
        if (tipoComprobante == null || tipoComprobante.isBlank()) {
            tipoComprobante = "E";
        }
    }

    // Getters explícitos para compatibilidad si Lombok falla
    public String getUuidNc() { return uuidNc; }
    public java.math.BigDecimal getTotal() { return total; }
    public java.util.List<CreditNoteLink> getLinks() { return links; }
    public java.util.List<CreditNoteItem> getItems() { return items; }
    public String getSerie() { return serie; }
    public String getFolio() { return folio; }
    public java.time.LocalDateTime getFechaEmision() { return fechaEmision; }
    public java.math.BigDecimal getSubtotal() { return subtotal; }
    public java.math.BigDecimal getIva() { return iva; }
    public String getTipoComprobante() { return tipoComprobante; }
    public String getUsoCfdi() { return usoCfdi; }
    public String getTipoRelacion() { return tipoRelacion; }
    public String getXmlBase64() { return xmlBase64; }
    public String getHtmlBase64() { return htmlBase64; }
    public String getEstatus() { return estatus; }

    // Setters explícitos usados por el servicio
    public void setLinks(java.util.List<CreditNoteLink> links) { this.links = links; }
    public void setItems(java.util.List<CreditNoteItem> items) { this.items = items; }
    public void setRfcEmisor(String rfcEmisor) { this.rfcEmisor = rfcEmisor; }
    public void setRfcReceptor(String rfcReceptor) { this.rfcReceptor = rfcReceptor; }

    // Builder manual para compatibilidad cuando Lombok no esté activo
    public static CreditNoteBuilder builder() { return new CreditNoteBuilder(); }
    public static class CreditNoteBuilder {
        private final CreditNote c = new CreditNote();
        public CreditNoteBuilder uuidNc(String uuidNc) { c.uuidNc = uuidNc; return this; }
        public CreditNoteBuilder fechaEmision(java.time.LocalDateTime fechaEmision) { c.fechaEmision = fechaEmision; return this; }
        public CreditNoteBuilder serie(String serie) { c.serie = serie; return this; }
        public CreditNoteBuilder folio(String folio) { c.folio = folio; return this; }
        public CreditNoteBuilder estatus(String estatus) { c.estatus = estatus; return this; }
        public CreditNoteBuilder xmlBase64(String xmlBase64) { c.xmlBase64 = xmlBase64; return this; }
        public CreditNoteBuilder htmlBase64(String htmlBase64) { c.htmlBase64 = htmlBase64; return this; }
        public CreditNoteBuilder total(java.math.BigDecimal total) { c.total = total; return this; }
        public CreditNoteBuilder subtotal(java.math.BigDecimal subtotal) { c.subtotal = subtotal; return this; }
        public CreditNoteBuilder iva(java.math.BigDecimal iva) { c.iva = iva; return this; }
        public CreditNoteBuilder tipoComprobante(String tipoComprobante) { c.tipoComprobante = tipoComprobante; return this; }
        public CreditNoteBuilder usoCfdi(String usoCfdi) { c.usoCfdi = usoCfdi; return this; }
        public CreditNoteBuilder tipoRelacion(String tipoRelacion) { c.tipoRelacion = tipoRelacion; return this; }
        public CreditNoteBuilder rfcEmisor(String rfcEmisor) { c.rfcEmisor = rfcEmisor; return this; }
        public CreditNoteBuilder rfcReceptor(String rfcReceptor) { c.rfcReceptor = rfcReceptor; return this; }
        public CreditNoteBuilder uniqueKey(String uniqueKey) { c.uniqueKey = uniqueKey; return this; }
        public CreditNote build() { return c; }
    }
}