package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.CartaPorteSaveRequest;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement;
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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
            final CartaPorteComplement complemento = request.getComplemento();
            final CartaPorteComplement.Ubicacion ubicacionOrigen = findUbicacion(complemento, "Origen");
            final CartaPorteComplement.Ubicacion ubicacionDestino = findUbicacion(complemento, "Destino");
            final String origenLabel = firstNonBlank(request.getOrigen(), resumenUbicacion(ubicacionOrigen));
            final String destinoLabel = firstNonBlank(request.getDestino(), resumenUbicacion(ubicacionDestino));
            final String fechaSalida = firstNonBlank(request.getFechaSalida(), ubicacionOrigen != null ? ubicacionOrigen.getFechaHoraSalidaLlegada() : null);
            final String fechaLlegada = firstNonBlank(request.getFechaLlegada(), ubicacionDestino != null ? ubicacionDestino.getFechaHoraSalidaLlegada() : null);

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
                ps.setString(25, origenLabel);
                ps.setString(26, destinoLabel);
                ps.setTimestamp(27, parseDate(fechaSalida));
                ps.setTimestamp(28, parseDate(fechaLlegada));
                
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
        String normalized = fecha.trim();
        try {
            if (normalized.endsWith("Z")) {
                Instant instant = Instant.parse(normalized);
                return Timestamp.from(instant);
            }
            if (normalized.contains("T")) {
                String iso = normalized;
                if (iso.length() == 16) {
                    iso = iso + ":00";
                }
                try {
                    LocalDateTime ldt = LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    return Timestamp.valueOf(ldt);
                } catch (Exception ex) {
                    OffsetDateTime odt = OffsetDateTime.parse(iso, DateTimeFormatter.ISO_DATE_TIME);
                    return Timestamp.from(odt.toInstant());
                }
            }
            if (normalized.length() == 10) {
                LocalDate date = LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE);
                return Timestamp.valueOf(date.atStartOfDay());
            }
            Date parsedDate = dateFormat.parse(normalized);
            return new Timestamp(parsedDate.getTime());
        } catch (Exception e) {
            logger.warn("Error al parsear fecha '{}', usando null", fecha);
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private CartaPorteComplement.Ubicacion findUbicacion(CartaPorteComplement complemento, String tipo) {
        if (complemento == null || complemento.getUbicaciones() == null || tipo == null) {
            return null;
        }
        return complemento.getUbicaciones().stream()
                .filter(u -> u.getTipoUbicacion() != null && u.getTipoUbicacion().equalsIgnoreCase(tipo))
                .findFirst()
                .orElse(null);
    }

    private String resumenUbicacion(CartaPorteComplement.Ubicacion ubicacion) {
        if (ubicacion == null) {
            return null;
        }
        CartaPorteComplement.Domicilio dom = ubicacion.getDomicilio();
        if (dom != null) {
            StringBuilder sb = new StringBuilder();
            appendSegment(sb, dom.getCalle());
            appendSegment(sb, dom.getMunicipio());
            appendSegment(sb, dom.getEstado());
            appendSegment(sb, dom.getCodigoPostal());
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        if (ubicacion.getNombreRemitenteDestinatario() != null && !ubicacion.getNombreRemitenteDestinatario().isBlank()) {
            return ubicacion.getNombreRemitenteDestinatario();
        }
        return ubicacion.getRfcRemitenteDestinatario();
    }

    private void appendSegment(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(value.trim());
    }

    /**
     * Actualiza el ID_FACTURA en la tabla CARTA_PORTE
     * @param idCartaPorte ID de la carta porte a actualizar
     * @param idFactura ID de la factura relacionada
     */
    public void actualizarIdFactura(Long idCartaPorte, Long idFactura) {
        if (idCartaPorte == null || idFactura == null) {
            logger.warn("No se puede actualizar ID_FACTURA: idCartaPorte={}, idFactura={}", idCartaPorte, idFactura);
            return;
        }
        
        String sql = "UPDATE CARTA_PORTE SET ID_FACTURA = ? WHERE ID_DATO_FISCAL = ?";
        try {
            int updated = jdbcTemplate.update(sql, idFactura, idCartaPorte);
            if (updated > 0) {
                logger.info("ID_FACTURA actualizado exitosamente: idCartaPorte={}, idFactura={}", idCartaPorte, idFactura);
            } else {
                logger.warn("No se encontró registro en CARTA_PORTE con ID: {}", idCartaPorte);
            }
        } catch (Exception e) {
            logger.error("Error al actualizar ID_FACTURA en CARTA_PORTE: {}", e.getMessage(), e);
            throw new DataAccessException("Error al actualizar ID_FACTURA: " + e.getMessage(), e) {};
        }
    }
    
    /**
     * Busca una carta porte por ID_FACTURA
     * @param idFactura ID de la factura relacionada
     * @return ID_DATO_FISCAL de la carta porte si existe, Optional.empty() si no existe
     */
    public java.util.Optional<Long> buscarPorIdFactura(Long idFactura) {
        if (idFactura == null) {
            return java.util.Optional.empty();
        }
        
        String sql = "SELECT ID_DATO_FISCAL FROM CARTA_PORTE WHERE ID_FACTURA = ?";
        try {
            java.util.List<Long> resultados = jdbcTemplate.query(sql, 
                (rs, rowNum) -> rs.getLong("ID_DATO_FISCAL"), 
                idFactura);
            
            if (resultados != null && !resultados.isEmpty()) {
                return java.util.Optional.of(resultados.get(0));
            }
            return java.util.Optional.empty();
        } catch (Exception e) {
            logger.warn("Error al buscar carta porte por ID_FACTURA {}: {}", idFactura, e.getMessage());
            return java.util.Optional.empty();
        }
    }
}