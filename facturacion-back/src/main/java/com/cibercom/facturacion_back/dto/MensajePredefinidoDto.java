package com.cibercom.facturacion_back.dto;

/**
 * DTO para representar mensajes predefinidos o personalizados de correo.
 */
public class MensajePredefinidoDto {
    private String id;
    private String nombre;
    private String asunto;
    private String mensaje;

    // Constructor por defecto
    public MensajePredefinidoDto() {
    }

    // Constructor con todos los parámetros
    public MensajePredefinidoDto(String id, String nombre, String asunto, String mensaje) {
        this.id = id;
        this.nombre = nombre;
        this.asunto = asunto;
        this.mensaje = mensaje;
    }
    
    // Métodos explícitos para compatibilidad
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getAsunto() {
        return asunto;
    }
    
    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }
    
    public String getMensaje() {
        return mensaje;
    }
    
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
    
    // Método builder explícito para compatibilidad
    public static MensajePredefinidoDtoBuilder builder() {
        return new MensajePredefinidoDtoBuilder();
    }
    
    public static class MensajePredefinidoDtoBuilder {
        private String id;
        private String nombre;
        private String asunto;
        private String mensaje;
        
        public MensajePredefinidoDtoBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public MensajePredefinidoDtoBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }
        
        public MensajePredefinidoDtoBuilder asunto(String asunto) {
            this.asunto = asunto;
            return this;
        }
        
        public MensajePredefinidoDtoBuilder mensaje(String mensaje) {
            this.mensaje = mensaje;
            return this;
        }
        
        public MensajePredefinidoDto build() {
            return new MensajePredefinidoDto(id, nombre, asunto, mensaje);
        }
    }
}