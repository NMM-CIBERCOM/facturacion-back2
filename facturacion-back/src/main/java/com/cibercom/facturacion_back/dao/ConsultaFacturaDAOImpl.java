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
                
                // Configurar parámetros del stored procedure
                int paramIndex = 1;
                
                // RFC Receptor
                if (request.getRfcReceptor() != null && !request.getRfcReceptor().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getRfcReceptor());
                    logger.debug("Parámetro {}: RFC Receptor = {}", paramIndex-1, request.getRfcReceptor());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: RFC Receptor = NULL", paramIndex-1);
                }
                
                // Nombre/Apellido (concatenado)
                String nombreCompleto = construirNombreCompleto(request);
                if (nombreCompleto != null && !nombreCompleto.trim().isEmpty()) {
                    stmt.setString(paramIndex++, nombreCompleto);
                    logger.debug("Parámetro {}: Nombre Completo = {}", paramIndex-1, nombreCompleto);
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Nombre Completo = NULL", paramIndex-1);
                }
                
                // Razón Social
                if (request.getRazonSocial() != null && !request.getRazonSocial().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getRazonSocial());
                    logger.debug("Parámetro {}: Razón Social = {}", paramIndex-1, request.getRazonSocial());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Razón Social = NULL", paramIndex-1);
                }
                
                // Almacén
                if (request.getAlmacen() != null && !request.getAlmacen().trim().isEmpty() && !"todos".equals(request.getAlmacen())) {
                    stmt.setString(paramIndex++, request.getAlmacen());
                    logger.debug("Parámetro {}: Almacén = {}", paramIndex-1, request.getAlmacen());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Almacén = NULL", paramIndex-1);
                }
                
                // Usuario
                if (request.getUsuario() != null && !request.getUsuario().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getUsuario());
                    logger.debug("Parámetro {}: Usuario = {}", paramIndex-1, request.getUsuario());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Usuario = NULL", paramIndex-1);
                }
                
                // Serie
                if (request.getSerie() != null && !request.getSerie().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getSerie());
                    logger.debug("Parámetro {}: Serie = {}", paramIndex-1, request.getSerie());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Serie = NULL", paramIndex-1);
                }
                
                // Folio
                if (request.getFolio() != null && !request.getFolio().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getFolio());
                    logger.debug("Parámetro {}: Folio = {}", paramIndex-1, request.getFolio());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: Folio = NULL", paramIndex-1);
                }
                
                // Fecha inicio
                if (request.getFechaInicio() != null) {
                    stmt.setDate(paramIndex++, Date.valueOf(request.getFechaInicio()));
                    logger.debug("Parámetro {}: Fecha Inicio = {}", paramIndex-1, request.getFechaInicio());
                } else {
                    stmt.setNull(paramIndex++, Types.DATE);
                    logger.debug("Parámetro {}: Fecha Inicio = NULL", paramIndex-1);
                }
                
                // Fecha fin
                if (request.getFechaFin() != null) {
                    stmt.setDate(paramIndex++, Date.valueOf(request.getFechaFin()));
                    logger.debug("Parámetro {}: Fecha Fin = {}", paramIndex-1, request.getFechaFin());
                } else {
                    stmt.setNull(paramIndex++, Types.DATE);
                    logger.debug("Parámetro {}: Fecha Fin = NULL", paramIndex-1);
                }
                
                // Perfil del usuario
                if (request.getPerfilUsuario() != null && !request.getPerfilUsuario().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getPerfilUsuario());
                    logger.debug("Parámetro {}: Perfil Usuario = {}", paramIndex-1, request.getPerfilUsuario());
                } else {
                    stmt.setString(paramIndex++, "OPERADOR"); // Perfil por defecto
                    logger.debug("Parámetro {}: Perfil Usuario = OPERADOR (por defecto)", paramIndex-1);
                }
                
                // UUID (opcional)
                if (request.getUuid() != null && !request.getUuid().trim().isEmpty()) {
                    stmt.setString(paramIndex++, request.getUuid());
                    logger.debug("Parámetro {}: UUID = {}", paramIndex-1, request.getUuid());
                } else {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                    logger.debug("Parámetro {}: UUID = NULL", paramIndex-1);
                }
                
                // Registrar parámetro OUT (REF CURSOR) en el índice 12
                stmt.registerOutParameter(12, OracleTypes.CURSOR);

                logger.info("Ejecutando stored procedure con {} parámetros IN y 1 OUT cursor", paramIndex - 1);

                // Ejecutar el stored procedure
                stmt.execute();

                // Recuperar el cursor de salida (índice 12)
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
                throw new RuntimeException("Error al ejecutar el stored procedure FEE_UTIL_PCK.buscaFacturas: " + e.getMessage(), e);
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

    @Override
    public boolean cancelarFactura(com.cibercom.facturacion_back.dto.CancelFacturaRequest request) {
        String updateSql =
            "UPDATE FACTURAS\n" +
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
        String sql = "UPDATE FACTURAS SET ESTADO='EN PROCESO DE CANCELACION' WHERE UUID=? AND ESTADO IN ('VIGENTE','ACTIVA','EMITIDA')";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error al marcar EN_PROCESO {}: {}", uuid, e.getMessage());
            throw new RuntimeException("Error marcando EN_PROCESO: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizarEstado(String uuid, String estado) {
        String sql = "UPDATE FACTURAS SET ESTADO=? WHERE UUID=?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, uuid);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error al actualizar estado {} a {}: {}", uuid, estado, e.getMessage());
            throw new RuntimeException("Error actualizando estado: " + e.getMessage(), e);
        }
    }

    @Override
    public FacturaInfo obtenerFacturaPorUuid(String uuid) {
        String sql = "SELECT UUID, EMISOR_RFC, RECEPTOR_RFC, FECHA_FACTURA, TOTAL, SERIE, FOLIO, TIENDA, ESTADO FROM FACTURAS WHERE UUID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FacturaInfo info = new FacturaInfo();
                    info.uuid = rs.getString("UUID");
                    info.rfcEmisor = rs.getString("EMISOR_RFC");
                    info.rfcReceptor = rs.getString("RECEPTOR_RFC");
                    java.sql.Timestamp ts = rs.getTimestamp("FECHA_FACTURA");
                    if (ts != null) info.fechaFactura = ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
                    info.total = rs.getBigDecimal("TOTAL");
                    info.serie = rs.getString("SERIE");
                    info.folio = rs.getString("FOLIO");
                    info.tienda = rs.getString("TIENDA");
                    info.estatus = rs.getString("ESTADO");
                    return info;
                }
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo factura por UUID {}: {}", uuid, e.getMessage());
            throw new RuntimeException("Error consultando factura por UUID: " + e.getMessage(), e);
        }
    }
    
    /**
     * Construye el nombre completo a partir de los campos individuales
     */
    private String construirNombreCompleto(ConsultaFacturaRequest request) {
        StringBuilder nombreCompleto = new StringBuilder();
        
        if (request.getNombreCliente() != null && !request.getNombreCliente().trim().isEmpty()) {
            nombreCompleto.append(request.getNombreCliente().trim());
        }
        
        if (request.getApellidoPaterno() != null && !request.getApellidoPaterno().trim().isEmpty()) {
            if (nombreCompleto.length() > 0) nombreCompleto.append(" ");
            nombreCompleto.append(request.getApellidoPaterno().trim());
        }
        
        if (request.getApellidoMaterno() != null && !request.getApellidoMaterno().trim().isEmpty()) {
            if (nombreCompleto.length() > 0) nombreCompleto.append(" ");
            nombreCompleto.append(request.getApellidoMaterno().trim());
        }
        
        return nombreCompleto.length() > 0 ? nombreCompleto.toString() : null;
    }
    
    /**
     * Mapea el resultado de la base de datos a un DTO
     */
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
        
        // Determinar si permite cancelación basado en el campo del stored procedure
        String permiteCancelacion = rs.getString("PERMITE_CANCELACION");
        factura.setPermiteCancelacion("SI".equals(permiteCancelacion));
        
        return factura;
    }
}
