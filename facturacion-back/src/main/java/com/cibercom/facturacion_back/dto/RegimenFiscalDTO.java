package com.cibercom.facturacion_back.dto;

public class RegimenFiscalDTO {
    private String clave;
    private String descripcion;

    public RegimenFiscalDTO() {
    }

    public RegimenFiscalDTO(String clave, String descripcion) {
        this.clave = clave;
        this.descripcion = descripcion;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public static RegimenFiscalDTO fromString(String regimenString) {
        String[] parts = regimenString.split(" - ", 2);
        return new RegimenFiscalDTO(parts[0], parts.length > 1 ? parts[1] : "");
    }

    @Override
    public String toString() {
        return "RegimenFiscalDTO{" +
                "clave='" + clave + '\'' +
                ", descripcion='" + descripcion + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegimenFiscalDTO that = (RegimenFiscalDTO) o;
        return java.util.Objects.equals(clave, that.clave) &&
                java.util.Objects.equals(descripcion, that.descripcion);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(clave, descripcion);
    }
}