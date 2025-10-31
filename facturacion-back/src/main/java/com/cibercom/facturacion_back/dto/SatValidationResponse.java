package com.cibercom.facturacion_back.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SatValidationResponse {
    
    private boolean valido;
    private String mensaje;
    private LocalDateTime timestamp;
    private List<String> errores;
    private DatosValidados datosValidados;

    // Getters y setters explícitos para compatibilidad cuando Lombok no está activo
    public boolean isValido() { return valido; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<String> getErrores() { return errores; }
    public DatosValidados getDatosValidados() { return datosValidados; }

    public void setValido(boolean valido) { this.valido = valido; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setErrores(List<String> errores) { this.errores = errores; }
    public void setDatosValidados(DatosValidados datosValidados) { this.datosValidados = datosValidados; }

    // Builder manual cuando la anotación de Lombok no está activa
    public static SatValidationResponseBuilder builder() { return new SatValidationResponseBuilder(); }
    public static class SatValidationResponseBuilder {
        private final SatValidationResponse r = new SatValidationResponse();
        public SatValidationResponseBuilder valido(boolean valido) { r.valido = valido; return this; }
        public SatValidationResponseBuilder mensaje(String mensaje) { r.mensaje = mensaje; return this; }
        public SatValidationResponseBuilder timestamp(LocalDateTime timestamp) { r.timestamp = timestamp; return this; }
        public SatValidationResponseBuilder errores(List<String> errores) { r.errores = errores; return this; }
        public SatValidationResponseBuilder datosValidados(DatosValidados datosValidados) { r.datosValidados = datosValidados; return this; }
        public SatValidationResponse build() { return r; }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DatosValidados {
        private String nombre;
        private String rfc;
        private String codigoPostal;
        private String regimenFiscal;
        private String tipoPersona;

        // Getters y setters explícitos
        public String getNombre() { return nombre; }
        public String getRfc() { return rfc; }
        public String getCodigoPostal() { return codigoPostal; }
        public String getRegimenFiscal() { return regimenFiscal; }
        public String getTipoPersona() { return tipoPersona; }

        public void setNombre(String nombre) { this.nombre = nombre; }
        public void setRfc(String rfc) { this.rfc = rfc; }
        public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
        public void setRegimenFiscal(String regimenFiscal) { this.regimenFiscal = regimenFiscal; }
        public void setTipoPersona(String tipoPersona) { this.tipoPersona = tipoPersona; }

        // Builder manual para el inner class
        public static DatosValidadosBuilder builder() { return new DatosValidadosBuilder(); }
        public static class DatosValidadosBuilder {
            private final DatosValidados d = new DatosValidados();
            public DatosValidadosBuilder nombre(String nombre) { d.nombre = nombre; return this; }
            public DatosValidadosBuilder rfc(String rfc) { d.rfc = rfc; return this; }
            public DatosValidadosBuilder codigoPostal(String codigoPostal) { d.codigoPostal = codigoPostal; return this; }
            public DatosValidadosBuilder regimenFiscal(String regimenFiscal) { d.regimenFiscal = regimenFiscal; return this; }
            public DatosValidadosBuilder tipoPersona(String tipoPersona) { d.tipoPersona = tipoPersona; return this; }
            public DatosValidados build() { return d; }
        }
    }
}