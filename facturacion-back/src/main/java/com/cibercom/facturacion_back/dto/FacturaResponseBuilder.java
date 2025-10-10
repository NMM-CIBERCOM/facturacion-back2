package com.cibercom.facturacion_back.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FacturaResponseBuilder {
    private boolean exitoso;
    private String mensaje;
    private LocalDateTime timestamp;
    private String uuid;
    private String xmlTimbrado;
    private FacturaResponse.DatosFactura datosFactura;
    private String errores;

    FacturaResponseBuilder() {
    }

    public FacturaResponseBuilder exitoso(boolean exitoso) {
        this.exitoso = exitoso;
        return this;
    }

    public FacturaResponseBuilder mensaje(String mensaje) {
        this.mensaje = mensaje;
        return this;
    }

    public FacturaResponseBuilder timestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public FacturaResponseBuilder uuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public FacturaResponseBuilder xmlTimbrado(String xmlTimbrado) {
        this.xmlTimbrado = xmlTimbrado;
        return this;
    }

    public FacturaResponseBuilder datosFactura(FacturaResponse.DatosFactura datosFactura) {
        this.datosFactura = datosFactura;
        return this;
    }

    public FacturaResponseBuilder errores(String errores) {
        this.errores = errores;
        return this;
    }

    public FacturaResponse build() {
        return new FacturaResponse(exitoso, mensaje, timestamp, uuid, xmlTimbrado, datosFactura, errores);
    }
}