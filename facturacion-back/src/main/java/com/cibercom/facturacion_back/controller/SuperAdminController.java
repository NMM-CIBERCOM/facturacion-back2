package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.model.PaymentStatus;
import com.cibercom.facturacion_back.model.UserRole;
import com.cibercom.facturacion_back.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador REST para la gestión de pagos y super administrador
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class SuperAdminController {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Verifica si el usuario es super administrador
     */
    private boolean isSuperAdmin(String token) {
        try {
            String role = jwtUtil.getRoleFromToken(token);
            return UserRole.SUPER_ADMINISTRADOR.getCode().equals(role);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene el estado de pago de todos los usuarios
     */
    @GetMapping("/payment-status")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@RequestHeader("Authorization") String token) {
        try {
            if (!isSuperAdmin(token)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Acceso denegado: Se requieren permisos de Super Administrador"
                ));
            }

            String sql = "SELECT u.NO_USUARIO, u.NOMBRE_EMPLEADO, " +
                        "COALESCE(ups.PAYMENT_STATUS, 'PAID') as PAYMENT_STATUS, " +
                        "ups.LAST_PAYMENT_DATE, ups.NEXT_PAYMENT_DATE, ups.AMOUNT " +
                        "FROM USUARIOS u " +
                        "LEFT JOIN USER_PAYMENT_STATUS ups ON u.NO_USUARIO = ups.NO_USUARIO " +
                        "WHERE u.ESTATUS_USUARIO = 'A' " +
                        "ORDER BY u.NOMBRE_EMPLEADO";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "users", users,
                "total", users.size()
            ));

        } catch (Exception e) {
            logger.error("Error obteniendo estado de pagos: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    /**
     * Actualiza el estado de pago de un usuario
     */
    @PostMapping("/payment-status")
    public ResponseEntity<Map<String, Object>> updatePaymentStatus(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            if (!isSuperAdmin(token)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Acceso denegado: Se requieren permisos de Super Administrador"
                ));
            }

            String username = (String) request.get("username");
            String status = (String) request.get("status");
            Double amount = request.get("amount") != null ? Double.valueOf(request.get("amount").toString()) : null;
            String paymentMethod = (String) request.get("paymentMethod");
            String notes = (String) request.get("notes");

            if (username == null || status == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Username y status son requeridos"
                ));
            }

            // Validar estado de pago
            try {
                PaymentStatus.fromCode(status);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Estado de pago inválido"
                ));
            }

            // Insertar o actualizar estado de pago
            String sql = "MERGE INTO USER_PAYMENT_STATUS ups " +
                        "USING (SELECT ? as NO_USUARIO FROM DUAL) u " +
                        "ON (ups.NO_USUARIO = u.NO_USUARIO) " +
                        "WHEN MATCHED THEN " +
                        "  UPDATE SET PAYMENT_STATUS = ?, AMOUNT = ?, PAYMENT_METHOD = ?, " +
                        "           NOTES = ?, UPDATED_AT = SYSDATE, UPDATED_BY = ? " +
                        "WHEN NOT MATCHED THEN " +
                        "  INSERT (NO_USUARIO, PAYMENT_STATUS, AMOUNT, PAYMENT_METHOD, NOTES, " +
                        "          CREATED_AT, UPDATED_AT, UPDATED_BY) " +
                        "  VALUES (?, ?, ?, ?, ?, SYSDATE, SYSDATE, ?)";

            String updatedBy = jwtUtil.getUsernameFromToken(token);
            int result = jdbcTemplate.update(sql, username, status, amount, paymentMethod, notes, updatedBy,
                                           username, status, amount, paymentMethod, notes, updatedBy);

            if (result > 0) {
                logger.info("Estado de pago actualizado para usuario: {} a {}", username, status);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estado de pago actualizado exitosamente"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No se pudo actualizar el estado de pago"
                ));
            }

        } catch (Exception e) {
            logger.error("Error actualizando estado de pago: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    /**
     * Deniega acceso a usuarios por falta de pago
     */
    @PostMapping("/deny-access")
    public ResponseEntity<Map<String, Object>> denyAccess(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            if (!isSuperAdmin(token)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Acceso denegado: Se requieren permisos de Super Administrador"
                ));
            }

            String username = (String) request.get("username");
            String reason = (String) request.get("reason");

            if (username == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Username es requerido"
                ));
            }

            // Actualizar estado de pago a SUSPENDED
            String sql = "MERGE INTO USER_PAYMENT_STATUS ups " +
                        "USING (SELECT ? as NO_USUARIO FROM DUAL) u " +
                        "ON (ups.NO_USUARIO = u.NO_USUARIO) " +
                        "WHEN MATCHED THEN " +
                        "  UPDATE SET PAYMENT_STATUS = 'SUSPENDED', NOTES = ?, UPDATED_AT = SYSDATE " +
                        "WHEN NOT MATCHED THEN " +
                        "  INSERT (NO_USUARIO, PAYMENT_STATUS, NOTES, CREATED_AT, UPDATED_AT) " +
                        "  VALUES (?, 'SUSPENDED', ?, SYSDATE, SYSDATE)";

            int result = jdbcTemplate.update(sql, username, reason, username, reason);

            if (result > 0) {
                logger.info("Acceso denegado para usuario: {} - Razón: {}", username, reason);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Acceso denegado exitosamente"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No se pudo denegar el acceso"
                ));
            }

        } catch (Exception e) {
            logger.error("Error denegando acceso: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    /**
     * Restaura acceso a usuarios
     */
    @PostMapping("/restore-access")
    public ResponseEntity<Map<String, Object>> restoreAccess(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            if (!isSuperAdmin(token)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Acceso denegado: Se requieren permisos de Super Administrador"
                ));
            }

            String username = (String) request.get("username");
            String status = (String) request.get("status");

            if (username == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Username es requerido"
                ));
            }

            // Usar PAID por defecto si no se especifica
            if (status == null) {
                status = "PAID";
            }

            String sql = "UPDATE USER_PAYMENT_STATUS SET PAYMENT_STATUS = ?, UPDATED_AT = SYSDATE " +
                        "WHERE NO_USUARIO = ?";

            int result = jdbcTemplate.update(sql, status, username);

            if (result > 0) {
                logger.info("Acceso restaurado para usuario: {} con estado: {}", username, status);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Acceso restaurado exitosamente"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Usuario no encontrado o no se pudo restaurar el acceso"
                ));
            }

        } catch (Exception e) {
            logger.error("Error restaurando acceso: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    /**
     * Obtiene estadísticas de pagos
     */
    @GetMapping("/payment-stats")
    public ResponseEntity<Map<String, Object>> getPaymentStats(@RequestHeader("Authorization") String token) {
        try {
            if (!isSuperAdmin(token)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Acceso denegado: Se requieren permisos de Super Administrador"
                ));
            }

            String sql = "SELECT " +
                        "COUNT(*) as TOTAL_USERS, " +
                        "SUM(CASE WHEN COALESCE(ups.PAYMENT_STATUS, 'PAID') = 'PAID' THEN 1 ELSE 0 END) as PAID_USERS, " +
                        "SUM(CASE WHEN ups.PAYMENT_STATUS = 'PENDING' THEN 1 ELSE 0 END) as PENDING_USERS, " +
                        "SUM(CASE WHEN ups.PAYMENT_STATUS = 'OVERDUE' THEN 1 ELSE 0 END) as OVERDUE_USERS, " +
                        "SUM(CASE WHEN ups.PAYMENT_STATUS = 'SUSPENDED' THEN 1 ELSE 0 END) as SUSPENDED_USERS " +
                        "FROM USUARIOS u " +
                        "LEFT JOIN USER_PAYMENT_STATUS ups ON u.NO_USUARIO = ups.NO_USUARIO " +
                        "WHERE u.ESTATUS_USUARIO = 'A'";

            Map<String, Object> stats = jdbcTemplate.queryForMap(sql);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "stats", stats
            ));

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas de pagos: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    /**
     * Health check del controlador de super administrador
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("Health check solicitado para SuperAdminController");
        
        Map<String, Object> response = Map.of(
            "success", true,
            "message", "SuperAdminController funcionando correctamente",
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(response);
    }
}
