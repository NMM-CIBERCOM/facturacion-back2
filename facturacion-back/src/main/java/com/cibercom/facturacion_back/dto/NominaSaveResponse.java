package com.cibercom.facturacion_back.dto;

import java.util.List;

public class NominaSaveResponse {
    private boolean ok;
    private String message;
    private String uuidFactura;
    private Long idFactura;
    private Long idFacturaNomina;
    private List<String> errors;

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUuidFactura() { return uuidFactura; }
    public void setUuidFactura(String uuidFactura) { this.uuidFactura = uuidFactura; }

    public Long getIdFactura() { return idFactura; }
    public void setIdFactura(Long idFactura) { this.idFactura = idFactura; }

    public Long getIdFacturaNomina() { return idFacturaNomina; }
    public void setIdFacturaNomina(Long idFacturaNomina) { this.idFacturaNomina = idFacturaNomina; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}