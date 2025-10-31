package com.cibercom.facturacion_back.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador de prueba para diagnosticar problemas con perfiles
 */
@RestController
@RequestMapping("/api/test-menu")
@CrossOrigin(origins = "*")
public class TestMenuController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Endpoint de prueba para obtener perfiles
     */
    @GetMapping("/perfiles")
    public ResponseEntity<List<Map<String, Object>>> testPerfiles() {
        try {
            String sql = "SELECT ID_PERFIL, NOMBRE_PERFIL FROM PERFIL ORDER BY NOMBRE_PERFIL";
            
            List<Map<String, Object>> perfiles = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> perfil = new HashMap<>();
                perfil.put("idPerfil", rs.getInt("ID_PERFIL"));
                perfil.put("nombrePerfil", rs.getString("NOMBRE_PERFIL"));
                return perfil;
            });
            
            return ResponseEntity.ok(perfiles);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(List.of(error));
        }
    }

    /**
     * Endpoint de prueba para verificar conexión a base de datos
     */
    @GetMapping("/db-test")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        try {
            String sql = "SELECT COUNT(*) as total FROM PERFIL";
            Integer total = jdbcTemplate.queryForObject(sql, Integer.class);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalPerfiles", total);
            response.put("message", "Conexión a base de datos exitosa");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Endpoint de prueba para verificar tabla PERFIL
     */
    @GetMapping("/perfil-structure")
    public ResponseEntity<Map<String, Object>> testPerfilStructure() {
        try {
            String sql = "SELECT COLUMN_NAME, DATA_TYPE, NULLABLE FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'PERFIL' ORDER BY COLUMN_ID";
            
            List<Map<String, Object>> columns = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> column = new HashMap<>();
                column.put("name", rs.getString("COLUMN_NAME"));
                column.put("type", rs.getString("DATA_TYPE"));
                column.put("nullable", rs.getString("NULLABLE"));
                return column;
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("columns", columns);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
