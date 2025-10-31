package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.PantallaConfigDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class PantallaConfigService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<PantallaConfigDto> getPantallasByPerfil(Integer idPerfil) {
        String sql = """
            SELECT 
                mc.ID_CONFIG,
                mc.ID_PERFIL,
                mc.MENU_LABEL,
                mc.MENU_PATH,
                mc.IS_VISIBLE,
                mc.ORDEN,
                p.NOMBRE_PERFIL as PARENT_LABEL
            FROM MENU_CONFIG mc
            JOIN PERFIL p ON mc.ID_PERFIL = p.ID_PERFIL
            WHERE mc.ID_PERFIL = ? 
            AND mc.MENU_PATH IS NOT NULL
            ORDER BY mc.ORDEN
            """;

        return jdbcTemplate.query(sql, new PantallaConfigRowMapper(), idPerfil);
    }

    public boolean updatePantallaVisibilidad(Integer idConfig, Boolean isVisible, String usuario) {
        try {
            String sql = """
                UPDATE MENU_CONFIG 
                SET IS_VISIBLE = ?, 
                    FECHA_MODIFICACION = SYSDATE,
                    USUARIO_MODIFICACION = ?
                WHERE ID_CONFIG = ?
                """;
            
            int rowsAffected = jdbcTemplate.update(sql, isVisible ? 1 : 0, usuario, idConfig);
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePantallaOrden(Integer idConfig, Integer orden, String usuario) {
        try {
            String sql = """
                UPDATE MENU_CONFIG 
                SET ORDEN = ?, 
                    FECHA_MODIFICACION = SYSDATE,
                    USUARIO_MODIFICACION = ?
                WHERE ID_CONFIG = ?
                """;
            
            int rowsAffected = jdbcTemplate.update(sql, orden, usuario, idConfig);
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static class PantallaConfigRowMapper implements RowMapper<PantallaConfigDto> {
        @Override
        public PantallaConfigDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            PantallaConfigDto dto = new PantallaConfigDto();
            dto.setIdConfig(rs.getInt("ID_CONFIG"));
            dto.setIdPerfil(rs.getInt("ID_PERFIL"));
            dto.setMenuLabel(rs.getString("MENU_LABEL"));
            dto.setMenuPath(rs.getString("MENU_PATH"));
            dto.setIsVisible(rs.getInt("IS_VISIBLE") == 1);
            dto.setOrden(rs.getInt("ORDEN"));
            dto.setParentLabel(rs.getString("PARENT_LABEL"));
            return dto;
        }
    }
}
