package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.CartaPorteSaveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Repository
@Profile("oracle")
public class CartaPorteDAO {

    private static final Logger logger = LoggerFactory.getLogger(CartaPorteDAO.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public Long insertar(CartaPorteSaveRequest request) throws DataAccessException {
        logger.info("Insertando carta porte en base de datos para RFC: {}", request.getRfcCompleto());
        
        String sql = """
            INSERT INTO CARTA_PORTE (
                RFC, CORREO_ELECTRONICO, RAZON_SOCIAL, NOMBRE, APELLIDO_PATERNO, APELLIDO_MATERNO,
                PAIS, NO_REGISTRO_TRIB, DOMICILIO_FISCAL, REGIMEN_FISCAL, USO_CFDI,
                DESCRIPCION, NUMERO_SERIE, PRECIO, PERSONA_AUTORIZA, PUESTO_AUTORIZA,
                TIPO_TRANSPORTE, PERMISO_SCT, NO_PERMISO_SCT, PLACAS_VEHICULO, CONFIG_VEHICULAR,
                NOMBRE_TRANSPORTISTA, RFC_TRANSPORTISTA, BIENES_TRANSPORTADOS,
                ORIGEN_DOMICILIO, DESTINO_DOMICILIO, FECHA_SALIDA, FECHA_LLEGADA,
                USUARIO_CREACION
            ) VALUES (
                ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?, ?,
                ?
            )
        """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"ID_DATO_FISCAL"});
                
                // Datos fiscales del receptor
                ps.setString(1, request.getRfcCompleto());
                ps.setString(2, request.getCorreoElectronico());
                ps.setString(3, request.getRazonSocial());
                ps.setString(4, request.getNombre());
                ps.setString(5, request.getPaterno());
                ps.setString(6, request.getMaterno());
                
                // Datos adicionales
                ps.setString(7, request.getPais() != null ? request.getPais() : "México");
                ps.setString(8, request.getNoRegistroIdentidadTributaria());
                ps.setString(9, request.getDomicilioFiscal());
                ps.setString(10, request.getRegimenFiscal());
                ps.setString(11, request.getUsoCfdi());
                
                // Información general
                ps.setString(12, request.getDescripcion());
                ps.setString(13, request.getNumeroSerie());
                ps.setBigDecimal(14, parsePrecio(request.getPrecio()));
                ps.setString(15, request.getPersonaAutoriza());
                ps.setString(16, request.getPuesto());
                
                // Datos de transporte
                ps.setString(17, request.getTipoTransporte());
                ps.setString(18, request.getPermisoSCT());
                ps.setString(19, request.getNumeroPermisoSCT());
                ps.setString(20, request.getPlacasVehiculo());
                ps.setString(21, request.getConfigVehicular());
                ps.setString(22, request.getNombreTransportista());
                ps.setString(23, request.getRfcTransportista());
                ps.setString(24, request.getBienesTransportados());
                
                // Origen y destino
                ps.setString(25, request.getOrigen());
                ps.setString(26, request.getDestino());
                ps.setTimestamp(27, parseDate(request.getFechaSalida()));
                ps.setTimestamp(28, parseDate(request.getFechaLlegada()));
                
                // Usuario de creación (por ahora hardcodeado, se puede mejorar con autenticación)
                ps.setString(29, "SISTEMA");
                
                return ps;
            }, keyHolder);
            
            Number generatedId = keyHolder.getKey();
            if (generatedId != null) {
                Long id = generatedId.longValue();
                logger.info("Carta porte insertada exitosamente con ID: {}", id);
                return id;
            } else {
                throw new RuntimeException("No se pudo obtener el ID generado");
            }
            
        } catch (Exception e) {
            logger.error("Error al insertar carta porte: {}", e.getMessage(), e);
            throw new DataAccessException("Error al insertar carta porte: " + e.getMessage(), e) {};
        }
    }
    
    private BigDecimal parsePrecio(String precio) {
        if (precio == null || precio.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            // Remover comas y espacios para parsear correctamente
            String cleanPrecio = precio.replaceAll("[,\\s]", "");
            return new BigDecimal(cleanPrecio);
        } catch (NumberFormatException e) {
            logger.warn("Error al parsear precio '{}', usando 0.00", precio);
            return BigDecimal.ZERO;
        }
    }
    
    private Timestamp parseDate(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }
        try {
            Date parsedDate = dateFormat.parse(fecha);
            return new Timestamp(parsedDate.getTime());
        } catch (ParseException e) {
            logger.warn("Error al parsear fecha '{}', usando null", fecha);
            return null;
        }
    }
}