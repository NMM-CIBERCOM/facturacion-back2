package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.NominaSaveRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
@Profile("oracle")
public class NominaOracleDAO {

    private final JdbcTemplate jdbcTemplate;
    private volatile String lastInsertError;

    public NominaOracleDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Obtiene datos de nómina vinculados a una factura por su ID_FACTURA.
     * Devuelve un mapa tolerante a esquemas (solo incluye columnas existentes).
     */
    public java.util.Map<String, Object> buscarPorIdFactura(Long idFactura) {
        try {
            if (idFactura == null) return null;
            // Verificar columna FK
            if (!hasColumn("NOMINAS", "ID_FACTURA")) {
                return null;
            }

            // Detectar columnas disponibles de NOMINAS
            boolean hasIdEmpleado = hasColumn("NOMINAS", "ID_EMPLEADO");
            boolean hasFechaPago = hasColumn("NOMINAS", "FECHA_PAGO");
            boolean hasFechaNomina = hasColumn("NOMINAS", "FECHA_NOMINA");
            boolean hasRfcEmisor = hasColumn("NOMINAS", "RFC_EMISOR");
            boolean hasRfcReceptor = hasColumn("NOMINAS", "RFC_RECEPTOR");
            boolean hasNombre = hasColumn("NOMINAS", "NOMBRE");
            boolean hasCurp = hasColumn("NOMINAS", "CURP");
            boolean hasPeriodoPago = hasColumn("NOMINAS", "PERIODO_PAGO");
            boolean hasPercepciones = hasColumn("NOMINAS", "PERCEPCIONES");
            boolean hasDeducciones = hasColumn("NOMINAS", "DEDUCCIONES");
            boolean hasTipoNomina = hasColumn("NOMINAS", "TIPO_NOMINA");
            boolean hasUsoCfdi = hasColumn("NOMINAS", "USO_CFDI");
            boolean hasCorreo = hasColumn("NOMINAS", "CORREO_ELECTRONICO");

            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ");
            if (hasIdEmpleado) sb.append("ID_EMPLEADO, ");
            if (hasNombre) sb.append("NOMBRE, ");
            if (hasCurp) sb.append("CURP, ");
            if (hasRfcEmisor) sb.append("RFC_EMISOR, ");
            if (hasRfcReceptor) sb.append("RFC_RECEPTOR, ");
            if (hasPeriodoPago) sb.append("PERIODO_PAGO, ");
            if (hasFechaPago) sb.append("FECHA_PAGO, ");
            if (hasFechaNomina) sb.append("FECHA_NOMINA, ");
            if (hasPercepciones) sb.append("PERCEPCIONES, ");
            if (hasDeducciones) sb.append("DEDUCCIONES, ");
            if (hasTipoNomina) sb.append("TIPO_NOMINA, ");
            if (hasUsoCfdi) sb.append("USO_CFDI, ");
            if (hasCorreo) sb.append("CORREO_ELECTRONICO, ");
            // quitar última coma si existe
            int lastComma = sb.lastIndexOf(", ");
            if (lastComma > 7) sb.delete(lastComma, lastComma + 2);
            sb.append(" FROM NOMINAS WHERE ID_FACTURA = ?");

            String sql = sb.toString();
            return jdbcTemplate.query(sql, new Object[]{idFactura}, rs -> {
                if (!rs.next()) return null;
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                try { if (hasIdEmpleado) map.put("idEmpleado", rs.getString("ID_EMPLEADO")); } catch (Exception ignore) {}
                try { if (hasNombre) map.put("nombre", rs.getString("NOMBRE")); } catch (Exception ignore) {}
                try { if (hasCurp) map.put("curp", rs.getString("CURP")); } catch (Exception ignore) {}
                try { if (hasRfcEmisor) map.put("rfcEmisor", rs.getString("RFC_EMISOR")); } catch (Exception ignore) {}
                try { if (hasRfcReceptor) map.put("rfcReceptor", rs.getString("RFC_RECEPTOR")); } catch (Exception ignore) {}
                try { if (hasPeriodoPago) map.put("periodoPago", rs.getString("PERIODO_PAGO")); } catch (Exception ignore) {}
                try { if (hasFechaPago) map.put("fechaPago", rs.getDate("FECHA_PAGO") != null ? rs.getDate("FECHA_PAGO").toLocalDate().toString() : ""); } catch (Exception ignore) {}
                try { if (hasFechaNomina) map.put("fechaNomina", rs.getDate("FECHA_NOMINA") != null ? rs.getDate("FECHA_NOMINA").toLocalDate().toString() : ""); } catch (Exception ignore) {}
                try { if (hasPercepciones) map.put("percepciones", rs.getBigDecimal("PERCEPCIONES")); } catch (Exception ignore) {}
                try { if (hasDeducciones) map.put("deducciones", rs.getBigDecimal("DEDUCCIONES")); } catch (Exception ignore) {}
                try { if (hasTipoNomina) map.put("tipoNomina", rs.getString("TIPO_NOMINA")); } catch (Exception ignore) {}
                try { if (hasUsoCfdi) map.put("usoCfdi", rs.getString("USO_CFDI")); } catch (Exception ignore) {}
                try { if (hasCorreo) map.put("correoElectronico", rs.getString("CORREO_ELECTRONICO")); } catch (Exception ignore) {}
                return map;
            });
        } catch (Exception e) {
            this.lastInsertError = e.getMessage();
            return null;
        }
    }

    public String getLastInsertError() { return lastInsertError; }

    public Long insertar(Long idFactura, NominaSaveRequest req) {
        lastInsertError = null;
        try {
            String sql = "INSERT INTO NOMINAS (\n" +
                    "  ID_FACTURA, ID_EMPLEADO, FECHA_NOMINA, RFC_EMISOR, RFC_RECEPTOR, NOMBRE, CURP, \n" +
                    "  PERIODO_PAGO, FECHA_PAGO, PERCEPCIONES, DEDUCCIONES, TIPO_NOMINA, USO_CFDI, CORREO_ELECTRONICO, USUARIO_CREACION\n" +
                    ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

            KeyHolder kh = new GeneratedKeyHolder();
            int updated = jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID_FACTURA_NOMINA"});
                ps.setLong(1, idFactura);
                ps.setString(2, nullToEmpty(req.getIdEmpleado()));
                ps.setDate(3, parseDate(req.getFechaNomina()));
                ps.setString(4, nullToEmpty(req.getRfcEmisor()));
                ps.setString(5, nullToEmpty(req.getRfcReceptor()));
                ps.setString(6, nullToEmpty(req.getNombre()));
                ps.setString(7, nullToEmpty(req.getCurp()));
                ps.setString(8, nullToEmpty(req.getPeriodoPago()));
                ps.setDate(9, parseDate(req.getFechaPago()));
                ps.setBigDecimal(10, parseDecimal(req.getPercepciones()));
                ps.setBigDecimal(11, parseDecimal(req.getDeducciones()));
                ps.setString(12, nullToEmpty(req.getTipoNomina()));
                ps.setString(13, nullToEmpty(req.getUsoCfdi()));
                ps.setString(14, nullToEmpty(req.getCorreoElectronico()));
                ps.setString(15, nullToEmpty(req.getUsuarioCreacion()));
                return ps;
            }, kh);

            if (updated <= 0) return null;
            try {
                Number n = kh.getKey();
                return n != null ? n.longValue() : null;
            } catch (Exception ignore) {
                return null;
            }
        } catch (Exception e) {
            lastInsertError = e.getMessage();
            return null;
        }
    }

    /**
     * Consulta historial de nóminas por ID_EMPLEADO.
     * Devuelve ID de nómina, ID empleado, fecha (preferente FECHA_PAGO) y estado de la factura.
     */
    public java.util.List<com.cibercom.facturacion_back.dto.NominaHistorialDTO> buscarHistorialPorEmpleado(String idEmpleado) {
        // Detectar columnas disponibles para evitar errores de SQL en instalaciones con esquemas variables
        String idCol = pickFirstAvailable("NOMINAS", "ID_FACTURA_NOMINA", "ID_NOMINA", "ID");
        boolean hasFechaPago = hasColumn("NOMINAS", "FECHA_PAGO");
        boolean hasFechaNomina = hasColumn("NOMINAS", "FECHA_NOMINA");
        boolean joinOnFacturas = hasColumn("NOMINAS", "ID_FACTURA") && hasColumn("FACTURAS", "ID_FACTURA");
        boolean hasEstado = joinOnFacturas && hasColumn("FACTURAS", "ESTADO");
        String uuidCol = joinOnFacturas ? pickFirstAvailable("FACTURAS",
                "UUID", "FOLIO_FISCAL", "FOLIO_FISCAL_SAT", "UUID_CFDI", "FOLIO_FISCAL_UUID"
        ) : null;

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        if (idCol != null) sb.append("n.").append(idCol).append(" AS IDSEL, "); else sb.append("n.ID_EMPLEADO AS IDSEL, ");
        sb.append("n.ID_EMPLEADO");
        if (hasFechaPago) sb.append(", n.FECHA_PAGO");
        if (hasFechaNomina) sb.append(", n.FECHA_NOMINA");
        if (hasEstado) sb.append(", f.ESTADO");
        if (uuidCol != null) sb.append(", f.").append(uuidCol).append(" AS UUIDSEL");
        sb.append(" FROM NOMINAS n ");
        if (joinOnFacturas) sb.append("LEFT JOIN FACTURAS f ON f.ID_FACTURA = n.ID_FACTURA ");
        sb.append("WHERE n.ID_EMPLEADO = ? ");
        if (hasFechaPago) sb.append("ORDER BY n.FECHA_PAGO DESC");
        else if (hasFechaNomina) sb.append("ORDER BY n.FECHA_NOMINA DESC");
        else if (idCol != null) sb.append("ORDER BY IDSEL DESC");

        String sql = sb.toString();

        return jdbcTemplate.query(sql, new Object[]{idEmpleado}, (rs, rowNum) -> {
            com.cibercom.facturacion_back.dto.NominaHistorialDTO dto = new com.cibercom.facturacion_back.dto.NominaHistorialDTO();
            try { dto.setId(rs.getLong("IDSEL")); } catch (Exception ignore) { dto.setId(null); }
            dto.setIdEmpleado(rs.getString("ID_EMPLEADO"));
            java.sql.Date fp = null, fn = null;
            try { fp = hasFechaPago ? rs.getDate("FECHA_PAGO") : null; } catch (Exception ignore) {}
            try { fn = hasFechaNomina ? rs.getDate("FECHA_NOMINA") : null; } catch (Exception ignore) {}
            java.sql.Date fecha = fp != null ? fp : fn;
            String fechaStr;
            if (fecha != null) {
                java.time.LocalDate ld = fecha.toLocalDate();
                fechaStr = ld.toString(); // YYYY-MM-DD
            } else {
                fechaStr = "";
            }
            dto.setFecha(fechaStr);
            String estado = null;
            try { estado = rs.getString("ESTADO"); } catch (Exception ignore) {}
            dto.setEstado(estado != null ? estado : "");

            String uuid = null;
            try { uuid = rs.getString("UUIDSEL"); } catch (Exception ignore) {}
            dto.setUuid(uuid != null ? uuid : "");
            return dto;
        });
    }

    private java.sql.Date parseDate(String s) {
        try {
            if (s == null || s.isBlank()) return null;
            java.time.LocalDate ld = java.time.LocalDate.parse(s);
            return java.sql.Date.valueOf(ld);
        } catch (Exception e) {
            return null; // dejar que DB maneje default/NULL si permitido
        }
    }

    private java.math.BigDecimal parseDecimal(String s) {
        try {
            if (s == null || s.isBlank()) return java.math.BigDecimal.ZERO;
            return new java.math.BigDecimal(s.trim());
        } catch (Exception e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private boolean hasColumn(String table, String column) {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM ALL_TAB_COLUMNS WHERE UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?",
                    Integer.class,
                    table.toUpperCase(),
                    column.toUpperCase()
            );
            return cnt != null && cnt > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String pickFirstAvailable(String table, String... candidates) {
        for (String c : candidates) {
            if (hasColumn(table, c)) return c;
        }
        return null;
    }
}