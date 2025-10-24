package com.cibercom.facturacion_back.dto;

// Lombok annotations retained, but manual code added for reliability
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsultaEstatusResponse {
    private boolean exitoso;
    private String mensaje;
    private String codigoEstatus;
    private String estado;
    private String esCancelable;
    private String estatusCancelacion;

    // Explicit getters/setters to avoid Lombok dependency
    public boolean isExitoso() { return exitoso; }
    public void setExitoso(boolean exitoso) { this.exitoso = exitoso; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getCodigoEstatus() { return codigoEstatus; }
    public void setCodigoEstatus(String codigoEstatus) { this.codigoEstatus = codigoEstatus; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEsCancelable() { return esCancelable; }
    public void setEsCancelable(String esCancelable) { this.esCancelable = esCancelable; }

    public String getEstatusCancelacion() { return estatusCancelacion; }
    public void setEstatusCancelacion(String estatusCancelacion) { this.estatusCancelacion = estatusCancelacion; }

    // Manual Builder implementation to replace Lombok builder if processing fails
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private boolean exitoso;
        private String mensaje;
        private String codigoEstatus;
        private String estado;
        private String esCancelable;
        private String estatusCancelacion;

        public Builder exitoso(boolean exitoso) { this.exitoso = exitoso; return this; }
        public Builder mensaje(String mensaje) { this.mensaje = mensaje; return this; }
        public Builder codigoEstatus(String codigoEstatus) { this.codigoEstatus = codigoEstatus; return this; }
        public Builder estado(String estado) { this.estado = estado; return this; }
        public Builder esCancelable(String esCancelable) { this.esCancelable = esCancelable; return this; }
        public Builder estatusCancelacion(String estatusCancelacion) { this.estatusCancelacion = estatusCancelacion; return this; }

        public ConsultaEstatusResponse build() {
            ConsultaEstatusResponse r = new ConsultaEstatusResponse();
            r.setExitoso(this.exitoso);
            r.setMensaje(this.mensaje);
            r.setCodigoEstatus(this.codigoEstatus);
            r.setEstado(this.estado);
            r.setEsCancelable(this.esCancelable);
            r.setEstatusCancelacion(this.estatusCancelacion);
            return r;
        }
    }

    public static ConsultaEstatusResponse exito(String codigoEstatus, String estado,
                                                String esCancelable, String estatusCancelacion) {
        return ConsultaEstatusResponse.builder()
                .exitoso(true)
                .mensaje("Consulta de estatus exitosa")
                .codigoEstatus(codigoEstatus)
                .estado(estado)
                .esCancelable(esCancelable)
                .estatusCancelacion(estatusCancelacion)
                .build();
    }

    public static ConsultaEstatusResponse error(String mensaje) {
        return ConsultaEstatusResponse.builder()
                .exitoso(false)
                .mensaje(mensaje)
                .build();
    }
}