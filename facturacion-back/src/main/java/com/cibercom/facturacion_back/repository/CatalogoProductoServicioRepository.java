package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.CatalogoProductoServicio;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("oracle")
public interface CatalogoProductoServicioRepository extends JpaRepository<CatalogoProductoServicio, Long> {

    /**
     * Busca todos los productos/servicios activos de un usuario
     */
    List<CatalogoProductoServicio> findByIdUsuarioAndActivoOrderByDescripcion(Long idUsuario, String activo);

    /**
     * Busca un producto/servicio específico por usuario y clave (activo)
     */
    Optional<CatalogoProductoServicio> findByIdUsuarioAndClaveProdServAndActivo(
            Long idUsuario, String claveProdServ, String activo);

    /**
     * Busca un producto/servicio específico por usuario y clave (cualquier estado)
     */
    Optional<CatalogoProductoServicio> findByIdUsuarioAndClaveProdServ(
            Long idUsuario, String claveProdServ);

    /**
     * Busca productos/servicios por usuario con filtro de búsqueda en clave o descripción
     */
    @Query("SELECT c FROM CatalogoProductoServicio c WHERE c.idUsuario = :idUsuario " +
           "AND c.activo = '1' " +
           "AND (LOWER(c.claveProdServ) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
           "ORDER BY c.descripcion")
    List<CatalogoProductoServicio> buscarPorUsuarioYTexto(
            @Param("idUsuario") Long idUsuario, 
            @Param("busqueda") String busqueda);
}

