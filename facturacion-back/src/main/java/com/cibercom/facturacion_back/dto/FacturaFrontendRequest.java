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

    // razonSocial es obligatorio solo para personas morales (RFC de 12 caracteres)
    // Se valida condicionalmente en el servicio según el tipo de persona
    private String razonSocial;

    // nombre y paterno son obligatorios solo para personas físicas (RFC de 13 caracteres)
    // Se valida condicionalmente en el servicio según el tipo de persona
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

    // Campos para relaciones de CFDI (sustitución / refactura)
    private String uuidCfdiRelacionado;
    private String tipoRelacion;

    /** Tipo de documento para el PDF: "Refactura" muestra título y UUID relacionado en el PDF. */
    private String tipoDocumento;

    // Conceptos de la factura (para modo manual)
    private java.util.List<Concepto> conceptos;

    // Clase interna para Concepto
    @Data
    public static class Concepto {
        private String descripcion;
        private java.math.BigDecimal cantidad;
        private String unidad;
        private java.math.BigDecimal precioUnitario;
        private java.math.BigDecimal importe;
        private String claveProdServ;
        private String claveUnidad;
        private String objetoImp;
        private java.math.BigDecimal tasaIva;
    }

    // Getters explícitos para asegurar compilación
    public String getRfc() { return rfc; }
    public String getCorreoElectronico() { return correoElectronico; }
    public String getRazonSocial() { return razonSocial; }
    public String getNombre() { return nombre; }
    public String getPaterno() { return paterno; }
    public String getMaterno() { return materno; }
    public String getPais() { return pais; }
    public String getNoRegistroIdentidadTributaria() { return noRegistroIdentidadTributaria; }
    public String getDomicilioFiscal() { return domicilioFiscal; }
    public String getRegimenFiscal() { return regimenFiscal; }
    public String getUsoCfdi() { return usoCfdi; }
    public String getCodigoFacturacion() { return codigoFacturacion; }
    public String getTienda() { return tienda; }
    public LocalDate getFecha() { return fecha; }
    public String getTerminal() { return terminal; }
    public String getBoleta() { return boleta; }
    public String getMedioPago() { return medioPago; }
    public String getFormaPago() { return formaPago; }
    public Boolean getIepsDesglosado() { return iepsDesglosado; }
    public Boolean getGuardarEnMongo() { return guardarEnMongo; }
    public String getUuidCfdiRelacionado() { return uuidCfdiRelacionado; }
    public String getTipoRelacion() { return tipoRelacion; }
    public java.util.List<Concepto> getConceptos() { return conceptos; }
    public void setConceptos(java.util.List<Concepto> conceptos) { this.conceptos = conceptos; }
}