package com.cibercom.facturacion_back.model;

// import com.cibercom.facturacion_back.dto.RegimenFiscalDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FacturaInfo {
    // Datos comunes
    private String rfc;
    private String curp; // Puede estar vacío para persona moral
    private String nombre; // Para persona física: Nombre(s). Para persona moral: Denominación/Razón Social
    private String primerApellido; // Solo para persona física
    private String segundoApellido; // Solo para persona física

    // Datos de domicilio
    private String cp;
    private String tipoVialidad;
    private String calle;
    private String numExt;
    private String numInt; // Puede estar vacío
    private String colonia;
    private String localidad; // Puede estar vacío
    private String municipio;
    private String entidadFederativa;
    private String entreCalle; // Puede estar vacío
    private String yCalle; // Puede estar vacío
    
    private List<String> regimenesFiscales; // Cambiado temporalmente a String
    private LocalDateTime fechaUltimaActualizacion;
}

