package com.cibercom.facturacion_back.dto;

import java.math.BigDecimal;

public class FacturaLibreSaveRequest {
    
    // Datos fiscales del receptor
    private String rfc;
    private String correoElectronico;
    private String razonSocial;
    private String nombre;
    private String paterno;
    private String materno;
    private String pais;
    private String noRegistroIdentidadTributaria;
    private String domicilioFiscal;
    private String regimenFiscal;
    private String usoCfdi;
    
    // Datos del concepto
    private ConceptoData concepto;
    
    // Datos de pago
    private String medioPago;
    private String formaPago;
    
    // Totales
    private BigDecimal descuentoTotal;
    private BigDecimal subtotal;
    private BigDecimal ivaTotal;
    private BigDecimal iepsTotal;
    private BigDecimal total;
    private String ivaDesglosado;
    private String iepsDesglosado;
    
    // Boleta
    private String tienda;
    private String terminal;
    private String boleta;
    
    // Justificación
    private String justificacion;
    
    // Tipo documento
    private String tipoDocumento;
    private String uuid;
    private String emisor;
    private String fechaEmision;
    
    // Comentarios
    private String comentarios;
    
    // Clase interna para el concepto
    public static class ConceptoData {
        private String sku;
        private String unidadMedida;
        private String descripcion;
        private BigDecimal valorUnitario;
        private BigDecimal descuentoConcepto;
        private BigDecimal tasaIva;
        private BigDecimal iva;
        private BigDecimal tasaIeps;
        private BigDecimal ieps;
        private BigDecimal tasaIe;
        private BigDecimal ie;
        private String noPedimento;
        
        // Getters y Setters para ConceptoData
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        
        public String getUnidadMedida() { return unidadMedida; }
        public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }
        
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        
        public BigDecimal getValorUnitario() { return valorUnitario; }
        public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
        
        public BigDecimal getDescuentoConcepto() { return descuentoConcepto; }
        public void setDescuentoConcepto(BigDecimal descuentoConcepto) { this.descuentoConcepto = descuentoConcepto; }
        
        public BigDecimal getTasaIva() { return tasaIva; }
        public void setTasaIva(BigDecimal tasaIva) { this.tasaIva = tasaIva; }
        
        public BigDecimal getIva() { return iva; }
        public void setIva(BigDecimal iva) { this.iva = iva; }
        
        public BigDecimal getTasaIeps() { return tasaIeps; }
        public void setTasaIeps(BigDecimal tasaIeps) { this.tasaIeps = tasaIeps; }
        
        public BigDecimal getIeps() { return ieps; }
        public void setIeps(BigDecimal ieps) { this.ieps = ieps; }
        
        public BigDecimal getTasaIe() { return tasaIe; }
        public void setTasaIe(BigDecimal tasaIe) { this.tasaIe = tasaIe; }
        
        public BigDecimal getIe() { return ie; }
        public void setIe(BigDecimal ie) { this.ie = ie; }
        
        public String getNoPedimento() { return noPedimento; }
        public void setNoPedimento(String noPedimento) { this.noPedimento = noPedimento; }
    }
    
    // Getters y Setters
    public String getRfc() { return rfc; }
    public void setRfc(String rfc) { this.rfc = rfc; }
    
    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }
    
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getPaterno() { return paterno; }
    public void setPaterno(String paterno) { this.paterno = paterno; }
    
    public String getMaterno() { return materno; }
    public void setMaterno(String materno) { this.materno = materno; }
    
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    
    public String getNoRegistroIdentidadTributaria() { return noRegistroIdentidadTributaria; }
    public void setNoRegistroIdentidadTributaria(String noRegistroIdentidadTributaria) { this.noRegistroIdentidadTributaria = noRegistroIdentidadTributaria; }
    
    public String getDomicilioFiscal() { return domicilioFiscal; }
    public void setDomicilioFiscal(String domicilioFiscal) { this.domicilioFiscal = domicilioFiscal; }
    
    public String getRegimenFiscal() { return regimenFiscal; }
    public void setRegimenFiscal(String regimenFiscal) { this.regimenFiscal = regimenFiscal; }
    
    public String getUsoCfdi() { return usoCfdi; }
    public void setUsoCfdi(String usoCfdi) { this.usoCfdi = usoCfdi; }
    
    public String getTienda() { return tienda; }
    public void setTienda(String tienda) { this.tienda = tienda; }
    
    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }
    
    public String getBoleta() { return boleta; }
    public void setBoleta(String boleta) { this.boleta = boleta; }
    
    public String getJustificacion() { return justificacion; }
    public void setJustificacion(String justificacion) { this.justificacion = justificacion; }
    
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    
    public String getEmisor() { return emisor; }
    public void setEmisor(String emisor) { this.emisor = emisor; }
    
    public String getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(String fechaEmision) { this.fechaEmision = fechaEmision; }
    
    public ConceptoData getConcepto() { return concepto; }
    public void setConcepto(ConceptoData concepto) { this.concepto = concepto; }
    
    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }
    
    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
    
    public BigDecimal getDescuentoTotal() { return descuentoTotal; }
    public void setDescuentoTotal(BigDecimal descuentoTotal) { this.descuentoTotal = descuentoTotal; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getIvaTotal() { return ivaTotal; }
    public void setIvaTotal(BigDecimal ivaTotal) { this.ivaTotal = ivaTotal; }
    
    public BigDecimal getIepsTotal() { return iepsTotal; }
    public void setIepsTotal(BigDecimal iepsTotal) { this.iepsTotal = iepsTotal; }
    
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    
    public String getIvaDesglosado() { return ivaDesglosado; }
    public void setIvaDesglosado(String ivaDesglosado) { this.ivaDesglosado = ivaDesglosado; }
    
    public String getIepsDesglosado() { return iepsDesglosado; }
    public void setIepsDesglosado(String iepsDesglosado) { this.iepsDesglosado = iepsDesglosado; }
    
    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }
}