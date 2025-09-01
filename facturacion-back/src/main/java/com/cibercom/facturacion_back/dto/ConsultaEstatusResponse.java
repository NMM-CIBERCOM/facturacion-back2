package com.cibercom.facturacion_back.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsultaEstatusResponse {
    private boolean exitoso;
    private String mensaje;
    private String codigoEstatus;
    private String estado;
    private String esCancelable;
    private String estatusCancelacion;
    
    public static ConsultaEstatusResponse exito(String codigoEstatus, String estado, 
                                              String esCancelable, String estatusCancelacion) {
        return ConsultaEstatusResponse.builder()
                .exitoso(true)
                .mensaje("Consulta de estatus exitosa")
                .codigoEstatus(codigoEstatus)
                .estado(estado)
                .esCancelable(esCancelable)
                .estatusCancelacion(estatusCancelacion)
                .build();
    }
    
    public static ConsultaEstatusResponse error(String mensaje) {
        return ConsultaEstatusResponse.builder()
                .exitoso(false)
                .mensaje(mensaje)
                .build();
    }
}