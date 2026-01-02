package com.cibercom.facturacion_back.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * DAO para insertar conceptos ligados a FACTURAS en Oracle.
 * Resuelve dinámicamente la columna de UUID en FACTURAS y obtiene ID_FACTURA
 * si existe; de lo contrario retorna un error controlado.
 */
@Repository
@Profile("oracle")
public class ConceptoOracleDAO {

    private static final Logger logger = LoggerFactory.getLogger(ConceptoOracleDAO.class);
    private final JdbcTemplate jdbcTemplate;
    private volatile String lastInsertError;

    public ConceptoOracleDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------- Metadatos de columnas --------------------
    static class ColumnMeta {
        String name;
        boolean nullable;
        boolean hasDefault;
    }

    private java.util.Map<String, ColumnMeta> fetchMetaForTable(String table) {
        java.util.Map<String, ColumnMeta> meta = new java.util.HashMap<>();
        try {
            String sql = "SELECT UPPER(COLUMN_NAME) AS COLUMN_NAME, NULLABLE, DATA_DEFAULT FROM ALL_TAB_COLUMNS WHERE UPPER(TABLE_NAME) = ?";
            jdbcTemplate.query(sql, (org.springframework.jdbc.core.ResultSetExtractor<Void>) rs -> {
                while (rs.next()) {
                    ColumnMeta cm = new ColumnMeta();
                    cm.name = rs.getString("COLUMN_NAME");
                    cm.nullable = "Y".equalsIgnoreCase(rs.getString("NULLABLE"));
                    String def = null;
                    try { def = rs.getString("DATA_DEFAULT"); } catch (Exception ignored) {}
                    cm.hasDefault = def != null && !def.isBlank();
                    meta.put(cm.name, cm);
                }
                return null;
            }, table.toUpperCase());
        } catch (Exception e) {
            // ignorar; meta vacío
        }
        return meta;
    }

    private boolean isNotNullWithoutDefault(String table, String col) {
        try {
            String sql = "SELECT NULLABLE, DATA_DEFAULT FROM ALL_TAB_COLUMNS WHERE UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?";
            return jdbcTemplate.query(sql, rs -> {
                if (rs.next()) {
                    boolean nullable = "Y".equalsIgnoreCase(rs.getString("NULLABLE"));
                    String def = null;
                    try { def = rs.getString("DATA_DEFAULT"); } catch (Exception ignored) {}
                    boolean hasDefault = def != null && !def.isBlank();
                    return !nullable && !hasDefault;
                }
                return false;
            }, table.toUpperCase(), col.toUpperCase());
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------- Secuencias --------------------
    private boolean hasSequence(String seqName) {
        try {
            Integer c = jdbcTemplate.query(
                    "SELECT COUNT(*) AS C FROM ALL_SEQUENCES WHERE SEQUENCE_NAME =?",
                    rs -> rs.next() ? rs.getInt("C") : 0,
                    seqName.toUpperCase()
            );
            return c != null && c > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String pickFirstSequence(String... candidates) {
        for (String c : candidates) {
            if (hasSequence(c)) return c;
        }
        return null;
    }

    /** Obtiene el ID_FACTURA numérico a partir del UUID, si la columna existe. */
    public Optional<Long> obtenerIdFacturaPorUuid(String uuid) {
        try {
            // Detectar columnas disponibles
            String uuidCol = pickFirst(new String[]{
                    "UUID", "FOLIO_FISCAL", "FOLIO_FISCAL_SAT", "UUID_CFDI", "FOLIO_FISCAL_UUID"
            });
            if (uuidCol == null) {
                return Optional.empty();
            }
            boolean hasIdFactura = hasColumn("FACTURAS", "ID_FACTURA");
            if (!hasIdFactura) {
                return Optional.empty();
            }
            Long id = jdbcTemplate.query(
                    "SELECT ID_FACTURA FROM FACTURAS WHERE " + uuidCol + " = ?",
                    rs -> rs.next() ? rs.getLong("ID_FACTURA") : null,
                    uuid
            );
            return Optional.ofNullable(id);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Helper para añadir ID_CONCEPTO si es requerido por el esquema
    private void appendIdConceptoIfRequired(StringBuilder sqlCols, StringBuilder sqlVals) {
        if (hasColumn("CONCEPTOS", "ID_CONCEPTO") && isNotNullWithoutDefault("CONCEPTOS", "ID_CONCEPTO")) {
            sqlCols.append(", ID_CONCEPTO");
            // Buscar secuencia común
            String seq = pickFirstSequence("CONCEPTOS_SEQ", "SEQ_CONCEPTOS", "CONCEPTO_SEQ", "SEQ_CONCEPTO", "S_CONCEPTOS", "ID_CONCEPTO_SEQ");
            if (seq != null) {
                sqlVals.append(", ").append(seq).append(".NEXTVAL");
            } else {
                // Fallback inseguro pero funcional para entornos de prueba
                sqlVals.append(", NVL((SELECT MAX(ID_CONCEPTO)+1 FROM CONCEPTOS),1)");
            }
        }
    }

    /** Inserta un registro en CONCEPTOS usando ID_FACTURA. */
    public boolean insertarConcepto(Long idFactura,
                                    String skuClaveSat,
                                    String descripcion,
                                    String unidadMedida,
                                    BigDecimal valorUnitario,
                                    BigDecimal cantidad,
                                    BigDecimal descuento,
                                    BigDecimal tasaIva,
                                    BigDecimal iva,
                                    BigDecimal tasaIeps,
                                    BigDecimal ieps,
                                    String noPedimento) {
        lastInsertError = null;
        try {
            // Requeridos mínimos
            if (!hasColumn("CONCEPTOS", "ID_FACTURA")) { lastInsertError = "Falta columna ID_FACTURA"; return false; }
            if (!hasColumn("CONCEPTOS", "SKU_CLAVE_SAT")) { lastInsertError = "Falta columna SKU_CLAVE_SAT"; return false; }
            if (!hasColumn("CONCEPTOS", "DESCRIPCION")) { lastInsertError = "Falta columna DESCRIPCION"; return false; }

            StringBuilder sqlCols = new StringBuilder("INSERT INTO CONCEPTOS (ID_FACTURA, SKU_CLAVE_SAT, DESCRIPCION");
            StringBuilder sqlVals = new StringBuilder("VALUES (?,?,?");
            java.util.List<Object> params = new java.util.ArrayList<>();
            params.add(idFactura);
            params.add(skuClaveSat);
            params.add(descripcion);

            // ID_CONCEPTO si aplica
            appendIdConceptoIfRequired(sqlCols, sqlVals);

            if (hasColumn("CONCEPTOS", "UNIDAD_MEDIDA")) { sqlCols.append(", UNIDAD_MEDIDA"); sqlVals.append(",?"); params.add(unidadMedida); }
            if (hasColumn("CONCEPTOS", "VALOR_UNITARIO")) { sqlCols.append(", VALOR_UNITARIO"); sqlVals.append(",?"); params.add(valorUnitario); }
            if (hasColumn("CONCEPTOS", "CANTIDAD")) { sqlCols.append(", CANTIDAD"); sqlVals.append(",?"); params.add(cantidad); }
            if (hasColumn("CONCEPTOS", "DESCUENTO")) { sqlCols.append(", DESCUENTO"); sqlVals.append(",?"); params.add(descuento); }
            if (hasColumn("CONCEPTOS", "TASA_IVA")) { sqlCols.append(", TASA_IVA"); sqlVals.append(",?"); params.add(tasaIva); }
            if (hasColumn("CONCEPTOS", "IVA")) { sqlCols.append(", IVA"); sqlVals.append(",?"); params.add(iva); }
            if (hasColumn("CONCEPTOS", "TASA_IEPS")) { sqlCols.append(", TASA_IEPS"); sqlVals.append(",?"); params.add(tasaIeps); }
            if (hasColumn("CONCEPTOS", "IEPS")) { sqlCols.append(", IEPS"); sqlVals.append(",?"); params.add(ieps); }
            if (hasColumn("CONCEPTOS", "NO_PEDIMENTO")) { sqlCols.append(", NO_PEDIMENTO"); sqlVals.append(",?"); params.add(noPedimento); }

            sqlCols.append(") ");
            sqlVals.append(")");

            int updated = jdbcTemplate.update(sqlCols.toString() + sqlVals.toString(), params.toArray());
            return updated > 0;
        } catch (Exception e) {
            lastInsertError = e.getMessage();
            return false;
        }
    }

    /** Inserta un concepto sin vínculo a factura (CONCEPTOS libres). */
    public boolean insertarConceptoLibre(
            String skuClaveSat,
            String descripcion,
            String unidadMedida,
            BigDecimal valorUnitario,
            BigDecimal cantidad,
            BigDecimal descuento,
            BigDecimal tasaIva,
            BigDecimal iva,
            BigDecimal tasaIeps,
            BigDecimal ieps,
            String noPedimento
    ) {
        lastInsertError = null;
        try {
            // Verificar que el esquema permita inserción libre
            if (hasColumn("CONCEPTOS", "ID_FACTURA") && isNotNullWithoutDefault("CONCEPTOS", "ID_FACTURA")) {
                lastInsertError = "El esquema requiere ID_FACTURA NOT NULL; no se permite inserción libre";
                return false;
            }

            // Campos mínimos requeridos
            if (!hasColumn("CONCEPTOS", "SKU_CLAVE_SAT")) { lastInsertError = "Falta columna SKU_CLAVE_SAT"; return false; }
            if (!hasColumn("CONCEPTOS", "DESCRIPCION")) { lastInsertError = "Falta columna DESCRIPCION"; return false; }

            StringBuilder sqlCols = new StringBuilder("INSERT INTO CONCEPTOS (SKU_CLAVE_SAT, DESCRIPCION");
            StringBuilder sqlVals = new StringBuilder("VALUES (?,?");
            java.util.List<Object> params = new java.util.ArrayList<>();
            params.add(skuClaveSat);
            params.add(descripcion);

            // ID_CONCEPTO si aplica
            appendIdConceptoIfRequired(sqlCols, sqlVals);

            if (hasColumn("CONCEPTOS", "UNIDAD_MEDIDA")) { sqlCols.append(", UNIDAD_MEDIDA"); sqlVals.append(",?"); params.add(unidadMedida); }
            if (hasColumn("CONCEPTOS", "VALOR_UNITARIO")) { sqlCols.append(", VALOR_UNITARIO"); sqlVals.append(",?"); params.add(valorUnitario); }
            if (hasColumn("CONCEPTOS", "CANTIDAD")) { sqlCols.append(", CANTIDAD"); sqlVals.append(",?"); params.add(cantidad); }
            if (hasColumn("CONCEPTOS", "DESCUENTO")) { sqlCols.append(", DESCUENTO"); sqlVals.append(",?"); params.add(descuento); }
            if (hasColumn("CONCEPTOS", "TASA_IVA")) { sqlCols.append(", TASA_IVA"); sqlVals.append(",?"); params.add(tasaIva); }
            if (hasColumn("CONCEPTOS", "IVA")) { sqlCols.append(", IVA"); sqlVals.append(",?"); params.add(iva); }
            if (hasColumn("CONCEPTOS", "TASA_IEPS")) { sqlCols.append(", TASA_IEPS"); sqlVals.append(",?"); params.add(tasaIeps); }
            if (hasColumn("CONCEPTOS", "IEPS")) { sqlCols.append(", IEPS"); sqlVals.append(",?"); params.add(ieps); }
            if (hasColumn("CONCEPTOS", "NO_PEDIMENTO")) { sqlCols.append(", NO_PEDIMENTO"); sqlVals.append(",?"); params.add(noPedimento); }

            sqlCols.append(") ");
            sqlVals.append(")");

            int updated = jdbcTemplate.update(sqlCols.toString() + sqlVals.toString(), params.toArray());
            return updated > 0;
        } catch (Exception e) {
            lastInsertError = e.getMessage();
            return false;
        }
    }

    public String getLastInsertError() { return lastInsertError; }

    /**
     * Consulta conceptos desde la tabla CONCEPTOS (modo manual) por ID_FACTURA
     * Hace JOIN con CATALOGOS_PRODUCTOS_SERVICIOS para obtener los campos del catálogo
     */
    public java.util.List<java.util.Map<String, Object>> buscarConceptosPorIdFactura(Long idFactura) {
        try {
            if (idFactura == null) {
                return new java.util.ArrayList<>();
            }
            
            // Verificar que exista la tabla CONCEPTOS
            if (!hasColumn("CONCEPTOS", "ID_FACTURA")) {
                logger.warn("Tabla CONCEPTOS no tiene columna ID_FACTURA, no se pueden consultar conceptos");
                return new java.util.ArrayList<>();
            }
            
            // Construir SELECT dinámicamente según las columnas disponibles
            StringBuilder sql = new StringBuilder("SELECT ");
            java.util.List<String> columnas = new java.util.ArrayList<>();
            
            // Columnas básicas de CONCEPTOS
            if (hasColumn("CONCEPTOS", "ID_CONCEPTO")) columnas.add("c.ID_CONCEPTO");
            if (hasColumn("CONCEPTOS", "ID_FACTURA")) columnas.add("c.ID_FACTURA");
            if (hasColumn("CONCEPTOS", "SKU_CLAVE_SAT")) columnas.add("c.SKU_CLAVE_SAT");
            if (hasColumn("CONCEPTOS", "DESCRIPCION")) columnas.add("c.DESCRIPCION");
            if (hasColumn("CONCEPTOS", "CANTIDAD")) columnas.add("c.CANTIDAD");
            if (hasColumn("CONCEPTOS", "UNIDAD_MEDIDA")) columnas.add("c.UNIDAD_MEDIDA");
            if (hasColumn("CONCEPTOS", "VALOR_UNITARIO")) columnas.add("c.VALOR_UNITARIO");
            if (hasColumn("CONCEPTOS", "IMPORTE")) columnas.add("c.IMPORTE");
            if (hasColumn("CONCEPTOS", "TASA_IVA")) columnas.add("c.TASA_IVA");
            if (hasColumn("CONCEPTOS", "IVA")) columnas.add("c.IVA");
            
            // Campos del catálogo CATALOGOS_PRODUCTOS_SERVICIOS
            // Hacer JOIN con CATALOGOS_PRODUCTOS_SERVICIOS usando SKU_CLAVE_SAT = CLAVE_PROD_SERV
            sql.append(String.join(", ", columnas));
            sql.append(", cat.CLAVE_PROD_SERV, cat.OBJETO_IMPUESTO, cat.TASA_IVA AS TASA_IVA_CATALOGO, cat.UNIDAD AS UNIDAD_CATALOGO ");
            sql.append("FROM CONCEPTOS c ");
            sql.append("LEFT JOIN CATALOGOS_PRODUCTOS_SERVICIOS cat ON ");
            sql.append("  cat.CLAVE_PROD_SERV = c.SKU_CLAVE_SAT ");
            sql.append("  AND cat.ACTIVO = '1' ");
            sql.append("WHERE c.ID_FACTURA = ? ");
            sql.append("ORDER BY c.ID_CONCEPTO");
            
            logger.info("Consulta CONCEPTOS por ID_FACTURA SQL: {}", sql.toString());
            
            return jdbcTemplate.query(sql.toString(), new Object[]{idFactura}, (rs, rowNum) -> {
                java.util.Map<String, Object> concepto = new java.util.HashMap<>();
                
                // Mapear columnas de CONCEPTOS
                if (hasColumn("CONCEPTOS", "CANTIDAD")) {
                    try { concepto.put("cantidad", rs.getBigDecimal("CANTIDAD")); } catch (Exception e) {}
                }
                if (hasColumn("CONCEPTOS", "DESCRIPCION")) {
                    try { concepto.put("descripcion", rs.getString("DESCRIPCION")); } catch (Exception e) {}
                }
                if (hasColumn("CONCEPTOS", "UNIDAD_MEDIDA")) {
                    try { concepto.put("unidad", rs.getString("UNIDAD_MEDIDA")); } catch (Exception e) {}
                }
                if (hasColumn("CONCEPTOS", "VALOR_UNITARIO")) {
                    try { concepto.put("valorUnitario", rs.getBigDecimal("VALOR_UNITARIO")); } catch (Exception e) {}
                }
                if (hasColumn("CONCEPTOS", "IMPORTE")) {
                    try { concepto.put("importe", rs.getBigDecimal("IMPORTE")); } catch (Exception e) {}
                }
                if (hasColumn("CONCEPTOS", "IVA")) {
                    try { concepto.put("iva", rs.getBigDecimal("IVA")); } catch (Exception e) {}
                }
                if (hasColumn("CONCEPTOS", "TASA_IVA")) {
                    try { concepto.put("tasaIva", rs.getBigDecimal("TASA_IVA")); } catch (Exception e) {}
                }
                
                // Campos del catálogo (priorizar los del catálogo sobre los de CONCEPTOS)
                try {
                    String claveProdServ = rs.getString("CLAVE_PROD_SERV");
                    if (claveProdServ != null && !claveProdServ.trim().isEmpty()) {
                        concepto.put("claveProdServ", claveProdServ);
                    } else {
                        // Si no hay en catálogo, usar SKU_CLAVE_SAT de CONCEPTOS
                        try {
                            String skuClaveSat = rs.getString("SKU_CLAVE_SAT");
                            if (skuClaveSat != null && !skuClaveSat.trim().isEmpty()) {
                                concepto.put("claveProdServ", skuClaveSat);
                            }
                        } catch (Exception e) {}
                    }
                } catch (Exception e) {}
                
                try {
                    String objetoImp = rs.getString("OBJETO_IMPUESTO");
                    if (objetoImp != null && !objetoImp.trim().isEmpty()) {
                        concepto.put("objetoImp", objetoImp);
                    }
                } catch (Exception e) {}
                
                try {
                    java.math.BigDecimal tasaCatalogo = rs.getBigDecimal("TASA_IVA_CATALOGO");
                    if (tasaCatalogo != null) {
                        concepto.put("tasaIva", tasaCatalogo);
                    }
                } catch (Exception e) {}
                
                try {
                    String unidadCatalogo = rs.getString("UNIDAD_CATALOGO");
                    if (unidadCatalogo != null && !unidadCatalogo.trim().isEmpty()) {
                        concepto.put("unidad", unidadCatalogo);
                    }
                } catch (Exception e) {}
                
                return concepto;
            });
        } catch (Exception e) {
            logger.warn("Error al consultar conceptos desde CONCEPTOS para ID_FACTURA {}: {}", idFactura, e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    private boolean hasColumn(String table, String col) {
        try {
            Integer c = jdbcTemplate.query(
                    "SELECT COUNT(*) AS C FROM ALL_TAB_COLUMNS WHERE UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?",
                    rs -> rs.next() ? rs.getInt("C") : 0,
                    table.toUpperCase(), col.toUpperCase()
            );
            return c != null && c > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String pickFirst(String[] candidates) {
        for (String c : candidates) {
            if (hasColumn("FACTURAS", c)) return c;
        }
        return null;
    }
}