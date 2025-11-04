package com.cibercom.facturacion_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ConsultaFacturaResponse {

    private boolean exitoso;
    private String mensaje;
    private LocalDateTime timestamp;
    private List<FacturaConsultaDTO> facturas;
    private int totalFacturas;
    private String error;

    public ConsultaFacturaResponse() {
    }

    public static ConsultaFacturaResponse exito(List<FacturaConsultaDTO> facturas) {
        ConsultaFacturaResponse response = new ConsultaFacturaResponse();
        response.exitoso = true;
        response.mensaje = "Consulta realizada exitosamente";
        response.timestamp = LocalDateTime.now();
        response.facturas = facturas;
        response.totalFacturas = facturas != null ? facturas.size() : 0;
        return response;
    }

    public static ConsultaFacturaResponse error(String mensaje) {
        ConsultaFacturaResponse response = new ConsultaFacturaResponse();
        response.exitoso = false;
        response.mensaje = mensaje;
        response.timestamp = LocalDateTime.now();
        response.error = mensaje;
        return response;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public void setExitoso(boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<FacturaConsultaDTO> getFacturas() {
        return facturas;
    }

    public void setFacturas(List<FacturaConsultaDTO> facturas) {
        this.facturas = facturas;
    }

    public int getTotalFacturas() {
        return totalFacturas;
    }

    public void setTotalFacturas(int totalFacturas) {
        this.totalFacturas = totalFacturas;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public static class FacturaConsultaDTO {
        private String uuid;
        private String rfcEmisor;
        private String rfcReceptor;
        private String serie;
        private String folio;
        private LocalDate fechaEmision;
        private BigDecimal importe;
        private String estatusFacturacion;
        private String estatusSat;
        private String tienda;
        private String almacen;
        private String usuario;
        private boolean permiteCancelacion;
        private String motivoNoCancelacion;

        public FacturaConsultaDTO() {
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getRfcEmisor() {
            return rfcEmisor;
        }

        public void setRfcEmisor(String rfcEmisor) {
            this.rfcEmisor = rfcEmisor;
        }

        public String getRfcReceptor() {
            return rfcReceptor;
        }

        public void setRfcReceptor(String rfcReceptor) {
            this.rfcReceptor = rfcReceptor;
        }

        public String getSerie() {
            return serie;
        }

        public void setSerie(String serie) {
            this.serie = serie;
        }

        public String getFolio() {
            return folio;
        }

        public void setFolio(String folio) {
            this.folio = folio;
        }

        public LocalDate getFechaEmision() {
            return fechaEmision;
        }

        public void setFechaEmision(LocalDate fechaEmision) {
            this.fechaEmision = fechaEmision;
        }

        public BigDecimal getImporte() {
            return importe;
        }

        public void setImporte(BigDecimal importe) {
            this.importe = importe;
        }

        public String getEstatusFacturacion() {
            return estatusFacturacion;
        }

        public void setEstatusFacturacion(String estatusFacturacion) {
            this.estatusFacturacion = estatusFacturacion;
        }

        public String getEstatusSat() {
            return estatusSat;
        }

        public void setEstatusSat(String estatusSat) {
            this.estatusSat = estatusSat;
        }

        public String getTienda() {
            return tienda;
        }

        public void setTienda(String tienda) {
            this.tienda = tienda;
        }

        public String getAlmacen() {
            return almacen;
        }

        public void setAlmacen(String almacen) {
            this.almacen = almacen;
        }

        public String getUsuario() {
            return usuario;
        }

        public void setUsuario(String usuario) {
            this.usuario = usuario;
        }

        public boolean isPermiteCancelacion() {
            return permiteCancelacion;
        }

        public void setPermiteCancelacion(boolean permiteCancelacion) {
            this.permiteCancelacion = permiteCancelacion;
        }

        public String getMotivoNoCancelacion() {
            return motivoNoCancelacion;
        }

        public void setMotivoNoCancelacion(String motivoNoCancelacion) {
            this.motivoNoCancelacion = motivoNoCancelacion;
        }
    }
}



