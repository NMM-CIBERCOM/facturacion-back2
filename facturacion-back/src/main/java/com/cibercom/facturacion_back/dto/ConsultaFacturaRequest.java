package com.cibercom.facturacion_back.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Data
public class ConsultaFacturaRequest {
    private String rfcReceptor;
    private String nombreCliente;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String razonSocial;
    private String almacen;
    private String usuario;
    private String serie;
    private String folio;
    private String uuid;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaFin;

    private String tienda;
    private String te;
    private String tr;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaTienda;

    private String codigoFacturacion;
    private String motivoSustitucion;
    private String perfilUsuario;
    
    /**
     * Verifica si al menos un campo de búsqueda está lleno
     */
    public boolean tieneAlMenosUnCampoLleno() {
        return notBlank(rfcReceptor) || notBlank(nombreCliente) || notBlank(apellidoPaterno) ||
                notBlank(razonSocial) || (notBlank(almacen) && !"todos".equalsIgnoreCase(almacen)) ||
                notBlank(usuario) || notBlank(serie) || notBlank(folio) || notBlank(uuid) ||
                (fechaInicio != null && fechaFin != null);
    }
    
    /**
     * Verifica si el rango de fechas es válido (no excede 365 días)
     */
    public boolean rangoFechasValido() {
        if (fechaInicio == null || fechaFin == null) return true;
        if (fechaInicio.isAfter(fechaFin)) return false;
        long dias = java.time.Duration.between(fechaInicio.atStartOfDay(), fechaFin.atStartOfDay()).toDays();
        return dias <= 365;
    }
    
    private boolean notBlank(String s) { 
        return s != null && !s.trim().isEmpty(); 
    }

    // Getters/Setters explícitos para confiabilidad si Lombok no está activo
    public String getRfcReceptor() { return rfcReceptor; }
    public void setRfcReceptor(String rfcReceptor) { this.rfcReceptor = rfcReceptor; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getApellidoPaterno() { return apellidoPaterno; }
    public void setApellidoPaterno(String apellidoPaterno) { this.apellidoPaterno = apellidoPaterno; }

    public String getApellidoMaterno() { return apellidoMaterno; }
    public void setApellidoMaterno(String apellidoMaterno) { this.apellidoMaterno = apellidoMaterno; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getAlmacen() { return almacen; }
    public void setAlmacen(String almacen) { this.almacen = almacen; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getTienda() { return tienda; }
    public void setTienda(String tienda) { this.tienda = tienda; }

    public String getTe() { return te; }
    public void setTe(String te) { this.te = te; }

    public String getTr() { return tr; }
    public void setTr(String tr) { this.tr = tr; }

    public LocalDate getFechaTienda() { return fechaTienda; }
    public void setFechaTienda(LocalDate fechaTienda) { this.fechaTienda = fechaTienda; }

    public String getCodigoFacturacion() { return codigoFacturacion; }
    public void setCodigoFacturacion(String codigoFacturacion) { this.codigoFacturacion = codigoFacturacion; }

    public String getMotivoSustitucion() { return motivoSustitucion; }
    public void setMotivoSustitucion(String motivoSustitucion) { this.motivoSustitucion = motivoSustitucion; }

    public String getPerfilUsuario() { return perfilUsuario; }
    public void setPerfilUsuario(String perfilUsuario) { this.perfilUsuario = perfilUsuario; }

    @Override
    public String toString() {
        return "ConsultaFacturaRequest{" +
                "rfcReceptor='" + rfcReceptor + '\'' +
                ", nombreCliente='" + nombreCliente + '\'' +
                ", apellidoPaterno='" + apellidoPaterno + '\'' +
                ", apellidoMaterno='" + apellidoMaterno + '\'' +
                ", razonSocial='" + razonSocial + '\'' +
                ", almacen='" + almacen + '\'' +
                ", usuario='" + usuario + '\'' +
                ", serie='" + serie + '\'' +
                ", folio='" + folio + '\'' +
                ", uuid='" + uuid + '\'' +
                ", fechaInicio=" + fechaInicio +
                ", fechaFin=" + fechaFin +
                ", tienda='" + tienda + '\'' +
                ", te='" + te + '\'' +
                ", tr='" + tr + '\'' +
                ", fechaTienda=" + fechaTienda +
                ", codigoFacturacion='" + codigoFacturacion + '\'' +
                ", motivoSustitucion='" + motivoSustitucion + '\'' +
                ", perfilUsuario='" + perfilUsuario + '\'' +
                '}';
    }
}


