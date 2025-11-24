package com.cibercom.facturacion_back.dto;

/**
 * Response para el guardado de factura global.
 */
public class FacturaGlobalGuardarResponse {
    private Boolean success;
    private String message;
    private Long idFacturaGlobal;
    private String uuid;
    private String serie;
    private String folio;
    private Integer facturasRelacionadas;

    // Getters y Setters
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getIdFacturaGlobal() { return idFacturaGlobal; }
    public void setIdFacturaGlobal(Long idFacturaGlobal) { this.idFacturaGlobal = idFacturaGlobal; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public Integer getFacturasRelacionadas() { return facturasRelacionadas; }
    public void setFacturasRelacionadas(Integer facturasRelacionadas) { this.facturasRelacionadas = facturasRelacionadas; }
}


