package com.cibercom.facturacion_back.dto;

import lombok.Data;

import java.util.List;

@Data
public class PagoComplementoRequest {
    private String facturaUuid;
    private Long facturaId;
    private String usuarioRegistro;
    private String correoReceptor;
    private List<PagoDetalleRequest> pagos;
}

