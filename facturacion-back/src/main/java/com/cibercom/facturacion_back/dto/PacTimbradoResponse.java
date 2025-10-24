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
        public PacTimbradoResponse build() { return r; }
    }
}
