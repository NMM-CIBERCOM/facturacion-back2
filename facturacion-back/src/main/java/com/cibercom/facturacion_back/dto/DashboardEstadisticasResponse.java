package com.cibercom.facturacion_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardEstadisticasResponse {
    private boolean exitoso;
    private String mensaje;
    private EstadisticasRapidas estadisticasRapidas;
    private List<DatoGrafico> datosGrafico;
    private List<FacturaResumen> ultimasFacturas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadisticasRapidas {
        private Long facturasHoy;
        private Long facturasMes;
        private Long facturasAnio;
        private BigDecimal ingresosHoy;
        private BigDecimal ingresosMes;
        private BigDecimal ingresosAnio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatoGrafico {
        private String mes; // "Ene", "Feb", etc.
        private Long facturas;
        private Long boletas;
        private Long notas;
        private Long tickets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturaResumen {
        private String uuid;
        private String serie;
        private String folio;
        private String receptorRazonSocial;
        private BigDecimal total;
        private java.time.LocalDateTime fechaFactura;
    }
}

