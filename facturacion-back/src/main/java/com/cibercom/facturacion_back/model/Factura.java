package com.cibercom.facturacion_back.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "FACTURAS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Factura {
    
    @Id
    @Column(name = "UUID", length = 40)
    private String uuid;
    
    @Column(name = "XML_CONTENT", columnDefinition = "CLOB")
    private String xmlContent;
    
    @Transient
    private LocalDateTime fechaGeneracion;
    
    @Transient
    private LocalDateTime fechaTimbrado;
    
    // Datos del Emisor
    @Column(name = "RFC_E", length = 13)
    private String emisorRfc;
    
    @Transient
    private String emisorRazonSocial;
    
    @Transient
    private String emisorNombre;
    
    @Transient
    private String emisorPaterno;
    
    @Transient
    private String emisorMaterno;
    
    @Transient
    private String emisorCorreo;
    
    @Transient
    private String emisorPais;
    
    @Transient
    private String emisorDomicilioFiscal;
    
    @Transient
    private String emisorRegimenFiscal;
    
    // Datos del Receptor
    @Column(name = "RFC_R", length = 13)
    private String receptorRfc;
    
    @Column(name = "RAZON_SOCIAL", length = 400)
    private String receptorRazonSocial;
    
    @Column(name = "ID_RECEPTOR")
    private Long idReceptor;
    
    @Transient
    private String receptorNombre;
    
    @Transient
    private String receptorPaterno;
    
    @Transient
    private String receptorMaterno;
    
    @Transient
    private String receptorCorreo;
    
    @Transient
    private String receptorPais;
    
    @Transient
    private String receptorDomicilioFiscal;
    
    @Transient
    private String receptorRegimenFiscal;
    
    @Column(name = "USO_CFDI", length = 4)
    private String receptorUsoCfdi;
    
    // Datos de la Factura
    @Transient
    private String codigoFacturacion;
    
    @Formula("TO_CHAR(TIENDA_ORIGEN)")
    private String tienda;

    @Column(name = "TIENDA_ORIGEN")
    private Integer tiendaOrigen;
    
    @Column(name = "FECHA")
    private LocalDateTime fechaFactura;
    
    @Formula("TO_CHAR(TERMINAL_BOL)")
    private String terminal;
    
    @Formula("TO_CHAR(BOLETA_BOL)")
    private String boleta;
    
    @Column(name = "METODO_PAGO", length = 3)
    private String medioPago;
    
    @Column(name = "FORMA_PAGO", length = 4)
    private String formaPago;
    
    @Transient
    private Boolean iepsDesglosado;
    
    // Totales
    @Column(name = "SUBTOTAL", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "DESCUENTO", precision = 15, scale = 2)
    private BigDecimal descuento;
    
    @Transient
    private BigDecimal iva;
    
    @Transient
    private BigDecimal ieps;
    
    @Column(name = "IMPORTE", precision = 15, scale = 2)
    private BigDecimal total;
    
    // Estado y Control
    @Transient
    private String estado; // Código del estado según SAT (66, 0, 1, 2, 3, 4, 99, 67)
    
    @Transient
    private String estadoDescripcion; // Descripción del estado

    @Column(name = "ESTATUS_FACTURA")
    private Integer estatusFactura;

    @Column(name = "STATUS_SAT", length = 500)
    private String statusSat;

    @Column(name = "CODE_SAT")
    private BigDecimal codeSat;
    
    @Column(name = "SERIE", length = 10)
    private String serie;
    
    @Column(name = "FOLIO")
    private String folio;
    
    @Column(name = "TERMINAL_BOL")
    private Integer terminalBol;

    @Column(name = "BOLETA_BOL")
    private Integer boletaBol;
    
    @Transient
    private String cadenaOriginal;
    
    @Transient
    private String selloDigital;
    
    @Transient
    private String certificado;
    
    
    
    @Transient
    private LocalDateTime fechaCreacion;
    
    @Transient
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
    
    // Método para obtener la fecha de emisión (requerido por CorreoService)
    public LocalDateTime getFechaEmision() {
        return fechaGeneracion;
    }
    
    public String getEmisorRfc() { return emisorRfc; }
    public String getEmisorRazonSocial() { return emisorRazonSocial; }
    public String getReceptorRfc() { return receptorRfc; }
    public String getReceptorRazonSocial() { return receptorRazonSocial; }
    public Long getIdReceptor() { return idReceptor; }
    public java.math.BigDecimal getSubtotal() { return subtotal; }
    public java.math.BigDecimal getIva() { return iva; }
    public String getSerie() { return serie; }
    public String getFolio() { return folio; }
    public String getUuid() { return uuid; }
    public java.time.LocalDateTime getFechaTimbrado() { return fechaTimbrado; }
    public java.math.BigDecimal getTotal() { return total; }
    public java.time.LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    // Getters explícitos adicionales para compatibilidad cuando Lombok no esté activo
    public String getEstado() { return estado; }
    public String getXmlContent() { return xmlContent; }
    public String getCadenaOriginal() { return cadenaOriginal; }
    public String getSelloDigital() { return selloDigital; }
    public String getCertificado() { return certificado; }
    public String getReceptorNombre() { return receptorNombre; }
    public String getReceptorPaterno() { return receptorPaterno; }
    public String getReceptorMaterno() { return receptorMaterno; }
    public String getReceptorPais() { return receptorPais; }
    public String getReceptorDomicilioFiscal() { return receptorDomicilioFiscal; }
    public String getReceptorRegimenFiscal() { return receptorRegimenFiscal; }
    // Getter faltante para compatibilidad
    public String getEstadoDescripcion() { return estadoDescripcion; }
    public String getReceptorUsoCfdi() { return receptorUsoCfdi; }

    // Explicit getters needed by controller
    public String getTienda() { return tienda; }
    public Integer getTiendaOrigen() { return tiendaOrigen; }
    public String getTerminal() { return terminal; }
    public Integer getTerminalBol() { return terminalBol; }
    public String getBoleta() { return boleta; }
    public Integer getBoletaBol() { return boletaBol; }

    // Setters explícitos para compatibilidad cuando Lombok no está activo
    public void setUuid(String uuid) { this.uuid = uuid; }
    public void setSerie(String serie) { this.serie = serie; }
    public void setFolio(String folio) { this.folio = folio; }
    public void setEmisorRfc(String emisorRfc) { this.emisorRfc = emisorRfc; }
    public void setReceptorRfc(String receptorRfc) { this.receptorRfc = receptorRfc; }
    public void setIdReceptor(Long idReceptor) { this.idReceptor = idReceptor; }
    public void setFechaTimbrado(java.time.LocalDateTime fechaTimbrado) { this.fechaTimbrado = fechaTimbrado; }
    public void setTotal(java.math.BigDecimal total) { this.total = total; }
    public void setXmlContent(String xmlContent) { this.xmlContent = xmlContent; }
    public void setCadenaOriginal(String cadenaOriginal) { this.cadenaOriginal = cadenaOriginal; }
    public void setSelloDigital(String selloDigital) { this.selloDigital = selloDigital; }
    public void setCertificado(String certificado) { this.certificado = certificado; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setEstadoDescripcion(String estadoDescripcion) { this.estadoDescripcion = estadoDescripcion; }
    
    // Manual builder to replace Lombok builder when annotation processing fails
    public static FacturaBuilder builder() {
        return new FacturaBuilder();
    }
    
    public static class FacturaBuilder {
        private final Factura f = new Factura();
    
        public FacturaBuilder uuid(String uuid) { f.uuid = uuid; return this; }
        public FacturaBuilder xmlContent(String xmlContent) { f.xmlContent = xmlContent; return this; }
        public FacturaBuilder fechaGeneracion(java.time.LocalDateTime fechaGeneracion) { f.fechaGeneracion = fechaGeneracion; return this; }
        public FacturaBuilder fechaTimbrado(java.time.LocalDateTime fechaTimbrado) { f.fechaTimbrado = fechaTimbrado; return this; }
    
        public FacturaBuilder emisorRfc(String emisorRfc) { f.emisorRfc = emisorRfc; return this; }
        public FacturaBuilder emisorRazonSocial(String emisorRazonSocial) { f.emisorRazonSocial = emisorRazonSocial; return this; }
        public FacturaBuilder emisorNombre(String emisorNombre) { f.emisorNombre = emisorNombre; return this; }
        public FacturaBuilder emisorPaterno(String emisorPaterno) { f.emisorPaterno = emisorPaterno; return this; }
        public FacturaBuilder emisorMaterno(String emisorMaterno) { f.emisorMaterno = emisorMaterno; return this; }
        public FacturaBuilder emisorCorreo(String emisorCorreo) { f.emisorCorreo = emisorCorreo; return this; }
        public FacturaBuilder emisorPais(String emisorPais) { f.emisorPais = emisorPais; return this; }
        public FacturaBuilder emisorDomicilioFiscal(String emisorDomicilioFiscal) { f.emisorDomicilioFiscal = emisorDomicilioFiscal; return this; }
        public FacturaBuilder emisorRegimenFiscal(String emisorRegimenFiscal) { f.emisorRegimenFiscal = emisorRegimenFiscal; return this; }
    
        public FacturaBuilder receptorRfc(String receptorRfc) { f.receptorRfc = receptorRfc; return this; }
        public FacturaBuilder receptorRazonSocial(String receptorRazonSocial) { f.receptorRazonSocial = receptorRazonSocial; return this; }
        public FacturaBuilder idReceptor(Long idReceptor) { f.idReceptor = idReceptor; return this; }
        public FacturaBuilder receptorNombre(String receptorNombre) { f.receptorNombre = receptorNombre; return this; }
        public FacturaBuilder receptorPaterno(String receptorPaterno) { f.receptorPaterno = receptorPaterno; return this; }
        public FacturaBuilder receptorMaterno(String receptorMaterno) { f.receptorMaterno = receptorMaterno; return this; }
        public FacturaBuilder receptorCorreo(String receptorCorreo) { f.receptorCorreo = receptorCorreo; return this; }
        public FacturaBuilder receptorPais(String receptorPais) { f.receptorPais = receptorPais; return this; }
        public FacturaBuilder receptorDomicilioFiscal(String receptorDomicilioFiscal) { f.receptorDomicilioFiscal = receptorDomicilioFiscal; return this; }
        public FacturaBuilder receptorRegimenFiscal(String receptorRegimenFiscal) { f.receptorRegimenFiscal = receptorRegimenFiscal; return this; }
        public FacturaBuilder receptorUsoCfdi(String receptorUsoCfdi) { f.receptorUsoCfdi = receptorUsoCfdi; return this; }
    
        public FacturaBuilder codigoFacturacion(String codigoFacturacion) { f.codigoFacturacion = codigoFacturacion; return this; }
        public FacturaBuilder tienda(String tienda) { f.tienda = tienda; return this; }
        public FacturaBuilder fechaFactura(java.time.LocalDateTime fechaFactura) { f.fechaFactura = fechaFactura; return this; }
        public FacturaBuilder terminal(String terminal) { f.terminal = terminal; return this; }
        public FacturaBuilder boleta(String boleta) { f.boleta = boleta; return this; }
        public FacturaBuilder medioPago(String medioPago) { f.medioPago = medioPago; return this; }
        public FacturaBuilder formaPago(String formaPago) { f.formaPago = formaPago; return this; }
        public FacturaBuilder iepsDesglosado(Boolean iepsDesglosado) { f.iepsDesglosado = iepsDesglosado; return this; }
    
        public FacturaBuilder subtotal(java.math.BigDecimal subtotal) { f.subtotal = subtotal; return this; }
        public FacturaBuilder iva(java.math.BigDecimal iva) { f.iva = iva; return this; }
        public FacturaBuilder ieps(java.math.BigDecimal ieps) { f.ieps = ieps; return this; }
        public FacturaBuilder total(java.math.BigDecimal total) { f.total = total; return this; }
    
        public FacturaBuilder estado(String estado) { f.estado = estado; return this; }
        public FacturaBuilder estadoDescripcion(String estadoDescripcion) { f.estadoDescripcion = estadoDescripcion; return this; }
        public FacturaBuilder serie(String serie) { f.serie = serie; return this; }
        public FacturaBuilder folio(String folio) { f.folio = folio; return this; }
        public FacturaBuilder cadenaOriginal(String cadenaOriginal) { f.cadenaOriginal = cadenaOriginal; return this; }
        public FacturaBuilder selloDigital(String selloDigital) { f.selloDigital = selloDigital; return this; }
        public FacturaBuilder certificado(String certificado) { f.certificado = certificado; return this; }
        public FacturaBuilder tiendaOrigen(Integer tiendaOrigen) { f.tiendaOrigen = tiendaOrigen; return this; }
        public FacturaBuilder terminalBol(Integer terminalBol) { f.terminalBol = terminalBol; return this; }
        public FacturaBuilder boletaBol(Integer boletaBol) { f.boletaBol = boletaBol; return this; }
    
        public FacturaBuilder fechaCreacion(java.time.LocalDateTime fechaCreacion) { f.fechaCreacion = fechaCreacion; return this; }
        public FacturaBuilder fechaModificacion(java.time.LocalDateTime fechaModificacion) { f.fechaModificacion = fechaModificacion; return this; }
    
        public Factura build() { return f; }
    }
}