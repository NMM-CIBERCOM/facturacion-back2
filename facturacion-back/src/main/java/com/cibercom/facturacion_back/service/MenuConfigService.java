package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.MenuConfigDto;
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
 * Servicio para la gestión de configuración de menús
 */
@Service
@Transactional
public class MenuConfigService {

    private static final Logger logger = LoggerFactory.getLogger(MenuConfigService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Obtiene la configuración de menús para un perfil específico
     */
    public List<MenuConfigDto> obtenerConfiguracionPorPerfil(Integer idPerfil) {
        String sql = "SELECT mc.ID_CONFIG, mc.ID_PERFIL, mc.MENU_LABEL, mc.MENU_PATH, " +
                    "mc.IS_VISIBLE, mc.ORDEN, p.NOMBRE_PERFIL " +
                    "FROM MENU_CONFIG mc " +
                    "LEFT JOIN PERFIL p ON mc.ID_PERFIL = p.ID_PERFIL " +
                    "WHERE mc.ID_PERFIL = ? " +
                    "ORDER BY mc.ORDEN";

        logger.info("Ejecutando consulta para perfil {}: {}", idPerfil, sql);
        List<MenuConfigDto> result = jdbcTemplate.query(sql, new MenuConfigRowMapper(), idPerfil);
        logger.info("Resultado de la consulta: {} registros encontrados", result.size());
        return result;
    }

    /**
     * Obtiene solo las pestañas principales (sin MENU_PATH) para un perfil específico
     */
    public List<MenuConfigDto> obtenerPestañasPorPerfil(Integer idPerfil) {
        String sql = "SELECT mc.ID_CONFIG, mc.ID_PERFIL, mc.MENU_LABEL, mc.MENU_PATH, " +
                    "mc.IS_VISIBLE, mc.ORDEN, p.NOMBRE_PERFIL " +
                    "FROM MENU_CONFIG mc " +
                    "LEFT JOIN PERFIL p ON mc.ID_PERFIL = p.ID_PERFIL " +
                    "WHERE mc.ID_PERFIL = ? AND mc.MENU_PATH IS NULL " +
                    "ORDER BY mc.ORDEN";

        logger.info("Ejecutando consulta de pestañas para perfil {}: {}", idPerfil, sql);
        List<MenuConfigDto> result = jdbcTemplate.query(sql, new MenuConfigRowMapper(), idPerfil);
        logger.info("Resultado de pestañas: {} registros encontrados", result.size());
        return result;
    }

    /**
     * Obtiene solo las pantallas específicas (con MENU_PATH) para un perfil específico
     */
    public List<MenuConfigDto> obtenerPantallasPorPerfil(Integer idPerfil) {
        String sql = "SELECT mc.ID_CONFIG, mc.ID_PERFIL, mc.MENU_LABEL, mc.MENU_PATH, " +
                    "mc.IS_VISIBLE, mc.ORDEN, p.NOMBRE_PERFIL " +
                    "FROM MENU_CONFIG mc " +
                    "LEFT JOIN PERFIL p ON mc.ID_PERFIL = p.ID_PERFIL " +
                    "WHERE mc.ID_PERFIL = ? AND mc.MENU_PATH IS NOT NULL " +
                    "ORDER BY mc.ORDEN";

        logger.info("Ejecutando consulta de pantallas para perfil {}: {}", idPerfil, sql);
        List<MenuConfigDto> result = jdbcTemplate.query(sql, new MenuConfigRowMapper(), idPerfil);
        logger.info("Resultado de pantallas: {} registros encontrados", result.size());
        return result;
    }

    /**
     * Obtiene todas las configuraciones de menús
     */
    public List<MenuConfigDto> obtenerTodasLasConfiguraciones() {
        String sql = "SELECT mc.ID_CONFIG, mc.ID_PERFIL, mc.MENU_LABEL, mc.MENU_PATH, " +
                    "mc.IS_VISIBLE, mc.ORDEN, p.NOMBRE_PERFIL " +
                    "FROM MENU_CONFIG mc " +
                    "LEFT JOIN PERFIL p ON mc.ID_PERFIL = p.ID_PERFIL " +
                    "ORDER BY mc.ID_PERFIL, mc.ORDEN";

        return jdbcTemplate.query(sql, new MenuConfigRowMapper());
    }

    /**
     * Actualiza la visibilidad de un menú
     */
    public Map<String, Object> actualizarVisibilidadMenu(Long idConfig, Boolean isVisible, String usuarioMod) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String sql = "UPDATE MENU_CONFIG SET IS_VISIBLE = ?, FECHA_MODIFICACION = SYSDATE, USUARIO_MODIFICACION = ? " +
                        "WHERE ID_CONFIG = ?";
            
            int result = jdbcTemplate.update(sql, isVisible ? 1 : 0, usuarioMod, idConfig);
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "Configuración de menú actualizada exitosamente");
            } else {
                response.put("success", false);
                response.put("message", "Configuración de menú no encontrada");
            }
            
        } catch (Exception e) {
            logger.error("Error al actualizar visibilidad del menú: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Actualiza el orden de un menú
     */
    public Map<String, Object> actualizarOrdenMenu(Long idConfig, Integer orden, String usuarioMod) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String sql = "UPDATE MENU_CONFIG SET ORDEN = ?, FECHA_MODIFICACION = SYSDATE, USUARIO_MODIFICACION = ? " +
                        "WHERE ID_CONFIG = ?";
            
            int result = jdbcTemplate.update(sql, orden, usuarioMod, idConfig);
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "Orden del menú actualizado exitosamente");
            } else {
                response.put("success", false);
                response.put("message", "Configuración de menú no encontrada");
            }
            
        } catch (Exception e) {
            logger.error("Error al actualizar orden del menú: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Obtiene los perfiles disponibles
     */
    public List<Map<String, Object>> obtenerPerfiles() {
        String sql = "SELECT ID_PERFIL, NOMBRE_PERFIL FROM PERFIL ORDER BY NOMBRE_PERFIL";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> perfil = new HashMap<>();
            perfil.put("idPerfil", rs.getInt("ID_PERFIL"));
            perfil.put("nombrePerfil", rs.getString("NOMBRE_PERFIL"));
            return perfil;
        });
    }

    /**
     * RowMapper para mapear resultados de consulta a MenuConfigDto
     */
    private static class MenuConfigRowMapper implements RowMapper<MenuConfigDto> {
        @Override
        public MenuConfigDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            MenuConfigDto menuConfig = new MenuConfigDto();
            menuConfig.setIdConfig(rs.getLong("ID_CONFIG"));
            menuConfig.setIdPerfil(rs.getInt("ID_PERFIL"));
            menuConfig.setMenuLabel(rs.getString("MENU_LABEL"));
            menuConfig.setMenuPath(rs.getString("MENU_PATH"));
            menuConfig.setIsVisible(rs.getInt("IS_VISIBLE") == 1);
            menuConfig.setOrden(rs.getInt("ORDEN"));
            menuConfig.setNombrePerfil(rs.getString("NOMBRE_PERFIL"));
            return menuConfig;
        }
    }
}
