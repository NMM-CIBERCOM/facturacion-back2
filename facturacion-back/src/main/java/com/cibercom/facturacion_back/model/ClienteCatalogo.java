package com.cibercom.facturacion_back.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CLIENTES")
public class ClienteCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CLIENTE")
    private Long idCliente;

    @Column(name = "RFC", length = 13, nullable = false)
    private String rfc;

    @Column(name = "RAZON_SOCIAL", length = 255, nullable = false)
    private String razonSocial;

    @Column(name = "NOMBRE", length = 100)
    private String nombre;

    @Column(name = "PATERNO", length = 100)
    private String paterno;

    @Column(name = "MATERNO", length = 100)
    private String materno;

    @Column(name = "CORREO_ELECTRONICO", length = 150)
    private String correoElectronico;

    @Column(name = "DOMICILIO_FISCAL", length = 255)
    private String domicilioFiscal;

    @Column(name = "REGIMEN_FISCAL", length = 150)
    private String regimenFiscal;

    @Column(name = "PAIS", length = 100)
    private String pais;

    @Column(name = "REGISTRO_TRIBUTARIO", length = 50)
    private String registroTributario;

    @Column(name = "USO_CFDI", length = 100)
    private String usoCfdi;

    @Column(name = "FECHA_ALTA")
    private LocalDateTime fechaAlta;

    public Long getIdCliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente = idCliente; }

    public String getRfc() { return rfc; }
    public void setRfc(String rfc) { this.rfc = rfc; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getPaterno() { return paterno; }
    public void setPaterno(String paterno) { this.paterno = paterno; }

    public String getMaterno() { return materno; }
    public void setMaterno(String materno) { this.materno = materno; }

    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }

    public String getDomicilioFiscal() { return domicilioFiscal; }
    public void setDomicilioFiscal(String domicilioFiscal) { this.domicilioFiscal = domicilioFiscal; }

    public String getRegimenFiscal() { return regimenFiscal; }
    public void setRegimenFiscal(String regimenFiscal) { this.regimenFiscal = regimenFiscal; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getRegistroTributario() { return registroTributario; }
    public void setRegistroTributario(String registroTributario) { this.registroTributario = registroTributario; }

    public String getUsoCfdi() { return usoCfdi; }
    public void setUsoCfdi(String usoCfdi) { this.usoCfdi = usoCfdi; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }
}