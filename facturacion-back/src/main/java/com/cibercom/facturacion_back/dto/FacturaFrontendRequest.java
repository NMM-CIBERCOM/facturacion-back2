package com.cibercom.facturacion_back.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

import java.time.LocalDate;

@Data
public class FacturaFrontendRequest {

    @NotBlank(message = "El RFC es obligatorio")
    private String rfc;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String correoElectronico;

    @NotBlank(message = "La razón social es obligatoria")
    private String razonSocial;

    private String nombre;
    private String paterno;
    private String materno;
    private String pais;
    private String noRegistroIdentidadTributaria;

    @NotBlank(message = "El domicilio fiscal es obligatorio")
    private String domicilioFiscal;

    @NotBlank(message = "El régimen fiscal es obligatorio")
    private String regimenFiscal;

    @NotBlank(message = "El uso CFDI es obligatorio")
    private String usoCfdi;

    private String codigoFacturacion;
    private String tienda;
    private LocalDate fecha;
    private String terminal;
    private String boleta;

    private String medioPago;
    private String formaPago;
    private Boolean iepsDesglosado;

    private Boolean guardarEnMongo;
}