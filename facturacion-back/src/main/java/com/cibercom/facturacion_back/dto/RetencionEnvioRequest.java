package com.cibercom.facturacion_back.dto;

import lombok.Data;

@Data
public class RetencionEnvioRequest {
    private String uuidRetencion;
    private String correoReceptor;
    private String rfcReceptor;
    private String rfcEmisor;
    private String nombreReceptor;
    private String nombreEmisor;
    private String serieRetencion;
    private String folioRetencion;
    private String fechaTimbrado;
    private String tipoRetencion;
    private String montoRetenido;
    private String baseRetencion;
}

