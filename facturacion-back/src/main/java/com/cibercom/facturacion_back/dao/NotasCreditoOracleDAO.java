package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.CreditNoteSaveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para inserción dinámica en NOTAS_CREDITO (Oracle).
 * Se adapta a variantes de columnas presentes en el schema.
 */
@Repository
@Profile("oracle")
public class NotasCreditoOracleDAO {

    private final JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(NotasCreditoOracleDAO.class);
    private volatile String lastInsertError;

    public NotasCreditoOracleDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    static class ColumnMeta {
        String name;
        String dataType;
        boolean nullable;
        boolean hasDefault;
    }

    private java.util.Map<String, ColumnMeta> fetchColumnsMeta() {
        java.util.Map<String, ColumnMeta> meta = new java.util.HashMap<>();
        try {
            String sqlUser = "SELECT UPPER(COLUMN_NAME) AS COLUMN_NAME, UPPER(DATA_TYPE) AS DATA_TYPE, NULLABLE, DATA_DEFAULT FROM USER_TAB_COLUMNS WHERE UPPER(TABLE_NAME) = 'NOTAS_CREDITO'";
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
            logger.warn("No se pudieron consultar columnas en USER_TAB_COLUMNS para NOTAS_CREDITO: {}", e.getMessage());
        }
        if (meta.isEmpty()) {
            try {
                String sqlAll = "SELECT UPPER(COLUMN_NAME) AS COLUMN_NAME, UPPER(DATA_TYPE) AS DATA_TYPE, NULLABLE, DATA_DEFAULT FROM ALL_TAB_COLUMNS WHERE UPPER(TABLE_NAME) = 'NOTAS_CREDITO'";
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
                logger.warn("No se pudieron consultar columnas en ALL_TAB_COLUMNS para NOTAS_CREDITO: {}", e.getMessage());
            }
        }
        return meta;
    }

    private java.util.Set<String> availableNames(java.util.Map<String, ColumnMeta> meta) {
        return new java.util.HashSet<>(meta.keySet());
    }

    private boolean hasColumn(String table, String column) {
        try {
            Integer c = jdbcTemplate.query(
                    "SELECT COUNT(*) AS C FROM ALL_TAB_COLUMNS WHERE UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?",
                    rs -> rs.next() ? rs.getInt("C") : 0,
                    table.toUpperCase(), column.toUpperCase()
            );
            return c != null && c > 0;
        } catch (Exception e) { return false; }
    }

    private String pickFirst(java.util.Set<String> avail, String... candidates) {
        for (String c : candidates) {
            if (avail.contains(c)) return c;
        }
        return null;
    }

    /** Obtiene ID_FACTURA por UUID en FACTURAS si la columna existe */
    private java.util.Optional<Long> obtenerIdFacturaPorUuid(String uuid) {
        try {
            // Detectar nombre de columna UUID en FACTURAS
            String uuidCol = null;
            java.util.Set<String> fAvail = new java.util.HashSet<>();
            try {
                jdbcTemplate.query(
                        "SELECT UPPER(COLUMN_NAME) AS COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE UPPER(TABLE_NAME)='FACTURAS'",
                        rs -> {
                            while (rs.next()) fAvail.add(rs.getString("COLUMN_NAME"));
                            return null;
                        }
                );
            } catch (Exception ignored) {}
            uuidCol = pickFirst(fAvail, "UUID", "FOLIO_FISCAL", "FOLIO_FISCAL_SAT", "UUID_CFDI", "FOLIO_FISCAL_UUID");
            if (uuidCol == null || !hasColumn("FACTURAS", "ID_FACTURA")) return java.util.Optional.empty();

            Long id = jdbcTemplate.query(
                    "SELECT ID_FACTURA FROM FACTURAS WHERE " + uuidCol + " = ?",
                    rs -> rs.next() ? rs.getLong("ID_FACTURA") : null,
                    uuid
            );
            return java.util.Optional.ofNullable(id);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    /** Obtiene ID_FACTURA por SERIE/FOLIO si existen en FACTURAS */
    private java.util.Optional<Long> obtenerIdFacturaPorSerieFolio(String serie, String folio) {
        try {
            if (serie == null || folio == null) return java.util.Optional.empty();
            if (!hasColumn("FACTURAS", "ID_FACTURA") || !hasColumn("FACTURAS", "SERIE") || !hasColumn("FACTURAS", "FOLIO")) {
                return java.util.Optional.empty();
            }
            Long id = jdbcTemplate.query(
                    "SELECT ID_FACTURA FROM FACTURAS WHERE SERIE = ? AND FOLIO = ?",
                    rs -> rs.next() ? rs.getLong("ID_FACTURA") : null,
                    serie, folio
            );
            return java.util.Optional.ofNullable(id);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    /** Resuelve ID_FACTURA_ORIG a partir de los datos del request */
    private Long resolverIdFacturaOrig(CreditNoteSaveRequest req) {
        // 1) Intentar con UUID
        if (req.getUuidFacturaOrig() != null && !req.getUuidFacturaOrig().isBlank()) {
            java.util.Optional<Long> byUuid = obtenerIdFacturaPorUuid(req.getUuidFacturaOrig());
            if (byUuid.isPresent()) return byUuid.get();
        }
        // 2) Intentar con SERIE/FOLIO
        if (req.getSerieFacturaOrig() != null && req.getFolioFacturaOrig() != null) {
            java.util.Optional<Long> bySf = obtenerIdFacturaPorSerieFolio(req.getSerieFacturaOrig(), req.getFolioFacturaOrig());
            if (bySf.isPresent()) return bySf.get();
        }
        return null; // No resuelto
    }

    private void addIfPresentWithFallback(java.util.Map<String, ColumnMeta> meta,
                                          java.util.Set<String> avail,
                                          List<String> cols,
                                          List<Object> vals,
                                          String name,
                                          Object value,
                                          Object fallback) {
        if (!avail.contains(name)) return;
        ColumnMeta cm = meta.get(name);
        boolean requireValue = cm != null && !cm.nullable && !cm.hasDefault;
        Object v = value;
        if (v == null && requireValue) {
            v = fallback;
        }
        if (v != null) {
            cols.add(name);
            vals.add(v);
        }
    }

    /**
     * Inserta una nota de crédito en NOTAS_CREDITO con columnas dinámicas.
     */
    public boolean insertar(CreditNoteSaveRequest req) {
        lastInsertError = null;
        try {
            java.util.Map<String, ColumnMeta> meta = fetchColumnsMeta();
            java.util.Set<String> avail = availableNames(meta);
            if (avail.isEmpty()) { lastInsertError = "Sin columnas visibles en NOTAS_CREDITO"; return false; }

            List<String> cols = new ArrayList<>();
            List<Object> vals = new ArrayList<>();

            // UUID de la NC en columnas posibles
            addIfPresentWithFallback(meta, avail, cols, vals, "UUID_NC", req.getUuidNc(), req.getUuidNc());
            addIfPresentWithFallback(meta, avail, cols, vals, "UUID", req.getUuidNc(), req.getUuidNc());
            addIfPresentWithFallback(meta, avail, cols, vals, "FOLIO_FISCAL", req.getUuidNc(), req.getUuidNc());
            addIfPresentWithFallback(meta, avail, cols, vals, "UUID_CFDI", req.getUuidNc(), req.getUuidNc());

            // Serie/Folio y fechas (variantes de nombres posibles en distintos esquemas)
            addIfPresentWithFallback(meta, avail, cols, vals, "SERIE", req.getSerieNc(), req.getSerieNc());
            addIfPresentWithFallback(meta, avail, cols, vals, "FOLIO", req.getFolioNc(), req.getFolioNc());
            addIfPresentWithFallback(meta, avail, cols, vals, "SERIE_NC", req.getSerieNc(), req.getSerieNc());
            addIfPresentWithFallback(meta, avail, cols, vals, "NC_SERIE", req.getSerieNc(), req.getSerieNc());
            addIfPresentWithFallback(meta, avail, cols, vals, "FOLIO_NC", req.getFolioNc(), req.getFolioNc());
            addIfPresentWithFallback(meta, avail, cols, vals, "NC_FOLIO", req.getFolioNc(), req.getFolioNc());
            Timestamp nowTs = Timestamp.valueOf(req.getFechaEmision() != null ? req.getFechaEmision() : LocalDateTime.now());
            addIfPresentWithFallback(meta, avail, cols, vals, "FECHA_EMISION", nowTs, nowTs);
            addIfPresentWithFallback(meta, avail, cols, vals, "FECHA_GENERACION", nowTs, nowTs);
            addIfPresentWithFallback(meta, avail, cols, vals, "FECHA_TIMBRADO", nowTs, nowTs);

            // Contenido XML (en texto/base64 según esquema)
            addIfPresentWithFallback(meta, avail, cols, vals, "XML_CONTENT", req.getXmlContent(), req.getXmlContent());
            addIfPresentWithFallback(meta, avail, cols, vals, "XML_BASE64", req.getXmlContent(), req.getXmlContent());

            // Importes
            addIfPresentWithFallback(meta, avail, cols, vals, "SUBTOTAL", req.getSubtotal(), req.getSubtotal());
            addIfPresentWithFallback(meta, avail, cols, vals, "IVA", req.getIvaImporte(), req.getIvaImporte());
            addIfPresentWithFallback(meta, avail, cols, vals, "IEPS", req.getIepsImporte(), req.getIepsImporte());
            addIfPresentWithFallback(meta, avail, cols, vals, "TOTAL", req.getTotal(), req.getTotal());

            // Pagos/CFDI
            addIfPresentWithFallback(meta, avail, cols, vals, "FORMA_PAGO", req.getFormaPago(), req.getFormaPago());
            addIfPresentWithFallback(meta, avail, cols, vals, "METODO_PAGO", req.getMetodoPago(), req.getMetodoPago());
            addIfPresentWithFallback(meta, avail, cols, vals, "USO_CFDI", req.getUsoCfdi(), req.getUsoCfdi());

            // RFCs
            addIfPresentWithFallback(meta, avail, cols, vals, "RFC_EMISOR", req.getRfcEmisor(), req.getRfcEmisor());
            addIfPresentWithFallback(meta, avail, cols, vals, "RFC_RECEPTOR", req.getRfcReceptor(), req.getRfcReceptor());

            // Estado/Estatus y tipo
            addIfPresentWithFallback(meta, avail, cols, vals, "ESTADO", "EMITIDA", "EMITIDA");
            addIfPresentWithFallback(meta, avail, cols, vals, "ESTATUS", "EMITIDA", "EMITIDA");
            addIfPresentWithFallback(meta, avail, cols, vals, "TIPO_COMPROBANTE", "E", "E");

            // Motivo y relación a factura de origen
            addIfPresentWithFallback(meta, avail, cols, vals, "MOTIVO", req.getMotivo(), req.getMotivo());
            addIfPresentWithFallback(meta, avail, cols, vals, "UUID_FACTURA_ORIG", req.getUuidFacturaOrig(), req.getUuidFacturaOrig());
            addIfPresentWithFallback(meta, avail, cols, vals, "FACTURA_ORIG_UUID", req.getUuidFacturaOrig(), req.getUuidFacturaOrig());
            addIfPresentWithFallback(meta, avail, cols, vals, "REL_UUID", req.getUuidFacturaOrig(), req.getUuidFacturaOrig());
            addIfPresentWithFallback(meta, avail, cols, vals, "SERIE_ORIG", req.getSerieFacturaOrig(), req.getSerieFacturaOrig());
            addIfPresentWithFallback(meta, avail, cols, vals, "FOLIO_ORIG", req.getFolioFacturaOrig(), req.getFolioFacturaOrig());

            // Incluir ID_FACTURA_ORIG respetando nulabilidad; si es NOT NULL y no resolvible, abortar
            if (avail.contains("ID_FACTURA_ORIG")) {
                ColumnMeta cmId = meta.get("ID_FACTURA_ORIG");
                boolean requireId = cmId != null && !cmId.nullable && !cmId.hasDefault;
                Long idOrig = resolverIdFacturaOrig(req);
                if (idOrig != null) {
                    cols.add("ID_FACTURA_ORIG");
                    vals.add(idOrig);
                } else if (requireId) {
                    lastInsertError = "ID_FACTURA_ORIG (NOT NULL) no resuelto desde UUID/SERIE/FOLIO";
                    logger.warn("ID_FACTURA_ORIG requerido pero no resuelto para uuidNc={} uuidFacturaOrig={} serieOrig={} folioOrig={}",
                            req.getUuidNc(), req.getUuidFacturaOrig(), req.getSerieFacturaOrig(), req.getFolioFacturaOrig());
                    return false;
                } // si es nullable, omitimos la columna para no forzar NULL
            }

            // Tasas opcionales
            addIfPresentWithFallback(meta, avail, cols, vals, "IVA_TASA", req.getIvaPorcentaje(), req.getIvaPorcentaje());
            addIfPresentWithFallback(meta, avail, cols, vals, "IVA_PORCENTAJE", req.getIvaPorcentaje(), req.getIvaPorcentaje());
            addIfPresentWithFallback(meta, avail, cols, vals, "IEPS_TASA", req.getIepsPorcentaje(), req.getIepsPorcentaje());
            addIfPresentWithFallback(meta, avail, cols, vals, "IEPS_PORCENTAJE", req.getIepsPorcentaje(), req.getIepsPorcentaje());

            String sql = "INSERT INTO NOTAS_CREDITO (" + String.join(", ", cols) + ") VALUES (" + String.join(", ", java.util.Collections.nCopies(cols.size(), "?")) + ")";
            int updated = jdbcTemplate.update(sql, vals.toArray());
            return updated > 0;
        } catch (Exception e) {
            lastInsertError = e.getMessage();
            logger.warn("Error insertando en NOTAS_CREDITO: {}", e.getMessage());
            return false;
        }
    }

    public String getLastInsertError() { return lastInsertError; }
}