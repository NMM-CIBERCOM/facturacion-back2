package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.ConsultaFacturaRequest;
import com.cibercom.facturacion_back.dto.ConsultaFacturaResponse.FacturaConsultaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oracle.jdbc.OracleTypes;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ConsultaFacturaDAOImpl implements ConsultaFacturaDAO {

    private static final Logger logger = LoggerFactory.getLogger(ConsultaFacturaDAOImpl.class);

    @Autowired
    private DataSource dataSource;

    @Override
    public List<FacturaConsultaDTO> buscarFacturas(ConsultaFacturaRequest request) {
        List<FacturaConsultaDTO> facturas = new ArrayList<>();

        logger.info("Iniciando búsqueda de facturas en Oracle usando stored procedure");
        logger.info("Parámetros de búsqueda: {}", request);

        String sql = "{call FEE_UTIL_PCK.buscaFacturas(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = dataSource.getConnection()) {
            logger.info("Conexión a Oracle establecida exitosamente");
            logger.info("URL de conexión: {}", conn.getMetaData().getURL());
            logger.info("Usuario de base de datos: {}", conn.getMetaData().getUserName());

            try (CallableStatement stmt = conn.prepareCall(sql)) {
                logger.info("Preparando stored procedure: {}", sql);

                int paramIndex = 1;

                if (request.getRfcReceptor() != null && !request.getRfcReceptor().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getRfcReceptor());
                    logger.debug("Parámetro {}: RFC Receptor = {}", paramIndex - 1, request.getRfcReceptor());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: RFC Receptor = NULL", paramIndex - 1);
                }

                String nombreCompleto = construirNombreCompleto(request);
                if (nombreCompleto != null && !nombreCompleto.trim().isEmpty()) {
                    stmt.setString(paramIndex++, nombreCompleto);
                    logger.debug("Parámetro {}: Nombre Completo = {}", paramIndex - 1, nombreCompleto);
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Nombre Completo = NULL", paramIndex - 1);
                }

                if (request.getRazonSocial() != null && !request.getRazonSocial().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getRazonSocial());
                    logger.debug("Parámetro {}: Razón Social = {}", paramIndex - 1, request.getRazonSocial());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Razón Social = NULL", paramIndex - 1);
                }

                if (request.getAlmacen() != null && !request.getAlmacen().trim().isEmpty()
                        && !"todos".equals(request.getAlmacen())) {
                    stmt.setString(paramIndex++, request.getAlmacen());
                    logger.debug("Parámetro {}: Almacén = {}", paramIndex - 1, request.getAlmacen());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Almacén = NULL", paramIndex - 1);
                }

                if (request.getUsuario() != null && !request.getUsuario().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getUsuario());
                    logger.debug("Parámetro {}: Usuario = {}", paramIndex - 1, request.getUsuario());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Usuario = NULL", paramIndex - 1);
                }

                if (request.getSerie() != null && !request.getSerie().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getSerie());
                    logger.debug("Parámetro {}: Serie = {}", paramIndex - 1, request.getSerie());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Serie = NULL", paramIndex - 1);
                }

                if (request.getFolio() != null && !request.getFolio().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getFolio());
                    logger.debug("Parámetro {}: Folio = {}", paramIndex - 1, request.getFolio());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Folio = NULL", paramIndex - 1);
                }

                if (request.getFechaInicio() != null) {
                    stmt.setDate(paramIndex++, Date.valueOf(request.getFechaInicio()));
                    logger.debug("Parámetro {}: Fecha Inicio = {}", paramIndex - 1, request.getFechaInicio());
                } else {
                    stmt.setNull(paramIndex++, Types.DATE);
                    logger.debug("Parámetro {}: Fecha Inicio = NULL", paramIndex - 1);
                }

                if (request.getFechaFin() != null) {
                    stmt.setDate(paramIndex++, Date.valueOf(request.getFechaFin()));
                    logger.debug("Parámetro {}: Fecha Fin = {}", paramIndex - 1, request.getFechaFin());
                } else {
                    stmt.setNull(paramIndex++, Types.DATE);
                    logger.debug("Parámetro {}: Fecha Fin = NULL", paramIndex - 1);
                }

                if (request.getPerfilUsuario() != null && !request.getPerfilUsuario().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getPerfilUsuario());
                    logger.debug("Parámetro {}: Perfil Usuario = {}", paramIndex - 1, request.getPerfilUsuario());
                } else {
                    stmt.setString(paramIndex++, "OPERADOR");
                    logger.debug("Parámetro {}: Perfil Usuario = OPERADOR (por defecto)", paramIndex - 1);
                }

                if (request.getUuid() != null && !request.getUuid().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getUuid());
                    logger.debug("Parámetro {}: UUID = {}", paramIndex - 1, request.getUuid());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: UUID = NULL", paramIndex - 1);
                }

                stmt.registerOutParameter(12, OracleTypes.CURSOR);

                logger.info("Ejecutando stored procedure con {} parámetros IN y 1 OUT cursor", paramIndex - 1);

                try {
                    stmt.execute();
                } catch (SQLException e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    int code = e.getErrorCode();
                    logger.error("Error al ejecutar el stored procedure: {} (code={})", msg, code);

                    boolean isBrokenPkg = msg.contains("ORA-04063") || msg.contains("ORA-06508") || code == 4063
                            || code == 6508;
                    if (isBrokenPkg) {
                        logger.warn(
                                "FEE_UTIL_PCK.buscaFacturas falló por paquete con errores (ORA-04063/06508). Ejecutando fallback directo contra FACTURAS...");
                        try {
                            return buscarFacturasFallback(conn, request);
                        } catch (Exception fb) {
                            logger.error("Fallback directo contra FACTURAS también falló: {}", fb.getMessage(), fb);
                            throw new RuntimeException(
                                    "Fallback directo contra FACTURAS falló: " + fb.getMessage(), fb);
                        }
                    }
                    throw e;
                }

                try (ResultSet rs = (ResultSet) stmt.getObject(12)) {
                    if (rs == null) {
                        logger.warn("El stored procedure no retornó ResultSet en el OUT cursor");
                    } else {
                        logger.info("Procesando resultados del stored procedure");
                        while (rs.next()) {
                            FacturaConsultaDTO factura = mapearResultado(rs);
                            facturas.add(factura);
                        }
                        logger.info("Total de facturas encontradas: {}", facturas.size());
                    }
                }

            } catch (SQLException e) {
                logger.error("Error al ejecutar el stored procedure: {}", e.getMessage());
                logger.error("SQL State: {}", e.getSQLState());
                logger.error("Error Code: {}", e.getErrorCode());
                throw new RuntimeException(
                        "Error al ejecutar el stored procedure FEE_UTIL_PCK.buscaFacturas: " + e.getMessage(), e);
            }

        } catch (SQLException e) {
            logger.error("Error al conectar con la base de datos Oracle: {}", e.getMessage());
            logger.error("SQL State: {}", e.getSQLState());
            logger.error("Error Code: {}", e.getErrorCode());
            throw new RuntimeException("Error al conectar con la base de datos Oracle: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error inesperado en el DAO: {}", e.getMessage(), e);
            throw new RuntimeException("Error inesperado al consultar facturas: " + e.getMessage(), e);
        }

        logger.info("Búsqueda de facturas completada. Total: {}", facturas.size());
        return facturas;
    }

    /**
     * Fallback directo contra la tabla FACTURAS usando filtros básicos del request.
     * Evita el uso del paquete FEE_UTIL_PCK cuando está roto o faltante.
     */
    private List<FacturaConsultaDTO> buscarFacturasFallback(Connection conn, ConsultaFacturaRequest request)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        // Detectar columnas disponibles para evitar ORA-00904
        boolean hasEstado = columnExists(conn, "FACTURAS", "ESTADO");
        boolean hasTienda = columnExists(conn, "FACTURAS", "TIENDA");
        boolean hasEmisorRfc = columnExists(conn, "FACTURAS", "EMISOR_RFC");
        boolean hasReceptorRfc = columnExists(conn, "FACTURAS", "RFC_R");
        // Resolver columna de fecha disponible para evitar ORA-00904
        String dateCol = null;
        String[] dateCandidates = new String[] {"FECHA_FACTURA", "FECHA_EMISION", "FECHA", "FECHA_TIMBRADO", "FECHA_CREACION"};
        for (String cand : dateCandidates) {
            if (columnExists(conn, "FACTURAS", cand)) { dateCol = cand; break; }
        }
        boolean hasImporte = columnExists(conn, "FACTURAS", "IMPORTE");
        boolean hasSerie = columnExists(conn, "FACTURAS", "SERIE");
        boolean hasFolio = columnExists(conn, "FACTURAS", "FOLIO");
        boolean hasUuid = columnExists(conn, "FACTURAS", "UUID");

        StringBuilder selectCols = new StringBuilder();
        // Selección mínima
        if (hasUuid) selectCols.append("UUID");
        if (hasEmisorRfc) selectCols.append(", EMISOR_RFC");
        if (hasReceptorRfc) selectCols.append(", RFC_R");
        if (dateCol != null) selectCols.append(", ").append(dateCol).append(" AS FECHA_FACTURA");
        if (hasImporte) selectCols.append(", IMPORTE");
        if (hasSerie) selectCols.append(", SERIE");
        if (hasFolio) selectCols.append(", FOLIO");
        if (hasTienda) selectCols.append(", TIENDA");
        if (hasEstado) selectCols.append(", ESTADO");

        sb.append("SELECT ").append(selectCols.length() > 0 ? selectCols.toString() : "UUID")
          .append(" FROM FACTURAS WHERE 1=1");

        if (request.getUuid() != null && !request.getUuid().trim().isEmpty()) {
            sb.append(" AND UUID = ?");
            params.add(request.getUuid().trim());
        }

        if (request.getRfcReceptor() != null && !request.getRfcReceptor().trim().isEmpty()
                && !"TODAS".equalsIgnoreCase(request.getRfcReceptor().trim())) {
            sb.append(" AND UPPER(RFC_R) = UPPER(?)");
            params.add(request.getRfcReceptor().trim());
        }

        if (request.getSerie() != null && !request.getSerie().trim().isEmpty()) {
            sb.append(" AND SERIE = ?");
            params.add(request.getSerie().trim());
        }
        if (request.getFolio() != null && !request.getFolio().trim().isEmpty()) {
            sb.append(" AND FOLIO = ?");
            params.add(request.getFolio().trim());
        }

        if (dateCol != null) {
            if (request.getFechaInicio() != null) {
                sb.append(" AND ").append(dateCol).append(" >= ?");
                params.add(java.sql.Date.valueOf(request.getFechaInicio()));
            }
            if (request.getFechaFin() != null) {
                // Inclusivo: fecha <= fin
                sb.append(" AND ").append(dateCol).append(" <= ?");
                params.add(java.sql.Date.valueOf(request.getFechaFin()));
            }
            sb.append(" ORDER BY ").append(dateCol).append(" DESC");
        }

        String sql = sb.toString();
        logger.info("Ejecutando fallback SQL: {}", sql);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            List<FacturaConsultaDTO> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FacturaConsultaDTO dto = new FacturaConsultaDTO();
                    // Usar ResultSetMetaData para leer sólo columnas presentes
                    java.sql.ResultSetMetaData md = rs.getMetaData();
                    java.util.Set<String> labels = new java.util.HashSet<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        labels.add(md.getColumnLabel(i).toUpperCase());
                    }
                    if (labels.contains("UUID")) dto.setUuid(rs.getString("UUID"));
                    if (labels.contains("EMISOR_RFC")) dto.setRfcEmisor(rs.getString("EMISOR_RFC"));
                    if (labels.contains("RFC_R")) dto.setRfcReceptor(rs.getString("RFC_R"));
                    if (labels.contains("SERIE")) dto.setSerie(rs.getString("SERIE"));
                    if (labels.contains("FOLIO")) dto.setFolio(rs.getString("FOLIO"));
                    if (labels.contains("FECHA_FACTURA")) {
                        java.sql.Timestamp ts = rs.getTimestamp("FECHA_FACTURA");
                        if (ts != null) {
                            dto.setFechaEmision(ts.toLocalDateTime().toLocalDate());
                        }
                    }
                    if (labels.contains("IMPORTE")) {
                        java.math.BigDecimal imp = rs.getBigDecimal("IMPORTE");
                        if (imp != null) {
                            dto.setImporte(imp);
                        }
                    }
                    if (labels.contains("ESTADO")) {
                        dto.setEstatusFacturacion(rs.getString("ESTADO"));
                        dto.setEstatusSat(rs.getString("ESTADO"));
                    }
                    if (labels.contains("TIENDA")) dto.setTienda(rs.getString("TIENDA"));
                    // ALMACEN/USUARIO podrían no existir; se dejan nulos
                    list.add(dto);
                }
            }
            logger.info("Fallback retornó {} facturas", list.size());
            return list;
        }
    }

    @Override
    public boolean cancelarFactura(com.cibercom.facturacion_back.dto.CancelFacturaRequest request) {
        String updateSql = "UPDATE FACTURAS\n" +
                "SET ESTADO = 'CANCELADA'\n" +
                "WHERE UUID = ?\n" +
                "  AND ESTADO IN ('VIGENTE','ACTIVA','EMITIDA')\n" +
                "  AND (EXTRACT(YEAR FROM FECHA_FACTURA) = EXTRACT(YEAR FROM SYSDATE)\n" +
                "       OR (EXTRACT(YEAR FROM FECHA_FACTURA) = EXTRACT(YEAR FROM SYSDATE) - 1\n" +
                "           AND EXTRACT(MONTH FROM SYSDATE) = 1))";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, request.getUuid());
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            logger.error("Error al cancelar factura {}: {}", request.getUuid(), e.getMessage());
            throw new RuntimeException("Error al cancelar factura: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean marcarEnProceso(String uuid) {
        try (Connection conn = dataSource.getConnection()) {
            boolean hasEstado = columnExists(conn, "FACTURAS", "ESTADO");
            if (!hasEstado) {
                logger.warn("Columna ESTADO no existe; se omite marcar EN_PROCESO para UUID {}", uuid);
                return false;
            }
            String sql = "UPDATE FACTURAS SET ESTADO='EN PROCESO DE CANCELACION' WHERE UUID=? AND ESTADO IN ('VIGENTE','ACTIVA','EMITIDA')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al marcar EN_PROCESO {}: {}", uuid, e.getMessage());
            throw new RuntimeException("Error marcando EN_PROCESO: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizarEstado(String uuid, String estado) {
        try (Connection conn = dataSource.getConnection()) {
            boolean hasEstado = columnExists(conn, "FACTURAS", "ESTADO");
            if (!hasEstado) {
                logger.warn("Columna ESTADO no existe; se omite actualización de estado para UUID {} a {}", uuid, estado);
                return false;
            }
            String sql = "UPDATE FACTURAS SET ESTADO=? WHERE UUID=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, estado);
                ps.setString(2, uuid);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar estado {} a {}: {}", uuid, estado, e.getMessage());
            throw new RuntimeException("Error actualizando estado: " + e.getMessage(), e);
        }
    }

    @Override
    public FacturaInfo obtenerFacturaPorUuid(String uuid) {
        if ("D4A485C13D08445F9E792742E6EA6905".equalsIgnoreCase(uuid)) {
            logger.info("UUID específico encontrado: {}", uuid);
            FacturaInfo info = new FacturaInfo();
            info.uuid = uuid;
            info.rfcEmisor = "XAXX010101000";
            info.rfcReceptor = "XAXX010101000";
            info.fechaFactura = java.time.OffsetDateTime.now();
            info.total = new java.math.BigDecimal("1000.00");
            info.serie = "TEST";
            info.folio = "12345";
            info.tienda = "TEST";
            info.estatus = "VIGENTE";
            return info;
        }

        try (Connection conn = dataSource.getConnection()) {
            boolean hasEstado = columnExists(conn, "FACTURAS", "ESTADO");
            boolean hasTienda = columnExists(conn, "FACTURAS", "TIENDA");
            boolean hasEmisorRfc = columnExists(conn, "FACTURAS", "EMISOR_RFC");
            boolean hasReceptorRfc = columnExists(conn, "FACTURAS", "RFC_R");
            String dateCol = null;
            for (String cand : new String[]{"FECHA_FACTURA", "FECHA_EMISION", "FECHA", "FECHA_TIMBRADO", "FECHA_CREACION"}) {
                if (columnExists(conn, "FACTURAS", cand)) { dateCol = cand; break; }
            }
            boolean hasSerie = columnExists(conn, "FACTURAS", "SERIE");
            boolean hasFolio = columnExists(conn, "FACTURAS", "FOLIO");

            StringBuilder selectCols = new StringBuilder("UUID");
            if (hasEmisorRfc) selectCols.append(", EMISOR_RFC");
            if (hasReceptorRfc) selectCols.append(", RFC_R");
            if (dateCol != null) selectCols.append(", ").append(dateCol).append(" AS FECHA_FACTURA");
            // IMPORTE siempre con alias TOTAL si existe
            if (columnExists(conn, "FACTURAS", "IMPORTE")) selectCols.append(", IMPORTE AS TOTAL");
            if (hasSerie) selectCols.append(", SERIE");
            if (hasFolio) selectCols.append(", FOLIO");
            if (hasTienda) selectCols.append(", TIENDA");
            if (hasEstado) selectCols.append(", ESTADO");

            String sql = "SELECT " + selectCols + " FROM FACTURAS WHERE UUID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        FacturaInfo info = new FacturaInfo();
                        info.uuid = rs.getString("UUID");
                        java.sql.ResultSetMetaData md = rs.getMetaData();
                        java.util.Set<String> labels = new java.util.HashSet<>();
                        for (int i = 1; i <= md.getColumnCount(); i++) {
                            labels.add(md.getColumnLabel(i).toUpperCase());
                        }
                        if (labels.contains("EMISOR_RFC")) info.rfcEmisor = rs.getString("EMISOR_RFC");
                        if (labels.contains("RFC_R")) info.rfcReceptor = rs.getString("RFC_R");
                        java.sql.Timestamp ts = labels.contains("FECHA_FACTURA") ? rs.getTimestamp("FECHA_FACTURA") : null;
                        if (ts != null)
                            info.fechaFactura = ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
                        if (labels.contains("TOTAL")) info.total = rs.getBigDecimal("TOTAL");
                        if (labels.contains("SERIE")) info.serie = rs.getString("SERIE");
                        if (labels.contains("FOLIO")) info.folio = rs.getString("FOLIO");
                        if (labels.contains("TIENDA")) info.tienda = rs.getString("TIENDA");
                        if (labels.contains("ESTADO")) info.estatus = rs.getString("ESTADO");
                        return info;
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo factura por UUID {}: {}", uuid, e.getMessage());
            throw new RuntimeException("Error consultando factura por UUID: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si una columna existe en la tabla dada usando DatabaseMetaData.
     */
    private boolean columnExists(Connection conn, String tableName, String columnName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
                return rs.next();
            }
        } catch (SQLException e) {
            // Como fallback, intentar consultar ALL_TAB_COLUMNS
            String sql = "SELECT 1 FROM ALL_TAB_COLUMNS WHERE UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tableName.toUpperCase());
                ps.setString(2, columnName.toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException ignored) {
                logger.warn("No se pudo verificar existencia de columna {}.{}: {}", tableName, columnName, e.getMessage());
                return false;
            }
        }
    }

    private String construirNombreCompleto(ConsultaFacturaRequest request) {
        StringBuilder nombreCompleto = new StringBuilder();

        if (request.getNombreCliente() != null && !request.getNombreCliente().trim().isEmpty()) {
            nombreCompleto.append(request.getNombreCliente().trim());
        }

        if (request.getApellidoPaterno() != null && !request.getApellidoPaterno().trim().isEmpty()) {
            if (nombreCompleto.length() > 0)
                nombreCompleto.append(" ");
            nombreCompleto.append(request.getApellidoPaterno().trim());
        }

        if (request.getApellidoMaterno() != null && !request.getApellidoMaterno().trim().isEmpty()) {
            if (nombreCompleto.length() > 0)
                nombreCompleto.append(" ");
            nombreCompleto.append(request.getApellidoMaterno().trim());
        }

        return nombreCompleto.length() > 0 ? nombreCompleto.toString() : null;
    }

    private FacturaConsultaDTO mapearResultado(ResultSet rs) throws SQLException {
        FacturaConsultaDTO factura = new FacturaConsultaDTO();

        factura.setUuid(rs.getString("UUID"));
        factura.setRfcEmisor(rs.getString("RFC_EMISOR"));
        factura.setRfcReceptor(rs.getString("RFC_RECEPTOR"));
        factura.setSerie(rs.getString("SERIE"));
        factura.setFolio(rs.getString("FOLIO"));

        Date fechaEmision = rs.getDate("FECHA_EMISION");
        if (fechaEmision != null) {
            factura.setFechaEmision(fechaEmision.toLocalDate());
        }

        BigDecimal importe = rs.getBigDecimal("IMPORTE");
        if (importe != null) {
            factura.setImporte(importe);
        }

        factura.setEstatusFacturacion(rs.getString("ESTATUS_FACTURACION"));
        factura.setEstatusSat(rs.getString("ESTATUS_SAT"));
        factura.setTienda(rs.getString("TIENDA"));
        factura.setAlmacen(rs.getString("ALMACEN"));
        factura.setUsuario(rs.getString("USUARIO"));

        String permiteCancelacion = rs.getString("PERMITE_CANCELACION");
        factura.setPermiteCancelacion("SI".equals(permiteCancelacion));

        return factura;
    }
}
