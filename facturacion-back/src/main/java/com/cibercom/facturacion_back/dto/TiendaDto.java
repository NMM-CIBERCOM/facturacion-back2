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
}