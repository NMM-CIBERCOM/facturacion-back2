package com.cibercom.facturacion_back.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

@Repository
@Profile("oracle")
public class PagoOracleDAO {

    private static final Logger logger = LoggerFactory.getLogger(PagoOracleDAO.class);
    private final JdbcTemplate jdbcTemplate;
    private volatile String lastInsertError;

    public PagoOracleDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record PagoRegistro(
            Long facturaId,
            LocalDate fechaPago,
            BigDecimal monto,
            String formaPago,
            String moneda,
            String uuidPago,
            String relacionUuid,
            Long usuarioRegistro
    ) {}

    public record PagoConsulta(
            Long idPago,
            Long facturaId,
            LocalDate fechaPago,
            BigDecimal monto,
            String formaPago,
            String moneda,
            String uuidPago,
            String relacionUuid,
            Integer parcialidad
    ) {}

    public boolean insertarPago(PagoRegistro registro) {
        lastInsertError = null;
        try {
            String sql = "INSERT INTO PAGOS " +
                    "(FACTURA_ID, FECHA_PAGO, MONTO, FORMA_PAGO, MONEDA, UUID, RELACION_UUID, USUARIO_REGISTRO) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            int updated = jdbcTemplate.update(sql, ps -> {
                ps.setLong(1, registro.facturaId());
                ps.setDate(2, java.sql.Date.valueOf(registro.fechaPago()));
                ps.setBigDecimal(3, registro.monto());
                ps.setString(4, registro.formaPago());
                ps.setString(5, registro.moneda());
                if (registro.uuidPago() != null && !registro.uuidPago().isBlank()) {
                    ps.setString(6, registro.uuidPago());
                } else {
                    ps.setNull(6, Types.VARCHAR);
                }
                ps.setString(7, registro.relacionUuid());
                if (registro.usuarioRegistro() != null) {
                    ps.setLong(8, registro.usuarioRegistro());
                } else {
                    ps.setNull(8, Types.NUMERIC);
                }
            });
            return updated > 0;
        } catch (Exception e) {
            lastInsertError = e.getMessage();
            return false;
        }
    }

    public String getLastInsertError() {
        return lastInsertError;
    }

    /**
     * Obtiene los pagos de un complemento de pago por UUID del complemento
     */
    public List<PagoConsulta> obtenerPagosPorUuidComplemento(String uuidComplemento) {
        try {
            String sql = "SELECT ID_PAGO, FACTURA_ID, FECHA_PAGO, MONTO, FORMA_PAGO, MONEDA, UUID, RELACION_UUID " +
                    "FROM PAGOS WHERE UUID = ? ORDER BY FECHA_PAGO, ID_PAGO";
            
            List<PagoConsulta> pagos = jdbcTemplate.query(sql, new RowMapper<PagoConsulta>() {
                @Override
                public PagoConsulta mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new PagoConsulta(
                            rs.getLong("ID_PAGO"),
                            rs.getLong("FACTURA_ID"),
                            rs.getDate("FECHA_PAGO") != null ? rs.getDate("FECHA_PAGO").toLocalDate() : null,
                            rs.getBigDecimal("MONTO"),
                            rs.getString("FORMA_PAGO"),
                            rs.getString("MONEDA"),
                            rs.getString("UUID"),
                            rs.getString("RELACION_UUID"),
                            rowNum + 1 // Calcular parcialidad basada en el número de fila
                    );
                }
            }, uuidComplemento);
            
            return pagos;
        } catch (Exception e) {
            lastInsertError = e.getMessage();
            logger.error("Error al obtener pagos por UUID complemento {}: {}", uuidComplemento, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Obtiene los pagos por FACTURA_ID (factura original)
     */
    public List<PagoConsulta> obtenerPagosPorFacturaId(Long facturaId) {
        try {
            String sql = "SELECT ID_PAGO, FACTURA_ID, FECHA_PAGO, MONTO, FORMA_PAGO, MONEDA, UUID, RELACION_UUID " +
                    "FROM PAGOS WHERE FACTURA_ID = ? ORDER BY FECHA_PAGO, ID_PAGO";
            
            List<PagoConsulta> pagos = jdbcTemplate.query(sql, new RowMapper<PagoConsulta>() {
                @Override
                public PagoConsulta mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new PagoConsulta(
                            rs.getLong("ID_PAGO"),
                            rs.getLong("FACTURA_ID"),
                            rs.getDate("FECHA_PAGO") != null ? rs.getDate("FECHA_PAGO").toLocalDate() : null,
                            rs.getBigDecimal("MONTO"),
                            rs.getString("FORMA_PAGO"),
                            rs.getString("MONEDA"),
                            rs.getString("UUID"),
                            rs.getString("RELACION_UUID"),
                            rowNum + 1 // Calcular parcialidad basada en el número de fila
                    );
                }
            }, facturaId);
            
            return pagos;
        } catch (Exception e) {
            lastInsertError = e.getMessage();
            logger.error("Error al obtener pagos por FACTURA_ID {}: {}", facturaId, e.getMessage(), e);
            return List.of();
        }
    }
}

