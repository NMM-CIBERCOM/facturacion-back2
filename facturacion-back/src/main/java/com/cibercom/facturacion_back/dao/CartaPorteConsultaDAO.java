package com.cibercom.facturacion_back.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@Profile("oracle")
public class CartaPorteConsultaDAO {

    private static final Logger logger = LoggerFactory.getLogger(CartaPorteConsultaDAO.class);

    private final JdbcTemplate jdbcTemplate;

    public CartaPorteConsultaDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CartaPorteRow> buscarPorRangoFechas(LocalDate inicio, LocalDate fin) {
        String select = "SELECT ID_DATO_FISCAL, RFC, RAZON_SOCIAL, NUMERO_SERIE, PRECIO, FECHA_SALIDA, FECHA_LLEGADA FROM CARTA_PORTE WHERE 1=1";
        List<Object> params = new ArrayList<>();
        StringBuilder sb = new StringBuilder(select);
        if (inicio != null) {
            sb.append(" AND TRUNC(FECHA_SALIDA) >= TRUNC(TO_DATE(?, 'YYYY-MM-DD'))");
            params.add(inicio.toString());
        }
        if (fin != null) {
            sb.append(" AND TRUNC(FECHA_SALIDA) <= TRUNC(TO_DATE(?, 'YYYY-MM-DD'))");
            params.add(fin.toString());
        }
        sb.append(" ORDER BY FECHA_SALIDA");
        logger.info("Consulta CARTA_PORTE SQL: {}", sb);
        return jdbcTemplate.query(sb.toString(), params.toArray(), mapper());
    }

    private RowMapper<CartaPorteRow> mapper() {
        return new RowMapper<CartaPorteRow>() {
            @Override
            public CartaPorteRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                CartaPorteRow r = new CartaPorteRow();
                r.id = getLong(rs, "ID_DATO_FISCAL");
                r.rfc = getString(rs, "RFC");
                r.razonSocial = getString(rs, "RAZON_SOCIAL");
                r.numeroSerie = getString(rs, "NUMERO_SERIE");
                try {
                    java.sql.Timestamp ts = rs.getTimestamp("FECHA_SALIDA");
                    if (ts != null) r.fechaSalida = ts.toLocalDateTime().toLocalDate();
                } catch (SQLException ignored) {}
                try {
                    java.sql.Timestamp ts = rs.getTimestamp("FECHA_LLEGADA");
                    if (ts != null) r.fechaLlegada = ts.toLocalDateTime().toLocalDate();
                } catch (SQLException ignored) {}
                try { r.precio = rs.getBigDecimal("PRECIO"); } catch (SQLException ignored) {}
                return r;
            }
        };
    }

    private Long getLong(ResultSet rs, String col) throws SQLException {
        try {
            long v = rs.getLong(col);
            return rs.wasNull() ? null : v;
        } catch (SQLException e) { return null; }
    }
    private String getString(ResultSet rs, String col) throws SQLException {
        try { return rs.getString(col); } catch (SQLException e) { return null; }
    }

    public static class CartaPorteRow {
        public Long id;
        public String rfc;
        public String razonSocial;
        public String numeroSerie;
        public java.math.BigDecimal precio;
        public LocalDate fechaSalida;
        public LocalDate fechaLlegada;
    }
}