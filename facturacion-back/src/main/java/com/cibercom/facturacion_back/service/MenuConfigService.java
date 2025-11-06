package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.PerfilDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Servicio para la gestión de configuraciones de menú
 */
@Service
@Transactional
public class MenuConfigService {

    private static final Logger logger = LoggerFactory.getLogger(MenuConfigService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Obtiene todos los perfiles disponibles
     * Reutiliza el método del UsuarioService
     */
    public List<PerfilDto> obtenerPerfiles() {
        return usuarioService.obtenerPerfiles();
    }

    /**
     * Obtiene las configuraciones de menú para un perfil específico
     * Incluye tanto pestañas principales (MENU_PATH es NULL) como pantallas específicas
     */
    public List<Map<String, Object>> obtenerConfiguracionesPorPerfil(Integer idPerfil) {
        try {
            String sql = "SELECT ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, " +
                        "(SELECT NOMBRE_PERFIL FROM PERFIL WHERE ID_PERFIL = ?) AS NOMBRE_PERFIL " +
                        "FROM MENU_CONFIG " +
                        "WHERE ID_PERFIL = ? " +
                        "ORDER BY ORDEN, MENU_LABEL";

            return jdbcTemplate.query(sql, new RowMapper<Map<String, Object>>() {
                @Override
                public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Map<String, Object> config = new HashMap<>();
                    config.put("idConfig", rs.getInt("ID_CONFIG"));
                    config.put("idPerfil", rs.getInt("ID_PERFIL"));
                    config.put("menuLabel", rs.getString("MENU_LABEL"));
                    config.put("menuPath", rs.getString("MENU_PATH"));
                    config.put("isVisible", rs.getInt("IS_VISIBLE") == 1); // Convertir NUMBER a boolean
                    config.put("orden", rs.getInt("ORDEN"));
                    config.put("nombrePerfil", rs.getString("NOMBRE_PERFIL"));
                    return config;
                }
            }, idPerfil, idPerfil);

        } catch (Exception e) {
            logger.error("Error al obtener configuraciones de menú para perfil {}: {}", idPerfil, e.getMessage(), e);
            throw new RuntimeException("Error al obtener configuraciones de menú", e);
        }
    }

    /**
     * Obtiene solo las pantallas específicas (con MENU_PATH) para un perfil
     */
    public List<Map<String, Object>> obtenerPantallasPorPerfil(Integer idPerfil) {
        try {
            String sql = "SELECT mc.ID_CONFIG, mc.ID_PERFIL, mc.MENU_LABEL, mc.MENU_PATH, " +
                        "mc.IS_VISIBLE, mc.ORDEN " +
                        "FROM MENU_CONFIG mc " +
                        "WHERE mc.ID_PERFIL = ? AND mc.MENU_PATH IS NOT NULL " +
                        "ORDER BY mc.ORDEN, mc.MENU_LABEL";

            return jdbcTemplate.query(sql, new RowMapper<Map<String, Object>>() {
                @Override
                public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Map<String, Object> pantalla = new HashMap<>();
                    pantalla.put("idConfig", rs.getInt("ID_CONFIG"));
                    pantalla.put("idPerfil", rs.getInt("ID_PERFIL"));
                    pantalla.put("menuLabel", rs.getString("MENU_LABEL"));
                    pantalla.put("menuPath", rs.getString("MENU_PATH"));
                    pantalla.put("isVisible", rs.getInt("IS_VISIBLE") == 1); // Convertir NUMBER a boolean
                    pantalla.put("orden", rs.getInt("ORDEN"));
                    // parentLabel se calculará en el frontend si es necesario
                    return pantalla;
                }
            }, idPerfil);

        } catch (Exception e) {
            logger.error("Error al obtener pantallas para perfil {}: {}", idPerfil, e.getMessage(), e);
            throw new RuntimeException("Error al obtener pantallas", e);
        }
    }

    /**
     * Actualiza la visibilidad de una configuración de menú
     */
    public Map<String, Object> actualizarVisibilidad(Integer idConfig, Boolean isVisible, String usuarioMod) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validar que isVisible no sea null
            if (isVisible == null) {
                logger.error("❌ ERROR: isVisible es NULL para ID_CONFIG: {}", idConfig);
                response.put("success", false);
                response.put("message", "El valor de isVisible no puede ser null");
                return response;
            }
            
            // Convertir Boolean a Integer (1 = visible, 0 = oculto)
            Integer isVisibleInt = isVisible ? 1 : 0;
            String usuario = usuarioMod != null && !usuarioMod.trim().isEmpty() ? usuarioMod : "admin";
            
            logger.info("🔄 INICIANDO ACTUALIZACIÓN - ID_CONFIG: {}, isVisible (Boolean): {}, isVisibleInt (Integer): {}, USUARIO: {}", 
                        idConfig, isVisible, isVisibleInt, usuario);
            
            // Usar los nombres correctos de las columnas según la estructura real de la tabla
            String sql = "UPDATE MENU_CONFIG SET IS_VISIBLE = ?, FECHA_MODIFICACION = SYSDATE, USUARIO_MODIFICACION = ? " +
                        "WHERE ID_CONFIG = ?";
            
            logger.info("📝 Ejecutando SQL: {} con parámetros: IS_VISIBLE={}, USUARIO={}, ID_CONFIG={}", 
                        sql, isVisibleInt, usuario, idConfig);
            
            int rowsAffected = jdbcTemplate.update(sql, isVisibleInt, usuario, idConfig);
            
            logger.info("📊 Filas afectadas por UPDATE: {}", rowsAffected);
            
            if (rowsAffected > 0) {
                // Verificar el valor actualizado en la BD
                String verifySql = "SELECT IS_VISIBLE FROM MENU_CONFIG WHERE ID_CONFIG = ?";
                try {
                    Integer valorActualizado = jdbcTemplate.queryForObject(verifySql, Integer.class, idConfig);
                    logger.info("✅ VERIFICACIÓN EN BD - ID_CONFIG: {}, IS_VISIBLE después de UPDATE: {}", idConfig, valorActualizado);
                    
                    // Verificar que el valor guardado coincida con el esperado
                    if (!valorActualizado.equals(isVisibleInt)) {
                        logger.error("❌ ERROR DE PERSISTENCIA - Valor esperado: {}, Valor en BD: {}", isVisibleInt, valorActualizado);
                        response.put("success", false);
                        response.put("message", String.format("Error: El valor guardado (%d) no coincide con el esperado (%d)", valorActualizado, isVisibleInt));
                        return response;
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ No se pudo verificar el valor en BD: {}", e.getMessage());
                }
                
                response.put("success", true);
                response.put("message", "Visibilidad actualizada correctamente");
                response.put("isVisible", isVisible);
                response.put("isVisibleInt", isVisibleInt);
                logger.info("✅ ÉXITO - Visibilidad actualizada para ID_CONFIG: {} -> IS_VISIBLE = {}", idConfig, isVisibleInt);
            } else {
                response.put("success", false);
                response.put("message", "No se encontró la configuración con ID: " + idConfig);
                logger.warn("⚠️ No se encontró configuración con ID_CONFIG: {}", idConfig);
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("❌ EXCEPCIÓN al actualizar visibilidad de config {}: {}", idConfig, e.getMessage(), e);
            response.put("success", false);
            String errorMessage = e.getMessage();
            // Si es un error de SQL, extraer solo el mensaje relevante
            if (errorMessage != null && errorMessage.contains("bad SQL grammar")) {
                errorMessage = "Error en la consulta SQL. Verifique la estructura de la tabla MENU_CONFIG.";
            }
            response.put("message", "Error al actualizar visibilidad: " + errorMessage);
            return response;
        }
    }

    /**
     * Actualiza la visibilidad de una pantalla específica
     */
    public Map<String, Object> actualizarVisibilidadPantalla(Integer idConfig, Boolean isVisible, String usuarioMod) {
        // Reutiliza el mismo método ya que ambas tablas tienen la misma estructura
        return actualizarVisibilidad(idConfig, isVisible, usuarioMod);
    }
}

