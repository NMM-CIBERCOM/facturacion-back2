package com.cibercom.facturacion_back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO de historial de nóminas para la tabla de historial en frontend.
 */
public class NominaHistorialDTO {

    @JsonProperty("id")
    private Long id; // ID_FACTURA_NOMINA

    @JsonProperty("idEmpleado")
    private String idEmpleado;

    @JsonProperty("fecha")
    private String fecha; // YYYY-MM-DD (FECHA_PAGO preferente)

    @JsonProperty("estado")
    private String estado; // ESTADO de FACTURAS, si disponible

    @JsonProperty("uuid")
    private String uuid; // UUID/Folio Fiscal si disponible

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(String idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
}