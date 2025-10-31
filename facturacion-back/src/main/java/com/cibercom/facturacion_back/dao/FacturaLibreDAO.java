package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.FacturaLibreSaveRequest;
import com.cibercom.facturacion_back.model.ClienteCatalogo;
import com.cibercom.facturacion_back.service.ClienteCatalogoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FacturaLibreDAO {

    private static final Logger logger = LoggerFactory.getLogger(FacturaLibreDAO.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ClienteCatalogoService clienteCatalogoService;
    
    public String insertar(FacturaLibreSaveRequest request) throws Exception {
        logger.info("Insertando factura libre en FACTURAS");

        // Resolver ID_RECEPTOR basado en los datos fiscales
        Long idReceptor = resolverIdReceptorPorRfc(request);
        logger.info("ID_RECEPTOR resuelto: {}", idReceptor);
        
        Connection connection = dataSource.getConnection();
        
        // Mapear a las columnas existentes en la tabla FACTURAS
        String sql = "INSERT INTO FACTURAS (RFC_R, EMAIL_ENVIO, RAZON_SOCIAL, USO_CFDI, METODO_PAGO, FORMA_PAGO, " +
                    "SUBTOTAL, DESCUENTO, IVA16, IMPORTE, TIENDA_BOL, TERMINAL_BOL, BOLETA_BOL, " +
                    "UUID, COMENTARIOS, FECHA, SERIE, FOLIO, RFC_E, TIPO_FACTURA, USUARIO, TIENDA_ORIGEN, SISTEMA_ORIGEN, ID_RECEPTOR) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // RFC del receptor (cliente)
            stmt.setString(1, request.getRfc());
            
            // Email
            stmt.setString(2, request.getCorreoElectronico());
            
            // Razón social
            stmt.setString(3, request.getRazonSocial());
            
            // Uso CFDI
            stmt.setString(4, request.getUsoCfdi());
            
            // Método de pago
            stmt.setString(5, request.getMedioPago());
            
            // Forma de pago
            stmt.setString(6, request.getFormaPago());
            
            // Subtotal
            if (request.getSubtotal() != null) {
                stmt.setBigDecimal(7, request.getSubtotal());
            } else {
                stmt.setBigDecimal(7, BigDecimal.ZERO);
            }
            
            // Descuento
            if (request.getDescuentoTotal() != null) {
                stmt.setBigDecimal(8, request.getDescuentoTotal());
            } else {
                stmt.setBigDecimal(8, BigDecimal.ZERO);
            }
            
            // IVA (usando IVA16 como campo principal)
            if (request.getIvaTotal() != null) {
                stmt.setBigDecimal(9, request.getIvaTotal());
            } else {
                stmt.setBigDecimal(9, BigDecimal.ZERO);
            }
            
            // Total (IMPORTE)
            if (request.getTotal() != null) {
                stmt.setBigDecimal(10, request.getTotal());
            } else {
                stmt.setBigDecimal(10, BigDecimal.ZERO);
            }
            
            // Datos de boleta
            if (request.getTienda() != null && !request.getTienda().trim().isEmpty()) {
                try {
                    stmt.setInt(11, Integer.parseInt(request.getTienda()));
                } catch (NumberFormatException e) {
                    stmt.setNull(11, java.sql.Types.INTEGER);
                }
            } else {
                stmt.setNull(11, java.sql.Types.INTEGER);
            }
            
            if (request.getTerminal() != null && !request.getTerminal().trim().isEmpty()) {
                try {
                    stmt.setInt(12, Integer.parseInt(request.getTerminal()));
                } catch (NumberFormatException e) {
                    stmt.setNull(12, java.sql.Types.INTEGER);
                }
            } else {
                stmt.setNull(12, java.sql.Types.INTEGER);
            }
            
            if (request.getBoleta() != null && !request.getBoleta().trim().isEmpty()) {
                try {
                    stmt.setInt(13, Integer.parseInt(request.getBoleta()));
                } catch (NumberFormatException e) {
                    stmt.setNull(13, java.sql.Types.INTEGER);
                }
            } else {
                stmt.setNull(13, java.sql.Types.INTEGER);
            }
            
            // UUID - generar automáticamente si no existe
            String uuid = request.getUuid();
            if (uuid == null || uuid.trim().isEmpty()) {
                uuid = UUID.randomUUID().toString();
                logger.info("UUID generado automáticamente: {}", uuid);
            }
            stmt.setString(14, uuid);

            // Comentarios
            stmt.setString(15, request.getComentarios());

            // SERIE
            stmt.setString(16, "LIB");

            // FOLIO
            stmt.setInt(17, 1);

            // RFC_E (RFC Emisor)
            stmt.setString(18, "CIBERCOM");

            // TIPO_FACTURA
            stmt.setInt(19, 1);

            // USUARIO
            stmt.setInt(20, 1);

            // TIENDA_ORIGEN
            stmt.setInt(21, 1);

            // SISTEMA_ORIGEN
            stmt.setString(22, "LIBRE");

            // ID_RECEPTOR
            if (idReceptor != null) {
                stmt.setLong(23, idReceptor);
            } else {
                stmt.setNull(23, java.sql.Types.BIGINT);
            }

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("Factura libre insertada exitosamente con UUID: {}", uuid);
                return uuid;
            } else {
                throw new SQLException("No se pudo insertar la factura libre.");
            }
        } finally {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }
    
    private Long resolverIdReceptorPorRfc(FacturaLibreSaveRequest request) {
         try {
             String rfc = request.getRfc();
             if (rfc == null || rfc.trim().isEmpty()) {
                 logger.warn("RFC receptor vacío o nulo");
                 return null;
             }
             
             String normalized = rfc.trim().toUpperCase();
             Optional<ClienteCatalogo> cliente = clienteCatalogoService.buscarPorRfc(normalized);
             if (cliente.isPresent()) {
                 logger.info("Cliente encontrado en catálogo con ID: {}", cliente.get().getIdCliente());
                 return cliente.get().getIdCliente();
             } else {
                 logger.warn("Cliente no encontrado en catálogo para RFC: {}", normalized);
                 // Por ahora retornamos null, pero se podría crear el cliente aquí
                 return null;
             }
         } catch (Exception e) {
             logger.error("Error al resolver ID_RECEPTOR para RFC {}: {}", request.getRfc(), e.getMessage());
             return null;
         }
     }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            logger.warn("Error al parsear BigDecimal: {}", value);
            return BigDecimal.ZERO;
        }
    }
}