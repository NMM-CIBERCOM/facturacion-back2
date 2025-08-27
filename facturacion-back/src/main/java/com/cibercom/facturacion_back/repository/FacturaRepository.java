package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, String> {
    
    // Buscar por UUID
    Optional<Factura> findByUuid(String uuid);
    
    // Buscar por emisor
    List<Factura> findByEmisorRfc(String emisorRfc);
    
    // Buscar por receptor
    List<Factura> findByReceptorRfc(String receptorRfc);
    
    // Buscar por rango de fechas
    List<Factura> findByFechaGeneracionBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);
    
    // Buscar por estado
    List<Factura> findByEstado(String estado);
    
    // Buscar por tienda
    List<Factura> findByTienda(String tienda);
    
    // Buscar por código de facturación
    Optional<Factura> findByCodigoFacturacion(String codigoFacturacion);
    
    // Buscar facturas timbradas
    @Query("SELECT f FROM Factura f WHERE f.estado = 'TIMBRADA'")
    List<Factura> findFacturasTimbradas();
    
    // Contar facturas por estado
    @Query("SELECT f.estado, COUNT(f) FROM Factura f GROUP BY f.estado")
    List<Object[]> countByEstado();
    
    // Buscar facturas por emisor y rango de fechas
    @Query("SELECT f FROM Factura f WHERE f.emisorRfc = :emisorRfc AND f.fechaGeneracion BETWEEN :fechaInicio AND :fechaFin")
    List<Factura> findByEmisorRfcAndFechaGeneracionBetween(
        @Param("emisorRfc") String emisorRfc,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
} 