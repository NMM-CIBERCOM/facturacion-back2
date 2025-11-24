package com.cibercom.facturacion_back.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PagoDetalleRequest {
    private String fechaPago;
    private String formaPago;
    private String moneda;
    private BigDecimal monto;
    private String uuid;
}

