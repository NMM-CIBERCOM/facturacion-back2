package com.cibercom.facturacion_back.dao;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO específico para consultar/insertar en la tabla FACTURAS por UUID en Oracle
 * usando nombres de columnas compatibles con diferentes variantes de schema.
 */
@Repository
@Profile("oracle")
public class UuidFacturaOracleDAO {

    private final JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(UuidFacturaOracleDAO.class);
    private volatile String lastInsertError;

    public UuidFacturaOracleDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Intenta obtener datos básicos por UUID.
     * Construye un SELECT dinámico según las columnas disponibles,
     * evitando identificadores inválidos en variantes de esquema.
     */
    public Optional<Result> obtenerBasicosPorUuid(String uuid) {
        // Detectar tabla principal (FACTURAS o FACTURA) y columnas disponibles del schema
        String tableName = detectFacturaTableName();
        java.util.Map<String, ColumnMeta> meta = fetchColumnsMeta(tableName);
        java.util.Set<String> avail = availableNames(meta);
        String uuidCol = pickFirst(avail, "UUID", "FOLIO_FISCAL", "FOLIO_FISCAL_SAT", "UUID_CFDI", "FOLIO_FISCAL_UUID");
        if (uuidCol == null) uuidCol = "UUID"; // fallback

        // Expresiones dinámicas con alias estándar esperados por map()
        String xmlExpr = avail.contains("XML_CONTENT") ? "XML_CONTENT" :
                (avail.contains("XML") ? "XML AS XML_CONTENT" :
                        (avail.contains("XML_CFDI") ? "XML_CFDI AS XML_CONTENT" : "NULL AS XML_CONTENT"));
        String serieExpr = avail.contains("SERIE") ? "SERIE" : "NULL AS SERIE";
        String folioExpr = avail.contains("FOLIO") ? "FOLIO" : "NULL AS FOLIO";
        String subtotalExpr = avail.contains("SUBTOTAL") ? "SUBTOTAL" : "NULL AS SUBTOTAL";
        String descuentoExpr = avail.contains("DESCUENTO") ? "DESCUENTO" : "NULL AS DESCUENTO";

        String ivaExpr;
        if (avail.contains("IVA")) {
            ivaExpr = "NVL(IVA,0) AS IVA";
        } else {
            // Sumar posibles columnas de IVA por tasa si existen
            java.util.List<String> ivaParts = new java.util.ArrayList<>();
            if (avail.contains("IVA16")) ivaParts.add("NVL(IVA16,0)");
            if (avail.contains("IVA8")) ivaParts.add("NVL(IVA8,0)");
            if (avail.contains("IVA0")) ivaParts.add("NVL(IVA0,0)");
            // IVA Exento no suma importe (generalmente 0), no lo incluimos en IVA total
            if (ivaParts.isEmpty()) ivaExpr = "NULL AS IVA"; else ivaExpr = String.join(" + ", ivaParts) + " AS IVA";
        }

        // IEPS: usar columna total si existe; si no, sumar por tasas conocidas
        String iepsExpr;
        if (avail.contains("IEPS")) {
            iepsExpr = "NVL(IEPS,0) AS IEPS";
        } else {
            java.util.List<String> iepsParts = new java.util.ArrayList<>();
            String[] iepsCols = new String[]{
                    "IEPS_26","IEPS_160","IEPS_8","IEPS_30","IEPS_30_4","IEPS_7","IEPS_53",
                    "IEPS_25","IEPS_6","IEPS_50","IEPS_9","IEPS_3","IEPS_43"
            };
            for (String c : iepsCols) {
                if (avail.contains(c)) iepsParts.add("NVL("+c+",0)");
            }
            iepsExpr = iepsParts.isEmpty() ? "NULL AS IEPS" : String.join(" + ", iepsParts) + " AS IEPS";
        }

        String totalExpr;
        if (avail.contains("TOTAL")) {
            totalExpr = "NVL(TOTAL,0) AS TOTAL";
        } else if (avail.contains("IMPORTE")) {
            totalExpr = "NVL(IMPORTE,0) AS TOTAL";
        } else {
            totalExpr = "NULL AS TOTAL";
        }

        String metodoPagoExpr = avail.contains("MEDIO_PAGO") ? "MEDIO_PAGO" :
                (avail.contains("METODO_PAGO") ? "METODO_PAGO AS MEDIO_PAGO" : "NULL AS MEDIO_PAGO");
        String formaPagoExpr = avail.contains("FORMA_PAGO") ? "FORMA_PAGO" : "NULL AS FORMA_PAGO";
        String usoCfdiExpr = avail.contains("RECEPTOR_USO_CFDI") ? "RECEPTOR_USO_CFDI" :
                (avail.contains("USO_CFDI") ? "USO_CFDI AS RECEPTOR_USO_CFDI" : "NULL AS RECEPTOR_USO_CFDI");
        String estadoExpr = avail.contains("ESTADO") ? "ESTADO" :
                (avail.contains("ESTATUS_FACTURA") ? "TO_CHAR(ESTATUS_FACTURA) AS ESTADO" : "NULL AS ESTADO");
        String estadoDescExpr = avail.contains("ESTADO_DESCRIPCION") ? "ESTADO_DESCRIPCION" : "NULL AS ESTADO_DESCRIPCION";

        // RFCs y fecha
        String rfcEmisorExpr = avail.contains("RFC_E") ? "RFC_E AS RFC_EMISOR" :
                (avail.contains("RFC_EMISOR") ? "RFC_EMISOR" : "NULL AS RFC_EMISOR");
        String rfcReceptorExpr = avail.contains("RFC_R") ? "RFC_R AS RFC_RECEPTOR" :
                (avail.contains("RFC_RECEPTOR") ? "RFC_RECEPTOR" : "NULL AS RFC_RECEPTOR");
        String fechaExpr;
        if (avail.contains("FECHA_FACTURA")) fechaExpr = "FECHA_FACTURA";
        else if (avail.contains("FECHA_EMISION")) fechaExpr = "FECHA_EMISION AS FECHA_FACTURA";
        else if (avail.contains("FECHA_TIMBRADO")) fechaExpr = "FECHA_TIMBRADO AS FECHA_FACTURA";
        else if (avail.contains("FECHA")) fechaExpr = "FECHA AS FECHA_FACTURA";
        else fechaExpr = "NULL AS FECHA_FACTURA";

        // Desgloses opcionales: agregar columnas si existen
        String iva16Expr = avail.contains("IVA16") ? "IVA16" : "NULL AS IVA16";
        String iva8Expr = avail.contains("IVA8") ? "IVA8" : "NULL AS IVA8";
        String iva0Expr = avail.contains("IVA0") ? "IVA0" : "NULL AS IVA0";
        String ivaExentoExpr = avail.contains("IVA_EXENTO") ? "IVA_EXENTO" : "NULL AS IVA_EXENTO";
        String ieps26Expr = avail.contains("IEPS_26") ? "IEPS_26" : "NULL AS IEPS_26";
        String ieps160Expr = avail.contains("IEPS_160") ? "IEPS_160" : "NULL AS IEPS_160";
        String ieps8Expr = avail.contains("IEPS_8") ? "IEPS_8" : "NULL AS IEPS_8";
        String ieps30Expr = avail.contains("IEPS_30") ? "IEPS_30" : "NULL AS IEPS_30";
        String ieps304Expr = avail.contains("IEPS_30_4") ? "IEPS_30_4" : "NULL AS IEPS_30_4";
        String ieps7Expr = avail.contains("IEPS_7") ? "IEPS_7" : "NULL AS IEPS_7";
        String ieps53Expr = avail.contains("IEPS_53") ? "IEPS_53" : "NULL AS IEPS_53";
        String ieps25Expr = avail.contains("IEPS_25") ? "IEPS_25" : "NULL AS IEPS_25";
        String ieps6Expr = avail.contains("IEPS_6") ? "IEPS_6" : "NULL AS IEPS_6";
        String ieps50Expr = avail.contains("IEPS_50") ? "IEPS_50" : "NULL AS IEPS_50";
        String ieps9Expr = avail.contains("IEPS_9") ? "IEPS_9" : "NULL AS IEPS_9";
        String ieps3Expr = avail.contains("IEPS_3") ? "IEPS_3" : "NULL AS IEPS_3";
        String ieps43Expr = avail.contains("IEPS_43") ? "IEPS_43" : "NULL AS IEPS_43";

        String sql = "SELECT " + xmlExpr + ", " + serieExpr + ", " + folioExpr + ", " + subtotalExpr + ", " + descuentoExpr + ", " +
                ivaExpr + ", " + iepsExpr + ", " + totalExpr + ", " +
                metodoPagoExpr + ", " + formaPagoExpr + ", " + usoCfdiExpr + ", " + estadoExpr + ", " + estadoDescExpr + ", " +
                rfcEmisorExpr + ", " + rfcReceptorExpr + ", " + fechaExpr + ", " +
                iva16Expr + ", " + iva8Expr + ", " + iva0Expr + ", " + ivaExentoExpr + ", " +
                ieps26Expr + ", " + ieps160Expr + ", " + ieps8Expr + ", " + ieps30Expr + ", " + ieps304Expr + ", " + ieps7Expr + ", " + ieps53Expr + ", " +
                ieps25Expr + ", " + ieps6Expr + ", " + ieps50Expr + ", " + ieps9Expr + ", " + ieps3Expr + ", " + ieps43Expr +
                " FROM " + tableName + " WHERE " + uuidCol + " = ?";

        try {
            Result r = jdbcTemplate.query(sql, rs -> rs.next() ? map(rs) : null, uuid);
            return Optional.ofNullable(r);
        } catch (Exception e) {
            logger.warn("Fallo consultando Oracle por UUID {}: {}", uuid, e.getMessage());
            return Optional.empty();
        }
    }

    // Metadatos de columna
    static class ColumnMeta {
        String name;
        String dataType;
        boolean nullable;
        boolean hasDefault;
    }

    // Helper: detectar nombre de tabla principal de facturas (FACTURAS o FACTURA)
    private String detectFacturaTableName() {
        // Intentar en USER_TABLES primero
        try {
            Integer c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM USER_TABLES WHERE UPPER(TABLE_NAME) = 'FACTURAS'",
                    Integer.class
            );
            if (c != null && c > 0) return "FACTURAS";
        } catch (Exception ignored) {}
        try {
            Integer c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM USER_TABLES WHERE UPPER(TABLE_NAME) = 'FACTURA'",
                    Integer.class
            );
            if (c != null && c > 0) return "FACTURA";
        } catch (Exception ignored) {}
        // Fallback a ALL_TABLES por si usuario no tiene privilegios en USER_TABLES
        try {
            Integer c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ALL_TABLES WHERE UPPER(OWNER) = UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) AND UPPER(TABLE_NAME) = 'FACTURAS'",
                    Integer.class
            );
            if (c != null && c > 0) return "FACTURAS";
        } catch (Exception ignored) {}
        try {
            Integer c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ALL_TABLES WHERE UPPER(OWNER) = UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) AND UPPER(TABLE_NAME) = 'FACTURA'",
                    Integer.class
            );
            if (c != null && c > 0) return "FACTURA";
        } catch (Exception ignored) {}
        // Por defecto
        return "FACTURAS";
    }

    // Helper: obtener metadatos de columnas de la tabla de facturas
    private java.util.Map<String, ColumnMeta> fetchColumnsMeta(String tableName) {
        java.util.Map<String, ColumnMeta> meta = new java.util.HashMap<>();
        // Preferir USER_TAB_COLUMNS para evitar permisos sobre ALL_TAB_COLUMNS
        try {
            String sqlUser = "SELECT UPPER(COLUMN_NAME) AS COLUMN_NAME, UPPER(DATA_TYPE) AS DATA_TYPE, NULLABLE, DATA_DEFAULT FROM USER_TAB_COLUMNS WHERE UPPER(TABLE_NAME) = '" + tableName.toUpperCase() + "'";
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
            logger.warn("No se pudieron consultar columnas en USER_TAB_COLUMNS: {}", e.getMessage());
        }
        // Fallback a ALL_TAB_COLUMNS si lo anterior no devolvió nada
        if (meta.isEmpty()) {
            try {
                String sqlAll = "SELECT UPPER(COLUMN_NAME) AS COLUMN_NAME, UPPER(DATA_TYPE) AS DATA_TYPE, NULLABLE, DATA_DEFAULT FROM ALL_TAB_COLUMNS WHERE UPPER(TABLE_NAME) = '" + tableName.toUpperCase() + "' AND UPPER(OWNER) = UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA'))";
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
                logger.warn("No se pudieron consultar columnas en ALL_TAB_COLUMNS: {}", e.getMessage());
            }
        }
        return meta;
    }

    // Helper: nombres disponibles
    private java.util.Set<String> availableNames(java.util.Map<String, ColumnMeta> meta) {
        return new java.util.HashSet<>(meta.keySet());
    }

    // Helper: elegir la primera columna disponible en orden de preferencia
    private String pickFirst(java.util.Set<String> avail, String... candidates) {
        for (String c : candidates) {
            if (avail.contains(c)) return c;
        }
        return null;
    }

    // Helper: agregar columna si existe, con valor o fallback si es NOT NULL sin default
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

        // Ajuste por tipo de dato para evitar errores de conversión (ej. ORA-01722)
        String dt = (cm != null && cm.dataType != null) ? cm.dataType.toUpperCase() : "";
        try {
            if (dt.contains("NUMBER") || dt.contains("INTEGER") || dt.contains("FLOAT")) {
                if (v instanceof String) {
                    String sv = (String) v;
                    if (sv != null && sv.matches("^-?\\d+(\\.\\d+)?$")) {
                        v = new java.math.BigDecimal(sv);
                    } else {
                        // Valor no numérico para columna numérica: si es requerido, usar 0; si no, omitir
                        v = requireValue ? java.math.BigDecimal.ZERO : null;
                    }
                } else if (v == null && requireValue) {
                    v = java.math.BigDecimal.ZERO;
                }
            } else if (dt.contains("DATE")) {
                if (v instanceof java.util.Date && !(v instanceof java.sql.Date)) {
                    v = new java.sql.Date(((java.util.Date) v).getTime());
                }
                if (v == null && requireValue) {
                    v = new java.sql.Date(System.currentTimeMillis());
                }
            } else if (dt.contains("TIMESTAMP")) {
                if (v == null && requireValue) {
                    v = new java.sql.Timestamp(System.currentTimeMillis());
                }
            } else if (dt.contains("BLOB")) {
                if (v == null && requireValue) {
                    v = new byte[0];
                }
            } else if ((dt.contains("CHAR") || dt.contains("VARCHAR") || dt.contains("CLOB") || dt.contains("LONG"))) {
                if (v == null && requireValue) {
                    v = (fallback != null) ? fallback : "";
                }
            }
        } catch (Exception ignored) {
            // Si falla la conversión, dejamos que el fallback genérico al final maneje esta columna
        }

        if (v != null) {
            cols.add(name);
            vals.add(v);
        }
    }

    /** Inserta un registro mínimo de factura para pruebas con columnas dinámicas. */
    public boolean insertarBasico(String uuid,
                                  String xmlContent,
                                  String serie,
                                  String folio,
                                  BigDecimal subtotal,
                                  BigDecimal iva,
                                  BigDecimal ieps,
                                  BigDecimal total,
                                  String formaPago,
                                  String usoCfdi,
                                  String estado,
                                  String estadoDescripcion,
                                  String medioPago,
                                  String rfcReceptor,
                                  String rfcEmisor) {
        return insertarBasicoConIdReceptor(uuid, xmlContent, serie, folio, subtotal, iva, ieps, total,
                formaPago, usoCfdi, estado, estadoDescripcion, medioPago, rfcReceptor, rfcEmisor, null, null, null);
    }

    /** Variante que permite especificar ID_RECEPTOR explícitamente si el esquema lo requiere como NOT NULL. */
    public boolean insertarBasicoConIdReceptor(String uuid,
                                               String xmlContent,
                                               String serie,
                                               String folio,
                                               BigDecimal subtotal,
                                               BigDecimal iva,
                                               BigDecimal ieps,
                                               BigDecimal total,
                                               String formaPago,
                                               String usoCfdi,
                                               String estado,
                                               String estadoDescripcion,
                                               String medioPago,
                                               String rfcReceptor,
                                               String rfcEmisor,
                                               Long idReceptor) {
        return insertarBasicoConIdReceptor(uuid, xmlContent, serie, folio, subtotal, iva, ieps, total,
                formaPago, usoCfdi, estado, estadoDescripcion, medioPago, rfcReceptor, rfcEmisor, null, idReceptor, null);
    }

    /** Variante extendida que permite indicar explícitamente el TIPO_FACTURA (ej. 2 = egreso). */
    public boolean insertarBasicoConIdReceptor(String uuid,
                                               String xmlContent,
                                               String serie,
                                               String folio,
                                               BigDecimal subtotal,
                                               BigDecimal iva,
                                               BigDecimal ieps,
                                               BigDecimal total,
                                               String formaPago,
                                               String usoCfdi,
                                               String estado,
                                               String estadoDescripcion,
                                              String medioPago,
                                              String rfcReceptor,
                                              String rfcEmisor,
                                              String correoReceptor,
                                              Long idReceptor,
                                              Integer tipoFactura) {
        lastInsertError = null;
        try {
            String tableName = detectFacturaTableName();
            java.util.Map<String, ColumnMeta> meta = fetchColumnsMeta(tableName);
            java.util.Set<String> avail = availableNames(meta);
            if (avail.isEmpty()) {
                lastInsertError = "Sin columnas visibles en " + tableName + " (ALL_TAB_COLUMNS vacío)";
                return false;
            }
            String uuidCol = pickFirst(avail, "UUID", "FOLIO_FISCAL", "FOLIO_FISCAL_SAT", "UUID_CFDI", "FOLIO_FISCAL_UUID");
            if (uuidCol == null) {
                lastInsertError = "No se encontró columna UUID compatible en " + tableName;
                return false;
            }

            List<String> cols = new ArrayList<>();
            List<Object> vals = new ArrayList<>();
            cols.add(uuidCol); vals.add(uuid);

            // Valores por defecto seguros
            String xmlFallback = xmlContent != null ? xmlContent : "<cfdi:Comprobante xmlns:cfdi='http://www.sat.gob.mx/cfd/4'></cfdi:Comprobante>";
            String serieFallback = serie != null ? serie : "LIB";
            String folioFallback = folio != null ? folio : "FREE";
            BigDecimal subtotalFallback = subtotal != null ? subtotal : BigDecimal.ZERO;
            BigDecimal ivaFallback = iva != null ? iva : BigDecimal.ZERO;
            BigDecimal iepsFallback = ieps != null ? ieps : BigDecimal.ZERO;
            BigDecimal totalFallback = total != null ? total : subtotalFallback.add(ivaFallback).add(iepsFallback);
            String formaPagoFallback = formaPago != null ? formaPago : "99"; // No aplica
            String usoCfdiFallback = usoCfdi != null ? usoCfdi : "S01"; // Sin efectos fiscales
            String estadoFallback = estado != null ? estado : "EN_CAPTURA";
            String estadoDescFallback = estadoDescripcion != null ? estadoDescripcion : "Factura temporal para Captura Libre";
            String medioPagoFallback = medioPago != null ? medioPago : "PUE";
            String rfcReceptorFallback = rfcReceptor != null ? rfcReceptor : "XAXX010101000";
            String rfcEmisorFallback = rfcEmisor != null ? rfcEmisor : "AAA010101AAA";
            String correoReceptorFallback = correoReceptor != null ? correoReceptor : "";
            java.sql.Timestamp nowTs = new java.sql.Timestamp(System.currentTimeMillis());

            // Agregar columnas consideradas comunes con fallback si son NOT NULL
            addIfPresentWithFallback(meta, avail, cols, vals, "XML_CONTENT", xmlContent, xmlFallback);
            // Variantes de columna para XML
            addIfPresentWithFallback(meta, avail, cols, vals, "XML", xmlContent, xmlFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "XML_CFDI", xmlContent, xmlFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "SERIE", serie, serieFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "FOLIO", folio, folioFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "SUBTOTAL", subtotal, subtotalFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "DESCUENTO", BigDecimal.ZERO, BigDecimal.ZERO);
            addIfPresentWithFallback(meta, avail, cols, vals, "IVA", iva, ivaFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "IEPS", ieps, iepsFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "TOTAL", total, totalFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "FORMA_PAGO", formaPago, formaPagoFallback);
            // USO CFDI puede estar como USO_CFDI o RECEPTOR_USO_CFDI
            addIfPresentWithFallback(meta, avail, cols, vals, "USO_CFDI", usoCfdi, usoCfdiFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "RECEPTOR_USO_CFDI", usoCfdi, usoCfdiFallback);
            if (tipoFactura != null) {
                addIfPresentWithFallback(meta, avail, cols, vals, "TIPO_FACTURA", tipoFactura, tipoFactura);
            }
            addIfPresentWithFallback(meta, avail, cols, vals, "ESTADO", estado, estadoFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "ESTADO_DESCRIPCION", estadoDescripcion, estadoDescFallback);
            // Medio/Metodo de pago puede variar
            addIfPresentWithFallback(meta, avail, cols, vals, "MEDIO_PAGO", medioPago, medioPagoFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "METODO_PAGO", medioPago, medioPagoFallback);
            // RFC receptor/emisor pueden variar en nombre
            addIfPresentWithFallback(meta, avail, cols, vals, "RFC_R", rfcReceptor, rfcReceptorFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "RFC_RECEPTOR", rfcReceptor, rfcReceptorFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "RFC_E", rfcEmisor, rfcEmisorFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "RFC_EMISOR", rfcEmisor, rfcEmisorFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "CORREO_ELECTRONICO", correoReceptor, correoReceptorFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "CORREO", correoReceptor, correoReceptorFallback);
            addIfPresentWithFallback(meta, avail, cols, vals, "EMAIL", correoReceptor, correoReceptorFallback);
            // ID_RECEPTOR si el esquema lo tiene y lo exige como NOT NULL
            if (avail.contains("ID_RECEPTOR")) {
                ColumnMeta cm = meta.get("ID_RECEPTOR");
                boolean requireValue = cm != null && !cm.nullable && !cm.hasDefault;
                if (idReceptor != null) {
                    cols.add("ID_RECEPTOR");
                    vals.add(idReceptor);
                } else if (requireValue) {
                    // Si es requerido y no se proporcionó, devolver error claro
                    lastInsertError = "ID_RECEPTOR requerido por esquema FACTURAS (NOT NULL)";
                    return false;
                }
            }
            // Fechas si el esquema las requiere como NOT NULL
            addIfPresentWithFallback(meta, avail, cols, vals, "FECHA_GENERACION", null, nowTs);
            addIfPresentWithFallback(meta, avail, cols, vals, "FECHA_TIMBRADO", null, nowTs);
            // Algunas variantes usan FECHA en lugar de FECHA_GENERACION/FECHA_TIMBRADO
            addIfPresentWithFallback(meta, avail, cols, vals, "FECHA", null, new java.sql.Date(nowTs.getTime()));

            // Fallback genérico: asegurar valores para cualquier columna NOT NULL sin default que no hayamos agregado
            for (java.util.Map.Entry<String, ColumnMeta> entry : meta.entrySet()) {
                ColumnMeta cm = entry.getValue();
                String colName = cm.name;
                // Saltar si ya la agregamos o si la columna tiene default/permite NULL
                if (cols.contains(colName) || cm.nullable || cm.hasDefault) continue;
                // Evitar forzar ID primarios que suelen venir de secuencias o triggers
                if ("ID_FACTURA".equalsIgnoreCase(colName)) continue;
                // Evitar columnas dependientes de catálogo que varían (p.ej. ID_RECEPTOR)
                if ("ID_RECEPTOR".equalsIgnoreCase(colName)) continue;

                Object fallbackVal = null;
                String dt = cm.dataType != null ? cm.dataType.toUpperCase() : "";
                if (dt.contains("CHAR") || dt.contains("VARCHAR")) {
                    // Texto genérico
                    fallbackVal = "N/A";
                } else if (dt.contains("NUMBER") || dt.contains("INTEGER") || dt.contains("FLOAT")) {
                    // Numérico genérico
                    fallbackVal = java.math.BigDecimal.ZERO;
                } else if (dt.contains("DATE")) {
                    fallbackVal = new java.sql.Date(nowTs.getTime());
                } else if (dt.contains("TIMESTAMP")) {
                    fallbackVal = nowTs;
                } else if (dt.contains("CLOB") || dt.contains("LONG")) {
                    fallbackVal = xmlFallback; // contenido textual si aplica
                } else if (dt.contains("BLOB")) {
                    fallbackVal = new byte[0];
                } else {
                    // Desconocido: usar texto genérico
                    fallbackVal = "N/A";
                }

                // Agregar columna con fallback
                cols.add(colName);
                vals.add(fallbackVal);
            }

            // Construir INSERT dinámico
            String sql = "INSERT INTO " + tableName + " (" + String.join(", ", cols) + ") VALUES (" + String.join(", ", java.util.Collections.nCopies(cols.size(), "?")) + ")";
            int updated = jdbcTemplate.update(sql, vals.toArray());
            return updated > 0;
        } catch (Exception e) {
            lastInsertError = rootCauseMessage(e);
            logger.error("Error al insertar en tabla de facturas: {}", lastInsertError, e);
            return false;
        }
    }

    private String rootCauseMessage(Throwable t) {
        Throwable c = t;
        String msg = t.getMessage();
        while (c.getCause() != null) {
            c = c.getCause();
            if (c.getMessage() != null) msg = c.getMessage();
        }
        return msg;
    }

    private Result map(ResultSet rs) throws SQLException {
        Result r = new Result();
        r.xmlContent = rs.getString("XML_CONTENT");
        r.serie = rs.getString("SERIE");
        r.folio = rs.getString("FOLIO");
        try { r.subtotal = getBD(rs, "SUBTOTAL"); } catch (SQLException ignored) { r.subtotal = null; }
        try { r.descuento = getBD(rs, "DESCUENTO"); } catch (SQLException ignored) { r.descuento = null; }
        try { r.iva = getBD(rs, "IVA"); } catch (SQLException ignored) { r.iva = null; }
        try { r.ieps = getBD(rs, "IEPS"); } catch (SQLException ignored) { r.ieps = null; }
        try { r.total = getBD(rs, "TOTAL"); } catch (SQLException ignored) { r.total = null; }
        // Campos opcionales
        try { r.metodoPago = rs.getString("MEDIO_PAGO"); } catch (SQLException ignored) {}
        try { r.formaPago = rs.getString("FORMA_PAGO"); } catch (SQLException ignored) {}
        try { r.usoCfdi = rs.getString("RECEPTOR_USO_CFDI"); } catch (SQLException ignored) {}
        try { r.estadoCodigo = rs.getString("ESTADO"); } catch (SQLException ignored) {}
        try { r.estadoDescripcion = rs.getString("ESTADO_DESCRIPCION"); } catch (SQLException ignored) {}
        try { r.rfcEmisor = rs.getString("RFC_EMISOR"); } catch (SQLException ignored) {}
        try { r.rfcReceptor = rs.getString("RFC_RECEPTOR"); } catch (SQLException ignored) {}
        try {
            java.sql.Timestamp ts = null;
            try { ts = rs.getTimestamp("FECHA_FACTURA"); } catch (SQLException ignored2) {}
            r.fechaFactura = ts != null ? ts.toInstant() : null;
        } catch (Exception ignored) {}
        // Desgloses
        try { r.iva16 = getBD(rs, "IVA16"); } catch (SQLException ignored) { r.iva16 = null; }
        try { r.iva8 = getBD(rs, "IVA8"); } catch (SQLException ignored) { r.iva8 = null; }
        try { r.iva0 = getBD(rs, "IVA0"); } catch (SQLException ignored) { r.iva0 = null; }
        try { r.ivaExento = getBD(rs, "IVA_EXENTO"); } catch (SQLException ignored) { r.ivaExento = null; }
        try { r.ieps26 = getBD(rs, "IEPS_26"); } catch (SQLException ignored) { r.ieps26 = null; }
        try { r.ieps160 = getBD(rs, "IEPS_160"); } catch (SQLException ignored) { r.ieps160 = null; }
        try { r.ieps8 = getBD(rs, "IEPS_8"); } catch (SQLException ignored) { r.ieps8 = null; }
        try { r.ieps30 = getBD(rs, "IEPS_30"); } catch (SQLException ignored) { r.ieps30 = null; }
        try { r.ieps304 = getBD(rs, "IEPS_30_4"); } catch (SQLException ignored) { r.ieps304 = null; }
        try { r.ieps7 = getBD(rs, "IEPS_7"); } catch (SQLException ignored) { r.ieps7 = null; }
        try { r.ieps53 = getBD(rs, "IEPS_53"); } catch (SQLException ignored) { r.ieps53 = null; }
        try { r.ieps25 = getBD(rs, "IEPS_25"); } catch (SQLException ignored) { r.ieps25 = null; }
        try { r.ieps6 = getBD(rs, "IEPS_6"); } catch (SQLException ignored) { r.ieps6 = null; }
        try { r.ieps50 = getBD(rs, "IEPS_50"); } catch (SQLException ignored) { r.ieps50 = null; }
        try { r.ieps9 = getBD(rs, "IEPS_9"); } catch (SQLException ignored) { r.ieps9 = null; }
        try { r.ieps3 = getBD(rs, "IEPS_3"); } catch (SQLException ignored) { r.ieps3 = null; }
        try { r.ieps43 = getBD(rs, "IEPS_43"); } catch (SQLException ignored) { r.ieps43 = null; }
        return r;
    }

    private BigDecimal getBD(ResultSet rs, String col) throws SQLException {
        BigDecimal bd = null;
        try {
            bd = rs.getBigDecimal(col);
        } catch (SQLException e) {
            throw e;
        }
        return bd != null ? bd : BigDecimal.ZERO;
    }

    /** Resultado compacto para la consulta por UUID */
    public static class Result {
        public String xmlContent;
        public String serie;
        public String folio;
        public BigDecimal subtotal;
        public BigDecimal descuento;
        public BigDecimal iva;
        public BigDecimal ieps;
        public BigDecimal total;
        public String formaPago;
        public String usoCfdi;
        public String estadoCodigo;
        public String estadoDescripcion;
        // No se expone metodoPago ya que el schema común usa MEDIO_PAGO/FORMa_PAGO
        public String metodoPago; // opcional/no usado
        public String rfcEmisor;
        public String rfcReceptor;
        public java.time.Instant fechaFactura;
        // Desgloses
        public BigDecimal iva16;
        public BigDecimal iva8;
        public BigDecimal iva0;
        public BigDecimal ivaExento;
        public BigDecimal ieps26;
        public BigDecimal ieps160;
        public BigDecimal ieps8;
        public BigDecimal ieps30;
        public BigDecimal ieps304;
        public BigDecimal ieps7;
        public BigDecimal ieps53;
        public BigDecimal ieps25;
        public BigDecimal ieps6;
        public BigDecimal ieps50;
        public BigDecimal ieps9;
        public BigDecimal ieps3;
        public BigDecimal ieps43;
    }
    public String getLastInsertError() {
        return lastInsertError;
    }
}