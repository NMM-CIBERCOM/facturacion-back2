package com.cibercom.facturacion_back.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para transferencia de datos de tiendas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TiendaDto {

    private Long idTienda;

    @NotBlank(message = "El código de tienda es obligatorio")
    @Size(max = 20, message = "El código de tienda no puede exceder 20 caracteres")
    private String codigoTienda;

    @NotBlank(message = "El nombre de tienda es obligatorio")
    @Size(max = 100, message = "El nombre de tienda no puede exceder 100 caracteres")
    private String nombreTienda;

    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String direccion;

    @Size(max = 100, message = "La ciudad no puede exceder 100 caracteres")
    private String ciudad;

    @Size(max = 100, message = "El estado no puede exceder 100 caracteres")
    private String estado;

    @Size(max = 10, message = "El código postal no puede exceder 10 caracteres")
    private String codigoPostal;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;

    @Email(message = "El formato del email no es válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;

    @Size(max = 100, message = "El nombre del gerente no puede exceder 100 caracteres")
    private String gerente;

    @Size(max = 50, message = "La región no puede exceder 50 caracteres")
    private String region;

    @Size(max = 50, message = "La zona no puede exceder 50 caracteres")
    private String zona;

    @Size(max = 50, message = "El tipo de tienda no puede exceder 50 caracteres")
    private String tipoTienda;

    @Size(max = 20, message = "El estado de tienda no puede exceder 20 caracteres")
    private String estadoTienda;

    private LocalDate fechaApertura;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaModificacion;

    private String usuarioCreacion;

    private String usuarioModificacion;

    private String observaciones;

    // Getters y setters explícitos para compatibilidad si Lombok falla
    public Long getIdTienda() { return idTienda; }
    public void setIdTienda(Long idTienda) { this.idTienda = idTienda; }

    public String getCodigoTienda() { return codigoTienda; }
    public void setCodigoTienda(String codigoTienda) { this.codigoTienda = codigoTienda; }

    public String getNombreTienda() { return nombreTienda; }
    public void setNombreTienda(String nombreTienda) { this.nombreTienda = nombreTienda; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGerente() { return gerente; }
    public void setGerente(String gerente) { this.gerente = gerente; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public String getTipoTienda() { return tipoTienda; }
    public void setTipoTienda(String tipoTienda) { this.tipoTienda = tipoTienda; }

    public String getEstadoTienda() { return estadoTienda; }
    public void setEstadoTienda(String estadoTienda) { this.estadoTienda = estadoTienda; }

    public java.time.LocalDate getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(java.time.LocalDate fechaApertura) { this.fechaApertura = fechaApertura; }

    public java.time.LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(java.time.LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public java.time.LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(java.time.LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public String getUsuarioCreacion() { return usuarioCreacion; }
    public void setUsuarioCreacion(String usuarioCreacion) { this.usuarioCreacion = usuarioCreacion; }

    public String getUsuarioModificacion() { return usuarioModificacion; }
    public void setUsuarioModificacion(String usuarioModificacion) { this.usuarioModificacion = usuarioModificacion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}