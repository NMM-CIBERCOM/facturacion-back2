package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.entity.FormatoCorreo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la configuración de formato de correo electrónico
 */
@Repository
public interface FormatoCorreoRepository extends JpaRepository<FormatoCorreo, Long> {
    
    /**
     * Busca la configuración activa
     * 
     * @return Optional con la configuración activa
     */
    Optional<FormatoCorreo> findByActivo(Boolean activo);
    
    /**
     * Busca todas las configuraciones activas
     * 
     * @return Lista de configuraciones activas
     */
    List<FormatoCorreo> findAllByActivo(Boolean activo);
    
    /**
     * Busca todas las configuraciones ordenadas por fecha de creación descendente
     * 
     * @return Lista de configuraciones ordenadas
     */
    List<FormatoCorreo> findAllByOrderByFechaCreacionDesc();
    
    /**
     * Busca configuraciones por tipo de fuente
     * 
     * @param tipoFuente Tipo de fuente a buscar
     * @return Lista de configuraciones con el tipo de fuente especificado
     */
    List<FormatoCorreo> findByTipoFuenteAndActivo(String tipoFuente, Boolean activo);
    
    /**
     * Busca configuraciones por tamaño de fuente
     * 
     * @param tamanoFuente Tamaño de fuente a buscar
     * @return Lista de configuraciones con el tamaño de fuente especificado
     */
    List<FormatoCorreo> findByTamanoFuenteAndActivo(Integer tamanoFuente, Boolean activo);
    
    /**
     * Desactiva todas las configuraciones
     */
    @Modifying
    @Query("UPDATE FormatoCorreo f SET f.activo = :valor")
    void desactivarTodas(@Param("valor") Boolean valor);
    
    // Evita selección de columna inexistente en algunos esquemas Oracle
    /**
     * Activa una configuración específica y desactiva las demás
     * 
     * @param id ID de la configuración a activar
     */
    @Modifying
    @Query("UPDATE FormatoCorreo f SET f.activo = CASE WHEN f.id = :id THEN true ELSE false END")
    void activarConfiguracion(@Param("id") Long id);
    
    /**
     * Cuenta las configuraciones activas
     * 
     * @return Número de configuraciones activas
     */
    long countByActivo(Boolean activo);
    
    /**
     * Verifica si existe una configuración con los mismos parámetros
     * 
     * @param tipoFuente Tipo de fuente
     * @param tamanoFuente Tamaño de fuente
     * @param esCursiva Si es cursiva
     * @param colorTexto Color del texto
     * @return true si existe una configuración similar
     */
    boolean existsByTipoFuenteAndTamanoFuenteAndEsCursivaAndColorTextoAndActivo(
            String tipoFuente,
            Integer tamanoFuente,
            Boolean esCursiva,
            String colorTexto,
            Boolean activo);

    boolean existsByTipoFuenteAndTamanoFuenteAndEsCursivaAndEsSubrayadoAndColorTextoAndActivo(
            String tipoFuente,
            Integer tamanoFuente,
            Boolean esCursiva,
            Boolean esSubrayado,
            String colorTexto,
            Boolean activo);
}