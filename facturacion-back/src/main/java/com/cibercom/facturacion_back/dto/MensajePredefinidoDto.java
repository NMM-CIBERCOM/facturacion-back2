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
}