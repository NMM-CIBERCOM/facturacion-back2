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
    }
    
    // Métodos explícitos para compatibilidad
    public boolean isValido() {
        return valido;
    }
    
    public void setValido(boolean valido) {
        this.valido = valido;
    }
    
    public List<String> getErrores() {
        return errores;
    }
    
    public void setErrores(List<String> errores) {
        this.errores = errores;
    }
} 