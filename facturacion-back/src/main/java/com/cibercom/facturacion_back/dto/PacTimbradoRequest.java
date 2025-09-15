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
}
