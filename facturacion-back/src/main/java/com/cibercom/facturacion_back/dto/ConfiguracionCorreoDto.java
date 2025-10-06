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
}