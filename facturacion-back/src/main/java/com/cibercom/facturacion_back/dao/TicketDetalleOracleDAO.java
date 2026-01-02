package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.TicketDetalleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@Profile("oracle")
public class TicketDetalleOracleDAO implements TicketDetalleDAO {

    private static final Logger logger = LoggerFactory.getLogger(TicketDetalleOracleDAO.class);
    private final JdbcTemplate jdbcTemplate;

    public TicketDetalleOracleDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TicketDetalleDto> buscarPorIdTicket(Long idTicket) {
        if (idTicket == null) {
            throw new IllegalArgumentException("ID_TICKET no puede ser nulo");
        }
        String sql = "SELECT " +
                "ID_DETALLE, ID_TICKET, PRODUCTO_ID, DESCRIPCION, CANTIDAD, UNIDAD, " +
                "PRECIO_UNITARIO, DESCUENTO, SUBTOTAL, IVA_PORCENTAJE, IVA_IMPORTE, " +
                "IEPS_PORCENTAJE, IEPS_IMPORTE, TOTAL " +
                "FROM TICKETS_DETALLE WHERE ID_TICKET = ?";
        logger.info("Consulta TICKETS_DETALLE SQL: {}", sql);
        return jdbcTemplate.query(sql, new Object[]{idTicket}, mapper());
    }

    @Override
    public List<TicketDetalleDto> buscarPorIdFactura(Long idFactura) {
        if (idFactura == null) {
            throw new IllegalArgumentException("ID_FACTURA no puede ser nulo");
        }
        // Usamos JOIN para obtener detalles asociados a la factura por medio de TICKETS.ID_TICKET
        // LEFT JOIN con CATALOGOS_PRODUCTOS_SERVICIOS para obtener campos del catálogo
        // Relacionamos por PRODUCTO_ID = ID del catálogo, y también por DESCRIPCION y ID_USUARIO
        // Incluimos ID_USUARIO de la factura para filtrar el catálogo correcto
        String sql = "SELECT d.ID_DETALLE, d.ID_TICKET, d.PRODUCTO_ID, d.DESCRIPCION, d.CANTIDAD, d.UNIDAD, " +
                "d.PRECIO_UNITARIO, d.DESCUENTO, d.SUBTOTAL, d.IVA_PORCENTAJE, d.IVA_IMPORTE, " +
                "d.IEPS_PORCENTAJE, d.IEPS_IMPORTE, d.TOTAL, " +
                "c.CLAVE_PROD_SERV, c.OBJETO_IMPUESTO, c.TASA_IVA, c.UNIDAD AS UNIDAD_CATALOGO, c.CLAVE_UNIDAD " +
                "FROM TICKETS_DETALLE d " +
                "JOIN TICKETS t ON t.ID_TICKET = d.ID_TICKET " +
                "LEFT JOIN FACTURAS f ON f.ID_FACTURA = t.ID_FACTURA " +
                "LEFT JOIN CATALOGOS_PRODUCTOS_SERVICIOS c ON " +
                "  ((c.ID = d.PRODUCTO_ID OR " +
                "    (c.DESCRIPCION = d.DESCRIPCION AND c.ID_USUARIO = f.USUARIO)) " +
                "   AND c.ACTIVO = '1') " +
                "WHERE t.ID_FACTURA = ?";
        logger.info("Consulta TICKETS_DETALLE por ID_FACTURA con catálogo SQL: {}", sql);
        return jdbcTemplate.query(sql, new Object[]{idFactura}, mapperConCatalogo());
    }

    private RowMapper<TicketDetalleDto> mapper() {
        return new RowMapper<TicketDetalleDto>() {
            @Override
            public TicketDetalleDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                TicketDetalleDto d = new TicketDetalleDto();
                d.setIdDetalle(getLong(rs, "ID_DETALLE"));
                d.setIdTicket(getLong(rs, "ID_TICKET"));
                d.setProductoId(getLong(rs, "PRODUCTO_ID"));
                d.setDescripcion(getString(rs, "DESCRIPCION"));
                d.setCantidad(getBD(rs, "CANTIDAD"));
                d.setUnidad(getString(rs, "UNIDAD"));
                d.setPrecioUnitario(getBD(rs, "PRECIO_UNITARIO"));
                d.setDescuento(getBD(rs, "DESCUENTO"));
                d.setSubtotal(getBD(rs, "SUBTOTAL"));
                d.setIvaPorcentaje(getBD(rs, "IVA_PORCENTAJE"));
                d.setIvaImporte(getBD(rs, "IVA_IMPORTE"));
                d.setIepsPorcentaje(getBD(rs, "IEPS_PORCENTAJE"));
                d.setIepsImporte(getBD(rs, "IEPS_IMPORTE"));
                d.setTotal(getBD(rs, "TOTAL"));
                return d;
            }
        };
    }
    
    private RowMapper<TicketDetalleDto> mapperConCatalogo() {
        return new RowMapper<TicketDetalleDto>() {
            @Override
            public TicketDetalleDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                TicketDetalleDto d = new TicketDetalleDto();
                d.setIdDetalle(getLong(rs, "ID_DETALLE"));
                d.setIdTicket(getLong(rs, "ID_TICKET"));
                d.setProductoId(getLong(rs, "PRODUCTO_ID"));
                d.setDescripcion(getString(rs, "DESCRIPCION"));
                d.setCantidad(getBD(rs, "CANTIDAD"));
                d.setUnidad(getString(rs, "UNIDAD"));
                d.setPrecioUnitario(getBD(rs, "PRECIO_UNITARIO"));
                d.setDescuento(getBD(rs, "DESCUENTO"));
                d.setSubtotal(getBD(rs, "SUBTOTAL"));
                d.setIvaPorcentaje(getBD(rs, "IVA_PORCENTAJE"));
                d.setIvaImporte(getBD(rs, "IVA_IMPORTE"));
                d.setIepsPorcentaje(getBD(rs, "IEPS_PORCENTAJE"));
                d.setIepsImporte(getBD(rs, "IEPS_IMPORTE"));
                d.setTotal(getBD(rs, "TOTAL"));
                // Campos del catálogo
                d.setClaveProdServ(getString(rs, "CLAVE_PROD_SERV"));
                d.setObjetoImpuesto(getString(rs, "OBJETO_IMPUESTO"));
                d.setTasaIva(getBD(rs, "TASA_IVA"));
                d.setUnidadCatalogo(getString(rs, "UNIDAD_CATALOGO"));
                d.setClaveUnidad(getString(rs, "CLAVE_UNIDAD"));
                return d;
            }
        };
    }

    private java.math.BigDecimal getBD(ResultSet rs, String col) throws SQLException {
        try { return rs.getBigDecimal(col); } catch (SQLException e) { return null; }
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
}