/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cibercom.facturacion_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class FacturaDTO {

    @NotBlank
    @Size(min = 12, max = 13) // ejemplo de tamaño RFC México
    private String rfc;

    @Email
    private String correoElectronico;

    @NotBlank
    private String razonSocial;

    private String nombre;
    private String paterno;
    private String materno;
    private String pais;
    private String noRegistroIdentidadTributaria;
    private String domicilioFiscal;
    private String regimenFiscal;
    private String usoCfdi;
    private String codigoFacturacion;
    private String tienda;

    @NotNull
    private LocalDate fecha;

    private String terminal;
    private String boleta;
    private String medioPago;
    private String formaPago;

    private boolean iepsDesglosado;

    private boolean guardarEnMongo;

    private String calle;
    private String noExterior;
    private String noInterior;
    private String colonia;
    private String localidad;
    private String municipio;
    private String codigoPostal;
    private String estado;
    private String metodoPago;

    private boolean generarFactura;

    // Getters y Setters

    public String getRfc() { return rfc; }
    public void setRfc(String rfc) { this.rfc = rfc; }

    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getPaterno() { return paterno; }
    public void setPaterno(String paterno) { this.paterno = paterno; }

    public String getMaterno() { return materno; }
    public void setMaterno(String materno) { this.materno = materno; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getNoRegistroIdentidadTributaria() { return noRegistroIdentidadTributaria; }
    public void setNoRegistroIdentidadTributaria(String noRegistroIdentidadTributaria) { this.noRegistroIdentidadTributaria = noRegistroIdentidadTributaria; }

    public String getDomicilioFiscal() { return domicilioFiscal; }
    public void setDomicilioFiscal(String domicilioFiscal) { this.domicilioFiscal = domicilioFiscal; }

    public String getRegimenFiscal() { return regimenFiscal; }
    public void setRegimenFiscal(String regimenFiscal) { this.regimenFiscal = regimenFiscal; }

    public String getUsoCfdi() { return usoCfdi; }
    public void setUsoCfdi(String usoCfdi) { this.usoCfdi = usoCfdi; }

    public String getCodigoFacturacion() { return codigoFacturacion; }
    public void setCodigoFacturacion(String codigoFacturacion) { this.codigoFacturacion = codigoFacturacion; }

    public String getTienda() { return tienda; }
    public void setTienda(String tienda) { this.tienda = tienda; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }

    public String getBoleta() { return boleta; }
    public void setBoleta(String boleta) { this.boleta = boleta; }

    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public boolean isIepsDesglosado() { return iepsDesglosado; }
    public void setIepsDesglosado(boolean iepsDesglosado) { this.iepsDesglosado = iepsDesglosado; }

    public boolean isGuardarEnMongo() { return guardarEnMongo; }
    public void setGuardarEnMongo(boolean guardarEnMongo) { this.guardarEnMongo = guardarEnMongo; }

    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }

    public String getNoExterior() { return noExterior; }
    public void setNoExterior(String noExterior) { this.noExterior = noExterior; }

    public String getNoInterior() { return noInterior; }
    public void setNoInterior(String noInterior) { this.noInterior = noInterior; }

    public String getColonia() { return colonia; }
    public void setColonia(String colonia) { this.colonia = colonia; }

    public String getLocalidad() { return localidad; }
    public void setLocalidad(String localidad) { this.localidad = localidad; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public boolean isGenerarFactura() { return generarFactura; }
    public void setGenerarFactura(boolean generarFactura) { this.generarFactura = generarFactura; }

    @Override
    public String toString() {
        return "FacturaDTO{" +
                "rfc='" + rfc + '\'' +
                ", correoElectronico='" + correoElectronico + '\'' +
                ", razonSocial='" + razonSocial + '\'' +
                ", nombre='" + nombre + '\'' +
                ", paterno='" + paterno + '\'' +
                ", materno='" + materno + '\'' +
                ", pais='" + pais + '\'' +
                ", noRegistroIdentidadTributaria='" + noRegistroIdentidadTributaria + '\'' +
                ", domicilioFiscal='" + domicilioFiscal + '\'' +
                ", regimenFiscal='" + regimenFiscal + '\'' +
                ", usoCfdi='" + usoCfdi + '\'' +
                ", codigoFacturacion='" + codigoFacturacion + '\'' +
                ", tienda='" + tienda + '\'' +
                ", fecha=" + fecha +
                ", terminal='" + terminal + '\'' +
                ", boleta='" + boleta + '\'' +
                ", medioPago='" + medioPago + '\'' +
                ", formaPago='" + formaPago + '\'' +
                ", iepsDesglosado=" + iepsDesglosado +
                ", guardarEnMongo=" + guardarEnMongo +
                ", calle='" + calle + '\'' +
                ", noExterior='" + noExterior + '\'' +
                ", noInterior='" + noInterior + '\'' +
                ", colonia='" + colonia + '\'' +
                ", localidad='" + localidad + '\'' +
                ", municipio='" + municipio + '\'' +
                ", codigoPostal='" + codigoPostal + '\'' +
                ", estado='" + estado + '\'' +
                ", metodoPago='" + metodoPago + '\'' +
                ", generarFactura=" + generarFactura +
                '}';
    }
}
