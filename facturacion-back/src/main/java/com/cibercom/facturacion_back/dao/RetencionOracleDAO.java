package com.cibercom.facturacion_back.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("oracle")
public class RetencionOracleDAO {

    private static final Logger logger = LoggerFactory.getLogger(RetencionOracleDAO.class);
    private final JdbcTemplate jdbcTemplate;
    private volatile String lastInsertError;

    public RetencionOracleDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record RetencionRegistro(
            Long facturaId,
            String tipoRetencion,
            String claveRetencion,
            BigDecimal baseRetencion,
            String impuesto,
            BigDecimal montoRetenido,
            Integer periodoMesIni,
            Integer periodoMesFin,
            Integer periodoEjercicio,
            String uuidRetencion,
            LocalDate fechaRetencion,
            String comentarios,
            Long usuarioRegistro
    ) {}

    public record RetencionConsulta(
            Long idRetencion,
            Long facturaId,
            String tipoRetencion,
            String claveRetencion,
            BigDecimal baseRetencion,
            String impuesto,
            BigDecimal montoRetenido,
            Integer periodoMesIni,
            Integer periodoMesFin,
            Integer periodoEjercicio,
            String uuidRetencion,
            LocalDate fechaRetencion,
            String comentarios,
            Long usuarioRegistro,
            LocalDate fechaRegistro
    ) {}

    public Optional<Long> insertarRetencion(RetencionRegistro registro) {
        lastInsertError = null;
        try {
            String sql = "INSERT INTO RETENCIONES " +
                    "(ID_FACTURA, TIPO_RETENCION, CLAVE_RETENCION, BASE_RETENCION, IMPUESTO, " +
                    "MONTO_RETENIDO, PERIODO_MES_INI, PERIODO_MES_FIN, PERIODO_EJERCICIO, " +
                    "UUID_RETENCION, FECHA_RETENCION, COMENTARIOS, USUARIO_REGISTRO) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            
            int updated = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"ID_RETENCION"});
                ps.setLong(1, registro.facturaId());
                ps.setString(2, registro.tipoRetencion());
                if (registro.claveRetencion() != null && !registro.claveRetencion().isBlank()) {
                    ps.setString(3, registro.claveRetencion());
                } else {
                    ps.setNull(3, Types.VARCHAR);
                }
                ps.setBigDecimal(4, registro.baseRetencion());
                if (registro.impuesto() != null && !registro.impuesto().isBlank()) {
                    ps.setString(5, registro.impuesto());
                } else {
                    ps.setNull(5, Types.VARCHAR);
                }
                ps.setBigDecimal(6, registro.montoRetenido());
                if (registro.periodoMesIni() != null) {
                    ps.setInt(7, registro.periodoMesIni());
                } else {
                    ps.setNull(7, Types.NUMERIC);
                }
                if (registro.periodoMesFin() != null) {
                    ps.setInt(8, registro.periodoMesFin());
                } else {
                    ps.setNull(8, Types.NUMERIC);
                }
                if (registro.periodoEjercicio() != null) {
                    ps.setInt(9, registro.periodoEjercicio());
                } else {
                    ps.setNull(9, Types.NUMERIC);
                }
                if (registro.uuidRetencion() != null && !registro.uuidRetencion().isBlank()) {
                    ps.setString(10, registro.uuidRetencion());
                } else {
                    ps.setNull(10, Types.VARCHAR);
                }
                if (registro.fechaRetencion() != null) {
                    ps.setDate(11, java.sql.Date.valueOf(registro.fechaRetencion()));
                } else {
                    ps.setDate(11, java.sql.Date.valueOf(LocalDate.now()));
                }
                if (registro.comentarios() != null && !registro.comentarios().isBlank()) {
                    ps.setString(12, registro.comentarios());
                } else {
                    ps.setNull(12, Types.VARCHAR);
                }
                if (registro.usuarioRegistro() != null) {
                    ps.setLong(13, registro.usuarioRegistro());
                } else {
                    ps.setNull(13, Types.NUMERIC);
                }
                return ps;
            }, keyHolder);

            if (updated > 0 && keyHolder.getKey() != null) {
                return Optional.of(keyHolder.getKey().longValue());
            }
            return Optional.empty();
        } catch (Exception e) {
            lastInsertError = e.getMessage();
            logger.error("Error al insertar retención: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public boolean actualizarUuidRetencion(Long idRetencion, String uuidRetencion) {
        lastInsertError = null;
        try {
            String sql = "UPDATE RETENCIONES SET UUID_RETENCION = ? WHERE ID_RETENCION = ?";
            int updated = jdbcTemplate.update(sql, uuidRetencion, idRetencion);
            return updated > 0;
        } catch (Exception e) {
            lastInsertError = e.getMessage();
            logger.error("Error al actualizar UUID de retención {}: {}", idRetencion, e.getMessage(), e);
            return false;
        }
    }

    public Optional<RetencionConsulta> obtenerRetencionPorId(Long idRetencion) {
        try {
            String sql = "SELECT ID_RETENCION, ID_FACTURA, TIPO_RETENCION, CLAVE_RETENCION, " +
                    "BASE_RETENCION, IMPUESTO, MONTO_RETENIDO, PERIODO_MES_INI, PERIODO_MES_FIN, " +
                    "PERIODO_EJERCICIO, UUID_RETENCION, FECHA_RETENCION, COMENTARIOS, " +
                    "USUARIO_REGISTRO, FECHA_REGISTRO " +
                    "FROM RETENCIONES WHERE ID_RETENCION = ?";
            
            List<RetencionConsulta> resultados = jdbcTemplate.query(sql, new RowMapper<RetencionConsulta>() {
                @Override
                public RetencionConsulta mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new RetencionConsulta(
                            rs.getLong("ID_RETENCION"),
                            rs.getLong("ID_FACTURA"),
                            rs.getString("TIPO_RETENCION"),
                            rs.getString("CLAVE_RETENCION"),
                            rs.getBigDecimal("BASE_RETENCION"),
                            rs.getString("IMPUESTO"),
                            rs.getBigDecimal("MONTO_RETENIDO"),
                            rs.getObject("PERIODO_MES_INI", Integer.class),
                            rs.getObject("PERIODO_MES_FIN", Integer.class),
                            rs.getObject("PERIODO_EJERCICIO", Integer.class),
                            rs.getString("UUID_RETENCION"),
                            rs.getDate("FECHA_RETENCION") != null ? rs.getDate("FECHA_RETENCION").toLocalDate() : null,
                            rs.getString("COMENTARIOS"),
                            rs.getObject("USUARIO_REGISTRO", Long.class),
                            rs.getDate("FECHA_REGISTRO") != null ? rs.getDate("FECHA_REGISTRO").toLocalDate() : null
                    );
                }
            }, idRetencion);
            
            return resultados.isEmpty() ? Optional.empty() : Optional.of(resultados.get(0));
        } catch (Exception e) {
            lastInsertError = e.getMessage();
            logger.error("Error al obtener retención por ID {}: {}", idRetencion, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<RetencionConsulta> obtenerRetencionPorUuid(String uuidRetencion) {
        try {
            String sql = "SELECT ID_RETENCION, ID_FACTURA, TIPO_RETENCION, CLAVE_RETENCION, " +
                    "BASE_RETENCION, IMPUESTO, MONTO_RETENIDO, PERIODO_MES_INI, PERIODO_MES_FIN, " +
                    "PERIODO_EJERCICIO, UUID_RETENCION, FECHA_RETENCION, COMENTARIOS, " +
                    "USUARIO_REGISTRO, FECHA_REGISTRO " +
                    "FROM RETENCIONES WHERE UUID_RETENCION = ?";
            
            List<RetencionConsulta> resultados = jdbcTemplate.query(sql, new RowMapper<RetencionConsulta>() {
                @Override
                public RetencionConsulta mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new RetencionConsulta(
                            rs.getLong("ID_RETENCION"),
                            rs.getLong("ID_FACTURA"),
                            rs.getString("TIPO_RETENCION"),
                            rs.getString("CLAVE_RETENCION"),
                            rs.getBigDecimal("BASE_RETENCION"),
                            rs.getString("IMPUESTO"),
                            rs.getBigDecimal("MONTO_RETENIDO"),
                            rs.getObject("PERIODO_MES_INI", Integer.class),
                            rs.getObject("PERIODO_MES_FIN", Integer.class),
                            rs.getObject("PERIODO_EJERCICIO", Integer.class),
                            rs.getString("UUID_RETENCION"),
                            rs.getDate("FECHA_RETENCION") != null ? rs.getDate("FECHA_RETENCION").toLocalDate() : null,
                            rs.getString("COMENTARIOS"),
                            rs.getObject("USUARIO_REGISTRO", Long.class),
                            rs.getDate("FECHA_REGISTRO") != null ? rs.getDate("FECHA_REGISTRO").toLocalDate() : null
                    );
                }
            }, uuidRetencion);
            
            return resultados.isEmpty() ? Optional.empty() : Optional.of(resultados.get(0));
        } catch (Exception e) {
            lastInsertError = e.getMessage();
            logger.error("Error al obtener retención por UUID {}: {}", uuidRetencion, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public String getLastInsertError() {
        return lastInsertError;
    }
}

