package com.cibercom.facturacion_back.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ConsultaFacturaRequest {
    private String rfcReceptor;
    private String nombreCliente;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String razonSocial;
    private String almacen;
    private String usuario;
    private String serie;
    private String folio;
    private String uuid;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaFin;

    private String tienda;
    private String te;
    private String tr;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaTienda;

    private String codigoFacturacion;
    private String motivoSustitucion;
    private String perfilUsuario;

    public boolean tieneAlMenosUnCampoLleno() {
        return notBlank(rfcReceptor) || notBlank(nombreCliente) || notBlank(apellidoPaterno) ||
               notBlank(razonSocial) || (notBlank(almacen) && !"todos".equalsIgnoreCase(almacen)) ||
               notBlank(usuario) || notBlank(serie) || notBlank(folio) || notBlank(uuid) ||
               (fechaInicio != null && fechaFin != null);
    }

    public boolean rangoFechasValido() {
        if (fechaInicio == null || fechaFin == null) return true;
        if (fechaInicio.isAfter(fechaFin)) return false;
        long dias = java.time.Duration.between(fechaInicio.atStartOfDay(), fechaFin.atStartOfDay()).toDays();
        return dias <= 365;
    }

    private boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

    @Override
    public String toString() {
        return "ConsultaFacturaRequest{" +
                "rfcReceptor='" + rfcReceptor + '\'' +
                ", nombreCliente='" + nombreCliente + '\'' +
                ", apellidoPaterno='" + apellidoPaterno + '\'' +
                ", apellidoMaterno='" + apellidoMaterno + '\'' +
                ", razonSocial='" + razonSocial + '\'' +
                ", almacen='" + almacen + '\'' +
                ", usuario='" + usuario + '\'' +
                ", serie='" + serie + '\'' +
                ", folio='" + folio + '\'' +
                ", uuid='" + uuid + '\'' +
                ", fechaInicio=" + fechaInicio +
                ", fechaFin=" + fechaFin +
                ", tienda='" + tienda + '\'' +
                ", te='" + te + '\'' +
                ", tr='" + tr + '\'' +
                ", fechaTienda=" + fechaTienda +
                ", codigoFacturacion='" + codigoFacturacion + '\'' +
                ", motivoSustitucion='" + motivoSustitucion + '\'' +
                ", perfilUsuario='" + perfilUsuario + '\'' +
                '}';
    }
}


