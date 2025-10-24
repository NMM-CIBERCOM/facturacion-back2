package com.cibercom.facturacion_back.dao;

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