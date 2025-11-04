package com.cibercom.facturacion_back.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class FacturaInfo {
    private String rfc;
    private String curp;
    private String nombre;
    private String primerApellido;
    private String segundoApellido;
    private String tipoVialidad;
    private String calle;
    private String numExt;
    private String numInt;
    private String colonia;
    private String localidad;
    private String municipio;
    private String entidadFederativa;
    private String entreCalle;
    private String yCalle;
    private String cp;
    private List<String> regimenesFiscales = new ArrayList<>();
    private String fechaUltimaActualizacion;
}