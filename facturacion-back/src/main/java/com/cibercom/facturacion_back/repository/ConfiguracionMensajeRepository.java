package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.ConfiguracionMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracionMensajeRepository extends JpaRepository<ConfiguracionMensaje, Long> {
    
    // Buscar configuración activa por tipo de mensaje
    Optional<ConfiguracionMensaje> findByTipoMensajeAndActivo(String tipoMensaje, String activo);
    
    // Buscar todas las configuraciones activas
    List<ConfiguracionMensaje> findByActivoOrderByFechaCreacionDesc(String activo);
    
    // Buscar configuraciones por mensaje seleccionado
    List<ConfiguracionMensaje> findByMensajeSeleccionadoAndActivo(String mensajeSeleccionado, String activo);
    
    // Buscar la configuración más reciente activa
    @Query("SELECT c FROM ConfiguracionMensaje c WHERE c.activo = 'S' ORDER BY c.fechaModificacion DESC, c.fechaCreacion DESC")
    Optional<ConfiguracionMensaje> findConfiguracionActual();
    
    // Buscar configuraciones personalizadas activas
    @Query("SELECT c FROM ConfiguracionMensaje c WHERE c.tipoMensaje = 'personalizado' AND c.activo = 'S' ORDER BY c.fechaCreacion DESC")
    List<ConfiguracionMensaje> findMensajesPersonalizadosActivos();
    
    // Desactivar todas las configuraciones existentes
    @Query("UPDATE ConfiguracionMensaje c SET c.activo = 'N', c.fechaModificacion = CURRENT_TIMESTAMP WHERE c.activo = 'S'")
    @Modifying
    @Transactional
    void desactivarTodasLasConfiguraciones();
    
    // Método alternativo para desactivar todas las configuraciones (usado en el servicio)
    @Query("UPDATE ConfiguracionMensaje c SET c.activo = 'N', c.fechaModificacion = CURRENT_TIMESTAMP WHERE c.activo = 'S'")
    @Modifying
    @Transactional
    void deactivateAllConfigurations();
    
    // Buscar la configuración más reciente activa (método alternativo)
    @Query("SELECT c FROM ConfiguracionMensaje c WHERE c.activo = 'S' AND c.tipoMensaje = 'configuracion_principal' ORDER BY c.fechaModificacion DESC, c.fechaCreacion DESC")
    Optional<ConfiguracionMensaje> findMostRecentActiveConfiguration();
    
    // Buscar mensajes personalizados activos (método alternativo)
    @Query("SELECT c FROM ConfiguracionMensaje c WHERE c.mensajeSeleccionado = 'personalizado' AND c.activo = 'S' ORDER BY c.fechaCreacion DESC")
    List<ConfiguracionMensaje> findActivePersonalizedMessages();
}