package com.cibercom.facturacion_back.dto;

import lombok.Data;

import java.util.List;

@Data
public class PagoComplementoEnvioRequest {
    private String uuidComplemento;
    private String facturaUuid;
    private String correoReceptor;
    private String serieComplemento;
    private String folioComplemento;
    private String fechaTimbrado;
    private String rfcReceptor;
    private String rfcEmisor;
    private String nombreReceptor;
    private String nombreEmisor;
    private String metodoCfdi;
    private String formaCfdi;
    private String totalPagado;
    private String moneda;
    private List<PagoDetalleRequest> pagos;
}

