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
public class FacturasPorUsuarioResponse {
    private boolean exitoso;
    private String mensaje;
    private List<UsuarioFacturas> usuarios;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioFacturas {
        private String usuario;
        private String nombreUsuario;
        private Long totalFacturas;
        private Long totalNotasCredito;
        private BigDecimal totalImporte;
        private List<DocumentoFacturacion> documentos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentoFacturacion {
        private String uuid;
        private String tipo; // "FACTURA", "NOTA_CREDITO", etc.
        private String serie;
        private String folio;
        private String receptorRazonSocial;
        private String receptorRfc;
        private BigDecimal total;
        private LocalDateTime fechaFactura;
        private String estatusFacturacion;
        private String estatusSat;
    }
}

