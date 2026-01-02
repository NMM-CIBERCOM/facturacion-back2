package com.cibercom.facturacion_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaSustituidaResponse {
    private boolean exitoso;
    private String mensaje;
    private List<FacturaSustituida> facturas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturaSustituida {
        private String uuid;
        private String uuidOrig;
        private String serie;
        private String folio;
        private String receptorRazonSocial;
        private String receptorRfc;
        private BigDecimal total;
        private LocalDateTime fechaFactura;
        private String serieOrig;
        private String folioOrig;
    }
}

