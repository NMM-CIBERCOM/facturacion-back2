package com.cibercom.facturacion_back.dto;

// Lombok annotations retained, but manual code added for reliability
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionCorreoResponseDto {
    private boolean exitoso;
    private String mensaje;
    private String mensajeSeleccionado;
    private List<MensajePredefinidoDto> mensajesPersonalizados;
    private String rfcReceptor;
    private FormatoCorreoDto formatoCorreo;

    // Explicit getters/setters to avoid Lombok dependency
    public boolean isExitoso() { return exitoso; }
    public void setExitoso(boolean exitoso) { this.exitoso = exitoso; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getMensajeSeleccionado() { return mensajeSeleccionado; }
    public void setMensajeSeleccionado(String mensajeSeleccionado) { this.mensajeSeleccionado = mensajeSeleccionado; }

    public List<MensajePredefinidoDto> getMensajesPersonalizados() { return mensajesPersonalizados; }
    public void setMensajesPersonalizados(List<MensajePredefinidoDto> mensajesPersonalizados) { this.mensajesPersonalizados = mensajesPersonalizados; }

    public String getRfcReceptor() { return rfcReceptor; }
    public void setRfcReceptor(String rfcReceptor) { this.rfcReceptor = rfcReceptor; }

    public FormatoCorreoDto getFormatoCorreo() { return formatoCorreo; }
    public void setFormatoCorreo(FormatoCorreoDto formatoCorreo) { this.formatoCorreo = formatoCorreo; }

    // Manual builder to replace Lombok builder if processing fails
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private boolean exitoso;
        private String mensaje;
        private String mensajeSeleccionado;
        private List<MensajePredefinidoDto> mensajesPersonalizados;
        private String rfcReceptor;
        private FormatoCorreoDto formatoCorreo;

        public Builder exitoso(boolean exitoso) { this.exitoso = exitoso; return this; }
        public Builder mensaje(String mensaje) { this.mensaje = mensaje; return this; }
        public Builder mensajeSeleccionado(String mensajeSeleccionado) { this.mensajeSeleccionado = mensajeSeleccionado; return this; }
        public Builder mensajesPersonalizados(List<MensajePredefinidoDto> mensajesPersonalizados) { this.mensajesPersonalizados = mensajesPersonalizados; return this; }
        public Builder rfcReceptor(String rfcReceptor) { this.rfcReceptor = rfcReceptor; return this; }
        public Builder formatoCorreo(FormatoCorreoDto formatoCorreo) { this.formatoCorreo = formatoCorreo; return this; }

        public ConfiguracionCorreoResponseDto build() {
            ConfiguracionCorreoResponseDto r = new ConfiguracionCorreoResponseDto();
            r.setExitoso(this.exitoso);
            r.setMensaje(this.mensaje);
            r.setMensajeSeleccionado(this.mensajeSeleccionado);
            r.setMensajesPersonalizados(this.mensajesPersonalizados);
            r.setRfcReceptor(this.rfcReceptor);
            r.setFormatoCorreo(this.formatoCorreo);
            return r;
        }
    }
}