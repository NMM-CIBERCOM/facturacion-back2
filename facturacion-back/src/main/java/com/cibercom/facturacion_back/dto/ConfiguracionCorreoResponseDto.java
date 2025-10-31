package com.cibercom.facturacion_back.dto;

import java.util.List;

public class ConfiguracionCorreoResponseDto {
    private boolean exitoso;
    private String mensaje;
    private String mensajeSeleccionado;
    private List<MensajePredefinidoDto> mensajesPersonalizados;
    private String rfcReceptor;
    private FormatoCorreoDto formatoCorreo;

    // Constructor por defecto
    public ConfiguracionCorreoResponseDto() {
    }

    // Constructor con todos los parámetros
    public ConfiguracionCorreoResponseDto(boolean exitoso, String mensaje, String mensajeSeleccionado, 
                                        List<MensajePredefinidoDto> mensajesPersonalizados, 
                                        String rfcReceptor, FormatoCorreoDto formatoCorreo) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
        this.mensajeSeleccionado = mensajeSeleccionado;
        this.mensajesPersonalizados = mensajesPersonalizados;
        this.rfcReceptor = rfcReceptor;
        this.formatoCorreo = formatoCorreo;
    }
    public String getRfcReceptor() {
        return rfcReceptor;
    }
    public void setRfcReceptor(String rfcReceptor) {
        this.rfcReceptor = rfcReceptor;
    }
    
    // Métodos explícitos para compatibilidad
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
    
    public String getMensajeSeleccionado() {
        return mensajeSeleccionado;
    }
    
    public void setMensajeSeleccionado(String mensajeSeleccionado) {
        this.mensajeSeleccionado = mensajeSeleccionado;
    }
    
    public List<MensajePredefinidoDto> getMensajesPersonalizados() {
        return mensajesPersonalizados;
    }
    
    public void setMensajesPersonalizados(List<MensajePredefinidoDto> mensajesPersonalizados) {
        this.mensajesPersonalizados = mensajesPersonalizados;
    }
    
    public FormatoCorreoDto getFormatoCorreo() {
        return formatoCorreo;
    }
    
    public void setFormatoCorreo(FormatoCorreoDto formatoCorreo) {
        this.formatoCorreo = formatoCorreo;
    }
    
    // Método builder explícito para compatibilidad
    public static ConfiguracionCorreoResponseDtoBuilder builder() {
        return new ConfiguracionCorreoResponseDtoBuilder();
    }
    
    public static class ConfiguracionCorreoResponseDtoBuilder {
        private boolean exitoso;
        private String mensaje;
        private String mensajeSeleccionado;
        private List<MensajePredefinidoDto> mensajesPersonalizados;
        private String rfcReceptor;
        private FormatoCorreoDto formatoCorreo;
        
        public ConfiguracionCorreoResponseDtoBuilder exitoso(boolean exitoso) {
            this.exitoso = exitoso;
            return this;
        }
        
        public ConfiguracionCorreoResponseDtoBuilder mensaje(String mensaje) {
            this.mensaje = mensaje;
            return this;
        }
        
        public ConfiguracionCorreoResponseDtoBuilder mensajeSeleccionado(String mensajeSeleccionado) {
            this.mensajeSeleccionado = mensajeSeleccionado;
            return this;
        }
        
        public ConfiguracionCorreoResponseDtoBuilder mensajesPersonalizados(List<MensajePredefinidoDto> mensajesPersonalizados) {
            this.mensajesPersonalizados = mensajesPersonalizados;
            return this;
        }
        
        public ConfiguracionCorreoResponseDtoBuilder rfcReceptor(String rfcReceptor) {
            this.rfcReceptor = rfcReceptor;
            return this;
        }
        
        public ConfiguracionCorreoResponseDtoBuilder formatoCorreo(FormatoCorreoDto formatoCorreo) {
            this.formatoCorreo = formatoCorreo;
            return this;
        }
        
        public ConfiguracionCorreoResponseDto build() {
            return new ConfiguracionCorreoResponseDto(exitoso, mensaje, mensajeSeleccionado, mensajesPersonalizados, rfcReceptor, formatoCorreo);
        }
    }
}