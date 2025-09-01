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
    private String status; // TIMBRADA, EN_PROCESO, RECHAZADA, ERROR
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
}
