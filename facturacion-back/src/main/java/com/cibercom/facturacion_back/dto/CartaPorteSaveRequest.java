package com.cibercom.facturacion_back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class CartaPorteSaveRequest {
    
    // Datos fiscales receptor
    @JsonProperty("rfcIniciales")
    private String rfcIniciales;
    
    @JsonProperty("rfcFecha")
    private String rfcFecha;
    
    @JsonProperty("rfcHomoclave")
    private String rfcHomoclave;
    
    @JsonProperty("correoElectronico")
    private String correoElectronico;
    
    @JsonProperty("razonSocial")
    private String razonSocial;
    
    @JsonProperty("nombre")
    private String nombre;
    
    @JsonProperty("paterno")
    private String paterno;
    
    @JsonProperty("materno")
    private String materno;
    
    // Campos adicionales que envía el frontend
    @JsonProperty("apellidoPaterno")
    private String apellidoPaterno;
    
    @JsonProperty("apellidoMaterno")
    private String apellidoMaterno;
    
    @JsonProperty("pais")
    private String pais;
    
    @JsonProperty("noRegistroIdentidadTributaria")
    private String noRegistroIdentidadTributaria;
    
    @JsonProperty("noRegistroTrib")
    private String noRegistroTrib;
    
    @JsonProperty("domicilioFiscal")
    private String domicilioFiscal;
    
    @JsonProperty("regimenFiscal")
    private String regimenFiscal;
    
    @JsonProperty("usoCfdi")
    private String usoCfdi;
    
    // Información general
    @JsonProperty("descripcion")
    private String descripcion;
    
    @JsonProperty("fechaInformacion")
    private String fechaInformacion;
    
    @JsonProperty("numeroSerie")
    private String numeroSerie;
    
    @JsonProperty("precio")
    private String precio;
    
    @JsonProperty("personaAutoriza")
    private String personaAutoriza;
    
    @JsonProperty("puesto")
    private String puesto;
    
    // Campos Carta Porte (transporte)
    @JsonProperty("tipoTransporte")
    private String tipoTransporte;
    
    @JsonProperty("permisoSCT")
    private String permisoSCT;
    
    @JsonProperty("permisoSct")
    private String permisoSct;
    
    @JsonProperty("numeroPermisoSCT")
    private String numeroPermisoSCT;
    
    @JsonProperty("noPermisoSct")
    private String noPermisoSct;
    
    @JsonProperty("placasVehiculo")
    private String placasVehiculo;
    
    @JsonProperty("configVehicular")
    private String configVehicular;
    
    @JsonProperty("nombreTransportista")
    private String nombreTransportista;
    
    @JsonProperty("rfcTransportista")
    private String rfcTransportista;
    
    @JsonProperty("bienesTransportados")
    private String bienesTransportados;
    
    @JsonProperty("origen")
    private String origen;
    
    @JsonProperty("destino")
    private String destino;
    
    @JsonProperty("origenDomicilio")
    private String origenDomicilio;
    
    @JsonProperty("destinoDomicilio")
    private String destinoDomicilio;
    
    @JsonProperty("fechaSalida")
    private String fechaSalida;
    
    @JsonProperty("fechaLlegada")
    private String fechaLlegada;
    
    // Getters y Setters
    public String getRfcIniciales() {
        return rfcIniciales;
    }
    
    public void setRfcIniciales(String rfcIniciales) {
        this.rfcIniciales = rfcIniciales;
    }
    
    public String getRfcFecha() {
        return rfcFecha;
    }
    
    public void setRfcFecha(String rfcFecha) {
        this.rfcFecha = rfcFecha;
    }
    
    public String getRfcHomoclave() {
        return rfcHomoclave;
    }
    
    public void setRfcHomoclave(String rfcHomoclave) {
        this.rfcHomoclave = rfcHomoclave;
    }
    
    public String getCorreoElectronico() {
        return correoElectronico;
    }
    
    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }
    
    public String getRazonSocial() {
        return razonSocial;
    }
    
    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getPaterno() {
        return paterno;
    }
    
    public void setPaterno(String paterno) {
        this.paterno = paterno;
    }
    
    public String getMaterno() {
        return materno;
    }
    
    public void setMaterno(String materno) {
        this.materno = materno;
    }
    
    public String getPais() {
        return pais;
    }
    
    public void setPais(String pais) {
        this.pais = pais;
    }
    
    public String getNoRegistroIdentidadTributaria() {
        return noRegistroIdentidadTributaria;
    }
    
    public void setNoRegistroIdentidadTributaria(String noRegistroIdentidadTributaria) {
        this.noRegistroIdentidadTributaria = noRegistroIdentidadTributaria;
    }
    
    public String getDomicilioFiscal() {
        return domicilioFiscal;
    }
    
    public void setDomicilioFiscal(String domicilioFiscal) {
        this.domicilioFiscal = domicilioFiscal;
    }
    
    public String getRegimenFiscal() {
        return regimenFiscal;
    }
    
    public void setRegimenFiscal(String regimenFiscal) {
        this.regimenFiscal = regimenFiscal;
    }
    
    public String getUsoCfdi() {
        return usoCfdi;
    }
    
    public void setUsoCfdi(String usoCfdi) {
        this.usoCfdi = usoCfdi;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getFechaInformacion() {
        return fechaInformacion;
    }
    
    public void setFechaInformacion(String fechaInformacion) {
        this.fechaInformacion = fechaInformacion;
    }
    
    public String getNumeroSerie() {
        return numeroSerie;
    }
    
    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }
    
    public String getPrecio() {
        return precio;
    }
    
    public void setPrecio(String precio) {
        this.precio = precio;
    }
    
    public String getPersonaAutoriza() {
        return personaAutoriza;
    }
    
    public void setPersonaAutoriza(String personaAutoriza) {
        this.personaAutoriza = personaAutoriza;
    }
    
    public String getPuesto() {
        return puesto;
    }
    
    public void setPuesto(String puesto) {
        this.puesto = puesto;
    }
    
    public String getTipoTransporte() {
        return tipoTransporte;
    }
    
    public void setTipoTransporte(String tipoTransporte) {
        this.tipoTransporte = tipoTransporte;
    }
    
    public String getPermisoSCT() {
        return permisoSCT;
    }
    
    public void setPermisoSCT(String permisoSCT) {
        this.permisoSCT = permisoSCT;
    }
    
    public String getNumeroPermisoSCT() {
        return numeroPermisoSCT;
    }
    
    public void setNumeroPermisoSCT(String numeroPermisoSCT) {
        this.numeroPermisoSCT = numeroPermisoSCT;
    }
    
    public String getPlacasVehiculo() {
        return placasVehiculo;
    }
    
    public void setPlacasVehiculo(String placasVehiculo) {
        this.placasVehiculo = placasVehiculo;
    }
    
    public String getConfigVehicular() {
        return configVehicular;
    }
    
    public void setConfigVehicular(String configVehicular) {
        this.configVehicular = configVehicular;
    }
    
    public String getNombreTransportista() {
        return nombreTransportista;
    }
    
    public void setNombreTransportista(String nombreTransportista) {
        this.nombreTransportista = nombreTransportista;
    }
    
    public String getRfcTransportista() {
        return rfcTransportista;
    }
    
    public void setRfcTransportista(String rfcTransportista) {
        this.rfcTransportista = rfcTransportista;
    }
    
    public String getBienesTransportados() {
        return bienesTransportados;
    }
    
    public void setBienesTransportados(String bienesTransportados) {
        this.bienesTransportados = bienesTransportados;
    }
    
    public String getOrigen() {
        return origen;
    }
    
    public void setOrigen(String origen) {
        this.origen = origen;
    }
    
    public String getDestino() {
        return destino;
    }
    
    public void setDestino(String destino) {
        this.destino = destino;
    }
    
    public String getFechaSalida() {
        return fechaSalida;
    }
    
    public void setFechaSalida(String fechaSalida) {
        this.fechaSalida = fechaSalida;
    }
    
    public String getFechaLlegada() {
        return fechaLlegada;
    }
    
    public void setFechaLlegada(String fechaLlegada) {
        this.fechaLlegada = fechaLlegada;
    }
    
    // Getters y setters para campos adicionales
    public String getApellidoPaterno() {
        return apellidoPaterno;
    }
    
    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }
    
    public String getApellidoMaterno() {
        return apellidoMaterno;
    }
    
    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = apellidoMaterno;
    }
    
    public String getNoRegistroTrib() {
        return noRegistroTrib;
    }
    
    public void setNoRegistroTrib(String noRegistroTrib) {
        this.noRegistroTrib = noRegistroTrib;
    }
    
    public String getPermisoSct() {
        return permisoSct;
    }
    
    public void setPermisoSct(String permisoSct) {
        this.permisoSct = permisoSct;
    }
    
    public String getNoPermisoSct() {
        return noPermisoSct;
    }
    
    public void setNoPermisoSct(String noPermisoSct) {
        this.noPermisoSct = noPermisoSct;
    }
    
    public String getOrigenDomicilio() {
        return origenDomicilio;
    }
    
    public void setOrigenDomicilio(String origenDomicilio) {
        this.origenDomicilio = origenDomicilio;
    }
    
    public String getDestinoDomicilio() {
        return destinoDomicilio;
    }
    
    public void setDestinoDomicilio(String destinoDomicilio) {
        this.destinoDomicilio = destinoDomicilio;
    }
    
    // Método auxiliar para obtener RFC completo
    public String getRfcCompleto() {
        String iniciales = rfcIniciales != null ? rfcIniciales.toUpperCase() : "";
        String fecha = rfcFecha != null ? rfcFecha : "";
        String homoclave = rfcHomoclave != null ? rfcHomoclave.toUpperCase() : "";
        return iniciales + fecha + homoclave;
    }
}