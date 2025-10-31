package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.UsuarioRegistroDto;
import com.cibercom.facturacion_back.dto.PerfilDto;
import com.cibercom.facturacion_back.dto.EmpleadoConsultaDTO;
import com.cibercom.facturacion_back.dto.UsuarioLoginDto;
import com.cibercom.facturacion_back.dto.TwoFactorSetupDto;
import com.cibercom.facturacion_back.model.UserRole;
import com.cibercom.facturacion_back.model.PaymentStatus;
import com.cibercom.facturacion_back.util.PasswordUtil;
import com.cibercom.facturacion_back.util.JwtUtil;
import com.cibercom.facturacion_back.util.TwoFactorAuthUtil;
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

/**
 * Servicio para la gestión de usuarios
 */
@Service
@Transactional
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private PasswordUtil passwordUtil;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private TwoFactorAuthUtil twoFactorAuthUtil;

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
        
        return jdbcTemplate.query(sql.toString(), new EmpleadoRowMapper(), params.toArray());
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
            
            // Consultar usuario por nombre de usuario (compatible con sistema actual)
            String sql = "SELECT u.NO_USUARIO, u.NOMBRE_EMPLEADO, u.PASSWORD, u.ESTATUS_USUARIO, u.ID_DFI, " +
                        "u.ID_ESTACIONAMIENTO, u.MODIFICA_UBICACION, u.ID_PERFIL, p.NOMBRE_PERFIL " +
                        "FROM USUARIOS u " +
                        "LEFT JOIN PERFIL p ON u.ID_PERFIL = p.ID_PERFIL " +
                        "WHERE u.NO_USUARIO = ? AND u.ESTATUS_USUARIO = 'A'";
            
            List<UsuarioLoginDto> usuarios = jdbcTemplate.query(sql, new UsuarioLoginRowMapper(), username);
            
            if (!usuarios.isEmpty()) {
                UsuarioLoginDto usuario = usuarios.get(0);
                
                // Verificar contraseña (compatible con sistema actual)
                boolean passwordValid = false;
                String storedPassword = getPasswordFromDatabase(username);
                
                if (storedPassword != null) {
                    // Verificar contraseña en texto plano (sistema actual)
                    passwordValid = password.equals(storedPassword);
                    
                    // Log para debugging
                    logger.info("Verificando contraseña para usuario: {}, longitud almacenada: {}", 
                               username, storedPassword.length());
                } else {
                    logger.warn("No se encontró contraseña para usuario: {}", username);
                }
                
                if (passwordValid) {
                    // Determinar rol del usuario
                    UserRole role = determineUserRole(usuario.getIdPerfil(), usuario.getNombrePerfil());
                    usuario.setRole(role);
                    
                    // Determinar estado de pago
                    PaymentStatus paymentStatus = determinePaymentStatus(username);
                    usuario.setPaymentStatus(paymentStatus);
                    
                    // Verificar acceso basado en estado de pago
                    if (!usuario.hasAccess()) {
                        logger.warn("Usuario sin acceso por estado de pago: {}", username);
                        response.put("success", false);
                        response.put("message", "Acceso denegado: Estado de pago inválido");
                        response.put("requiresPayment", true);
                        return response;
                    }
                    
                    // Generar token JWT
                    String token = jwtUtil.generateToken(username, role.getCode(), usuario.getIdPerfil());
                    usuario.setSessionToken(token);
                    
                    // Actualizar último login
                    updateLastLogin(username);
                    
                    logger.info("Usuario autenticado exitosamente: {}", username);
                    
                    response.put("success", true);
                    response.put("message", "Autenticación exitosa");
                    response.put("usuario", usuario);
                    response.put("token", token);
                    response.put("requiresTwoFactor", usuario.isTwoFactorEnabled());
                    
                } else {
                    logger.warn("Contraseña incorrecta para usuario: {}", username);
                    response.put("success", false);
                    response.put("message", "Credenciales inválidas");
                }
                
            } else {
                logger.warn("Usuario no encontrado o inactivo: {}", username);
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
     * RowMapper para mapear resultados de consulta a UsuarioLoginDto (versión robusta)
     */
    private static class UsuarioLoginRowMapper implements RowMapper<UsuarioLoginDto> {
        @Override
        public UsuarioLoginDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            UsuarioLoginDto usuario = new UsuarioLoginDto();
            
            // Campos básicos (siempre existen)
            usuario.setNoUsuario(rs.getString("NO_USUARIO"));
            usuario.setNombreEmpleado(rs.getString("NOMBRE_EMPLEADO"));
            usuario.setNombrePerfil(rs.getString("NOMBRE_PERFIL"));
            usuario.setIdPerfil(rs.getObject("ID_PERFIL", Integer.class));
            usuario.setEstatusUsuario(rs.getString("ESTATUS_USUARIO"));
            usuario.setIdDfi(rs.getObject("ID_DFI", Integer.class));
            usuario.setIdEstacionamiento(rs.getObject("ID_ESTACIONAMIENTO", Integer.class));
            usuario.setModificaUbicacion(rs.getString("MODIFICA_UBICACION"));
            
            // Campos nuevos (pueden no existir)
            usuario.setTwoFactorEnabled(false);
            usuario.setTwoFactorSecret(null);
            usuario.setLastLogin(null);
            
            return usuario;
        }
    }
    
    /**
     * Obtiene la contraseña del usuario desde la base de datos
     */
    private String getPasswordFromDatabase(String username) {
        try {
            String sql = "SELECT PASSWORD FROM USUARIOS WHERE NO_USUARIO = ?";
            return jdbcTemplate.queryForObject(sql, String.class, username);
        } catch (Exception e) {
            logger.error("Error obteniendo contraseña para usuario: {}", username, e);
            return null;
        }
    }
    
    /**
     * Determina el rol del usuario basado en su perfil
     */
    private UserRole determineUserRole(Integer idPerfil, String nombrePerfil) {
        if (idPerfil == null) {
            return UserRole.USUARIO;
        }
        
        // Mapeo de perfiles a roles
        if (nombrePerfil != null) {
            String perfilLower = nombrePerfil.toLowerCase();
            if (perfilLower.contains("super") || perfilLower.contains("superadmin")) {
                return UserRole.SUPER_ADMINISTRADOR;
            } else if (perfilLower.contains("admin") || perfilLower.contains("administrador")) {
                return UserRole.ADMINISTRADOR;
            } else if (perfilLower.contains("operador") || perfilLower.contains("operator")) {
                return UserRole.OPERADOR;
            }
        }
        
        // Por defecto, verificar por ID de perfil
        if (idPerfil == 1) {
            return UserRole.SUPER_ADMINISTRADOR;
        } else if (idPerfil == 2) {
            return UserRole.ADMINISTRADOR;
        } else if (idPerfil == 3) {
            return UserRole.OPERADOR;
        }
        
        return UserRole.USUARIO;
    }
    
    /**
     * Determina el estado de pago del usuario
     */
    private PaymentStatus determinePaymentStatus(String username) {
        try {
            String sql = "SELECT PAYMENT_STATUS FROM USER_PAYMENT_STATUS WHERE NO_USUARIO = ?";
            String status = jdbcTemplate.queryForObject(sql, String.class, username);
            return PaymentStatus.fromCode(status);
        } catch (Exception e) {
            logger.warn("No se pudo determinar estado de pago para usuario: {}, usando PAID por defecto", username);
            return PaymentStatus.PAID; // Por defecto, permitir acceso
        }
    }
    
    /**
     * Actualiza la contraseña a BCrypt
     */
    private void updatePasswordToBCrypt(String username, String password) {
        try {
            String hashedPassword = passwordUtil.encodeBCrypt(password);
            String sql = "UPDATE USUARIOS SET PASSWORD = ? WHERE NO_USUARIO = ?";
            jdbcTemplate.update(sql, hashedPassword, username);
            logger.info("Contraseña actualizada a BCrypt para usuario: {}", username);
        } catch (Exception e) {
            logger.error("Error actualizando contraseña a BCrypt para usuario: {}", username, e);
        }
    }
    
    /**
     * Actualiza el último login del usuario
     */
    private void updateLastLogin(String username) {
        try {
            String sql = "UPDATE USUARIOS SET LAST_LOGIN = SYSDATE WHERE NO_USUARIO = ?";
            jdbcTemplate.update(sql, username);
        } catch (Exception e) {
            logger.warn("Error actualizando último login para usuario: {}", username, e);
        }
    }
    
    /**
     * Configura autenticación en dos pasos para un usuario
     */
    public Map<String, Object> setupTwoFactorAuth(String username) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String secretKey = twoFactorAuthUtil.generateSecretKey();
            String qrCodeUrl = twoFactorAuthUtil.generateQRCodeUrl(username, secretKey, "Cibercom Facturación");
            
            // Guardar clave secreta en la base de datos
            String sql = "UPDATE USUARIOS SET TWO_FACTOR_SECRET = ?, TWO_FACTOR_ENABLED = 'N' WHERE NO_USUARIO = ?";
            jdbcTemplate.update(sql, secretKey, username);
            
            TwoFactorSetupDto setupDto = new TwoFactorSetupDto(qrCodeUrl, secretKey, false);
            setupDto.setMessage("Escanea el código QR con Google Authenticator y luego activa la autenticación");
            
            response.put("success", true);
            response.put("setup", setupDto);
            
        } catch (Exception e) {
            logger.error("Error configurando autenticación en dos pasos: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error configurando autenticación en dos pasos");
        }
        
        return response;
    }
    
    /**
     * Activa la autenticación en dos pasos después de verificar el código
     */
    public Map<String, Object> enableTwoFactorAuth(String username, String code) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Obtener clave secreta del usuario
            String sql = "SELECT TWO_FACTOR_SECRET FROM USUARIOS WHERE NO_USUARIO = ?";
            String secretKey = jdbcTemplate.queryForObject(sql, String.class, username);
            
            if (secretKey == null) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado o clave secreta no configurada");
                return response;
            }
            
            // Verificar código
            if (twoFactorAuthUtil.verifyCode(secretKey, code)) {
                // Activar autenticación en dos pasos
                String updateSql = "UPDATE USUARIOS SET TWO_FACTOR_ENABLED = 'Y' WHERE NO_USUARIO = ?";
                jdbcTemplate.update(updateSql, username);
                
                response.put("success", true);
                response.put("message", "Autenticación en dos pasos activada exitosamente");
            } else {
                response.put("success", false);
                response.put("message", "Código de verificación inválido");
            }
            
        } catch (Exception e) {
            logger.error("Error activando autenticación en dos pasos: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error activando autenticación en dos pasos");
        }
        
        return response;
    }
    
    /**
     * Verifica código de autenticación en dos pasos
     */
    public Map<String, Object> verifyTwoFactorCode(String username, String code) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String sql = "SELECT TWO_FACTOR_SECRET FROM USUARIOS WHERE NO_USUARIO = ? AND TWO_FACTOR_ENABLED = 'Y'";
            String secretKey = jdbcTemplate.queryForObject(sql, String.class, username);
            
            if (secretKey == null) {
                response.put("success", false);
                response.put("message", "Autenticación en dos pasos no configurada");
                return response;
            }
            
            if (twoFactorAuthUtil.verifyCode(secretKey, code)) {
                response.put("success", true);
                response.put("message", "Código verificado exitosamente");
            } else {
                response.put("success", false);
                response.put("message", "Código de verificación inválido");
            }
            
        } catch (Exception e) {
            logger.error("Error verificando código de autenticación en dos pasos: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error verificando código");
        }
        
        return response;
    }
}