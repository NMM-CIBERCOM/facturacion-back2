package com.cibercom.facturacion_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar mensajes predefinidos o personalizados de correo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajePredefinidoDto {
    private String id;
    private String nombre;
    private String asunto;
    private String mensaje;

    // Getters/Setters explícitos para compatibilidad si Lombok falla
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    // Builder manual para reemplazar Lombok si el procesamiento falla
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String id;
        private String nombre;
        private String asunto;
        private String mensaje;

        public Builder id(String id) { this.id = id; return this; }
        public Builder nombre(String nombre) { this.nombre = nombre; return this; }
        public Builder asunto(String asunto) { this.asunto = asunto; return this; }
        public Builder mensaje(String mensaje) { this.mensaje = mensaje; return this; }

        public MensajePredefinidoDto build() {
            MensajePredefinidoDto m = new MensajePredefinidoDto();
            m.setId(this.id);
            m.setNombre(this.nombre);
            m.setAsunto(this.asunto);
            m.setMensaje(this.mensaje);
            return m;
        }
    }
}