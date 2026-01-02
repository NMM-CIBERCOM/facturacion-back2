package com.cibercom.facturacion_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FacturaRequest {
    
    @NotBlank(message = "El nombre del emisor es obligatorio")
    private String nombreEmisor;
    
    @NotBlank(message = "El RFC del emisor es obligatorio")
    private String rfcEmisor;
    
    @NotBlank(message = "El código postal del emisor es obligatorio")
    private String codigoPostalEmisor;
    
    @NotBlank(message = "El régimen fiscal del emisor es obligatorio")
    private String regimenFiscalEmisor;
    
    @NotBlank(message = "El nombre del receptor es obligatorio")
    private String nombreReceptor;
    
    @NotBlank(message = "El RFC del receptor es obligatorio")
    private String rfcReceptor;
    
    @NotBlank(message = "El código postal del receptor es obligatorio")
    private String codigoPostalReceptor;
    
    @NotBlank(message = "El régimen fiscal del receptor es obligatorio")
    private String regimenFiscalReceptor;
    
    @NotNull(message = "La lista de conceptos es obligatoria")
    private List<Concepto> conceptos;
    
    @NotBlank(message = "El método de pago es obligatorio")
    private String metodoPago;
    
    @NotBlank(message = "La forma de pago es obligatoria")
    private String formaPago;
    
    @NotBlank(message = "El uso CFDI es obligatorio")
    private String usoCFDI;
    
    @Data
    public static class Concepto {
        @NotBlank(message = "La descripción del concepto es obligatoria")
        private String descripcion;
        
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
        private BigDecimal cantidad;
        
        @NotBlank(message = "La unidad es obligatoria")
        private String unidad;
        
        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
        private BigDecimal precioUnitario;
        
        @NotNull(message = "El importe es obligatorio")
        @DecimalMin(value = "0.01", message = "El importe debe ser mayor a 0")
        private BigDecimal importe;
        
        // Campos del catálogo de productos/servicios
        private String claveProdServ;  // Clave del catálogo c_ClaveProdServ
        private String claveUnidad;    // Clave del catálogo c_ClaveUnidad (generalmente igual a unidad)
        private String objetoImp;      // Objeto de impuesto del catálogo c_ObjetoImp (01, 02, 03)
        private BigDecimal tasaIva;    // Tasa de IVA (ej: 0.16 para 16%)

        // Getters y setters explícitos para asegurar disponibilidad en compilación
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public BigDecimal getCantidad() { return cantidad; }
        public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
        public BigDecimal getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
        public BigDecimal getImporte() { return importe; }
        public void setImporte(BigDecimal importe) { this.importe = importe; }
        public String getClaveProdServ() { return claveProdServ; }
        public void setClaveProdServ(String claveProdServ) { this.claveProdServ = claveProdServ; }
        public String getClaveUnidad() { return claveUnidad; }
        public void setClaveUnidad(String claveUnidad) { this.claveUnidad = claveUnidad; }
        public String getObjetoImp() { return objetoImp; }
        public void setObjetoImp(String objetoImp) { this.objetoImp = objetoImp; }
        public BigDecimal getTasaIva() { return tasaIva; }
        public void setTasaIva(BigDecimal tasaIva) { this.tasaIva = tasaIva; }
    }
    
    // Getters explícitos para asegurar disponibilidad si Lombok falla
    public String getNombreEmisor() { return nombreEmisor; }
    public String getRfcEmisor() { return rfcEmisor; }
    public String getCodigoPostalEmisor() { return codigoPostalEmisor; }
    public String getRegimenFiscalEmisor() { return regimenFiscalEmisor; }

    public String getNombreReceptor() { return nombreReceptor; }
    public String getRfcReceptor() { return rfcReceptor; }
    public String getCodigoPostalReceptor() { return codigoPostalReceptor; }
    public String getRegimenFiscalReceptor() { return regimenFiscalReceptor; }

    public java.util.List<Concepto> getConceptos() { return conceptos; }

    public String getMetodoPago() { return metodoPago; }
    public String getFormaPago() { return formaPago; }
    public String getUsoCFDI() { return usoCFDI; }

    // Setters explícitos para compatibilidad si Lombok falla
    public void setNombreEmisor(String nombreEmisor) { this.nombreEmisor = nombreEmisor; }
    public void setRfcEmisor(String rfcEmisor) { this.rfcEmisor = rfcEmisor; }
    public void setCodigoPostalEmisor(String codigoPostalEmisor) { this.codigoPostalEmisor = codigoPostalEmisor; }
    public void setRegimenFiscalEmisor(String regimenFiscalEmisor) { this.regimenFiscalEmisor = regimenFiscalEmisor; }

    public void setNombreReceptor(String nombreReceptor) { this.nombreReceptor = nombreReceptor; }
    public void setRfcReceptor(String rfcReceptor) { this.rfcReceptor = rfcReceptor; }
    public void setCodigoPostalReceptor(String codigoPostalReceptor) { this.codigoPostalReceptor = codigoPostalReceptor; }
    public void setRegimenFiscalReceptor(String regimenFiscalReceptor) { this.regimenFiscalReceptor = regimenFiscalReceptor; }

    public void setConceptos(java.util.List<Concepto> conceptos) { this.conceptos = conceptos; }

    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
    public void setUsoCFDI(String usoCFDI) { this.usoCFDI = usoCFDI; }
}