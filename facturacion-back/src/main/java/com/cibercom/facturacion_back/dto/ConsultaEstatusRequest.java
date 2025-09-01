package com.cibercom.facturacion_back.dto;

import lombok.Data;

@Data
public class ConsultaEstatusRequest {
    private String username;
    private String password;
    private String uuid;
    private String rfcEmisor;
    private String rfcReceptor;
    private String total;
}