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
    private String status;
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
    private String qrCode;
    
    // Métodos explícitos para compatibilidad
    public void setOk(Boolean ok) {
        this.ok = ok;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
