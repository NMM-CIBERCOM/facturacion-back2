package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.JustificacionDto;
import com.cibercom.facturacion_back.model.Justificacion;
import com.cibercom.facturacion_back.repository.JustificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JustificacionService {
    private final JustificacionRepository repo;
    private final JdbcTemplate jdbcTemplate;

    @Value("${facturacion.justificaciones.table-name:JUSTIFICACION}")
    private String justificacionesTable;

    public JustificacionService(JustificacionRepository repo, JdbcTemplate jdbcTemplate) {
        this.repo = repo;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<JustificacionDto> listarTodas() {
        // Intentar leer usando tabla configurada
        List<JustificacionDto> result = queryTable(justificacionesTable);
        
        // Si no hay resultados, probar con posible variante de nombre
        if (result.isEmpty()) {
            result = queryTable("JUSTIFICACIONES");
        }

        // Si aún vacío, buscar automáticamente tablas candidatas que contengan 'JUST'
        if (result.isEmpty()) {
            result = buscarTablaCandidata();
        }

        // Como último recurso, usar JPA si la tabla no existe o no tiene datos
        if (result.isEmpty()) {
            try {
                return repo.findAll().stream()
                        .map(this::toDto)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        
        return result;
    }

    private List<JustificacionDto> buscarTablaCandidata() {
        try {
            List<String> tablas = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM USER_TABLES WHERE TABLE_NAME LIKE '%JUST%' ORDER BY TABLE_NAME",
                String.class
            );
            for (String t : tablas) {
                List<JustificacionDto> intent = queryTable(t);
                if (intent.size() > 0) {
                    return intent;
                }
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    private List<JustificacionDto> queryTable(String tableName) {
        String sql = "SELECT ID, DESCRIPCION FROM " + tableName + " ORDER BY ID";
        try {
            return jdbcTemplate.query(sql, (rs, idx) -> {
                JustificacionDto dto = new JustificacionDto();
                dto.setId(rs.getLong("ID"));
                dto.setDescripcion(rs.getString("DESCRIPCION"));
                return dto;
            });
        } catch (Exception e) {
            // Tabla no existe o error de consulta: retornar vacío
            return Collections.emptyList();
        }
    }

    public JustificacionDto crear(String descripcion) {
        Justificacion entidad = new Justificacion();
        entidad.setDescripcion(descripcion);
        entidad = repo.save(entidad);
        return toDto(entidad);
    }

    private JustificacionDto toDto(Justificacion j) {
        JustificacionDto dto = new JustificacionDto();
        dto.setId(j.getId());
        dto.setDescripcion(j.getDescripcion());
        return dto;
    }
}