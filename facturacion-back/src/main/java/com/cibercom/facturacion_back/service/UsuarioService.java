package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.UsuarioRegistroDto;
import com.cibercom.facturacion_back.dto.PerfilDto;
import com.cibercom.facturacion_back.dto.EmpleadoConsultaDTO;
import com.cibercom.facturacion_back.dto.UsuarioLoginDto;
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
import java.util.ArrayList;
import java.util.Date;

/**
 * Servicio para la gestión de usuarios
 */
@Service
@Transactional
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Registra un nuevo usuario en el sistema
     */
    public Map<String, Object> registrarUsuario(UsuarioRegistroDto usuario) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Verificar si el usuario ya existe
            String checkSql = "SELECT COUNT(*) FROM USUARIOS WHERE NO_USUARIO = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, usuario.getNoUsuario());
            
            if (count > 0) {
                response.put("success", false);
                response.put("message", "El usuario ya existe");
                return response;
            }
            
            // Insertar nuevo usuario
            String insertSql = "INSERT INTO USUARIOS (NO_USUARIO, NOMBRE_EMPLEADO, PASSWORD, ESTATUS_USUARIO, " +
                             "ID_PERFIL, FECHA_ALTA, FECHA_MOD, USUARIO_MOD, ID_DFI, ID_ESTACIONAMIENTO, MODIFICA_UBICACION) " +
                             "VALUES (?, ?, ?, ?, ?, SYSDATE, SYSDATE, ?, ?, ?, ?)";
            
            int result = jdbcTemplate.update(insertSql,
                usuario.getNoUsuario(),
                usuario.getNombreEmpleado(),
                usuario.getPassword(),
                usuario.getEstatusUsuario() != null ? usuario.getEstatusUsuario() : "A",
                usuario.getIdPerfil(),
                usuario.getUsuarioMod() != null ? usuario.getUsuarioMod() : "SYSTEM",
                usuario.getIdDfi(),
                usuario.getIdEstacionamiento(),
                usuario.getModificaUbicacion() != null ? usuario.getModificaUbicacion() : "N"
            );
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "Usuario registrado exitosamente");
                response.put("usuario", usuario);
            } else {
                response.put("success", false);
                response.put("message", "Error al registrar usuario");
            }
            
        } catch (Exception e) {
            logger.error("Error al registrar usuario: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Obtiene todos los perfiles disponibles
     */
    public List<PerfilDto> obtenerPerfiles() {
        String sql = "SELECT ID_PERFIL, NOMBRE_PERFIL, STATUS_PERFIL FROM PERFIL ORDER BY NOMBRE_PERFIL";
        
        return jdbcTemplate.query(sql, new RowMapper<PerfilDto>() {
            @Override
            public PerfilDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                PerfilDto perfil = new PerfilDto();
                perfil.setIdPerfil(rs.getInt("ID_PERFIL"));
                perfil.setNombrePerfil(rs.getString("NOMBRE_PERFIL"));
                // Usar el nombre del perfil como descripción por defecto
                perfil.setDescripcion("Perfil: " + rs.getString("NOMBRE_PERFIL"));
                return perfil;
            }
        });
    }

    /**
     * Consulta todos los empleados
     */
    public List<EmpleadoConsultaDTO> consultarEmpleados() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.NO_USUARIO, ");
        sql.append("(e.NOMBRE || ' ' || e.APELLIDO_PATERNO || ' ' || e.APELLIDO_MATERNO) AS NOMBRE_EMPLEADO, ");
        sql.append("e.CORREO_ELECTRONICO AS CORREO, ");
        sql.append("e.ESTATUS_EMPLEADO AS ESTATUS_USUARIO, ");
        sql.append("e.FECHA_INGRESO AS FECHA_ALTA, ");
        sql.append("NULL AS FECHA_MOD, NULL AS ID_DFI, NULL AS ID_ESTACIONAMIENTO, NULL AS ID_PERFIL, ");
        sql.append("NULL AS MODIFICA_UBICACION, NULL AS PASSWORD, NULL AS USUARIO_MOD, ");
        sql.append("e.SALARIO_BASE AS SALARIO_BASE, e.RFC AS RFC, e.CURP AS CURP ");
        sql.append("FROM EMPLEADOS e ");
        sql.append("ORDER BY NOMBRE_EMPLEADO");

        return jdbcTemplate.query(sql.toString(), new EmpleadoRowMapper());
    }

    /**
     * Consulta empleados específicos por criterios
     */
    public List<EmpleadoConsultaDTO> consultarEmpleadosEspecificos(String noUsuario, String nombreEmpleado, String idPerfil) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.NO_USUARIO, ");
        sql.append("(e.NOMBRE || ' ' || e.APELLIDO_PATERNO || ' ' || e.APELLIDO_MATERNO) AS NOMBRE_EMPLEADO, ");
        sql.append("e.CORREO_ELECTRONICO AS CORREO, ");
        sql.append("e.ESTATUS_EMPLEADO AS ESTATUS_USUARIO, ");
        sql.append("e.FECHA_INGRESO AS FECHA_ALTA, ");
        sql.append("NULL AS FECHA_MOD, NULL AS ID_DFI, NULL AS ID_ESTACIONAMIENTO, NULL AS ID_PERFIL, ");
        sql.append("NULL AS MODIFICA_UBICACION, NULL AS PASSWORD, NULL AS USUARIO_MOD, ");
        sql.append("e.SALARIO_BASE AS SALARIO_BASE, e.RFC AS RFC, e.CURP AS CURP ");
        sql.append("FROM EMPLEADOS e ");
        sql.append("WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (noUsuario != null && !noUsuario.trim().isEmpty()) {
            sql.append("AND e.NO_USUARIO LIKE ? ");
            params.add("%" + noUsuario + "%");
        }

        if (nombreEmpleado != null && !nombreEmpleado.trim().isEmpty()) {
            sql.append("AND (e.NOMBRE || ' ' || e.APELLIDO_PATERNO || ' ' || e.APELLIDO_MATERNO) LIKE ? ");
            params.add("%" + nombreEmpleado + "%");
        }
        
        // idPerfil no aplica en EMPLEADOS; filtro omitido intencionalmente
        sql.append("ORDER BY NOMBRE_EMPLEADO");
        
        return jdbcTemplate.query(sql.toString(), params.toArray(), new EmpleadoRowMapper());
    }

    /**
     * Actualiza el perfil de un usuario
     */
    public Map<String, Object> actualizarPerfil(String noUsuario, Integer idPerfil, String usuarioMod) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String sql = "UPDATE USUARIOS SET ID_PERFIL = ?, FECHA_MOD = SYSDATE, USUARIO_MOD = ? " +
                        "WHERE NO_USUARIO = ?";
            
            int result = jdbcTemplate.update(sql, idPerfil, usuarioMod, noUsuario);
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "Perfil actualizado exitosamente");
            } else {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
            }
            
        } catch (Exception e) {
            logger.error("Error al actualizar perfil: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Elimina (desactiva) un usuario
     */
    public Map<String, Object> eliminarUsuario(String noUsuario, String usuarioMod) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String sql = "UPDATE USUARIOS SET ESTATUS_USUARIO = 'I', FECHA_MOD = SYSDATE, USUARIO_MOD = ? " +
                        "WHERE NO_USUARIO = ?";
            
            int result = jdbcTemplate.update(sql, usuarioMod, noUsuario);
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "Usuario eliminado exitosamente");
            } else {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
            }
            
        } catch (Exception e) {
            logger.error("Error al eliminar usuario: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Autentica un usuario con su nombre de usuario y contraseña
     */
    public Map<String, Object> autenticarUsuario(String username, String password) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Intentando autenticar usuario: {}", username);
            
            // Consultar usuario por nombre de usuario y contraseña
            String sql = "SELECT u.NO_USUARIO, u.NOMBRE_EMPLEADO, u.ESTATUS_USUARIO, u.ID_DFI, " +
                        "u.ID_ESTACIONAMIENTO, u.MODIFICA_UBICACION, u.ID_PERFIL, p.NOMBRE_PERFIL " +
                        "FROM USUARIOS u " +
                        "LEFT JOIN PERFIL p ON u.ID_PERFIL = p.ID_PERFIL " +
                        "WHERE u.NO_USUARIO = ? AND u.PASSWORD = ? AND u.ESTATUS_USUARIO = 'ACTIVO'";
            
            List<UsuarioLoginDto> usuarios = jdbcTemplate.query(sql, new UsuarioLoginRowMapper(), username, password);
            
            if (!usuarios.isEmpty()) {
                UsuarioLoginDto usuario = usuarios.get(0);
                logger.info("Usuario autenticado exitosamente: {}", username);
                
                response.put("success", true);
                response.put("message", "Autenticación exitosa");
                response.put("usuario", usuario);
                
                // Aquí podrías generar un token JWT si lo necesitas
                // response.put("token", generateJwtToken(usuario));
                
            } else {
                logger.warn("Fallo en la autenticación para usuario: {}", username);
                response.put("success", false);
                response.put("message", "Credenciales inválidas o usuario inactivo");
            }
            
        } catch (Exception e) {
            logger.error("Error durante la autenticación: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
        }
        
        return response;
    }

    /**
     * RowMapper para mapear resultados de consulta a EmpleadoConsultaDTO
     */
    private static class EmpleadoRowMapper implements RowMapper<EmpleadoConsultaDTO> {
        @Override
        public EmpleadoConsultaDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            EmpleadoConsultaDTO empleado = new EmpleadoConsultaDTO();
            empleado.setNoUsuario(rs.getString("NO_USUARIO"));
            empleado.setNombreEmpleado(rs.getString("NOMBRE_EMPLEADO"));
            empleado.setEstatusUsuario(rs.getString("ESTATUS_USUARIO"));
            empleado.setFechaAlta(rs.getTimestamp("FECHA_ALTA"));
            empleado.setFechaMod(rs.getTimestamp("FECHA_MOD"));
            empleado.setIdDfi(rs.getObject("ID_DFI", Integer.class));
            empleado.setIdEstacionamiento(rs.getObject("ID_ESTACIONAMIENTO", Integer.class));
            empleado.setIdPerfil(rs.getObject("ID_PERFIL", Integer.class));
            empleado.setModificaUbicacion(rs.getString("MODIFICA_UBICACION"));
            empleado.setPassword(rs.getString("PASSWORD"));
            empleado.setUsuarioMod(rs.getString("USUARIO_MOD"));
            empleado.setCorreo(rs.getString("CORREO"));
            empleado.setNombre(rs.getString("NOMBRE_EMPLEADO"));
            empleado.setSalarioBase(rs.getBigDecimal("SALARIO_BASE"));
            empleado.setRfc(rs.getString("RFC"));
            empleado.setCurp(rs.getString("CURP"));
            empleado.setNombrePerfil(null); // No aplica en EMPLEADOS
            empleado.setId(null);           // No existe ID en este esquema
            empleado.setContrasena(null);
            return empleado;
        }
    }


    /**
     * RowMapper para mapear resultados de consulta a UsuarioLoginDto
     */
    private static class UsuarioLoginRowMapper implements RowMapper<UsuarioLoginDto> {
        @Override
        public UsuarioLoginDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            UsuarioLoginDto usuario = new UsuarioLoginDto();
            usuario.setNoUsuario(rs.getString("NO_USUARIO"));
            usuario.setNombreEmpleado(rs.getString("NOMBRE_EMPLEADO"));
            usuario.setNombrePerfil(rs.getString("NOMBRE_PERFIL"));
            usuario.setIdPerfil(rs.getObject("ID_PERFIL", Integer.class));
            usuario.setEstatusUsuario(rs.getString("ESTATUS_USUARIO"));
            usuario.setIdDfi(rs.getObject("ID_DFI", Integer.class));
            usuario.setIdEstacionamiento(rs.getObject("ID_ESTACIONAMIENTO", Integer.class));
            usuario.setModificaUbicacion(rs.getString("MODIFICA_UBICACION"));
            
            return usuario;
        }
    }
}