package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.TicketDto;
import com.cibercom.facturacion_back.dto.TicketSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@Profile("oracle")
public class TicketOracleDAO implements TicketDAO {

    private static final Logger logger = LoggerFactory.getLogger(TicketOracleDAO.class);
    private final JdbcTemplate jdbcTemplate;

    public TicketOracleDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TicketDto> buscar(TicketSearchRequest request) {
        // Consulta explícita a la tabla TICKETS usando columnas del esquema proporcionado
        String idCol = "ID_TICKET";
        String tiendaCodigoCol = "CODIGO_TIENDA";
        String tiendaIdCol = "TIENDA_ID";
        String terminalCol = "TERMINAL_ID";
        String fechaCol = "FECHA";
        String folioCol = "FOLIO";
        String subtotalCol = "SUBTOTAL";
        String ivaCol = "IVA";
        String totalCol = "TOTAL";
        String formaPagoCol = "FORMA_PAGO";
        String rfcCol = "RFC_CLIENTE";
        String nombreCol = "NOMBRE_CLIENTE";
        String statusCol = "STATUS";
        String idFacturaCol = "ID_FACTURA";

        String select = "SELECT " +
                expr(idCol, "ID_TICKET") + ", " +
                expr(tiendaCodigoCol, "CODIGO_TIENDA") + ", " +
                expr(tiendaIdCol, "TIENDA_ID") + ", " +
                expr(terminalCol, "TERMINAL_ID") + ", " +
                expr(fechaCol, "FECHA") + ", " +
                expr(folioCol, "FOLIO") + ", " +
                expr(subtotalCol, "SUBTOTAL") + ", " +
                expr(ivaCol, "IVA") + ", " +
                expr(totalCol, "TOTAL") + ", " +
                expr(formaPagoCol, "FORMA_PAGO") + ", " +
                expr(rfcCol, "RFC_CLIENTE") + ", " +
                expr(nombreCol, "NOMBRE_CLIENTE") + ", " +
                expr(statusCol, "STATUS") + ", " +
                expr(idFacturaCol, "ID_FACTURA") +
                " FROM TICKETS WHERE 1=1";

        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(select);

        if (request.getCodigoTienda() != null && !request.getCodigoTienda().trim().isEmpty()) {
            sql.append(" AND ").append(tiendaCodigoCol).append(" = ?");
            params.add(request.getCodigoTienda().trim());
        }
        if (request.getTerminalId() != null) {
            sql.append(" AND ").append(terminalCol).append(" = ?");
            params.add(request.getTerminalId());
        }
        if (request.getFecha() != null && !request.getFecha().trim().isEmpty()) {
            sql.append(" AND TRUNC(").append(fechaCol).append(") = TO_DATE(?, 'YYYY-MM-DD')");
            params.add(request.getFecha().trim());
        }
        if (request.getFolio() != null) {
            sql.append(" AND ").append(folioCol).append(" = ?");
            params.add(request.getFolio());
        }

        logger.info("Consulta TICKETS SQL: {}", sql);
        return jdbcTemplate.query(sql.toString(), params.toArray(), mapper());
    }

    @Override
    public List<TicketDto> buscarPorIdFactura(Long idFactura) {
        if (idFactura == null) {
            throw new IllegalArgumentException("ID_FACTURA no puede ser nulo");
        }
        String idCol = "ID_TICKET";
        String tiendaCodigoCol = "CODIGO_TIENDA";
        String tiendaIdCol = "TIENDA_ID";
        String terminalCol = "TERMINAL_ID";
        String fechaCol = "FECHA";
        String folioCol = "FOLIO";
        String subtotalCol = "SUBTOTAL";
        String ivaCol = "IVA";
        String totalCol = "TOTAL";
        String formaPagoCol = "FORMA_PAGO";
        String rfcCol = "RFC_CLIENTE";
        String nombreCol = "NOMBRE_CLIENTE";
        String statusCol = "STATUS";
        String idFacturaCol = "ID_FACTURA";

        String select = "SELECT " +
                expr(idCol, "ID_TICKET") + ", " +
                expr(tiendaCodigoCol, "CODIGO_TIENDA") + ", " +
                expr(tiendaIdCol, "TIENDA_ID") + ", " +
                expr(terminalCol, "TERMINAL_ID") + ", " +
                expr(fechaCol, "FECHA") + ", " +
                expr(folioCol, "FOLIO") + ", " +
                expr(subtotalCol, "SUBTOTAL") + ", " +
                expr(ivaCol, "IVA") + ", " +
                expr(totalCol, "TOTAL") + ", " +
                expr(formaPagoCol, "FORMA_PAGO") + ", " +
                expr(rfcCol, "RFC_CLIENTE") + ", " +
                expr(nombreCol, "NOMBRE_CLIENTE") + ", " +
                expr(statusCol, "STATUS") + ", " +
                expr(idFacturaCol, "ID_FACTURA") +
                " FROM TICKETS WHERE " + idFacturaCol + " = ?";

        logger.info("Consulta TICKETS por ID_FACTURA SQL: {}", select);
        return jdbcTemplate.query(select, new Object[]{idFactura}, mapper());
    }

    @Override
    public List<TicketDto> buscarPorIdTicket(Long idTicket) {
        if (idTicket == null) {
            throw new IllegalArgumentException("ID_TICKET no puede ser nulo");
        }
        String idCol = "ID_TICKET";
        String tiendaCodigoCol = "CODIGO_TIENDA";
        String tiendaIdCol = "TIENDA_ID";
        String terminalCol = "TERMINAL_ID";
        String fechaCol = "FECHA";
        String folioCol = "FOLIO";
        String subtotalCol = "SUBTOTAL";
        String ivaCol = "IVA";
        String totalCol = "TOTAL";
        String formaPagoCol = "FORMA_PAGO";
        String rfcCol = "RFC_CLIENTE";
        String nombreCol = "NOMBRE_CLIENTE";
        String statusCol = "STATUS";
        String idFacturaCol = "ID_FACTURA";

        String select = "SELECT " +
                expr(idCol, "ID_TICKET") + ", " +
                expr(tiendaCodigoCol, "CODIGO_TIENDA") + ", " +
                expr(tiendaIdCol, "TIENDA_ID") + ", " +
                expr(terminalCol, "TERMINAL_ID") + ", " +
                expr(fechaCol, "FECHA") + ", " +
                expr(folioCol, "FOLIO") + ", " +
                expr(subtotalCol, "SUBTOTAL") + ", " +
                expr(ivaCol, "IVA") + ", " +
                expr(totalCol, "TOTAL") + ", " +
                expr(formaPagoCol, "FORMA_PAGO") + ", " +
                expr(rfcCol, "RFC_CLIENTE") + ", " +
                expr(nombreCol, "NOMBRE_CLIENTE") + ", " +
                expr(statusCol, "STATUS") + ", " +
                expr(idFacturaCol, "ID_FACTURA") +
                " FROM TICKETS WHERE " + idCol + " = ?";

        logger.info("Consulta TICKETS por ID_TICKET SQL: {}", select);
        return jdbcTemplate.query(select, new Object[]{idTicket}, mapper());
    }

    @Override
    public boolean actualizarIdFacturaPorFiltros(TicketSearchRequest request, Long idFactura) {
        if (idFactura == null) {
            logger.warn("ID_FACTURA nulo, no se puede actualizar TICKETS");
            return false;
        }

        // Columnas estándar del esquema actual
        String tiendaCodigoCol = "CODIGO_TIENDA";
        String terminalCol = "TERMINAL_ID";
        String fechaCol = "FECHA";
        String folioCol = "FOLIO";
        String idFacturaCol = "ID_FACTURA";

        // Validar que exista la columna ID_FACTURA en TICKETS
        Map<String, ColumnMeta> meta = fetchColumnsMeta();
        java.util.Set<String> avail = new java.util.HashSet<>(meta.keySet());
        if (!avail.contains(idFacturaCol)) {
            logger.warn("La columna {} no existe en TICKETS; se omite actualización", idFacturaCol);
            return false;
        }

        StringBuilder sql = new StringBuilder("UPDATE TICKETS SET ").append(idFacturaCol).append(" = ? WHERE 1=1");
        List<Object> params = new ArrayList<>();
        params.add(idFactura);

        if (request.getCodigoTienda() != null && !request.getCodigoTienda().trim().isEmpty()) {
            sql.append(" AND ").append(tiendaCodigoCol).append(" = ?");
            params.add(request.getCodigoTienda().trim());
        }
        if (request.getTerminalId() != null) {
            sql.append(" AND ").append(terminalCol).append(" = ?");
            params.add(request.getTerminalId());
        }
        if (request.getFecha() != null && !request.getFecha().trim().isEmpty()) {
            sql.append(" AND TRUNC(").append(fechaCol).append(") = TO_DATE(?, 'YYYY-MM-DD')");
            params.add(request.getFecha().trim());
        }
        if (request.getFolio() != null) {
            sql.append(" AND ").append(folioCol).append(" = ?");
            params.add(request.getFolio());
        }

        logger.info("Actualización TICKETS SQL: {}", sql);
        try {
            int updated = jdbcTemplate.update(sql.toString(), params.toArray());
            logger.info("Filas actualizadas en TICKETS: {}", updated);
            return updated > 0;
        } catch (Exception e) {
            logger.error("Error actualizando TICKETS con ID_FACTURA {}: {}", idFactura, e.getMessage());
            return false;
        }
    }

    private RowMapper<TicketDto> mapper() {
        return new RowMapper<TicketDto>() {
            @Override
            public TicketDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                TicketDto t = new TicketDto();
                t.setIdTicket(getLong(rs, "ID_TICKET"));
                t.setTiendaId(getInt(rs, "TIENDA_ID"));
                t.setCodigoTienda(getString(rs, "CODIGO_TIENDA"));
                t.setTerminalId(getInt(rs, "TERMINAL_ID"));
                Date d = rs.getDate("FECHA");
                t.setFecha(d != null ? d.toLocalDate() : null);
                t.setFolio(getInt(rs, "FOLIO"));
                t.setSubtotal(getBD(rs, "SUBTOTAL"));
                t.setIva(getBD(rs, "IVA"));
                t.setTotal(getBD(rs, "TOTAL"));
                t.setFormaPago(getString(rs, "FORMA_PAGO"));
                t.setRfcCliente(getString(rs, "RFC_CLIENTE"));
                t.setNombreCliente(getString(rs, "NOMBRE_CLIENTE"));
                t.setStatus(getInt(rs, "STATUS"));
                t.setIdFactura(getLong(rs, "ID_FACTURA"));
                return t;
            }
        };
    }

    private java.math.BigDecimal getBD(ResultSet rs, String col) throws SQLException {
        try { return rs.getBigDecimal(col); } catch (SQLException e) { return null; }
    }
    private Integer getInt(ResultSet rs, String col) throws SQLException {
        try {
            int v = rs.getInt(col);
            return rs.wasNull() ? null : v;
        } catch (SQLException e) { return null; }
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

    static class ColumnMeta {
        String name;
        String dataType;
        boolean nullable;
        boolean hasDefault;
    }

    private Map<String, ColumnMeta> fetchColumnsMeta() {
        Map<String, ColumnMeta> meta = new HashMap<>();
        try {
            String sqlUser = "SELECT UPPER(COLUMN_NAME) AS COLUMN_NAME, UPPER(DATA_TYPE) AS DATA_TYPE, NULLABLE, DATA_DEFAULT FROM USER_TAB_COLUMNS WHERE UPPER(TABLE_NAME) = 'TICKETS'";
            jdbcTemplate.query(sqlUser, (org.springframework.jdbc.core.ResultSetExtractor<Void>) rs -> {
                while (rs.next()) {
                    ColumnMeta cm = new ColumnMeta();
                    cm.name = rs.getString("COLUMN_NAME");
                    cm.dataType = rs.getString("DATA_TYPE");
                    cm.nullable = "Y".equalsIgnoreCase(rs.getString("NULLABLE"));
                    String def = null;
                    try { def = rs.getString("DATA_DEFAULT"); } catch (SQLException ignored) {}
                    cm.hasDefault = def != null && !def.isBlank();
                    meta.put(cm.name, cm);
                }
                return null;
            });
        } catch (Exception e) {
            logger.warn("No se pudieron consultar columnas en USER_TAB_COLUMNS TICKETS: {}", e.getMessage());
        }
        if (meta.isEmpty()) {
            try {
                String sqlAll = "SELECT UPPER(COLUMN_NAME) AS COLUMN_NAME, UPPER(DATA_TYPE) AS DATA_TYPE, NULLABLE, DATA_DEFAULT FROM ALL_TAB_COLUMNS WHERE UPPER(TABLE_NAME) = 'TICKETS'";
                jdbcTemplate.query(sqlAll, (org.springframework.jdbc.core.ResultSetExtractor<Void>) rs -> {
                    while (rs.next()) {
                        ColumnMeta cm = new ColumnMeta();
                        cm.name = rs.getString("COLUMN_NAME");
                        cm.dataType = rs.getString("DATA_TYPE");
                        cm.nullable = "Y".equalsIgnoreCase(rs.getString("NULLABLE"));
                        String def = null;
                        try { def = rs.getString("DATA_DEFAULT"); } catch (SQLException ignored) {}
                        cm.hasDefault = def != null && !def.isBlank();
                        meta.put(cm.name, cm);
                    }
                    return null;
                });
            } catch (Exception e) {
                logger.warn("No se pudieron consultar columnas en ALL_TAB_COLUMNS TICKETS: {}", e.getMessage());
            }
        }
        return meta;
    }

    private String pickFirst(Set<String> avail, String... candidates) {
        for (String c : candidates) {
            if (avail.contains(c)) return c;
        }
        return null;
    }

    private String expr(String raw, String alias) {
        if (raw == null) {
            // En ausencia de columna, retornar NULL con alias para mapear seguro
            return "NULL AS " + alias;
        }
        return raw + " AS " + alias;
    }
}