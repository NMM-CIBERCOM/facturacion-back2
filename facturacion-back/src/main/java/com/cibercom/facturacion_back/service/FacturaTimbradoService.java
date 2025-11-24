package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.FacturaRequest;
import com.cibercom.facturacion_back.dto.FacturaResponse;
import com.cibercom.facturacion_back.dto.PacTimbradoRequest;
import com.cibercom.facturacion_back.dto.PacTimbradoResponse;
import com.cibercom.facturacion_back.dto.SatValidationRequest;
import com.cibercom.facturacion_back.dto.SatValidationResponse;
import com.cibercom.facturacion_back.model.Factura;
import com.cibercom.facturacion_back.model.FacturaMongo;
import com.cibercom.facturacion_back.model.EstadoFactura;
import com.cibercom.facturacion_back.repository.FacturaRepository;
import com.cibercom.facturacion_back.repository.FacturaMongoRepository;
import com.cibercom.facturacion_back.integration.PacClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FacturaTimbradoService {

    private static final Logger logger = LoggerFactory.getLogger(FacturaTimbradoService.class);

    @Autowired
    private SatValidationService satValidationService;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private FacturaMongoRepository facturaMongoRepository;

    @Autowired
    private PacClient pacClient;

    @Autowired
    private Environment environment;

    public FacturaResponse iniciarTimbrado(FacturaRequest request) {
        try {
            SatValidationRequest emisorRequest = new SatValidationRequest();
            emisorRequest.setNombre(request.getNombreEmisor());
            emisorRequest.setRfc(request.getRfcEmisor());
            emisorRequest.setCodigoPostal(request.getCodigoPostalEmisor());
            emisorRequest.setRegimenFiscal(request.getRegimenFiscalEmisor());

            SatValidationResponse validacionEmisor = satValidationService.validarDatosSat(emisorRequest);
            if (!validacionEmisor.isValido()) {
                return FacturaResponse.builder()
                        .exitoso(false)
                        .mensaje("Datos del emisor inválidos")
                        .timestamp(LocalDateTime.now())
                        .errores("Errores en emisor: " + String.join(", ", validacionEmisor.getErrores()))
                        .build();
            }

            SatValidationRequest receptorRequest = new SatValidationRequest();
            receptorRequest.setNombre(request.getNombreReceptor());
            receptorRequest.setRfc(request.getRfcReceptor());
            receptorRequest.setCodigoPostal(request.getCodigoPostalReceptor());
            receptorRequest.setRegimenFiscal(request.getRegimenFiscalReceptor());

            SatValidationResponse validacionReceptor = satValidationService.validarDatosSat(receptorRequest);
            if (!validacionReceptor.isValido()) {
                return FacturaResponse.builder()
                        .exitoso(false)
                        .mensaje("Datos del receptor inválidos")
                        .timestamp(LocalDateTime.now())
                        .errores("Errores en receptor: " + String.join(", ", validacionReceptor.getErrores()))
                        .build();
            }

            // 3. Calcular totales
            BigDecimal subtotal = BigDecimal.ZERO;
            for (FacturaRequest.Concepto concepto : request.getConceptos()) {
                subtotal = subtotal.add(concepto.getImporte());
            }

            BigDecimal iva = subtotal.multiply(new BigDecimal("0.16")); // 16% IVA
            BigDecimal total = subtotal.add(iva);

            // 4. Generar XML según lineamientos del SAT
            String xml = generarXMLFactura(request, subtotal, iva, total);

            // 5. Enviar solicitud de timbrado al PAC (sin guardar antes)
            PacTimbradoRequest pacRequest = construirPacTimbradoRequest(request, xml, total);
            PacTimbradoResponse pacResponse = pacClient.solicitarTimbrado(pacRequest);

            // 6. Procesar respuesta del PAC
            if (pacResponse != null && Boolean.TRUE.equals(pacResponse.getOk())) {
                // Finkok retorna "TIMBRADO" cuando es exitoso, también aceptar "0" para compatibilidad
                String status = pacResponse.getStatus();
                if ("TIMBRADO".equals(status) || "TIMBRADO_PREVIAMENTE".equals(status) || "0".equals(status)) {
                    // Timbrado inmediato exitoso - Guardar directamente con UUID de Finkok y estado EMITIDA
                    String uuidFinkok = pacResponse.getUuid();
                    if (uuidFinkok == null || uuidFinkok.isBlank()) {
                        return FacturaResponse.builder()
                                .exitoso(false)
                                .mensaje("Error: Finkok no devolvió UUID en la respuesta")
                                .timestamp(LocalDateTime.now())
                                .errores("UUID no disponible en respuesta de Finkok")
                                .build();
                    }
                    // Normalizar formato del UUID de Finkok: asegurar que tenga guiones en formato estándar (8-4-4-4-12)
                    uuidFinkok = normalizarUUIDConGuiones(uuidFinkok);
                    
                    // Guardar factura directamente con UUID de Finkok y estado EMITIDA
                    guardarFacturaTimbrada(request, pacResponse, subtotal, iva, total, uuidFinkok);
                    
                    return construirRespuestaExitosa(request, subtotal, iva, total, pacResponse, uuidFinkok);
                } else if ("4".equals(status) || "EN_PROCESO_EMISION".equalsIgnoreCase(status)) {
                    // Timbrado en proceso (asíncrono) - EN_PROCESO_EMISION
                    // Para este caso, necesitamos un UUID temporal para seguimiento
                    String uuidTemporal = UUID.randomUUID().toString().replace("-", "").toUpperCase();
                    guardarFacturaEnProceso(request, pacResponse.getXmlTimbrado() != null ? pacResponse.getXmlTimbrado() : xml, uuidTemporal, subtotal, iva, total);
                    actualizarFacturaEnProcesoEmision(uuidTemporal);
                    return FacturaResponse.builder()
                            .exitoso(true)
                            .mensaje("Factura enviada a timbrado. Estado: EN_PROCESO_EMISION")
                            .timestamp(LocalDateTime.now())
                            .uuid(uuidTemporal)
                            .xmlTimbrado("En proceso de timbrado")
                            .datosFactura(construirDatosFacturaEnProceso(request, subtotal, iva, total, uuidTemporal))
                            .build();
                } else {
                    // Timbrado rechazado
                    return FacturaResponse.builder()
                            .exitoso(false)
                            .mensaje("Timbrado rechazado por PAC")
                            .timestamp(LocalDateTime.now())
                            .errores(pacResponse.getMessage())
                            .build();
                }
            } else {
                // Error en comunicación con PAC
                return FacturaResponse.builder()
                        .exitoso(false)
                        .mensaje("Error en comunicación con PAC")
                        .timestamp(LocalDateTime.now())
                        .errores(pacResponse != null ? pacResponse.getMessage() : "PAC no disponible")
                        .build();
            }

        } catch (Exception e) {
            return FacturaResponse.builder()
                    .exitoso(false)
                    .mensaje("Error al procesar timbrado")
                    .timestamp(LocalDateTime.now())
                    .errores("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Actualiza factura con datos de timbrado exitoso (llamado por callback)
     * Usa REQUIRES_NEW para asegurar que se ejecute en una transacción separada y se confirme inmediatamente
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void actualizarFacturaTimbrada(String uuid, PacTimbradoResponse pacResponse) {
        logger.info("=== ACTUALIZANDO FACTURA TIMBRADA ===");
        logger.info("UUID temporal: {}", uuid);
        
        // Obtener el UUID oficial que devuelve Finkok
        String uuidFinkok = pacResponse.getUuid();
        if (uuidFinkok != null && !uuidFinkok.isBlank()) {
            // Normalizar formato del UUID de Finkok: asegurar que tenga guiones en formato estándar (8-4-4-4-12)
            uuidFinkok = normalizarUUIDConGuiones(uuidFinkok);
            logger.info("UUID de Finkok (normalizado con guiones): {}", uuidFinkok);
        } else {
            logger.warn("⚠️ Finkok no devolvió UUID, usando UUID temporal");
            uuidFinkok = uuid;
        }
        
        logger.info("Perfil activo: {}", environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "oracle");
        
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0]
                : "oracle";

        if ("mongo".equals(activeProfile)) {
            // Buscar por UUID temporal (el que se usó para guardar inicialmente)
            FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
            if (facturaMongo != null) {
                logger.info("Factura encontrada en MongoDB, actualizando a EMITIDA (0)");
                // Actualizar el UUID con el oficial de Finkok
                facturaMongo.setUuid(uuidFinkok);
                facturaMongo.setEstado(EstadoFactura.EMITIDA.getCodigo());
                facturaMongo.setEstadoDescripcion(EstadoFactura.EMITIDA.getDescripcion());
                facturaMongo.setFechaTimbrado(pacResponse.getFechaTimbrado());
                facturaMongo.setXmlContent(pacResponse.getXmlTimbrado());
                facturaMongo.setCadenaOriginal(pacResponse.getCadenaOriginal());
                facturaMongo.setSelloDigital(pacResponse.getSelloDigital());
                facturaMongo.setCertificado(pacResponse.getCertificado());
                facturaMongo.setSerie(pacResponse.getSerie());
                facturaMongo.setFolio(pacResponse.getFolio());
                facturaMongoRepository.save(facturaMongo);
                logger.info("✓ Factura actualizada en MongoDB - UUID: {}, Estado: {}, Descripción: {}", 
                        uuidFinkok, EstadoFactura.EMITIDA.getCodigo(), EstadoFactura.EMITIDA.getDescripcion());
            } else {
                logger.error("✗ Factura no encontrada en MongoDB con UUID temporal: {}", uuid);
            }
        } else {
            // Buscar por UUID temporal (el que se usó para guardar inicialmente)
            Factura factura = facturaRepository.findByUuid(uuid).orElse(null);
            if (factura == null) {
                // Intentar buscar con variaciones del UUID
                factura = facturaRepository.findByUuid(uuid.toUpperCase()).orElse(null);
                if (factura == null) {
                    factura = facturaRepository.findByUuid(uuid.toLowerCase()).orElse(null);
                }
            }
            
            if (factura != null) {
                logger.info("Factura encontrada en Oracle, actualizando a EMITIDA (0)");
                logger.info("Estado anterior: {} - {}, EstatusFactura anterior: {}", 
                        factura.getEstado(), factura.getEstadoDescripcion(), factura.getEstatusFactura());
                // Actualizar el UUID con el oficial de Finkok
                factura.setUuid(uuidFinkok);
                factura.setEstado(EstadoFactura.EMITIDA.getCodigo());
                factura.setEstadoDescripcion(EstadoFactura.EMITIDA.getDescripcion());
                factura.setEstatusFactura(Integer.valueOf(EstadoFactura.EMITIDA.getCodigo()));
                factura.setStatusSat(EstadoFactura.EMITIDA.getDescripcion());
                factura.setFechaTimbrado(pacResponse.getFechaTimbrado());
                factura.setXmlContent(pacResponse.getXmlTimbrado());
                factura.setCadenaOriginal(pacResponse.getCadenaOriginal());
                factura.setSelloDigital(pacResponse.getSelloDigital());
                factura.setCertificado(pacResponse.getCertificado());
                factura.setSerie(pacResponse.getSerie());
                factura.setFolio(pacResponse.getFolio());
                facturaRepository.save(factura);
                
                logger.info("✓✓✓ Factura actualizada en Oracle - UUID: {}, Estado: {}, EstatusFactura: {}, StatusSat: {}", 
                        uuidFinkok,
                        EstadoFactura.EMITIDA.getCodigo(), 
                        EstadoFactura.EMITIDA.getCodigo(), 
                        EstadoFactura.EMITIDA.getDescripcion());
                
                // Verificar inmediatamente después de guardar usando el UUID de Finkok
                Factura facturaVerificada = facturaRepository.findByUuid(uuidFinkok).orElse(null);
                if (facturaVerificada == null) {
                    facturaVerificada = facturaRepository.findByUuid(uuidFinkok.toUpperCase()).orElse(null);
                }
                if (facturaVerificada == null) {
                    facturaVerificada = facturaRepository.findByUuid(uuidFinkok.toLowerCase()).orElse(null);
                }
                if (facturaVerificada != null) {
                    logger.info("✓ VERIFICACIÓN POST-ACTUALIZACIÓN - UUID: {}, Estado: {}, EstatusFactura: {}, StatusSat: {}", 
                            uuidFinkok, facturaVerificada.getEstado(), facturaVerificada.getEstatusFactura(), facturaVerificada.getStatusSat());
                    if (!EstadoFactura.EMITIDA.getCodigo().equals(facturaVerificada.getEstado())) {
                        logger.error("✗✗✗ ERROR: La factura NO se actualizó correctamente. Estado esperado: {}, Estado actual: {}", 
                                EstadoFactura.EMITIDA.getCodigo(), facturaVerificada.getEstado());
                    }
                } else {
                    logger.error("✗✗✗ ERROR: No se pudo verificar la actualización - Factura no encontrada después de guardar con UUID: {}", uuidFinkok);
                }
            } else {
                logger.error("✗✗✗ Factura no encontrada en Oracle con UUID: {} (ni con variaciones)", uuid);
                // Intentar buscar por UUID sin guiones o en mayúsculas
                // Intentar buscar con UUID en mayúsculas o minúsculas
                logger.warn("Intentando buscar factura con UUID alternativo (case-insensitive)...");
                Factura facturaAlt = facturaRepository.findByUuid(uuid.toUpperCase()).orElse(null);
                if (facturaAlt == null) {
                    facturaAlt = facturaRepository.findByUuid(uuid.toLowerCase()).orElse(null);
                }
                if (facturaAlt != null) {
                    logger.info("Factura encontrada con UUID alternativo (case-insensitive), actualizando...");
                    facturaAlt.setEstado(EstadoFactura.EMITIDA.getCodigo());
                    facturaAlt.setEstadoDescripcion(EstadoFactura.EMITIDA.getDescripcion());
                    facturaAlt.setEstatusFactura(Integer.valueOf(EstadoFactura.EMITIDA.getCodigo()));
                    facturaAlt.setStatusSat(EstadoFactura.EMITIDA.getDescripcion());
                    facturaAlt.setFechaTimbrado(pacResponse.getFechaTimbrado());
                    facturaAlt.setXmlContent(pacResponse.getXmlTimbrado());
                    facturaAlt.setCadenaOriginal(pacResponse.getCadenaOriginal());
                    facturaAlt.setSelloDigital(pacResponse.getSelloDigital());
                    facturaAlt.setCertificado(pacResponse.getCertificado());
                    facturaAlt.setSerie(pacResponse.getSerie());
                    facturaAlt.setFolio(pacResponse.getFolio());
                    facturaRepository.save(facturaAlt);
                    logger.info("✓ Factura actualizada con UUID alternativo");
                } else {
                    logger.error("✗ No se encontró factura con UUID: {} (ni con variaciones)", uuid);
                }
            }
        }
        logger.info("=== FIN ACTUALIZACIÓN FACTURA TIMBRADA ===");
    }

    /**
     * Construye la solicitud para el PAC
     */
    private PacTimbradoRequest construirPacTimbradoRequest(FacturaRequest request, String xml, BigDecimal total) {
        return PacTimbradoRequest.builder()
                .uuid(null) // No se envía UUID, Finkok lo genera al timbrar
                .xmlContent(xml)
                .rfcEmisor(request.getRfcEmisor())
                .rfcReceptor(request.getRfcReceptor())
                .total(total.doubleValue())
                .tipo("INGRESO")
                .fechaFactura(LocalDateTime.now().toString())
                .publicoGeneral(false)
                .serie("A")
                .folio("1")
                .tienda("TIENDA-001") // Valor por defecto ya que no existe en FacturaRequest
                .terminal("TERMINAL-001")
                .boleta("BOLETA-001")
                .medioPago(request.getMetodoPago())
                .formaPago(request.getFormaPago())
                .usoCFDI(request.getUsoCFDI())
                .regimenFiscalEmisor(request.getRegimenFiscalEmisor())
                .regimenFiscalReceptor(request.getRegimenFiscalReceptor())
                .build();
    }

    /**
     * Guarda la factura directamente con UUID de Finkok y estado EMITIDA
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private void guardarFacturaTimbrada(FacturaRequest request, PacTimbradoResponse pacResponse,
            BigDecimal subtotal, BigDecimal iva, BigDecimal total, String uuidFinkok) {
        logger.info("=== GUARDANDO FACTURA TIMBRADA ===");
        logger.info("UUID de Finkok: {}", uuidFinkok);
        logger.info("Perfil activo: {}", environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "oracle");
        
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0]
                : "oracle";

        if ("mongo".equals(activeProfile)) {
            FacturaMongo facturaMongo = FacturaMongo.builder()
                    .uuid(uuidFinkok)
                    .xmlContent(pacResponse.getXmlTimbrado())
                    .fechaGeneracion(LocalDateTime.now())
                    .fechaTimbrado(pacResponse.getFechaTimbrado())
                    .subtotal(subtotal)
                    .iva(iva)
                    .total(total)
                    .estado(EstadoFactura.EMITIDA.getCodigo())
                    .estadoDescripcion(EstadoFactura.EMITIDA.getDescripcion())
                    .serie(pacResponse.getSerie())
                    .folio(pacResponse.getFolio())
                    .cadenaOriginal(pacResponse.getCadenaOriginal())
                    .selloDigital(pacResponse.getSelloDigital())
                    .certificado(pacResponse.getCertificado())
                    .tienda("TIENDA-001") // Valor por defecto
                    .medioPago(request.getMetodoPago())
                    .formaPago(request.getFormaPago())
                    .build();
            facturaMongoRepository.save(facturaMongo);
            logger.info("✓ Factura guardada en MongoDB - UUID: {}, Estado: EMITIDA (0)", uuidFinkok);
        } else {
            Factura factura = Factura.builder()
                    .uuid(uuidFinkok)
                    .xmlContent(pacResponse.getXmlTimbrado())
                    .fechaGeneracion(LocalDateTime.now())
                    .fechaTimbrado(pacResponse.getFechaTimbrado())
                    .emisorRfc(request.getRfcEmisor())
                    .emisorRazonSocial(request.getNombreEmisor())
                    .receptorRfc(request.getRfcReceptor())
                    .receptorRazonSocial(request.getNombreReceptor())
                    .subtotal(subtotal)
                    .iva(iva)
                    .iepsDesglosado(false)
                    .total(total)
                    .estado(EstadoFactura.EMITIDA.getCodigo())
                    .estadoDescripcion(EstadoFactura.EMITIDA.getDescripcion())
                    .estatusFactura(Integer.valueOf(EstadoFactura.EMITIDA.getCodigo()))
                    .statusSat(EstadoFactura.EMITIDA.getDescripcion())
                    .serie(pacResponse.getSerie())
                    .folio(pacResponse.getFolio())
                    .cadenaOriginal(pacResponse.getCadenaOriginal())
                    .selloDigital(pacResponse.getSelloDigital())
                    .certificado(pacResponse.getCertificado())
                    .tienda("TIENDA-001") // Valor por defecto
                    .medioPago(request.getMetodoPago())
                    .formaPago(request.getFormaPago())
                    .build();
            facturaRepository.save(factura);
            facturaRepository.flush();
            logger.info("✓✓✓ Factura guardada en Oracle - UUID: {}, Estado: EMITIDA (0)", uuidFinkok);
        }
    }

    /**
     * Guarda la factura en estado POR_TIMBRAR (solo para casos asíncronos)
     */
    @Transactional
    private void guardarFacturaEnProceso(FacturaRequest request, String xml, String uuid,
            BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0]
                : "oracle";

        if ("mongo".equals(activeProfile)) {
            FacturaMongo facturaMongo = FacturaMongo.builder()
                    .uuid(uuid)
                    .xmlContent(xml)
                    .fechaGeneracion(LocalDateTime.now())
                    .subtotal(subtotal)
                    .iva(iva)
                    .total(total)
                    .estado(EstadoFactura.POR_TIMBRAR.getCodigo())
                    .estadoDescripcion(EstadoFactura.POR_TIMBRAR.getDescripcion())
                    .serie("A")
                    .folio("1")
                    .tienda("TIENDA-001") // Valor por defecto
                    .medioPago(request.getMetodoPago())
                    .formaPago(request.getFormaPago())
                    .build();
            facturaMongoRepository.save(facturaMongo);
        } else {
            Factura factura = Factura.builder()
                    .uuid(uuid)
                    .xmlContent(xml)
                    .fechaGeneracion(LocalDateTime.now())
                    .emisorRfc(request.getRfcEmisor())
                    .emisorRazonSocial(request.getNombreEmisor())
                    .receptorRfc(request.getRfcReceptor())
                    .receptorRazonSocial(request.getNombreReceptor())
                    .subtotal(subtotal)
                    .iva(iva)
                    .iepsDesglosado(false)
                    .total(total)
                    .estado(EstadoFactura.POR_TIMBRAR.getCodigo())
                    .estadoDescripcion(EstadoFactura.POR_TIMBRAR.getDescripcion())
                    .serie("A")
                    .folio("1")
                    .tienda("TIENDA-001") // Valor por defecto
                    .medioPago(request.getMetodoPago())
                    .formaPago(request.getFormaPago())
                    .build();
            facturaRepository.save(factura);
        }
    }

    /**
     * Actualiza factura a estado EN_PROCESO_EMISION
     */
    @Transactional
    private void actualizarFacturaEnProcesoEmision(String uuid) {
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0]
                : "oracle";

        if ("mongo".equals(activeProfile)) {
            FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
            if (facturaMongo != null) {
                facturaMongo.setEstado(EstadoFactura.EN_PROCESO_EMISION.getCodigo());
                facturaMongo.setEstadoDescripcion(EstadoFactura.EN_PROCESO_EMISION.getDescripcion());
                facturaMongoRepository.save(facturaMongo);
            }
        } else {
            Factura factura = facturaRepository.findByUuid(uuid).orElse(null);
            if (factura != null) {
                factura.setEstado(EstadoFactura.EN_PROCESO_EMISION.getCodigo());
                factura.setEstadoDescripcion(EstadoFactura.EN_PROCESO_EMISION.getDescripcion());
                factura.setEstatusFactura(Integer.valueOf(EstadoFactura.EN_PROCESO_EMISION.getCodigo()));
                factura.setStatusSat(EstadoFactura.EN_PROCESO_EMISION.getDescripcion());
                facturaRepository.save(factura);
            }
        }
    }

    /**
     * Actualiza factura como rechazada
     */
    @Transactional
    public void actualizarFacturaRechazada(String uuid, PacTimbradoResponse pacResponse) {
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0]
                : "oracle";

        if ("mongo".equals(activeProfile)) {
            FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
            if (facturaMongo != null) {
                facturaMongo.setEstado(EstadoFactura.CANCELADA_SAT.getCodigo());
                facturaMongo.setEstadoDescripcion(EstadoFactura.CANCELADA_SAT.getDescripcion());
                facturaMongoRepository.save(facturaMongo);
            }
        } else {
            Factura factura = facturaRepository.findByUuid(uuid).orElse(null);
            if (factura != null) {
                factura.setEstado(EstadoFactura.CANCELADA_SAT.getCodigo());
                factura.setEstadoDescripcion(EstadoFactura.CANCELADA_SAT.getDescripcion());
                factura.setEstatusFactura(Integer.valueOf(EstadoFactura.CANCELADA_SAT.getCodigo()));
                factura.setStatusSat(EstadoFactura.CANCELADA_SAT.getDescripcion());
                facturaRepository.save(factura);
            }
        }
    }

    /**
     * Actualiza factura con error
     */
    @Transactional
    private void actualizarFacturaError(String uuid, PacTimbradoResponse pacResponse) {
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0]
                : "oracle";

        if ("mongo".equals(activeProfile)) {
            FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
            if (facturaMongo != null) {
                facturaMongo.setEstado(EstadoFactura.FACTURA_TEMPORAL.getCodigo());
                facturaMongo.setEstadoDescripcion(EstadoFactura.FACTURA_TEMPORAL.getDescripcion());
                facturaMongoRepository.save(facturaMongo);
            }
        } else {
            Factura factura = facturaRepository.findByUuid(uuid).orElse(null);
            if (factura != null) {
                factura.setEstado(EstadoFactura.FACTURA_TEMPORAL.getCodigo());
                factura.setEstadoDescripcion(EstadoFactura.FACTURA_TEMPORAL.getDescripcion());
                factura.setEstatusFactura(Integer.valueOf(EstadoFactura.FACTURA_TEMPORAL.getCodigo()));
                factura.setStatusSat(EstadoFactura.FACTURA_TEMPORAL.getDescripcion());
                facturaRepository.save(factura);
            }
        }
    }

    /**
     * Construye respuesta exitosa para timbrado inmediato
     */
    private FacturaResponse construirRespuestaExitosa(FacturaRequest request, BigDecimal subtotal,
            BigDecimal iva, BigDecimal total, PacTimbradoResponse pacResponse, String uuidTemporal) {
        return FacturaResponse.builder()
                .exitoso(true)
                .mensaje("Factura timbrada exitosamente")
                .timestamp(LocalDateTime.now())
                .uuid(uuidTemporal)
                .xmlTimbrado(pacResponse.getXmlTimbrado())
                .datosFactura(FacturaResponse.DatosFactura.builder()
                        .folioFiscal(pacResponse.getFolioFiscal())
                        .serie(pacResponse.getSerie())
                        .folio(pacResponse.getFolio())
                        .fechaTimbrado(pacResponse.getFechaTimbrado())
                        .subtotal(subtotal)
                        .iva(iva)
                        .total(total)
                        .cadenaOriginal(pacResponse.getCadenaOriginal())
                        .selloDigital(pacResponse.getSelloDigital())
                        .certificado(pacResponse.getCertificado())
                        .build())
                .build();
    }

    /**
     * Construye datos de factura en proceso
     */
    private FacturaResponse.DatosFactura construirDatosFacturaEnProceso(FacturaRequest request,
            BigDecimal subtotal, BigDecimal iva,
            BigDecimal total, String uuid) {
        return FacturaResponse.DatosFactura.builder()
                .folioFiscal(uuid)
                .serie("A")
                .folio("1")
                .fechaTimbrado(null) // Aún no timbrada
                .subtotal(subtotal)
                .iva(iva)
                .total(total)
                .cadenaOriginal("En proceso de timbrado")
                .selloDigital("En proceso de timbrado")
                .certificado("En proceso de timbrado")
                .build();
    }

    /**
     * Genera XML de la factura (método copiado de FacturaService)
     */
    private String generarXMLFactura(FacturaRequest request, BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<cfdi:Comprobante ");
        xml.append("Version=\"4.0\" ");
        xml.append("Serie=\"").append("A").append("\" ");
        xml.append("Folio=\"").append("1").append("\" ");
        xml.append("Fecha=\"").append(LocalDateTime.now()).append("\" ");
        xml.append("SubTotal=\"").append(subtotal).append("\" ");
        xml.append("Moneda=\"MXN\" ");
        xml.append("Total=\"").append(total).append("\" ");
        xml.append("TipoDeComprobante=\"I\" ");
        xml.append("FormaPago=\"").append(request.getFormaPago()).append("\" ");
        xml.append("MetodoPago=\"").append(request.getMetodoPago()).append("\" ");
        xml.append("LugarExpedicion=\"").append(request.getCodigoPostalEmisor()).append("\" ");
        xml.append("xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\">\n");

        // Emisor
        xml.append("  <cfdi:Emisor ");
        xml.append("Rfc=\"").append(request.getRfcEmisor()).append("\" ");
        xml.append("Nombre=\"").append(request.getNombreEmisor()).append("\" ");
        xml.append("RegimenFiscal=\"").append(request.getRegimenFiscalEmisor()).append("\"/>\n");

        // Receptor
        // CRÍTICO: Validar y corregir UsoCFDI según tipo de persona y régimen fiscal
        String rfcReceptor = request.getRfcReceptor();
        String usoCfdiOriginal = request.getUsoCFDI();
        String usoCfdiFinal = validarYCorregirUsoCFDI(usoCfdiOriginal, rfcReceptor, request.getRegimenFiscalReceptor());
        if (!usoCfdiOriginal.equals(usoCfdiFinal)) {
            logger.warn("⚠️ UsoCFDI corregido de '{}' a '{}' para RFC {} (régimen: {})", 
                    usoCfdiOriginal, usoCfdiFinal, rfcReceptor, request.getRegimenFiscalReceptor());
        }
        xml.append("  <cfdi:Receptor ");
        xml.append("Rfc=\"").append(rfcReceptor).append("\" ");
        xml.append("Nombre=\"").append(request.getNombreReceptor()).append("\" ");
        xml.append("DomicilioFiscalReceptor=\"").append(request.getCodigoPostalReceptor()).append("\" ");
        xml.append("RegimenFiscalReceptor=\"").append(request.getRegimenFiscalReceptor()).append("\" ");
        xml.append("UsoCFDI=\"").append(usoCfdiFinal).append("\"/>\n");

        // Conceptos
        xml.append("  <cfdi:Conceptos>\n");
        for (FacturaRequest.Concepto concepto : request.getConceptos()) {
            xml.append("    <cfdi:Concepto ");
            xml.append("ClaveProdServ=\"01010101\" ");
            xml.append("NoIdentificacion=\"\" ");
            xml.append("Cantidad=\"").append(concepto.getCantidad()).append("\" ");
            xml.append("ClaveUnidad=\"H87\" ");
            xml.append("Unidad=\"").append(concepto.getUnidad()).append("\" ");
            xml.append("Descripcion=\"").append(concepto.getDescripcion()).append("\" ");
            xml.append("ValorUnitario=\"").append(concepto.getPrecioUnitario()).append("\" ");
            xml.append("Importe=\"").append(concepto.getImporte()).append("\" ");
            xml.append("Descuento=\"0.00\">\n");

            // Impuestos del concepto
            BigDecimal ivaConcepto = concepto.getImporte().multiply(new BigDecimal("0.16"));
            xml.append("      <cfdi:Impuestos>\n");
            xml.append("        <cfdi:Traslados>\n");
            xml.append("          <cfdi:Traslado ");
            xml.append("Base=\"").append(concepto.getImporte()).append("\" ");
            xml.append("Impuesto=\"002\" ");
            xml.append("TipoFactor=\"Tasa\" ");
            xml.append("TasaOCuota=\"0.160000\" ");
            xml.append("Importe=\"").append(ivaConcepto).append("\"/>\n");
            xml.append("        </cfdi:Traslados>\n");
            xml.append("      </cfdi:Impuestos>\n");
            xml.append("    </cfdi:Concepto>\n");
        }
        xml.append("  </cfdi:Conceptos>\n");

        // Impuestos
        xml.append("  <cfdi:Impuestos ");
        xml.append("TotalImpuestosTrasladados=\"").append(iva).append("\">\n");
        xml.append("    <cfdi:Traslados>\n");
        xml.append("      <cfdi:Traslado ");
        xml.append("Impuesto=\"002\" ");
        xml.append("TipoFactor=\"Tasa\" ");
        xml.append("TasaOCuota=\"0.160000\" ");
        xml.append("Importe=\"").append(iva).append("\"/>\n");
        xml.append("    </cfdi:Traslados>\n");
        xml.append("  </cfdi:Impuestos>\n");

        xml.append("</cfdi:Comprobante>");

        return xml.toString();
    }

    /**
     * Normaliza un UUID asegurando que tenga el formato estándar con guiones (8-4-4-4-12)
     * Si el UUID viene sin guiones, los agrega. Si ya los tiene, solo normaliza a mayúsculas.
     * 
     * @param uuid UUID que puede venir con o sin guiones
     * @return UUID normalizado con guiones en formato estándar: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
     */
    private String normalizarUUIDConGuiones(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return uuid;
        }
        
        // Normalizar a mayúsculas y remover espacios
        String uuidLimpio = uuid.trim().toUpperCase().replace(" ", "");
        
        // Si ya tiene guiones, solo verificar formato y normalizar
        if (uuidLimpio.contains("-")) {
            // Remover guiones existentes para normalizar
            String sinGuiones = uuidLimpio.replace("-", "");
            if (sinGuiones.length() != 32) {
                logger.warn("⚠️ UUID con longitud inválida: {} (esperado: 32 caracteres). Se mantendrá tal cual.", uuidLimpio);
                return uuidLimpio; // Retornar original si no tiene formato válido
            }
            // Re-formatear con guiones en posiciones correctas
            return sinGuiones.substring(0, 8) + "-" +
                   sinGuiones.substring(8, 12) + "-" +
                   sinGuiones.substring(12, 16) + "-" +
                   sinGuiones.substring(16, 20) + "-" +
                   sinGuiones.substring(20, 32);
        } else {
            // No tiene guiones, agregarlos
            if (uuidLimpio.length() != 32) {
                logger.warn("⚠️ UUID con longitud inválida: {} (esperado: 32 caracteres). Se mantendrá tal cual.", uuidLimpio);
                return uuidLimpio; // Retornar original si no tiene formato válido
            }
            // Formatear con guiones en formato estándar (8-4-4-4-12)
            return uuidLimpio.substring(0, 8) + "-" +
                   uuidLimpio.substring(8, 12) + "-" +
                   uuidLimpio.substring(12, 16) + "-" +
                   uuidLimpio.substring(16, 20) + "-" +
                   uuidLimpio.substring(20, 32);
        }
    }

    /**
     * Valida y corrige el UsoCFDI según el tipo de persona (física o moral) y régimen fiscal
     * CRÍTICO: El UsoCFDI debe corresponder con el tipo de persona y régimen conforme al catálogo c_UsoCFDI
     * 
     * @param usoCfdi UsoCFDI proporcionado
     * @param rfc RFC del receptor (para determinar tipo de persona)
     * @param regimenFiscal Régimen fiscal del receptor
     * @return UsoCFDI válido corregido si es necesario
     */
    private String validarYCorregirUsoCFDI(String usoCfdi, String rfc, String regimenFiscal) {
        if (usoCfdi == null || usoCfdi.trim().isEmpty()) {
            logger.warn("⚠️ UsoCFDI vacío, usando valor por defecto según tipo de persona");
            // Determinar tipo de persona por longitud del RFC
            boolean esPersonaFisica = rfc != null && rfc.length() == 13;
            return esPersonaFisica ? "D01" : "G01"; // D01 para física, G01 para moral
        }

        String usoCfdiUpper = usoCfdi.trim().toUpperCase();
        
        // Determinar tipo de persona por longitud del RFC
        boolean esPersonaFisica = rfc != null && rfc.length() == 13;
        boolean esPersonaMoral = rfc != null && rfc.length() == 12;
        
        // Regímenes fiscales de persona física (principalmente)
        String[] regimenesPersonaFisica = {"605", "606", "607", "608", "610", "611", "612", "614", "615", "616", "621", "625", "626"};
        boolean esRegimenPersonaFisica = false;
        if (regimenFiscal != null) {
            for (String regimen : regimenesPersonaFisica) {
                if (regimen.equals(regimenFiscal)) {
                    esRegimenPersonaFisica = true;
                    break;
                }
            }
        }

        // Validar UsoCFDI según tipo de persona
        if (esPersonaFisica || esRegimenPersonaFisica) {
            // Persona Física: UsoCFDI válidos son principalmente D01-D10 y algunos G específicos (NO G01)
            if (usoCfdiUpper.startsWith("D") || 
                usoCfdiUpper.equals("G02") || usoCfdiUpper.equals("G03") || 
                usoCfdiUpper.equals("CP01") || usoCfdiUpper.equals("CN01")) {
                logger.debug("✓ UsoCFDI válido para persona física: {}", usoCfdiUpper);
                return usoCfdiUpper;
            } else if (usoCfdiUpper.equals("G01")) {
                // G01 NO es válido para persona física
                logger.warn("⚠️ UsoCFDI G01 no es válido para persona física. Corrigiendo a D01 (Gastos en general).");
                logger.warn("⚠️ Para persona física con régimen {}, los UsoCFDI válidos son: D01-D10, G02, G03, CP01, CN01", regimenFiscal);
                return "D01"; // Valor por defecto seguro para persona física
            } else {
                logger.warn("⚠️ UsoCFDI '{}' puede no ser válido para persona física. Verificando...", usoCfdiUpper);
                // Permitir otros códigos pero advertir
                return usoCfdiUpper;
            }
        } else if (esPersonaMoral) {
            // Persona Moral: UsoCFDI válidos son principalmente G01, G02, G03, etc.
            if (usoCfdiUpper.startsWith("G") || 
                usoCfdiUpper.equals("CP01") || usoCfdiUpper.equals("CN01")) {
                logger.debug("✓ UsoCFDI válido para persona moral: {}", usoCfdiUpper);
                return usoCfdiUpper;
            } else if (usoCfdiUpper.startsWith("D")) {
                // D01-D10 son principalmente para persona física
                logger.warn("⚠️ UsoCFDI '{}' (deducciones) generalmente es para persona física. Para persona moral se recomienda G01, G02, G03.", usoCfdiUpper);
                // Permitir pero advertir
                return usoCfdiUpper;
            } else {
                logger.warn("⚠️ UsoCFDI '{}' puede no ser válido para persona moral. Verificando...", usoCfdiUpper);
                return usoCfdiUpper;
            }
        } else {
            // Tipo de persona no determinado, usar el valor proporcionado pero advertir
            logger.warn("⚠️ No se pudo determinar el tipo de persona del RFC: {}. Usando UsoCFDI proporcionado: {}", rfc, usoCfdiUpper);
            return usoCfdiUpper;
        }
    }
}
