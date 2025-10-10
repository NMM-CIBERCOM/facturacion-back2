package com.cibercom.facturacion_back.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que representa una tienda en el sistema
 */
@Entity
@Table(name = "tiendas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tienda")
    private Long idTienda;

    @Column(name = "codigo_tienda", length = 20, nullable = false, unique = true)
    private String codigoTienda;

    @Column(name = "nombre_tienda", length = 100, nullable = false)
    private String nombreTienda;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "estado", length = 100)
    private String estado;

    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "gerente", length = 100)
    private String gerente;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "zona", length = 50)
    private String zona;

    @Column(name = "tipo_tienda", length = 50)
    private String tipoTienda;

    @Column(name = "estado_tienda", length = 20)
    private String estadoTienda = "ACTIVO";

    @Column(name = "fecha_apertura")
    private LocalDate fechaApertura;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "usuario_creacion", length = 50)
    private String usuarioCreacion;

    @Column(name = "usuario_modificacion", length = 50)
    private String usuarioModificacion;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaModificacion = LocalDateTime.now();
        if (estadoTienda == null) {
            estadoTienda = "ACTIVO";
        }
        if (tipoTienda == null) {
            tipoTienda = "Sucursal";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}