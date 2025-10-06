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
public class ConfiguracionCorreoResponseDto {
    private boolean exitoso;
    private String mensaje;
    private String mensajeSeleccionado;
    private List<MensajePredefinidoDto> mensajesPersonalizados;
    private String rfcReceptor;
    private FormatoCorreoDto formatoCorreo;
    public String getRfcReceptor() {
        return rfcReceptor;
    }
    public void setRfcReceptor(String rfcReceptor) {
        this.rfcReceptor = rfcReceptor;
    }
}