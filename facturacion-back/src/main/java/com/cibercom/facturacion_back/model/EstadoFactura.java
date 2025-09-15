package com.cibercom.facturacion_back.model;

public enum EstadoFactura {

    POR_TIMBRAR("66", "POR TIMBRAR"),
    EMITIDA("0", "EMITIDA"),
    EN_PROCESO_CANCELACION("1", "EN PROCESO DE CANCELACION"),
    CANCELADA_SAT("2", "CANCELADA EN SAT"),
    DE_PASO("3", "DE PASO"),
    EN_PROCESO_EMISION("4", "EN PROCESO DE EMISION"),
    FACTURA_TEMPORAL("99", "FACTURA TEMPORAL"),
    EN_ESPERA_CANCELACION_BOLETA("67", "EN ESPERA DE CANCELACION BOLETA QUE SUSTITUYE");

    private final String codigo;
    private final String descripcion;

    EstadoFactura(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static EstadoFactura fromCodigo(String codigo) {
        for (EstadoFactura estado : values()) {
            if (estado.codigo.equals(codigo)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Código de estado no válido: " + codigo);
    }

    public static EstadoFactura fromDescripcion(String descripcion) {
        for (EstadoFactura estado : values()) {
            if (estado.descripcion.equalsIgnoreCase(descripcion)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Descripción de estado no válida: " + descripcion);
    }
}
