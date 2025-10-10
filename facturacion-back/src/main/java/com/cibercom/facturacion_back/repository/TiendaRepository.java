package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.Tienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de base de datos de tiendas
 */
@Repository
public interface TiendaRepository extends JpaRepository<Tienda, Long> {

    /**
     * Buscar tienda por código
     */
    Optional<Tienda> findByCodigoTienda(String codigoTienda);

    /**
     * Verificar si existe una tienda con el código dado
     */
    boolean existsByCodigoTienda(String codigoTienda);

    /**
     * Verificar si existe una tienda con el código dado excluyendo un ID específico
     */
    boolean existsByCodigoTiendaAndIdTiendaNot(String codigoTienda, Long idTienda);

    /**
     * Buscar tiendas por estado
     */
    List<Tienda> findByEstadoTienda(String estadoTienda);

    /**
     * Buscar tiendas por región
     */
    List<Tienda> findByRegion(String region);

    /**
     * Buscar tiendas por zona
     */
    List<Tienda> findByZona(String zona);

    /**
     * Buscar tiendas por tipo
     */
    List<Tienda> findByTipoTienda(String tipoTienda);

    /**
     * Buscar tiendas activas
     */
    @Query("SELECT t FROM Tienda t WHERE t.estadoTienda = 'ACTIVO'")
    List<Tienda> findTiendasActivas();

    /**
     * Buscar tiendas con filtros múltiples
     */
    @Query("SELECT t FROM Tienda t WHERE " +
           "(:estadoTienda IS NULL OR t.estadoTienda = :estadoTienda) AND " +
           "(:region IS NULL OR t.region = :region) AND " +
           "(:zona IS NULL OR t.zona = :zona) AND " +
           "(:tipoTienda IS NULL OR t.tipoTienda = :tipoTienda) AND " +
           "(:busqueda IS NULL OR " +
           " LOWER(t.nombreTienda) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           " LOWER(t.codigoTienda) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           " LOWER(t.ciudad) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           " LOWER(t.gerente) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<Tienda> findTiendasConFiltros(
            @Param("estadoTienda") String estadoTienda,
            @Param("region") String region,
            @Param("zona") String zona,
            @Param("tipoTienda") String tipoTienda,
            @Param("busqueda") String busqueda
    );

    /**
     * Obtener estadísticas de tiendas
     */
    @Query("SELECT " +
           "COUNT(t) as totalTiendas, " +
           "SUM(CASE WHEN t.estadoTienda = 'ACTIVO' THEN 1 ELSE 0 END) as tiendasActivas, " +
           "SUM(CASE WHEN t.estadoTienda = 'INACTIVO' THEN 1 ELSE 0 END) as tiendasInactivas, " +
           "SUM(CASE WHEN t.estadoTienda = 'SUSPENDIDO' THEN 1 ELSE 0 END) as tiendasSuspendidas " +
           "FROM Tienda t")
    Object[] getEstadisticasTiendas();

    /**
     * Obtener regiones únicas
     */
    @Query("SELECT DISTINCT t.region FROM Tienda t WHERE t.region IS NOT NULL ORDER BY t.region")
    List<String> findDistinctRegiones();

    /**
     * Obtener zonas únicas
     */
    @Query("SELECT DISTINCT t.zona FROM Tienda t WHERE t.zona IS NOT NULL ORDER BY t.zona")
    List<String> findDistinctZonas();

    /**
     * Obtener tipos de tienda únicos
     */
    @Query("SELECT DISTINCT t.tipoTienda FROM Tienda t WHERE t.tipoTienda IS NOT NULL ORDER BY t.tipoTienda")
    List<String> findDistinctTiposTienda();
}