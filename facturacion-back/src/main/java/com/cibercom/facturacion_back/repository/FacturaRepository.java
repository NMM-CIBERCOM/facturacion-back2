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

    Optional<Factura> findByUuid(String uuid);

    List<Factura> findByEmisorRfc(String emisorRfc);

    List<Factura> findByReceptorRfc(String receptorRfc);

    // Ajustado a columna real FECHA
    List<Factura> findByFechaFacturaBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    // Opcional: filtrar por estatus en SAT
    @Query("SELECT f FROM Factura f WHERE f.statusSat = 'TIMBRADA'")
    List<Factura> findTimbradas();

    // Reemplazo de búsqueda por emisor y rango de fecha
    @Query("SELECT f FROM Factura f WHERE f.emisorRfc = :rfc AND f.fechaFactura BETWEEN :inicio AND :fin")
    List<Factura> findByEmisorAndFecha(@Param("rfc") String rfc, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    @Query(value = "SELECT NVL(MAX(FOLIO), 0) FROM FACTURAS WHERE SERIE = :serie", nativeQuery = true)
    Integer findMaxFolioBySerie(@Param("serie") String serie);
}