package com.cibercom.facturacion_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionCorreoDto {
    private String mensajeSeleccionado;
    private List<MensajePredefinidoDto> mensajesPersonalizados;
    
    // Campos para crear/editar mensajes personalizados
    private String tipoMensaje;
    private String asuntoPersonalizado;
    private String mensajePersonalizado;
    private FormatoCorreoDto formatoCorreo;

    // Getters/Setters explícitos para compatibilidad si Lombok falla
    public String getMensajeSeleccionado() { return mensajeSeleccionado; }
    public void setMensajeSeleccionado(String mensajeSeleccionado) { this.mensajeSeleccionado = mensajeSeleccionado; }

    public List<MensajePredefinidoDto> getMensajesPersonalizados() { return mensajesPersonalizados; }
    public void setMensajesPersonalizados(List<MensajePredefinidoDto> mensajesPersonalizados) { this.mensajesPersonalizados = mensajesPersonalizados; }

    public String getTipoMensaje() { return tipoMensaje; }
    public void setTipoMensaje(String tipoMensaje) { this.tipoMensaje = tipoMensaje; }

    public String getAsuntoPersonalizado() { return asuntoPersonalizado; }
    public void setAsuntoPersonalizado(String asuntoPersonalizado) { this.asuntoPersonalizado = asuntoPersonalizado; }

    public String getMensajePersonalizado() { return mensajePersonalizado; }
    public void setMensajePersonalizado(String mensajePersonalizado) { this.mensajePersonalizado = mensajePersonalizado; }

    public FormatoCorreoDto getFormatoCorreo() { return formatoCorreo; }
    public void setFormatoCorreo(FormatoCorreoDto formatoCorreo) { this.formatoCorreo = formatoCorreo; }

    // Builder manual para reemplazar Lombok si el procesamiento falla
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String mensajeSeleccionado;
        private List<MensajePredefinidoDto> mensajesPersonalizados;
        private String tipoMensaje;
        private String asuntoPersonalizado;
        private String mensajePersonalizado;
        private FormatoCorreoDto formatoCorreo;

        public Builder mensajeSeleccionado(String mensajeSeleccionado) { this.mensajeSeleccionado = mensajeSeleccionado; return this; }
        public Builder mensajesPersonalizados(List<MensajePredefinidoDto> mensajesPersonalizados) { this.mensajesPersonalizados = mensajesPersonalizados; return this; }
        public Builder tipoMensaje(String tipoMensaje) { this.tipoMensaje = tipoMensaje; return this; }
        public Builder asuntoPersonalizado(String asuntoPersonalizado) { this.asuntoPersonalizado = asuntoPersonalizado; return this; }
        public Builder mensajePersonalizado(String mensajePersonalizado) { this.mensajePersonalizado = mensajePersonalizado; return this; }
        public Builder formatoCorreo(FormatoCorreoDto formatoCorreo) { this.formatoCorreo = formatoCorreo; return this; }

        public ConfiguracionCorreoDto build() {
            ConfiguracionCorreoDto c = new ConfiguracionCorreoDto();
            c.setMensajeSeleccionado(this.mensajeSeleccionado);
            c.setMensajesPersonalizados(this.mensajesPersonalizados);
            c.setTipoMensaje(this.tipoMensaje);
            c.setAsuntoPersonalizado(this.asuntoPersonalizado);
            c.setMensajePersonalizado(this.mensajePersonalizado);
            c.setFormatoCorreo(this.formatoCorreo);
            return c;
        }
    }
}