package com.cibercom.facturacion_back.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PagoComplementoResponse {
    private boolean success;
    private String message;
    private Long facturaId;
    private int pagosInsertados;
    private List<String> errors;
    private String uuidComplemento;
    private String xmlTimbrado;
    private String serieComplemento;
    private String folioComplemento;
    private String fechaTimbrado;
    private BigDecimal totalPagado;
    private String correoReceptor;
    private String rfcReceptor;
    private String rfcEmisor;
}

