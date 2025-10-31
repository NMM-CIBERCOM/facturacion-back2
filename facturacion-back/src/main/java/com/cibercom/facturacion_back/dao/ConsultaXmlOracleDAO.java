package com.cibercom.facturacion_back.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.Optional;

/**
 * DAO para consultar el XML timbrado por UUID desde la tabla CONSULTAS (Oracle).
 * Hace detección dinámica de nombres de columnas habituales para UUID y XML.
 */
@Repository
@Profile("oracle")
public class ConsultaXmlOracleDAO {

    private final JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(ConsultaXmlOracleDAO.class);

    public ConsultaXmlOracleDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    static class ColumnMeta {
        String name;
        String dataType;
        boolean nullable;
        boolean hasDefault;
    }

    private java.util.Map<String, ColumnMeta> fetchColumnsMeta(String tableName) {
        java.util.Map<String, ColumnMeta> meta = new java.util.HashMap<>();
        try {
            String sqlUser = "SELECT UPPER(COLUMN_NAME) AS COLUMN_NAME, UPPER(DATA_TYPE) AS DATA_TYPE, NULLABLE, DATA_DEFAULT " +
                    "FROM USER_TAB_COLUMNS WHERE UPPER(TABLE_NAME) = ?";
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
            }, tableName.toUpperCase());
        } catch (Exception e) {
            logger.warn("No se pudieron consultar columnas en USER_TAB_COLUMNS para {}: {}", tableName, e.getMessage());
        }
        if (meta.isEmpty()) {
            try {
                String sqlAll = "SELECT UPPER(COLUMN_NAME) AS COLUMN_NAME, UPPER(DATA_TYPE) AS DATA_TYPE, NULLABLE, DATA_DEFAULT " +
                        "FROM ALL_TAB_COLUMNS WHERE UPPER(TABLE_NAME) = ? AND UPPER(OWNER) = UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA'))";
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
                }, tableName.toUpperCase());
            } catch (Exception e) {
                logger.warn("No se pudieron consultar columnas en ALL_TAB_COLUMNS para {}: {}", tableName, e.getMessage());
            }
        }
        return meta;
    }

    private java.util.Set<String> availableNames(java.util.Map<String, ColumnMeta> meta) {
        return new java.util.HashSet<>(meta.keySet());
    }

    private String pickFirst(java.util.Set<String> avail, String... candidates) {
        for (String c : candidates) {
            if (avail.contains(c)) return c;
        }
        return null;
    }

    /** Obtiene el XML por UUID desde CONSULTAS si existe. */
    public Optional<String> obtenerXmlPorUuid(String uuid) {
        try {
            java.util.Map<String, ColumnMeta> meta = fetchColumnsMeta("CONSULTAS");
            java.util.Set<String> avail = availableNames(meta);
            if (avail.isEmpty()) {
                logger.info("Tabla CONSULTAS no disponible en el esquema actual");
                return Optional.empty();
            }

            String uuidCol = pickFirst(avail, "UUID", "FOLIO_FISCAL", "FOLIO_FISCAL_SAT", "UUID_CFDI", "FOLIO_FISCAL_UUID");
            if (uuidCol == null) uuidCol = "UUID";

            String xmlExpr = avail.contains("XML_CONTENT") ? "XML_CONTENT" :
                    (avail.contains("XML") ? "XML" :
                            (avail.contains("XML_CFDI") ? "XML_CFDI" :
                                    (avail.contains("XML_TIMBRADO") ? "XML_TIMBRADO" :
                                            (avail.contains("XML_SAT") ? "XML_SAT" : null))));
            if (xmlExpr == null) {
                logger.info("CONSULTAS no tiene columna XML reconocible");
                return Optional.empty();
            }

            String sql = "SELECT " + xmlExpr + " FROM CONSULTAS WHERE " + uuidCol + " = ?";
            String xml = jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString(1) : null, uuid);
            return Optional.ofNullable(xml);
        } catch (Exception e) {
            logger.warn("Fallo consultando XML en CONSULTAS por UUID {}: {}", uuid, e.getMessage());
            return Optional.empty();
        }
    }
}