package com.cibercom.facturacion_back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request para guardar datos de Nómina vinculados a una Factura.
 * Las fechas se esperan en formato ISO local (YYYY-MM-DD).
 */
public class NominaSaveRequest {

    @JsonProperty("idEmpleado")
    private String idEmpleado;

    @JsonProperty("fechaNomina")
    private String fechaNomina; // YYYY-MM-DD

    @JsonProperty("rfcEmisor")
    private String rfcEmisor;

    @JsonProperty("rfcReceptor")
    private String rfcReceptor;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("curp")
    private String curp;

    @JsonProperty("periodoPago")
    private String periodoPago;

    @JsonProperty("fechaPago")
    private String fechaPago; // YYYY-MM-DD

    @JsonProperty("percepciones")
    private String percepciones; // decimal string

    @JsonProperty("deducciones")
    private String deducciones; // decimal string

    @JsonProperty("total")
    private String total; // decimal string

    @JsonProperty("tipoNomina")
    private String tipoNomina; // O/E

    @JsonProperty("usoCfdi")
    private String usoCfdi; // CN01, etc.

    @JsonProperty("correoElectronico")
    private String correoElectronico;

    @JsonProperty("domicilioFiscalReceptor")
    private String domicilioFiscalReceptor; // Código postal del receptor

    // CRÍTICO NOM44: Campos requeridos cuando existe RegistroPatronal
    @JsonProperty("numSeguridadSocial")
    private String numSeguridadSocial; // Número de Seguridad Social (IMSS)
    
    @JsonProperty("fechaInicioRelLaboral")
    private String fechaInicioRelLaboral; // Fecha de inicio de relación laboral (YYYY-MM-DD)
    
    @JsonProperty("antiguedad")
    private String antiguedad; // Antigüedad en formato PnYnMnDn (ej: P1Y2M15D)
    
    @JsonProperty("riesgoPuesto")
    private String riesgoPuesto; // Clave de riesgo del puesto (ej: 1, 2, 3, etc.)
    
    @JsonProperty("salarioDiarioIntegrado")
    private String salarioDiarioIntegrado; // Salario diario integrado (decimal string)

    @JsonProperty("usuarioCreacion")
    private String usuarioCreacion;

    // Opcional: permitir que el frontend envíe un UUID deseado; si no, se genera
    @JsonProperty("uuidFactura")
    private String uuidFactura;

    public String getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(String idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getFechaNomina() { return fechaNomina; }
    public void setFechaNomina(String fechaNomina) { this.fechaNomina = fechaNomina; }

    public String getRfcEmisor() { return rfcEmisor; }
    public void setRfcEmisor(String rfcEmisor) { this.rfcEmisor = rfcEmisor; }

    public String getRfcReceptor() { return rfcReceptor; }
    public void setRfcReceptor(String rfcReceptor) { this.rfcReceptor = rfcReceptor; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCurp() { return curp; }
    public void setCurp(String curp) { this.curp = curp; }

    public String getPeriodoPago() { return periodoPago; }
    public void setPeriodoPago(String periodoPago) { this.periodoPago = periodoPago; }

    public String getFechaPago() { return fechaPago; }
    public void setFechaPago(String fechaPago) { this.fechaPago = fechaPago; }

    public String getPercepciones() { return percepciones; }
    public void setPercepciones(String percepciones) { this.percepciones = percepciones; }

    public String getDeducciones() { return deducciones; }
    public void setDeducciones(String deducciones) { this.deducciones = deducciones; }

    public String getTotal() { return total; }
    public void setTotal(String total) { this.total = total; }

    public String getTipoNomina() { return tipoNomina; }
    public void setTipoNomina(String tipoNomina) { this.tipoNomina = tipoNomina; }

    public String getUsoCfdi() { return usoCfdi; }
    public void setUsoCfdi(String usoCfdi) { this.usoCfdi = usoCfdi; }

    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }

    public String getDomicilioFiscalReceptor() { return domicilioFiscalReceptor; }
    public void setDomicilioFiscalReceptor(String domicilioFiscalReceptor) { this.domicilioFiscalReceptor = domicilioFiscalReceptor; }

    public String getNumSeguridadSocial() { return numSeguridadSocial; }
    public void setNumSeguridadSocial(String numSeguridadSocial) { this.numSeguridadSocial = numSeguridadSocial; }

    public String getFechaInicioRelLaboral() { return fechaInicioRelLaboral; }
    public void setFechaInicioRelLaboral(String fechaInicioRelLaboral) { this.fechaInicioRelLaboral = fechaInicioRelLaboral; }

    public String getAntiguedad() { return antiguedad; }
    public void setAntiguedad(String antiguedad) { this.antiguedad = antiguedad; }

    public String getRiesgoPuesto() { return riesgoPuesto; }
    public void setRiesgoPuesto(String riesgoPuesto) { this.riesgoPuesto = riesgoPuesto; }

    public String getSalarioDiarioIntegrado() { return salarioDiarioIntegrado; }
    public void setSalarioDiarioIntegrado(String salarioDiarioIntegrado) { this.salarioDiarioIntegrado = salarioDiarioIntegrado; }

    public String getUsuarioCreacion() { return usuarioCreacion; }
    public void setUsuarioCreacion(String usuarioCreacion) { this.usuarioCreacion = usuarioCreacion; }

    public String getUuidFactura() { return uuidFactura; }
    public void setUuidFactura(String uuidFactura) { this.uuidFactura = uuidFactura; }
}