package com.cibercom.facturacion_back.dto;

/** Respuesta al insertar un concepto. */
public class ConceptoInsertResponse {
    private boolean success;
    private String message;
    private Long idConcepto;
    private Long idFactura;

    public ConceptoInsertResponse() {}
    public ConceptoInsertResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getIdConcepto() { return idConcepto; }
    public void setIdConcepto(Long idConcepto) { this.idConcepto = idConcepto; }

    public Long getIdFactura() { return idFactura; }
    public void setIdFactura(Long idFactura) { this.idFactura = idFactura; }
}