package com.cibercom.facturacion_back.dto;

import lombok.Data;

@Data
public class PagoFacturaLookupResponse {
    private boolean success;
    private String message;
    private Long facturaId;
    private String uuid;
}

