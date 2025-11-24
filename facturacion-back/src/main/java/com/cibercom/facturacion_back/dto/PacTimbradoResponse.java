package com.cibercom.facturacion_back.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacTimbradoResponse {
    private Boolean ok;
    private String status;
    private String uuid;
    private String xmlTimbrado;
    private String cadenaOriginal;
    private String selloDigital;
    private String certificado;
    private String folioFiscal;
    private String serie;
    private String folio;
    private LocalDateTime fechaTimbrado;
    private String message;
    private String receiptId;
    private String qrCode;
    // Campos adicionales de Finkok
    private String codEstatus;
    private String satSeal;
    private String noCertificadoSAT;
    private String fecha;
    private String rfcEmisor;
    private String fechaRegistro;
    private String codigoError;
    private String mensajeIncidencia;
    private String idIncidencia;
    private String workProcessId;
    private String extraInfo;
    private String noCertificadoPac;
    
    // Métodos explícitos para compatibilidad
    public void setOk(Boolean ok) { this.ok = ok; }
    public void setStatus(String status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public void setXmlTimbrado(String xmlTimbrado) { this.xmlTimbrado = xmlTimbrado; }
    public void setCadenaOriginal(String cadenaOriginal) { this.cadenaOriginal = cadenaOriginal; }
    public void setSelloDigital(String selloDigital) { this.selloDigital = selloDigital; }
    public void setCertificado(String certificado) { this.certificado = certificado; }
    public void setFolioFiscal(String folioFiscal) { this.folioFiscal = folioFiscal; }
    public void setSerie(String serie) { this.serie = serie; }
    public void setFolio(String folio) { this.folio = folio; }
    public void setFechaTimbrado(LocalDateTime fechaTimbrado) { this.fechaTimbrado = fechaTimbrado; }
    public void setReceiptId(String receiptId) { this.receiptId = receiptId; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public void setCodEstatus(String codEstatus) { this.codEstatus = codEstatus; }
    public void setSatSeal(String satSeal) { this.satSeal = satSeal; }
    public void setNoCertificadoSAT(String noCertificadoSAT) { this.noCertificadoSAT = noCertificadoSAT; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public void setRfcEmisor(String rfcEmisor) { this.rfcEmisor = rfcEmisor; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public void setCodigoError(String codigoError) { this.codigoError = codigoError; }
    public void setMensajeIncidencia(String mensajeIncidencia) { this.mensajeIncidencia = mensajeIncidencia; }
    public void setIdIncidencia(String idIncidencia) { this.idIncidencia = idIncidencia; }
    public void setWorkProcessId(String workProcessId) { this.workProcessId = workProcessId; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }
    public void setNoCertificadoPac(String noCertificadoPac) { this.noCertificadoPac = noCertificadoPac; }

    public Boolean getOk() { return ok; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getUuid() { return uuid; }
    public String getXmlTimbrado() { return xmlTimbrado; }
    public String getCadenaOriginal() { return cadenaOriginal; }
    public String getSelloDigital() { return selloDigital; }
    public String getCertificado() { return certificado; }
    public String getFolioFiscal() { return folioFiscal; }
    public String getSerie() { return serie; }
    public String getFolio() { return folio; }
    public LocalDateTime getFechaTimbrado() { return fechaTimbrado; }
    public String getReceiptId() { return receiptId; }
    public String getQrCode() { return qrCode; }
    public String getCodEstatus() { return codEstatus; }
    public String getSatSeal() { return satSeal; }
    public String getNoCertificadoSAT() { return noCertificadoSAT; }
    public String getFecha() { return fecha; }
    public String getRfcEmisor() { return rfcEmisor; }
    public String getFechaRegistro() { return fechaRegistro; }
    public String getCodigoError() { return codigoError; }
    public String getMensajeIncidencia() { return mensajeIncidencia; }
    public String getIdIncidencia() { return idIncidencia; }
    public String getWorkProcessId() { return workProcessId; }
    public String getExtraInfo() { return extraInfo; }
    public String getNoCertificadoPac() { return noCertificadoPac; }

    // Builder manual para compatibilidad cuando Lombok no esté activo
    public static PacTimbradoResponseBuilder builder() { return new PacTimbradoResponseBuilder(); }
    public static class PacTimbradoResponseBuilder {
        private final PacTimbradoResponse r = new PacTimbradoResponse();
        public PacTimbradoResponseBuilder ok(Boolean ok) { r.ok = ok; return this; }
        public PacTimbradoResponseBuilder status(String status) { r.status = status; return this; }
        public PacTimbradoResponseBuilder uuid(String uuid) { r.uuid = uuid; return this; }
        public PacTimbradoResponseBuilder xmlTimbrado(String xmlTimbrado) { r.xmlTimbrado = xmlTimbrado; return this; }
        public PacTimbradoResponseBuilder cadenaOriginal(String cadenaOriginal) { r.cadenaOriginal = cadenaOriginal; return this; }
        public PacTimbradoResponseBuilder selloDigital(String selloDigital) { r.selloDigital = selloDigital; return this; }
        public PacTimbradoResponseBuilder certificado(String certificado) { r.certificado = certificado; return this; }
        public PacTimbradoResponseBuilder folioFiscal(String folioFiscal) { r.folioFiscal = folioFiscal; return this; }
        public PacTimbradoResponseBuilder serie(String serie) { r.serie = serie; return this; }
        public PacTimbradoResponseBuilder folio(String folio) { r.folio = folio; return this; }
        public PacTimbradoResponseBuilder fechaTimbrado(LocalDateTime fechaTimbrado) { r.fechaTimbrado = fechaTimbrado; return this; }
        public PacTimbradoResponseBuilder message(String message) { r.message = message; return this; }
        public PacTimbradoResponseBuilder receiptId(String receiptId) { r.receiptId = receiptId; return this; }
        public PacTimbradoResponseBuilder qrCode(String qrCode) { r.qrCode = qrCode; return this; }
        public PacTimbradoResponseBuilder codEstatus(String codEstatus) { r.codEstatus = codEstatus; return this; }
        public PacTimbradoResponseBuilder satSeal(String satSeal) { r.satSeal = satSeal; return this; }
        public PacTimbradoResponseBuilder noCertificadoSAT(String noCertificadoSAT) { r.noCertificadoSAT = noCertificadoSAT; return this; }
        public PacTimbradoResponseBuilder fecha(String fecha) { r.fecha = fecha; return this; }
        public PacTimbradoResponseBuilder rfcEmisor(String rfcEmisor) { r.rfcEmisor = rfcEmisor; return this; }
        public PacTimbradoResponseBuilder fechaRegistro(String fechaRegistro) { r.fechaRegistro = fechaRegistro; return this; }
        public PacTimbradoResponseBuilder codigoError(String codigoError) { r.codigoError = codigoError; return this; }
        public PacTimbradoResponseBuilder mensajeIncidencia(String mensajeIncidencia) { r.mensajeIncidencia = mensajeIncidencia; return this; }
        public PacTimbradoResponseBuilder idIncidencia(String idIncidencia) { r.idIncidencia = idIncidencia; return this; }
        public PacTimbradoResponseBuilder workProcessId(String workProcessId) { r.workProcessId = workProcessId; return this; }
        public PacTimbradoResponseBuilder extraInfo(String extraInfo) { r.extraInfo = extraInfo; return this; }
        public PacTimbradoResponseBuilder noCertificadoPac(String noCertificadoPac) { r.noCertificadoPac = noCertificadoPac; return this; }
        public PacTimbradoResponse build() { return r; }
    }
}
