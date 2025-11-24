package com.cibercom.facturacion_back.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacTimbradoRequest {
    private String uuid;
    private String xmlContent;
    private String rfcEmisor;
    private String rfcReceptor;
    private Double total;
    private String tipo;
    private String fechaFactura;
    private Boolean publicoGeneral;
    private String serie;
    private String folio;
    private String tienda;
    private String terminal;
    private String boleta;
    private String medioPago;
    private String formaPago;
    private String usoCFDI;
    private String regimenFiscalEmisor;
    private String regimenFiscalReceptor;
    // NUEVO: UUIDs de CFDIs relacionados (separados por coma)
    private String relacionadosUuids;
    // Credenciales de Finkok (opcionales, se pueden obtener de configuración)
    private String username;
    private String password;

    // Getters explícitos para compatibilidad cuando Lombok falle
    public String getUuid() { return uuid; }
    public String getXmlContent() { return xmlContent; }
    public String getRfcEmisor() { return rfcEmisor; }
    public String getRfcReceptor() { return rfcReceptor; }
    public Double getTotal() { return total; }
    public String getTipo() { return tipo; }
    public String getFechaFactura() { return fechaFactura; }
    public Boolean getPublicoGeneral() { return publicoGeneral; }
    public String getSerie() { return serie; }
    public String getFolio() { return folio; }
    public String getTienda() { return tienda; }
    public String getTerminal() { return terminal; }
    public String getBoleta() { return boleta; }
    public String getMedioPago() { return medioPago; }
    public String getFormaPago() { return formaPago; }
    public String getUsoCFDI() { return usoCFDI; }
    public String getRegimenFiscalEmisor() { return regimenFiscalEmisor; }
    public String getRegimenFiscalReceptor() { return regimenFiscalReceptor; }
    public String getRelacionadosUuids() { return relacionadosUuids; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    // Builder manual para compatibilidad cuando Lombok no esté activo
    public static PacTimbradoRequestBuilder builder() { return new PacTimbradoRequestBuilder(); }
    public static class PacTimbradoRequestBuilder {
        private final PacTimbradoRequest r = new PacTimbradoRequest();
        public PacTimbradoRequestBuilder uuid(String uuid) { r.uuid = uuid; return this; }
        public PacTimbradoRequestBuilder xmlContent(String xmlContent) { r.xmlContent = xmlContent; return this; }
        public PacTimbradoRequestBuilder rfcEmisor(String rfcEmisor) { r.rfcEmisor = rfcEmisor; return this; }
        public PacTimbradoRequestBuilder rfcReceptor(String rfcReceptor) { r.rfcReceptor = rfcReceptor; return this; }
        public PacTimbradoRequestBuilder total(Double total) { r.total = total; return this; }
        public PacTimbradoRequestBuilder tipo(String tipo) { r.tipo = tipo; return this; }
        public PacTimbradoRequestBuilder fechaFactura(String fechaFactura) { r.fechaFactura = fechaFactura; return this; }
        public PacTimbradoRequestBuilder publicoGeneral(Boolean publicoGeneral) { r.publicoGeneral = publicoGeneral; return this; }
        public PacTimbradoRequestBuilder serie(String serie) { r.serie = serie; return this; }
        public PacTimbradoRequestBuilder folio(String folio) { r.folio = folio; return this; }
        public PacTimbradoRequestBuilder tienda(String tienda) { r.tienda = tienda; return this; }
        public PacTimbradoRequestBuilder terminal(String terminal) { r.terminal = terminal; return this; }
        public PacTimbradoRequestBuilder boleta(String boleta) { r.boleta = boleta; return this; }
        public PacTimbradoRequestBuilder medioPago(String medioPago) { r.medioPago = medioPago; return this; }
        public PacTimbradoRequestBuilder formaPago(String formaPago) { r.formaPago = formaPago; return this; }
        public PacTimbradoRequestBuilder usoCFDI(String usoCFDI) { r.usoCFDI = usoCFDI; return this; }
        public PacTimbradoRequestBuilder regimenFiscalEmisor(String regimenFiscalEmisor) { r.regimenFiscalEmisor = regimenFiscalEmisor; return this; }
        public PacTimbradoRequestBuilder regimenFiscalReceptor(String regimenFiscalReceptor) { r.regimenFiscalReceptor = regimenFiscalReceptor; return this; }
        public PacTimbradoRequestBuilder relacionadosUuids(String relacionadosUuids) { r.relacionadosUuids = relacionadosUuids; return this; }
        public PacTimbradoRequestBuilder username(String username) { r.username = username; return this; }
        public PacTimbradoRequestBuilder password(String password) { r.password = password; return this; }
        public PacTimbradoRequest build() { return r; }
    }
}
