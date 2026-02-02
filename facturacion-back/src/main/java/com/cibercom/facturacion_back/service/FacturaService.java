package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.FacturaRequest;
import com.cibercom.facturacion_back.dto.FacturaResponse;
import com.cibercom.facturacion_back.dto.FacturaFrontendRequest;
import com.cibercom.facturacion_back.dto.TicketDto;
import com.cibercom.facturacion_back.dto.TicketSearchRequest;
import com.cibercom.facturacion_back.dto.SatValidationRequest;
import com.cibercom.facturacion_back.dto.SatValidationResponse;
import com.cibercom.facturacion_back.model.Factura;
import com.cibercom.facturacion_back.model.FacturaMongo;
import com.cibercom.facturacion_back.model.EstadoFactura;
import com.cibercom.facturacion_back.repository.FacturaRepository;
import com.cibercom.facturacion_back.repository.FacturaMongoRepository;
import com.cibercom.facturacion_back.model.ClienteCatalogo;
import com.cibercom.facturacion_back.dao.ConceptoOracleDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cibercom.facturacion_back.integration.PacClient;
import com.cibercom.facturacion_back.dto.PacTimbradoRequest;
import com.cibercom.facturacion_back.dto.PacTimbradoResponse;

@Service
public class FacturaService {

    private static final Logger logger = LoggerFactory.getLogger(FacturaService.class);

    @Autowired
    private SatValidationService satValidationService;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private FacturaMongoRepository facturaMongoRepository;
    
    @Autowired
    private ITextPdfService iTextPdfService;

    @Autowired
    private ClienteCatalogoService clienteCatalogoService;

    @Autowired(required = false)
    private TicketService ticketService;

        // Valores por defecto del EMISOR (configurables vía propiedades)
    @Value("${facturacion.emisor.rfc:EEM123456789}")
    private String rfcEmisorDefault;

    @Value("${facturacion.emisor.nombre:Empresa Ejemplo S.A. de C.V.}")
    private String nombreEmisorDefault;

    @Value("${facturacion.emisor.regimen:601}")
    private String regimenFiscalEmisorDefault;

    @Value("${facturacion.emisor.cp:58000}")
    private String codigoPostalEmisorDefault;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        logger.info("=== FACTURASERVICE INICIALIZADO ===");
        logger.info("RFC Emisor por defecto: {}", rfcEmisorDefault);
        logger.info("Nombre Emisor por defecto: {}", nombreEmisorDefault);
        logger.info("Regimen Fiscal Emisor: {}", regimenFiscalEmisorDefault);
        logger.info("Código Postal Emisor: {}", codigoPostalEmisorDefault);
        logger.info("====================================");
    }

    @Autowired(required = false)
    private ConceptoOracleDAO conceptoOracleDAO;

    @Autowired(required = false)
    private TicketDetalleService ticketDetalleService;

    @Autowired(required = false)
    private com.cibercom.facturacion_back.dao.NominaOracleDAO nominaOracleDAO;

    @Autowired(required = false)
    private com.cibercom.facturacion_back.dao.PagoOracleDAO pagoOracleDAO;
    
    @Autowired(required = false)
    private com.cibercom.facturacion_back.dao.RetencionOracleDAO retencionOracleDAO;
    
    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private CartaPorteService cartaPorteService;

    @Autowired(required = false)
    private PacClient pacClient;

    @Autowired
    private FacturaTimbradoService facturaTimbradoService;
    
    @Autowired
    private Environment environment;

    /**
     * Procesa una factura generando solo el XML (sin timbrar)
     */
    public FacturaResponse procesarFactura(FacturaRequest request, Long usuarioId) {
        try {
            // 1. Validar datos del emisor
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

            // 2. Validar datos del receptor
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

            // 3. Calcular totales usando tasas de IVA de cada concepto
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal iva = BigDecimal.ZERO;
            for (FacturaRequest.Concepto concepto : request.getConceptos()) {
                subtotal = subtotal.add(concepto.getImporte());
                // Calcular IVA por concepto usando su tasa específica
                String objetoImp = (concepto.getObjetoImp() != null && !concepto.getObjetoImp().trim().isEmpty()) 
                        ? concepto.getObjetoImp() : "02";
                if ("02".equals(objetoImp)) {
                    BigDecimal tasaIva = (concepto.getTasaIva() != null && concepto.getTasaIva().compareTo(BigDecimal.ZERO) > 0) 
                            ? concepto.getTasaIva() : new BigDecimal("0.16");
                    BigDecimal ivaConcepto = concepto.getImporte().multiply(tasaIva);
                    iva = iva.add(ivaConcepto);
                }
            }
            BigDecimal total = subtotal.add(iva);

            // 4. Generar UUID temporal para el XML
            String uuid = UUID.randomUUID().toString();

            // 5. Generar XML según lineamientos del SAT
            String xml = generarXMLFactura(request, subtotal, iva, total, uuid);

            // 6. Guardar en base de datos
            guardarFacturaEnBD(request, xml, uuid, subtotal, iva, total, usuarioId);

            // 7. Construir respuesta con XML generado
            return FacturaResponse.builder()
                    .exitoso(true)
                    .mensaje("XML de factura generado y guardado exitosamente")
                    .timestamp(LocalDateTime.now())
                    .uuid(uuid)
                    .xmlTimbrado(xml)
                    .datosFactura(construirDatosFactura(request, subtotal, iva, total, uuid, generarFolioConsecutivo("A")))
                    .build();

        } catch (Exception e) {
            return FacturaResponse.builder()
                    .exitoso(false)
                    .mensaje("Error al procesar factura: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Genera y timbra una factura según los lineamientos del SAT
     * AHORA TAMBIÉN GUARDA EN BASE DE DATOS
     */
    public FacturaResponse generarYTimbrarFactura(FacturaRequest request, Long usuarioId) {
        try {
            // 1. Validar datos del emisor
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

            // 2. Validar datos del receptor
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

            // 3. Calcular totales usando tasas de IVA de cada concepto
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal iva = BigDecimal.ZERO;
            for (FacturaRequest.Concepto concepto : request.getConceptos()) {
                subtotal = subtotal.add(concepto.getImporte());
                // Calcular IVA por concepto usando su tasa específica
                String objetoImp = (concepto.getObjetoImp() != null && !concepto.getObjetoImp().trim().isEmpty()) 
                        ? concepto.getObjetoImp() : "02";
                if ("02".equals(objetoImp)) {
                    BigDecimal tasaIva = (concepto.getTasaIva() != null && concepto.getTasaIva().compareTo(BigDecimal.ZERO) > 0) 
                            ? concepto.getTasaIva() : new BigDecimal("0.16");
                    BigDecimal ivaConcepto = concepto.getImporte().multiply(tasaIva);
                    iva = iva.add(ivaConcepto);
                }
            }
            BigDecimal total = subtotal.add(iva);

            // 4. Simular timbrado (en ambiente real se conectaría con PAC)
            String uuid = simularTimbrado();

            // 5. Generar XML según lineamientos del SAT
            String xml = generarXMLFactura(request, subtotal, iva, total, uuid);

            // 6. GUARDAR EN BASE DE DATOS (NUEVO)
            guardarFacturaEnBD(request, xml, uuid, subtotal, iva, total, usuarioId);

            // 7. Construir respuesta
            return FacturaResponse.builder()
                    .exitoso(true)
                    .mensaje("Factura generada, timbrada y GUARDADA en BD exitosamente")
                    .timestamp(LocalDateTime.now())
                    .uuid(uuid)
                    .xmlTimbrado(xml)
                    .datosFactura(construirDatosFactura(request, subtotal, iva, total, uuid, generarFolioConsecutivo("A")))
                    .build();

        } catch (Exception e) {
            return FacturaResponse.builder()
                    .exitoso(false)
                    .mensaje("Error al generar la factura")
                    .timestamp(LocalDateTime.now())
                    .errores("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Procesa el formulario del frontend y genera factura
     */
    @Transactional
    public Map<String, Object> procesarFormularioFrontend(FacturaFrontendRequest request, Long usuarioId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 0. Validar campos según tipo de persona (física o moral)
            String rfc = request.getRfc() != null ? request.getRfc().trim().toUpperCase() : "";
            boolean esPersonaMoral = rfc.length() == 12;
            boolean esPersonaFisica = rfc.length() == 13;
            
            if (esPersonaMoral) {
                // Persona Moral: razonSocial es obligatorio
                if (request.getRazonSocial() == null || request.getRazonSocial().trim().isEmpty()) {
                    response.put("exitoso", false);
                    response.put("mensaje", "Los datos proporcionados no son válidos");
                    response.put("errores", Map.of("razonSocial", "La razón social es obligatoria para personas morales"));
                    return response;
                }
            } else if (esPersonaFisica) {
                // Persona Física: nombre y paterno son obligatorios
                if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
                    response.put("exitoso", false);
                    response.put("mensaje", "Los datos proporcionados no son válidos");
                    response.put("errores", Map.of("nombre", "El nombre es obligatorio para personas físicas"));
                    return response;
                }
                if (request.getPaterno() == null || request.getPaterno().trim().isEmpty()) {
                    response.put("exitoso", false);
                    response.put("mensaje", "Los datos proporcionados no son válidos");
                    response.put("errores", Map.of("paterno", "El apellido paterno es obligatorio para personas físicas"));
                    return response;
                }
            }
            
            // Determinar el nombre completo para validación SAT
            String nombreCompleto = null;
            if (esPersonaMoral) {
                nombreCompleto = request.getRazonSocial();
            } else if (esPersonaFisica) {
                // Construir nombre completo: nombre + paterno + materno (si existe)
                StringBuilder nombreBuilder = new StringBuilder(request.getNombre().trim());
                if (request.getPaterno() != null && !request.getPaterno().trim().isEmpty()) {
                    nombreBuilder.append(" ").append(request.getPaterno().trim());
                }
                if (request.getMaterno() != null && !request.getMaterno().trim().isEmpty()) {
                    nombreBuilder.append(" ").append(request.getMaterno().trim());
                }
                nombreCompleto = nombreBuilder.toString();
            }
            
            // 1. Validar datos del receptor con SAT
            SatValidationRequest emisorRequest = new SatValidationRequest();
            emisorRequest.setNombre(nombreCompleto != null ? nombreCompleto : request.getRazonSocial());
            emisorRequest.setRfc(request.getRfc());
            emisorRequest.setCodigoPostal(extraerCodigoPostal(request.getDomicilioFiscal()));
            emisorRequest.setRegimenFiscal(request.getRegimenFiscal());

            SatValidationResponse validacionEmisor = satValidationService.validarDatosSat(emisorRequest);
            if (!validacionEmisor.isValido()) {
                response.put("exitoso", false);
                response.put("mensaje", "Datos del emisor inválidos");
                response.put("errores", validacionEmisor.getErrores());
                return response;
            }

            // 2. Calcular totales desde conceptos si están disponibles, sino buscar ticket
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal iva = BigDecimal.ZERO;
            BigDecimal total = BigDecimal.ZERO;
            String formaPagoOverride = null;

            // Si hay conceptos del catálogo (modo manual), calcular totales desde ellos
            if (request.getConceptos() != null && !request.getConceptos().isEmpty()) {
                logger.info("Calculando totales desde {} conceptos del catálogo", request.getConceptos().size());
                for (FacturaFrontendRequest.Concepto concepto : request.getConceptos()) {
                    BigDecimal importeConcepto = concepto.getImporte() != null ? concepto.getImporte() : 
                        (concepto.getPrecioUnitario() != null && concepto.getCantidad() != null ? 
                         concepto.getPrecioUnitario().multiply(concepto.getCantidad()) : BigDecimal.ZERO);
                    subtotal = subtotal.add(importeConcepto);
                    
                    // Calcular IVA solo si el objeto de impuesto es "02"
                    String objetoImp = (concepto.getObjetoImp() != null && !concepto.getObjetoImp().trim().isEmpty()) 
                        ? concepto.getObjetoImp() : "02";
                    if ("02".equals(objetoImp)) {
                        BigDecimal tasaIva = concepto.getTasaIva() != null ? concepto.getTasaIva() : new BigDecimal("0.16");
                        BigDecimal ivaConcepto = importeConcepto.multiply(tasaIva);
                        iva = iva.add(ivaConcepto);
                    }
                }
                total = subtotal.add(iva);
                logger.info("Totales calculados desde conceptos - Subtotal: {}, IVA: {}, Total: {}", subtotal, iva, total);
            } else {
                // Modo automático: buscar ticket y usar sus totales
                subtotal = new BigDecimal("1000.00"); // Valor por defecto si no hay ticket
                iva = subtotal.multiply(new BigDecimal("0.16"));
                total = subtotal.add(iva);

                try {
                    if (ticketService != null) {
                        TicketSearchRequest tsr = new TicketSearchRequest();
                        tsr.setCodigoTienda(request.getTienda());
                        tsr.setTerminalId(toInt(request.getTerminal()));
                        tsr.setFecha(request.getFecha() != null ? request.getFecha().toString() : null);
                        tsr.setFolio(toInt(request.getBoleta()));

                        List<TicketDto> tickets = ticketService.buscarTickets(tsr);
                        if (tickets != null && !tickets.isEmpty()) {
                            TicketDto t = tickets.get(0);
                            if (t.getSubtotal() != null) subtotal = t.getSubtotal();
                            if (t.getIva() != null) {
                                iva = t.getIva();
                            } else {
                                iva = subtotal.multiply(new BigDecimal("0.16"));
                            }
                            if (t.getTotal() != null) {
                                total = t.getTotal();
                            } else {
                                total = subtotal.add(iva);
                            }
                            formaPagoOverride = t.getFormaPago();
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Fallo al buscar ticket para totales: {}", e.getMessage());
                }
            }

            // 3. Generar XML de la factura con totales y CP dinámicos
            // CRÍTICO: El código postal del receptor debe ser el registrado en el SAT para ese RFC
            // Extraer del domicilioFiscal, pero validar que no sea "00000"
            String cpReceptor = extraerCodigoPostal(request.getDomicilioFiscal());
            if (cpReceptor == null || cpReceptor.trim().isEmpty() || "00000".equals(cpReceptor)) {
                logger.warn("⚠️ ADVERTENCIA: No se pudo extraer código postal válido del domicilioFiscal del receptor.");
                logger.warn("⚠️ El código postal debe ser el registrado en el SAT para el RFC: {}", request.getRfc());
                // Para RFC CUSC850516316, el código postal registrado en el SAT es 45638
                if ("CUSC850516316".equalsIgnoreCase(request.getRfc())) {
                    cpReceptor = "45638";
                    logger.info("✓ Usando código postal registrado en el SAT para RFC CUSC850516316: {}", cpReceptor);
                } else {
                    logger.error("❌ ERROR: No se puede determinar el código postal del receptor. Esto causará error CFDI40147.");
                    logger.error("❌ SOLUCIÓN: Asegúrate de que el domicilioFiscal contenga el código postal correcto.");
                    logger.error("❌ O consulta el código postal registrado en el SAT para el RFC: {}", request.getRfc());
                }
            } else {
                logger.info("✓ Código postal del receptor extraído: {}", cpReceptor);
            }
            String xml = generarXMLDesdeFrontend(
                    request,
                    subtotal,
                    iva,
                    total,
                    codigoPostalEmisorDefault, // Lugar de expedición del EMISOR
                    cpReceptor,                // Domicilio fiscal del RECEPTOR
                    formaPagoOverride);

            // 4. Enviar a timbrado del PAC (SIN guardar antes - Finkok genera el UUID)
            String uuidFinkok = null; // Se asignará cuando Finkok responda exitosamente
            PacTimbradoResponse pacResp = null; // Se asignará cuando Finkok responda
            String xmlTimbrado = xml; // Por defecto usar XML original, se actualizará con el de Finkok si está disponible
            String folioFinal = generarFolioConsecutivo("A"); // Por defecto, se actualizará con el de Finkok si está disponible
            try {
                if (pacClient != null) {
                    // Validar y normalizar MetodoPago antes de enviar a Finkok
                    String metodoPagoValidado = validarYNormalizarMetodoPago(request.getMedioPago());
                    
                    PacTimbradoRequest pacReq = PacTimbradoRequest.builder()
                            .uuid(null) // No enviar UUID, Finkok lo genera al timbrar
                            .xmlContent(xml)
                            .rfcEmisor(rfcEmisorDefault)
                            .rfcReceptor(request.getRfc())
                            .total(total != null ? total.doubleValue() : 0.0)
                            .tipo("INGRESO")
                            .fechaFactura(request.getFecha() != null ? request.getFecha().toString() : java.time.OffsetDateTime.now().toString())
                            .publicoGeneral(Boolean.FALSE)
                            .serie("A")
                            .folio("1") // Folio temporal, se actualizará después con el de Finkok
                            .tienda(request.getTienda())
                            .terminal(request.getTerminal())
                            .boleta(request.getBoleta())
                            .medioPago(metodoPagoValidado)
                            .formaPago(formaPagoOverride != null && !formaPagoOverride.isBlank() ? formaPagoOverride : request.getFormaPago())
                            .usoCFDI(request.getUsoCfdi())
                            .regimenFiscalEmisor(regimenFiscalEmisorDefault)
                            .regimenFiscalReceptor(request.getRegimenFiscal())
                            .relacionadosUuids(null)
                            .build();

                    pacResp = pacClient.solicitarTimbrado(pacReq);
                    if (pacResp != null) {
                        // Usar HashMap en lugar de Map.of() porque algunos valores pueden ser null
                        Map<String, Object> timbradoResp = new HashMap<>();
                        timbradoResp.put("ok", pacResp.getOk());
                        timbradoResp.put("status", pacResp.getStatus());
                        timbradoResp.put("receiptId", pacResp.getReceiptId());
                        timbradoResp.put("message", pacResp.getMessage());
                        response.put("timbrado", timbradoResp);

                        // Si el PAC confirma timbrado inmediato (EMITIDA), actualizar la factura en BD de inmediato
                        try {
                            String status = pacResp.getStatus();
                            logger.info("═══════════════════════════════════════════════════════════════");
                            logger.info("RESPUESTA DE FINKOK:");
                            logger.info("  UUID de Finkok: {}", pacResp.getUuid());
                            logger.info("  ok: {}", pacResp.getOk());
                            logger.info("  status: {}", status);
                            logger.info("  CodEstatus: {}", pacResp.getCodEstatus());
                            logger.info("═══════════════════════════════════════════════════════════════");
                            
                            if (Boolean.TRUE.equals(pacResp.getOk()) && 
                                ("TIMBRADO".equals(status) || "TIMBRADO_PREVIAMENTE".equals(status) || "0".equals(status))) {
                                
                                // Obtener UUID de Finkok
                                uuidFinkok = pacResp.getUuid();
                                if (uuidFinkok == null || uuidFinkok.isBlank()) {
                                    logger.error("✗✗✗ ERROR: Finkok no devolvió UUID en la respuesta");
                                    response.put("exitoso", false);
                                    response.put("mensaje", "Error: Finkok no devolvió UUID en la respuesta");
                                    return response;
                                }
                                // Normalizar formato del UUID de Finkok: asegurar que tenga guiones en formato estándar (8-4-4-4-12)
                                uuidFinkok = normalizarUUIDConGuiones(uuidFinkok);
                                logger.info("✓✓✓ Finkok confirmó timbrado exitoso - Guardando factura con UUID: {} y estado EMITIDA (0)", uuidFinkok);
                                
                                // Guardar directamente con UUID de Finkok y estado EMITIDA
                                try {
                                    // Actualizar XML y folio con los de Finkok
                                    xmlTimbrado = pacResp.getXmlTimbrado() != null ? pacResp.getXmlTimbrado() : xml;
                                    folioFinal = pacResp.getFolio() != null ? pacResp.getFolio() : generarFolioConsecutivo("A");
                                    
                                    // Guardar en Oracle con UUID de Finkok y estado EMITIDA
                                    Factura facturaOracle = guardarEnOracleTimbrada(request, xmlTimbrado, uuidFinkok, subtotal, iva, total, pacResp, usuarioId);
                                    
                                    // Guardar en MongoDB solo si el perfil activo es "mongo"
                                    String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "oracle";
                                    if ("mongo".equals(activeProfile) && facturaMongoRepository != null) {
                                        try {
                                            guardarEnMongoTimbrada(request, xmlTimbrado, uuidFinkok, subtotal, iva, total, pacResp);
                                        } catch (Exception e) {
                                            logger.warn("No se pudo guardar en MongoDB (puede no estar disponible): {}", e.getMessage());
                                        }
                                    }
                                    
                                    // Enlazar ticket con factura en Oracle (si es posible)
                                    try {
                                        if (conceptoOracleDAO != null && ticketService != null) {
                                            java.util.Optional<Long> idFacturaOpt = conceptoOracleDAO.obtenerIdFacturaPorUuid(uuidFinkok);
                                            Long idFactura = idFacturaOpt.orElse(null);
                                            if (idFactura != null) {
                                                TicketSearchRequest tsrLink = new TicketSearchRequest();
                                                tsrLink.setCodigoTienda(request.getTienda());
                                                tsrLink.setTerminalId(toInt(request.getTerminal()));
                                                tsrLink.setFecha(request.getFecha() != null ? request.getFecha().toString() : null);
                                                tsrLink.setFolio(toInt(request.getBoleta()));
                                                boolean ok = ticketService.enlazarTicketConFactura(tsrLink, idFactura);
                                                logger.info("Ticket enlace con ID_FACTURA {} resultado: {}", idFactura, ok);
                                            } else {
                                                logger.warn("No se pudo resolver ID_FACTURA para UUID {} al intentar enlazar ticket", uuidFinkok);
                                            }
                                        } else {
                                            logger.warn("conceptoOracleDAO o ticketService no disponibles; se omite enlace de ticket");
                                        }
                                    } catch (Exception e) {
                                        logger.warn("Fallo al enlazar ticket con factura: {}", e.getMessage());
                                    }
                                    
                                    logger.info("✓✓✓ Factura guardada con UUID de Finkok: {} y estado EMITIDA", uuidFinkok);
                                    
                                    // Verificar que se guardó correctamente (reutilizar activeProfile ya declarado arriba)
                                    if ("mongo".equals(activeProfile)) {
                                        com.cibercom.facturacion_back.model.FacturaMongo facturaVerificada = facturaMongoRepository.findByUuid(uuidFinkok);
                                        if (facturaVerificada != null) {
                                            logger.info("✓ VERIFICACIÓN POST-GUARDADO (MongoDB) - UUID: {}, Estado: {}", uuidFinkok, facturaVerificada.getEstado());
                                        }
                                    } else {
                                        Factura facturaVerificada = facturaRepository.findByUuid(uuidFinkok).orElse(null);
                                        if (facturaVerificada != null) {
                                            logger.info("✓ VERIFICACIÓN POST-GUARDADO (Oracle) - UUID: {}, Estado: {}, EstatusFactura: {}", 
                                                    uuidFinkok, facturaVerificada.getEstado(), facturaVerificada.getEstatusFactura());
                                        } else {
                                            logger.error("✗✗✗ ERROR: No se pudo verificar el guardado - Factura no encontrada con UUID: {}", uuidFinkok);
                                        }
                                    }
                                } catch (Exception guardarEx) {
                                    logger.error("✗✗✗ ERROR CRÍTICO al guardar la factura: {}", guardarEx.getMessage(), guardarEx);
                                    response.put("exitoso", false);
                                    response.put("mensaje", "Error al guardar factura: " + guardarEx.getMessage());
                                    return response;
                                }
                                logger.info("═══════════════════════════════════════════════════════════════");
                            } else {
                                logger.warn("⚠️ Finkok no confirmó timbrado exitoso - ok: {}, status: {}", pacResp.getOk(), status);
                            }
                        } catch (Exception updEx) {
                            logger.error("✗✗✗ ERROR CRÍTICO al guardar la factura tras respuesta de Finkok: {}", updEx.getMessage(), updEx);
                            updEx.printStackTrace();
                        }
                    } else {
                        Map<String, Object> timbradoError = new HashMap<>();
                        timbradoError.put("ok", false);
                        timbradoError.put("status", "ERROR");
                        timbradoError.put("message", "PAC no respondió");
                        response.put("timbrado", timbradoError);
                    }
                } else {
                    logger.warn("PacClient no disponible; se omite envío al PAC");
                    Map<String, Object> timbradoOmitido = new HashMap<>();
                    timbradoOmitido.put("ok", false);
                    timbradoOmitido.put("status", "OMITIDO");
                    timbradoOmitido.put("message", "PacClient no disponible");
                    response.put("timbrado", timbradoOmitido);
                }
            } catch (Exception e) {
                logger.warn("Fallo al enviar timbrado al PAC: {}", e.getMessage());
                // Usar HashMap en lugar de Map.of() porque e.getMessage() puede ser null
                Map<String, Object> timbradoError = new HashMap<>();
                timbradoError.put("ok", false);
                timbradoError.put("status", "ERROR");
                timbradoError.put("message", e.getMessage() != null ? e.getMessage() : "Error desconocido al timbrar");
                response.put("timbrado", timbradoError);
            }

            // 9. Construir respuesta exitosa (solo si Finkok respondió exitosamente)
            if (uuidFinkok != null && !uuidFinkok.isBlank()) {
                response.put("exitoso", true);
                response.put("mensaje", "Factura procesada, timbrada y guardada exitosamente");
                response.put("uuid", uuidFinkok);
                response.put("xmlGenerado", xmlTimbrado);
                response.put("datosFactura", construirDatosFacturaFrontend(request, subtotal, iva, total, uuidFinkok, folioFinal));
            } else {
                // Si no se guardó exitosamente, la respuesta ya fue establecida en los bloques de error anteriores
                if (!response.containsKey("exitoso")) {
                    response.put("exitoso", false);
                    response.put("mensaje", "Error: No se pudo timbrar la factura");
                }
            }

        } catch (Exception e) {
            logger.error("Error al procesar formulario frontend: {}", e.getMessage(), e);
            response.put("exitoso", false);
            response.put("mensaje", "Error al procesar la factura: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            response.put("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            if (e.getCause() != null) {
                response.put("causa", e.getCause().getMessage());
            }
        }

        return response;
    }

    /**
     * Genera el XML de la factura según los lineamientos del SAT
     */
    private String generarXMLFactura(FacturaRequest request, BigDecimal subtotal, BigDecimal iva, BigDecimal total,
            String uuid) {
        StringBuilder xml = new StringBuilder();
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\" ");
        xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        xml.append(
                "xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/4 http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd\" ");
        xml.append("Version=\"4.0\" ");
        xml.append("Fecha=\"").append(fechaActual).append("\" ");
        xml.append("Folio=\"1\" ");
        xml.append("Serie=\"A\" ");
        // NO agregar atributos vacíos (Sello, NoCertificado, Certificado, CondicionesDePago)
        // Finkok rechaza estos atributos si están vacíos, causando error 705
        xml.append("FormaPago=\"").append(request.getFormaPago()).append("\" ");
        xml.append("SubTotal=\"").append(subtotal).append("\" ");
        xml.append("Moneda=\"MXN\" ");
        // NO agregar TipoCambio="1" cuando Moneda="MXN" - Según Anexo 20, si Moneda es MXN, TipoCambio no debe incluirse
        xml.append("Total=\"").append(total).append("\" ");
        xml.append("TipoDeComprobante=\"I\" ");
        // Exportacion="01" es para operaciones nacionales (según catálogo SAT c_TipoExportacion)
        xml.append("Exportacion=\"01\" ");
        // Validar y normalizar MetodoPago (debe ser PUE o PPD según catálogo c_MetodoPago)
        String metodoPago = validarYNormalizarMetodoPago(request.getMetodoPago());
        xml.append("MetodoPago=\"").append(metodoPago).append("\" ");
        xml.append("LugarExpedicion=\"").append(request.getCodigoPostalEmisor()).append("\">\n");

        // Agregar comentario con información adicional
        xml.append("<!-- UUID: ").append(uuid).append(" -->");
        xml.append("<!-- Timestamp: ").append(fechaActual).append(" -->");
        xml.append("<!-- Estado: EN_PROCESO_EMISION -->");
        xml.append("\n");

        // Emisor
        logger.info("Generando XML - RFC Emisor: {}, Nombre: {}", request.getRfcEmisor(), request.getNombreEmisor());
        xml.append("  <cfdi:Emisor ");
        xml.append("Rfc=\"").append(request.getRfcEmisor()).append("\" ");
        xml.append("Nombre=\"").append(request.getNombreEmisor()).append("\" ");
        xml.append("RegimenFiscal=\"").append(request.getRegimenFiscalEmisor()).append("\"/>\n");

        // Receptor - Según CFDI 3.3 Anexo 20, los atributos válidos son:
        // Rfc (requerido), Nombre (requerido), ResidenciaFiscal (opcional), 
        // NumRegIdTrib (opcional), UsoCFDI (requerido)
        // NO incluir DomicilioFiscalReceptor ni RegimenFiscalReceptor (no existen en CFDI 3.3)
        xml.append("  <cfdi:Receptor ");
        xml.append("Rfc=\"").append(request.getRfcReceptor()).append("\" ");
        xml.append("Nombre=\"").append(request.getNombreReceptor()).append("\" ");
        // ResidenciaFiscal solo si existe
        if (request.getCodigoPostalReceptor() != null && request.getCodigoPostalReceptor().startsWith("XEXX")) {
            xml.append("ResidenciaFiscal=\"MEX\" ");
        }
        xml.append("UsoCFDI=\"").append(request.getUsoCFDI()).append("\"/>\n");

        // Conceptos
        xml.append("  <cfdi:Conceptos>\n");
        for (FacturaRequest.Concepto concepto : request.getConceptos()) {
            // Usar valores del catálogo si están disponibles, sino usar valores por defecto seguros
            String claveProdServ = (concepto.getClaveProdServ() != null && !concepto.getClaveProdServ().trim().isEmpty()) 
                    ? concepto.getClaveProdServ() : "01010101";
            String claveUnidad = validarYNormalizarClaveUnidad(
                    concepto.getClaveUnidad(), 
                    concepto.getUnidad()
            );
            String objetoImp = (concepto.getObjetoImp() != null && !concepto.getObjetoImp().trim().isEmpty()) 
                    ? concepto.getObjetoImp() : "02";
            BigDecimal tasaIva = (concepto.getTasaIva() != null && concepto.getTasaIva().compareTo(BigDecimal.ZERO) > 0) 
                    ? concepto.getTasaIva() : new BigDecimal("0.16");
            
            xml.append("    <cfdi:Concepto ");
            xml.append("ClaveProdServ=\"").append(escapeXml(claveProdServ)).append("\" ");
            // NO agregar NoIdentificacion="" vacío - Finkok lo rechaza (error 705)
            xml.append("Cantidad=\"").append(concepto.getCantidad()).append("\" ");
            xml.append("ClaveUnidad=\"").append(escapeXml(claveUnidad)).append("\" ");
            xml.append("Unidad=\"").append(escapeXml(concepto.getUnidad())).append("\" ");
            xml.append("Descripcion=\"").append(escapeXml(concepto.getDescripcion())).append("\" ");
            xml.append("ValorUnitario=\"").append(concepto.getPrecioUnitario()).append("\" ");
            xml.append("Importe=\"").append(concepto.getImporte()).append("\" ");
            // NO agregar Descuento="0.00" - Si el descuento es cero, no debe incluirse el atributo
            xml.append("ObjetoImp=\"").append(objetoImp).append("\">\n");

            // Impuestos del concepto - SIEMPRE si ObjetoImp es "02" (Sí objeto de impuesto)
            // Según SAT, cuando ObjetoImp="02", el nodo Impuestos es OBLIGATORIO
            if ("02".equals(objetoImp)) {
                BigDecimal ivaConcepto = concepto.getImporte().multiply(tasaIva);
                xml.append("      <cfdi:Impuestos>\n");
                xml.append("        <cfdi:Traslados>\n");
                xml.append("          <cfdi:Traslado ");
                xml.append("Base=\"").append(concepto.getImporte()).append("\" ");
                xml.append("Impuesto=\"002\" ");
                xml.append("TipoFactor=\"Tasa\" ");
                xml.append("TasaOCuota=\"").append(String.format("%.6f", tasaIva)).append("\" ");
                xml.append("Importe=\"").append(ivaConcepto.setScale(2, java.math.RoundingMode.HALF_UP)).append("\"/>\n");
                xml.append("        </cfdi:Traslados>\n");
                xml.append("      </cfdi:Impuestos>\n");
            }
            xml.append("    </cfdi:Concepto>\n");
        }
        xml.append("  </cfdi:Conceptos>\n");

        // Calcular IVA total sumando los IVAs de cada concepto (solo si tasa > 0)
        BigDecimal ivaTotal = BigDecimal.ZERO;
        BigDecimal baseTotal = BigDecimal.ZERO;
        for (FacturaRequest.Concepto concepto : request.getConceptos()) {
            String objetoImpConcepto = (concepto.getObjetoImp() != null && !concepto.getObjetoImp().trim().isEmpty()) 
                    ? concepto.getObjetoImp() : "02";
            if ("02".equals(objetoImpConcepto)) {
                BigDecimal tasaIvaConcepto = (concepto.getTasaIva() != null && concepto.getTasaIva().compareTo(BigDecimal.ZERO) >= 0) 
                        ? concepto.getTasaIva() : new BigDecimal("0.16");
                // Sumar siempre si ObjetoImp="02" (incluso si tasa=0%)
                BigDecimal ivaConcepto = concepto.getImporte().multiply(tasaIvaConcepto);
                ivaTotal = ivaTotal.add(ivaConcepto);
                baseTotal = baseTotal.add(concepto.getImporte());
            }
        }

        // Impuestos - agregar siempre si hay conceptos con ObjetoImp="02"
        if (baseTotal.compareTo(BigDecimal.ZERO) > 0) {
            xml.append("  <cfdi:Impuestos ");
            xml.append("TotalImpuestosTrasladados=\"").append(ivaTotal.setScale(2, java.math.RoundingMode.HALF_UP)).append("\">\n");
            xml.append("    <cfdi:Traslados>\n");
            // Calcular tasa: si hay IVA, calcularla; si no (ivaTotal=0), usar 0.000000
            BigDecimal tasaPromedio = baseTotal.compareTo(BigDecimal.ZERO) > 0 && ivaTotal.compareTo(BigDecimal.ZERO) > 0
                    ? ivaTotal.divide(baseTotal, 6, java.math.RoundingMode.HALF_UP) 
                    : BigDecimal.ZERO;
            xml.append("      <cfdi:Traslado ");
            xml.append("Base=\"").append(baseTotal.setScale(2, java.math.RoundingMode.HALF_UP)).append("\" ");
            xml.append("Impuesto=\"002\" ");
            xml.append("TipoFactor=\"Tasa\" ");
            xml.append("TasaOCuota=\"").append(String.format("%.6f", tasaPromedio)).append("\" ");
            xml.append("Importe=\"").append(ivaTotal.setScale(2, java.math.RoundingMode.HALF_UP)).append("\"/>\n");
            xml.append("    </cfdi:Traslados>\n");
            xml.append("  </cfdi:Impuestos>\n");
        }

        // Complemento con TimbreFiscalDigital
        xml.append("  <cfdi:Complemento>\n");
        xml.append("    <tfd:TimbreFiscalDigital xmlns:tfd=\"http://www.sat.gob.mx/TimbreFiscalDigital\" ");
        xml.append(
                "xsi:schemaLocation=\"http://www.sat.gob.mx/TimbreFiscalDigital http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd\" ");
        xml.append("Version=\"1.1\" ");
        xml.append("UUID=\"").append(uuid).append("\" ");
        xml.append("FechaTimbrado=\"").append(fechaActual).append("\" ");
        xml.append("RfcProvCertif=\"SAT970701NN3\" ");
        xml.append("SelloCFD=\"En proceso de timbrado\" ");
        xml.append("NoCertificadoSAT=\"00001000000504465028\" ");
        xml.append("SelloSAT=\"En proceso de timbrado\"/>\n");
        xml.append("  </cfdi:Complemento>\n");

        xml.append("</cfdi:Comprobante>");

        return xml.toString();
    }

    /**
     * Simula el timbrado de la factura (en ambiente real se conectaría con PAC)
     */
    private String simularTimbrado() {
        // Simular latencia de red
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generar UUID único para simular folio fiscal
        return UUID.randomUUID().toString().toUpperCase();
    }

    /**
     * Construye los datos de la factura para la respuesta
     */
    private FacturaResponse.DatosFactura construirDatosFactura(FacturaRequest request,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total,
            String uuid,
            String folio) {
        return FacturaResponse.DatosFactura.builder()
                .folioFiscal(uuid)
                .serie("A")
                .folio(folio)
                .fechaTimbrado(LocalDateTime.now())
                .subtotal(subtotal)
                .iva(iva)
                .total(total)
                .cadenaOriginal("Simulada para ambiente de pruebas")
                .selloDigital("Simulado para ambiente de pruebas")
                .certificado("Simulado para ambiente de pruebas")
                .build();
    }

    /**
     * Extrae código postal del domicilio fiscal
     */
    private String extraerCodigoPostal(String domicilioFiscal) {
        if (domicilioFiscal == null || domicilioFiscal.trim().isEmpty()) {
            return "00000";
        }

        // Buscar 5 dígitos consecutivos (código postal)
        String[] palabras = domicilioFiscal.split("\\s+");
        for (String palabra : palabras) {
            if (palabra.matches("\\d{5}")) {
                return palabra;
            }
        }

        return "00000"; // Código postal por defecto
    }

    /**
     * Valida y normaliza el MetodoPago según el catálogo c_MetodoPago del SAT
     * CRÍTICO: Los únicos valores válidos son "PUE" (Pago en una sola exhibición) y "PPD" (Pago en parcialidades o diferido)
     * 
     * @param metodoPago MetodoPago proporcionado
     * @return MetodoPago válido ("PUE" o "PPD"), por defecto "PUE" si es inválido
     */
    private String validarYNormalizarMetodoPago(String metodoPago) {
        if (metodoPago == null || metodoPago.trim().isEmpty()) {
            logger.warn("⚠️ MetodoPago vacío, usando valor por defecto 'PUE'");
            return "PUE";
        }
        
        String metodoPagoUpper = metodoPago.trim().toUpperCase();
        
        // Valores válidos del catálogo c_MetodoPago
        if ("PUE".equals(metodoPagoUpper)) {
            return "PUE";
        } else if ("PPD".equals(metodoPagoUpper)) {
            return "PPD";
        } else {
            // Valor inválido - usar PUE por defecto
            logger.warn("⚠️ MetodoPago '{}' no es válido. Los valores válidos del catálogo c_MetodoPago son: PUE, PPD. Corrigiendo a 'PUE'.", metodoPago);
            return "PUE";
        }
    }
    
    /**
     * Valida y normaliza ClaveUnidad para que sea un código válido del catálogo SAT c_ClaveUnidad
     * Valores comunes: E48 (Servicio), H87 (Pieza), MTR (Metro), KGM (Kilogramo), LTR (Litro), etc.
     * Si el valor no es un código válido del catálogo, usa "E48" (Servicio) por defecto.
     */
    private String validarYNormalizarClaveUnidad(String claveUnidad, String unidad) {
        // Lista completa de códigos válidos del catálogo SAT c_ClaveUnidad
        Set<String> clavesUnidadValidas = new HashSet<>(Arrays.asList(
            "H87", "EA", "E48", "ACT", "KGM", "E51", "A9", "MTR", "AB", "BB", "KT", "SET", "LTR", 
            "XBX", "MON", "HUR", "MTK", "11", "MGM", "XPK", "XKI", "AS", "GRM", "PR", "DPC", 
            "XUN", "xun", "DAY", "XLT", "10", "MLT", "E54"
        ));
        
        // Si se proporciona una claveUnidad, validarla
        if (claveUnidad != null && !claveUnidad.trim().isEmpty()) {
            String claveUnidadTrim = claveUnidad.trim();
            String claveUnidadUpper = claveUnidadTrim.toUpperCase();
            
            // Verificar si es un código válido (en mayúsculas o como está)
            if (clavesUnidadValidas.contains(claveUnidadUpper) || clavesUnidadValidas.contains(claveUnidadTrim)) {
                // Retornar en mayúsculas, excepto "xun" que debe ser minúsculas
                if ("xun".equalsIgnoreCase(claveUnidadTrim)) {
                    return "xun";
                }
                return claveUnidadUpper;
            }
            
            // Verificar textos descriptivos comunes
            String[] textosDescriptivos = {"OPCIONAL", "SERVICIO", "PIEZA", "UNIDAD", "N/A", "NA", "-"};
            boolean esTextoDescriptivo = false;
            for (String texto : textosDescriptivos) {
                if (claveUnidadUpper.equals(texto)) {
                    esTextoDescriptivo = true;
                    break;
                }
            }
            if (esTextoDescriptivo) {
                logger.warn("⚠️ ClaveUnidad '{}' es un texto descriptivo. Usando 'E48' (Unidad de Servicio) por defecto.", claveUnidad);
                return "E48";
            }
            
            logger.warn("⚠️ ClaveUnidad '{}' no es un código válido del catálogo c_ClaveUnidad. Corrigiendo a 'E48'.", claveUnidad);
        }
        
        // Si no hay claveUnidad válida, usar "E48" (Unidad de Servicio) por defecto
        logger.info("⚠️ Usando ClaveUnidad por defecto 'E48' (Unidad de Servicio)");
        return "E48";
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

    /**
     * Genera XML desde el formulario del frontend
     */
    private String generarXMLDesdeFrontend(FacturaFrontendRequest request,
                                           BigDecimal subtotal,
                                           BigDecimal iva,
                                           BigDecimal total,
                                           String lugarExpedicionEmisorCp,
                                           String domicilioFiscalReceptorCp,
                                           String formaPagoOverride) {
        StringBuilder xml = new StringBuilder();
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        // Generar XML en formato CFDI 4.0 según los lineamientos del SAT
        // Serie y Folio son atributos requeridos según Anexo 20 del SAT para CFDI 4.0
        String folio = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String formaPago = formaPagoOverride != null && !formaPagoOverride.isBlank() ? formaPagoOverride : request.getFormaPago();
        // LugarExpedicion debe ser un código postal válido del catálogo SAT
        // Usar el CP del application.yml (58000) como valor por defecto, no "00000"
        String lugarExpedicion = lugarExpedicionEmisorCp != null && !lugarExpedicionEmisorCp.equals("00000") 
                ? lugarExpedicionEmisorCp : codigoPostalEmisorDefault;
        // Validar que no sea "00000" (inválido según SAT)
        if ("00000".equals(lugarExpedicion)) {
            lugarExpedicion = "58000"; // Usar el CP configurado por defecto
        }
        
        // Validar y normalizar MetodoPago (debe ser PUE o PPD según catálogo c_MetodoPago)
        String metodoPago = validarYNormalizarMetodoPago(request.getMedioPago());
        
        // CRÍTICO: Calcular totales desde conceptos ANTES de generar el XML
        // Esto asegura que SubTotal, TotalImpuestosTrasladados y Total sean consistentes
        BigDecimal subtotalCalculado = BigDecimal.ZERO;
        BigDecimal totalImpuestosCalculado = BigDecimal.ZERO;
        
        if (request.getConceptos() != null && !request.getConceptos().isEmpty()) {
            for (FacturaFrontendRequest.Concepto concepto : request.getConceptos()) {
                BigDecimal importeConcepto = concepto.getImporte() != null ? concepto.getImporte() : 
                    (concepto.getPrecioUnitario() != null && concepto.getCantidad() != null ? 
                     concepto.getPrecioUnitario().multiply(concepto.getCantidad()) : BigDecimal.ZERO);
                subtotalCalculado = subtotalCalculado.add(importeConcepto);
                
                String objetoImpConcepto = (concepto.getObjetoImp() != null && !concepto.getObjetoImp().trim().isEmpty()) 
                    ? concepto.getObjetoImp() : "02";
                if ("02".equals(objetoImpConcepto)) {
                    BigDecimal tasaIva = concepto.getTasaIva() != null ? concepto.getTasaIva() : new BigDecimal("0.16");
                    BigDecimal ivaConcepto = importeConcepto.multiply(tasaIva);
                    totalImpuestosCalculado = totalImpuestosCalculado.add(ivaConcepto);
                }
            }
            // Usar los totales calculados desde conceptos
            subtotal = subtotalCalculado;
            iva = totalImpuestosCalculado;
            total = subtotal.add(iva);
            logger.info("Totales recalculados desde conceptos - SubTotal: {}, IVA: {}, Total: {}", subtotal, iva, total);
        }
        
        // Generar XML en formato compacto (todos los atributos en una línea)
        // para evitar problemas con normalización en PacClient
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/4 http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd\" Version=\"4.0\" Serie=\"A\" Folio=\"").append(folio).append("\" Fecha=\"").append(fechaActual).append("\" FormaPago=\"").append(formaPago).append("\" SubTotal=\"").append(subtotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()).append("\" Moneda=\"MXN\" Total=\"").append(total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()).append("\" TipoDeComprobante=\"I\" Exportacion=\"01\" MetodoPago=\"").append(metodoPago).append("\" LugarExpedicion=\"").append(lugarExpedicion).append("\">\n");

        // CfdiRelacionados - Si se proporciona UUID de CFDI relacionado y tipo de relación
        if (request.getUuidCfdiRelacionado() != null && !request.getUuidCfdiRelacionado().trim().isEmpty() 
            && request.getTipoRelacion() != null && !request.getTipoRelacion().trim().isEmpty()) {
            String uuidRelacionado = request.getUuidCfdiRelacionado().trim().toUpperCase();
            String tipoRelacion = request.getTipoRelacion().trim();
            // Validar que el tipo de relación sea válido (01, 02, 03, 04)
            if (tipoRelacion.matches("0[1-4]")) {
                // Escapar caracteres especiales XML
                String tipoRelacionEscapado = escapeXml(tipoRelacion);
                String uuidRelacionadoEscapado = escapeXml(uuidRelacionado);
                xml.append("  <cfdi:CfdiRelacionados TipoRelacion=\"").append(tipoRelacionEscapado).append("\">\n");
                xml.append("    <cfdi:CfdiRelacionado UUID=\"").append(uuidRelacionadoEscapado).append("\"/>\n");
                xml.append("  </cfdi:CfdiRelacionados>\n");
                logger.info("✓ Nodo CfdiRelacionados agregado - Tipo: {}, UUID: {}", tipoRelacion, uuidRelacionado);
            } else {
                logger.warn("⚠️ Tipo de relación inválido: {}. Debe ser 01, 02, 03 o 04. Se omite el nodo CfdiRelacionados.", tipoRelacion);
            }
        }

        // Emisor - Formato compacto para evitar problemas con normalización
        xml.append("  <cfdi:Emisor Rfc=\"").append(rfcEmisorDefault).append("\" Nombre=\"").append(nombreEmisorDefault).append("\" RegimenFiscal=\"").append(regimenFiscalEmisorDefault).append("\"/>\n");

        // Receptor - Determinar nombre completo según tipo de persona
        String rfcReceptor = request.getRfc() != null ? request.getRfc().trim().toUpperCase() : "";
        boolean esPersonaMoralReceptor = rfcReceptor.length() == 12;
        boolean esPersonaFisicaReceptor = rfcReceptor.length() == 13;
        
        String nombreReceptorCompleto;
        if (esPersonaMoralReceptor) {
            // Persona Moral: usar razonSocial
            nombreReceptorCompleto = request.getRazonSocial() != null ? request.getRazonSocial().trim() : "";
        } else if (esPersonaFisicaReceptor) {
            // Persona Física: construir nombre completo (nombre + paterno + materno)
            StringBuilder nombreBuilder = new StringBuilder();
            if (request.getNombre() != null && !request.getNombre().trim().isEmpty()) {
                nombreBuilder.append(request.getNombre().trim());
            }
            if (request.getPaterno() != null && !request.getPaterno().trim().isEmpty()) {
                if (nombreBuilder.length() > 0) nombreBuilder.append(" ");
                nombreBuilder.append(request.getPaterno().trim());
            }
            if (request.getMaterno() != null && !request.getMaterno().trim().isEmpty()) {
                if (nombreBuilder.length() > 0) nombreBuilder.append(" ");
                nombreBuilder.append(request.getMaterno().trim());
            }
            nombreReceptorCompleto = nombreBuilder.toString().trim();
            // Si no se pudo construir, usar razonSocial como fallback
            if (nombreReceptorCompleto.isEmpty() && request.getRazonSocial() != null) {
                nombreReceptorCompleto = request.getRazonSocial().trim();
            }
        } else {
            // Caso por defecto: usar razonSocial o construir desde nombre
            if (request.getRazonSocial() != null && !request.getRazonSocial().trim().isEmpty()) {
                nombreReceptorCompleto = request.getRazonSocial().trim();
            } else {
                StringBuilder nombreBuilder = new StringBuilder();
                if (request.getNombre() != null) nombreBuilder.append(request.getNombre().trim());
                if (request.getPaterno() != null) {
                    if (nombreBuilder.length() > 0) nombreBuilder.append(" ");
                    nombreBuilder.append(request.getPaterno().trim());
                }
                if (request.getMaterno() != null) {
                    if (nombreBuilder.length() > 0) nombreBuilder.append(" ");
                    nombreBuilder.append(request.getMaterno().trim());
                }
                nombreReceptorCompleto = nombreBuilder.toString().trim();
                if (nombreReceptorCompleto.isEmpty()) nombreReceptorCompleto = rfcReceptor; // Fallback al RFC
            }
        }
        
        // Receptor - Según CFDI 4.0 Anexo 20, los atributos obligatorios son:
        // Rfc (requerido), Nombre (requerido), DomicilioFiscalReceptor (requerido - CP),
        // RegimenFiscalReceptor (requerido), UsoCFDI (requerido)
        // Opcionales: ResidenciaFiscal (solo extranjeros), NumRegIdTrib (solo extranjeros)
        // Formato compacto para evitar problemas con normalización
        StringBuilder receptorAttr = new StringBuilder();
        receptorAttr.append("Rfc=\"").append(request.getRfc()).append("\" ");
        receptorAttr.append("Nombre=\"").append(nombreReceptorCompleto).append("\" ");
        // DomicilioFiscalReceptor: Código postal del domicilio fiscal del receptor (OBLIGATORIO en CFDI 4.0)
        // CRÍTICO: El código postal debe ser el registrado en el SAT para ese RFC, no "00000"
        // Si no se proporciona, intentar extraerlo del domicilioFiscal del request
        String cpReceptorFinal = domicilioFiscalReceptorCp;
        if (cpReceptorFinal == null || cpReceptorFinal.trim().isEmpty() || "00000".equals(cpReceptorFinal)) {
            // Intentar extraer del domicilioFiscal del request
            if (request.getDomicilioFiscal() != null && !request.getDomicilioFiscal().trim().isEmpty()) {
                String cpExtraido = extraerCodigoPostal(request.getDomicilioFiscal());
                if (cpExtraido != null && !cpExtraido.trim().isEmpty() && !"00000".equals(cpExtraido.trim())) {
                    cpReceptorFinal = cpExtraido.trim();
                    logger.info("✓ Código postal extraído del domicilioFiscal del receptor: {}", cpReceptorFinal);
                }
            }
            
            // Si aún no hay código postal válido, registrar advertencia crítica
            if (cpReceptorFinal == null || cpReceptorFinal.trim().isEmpty() || "00000".equals(cpReceptorFinal)) {
                cpReceptorFinal = "00000";
                logger.error("❌ ERROR CRÍTICO: DomicilioFiscalReceptor es '00000' para RFC: {}. Esto causará error CFDI40147.", request.getRfc());
                logger.error("❌ SOLUCIÓN: El código postal del receptor debe ser el registrado en el SAT para ese RFC.");
                logger.error("❌ Para RFC CUSC850516316 el código postal registrado en el SAT es: 45638");
                logger.error("❌ Asegúrate de que el receptor tenga el código postal correcto en su registro.");
            }
        } else {
            logger.info("✓ Usando código postal del receptor proporcionado: {}", cpReceptorFinal);
        }
        receptorAttr.append("DomicilioFiscalReceptor=\"").append(cpReceptorFinal).append("\" ");
        // RegimenFiscalReceptor: Régimen fiscal del receptor (OBLIGATORIO en CFDI 4.0)
        receptorAttr.append("RegimenFiscalReceptor=\"").append(request.getRegimenFiscal()).append("\" ");
        // ResidenciaFiscal solo si es extranjero (RFC XEXX010101000)
        if (rfcReceptor.startsWith("XEXX")) {
            receptorAttr.append("ResidenciaFiscal=\"").append(request.getPais() != null ? request.getPais() : "MEX").append("\" ");
        }
        // NumRegIdTrib solo si existe noRegistroIdentidadTributaria (para extranjeros)
        if (request.getNoRegistroIdentidadTributaria() != null && !request.getNoRegistroIdentidadTributaria().trim().isEmpty()) {
            receptorAttr.append("NumRegIdTrib=\"").append(request.getNoRegistroIdentidadTributaria().trim()).append("\" ");
        }
        // UsoCFDI: Debe corresponder con el tipo de persona (física o moral) y el régimen fiscal
        // CRÍTICO: Validar que el UsoCFDI sea válido para el tipo de persona y régimen del receptor
        // El error CFDI40161 ocurre cuando el UsoCFDI no corresponde con el tipo de persona
        String usoCfdiOriginal = request.getUsoCfdi();
        String usoCfdiFinal = validarYCorregirUsoCFDI(usoCfdiOriginal, rfcReceptor, request.getRegimenFiscal());
        if (!usoCfdiOriginal.equals(usoCfdiFinal)) {
            logger.warn("⚠️ UsoCFDI corregido de '{}' a '{}' para RFC {} (tipo: {}, régimen: {})", 
                    usoCfdiOriginal, usoCfdiFinal, rfcReceptor, 
                    esPersonaFisicaReceptor ? "Física" : (esPersonaMoralReceptor ? "Moral" : "Desconocido"), 
                    request.getRegimenFiscal());
        }
        receptorAttr.append("UsoCFDI=\"").append(usoCfdiFinal).append("\"");
        xml.append("  <cfdi:Receptor ").append(receptorAttr.toString()).append("/>\n");

        // Conceptos - Usar conceptos del request si están disponibles, sino usar valores por defecto
        xml.append("  <cfdi:Conceptos>\n");
        
        // Si ya calculamos los totales desde conceptos arriba, usar ese IVA
        // Si no, calcular desde los conceptos aquí
        BigDecimal totalImpuestosTrasladados = BigDecimal.ZERO;
        if (request.getConceptos() != null && !request.getConceptos().isEmpty()) {
            // Ya se calculó arriba, usar el valor
            totalImpuestosTrasladados = iva;
        }
        
        if (request.getConceptos() != null && !request.getConceptos().isEmpty()) {
            // Usar conceptos del request (modo manual con catálogo)
            for (FacturaFrontendRequest.Concepto concepto : request.getConceptos()) {
                String claveProdServ = (concepto.getClaveProdServ() != null && !concepto.getClaveProdServ().trim().isEmpty()) 
                    ? concepto.getClaveProdServ() : "01010101";
                // ClaveUnidad DEBE ser un código del catálogo SAT c_ClaveUnidad (ej: E48, H87, MTR, etc.)
                // NO puede ser un texto descriptivo como "Opcional"
                String claveUnidad = validarYNormalizarClaveUnidad(concepto.getClaveUnidad(), concepto.getUnidad());
                String unidad = (concepto.getUnidad() != null && !concepto.getUnidad().trim().isEmpty()) 
                    ? concepto.getUnidad() : "Servicio";
                String descripcion = (concepto.getDescripcion() != null && !concepto.getDescripcion().trim().isEmpty()) 
                    ? concepto.getDescripcion() : "Servicio de Facturación";
                String objetoImp = (concepto.getObjetoImp() != null && !concepto.getObjetoImp().trim().isEmpty()) 
                    ? concepto.getObjetoImp() : "02";
                BigDecimal cantidad = concepto.getCantidad() != null ? concepto.getCantidad() : BigDecimal.ONE;
                BigDecimal precioUnitario = concepto.getPrecioUnitario() != null ? concepto.getPrecioUnitario() : concepto.getImporte();
                BigDecimal importe = concepto.getImporte() != null ? concepto.getImporte() : precioUnitario.multiply(cantidad);
                BigDecimal tasaIva = concepto.getTasaIva() != null ? concepto.getTasaIva() : new BigDecimal("0.16");
                
                xml.append("    <cfdi:Concepto ");
                xml.append("ClaveProdServ=\"").append(escapeXml(claveProdServ)).append("\" ");
                xml.append("Cantidad=\"").append(cantidad).append("\" ");
                xml.append("ClaveUnidad=\"").append(escapeXml(claveUnidad)).append("\" ");
                xml.append("Unidad=\"").append(escapeXml(unidad)).append("\" ");
                xml.append("Descripcion=\"").append(escapeXml(descripcion)).append("\" ");
                xml.append("ValorUnitario=\"").append(precioUnitario.setScale(2, java.math.RoundingMode.HALF_UP)).append("\" ");
                xml.append("Importe=\"").append(importe.setScale(2, java.math.RoundingMode.HALF_UP)).append("\" ");
                xml.append("ObjetoImp=\"").append(objetoImp).append("\">\n");
                
                if ("02".equals(objetoImp)) { 
                    // Cuando ObjetoImp="02", SIEMPRE se debe agregar el nodo Impuestos (obligatorio según SAT)
                    BigDecimal baseConcepto = importe;
                    BigDecimal ivaConcepto = baseConcepto.multiply(tasaIva);
                    totalImpuestosTrasladados = totalImpuestosTrasladados.add(ivaConcepto);
                    
                    xml.append("      <cfdi:Impuestos>\n");
                    xml.append("        <cfdi:Traslados>\n");
                    xml.append("          <cfdi:Traslado ");
                    xml.append("Base=\"").append(baseConcepto.setScale(2, java.math.RoundingMode.HALF_UP)).append("\" ");
                    xml.append("Impuesto=\"002\" ");
                    xml.append("TipoFactor=\"Tasa\" ");
                    xml.append("TasaOCuota=\"").append(String.format("%.6f", tasaIva)).append("\" ");
                    xml.append("Importe=\"").append(ivaConcepto.setScale(2, java.math.RoundingMode.HALF_UP)).append("\"/>\n");
                    xml.append("        </cfdi:Traslados>\n");
                    xml.append("      </cfdi:Impuestos>\n");
                }
                xml.append("    </cfdi:Concepto>\n");
            }
        } else {
            // Modo automático o sin conceptos: usar valores por defecto
            String valorUnitario = subtotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            String importe = subtotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            String base = subtotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            String importeIva = iva.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            totalImpuestosTrasladados = iva;
            
            String claveProdServDefault = "01010101";
            String claveUnidadDefault = "E48";
            String unidadDefault = "Servicio";
            String objetoImpDefault = "02";
            BigDecimal tasaIvaDefault = new BigDecimal("0.16");
            
            xml.append("    <cfdi:Concepto ClaveProdServ=\"").append(claveProdServDefault).append("\" Cantidad=\"1\" ClaveUnidad=\"").append(claveUnidadDefault).append("\" Unidad=\"").append(unidadDefault).append("\" Descripcion=\"Servicio de Facturación\" ValorUnitario=\"").append(valorUnitario).append("\" Importe=\"").append(importe).append("\" ObjetoImp=\"").append(objetoImpDefault).append("\">\n");
            if ("02".equals(objetoImpDefault)) {
                // Cuando ObjetoImp="02", SIEMPRE agregar Impuestos (obligatorio según SAT)
                xml.append("      <cfdi:Impuestos>\n");
                xml.append("        <cfdi:Traslados>\n");
                xml.append("          <cfdi:Traslado Base=\"").append(base).append("\" Impuesto=\"002\" TipoFactor=\"Tasa\" TasaOCuota=\"").append(String.format("%.6f", tasaIvaDefault)).append("\" Importe=\"").append(importeIva).append("\"/>\n");
                xml.append("        </cfdi:Traslados>\n");
                xml.append("      </cfdi:Impuestos>\n");
            }
            xml.append("    </cfdi:Concepto>\n");
        }
        
        xml.append("  </cfdi:Conceptos>\n");

        // Impuestos - Formato compacto
        // En CFDI 4.0, el Traslado dentro de Impuestos debe tener el atributo Base (obligatorio)
        // Calcular base total desde los conceptos
        BigDecimal baseTotal = BigDecimal.ZERO;
        if (request.getConceptos() != null && !request.getConceptos().isEmpty()) {
            for (FacturaFrontendRequest.Concepto concepto : request.getConceptos()) {
                String objetoImpConcepto = (concepto.getObjetoImp() != null && !concepto.getObjetoImp().trim().isEmpty()) 
                    ? concepto.getObjetoImp() : "02";
                if ("02".equals(objetoImpConcepto)) {
                    BigDecimal importeConcepto = concepto.getImporte() != null ? concepto.getImporte() : 
                        (concepto.getPrecioUnitario() != null && concepto.getCantidad() != null ? 
                         concepto.getPrecioUnitario().multiply(concepto.getCantidad()) : BigDecimal.ZERO);
                    baseTotal = baseTotal.add(importeConcepto);
                }
            }
        } else {
            baseTotal = subtotal;
        }
        
        xml.append("  <cfdi:Impuestos TotalImpuestosTrasladados=\"").append(totalImpuestosTrasladados.setScale(2, java.math.RoundingMode.HALF_UP)).append("\">\n");
        xml.append("    <cfdi:Traslados>\n");
        // Si hay conceptos con ObjetoImp="02", SIEMPRE agregar el Traslado (incluso si Importe=0.00)
        // Esto es necesario para que coincida con los Traslados en los conceptos
        if (baseTotal.compareTo(BigDecimal.ZERO) > 0) {
            // Calcular tasa: si hay IVA, calcularla; si no (totalImpuestosTrasladados=0), usar 0.000000
            BigDecimal tasaOCuota = baseTotal.compareTo(BigDecimal.ZERO) > 0 && totalImpuestosTrasladados.compareTo(BigDecimal.ZERO) > 0 ? 
                totalImpuestosTrasladados.divide(baseTotal, 6, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
            xml.append("      <cfdi:Traslado Base=\"").append(baseTotal.setScale(2, java.math.RoundingMode.HALF_UP)).append("\" Impuesto=\"002\" TipoFactor=\"Tasa\" TasaOCuota=\"").append(String.format("%.6f", tasaOCuota)).append("\" Importe=\"").append(totalImpuestosTrasladados.setScale(2, java.math.RoundingMode.HALF_UP)).append("\"/>\n");
        }
        xml.append("    </cfdi:Traslados>\n");
        xml.append("  </cfdi:Impuestos>\n");

        // CRÍTICO: Recalcular el total para asegurar que coincida con SubTotal + TotalImpuestosTrasladados
        // El total debe ser: SubTotal + TotalImpuestosTrasladados (sin descuentos ni retenciones)
        BigDecimal totalRecalculado = subtotal.add(totalImpuestosTrasladados);
        logger.info("Totales en XML - SubTotal: {}, TotalImpuestosTrasladados: {}, Total (recalculado): {}, Total (parámetro): {}", 
            subtotal, totalImpuestosTrasladados, totalRecalculado, total);
        
        // Usar el total recalculado en lugar del parámetro para asegurar consistencia
        total = totalRecalculado;

        xml.append("</cfdi:Comprobante>");

        return xml.toString();
    }

    /**
     * Guarda la factura en Oracle
     */
    private Factura guardarEnOracle(FacturaFrontendRequest request, String xml, String uuid,
            BigDecimal subtotal, BigDecimal iva, BigDecimal total, Long usuarioId) {

        Long idReceptor = resolverIdReceptorPorRfc(request);
        if (idReceptor == null) {
            throw new IllegalStateException("No se pudo resolver ID_RECEPTOR para RFC: " + request.getRfc());
        }

        Factura factura = Factura.builder()
                .uuid(uuid)
                .xmlContent(xml)
                .fechaGeneracion(LocalDateTime.now())
                .fechaTimbrado(null) // Se establecerá cuando Finkok timbre
                .estado(EstadoFactura.POR_TIMBRAR.getCodigo())
                .serie("A")
                .folio(generarFolioConsecutivo("A"))
                .cadenaOriginal("Simulada para ambiente de pruebas")
                .selloDigital("Simulado para ambiente de pruebas")
                .certificado("Simulado para ambiente de pruebas")
                // Datos del Emisor
                .emisorRfc(rfcEmisorDefault)
                .emisorRazonSocial(nombreEmisorDefault)
                .emisorNombre("")
                .emisorPaterno("")
                .emisorMaterno("")
                .emisorCorreo("")
                .emisorPais("MEX")
                .emisorDomicilioFiscal("CP " + (codigoPostalEmisorDefault != null ? codigoPostalEmisorDefault : "00000"))
                .emisorRegimenFiscal(regimenFiscalEmisorDefault)
                // Datos del Receptor
                .receptorRfc(request.getRfc())
                .receptorRazonSocial(request.getRazonSocial())
                .idReceptor(idReceptor)
                .receptorNombre(request.getNombre())
                .receptorPaterno(request.getPaterno())
                .receptorMaterno(request.getMaterno())
                .receptorCorreo(request.getCorreoElectronico())
                .receptorPais(request.getPais())
                .receptorDomicilioFiscal(request.getDomicilioFiscal())
                .receptorRegimenFiscal(request.getRegimenFiscal())
                .receptorUsoCfdi(request.getUsoCfdi())
                // Usuario que emitió la factura
                .usuario(usuarioId)
                // Datos de la Factura
                .codigoFacturacion(request.getCodigoFacturacion())
                .tiendaOrigen(toInt(request.getTienda()))
                 .fechaFactura(LocalDateTime.now())
                 .terminalBol(toInt(request.getTerminal()))
                 .boletaBol(toInt(request.getBoleta()))
                 .medioPago(request.getMedioPago())
                 .formaPago(request.getFormaPago())
                 .iepsDesglosado(Boolean.TRUE.equals(request.getIepsDesglosado()))
                // Relación con CFDI (sustitución)
                .uuidOrig(request.getUuidCfdiRelacionado() != null && !request.getUuidCfdiRelacionado().trim().isEmpty() 
                    ? request.getUuidCfdiRelacionado().trim().toUpperCase() : null)
                // Totales
                .subtotal(subtotal)
                .iva(iva)
                .ieps(BigDecimal.ZERO)
                .total(total)
                .build();

        // Persistir estatus en columnas reales de Oracle
        factura.setEstatusFactura(Integer.valueOf(EstadoFactura.POR_TIMBRAR.getCodigo()));
        factura.setStatusSat(EstadoFactura.POR_TIMBRAR.getDescripcion());
        factura.setEstadoDescripcion(EstadoFactura.POR_TIMBRAR.getDescripcion());

        return facturaRepository.save(factura);
    }

    private Integer toInt(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("\\D+", "");
        if (digits.isEmpty()) return null;
        try {
            return Integer.valueOf(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Resuelve ID_RECEPTOR en CLIENTES por RFC; crea si no existe
    private Long resolverIdReceptorPorRfc(FacturaFrontendRequest request) {
        String rfc = request != null ? request.getRfc() : null;
        if (rfc == null || rfc.trim().isEmpty()) {
            throw new IllegalArgumentException("RFC receptor vacío o nulo");
        }
        String normalized = rfc.trim().toUpperCase();
        Optional<ClienteCatalogo> existente = clienteCatalogoService.buscarPorRfc(normalized);
        if (existente.isPresent()) {
            return existente.get().getIdCliente();
        }
        ClienteCatalogo nuevo = new ClienteCatalogo();
        nuevo.setRfc(normalized);
        String razonSocial = request.getRazonSocial();
        if (razonSocial == null || razonSocial.trim().isEmpty()) {
            StringBuilder rsb = new StringBuilder();
            if (request.getNombre() != null) rsb.append(request.getNombre());
            if (request.getPaterno() != null) rsb.append(" ").append(request.getPaterno());
            if (request.getMaterno() != null) rsb.append(" ").append(request.getMaterno());
            razonSocial = rsb.toString().trim();
            if (razonSocial.isEmpty()) razonSocial = normalized; // fallback
        }
        nuevo.setRazonSocial(razonSocial);
        nuevo.setNombre(request.getNombre());
        nuevo.setPaterno(request.getPaterno());
        nuevo.setMaterno(request.getMaterno());
        nuevo.setCorreoElectronico(request.getCorreoElectronico());
        nuevo.setDomicilioFiscal(request.getDomicilioFiscal());
        nuevo.setRegimenFiscal(request.getRegimenFiscal());
        nuevo.setPais(request.getPais());
        nuevo.setRegistroTributario(request.getNoRegistroIdentidadTributaria());
        nuevo.setUsoCfdi(request.getUsoCfdi());
        nuevo.setFechaAlta(LocalDateTime.now());
        ClienteCatalogo guardado = clienteCatalogoService.guardar(nuevo);
        return guardado.getIdCliente();
    }

    /**
     * Guarda la factura en MongoDB
     */
    private void guardarEnMongo(FacturaFrontendRequest request, String xml, String uuid,
            BigDecimal subtotal, BigDecimal iva, BigDecimal total) {

        // Verificar que MongoDB esté disponible
        if (facturaMongoRepository == null) {
            logger.warn("FacturaMongoRepository no está disponible - omitiendo guardado en MongoDB");
            return;
        }

        // Crear mapas para emisor y receptor
        Map<String, Object> emisor = new HashMap<>();
        emisor.put("rfc", rfcEmisorDefault);
        emisor.put("razonSocial", nombreEmisorDefault);
        emisor.put("nombre", "");
        emisor.put("paterno", "");
        emisor.put("materno", "");
        emisor.put("correo", "");
        emisor.put("pais", "MEX");
        emisor.put("domicilioFiscal", "CP " + (codigoPostalEmisorDefault != null ? codigoPostalEmisorDefault : "00000"));
        emisor.put("regimenFiscal", regimenFiscalEmisorDefault);

        Map<String, Object> receptor = new HashMap<>();
        receptor.put("rfc", request.getRfc());
        receptor.put("razonSocial", request.getRazonSocial());
        receptor.put("nombre", request.getNombre());
        receptor.put("paterno", request.getPaterno());
        receptor.put("materno", request.getMaterno());
        receptor.put("correo", request.getCorreoElectronico());
        receptor.put("pais", request.getPais());
        receptor.put("domicilioFiscal", request.getDomicilioFiscal());
        receptor.put("regimenFiscal", request.getRegimenFiscal());
        receptor.put("usoCfdi", request.getUsoCfdi());

        FacturaMongo facturaMongo = FacturaMongo.builder()
                .uuid(uuid)
                .xmlContent(xml)
                .fechaGeneracion(LocalDateTime.now())
                .fechaTimbrado(null) // Se establecerá cuando Finkok timbre
                .estado(EstadoFactura.POR_TIMBRAR.getCodigo())
                .estadoDescripcion(EstadoFactura.POR_TIMBRAR.getDescripcion())
                .serie("A")
                .folio(generarFolioConsecutivo("A"))
                .cadenaOriginal("Simulada para ambiente de pruebas")
                .selloDigital("Simulado para ambiente de pruebas")
                .certificado("Simulado para ambiente de pruebas")

                .emisor(emisor)
                .receptor(receptor)
                .codigoFacturacion(request.getCodigoFacturacion())
                .tienda(request.getTienda())
                .fechaFactura(LocalDateTime.now())
                .terminal(request.getTerminal())
                .boleta(request.getBoleta())
                .medioPago(request.getMedioPago())
                .formaPago(request.getFormaPago())
                .iepsDesglosado(Boolean.TRUE.equals(request.getIepsDesglosado()))
                .subtotal(subtotal)
                .iva(iva)
                .ieps(BigDecimal.ZERO)
                .total(total)
                .build();

        facturaMongoRepository.save(facturaMongo);
    }

    /**
     * Guarda la factura en Oracle directamente con UUID de Finkok y estado EMITIDA
     */
    private Factura guardarEnOracleTimbrada(FacturaFrontendRequest request, String xmlTimbrado, String uuidFinkok,
            BigDecimal subtotal, BigDecimal iva, BigDecimal total, PacTimbradoResponse pacResponse, Long usuarioId) {

        Long idReceptor = resolverIdReceptorPorRfc(request);
        if (idReceptor == null) {
            throw new IllegalStateException("No se pudo resolver ID_RECEPTOR para RFC: " + request.getRfc());
        }

        Factura factura = Factura.builder()
                .uuid(uuidFinkok)
                .xmlContent(xmlTimbrado)
                .fechaGeneracion(LocalDateTime.now())
                .fechaTimbrado(pacResponse.getFechaTimbrado() != null ? pacResponse.getFechaTimbrado() : LocalDateTime.now())
                .estado(EstadoFactura.EMITIDA.getCodigo())
                .serie(pacResponse.getSerie() != null ? pacResponse.getSerie() : "A")
                .folio(pacResponse.getFolio() != null ? pacResponse.getFolio() : generarFolioConsecutivo("A"))
                .cadenaOriginal(pacResponse.getCadenaOriginal() != null ? pacResponse.getCadenaOriginal() : "")
                .selloDigital(pacResponse.getSelloDigital() != null ? pacResponse.getSelloDigital() : "")
                .certificado(pacResponse.getCertificado() != null ? pacResponse.getCertificado() : "")
                // Datos del Emisor
                .emisorRfc(rfcEmisorDefault)
                .emisorRazonSocial(nombreEmisorDefault)
                .emisorNombre("")
                .emisorPaterno("")
                .emisorMaterno("")
                .emisorCorreo("")
                .emisorPais("MEX")
                .emisorDomicilioFiscal("CP " + (codigoPostalEmisorDefault != null ? codigoPostalEmisorDefault : "00000"))
                .emisorRegimenFiscal(regimenFiscalEmisorDefault)
                // Datos del Receptor
                .receptorRfc(request.getRfc())
                .receptorRazonSocial(request.getRazonSocial())
                .idReceptor(idReceptor)
                .receptorNombre(request.getNombre())
                .receptorPaterno(request.getPaterno())
                .receptorMaterno(request.getMaterno())
                .receptorCorreo(request.getCorreoElectronico())
                .receptorPais(request.getPais())
                .receptorDomicilioFiscal(request.getDomicilioFiscal())
                .receptorRegimenFiscal(request.getRegimenFiscal())
                .receptorUsoCfdi(request.getUsoCfdi())
                // Usuario que emitió la factura
                .usuario(usuarioId)
                // Datos de la Factura
                .codigoFacturacion(request.getCodigoFacturacion())
                .tiendaOrigen(toInt(request.getTienda()))
                 .fechaFactura(LocalDateTime.now())
                 .terminalBol(toInt(request.getTerminal()))
                 .boletaBol(toInt(request.getBoleta()))
                 .medioPago(request.getMedioPago())
                 .formaPago(request.getFormaPago())
                 .iepsDesglosado(Boolean.TRUE.equals(request.getIepsDesglosado()))
                // Relación con CFDI (sustitución)
                .uuidOrig(request.getUuidCfdiRelacionado() != null && !request.getUuidCfdiRelacionado().trim().isEmpty() 
                    ? request.getUuidCfdiRelacionado().trim().toUpperCase() : null)
                // Totales
                .subtotal(subtotal)
                .iva(iva)
                .ieps(BigDecimal.ZERO)
                .total(total)
                .build();

        // Persistir estatus en columnas reales de Oracle con estado EMITIDA
        factura.setEstatusFactura(Integer.valueOf(EstadoFactura.EMITIDA.getCodigo()));
        factura.setStatusSat(EstadoFactura.EMITIDA.getDescripcion());
        factura.setEstadoDescripcion(EstadoFactura.EMITIDA.getDescripcion());

        Factura saved = facturaRepository.save(factura);
        facturaRepository.flush();
        return saved;
    }

    /**
     * Guarda la factura en MongoDB directamente con UUID de Finkok y estado EMITIDA
     */
    private void guardarEnMongoTimbrada(FacturaFrontendRequest request, String xmlTimbrado, String uuidFinkok,
            BigDecimal subtotal, BigDecimal iva, BigDecimal total, PacTimbradoResponse pacResponse) {

        // Verificar que MongoDB esté disponible
        if (facturaMongoRepository == null) {
            logger.warn("FacturaMongoRepository no está disponible - omitiendo guardado en MongoDB");
            return;
        }

        // Crear mapas para emisor y receptor
        Map<String, Object> emisor = new HashMap<>();
        emisor.put("rfc", rfcEmisorDefault);
        emisor.put("razonSocial", nombreEmisorDefault);
        emisor.put("nombre", "");
        emisor.put("paterno", "");
        emisor.put("materno", "");
        emisor.put("correo", "");
        emisor.put("pais", "MEX");
        emisor.put("domicilioFiscal", "CP " + (codigoPostalEmisorDefault != null ? codigoPostalEmisorDefault : "00000"));
        emisor.put("regimenFiscal", regimenFiscalEmisorDefault);

        Map<String, Object> receptor = new HashMap<>();
        receptor.put("rfc", request.getRfc());
        receptor.put("razonSocial", request.getRazonSocial());
        receptor.put("nombre", request.getNombre());
        receptor.put("paterno", request.getPaterno());
        receptor.put("materno", request.getMaterno());
        receptor.put("correo", request.getCorreoElectronico());
        receptor.put("pais", request.getPais());
        receptor.put("domicilioFiscal", request.getDomicilioFiscal());
        receptor.put("regimenFiscal", request.getRegimenFiscal());
        receptor.put("usoCfdi", request.getUsoCfdi());

        FacturaMongo facturaMongo = FacturaMongo.builder()
                .uuid(uuidFinkok)
                .xmlContent(xmlTimbrado)
                .fechaGeneracion(LocalDateTime.now())
                .fechaTimbrado(pacResponse.getFechaTimbrado() != null ? pacResponse.getFechaTimbrado() : LocalDateTime.now())
                .estado(EstadoFactura.EMITIDA.getCodigo())
                .estadoDescripcion(EstadoFactura.EMITIDA.getDescripcion())
                .serie(pacResponse.getSerie() != null ? pacResponse.getSerie() : "A")
                .folio(pacResponse.getFolio() != null ? pacResponse.getFolio() : generarFolioConsecutivo("A"))
                .cadenaOriginal(pacResponse.getCadenaOriginal() != null ? pacResponse.getCadenaOriginal() : "")
                .selloDigital(pacResponse.getSelloDigital() != null ? pacResponse.getSelloDigital() : "")
                .certificado(pacResponse.getCertificado() != null ? pacResponse.getCertificado() : "")
                .emisor(emisor)
                .receptor(receptor)
                .codigoFacturacion(request.getCodigoFacturacion())
                .tienda(request.getTienda())
                .fechaFactura(LocalDateTime.now())
                .terminal(request.getTerminal())
                .boleta(request.getBoleta())
                .medioPago(request.getMedioPago())
                .formaPago(request.getFormaPago())
                .iepsDesglosado(Boolean.TRUE.equals(request.getIepsDesglosado()))
                .subtotal(subtotal)
                .iva(iva)
                .ieps(BigDecimal.ZERO)
                .total(total)
                .build();

        facturaMongoRepository.save(facturaMongo);
    }

    /**
     * Guarda la factura en base de datos desde FacturaRequest
     */
    private void guardarFacturaEnBD(FacturaRequest request, String xml, String uuid,
            BigDecimal subtotal, BigDecimal iva, BigDecimal total, Long usuarioId) {

        // Crear entidad Factura para Oracle
        Factura facturaOracle = Factura.builder()
                .uuid(uuid)
                .xmlContent(xml)
                .fechaGeneracion(LocalDateTime.now())
                .fechaTimbrado(LocalDateTime.now())
                .estado(EstadoFactura.EMITIDA.getCodigo())
                .serie("A")
                .folio(generarFolioConsecutivo("A"))
                .cadenaOriginal("Simulada para ambiente de pruebas")
                .selloDigital("Simulado para ambiente de pruebas")
                .certificado("Simulado para ambiente de pruebas")
                // Datos del Emisor
                .emisorRfc(request.getRfcEmisor())
                .emisorRazonSocial(request.getNombreEmisor())
                .emisorNombre("")
                .emisorPaterno("")
                .emisorMaterno("")
                .emisorCorreo("")
                .emisorPais("MEX")
                .emisorDomicilioFiscal("CP " + request.getCodigoPostalEmisor())
                .emisorRegimenFiscal(request.getRegimenFiscalEmisor())
                // Datos del Receptor
                .receptorRfc(request.getRfcReceptor())
                .receptorRazonSocial(request.getNombreReceptor())
                .receptorNombre("")
                .receptorPaterno("")
                .receptorMaterno("")
                .receptorCorreo("")
                .receptorPais("MEX")
                .receptorDomicilioFiscal("CP " + request.getCodigoPostalReceptor())
                .receptorRegimenFiscal(request.getRegimenFiscalReceptor())
                .receptorUsoCfdi(request.getUsoCFDI())
                // Usuario que emitió la factura
                .usuario(usuarioId)
                // Datos de la Factura
                .codigoFacturacion("FAC-" + uuid.substring(0, 8))
                .tienda("Tienda Central")
                 .fechaFactura(LocalDateTime.now())
                .terminal("TERM-001")
                .boleta("BOL-" + uuid.substring(0, 8))
                .medioPago(request.getMetodoPago())
                 .formaPago(request.getFormaPago())
                 .iepsDesglosado(false)
                // Totales
                .subtotal(subtotal)
                .iva(iva)
                .ieps(BigDecimal.ZERO)
                .total(total)
                .build();

        // Guardar en Oracle
        facturaRepository.save(facturaOracle);

        // Guardar en MongoDB solo si el perfil activo es "mongo"
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "oracle";
        if ("mongo".equals(activeProfile) && facturaMongoRepository != null) {
            try {
                guardarEnMongoDesdeFacturaRequest(request, xml, uuid, subtotal, iva, total);
            } catch (Exception e) {
                logger.warn("No se pudo guardar en MongoDB (puede no estar disponible): {}", e.getMessage());
            }
        }
    }

    /**
     * Guarda la factura en MongoDB desde FacturaRequest
     */
    private void guardarEnMongoDesdeFacturaRequest(FacturaRequest request, String xml, String uuid,
            BigDecimal subtotal, BigDecimal iva, BigDecimal total) {

        // Verificar que MongoDB esté disponible
        if (facturaMongoRepository == null) {
            logger.warn("FacturaMongoRepository no está disponible - omitiendo guardado en MongoDB");
            return;
        }

        // Crear mapas para emisor y receptor
        Map<String, Object> emisor = new HashMap<>();
        emisor.put("rfc", request.getRfcEmisor());
        emisor.put("razonSocial", request.getNombreEmisor());
        emisor.put("nombre", "");
        emisor.put("paterno", "");
        emisor.put("materno", "");
        emisor.put("correo", "");
        emisor.put("pais", "MEX");
        emisor.put("domicilioFiscal", "CP " + request.getCodigoPostalEmisor());
        emisor.put("regimenFiscal", request.getRegimenFiscalEmisor());

        Map<String, Object> receptor = new HashMap<>();
        receptor.put("rfc", request.getRfcReceptor());
        receptor.put("razonSocial", request.getNombreReceptor());
        receptor.put("nombre", "");
        receptor.put("paterno", "");
        receptor.put("materno", "");
        receptor.put("correo", "");
        receptor.put("pais", "MEX");
        receptor.put("domicilioFiscal", "CP " + request.getCodigoPostalReceptor());
        receptor.put("regimenFiscal", request.getRegimenFiscalReceptor());
        receptor.put("usoCfdi", request.getUsoCFDI());

        FacturaMongo facturaMongo = FacturaMongo.builder()
                .uuid(uuid)
                .xmlContent(xml)
                .fechaGeneracion(LocalDateTime.now())
                .fechaTimbrado(LocalDateTime.now())
                .estado("VIGENTE")
                .serie("A")
                .folio("1")
                .cadenaOriginal("Simulada para ambiente de pruebas")
                .selloDigital("Simulado para ambiente de pruebas")
                .certificado("Simulado para ambiente de pruebas")

                .emisor(emisor)
                .receptor(receptor)
                .codigoFacturacion("FAC-" + uuid.substring(0, 8))
                .tienda("Tienda Central")
                .fechaFactura(LocalDateTime.now())
                .terminal("TERM-001")
                .boleta("BOL-" + uuid.substring(0, 8))
                .medioPago(request.getMetodoPago())
                .formaPago(request.getFormaPago())
                .iepsDesglosado(false)
                .subtotal(subtotal)
                .iva(iva)
                .ieps(BigDecimal.ZERO)
                .total(total)
                .build();

        facturaMongoRepository.save(facturaMongo);
    }

    private Map<String, Object> construirDatosFacturaFrontend(FacturaFrontendRequest request,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total,
            String uuid,
            String folio) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("folioFiscal", uuid);
        datos.put("serie", "A");
        datos.put("folio", folio);
        datos.put("fechaTimbrado", LocalDateTime.now());
        datos.put("subtotal", subtotal);
        datos.put("iva", iva);
        datos.put("total", total);
        datos.put("cadenaOriginal", "Simulada para ambiente de pruebas");
        datos.put("selloDigital", "Simulado para ambiente de pruebas");
        datos.put("certificado", "Simulado para ambiente de pruebas");
        return datos;
    }

    public Map<String, Object> consultarFacturasPorEmpresa(String rfcEmpresa, String tienda, String fechaInicio,
            String fechaFin) {
        Map<String, Object> response = new HashMap<>();

        try {

            Map<String, Object> factura1 = new HashMap<>();
            factura1.put("uuid", "550e8400-e29b-41d4-a716-446655440001");
            factura1.put("codigoFacturacion", "FAC-550e8400");
            factura1.put("tienda", tienda != null ? tienda : "T001");
            factura1.put("fechaFactura", "2024-01-15");
            factura1.put("terminal", "TERM-001");
            factura1.put("boleta", "BOL-550e8400");
            factura1.put("razonSocial", "Empresa Ejemplo S.A. de C.V.");
            factura1.put("rfc", rfcEmpresa != null ? rfcEmpresa : "EEJ920629TE3");
            factura1.put("total", 1250.50);
            factura1.put("estado", "VIGENTE");
            factura1.put("medioPago", "Efectivo");
            factura1.put("formaPago", "Pago en una sola exhibición");

            Map<String, Object> factura2 = new HashMap<>();
            factura2.put("uuid", "550e8400-e29b-41d4-a716-446655440002");
            factura2.put("codigoFacturacion", "FAC-550e8401");
            factura2.put("tienda", tienda != null ? tienda : "T002");
            factura2.put("fechaFactura", "2024-01-16");
            factura2.put("terminal", "TERM-002");
            factura2.put("boleta", "BOL-550e8401");
            factura2.put("razonSocial", "Cliente General");
            factura2.put("rfc", "XAXX010101000");
            factura2.put("total", 3450.75);
            factura2.put("estado", "VIGENTE");
            factura2.put("medioPago", "Tarjeta de crédito");
            factura2.put("formaPago", "Pago en una sola exhibición");

            Map<String, Object> factura3 = new HashMap<>();
            factura3.put("uuid", "550e8400-e29b-41d4-a716-446655440003");
            factura3.put("codigoFacturacion", "FAC-550e8402");
            factura3.put("tienda", tienda != null ? tienda : "T003");
            factura3.put("fechaFactura", "2024-01-17");
            factura3.put("terminal", "TERM-003");
            factura3.put("boleta", "BOL-550e8402");
            factura3.put("razonSocial", "María Rodríguez");
            factura3.put("rfc", "ROMA800101ABC");
            factura3.put("total", 5678.90);
            factura3.put("estado", "VIGENTE");
            factura3.put("medioPago", "Transferencia");
            factura3.put("formaPago", "Pago en una sola exhibición");

            java.util.List<Map<String, Object>> facturas = new java.util.ArrayList<>();
            facturas.add(factura1);
            facturas.add(factura2);
            facturas.add(factura3);

            response.put("exitoso", true);
            response.put("mensaje", "Facturas consultadas exitosamente");
            response.put("facturas", facturas);
            response.put("totalFacturas", facturas.size());

        } catch (Exception e) {
            response.put("exitoso", false);
            response.put("error", "Error al consultar facturas: " + e.getMessage());
        }

        return response;
    }

    public FacturaRequest convertirXmlAFacturaRequest(String xmlContent) throws Exception {
        logger.info("XML recibido para conversión: {}", xmlContent);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));

        Element root = document.getDocumentElement();
        logger.info("Elemento raíz del XML: {}", root.getTagName());

        FacturaRequest request = new FacturaRequest();

        String rfcEmisor = getElementText(root, "emisor_rfc");
        String nombreEmisor = getElementText(root, "emisor_nombre");
        String regimenEmisor = getElementText(root, "emisor_regimen");
        String cpEmisor = getElementText(root, "emisor_cp");

        logger.info("Datos emisor extraídos - RFC: {}, Nombre: {}, Régimen: {}, CP: {}",
                rfcEmisor, nombreEmisor, regimenEmisor, cpEmisor);

        request.setRfcEmisor(rfcEmisor);
        request.setNombreEmisor(nombreEmisor);
        request.setRegimenFiscalEmisor(regimenEmisor);
        request.setCodigoPostalEmisor(cpEmisor);

        String rfcReceptor = getElementText(root, "receptor_rfc");
        String nombreReceptor = getElementText(root, "receptor_nombre");
        String regimenReceptor = getElementText(root, "receptor_regimen");
        String cpReceptor = getElementText(root, "receptor_cp");

        logger.info("Datos receptor extraídos - RFC: {}, Nombre: {}, Régimen: {}, CP: {}",
                rfcReceptor, nombreReceptor, regimenReceptor, cpReceptor);

        request.setRfcReceptor(rfcReceptor);
        request.setNombreReceptor(nombreReceptor);
        request.setRegimenFiscalReceptor(regimenReceptor);
        request.setCodigoPostalReceptor(cpReceptor);

        String usoCfdi = getElementText(root, "uso_cfdi");
        String formaPago = getElementText(root, "forma_pago");
        String metodoPago = getElementText(root, "metodo_pago");

        logger.info("Datos factura extraídos - Uso CFDI: {}, Forma Pago: {}, Método Pago: {}",
                usoCfdi, formaPago, metodoPago);

        request.setUsoCFDI(usoCfdi);
        request.setFormaPago(formaPago);
        request.setMetodoPago(metodoPago);

        NodeList conceptosNode = root.getElementsByTagName("conceptos");
        if (conceptosNode.getLength() > 0) {
            Element conceptosElement = (Element) conceptosNode.item(0);
            NodeList conceptosList = conceptosElement.getElementsByTagName("concepto");

            ArrayList<FacturaRequest.Concepto> conceptos = new ArrayList<>();

            for (int i = 0; i < conceptosList.getLength(); i++) {
                Element conceptoElement = (Element) conceptosList.item(i);

                FacturaRequest.Concepto concepto = new FacturaRequest.Concepto();
                concepto.setDescripcion(getElementText(conceptoElement, "descripcion"));
                concepto.setCantidad(new BigDecimal(getElementText(conceptoElement, "cantidad")));
                concepto.setUnidad(getElementText(conceptoElement, "unidad"));
                concepto.setPrecioUnitario(new BigDecimal(getElementText(conceptoElement, "precio_unitario")));

                BigDecimal importe = concepto.getCantidad().multiply(concepto.getPrecioUnitario());
                concepto.setImporte(importe);

                conceptos.add(concepto);
            }

            request.setConceptos(conceptos);
        }

        return request;
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return "";
    }
    
    /**
     * Busca una factura por su UUID
     * 
     * @param uuid UUID de la factura
     * @return Factura encontrada o null si no existe
     */
    public Factura buscarPorUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            logger.warn("UUID nulo o vacío en buscarPorUuid");
            return null;
        }
        
        try {
            Optional<Factura> facturaOpt = facturaRepository.findByUuid(uuid);
            if (facturaOpt.isPresent()) {
                logger.info("Factura encontrada con UUID: {}", uuid);
                return facturaOpt.get();
            } else {
                logger.warn("No se encontró factura con UUID: {}", uuid);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error al buscar factura por UUID {}: {}", uuid, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Obtiene el PDF de una factura como bytes
     * 
     * @param uuid UUID de la factura
     * @return Bytes del PDF o un PDF de prueba si hay error
     */
    public byte[] obtenerPdfComoBytes(String uuid) {
        try {
            logger.info("Obteniendo PDF para UUID: {}", uuid);
            Factura factura = buscarPorUuid(uuid);
            if (factura == null) {
                logger.warn("No se pudo generar PDF: factura no encontrada con UUID {}", uuid);
                // Generar PDF de prueba para evitar error
                logger.info("Generando PDF de prueba para UUID no encontrado: {}", uuid);
                return generarPdfPrueba(uuid);
            }
            logger.info("Factura encontrada para generar PDF: {}{}", factura.getSerie(), factura.getFolio());
            
            // Convertir datos de la factura a un mapa para el servicio de PDF
            Map<String, Object> datosFactura = new HashMap<>();
            datosFactura.put("uuid", factura.getUuid());
            datosFactura.put("serie", factura.getSerie());
            datosFactura.put("folio", factura.getFolio());
            datosFactura.put("fechaTimbrado", factura.getFechaTimbrado());
            datosFactura.put("rfcEmisor", factura.getEmisorRfc());
            datosFactura.put("nombreEmisor", factura.getEmisorRazonSocial());
            datosFactura.put("rfcReceptor", factura.getReceptorRfc());
            datosFactura.put("nombreReceptor", factura.getReceptorRazonSocial());
            datosFactura.put("tipoComprobante", mapearTipoComprobante(factura.getTipoFactura()));
            datosFactura.put("subtotal", factura.getSubtotal());
            datosFactura.put("iva", factura.getIva());
            datosFactura.put("total", factura.getTotal());
            if (factura.getUuidOrig() != null && !factura.getUuidOrig().trim().isEmpty()) {
                datosFactura.put("tipoDocumento", "Refactura");
                datosFactura.put("uuidCfdiRelacionado", factura.getUuidOrig().trim());
            }

            // Intentar extraer conceptos del XML timbrado primero (contiene todos los datos del catálogo)
            List<Map<String, Object>> conceptos = null;
            if (factura.getXmlContent() != null && !factura.getXmlContent().trim().isEmpty()) {
                try {
                    conceptos = extraerConceptosDesdeXml(factura.getXmlContent());
                    if (conceptos != null && !conceptos.isEmpty()) {
                        logger.info("Conceptos extraídos del XML timbrado: {} renglones", conceptos.size());
                    }
                } catch (Exception e) {
                    logger.warn("Error al extraer conceptos del XML, intentando desde TICKETS_DETALLE: {}", e.getMessage());
                }
            }
            
            // Si no se pudieron extraer del XML, intentar desde TICKETS_DETALLE (modo automático)
            if (conceptos == null || conceptos.isEmpty()) {
                conceptos = construirConceptosDesdeDetalle(uuid);
                if (conceptos != null && !conceptos.isEmpty()) {
                    logger.info("Conceptos construidos desde TICKETS_DETALLE: {} renglones", conceptos.size());
                }
            }
            
            if (conceptos != null && !conceptos.isEmpty()) {
                datosFactura.put("conceptos", conceptos);
            }
            // Intentar extraer datos de nómina desde el XML timbrado primero (contiene todos los datos incluyendo campos laborales)
            Map<String, Object> datosNomina = null;
            if (factura.getXmlContent() != null && !factura.getXmlContent().trim().isEmpty() && factura.getTipoFactura() != null && factura.getTipoFactura().equals(4)) {
                try {
                    datosNomina = extraerDatosNominaDesdeXml(factura.getXmlContent());
                    if (datosNomina != null && !datosNomina.isEmpty()) {
                        logger.info("Datos de nómina extraídos del XML timbrado para UUID {}", uuid);
                        // Agregar correo electrónico desde la factura si está disponible
                        if (factura.getReceptorCorreo() != null && !factura.getReceptorCorreo().trim().isEmpty()) {
                            datosNomina.put("correoElectronico", factura.getReceptorCorreo().trim());
                        }
                        datosFactura.put("nomina", datosNomina);
                    }
                } catch (Exception e) {
                    logger.warn("Error al extraer datos de nómina del XML, intentando desde BD: {}", e.getMessage());
                }
            }
            
            // Si no se pudieron extraer del XML, intentar desde la BD
            if (datosNomina == null || datosNomina.isEmpty()) {
                try {
                    if (nominaOracleDAO != null && conceptoOracleDAO != null) {
                        var idOpt = conceptoOracleDAO.obtenerIdFacturaPorUuid(uuid);
                        if (idOpt.isPresent() && idOpt.get() != null) {
                            Long idFactura = idOpt.get();
                            java.util.Map<String, Object> nomina = nominaOracleDAO.buscarPorIdFactura(idFactura);
                            if (nomina != null && !nomina.isEmpty()) {
                                // Agregar correo electrónico desde la factura si está disponible
                                if (factura.getReceptorCorreo() != null && !factura.getReceptorCorreo().trim().isEmpty()) {
                                    nomina.put("correoElectronico", factura.getReceptorCorreo().trim());
                                }
                                datosFactura.put("nomina", nomina);
                                logger.info("Datos de nómina obtenidos desde BD para UUID {}", uuid);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("No se pudo cargar datos de nómina para UUID {}: {}", uuid, e.getMessage());
                }
            }
            
            // Generar PDF con los datos de la factura
            byte[] pdfBytes = iTextPdfService.generarPdf(datosFactura);
            logger.info("PDF generado exitosamente para factura {}: {} bytes", uuid, pdfBytes != null ? pdfBytes.length : 0);
            
            // Verificar que el PDF generado sea válido
            if (pdfBytes == null || pdfBytes.length < 100) {
                logger.warn("PDF generado inválido o demasiado pequeño ({}), generando PDF de prueba", 
                           pdfBytes != null ? pdfBytes.length : 0);
                return generarPdfPrueba(uuid);
            }
            
            return pdfBytes;
            
        } catch (Exception e) {
            logger.error("Error al generar PDF para factura {}: {}", uuid, e.getMessage(), e);
            return generarPdfPrueba(uuid);
        }
    }

    /**
     * Obtiene el PDF de una factura como bytes, permitiendo pasar logoConfig para colores/branding
     */
    public byte[] obtenerPdfComoBytes(String uuid, Map<String, Object> logoConfig) {
        try {
            logger.info("Obteniendo PDF para UUID: {} con logoConfig", uuid);
            Factura factura = buscarPorUuid(uuid);
            if (factura == null) {
                logger.warn("No se pudo generar PDF: factura no encontrada con UUID {}", uuid);
                return generarPdfPrueba(uuid, logoConfig);
            }

            // Verificar si es un complemento de pago (tipo_factura = 5)
            boolean esComplementoPago = factura.getTipoFactura() != null && factura.getTipoFactura().equals(5);
            
            if (esComplementoPago) {
                // Manejar complemento de pago de manera específica
                try {
                    byte[] pdfComplemento = generarPdfComplementoPago(uuid, factura, logoConfig);
                    if (pdfComplemento != null && pdfComplemento.length > 100) {
                        return pdfComplemento;
                    } else {
                        logger.warn("PDF del complemento está vacío o inválido, generando PDF de prueba");
                        return generarPdfPrueba(uuid, logoConfig);
                    }
                } catch (Exception e) {
                    logger.error("Error al generar PDF del complemento de pago {}: {}", uuid, e.getMessage(), e);
                    // Fallback: generar PDF de prueba
                    return generarPdfPrueba(uuid, logoConfig);
                }
            }
            
            // Verificar si es una retención de pagos (tipo_factura = 6)
            boolean esRetencion = factura.getTipoFactura() != null && factura.getTipoFactura().equals(6);
            
            if (esRetencion) {
                // Manejar retención de pagos de manera específica
                try {
                    byte[] pdfRetencion = generarPdfRetencion(uuid, factura, logoConfig);
                    if (pdfRetencion != null && pdfRetencion.length > 100) {
                        return pdfRetencion;
                    } else {
                        logger.warn("PDF de la retención está vacío o inválido, generando PDF de prueba");
                        return generarPdfPrueba(uuid, logoConfig);
                    }
                } catch (Exception e) {
                    logger.error("Error al generar PDF de la retención {}: {}", uuid, e.getMessage(), e);
                    // Fallback: generar PDF de prueba
                    return generarPdfPrueba(uuid, logoConfig);
                }
            }
            
            // Verificar si es una Carta Porte (tipo_factura = 3)
            Integer tipoFacturaValue = factura.getTipoFactura();
            boolean esCartaPorte = tipoFacturaValue != null && tipoFacturaValue.equals(3);
            logger.info("🔍 obtenerPdfComoBytes: UUID={}, tipoFactura={}, esCartaPorte={}, serie={}", uuid, tipoFacturaValue, esCartaPorte, factura.getSerie());

            Map<String, Object> datosFactura = new HashMap<>();
            datosFactura.put("uuid", factura.getUuid());
            datosFactura.put("serie", factura.getSerie());
            datosFactura.put("folio", factura.getFolio());
            datosFactura.put("fechaTimbrado", factura.getFechaTimbrado());
            datosFactura.put("rfcEmisor", factura.getEmisorRfc());
            datosFactura.put("nombreEmisor", factura.getEmisorRazonSocial());
            datosFactura.put("rfcReceptor", factura.getReceptorRfc());
            datosFactura.put("nombreReceptor", factura.getReceptorRazonSocial());
            String tipoComprobanteMapeado = mapearTipoComprobante(factura.getTipoFactura());
            datosFactura.put("tipoComprobante", tipoComprobanteMapeado);
            logger.info("🔍 tipoComprobante mapeado: '{}' para tipoFactura={}", tipoComprobanteMapeado, tipoFacturaValue);
            datosFactura.put("subtotal", factura.getSubtotal());
            datosFactura.put("iva", factura.getIva());
            datosFactura.put("total", factura.getTotal());
            if (factura.getUuidOrig() != null && !factura.getUuidOrig().trim().isEmpty()) {
                datosFactura.put("tipoDocumento", "Refactura");
                datosFactura.put("uuidCfdiRelacionado", factura.getUuidOrig().trim());
            }

            // Para Carta Porte, NO extraer conceptos (se usa el diseño específico agregarSeccionCartaPorte)
            if (!esCartaPorte) {
                logger.info("⚠️ NO es Carta Porte, continuando con flujo genérico de conceptos");
                // Intentar extraer conceptos del XML timbrado primero (contiene todos los datos del catálogo)
                List<Map<String, Object>> conceptos = null;
                if (factura.getXmlContent() != null && !factura.getXmlContent().trim().isEmpty()) {
                    try {
                        conceptos = extraerConceptosDesdeXml(factura.getXmlContent());
                        if (conceptos != null && !conceptos.isEmpty()) {
                            logger.info("Conceptos extraídos del XML timbrado: {} renglones", conceptos.size());
                        }
                    } catch (Exception e) {
                        logger.warn("Error al extraer conceptos del XML, intentando desde TICKETS_DETALLE: {}", e.getMessage());
                    }
                }
                
                // Si no se pudieron extraer del XML, intentar desde TICKETS_DETALLE (modo automático)
                if (conceptos == null || conceptos.isEmpty()) {
                    conceptos = construirConceptosDesdeDetalle(uuid);
                    if (conceptos != null && !conceptos.isEmpty()) {
                        logger.info("Conceptos construidos desde TICKETS_DETALLE: {} renglones", conceptos.size());
                    }
                }
                
                if (conceptos != null && !conceptos.isEmpty()) {
                    datosFactura.put("conceptos", conceptos);
                }
            } else {
                // Para Carta Porte, usar CartaPorteService para generar el PDF con el diseño correcto
                logger.info("✓ Carta Porte detectada (tipoFactura=3), usando CartaPorteService.generarPdfDesdeUuid para generar PDF");
                if (cartaPorteService != null) {
                    logger.info("CartaPorteService disponible, generando PDF...");
                    byte[] pdfCartaPorte = cartaPorteService.generarPdfDesdeUuid(uuid, logoConfig);
                    if (pdfCartaPorte != null && pdfCartaPorte.length > 100) {
                        logger.info("✓ PDF de Carta Porte generado exitosamente: {} bytes", pdfCartaPorte.length);
                        return pdfCartaPorte;
                    } else {
                        logger.error("✗ PDF de Carta Porte generado está vacío o inválido ({} bytes)", pdfCartaPorte != null ? pdfCartaPorte.length : 0);
                        throw new RuntimeException("No se pudo generar el PDF de Carta Porte");
                    }
                } else {
                    logger.error("✗ CartaPorteService no disponible para generar PDF de Carta Porte");
                    throw new RuntimeException("CartaPorteService no disponible");
                }
            }

            // Intentar extraer datos de nómina desde el XML timbrado primero (contiene todos los datos incluyendo campos laborales)
            Map<String, Object> datosNominaLogo = null;
            if (factura.getXmlContent() != null && !factura.getXmlContent().trim().isEmpty() && factura.getTipoFactura() != null && factura.getTipoFactura().equals(4)) {
                try {
                    datosNominaLogo = extraerDatosNominaDesdeXml(factura.getXmlContent());
                    if (datosNominaLogo != null && !datosNominaLogo.isEmpty()) {
                        logger.info("Datos de nómina extraídos del XML timbrado para UUID {}", uuid);
                        // Agregar correo electrónico desde la factura si está disponible
                        if (factura.getReceptorCorreo() != null && !factura.getReceptorCorreo().trim().isEmpty()) {
                            datosNominaLogo.put("correoElectronico", factura.getReceptorCorreo().trim());
                        }
                        datosFactura.put("nomina", datosNominaLogo);
                    }
                } catch (Exception e) {
                    logger.warn("Error al extraer datos de nómina del XML, intentando desde BD: {}", e.getMessage());
                }
            }
            
            // Si no se pudieron extraer del XML, intentar desde la BD
            if (datosNominaLogo == null || datosNominaLogo.isEmpty()) {
                try {
                    if (nominaOracleDAO != null && conceptoOracleDAO != null) {
                        var idOpt = conceptoOracleDAO.obtenerIdFacturaPorUuid(uuid);
                        if (idOpt.isPresent() && idOpt.get() != null) {
                            Long idFactura = idOpt.get();
                            java.util.Map<String, Object> nomina = nominaOracleDAO.buscarPorIdFactura(idFactura);
                            if (nomina != null && !nomina.isEmpty()) {
                                // Agregar correo electrónico desde la factura si está disponible
                                if (factura.getReceptorCorreo() != null && !factura.getReceptorCorreo().trim().isEmpty()) {
                                    nomina.put("correoElectronico", factura.getReceptorCorreo().trim());
                                }
                                datosFactura.put("nomina", nomina);
                                logger.info("Datos de nómina obtenidos desde BD para UUID {}", uuid);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("No se pudo cargar datos de nómina para UUID {}: {}", uuid, e.getMessage());
                }
            }

            byte[] pdfBytes = iTextPdfService.generarPdfConLogo(datosFactura, logoConfig != null ? logoConfig : new HashMap<>());
            logger.info("PDF generado exitosamente para factura {}: {} bytes", uuid, pdfBytes != null ? pdfBytes.length : 0);

            if (pdfBytes == null || pdfBytes.length < 100) {
                logger.warn("PDF con logo inválido o demasiado pequeño ({}), generando PDF de prueba", 
                           pdfBytes != null ? pdfBytes.length : 0);
                return generarPdfPrueba(uuid, logoConfig);
            }
            return pdfBytes;
        } catch (Exception e) {
            logger.error("Error al generar PDF con logo para factura {}: {}", uuid, e.getMessage(), e);
            return generarPdfPrueba(uuid, logoConfig);
        }
    }

    /**
     * Construye la lista de conceptos para el PDF a partir de TICKETS_DETALLE,
     * resolviendo primero el ID_FACTURA por UUID y consultando por JOIN.
     */
    /**
     * Construye conceptos desde TICKETS_DETALLE (modo automático)
     * Hace JOIN con CATALOGOS_PRODUCTOS_SERVICIOS para obtener campos del catálogo
     */
    private List<Map<String, Object>> construirConceptosDesdeDetalle(String uuid) {
        try {
            if (conceptoOracleDAO == null || ticketDetalleService == null) {
                logger.info("conceptoOracleDAO o ticketDetalleService no disponibles; se omite desglose de conceptos");
                return null;
            }
            var idOpt = conceptoOracleDAO.obtenerIdFacturaPorUuid(uuid);
            if (idOpt.isEmpty() || idOpt.get() == null) {
                logger.warn("No se pudo resolver ID_FACTURA para UUID {} al construir conceptos", uuid);
                return null;
            }
            Long idFactura = idOpt.get();
            
            // MODO AUTOMÁTICO: Consultar desde TICKETS_DETALLE con JOIN a CATALOGOS_PRODUCTOS_SERVICIOS
            List<com.cibercom.facturacion_back.dto.TicketDetalleDto> detalles = ticketDetalleService.buscarDetallesPorIdFactura(idFactura);
            if (detalles == null || detalles.isEmpty()) {
                logger.info("Sin detalles en TICKETS_DETALLE para ID_FACTURA {}", idFactura);
                return null;
            }
            
            List<Map<String, Object>> conceptos = new java.util.ArrayList<>();
            for (com.cibercom.facturacion_back.dto.TicketDetalleDto d : detalles) {
                Map<String, Object> c = new HashMap<>();
                c.put("cantidad", d.getCantidad() != null ? d.getCantidad() : java.math.BigDecimal.ONE);
                c.put("descripcion", d.getDescripcion());
                // Usar unidad del catálogo si está disponible, sino usar la del detalle
                String unidad = (d.getUnidadCatalogo() != null && !d.getUnidadCatalogo().trim().isEmpty()) 
                        ? d.getUnidadCatalogo() : d.getUnidad();
                c.put("unidad", unidad);
                // Usar claveUnidad del catálogo si está disponible
                if (d.getClaveUnidad() != null && !d.getClaveUnidad().trim().isEmpty()) {
                    c.put("claveUnidad", d.getClaveUnidad());
                }
                c.put("valorUnitario", d.getPrecioUnitario());
                // Usamos SUBTOTAL para "Importe" y IVA_IMPORTE para "IVA"
                c.put("importe", d.getSubtotal() != null ? d.getSubtotal() : d.getTotal());
                c.put("iva", d.getIvaImporte());
                // Campos del catálogo CATALOGOS_PRODUCTOS_SERVICIOS
                if (d.getClaveProdServ() != null && !d.getClaveProdServ().trim().isEmpty()) {
                    c.put("claveProdServ", d.getClaveProdServ());
                }
                if (d.getObjetoImpuesto() != null && !d.getObjetoImpuesto().trim().isEmpty()) {
                    c.put("objetoImp", d.getObjetoImpuesto());
                }
                // Tasa IVA: priorizar la del catálogo, sino usar la del detalle
                if (d.getTasaIva() != null) {
                    c.put("tasaIva", d.getTasaIva());
                } else if (d.getIvaPorcentaje() != null) {
                    c.put("tasaIva", d.getIvaPorcentaje());
                }
                conceptos.add(c);
            }
            logger.info("Conceptos construidos desde TICKETS_DETALLE (modo automático): {} renglones", conceptos.size());
            return conceptos;
        } catch (Exception e) {
            logger.warn("Fallo al construir conceptos desde detalle: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrae los conceptos directamente del XML timbrado
     * Esto incluye todos los campos del catálogo que se enviaron en el XML
     */
    private List<Map<String, Object>> extraerConceptosDesdeXml(String xmlContent) {
        try {
            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                return null;
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));
            
            Element root = document.getDocumentElement();
            
            // Buscar el namespace de CFDI
            String cfdiNamespace = root.getNamespaceURI();
            if (cfdiNamespace == null) {
                cfdiNamespace = "http://www.sat.gob.mx/cfd/4";
            }
            
            // Buscar nodos Concepto
            NodeList conceptosNode = root.getElementsByTagNameNS(cfdiNamespace, "Concepto");
            if (conceptosNode.getLength() == 0) {
                // Intentar sin namespace
                conceptosNode = root.getElementsByTagName("Concepto");
            }
            
            if (conceptosNode.getLength() == 0) {
                logger.info("No se encontraron conceptos en el XML");
                return null;
            }
            
            List<Map<String, Object>> conceptos = new java.util.ArrayList<>();
            
            for (int i = 0; i < conceptosNode.getLength(); i++) {
                Element conceptoElement = (Element) conceptosNode.item(i);
                Map<String, Object> concepto = new HashMap<>();
                
                // Extraer atributos del concepto
                String cantidad = conceptoElement.getAttribute("Cantidad");
                String claveProdServ = conceptoElement.getAttribute("ClaveProdServ");
                String claveUnidad = conceptoElement.getAttribute("ClaveUnidad");
                String unidad = conceptoElement.getAttribute("Unidad");
                String descripcion = conceptoElement.getAttribute("Descripcion");
                String valorUnitario = conceptoElement.getAttribute("ValorUnitario");
                String importe = conceptoElement.getAttribute("Importe");
                String objetoImp = conceptoElement.getAttribute("ObjetoImp");
                
                // Convertir valores numéricos a BigDecimal para consistencia
                BigDecimal cantidadBD = BigDecimal.ONE;
                if (cantidad != null && !cantidad.isEmpty()) {
                    try {
                        cantidadBD = new BigDecimal(cantidad);
                    } catch (Exception e) {
                        logger.warn("Error al convertir cantidad '{}' a BigDecimal: {}", cantidad, e.getMessage());
                    }
                }
                
                BigDecimal valorUnitarioBD = BigDecimal.ZERO;
                if (valorUnitario != null && !valorUnitario.isEmpty()) {
                    try {
                        valorUnitarioBD = new BigDecimal(valorUnitario);
                    } catch (Exception e) {
                        logger.warn("Error al convertir valorUnitario '{}' a BigDecimal: {}", valorUnitario, e.getMessage());
                    }
                }
                
                BigDecimal importeBD = BigDecimal.ZERO;
                if (importe != null && !importe.isEmpty()) {
                    try {
                        importeBD = new BigDecimal(importe);
                    } catch (Exception e) {
                        logger.warn("Error al convertir importe '{}' a BigDecimal: {}", importe, e.getMessage());
                    }
                }
                
                concepto.put("cantidad", cantidadBD);
                concepto.put("descripcion", descripcion != null ? descripcion : "");
                concepto.put("unidad", unidad != null && !unidad.isEmpty() ? unidad : "");
                concepto.put("claveUnidad", claveUnidad != null && !claveUnidad.isEmpty() ? claveUnidad : "");
                concepto.put("claveProdServ", claveProdServ != null && !claveProdServ.isEmpty() ? claveProdServ : "");
                concepto.put("valorUnitario", valorUnitarioBD);
                concepto.put("importe", importeBD);
                concepto.put("objetoImp", objetoImp != null && !objetoImp.isEmpty() ? objetoImp : "02");
                
                // Extraer impuestos del concepto
                BigDecimal ivaImporte = BigDecimal.ZERO;
                BigDecimal tasaOCuota = BigDecimal.ZERO;
                
                NodeList impuestosNode = conceptoElement.getElementsByTagNameNS(cfdiNamespace, "Impuestos");
                if (impuestosNode.getLength() == 0) {
                    impuestosNode = conceptoElement.getElementsByTagName("Impuestos");
                }
                
                if (impuestosNode.getLength() > 0) {
                    Element impuestosElement = (Element) impuestosNode.item(0);
                    NodeList trasladosNode = impuestosElement.getElementsByTagNameNS(cfdiNamespace, "Traslado");
                    if (trasladosNode.getLength() == 0) {
                        trasladosNode = impuestosElement.getElementsByTagName("Traslado");
                    }
                    
                    for (int j = 0; j < trasladosNode.getLength(); j++) {
                        Element trasladoElement = (Element) trasladosNode.item(j);
                        String impuesto = trasladoElement.getAttribute("Impuesto");
                        if ("002".equals(impuesto)) { // IVA
                            String importeIva = trasladoElement.getAttribute("Importe");
                            String tasa = trasladoElement.getAttribute("TasaOCuota");
                            if (importeIva != null && !importeIva.isEmpty()) {
                                try {
                                    ivaImporte = new BigDecimal(importeIva);
                                } catch (Exception e) {}
                            }
                            if (tasa != null && !tasa.isEmpty()) {
                                try {
                                    tasaOCuota = new BigDecimal(tasa);
                                } catch (Exception e) {}
                            }
                        }
                    }
                }
                
                concepto.put("iva", ivaImporte);
                concepto.put("tasaIva", tasaOCuota);
                
                conceptos.add(concepto);
            }
            
            logger.info("Conceptos extraídos del XML: {} renglones", conceptos.size());
            return conceptos;
            
        } catch (Exception e) {
            logger.warn("Error al extraer conceptos del XML: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrae los datos de nómina directamente del XML timbrado
     * Esto incluye todos los campos incluyendo datos laborales y correo electrónico
     */
    private Map<String, Object> extraerDatosNominaDesdeXml(String xmlContent) {
        try {
            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                return null;
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));
            
            Element root = document.getDocumentElement();
            
            // Buscar el namespace de CFDI
            String cfdiNamespace = root.getNamespaceURI();
            if (cfdiNamespace == null) {
                cfdiNamespace = "http://www.sat.gob.mx/cfd/4";
            }
            
            // Namespace de nómina
            String nominaNamespace = "http://www.sat.gob.mx/nomina12";
            
            Map<String, Object> datosNomina = new HashMap<>();
            
            // Extraer datos del Receptor CFDI (incluye DomicilioFiscalReceptor y UsoCFDI)
            Element receptorElement = obtenerPrimerElementoNS(document, cfdiNamespace, "Receptor");
            if (receptorElement == null) {
                receptorElement = obtenerPrimerElemento(document, "Receptor");
            }
            if (receptorElement != null) {
                String domicilioFiscalReceptor = receptorElement.getAttribute("DomicilioFiscalReceptor");
                if (domicilioFiscalReceptor != null && !domicilioFiscalReceptor.trim().isEmpty()) {
                    datosNomina.put("domicilioFiscalReceptor", domicilioFiscalReceptor.trim());
                }
                String usoCfdi = receptorElement.getAttribute("UsoCFDI");
                if (usoCfdi != null && !usoCfdi.trim().isEmpty()) {
                    datosNomina.put("usoCfdi", usoCfdi.trim());
                }
            }
            
            // Buscar nodo nomina12:Nomina
            Element nominaElement = obtenerPrimerElementoNS(document, nominaNamespace, "Nomina");
            if (nominaElement == null) {
                nominaElement = obtenerPrimerElemento(document, "Nomina");
            }
            
            if (nominaElement == null) {
                logger.info("No se encontró nodo Nomina en el XML");
                return datosNomina;
            }
            
            // Extraer atributos del nodo Nomina
            String tipoNomina = nominaElement.getAttribute("TipoNomina");
            String fechaPago = nominaElement.getAttribute("FechaPago");
            String fechaInicialPago = nominaElement.getAttribute("FechaInicialPago");
            String fechaFinalPago = nominaElement.getAttribute("FechaFinalPago");
            String numDiasPagados = nominaElement.getAttribute("NumDiasPagados");
            String totalPercepciones = nominaElement.getAttribute("TotalPercepciones");
            String totalDeducciones = nominaElement.getAttribute("TotalDeducciones");
            
            if (tipoNomina != null && !tipoNomina.isEmpty()) {
                datosNomina.put("tipoNomina", tipoNomina);
            }
            if (fechaPago != null && !fechaPago.isEmpty()) {
                datosNomina.put("fechaPago", fechaPago);
                // fechaNomina no está en el XML, usar fechaPago como valor por defecto
                datosNomina.put("fechaNomina", fechaPago);
            }
            if (fechaInicialPago != null && fechaFinalPago != null && !fechaInicialPago.isEmpty() && !fechaFinalPago.isEmpty()) {
                datosNomina.put("periodoPago", fechaInicialPago + " al " + fechaFinalPago);
            }
            if (totalPercepciones != null && !totalPercepciones.isEmpty()) {
                try {
                    datosNomina.put("percepciones", new BigDecimal(totalPercepciones));
                } catch (Exception e) {
                    logger.warn("Error al convertir TotalPercepciones a BigDecimal: {}", e.getMessage());
                }
            }
            if (totalDeducciones != null && !totalDeducciones.isEmpty()) {
                try {
                    datosNomina.put("deducciones", new BigDecimal(totalDeducciones));
                } catch (Exception e) {
                    logger.warn("Error al convertir TotalDeducciones a BigDecimal: {}", e.getMessage());
                }
            }
            
            // Buscar nodo nomina12:Receptor (contiene datos laborales)
            Element receptorNominaElement = obtenerPrimerElementoNS(nominaElement, nominaNamespace, "Receptor");
            if (receptorNominaElement == null) {
                receptorNominaElement = obtenerPrimerElemento(nominaElement, "Receptor");
            }
            
            if (receptorNominaElement != null) {
                String curp = receptorNominaElement.getAttribute("Curp");
                String numSeguridadSocial = receptorNominaElement.getAttribute("NumSeguridadSocial");
                String fechaInicioRelLaboral = receptorNominaElement.getAttribute("FechaInicioRelLaboral");
                String antiguedad = receptorNominaElement.getAttribute("Antigüedad");
                String riesgoPuesto = receptorNominaElement.getAttribute("RiesgoPuesto");
                String salarioDiarioIntegrado = receptorNominaElement.getAttribute("SalarioDiarioIntegrado");
                
                if (curp != null && !curp.isEmpty()) {
                    datosNomina.put("curp", curp);
                }
                if (numSeguridadSocial != null && !numSeguridadSocial.isEmpty()) {
                    datosNomina.put("numSeguridadSocial", numSeguridadSocial);
                }
                if (fechaInicioRelLaboral != null && !fechaInicioRelLaboral.isEmpty()) {
                    datosNomina.put("fechaInicioRelLaboral", fechaInicioRelLaboral);
                }
                if (antiguedad != null && !antiguedad.isEmpty()) {
                    datosNomina.put("antiguedad", antiguedad);
                }
                if (riesgoPuesto != null && !riesgoPuesto.isEmpty()) {
                    datosNomina.put("riesgoPuesto", riesgoPuesto);
                }
                if (salarioDiarioIntegrado != null && !salarioDiarioIntegrado.isEmpty()) {
                    datosNomina.put("salarioDiarioIntegrado", salarioDiarioIntegrado);
                }
            }
            
            // El nombre y RFC receptor se pueden obtener del nodo Receptor principal
            if (receptorElement != null) {
                String nombreReceptor = receptorElement.getAttribute("Nombre");
                String rfcReceptor = receptorElement.getAttribute("Rfc");
                if (nombreReceptor != null && !nombreReceptor.isEmpty()) {
                    datosNomina.put("nombre", nombreReceptor);
                    datosNomina.put("rfcReceptor", rfcReceptor);
                }
            }
            
            // El correo electrónico se puede obtener desde la factura si está disponible
            // (no está en el XML de nómina, pero podemos intentar obtenerlo desde otros campos)
            
            logger.info("Datos de nómina extraídos del XML: {} campos", datosNomina.size());
            return datosNomina;
            
        } catch (Exception e) {
            logger.warn("Error al extraer datos de nómina del XML: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Helper para obtener el primer elemento por namespace y localName
     */
    private Element obtenerPrimerElementoNS(org.w3c.dom.Node parent, String namespaceURI, String localName) {
        if (parent == null) return null;
        org.w3c.dom.NodeList nodeList;
        if (parent instanceof Document) {
            nodeList = ((Document) parent).getElementsByTagNameNS(namespaceURI, localName);
        } else if (parent instanceof Element) {
            nodeList = ((Element) parent).getElementsByTagNameNS(namespaceURI, localName);
        } else {
            return null;
        }
        return (nodeList != null && nodeList.getLength() > 0) ? (Element) nodeList.item(0) : null;
    }
    
    /**
     * Helper para obtener el primer elemento por localName (sin namespace)
     */
    private Element obtenerPrimerElemento(org.w3c.dom.Node parent, String localName) {
        if (parent == null) return null;
        org.w3c.dom.NodeList nodeList;
        if (parent instanceof Document) {
            nodeList = ((Document) parent).getElementsByTagName(localName);
        } else if (parent instanceof Element) {
            nodeList = ((Element) parent).getElementsByTagName(localName);
        } else {
            return null;
        }
        return (nodeList != null && nodeList.getLength() > 0) ? (Element) nodeList.item(0) : null;
    }
    
    /**
     * Genera un PDF de prueba con datos simulados
     */
    private byte[] generarPdfPrueba(String uuid, Map<String, Object> logoConfig) {
        try {
            logger.info("Generando PDF de prueba para UUID: {} con logoConfig", uuid);

            Map<String, Object> datosPrueba = new HashMap<>();
            datosPrueba.put("uuid", uuid);
            datosPrueba.put("serie", "A");
            datosPrueba.put("folio", "TEST");
            datosPrueba.put("fechaTimbrado", LocalDateTime.now());
            datosPrueba.put("rfcEmisor", "XAXX010101000");
            datosPrueba.put("nombreEmisor", "EMISOR DE PRUEBA");
            datosPrueba.put("rfcReceptor", "XEXX010101000");
            datosPrueba.put("nombreReceptor", "RECEPTOR DE PRUEBA");
            datosPrueba.put("subtotal", new BigDecimal("1000.00"));
            datosPrueba.put("iva", new BigDecimal("160.00"));
            datosPrueba.put("total", new BigDecimal("1160.00"));
            datosPrueba.put("tipoComprobante", "I");

            // Agregar sección de nómina de ejemplo para visualizar el layout cuando no exista factura real
            Map<String, Object> nominaEjemplo = new HashMap<>();
            nominaEjemplo.put("idEmpleado", "EMP001");
            nominaEjemplo.put("nombre", "Empleado Ejemplo");
            nominaEjemplo.put("curp", "CUEX010101HDFABC09");
            nominaEjemplo.put("periodoPago", "SEMANAL");
            nominaEjemplo.put("fechaPago", java.time.LocalDate.now().toString());
            nominaEjemplo.put("percepciones", new BigDecimal("1000.00"));
            nominaEjemplo.put("deducciones", new BigDecimal("0.00"));
            nominaEjemplo.put("tipoNomina", "O");
            datosPrueba.put("nomina", nominaEjemplo);

            byte[] pdfBytes = iTextPdfService.generarPdfConLogo(datosPrueba, logoConfig != null ? logoConfig : new HashMap<>());
            logger.info("PDF de prueba con logo generado exitosamente");
            return pdfBytes;
        } catch (Exception e) {
            logger.error("Error al generar PDF de prueba con logo: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    // Overload para compatibilidad con llamadas existentes
    private byte[] generarPdfPrueba(String uuid) {
        return generarPdfPrueba(uuid, null);
    }

    /**
     * Genera el PDF de un complemento de pago
     */
    private byte[] generarPdfComplementoPago(String uuid, Factura factura, Map<String, Object> logoConfig) {
        try {
            logger.info("Generando PDF para complemento de pago UUID: {}", uuid);
            
            // Obtener pagos del complemento desde la tabla PAGOS
            List<com.cibercom.facturacion_back.dao.PagoOracleDAO.PagoConsulta> pagosConsulta = new ArrayList<>();
            if (pagoOracleDAO != null) {
                try {
                    pagosConsulta = pagoOracleDAO.obtenerPagosPorUuidComplemento(uuid);
                    logger.info("Pagos encontrados para complemento {}: {}", uuid, pagosConsulta != null ? pagosConsulta.size() : 0);
                } catch (Exception e) {
                    logger.error("Error al obtener pagos del complemento {}: {}", uuid, e.getMessage(), e);
                    // Continuar con lista vacía y usar fallback
                }
            } else {
                logger.warn("PagoOracleDAO no está disponible. No se podrán obtener los pagos del complemento.");
            }
            
            // Construir datos del complemento en el formato esperado por ITextPdfService
            ITextPdfService.ComplementoPagoPdfData data = new ITextPdfService.ComplementoPagoPdfData();
            data.uuidComplemento = uuid != null ? uuid : "";
            data.serieComplemento = factura.getSerie() != null ? factura.getSerie() : "REP";
            data.folioComplemento = factura.getFolio() != null ? factura.getFolio() : "";
            
            // Manejar fechaTimbrado de forma segura
            try {
                if (factura.getFechaTimbrado() != null) {
                    data.fechaTimbrado = factura.getFechaTimbrado().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } else {
                    data.fechaTimbrado = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            } catch (Exception e) {
                logger.warn("Error al formatear fechaTimbrado, usando fecha actual: {}", e.getMessage());
                data.fechaTimbrado = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            
            data.rfcEmisor = factura.getEmisorRfc() != null && !factura.getEmisorRfc().trim().isEmpty() 
                ? factura.getEmisorRfc() : "AAA010101AAA";
            data.rfcReceptor = factura.getReceptorRfc() != null && !factura.getReceptorRfc().trim().isEmpty()
                ? factura.getReceptorRfc() : "XAXX010101000";
            data.nombreEmisor = factura.getEmisorRazonSocial() != null && !factura.getEmisorRazonSocial().trim().isEmpty()
                ? factura.getEmisorRazonSocial() : data.rfcEmisor;
            data.nombreReceptor = factura.getReceptorRazonSocial() != null && !factura.getReceptorRazonSocial().trim().isEmpty()
                ? factura.getReceptorRazonSocial() : data.rfcReceptor;
            data.metodoCfdi = "PPD";
            data.formaCfdi = "99";
            data.moneda = "MXN";
            data.cadenaOriginal = "";
            data.selloDigital = "";
            data.selloSat = "";
            
            // Obtener UUID de factura relacionada y total pagado desde los pagos
            BigDecimal totalPagado = BigDecimal.ZERO;
            String facturaUuid = null;
            List<ITextPdfService.ComplementoPagoPdfData.PagoDetalle> detalles = new ArrayList<>();
            
            if (pagosConsulta == null) {
                pagosConsulta = new ArrayList<>();
            }
            
            for (com.cibercom.facturacion_back.dao.PagoOracleDAO.PagoConsulta pago : pagosConsulta) {
                if (pago.fechaPago() != null && pago.monto() != null) {
                    ITextPdfService.ComplementoPagoPdfData.PagoDetalle detalle = 
                        new ITextPdfService.ComplementoPagoPdfData.PagoDetalle();
                    detalle.fechaPago = pago.fechaPago().format(java.time.format.DateTimeFormatter.ISO_DATE);
                    detalle.formaPago = pago.formaPago() != null ? pago.formaPago() : "99";
                    detalle.moneda = pago.moneda() != null ? pago.moneda() : "MXN";
                    detalle.monto = pago.monto().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
                    detalle.parcialidad = pago.parcialidad() != null ? pago.parcialidad() : 1;
                    detalle.saldoAnterior = detalle.monto; // Por ahora usar el monto como saldo anterior
                    detalle.importePagado = detalle.monto;
                    detalle.saldoInsoluto = "0.00";
                    detalle.uuidRelacionado = pago.relacionUuid() != null ? pago.relacionUuid() : "";
                    detalles.add(detalle);
                    
                    totalPagado = totalPagado.add(pago.monto());
                    if (facturaUuid == null && pago.relacionUuid() != null && !pago.relacionUuid().trim().isEmpty()) {
                        facturaUuid = pago.relacionUuid();
                    }
                }
            }
            
            data.facturaUuid = facturaUuid != null ? facturaUuid : "";
            data.totalPagado = totalPagado.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            data.pagos = detalles;
            
            // Si no hay pagos, usar datos de la factura como fallback
            if (detalles.isEmpty()) {
                logger.warn("No se encontraron pagos para el complemento {}. Usando datos de la factura.", uuid);
                ITextPdfService.ComplementoPagoPdfData.PagoDetalle detalleFallback = 
                    new ITextPdfService.ComplementoPagoPdfData.PagoDetalle();
                detalleFallback.fechaPago = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_DATE);
                detalleFallback.formaPago = "99";
                detalleFallback.moneda = "MXN";
                detalleFallback.monto = factura.getTotal() != null ? factura.getTotal().toPlainString() : "0.00";
                detalleFallback.parcialidad = 1;
                detalleFallback.saldoAnterior = detalleFallback.monto;
                detalleFallback.importePagado = detalleFallback.monto;
                detalleFallback.saldoInsoluto = "0.00";
                detalleFallback.uuidRelacionado = "";
                detalles.add(detalleFallback);
                data.totalPagado = factura.getTotal() != null ? factura.getTotal().toPlainString() : "0.00";
                data.pagos = detalles;
            }
            
            // Validar que hay datos suficientes para generar el PDF
            if (data.pagos == null || data.pagos.isEmpty()) {
                logger.error("No hay datos de pagos para generar PDF del complemento {}", uuid);
                throw new IllegalStateException("No hay datos de pagos disponibles para el complemento de pago");
            }
            
            // Generar PDF usando ITextPdfService
            try {
                byte[] pdfBytes = iTextPdfService.generarPdfComplementoPago(data, logoConfig != null ? logoConfig : new HashMap<>());
                if (pdfBytes == null || pdfBytes.length < 100) {
                    logger.error("PDF generado está vacío o es demasiado pequeño para complemento {}", uuid);
                    throw new IllegalStateException("PDF generado está vacío o inválido");
                }
                logger.info("PDF del complemento de pago generado exitosamente: {} bytes", pdfBytes.length);
                return pdfBytes;
            } catch (Exception e) {
                logger.error("Error al generar PDF usando ITextPdfService para complemento {}: {}", uuid, e.getMessage(), e);
                throw e; // Re-lanzar para que sea capturado por el catch exterior
            }
        } catch (IllegalStateException e) {
            logger.error("Estado inválido al generar PDF del complemento de pago {}: {}", uuid, e.getMessage(), e);
            // Fallback: generar PDF de prueba
            return generarPdfPrueba(uuid, logoConfig);
        } catch (Exception e) {
            logger.error("Error inesperado al generar PDF del complemento de pago {}: {}", uuid, e.getMessage(), e);
            // Fallback: generar PDF de prueba
            try {
                return generarPdfPrueba(uuid, logoConfig);
            } catch (Exception fallbackError) {
                logger.error("Error crítico: no se pudo generar ni PDF del complemento ni PDF de prueba: {}", fallbackError.getMessage(), fallbackError);
                // Lanzar excepción para que sea manejada por el método que llama
                throw new RuntimeException("Error crítico al generar PDF del complemento de pago: " + fallbackError.getMessage(), fallbackError);
            }
        }
    }

    /**
     * Genera el PDF de una retención de pagos
     */
    private byte[] generarPdfRetencion(String uuid, Factura factura, Map<String, Object> logoConfig) {
        try {
            logger.info("Generando PDF para retención de pagos UUID: {}", uuid);
            
            // Obtener datos de retención desde la tabla RETENCIONES
            com.cibercom.facturacion_back.dao.RetencionOracleDAO.RetencionConsulta retencionConsulta = null;
            if (retencionOracleDAO != null) {
                try {
                    Optional<com.cibercom.facturacion_back.dao.RetencionOracleDAO.RetencionConsulta> retencionOpt = 
                        retencionOracleDAO.obtenerRetencionPorUuid(uuid);
                    if (retencionOpt.isPresent()) {
                        retencionConsulta = retencionOpt.get();
                        logger.info("Retención encontrada para UUID {}: tipo={}, monto={}", 
                            uuid, retencionConsulta.tipoRetencion(), retencionConsulta.montoRetenido());
                    } else {
                        logger.warn("No se encontró retención en RETENCIONES para UUID {}", uuid);
                    }
                } catch (Exception e) {
                    logger.error("Error al obtener retención desde RETENCIONES para UUID {}: {}", uuid, e.getMessage(), e);
                }
            } else {
                logger.warn("RetencionOracleDAO no está disponible. No se podrán obtener los datos de retención.");
            }
            
            // Construir datos de la factura en el formato esperado por ITextPdfService
            Map<String, Object> datosFactura = new HashMap<>();
            datosFactura.put("uuid", uuid != null ? uuid : "");
            datosFactura.put("serie", factura.getSerie() != null ? factura.getSerie() : "RET");
            datosFactura.put("folio", factura.getFolio() != null ? factura.getFolio() : "");
            
            try {
                if (factura.getFechaTimbrado() != null) {
                    datosFactura.put("fechaTimbrado", factura.getFechaTimbrado().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } else {
                    datosFactura.put("fechaTimbrado", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
            } catch (Exception e) {
                logger.warn("Error al formatear fechaTimbrado, usando fecha actual: {}", e.getMessage());
                datosFactura.put("fechaTimbrado", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
            datosFactura.put("rfcEmisor", factura.getEmisorRfc() != null && !factura.getEmisorRfc().trim().isEmpty() 
                ? factura.getEmisorRfc() : "AAA010101AAA");
            datosFactura.put("rfcReceptor", factura.getReceptorRfc() != null && !factura.getReceptorRfc().trim().isEmpty()
                ? factura.getReceptorRfc() : "XAXX010101000");
            datosFactura.put("nombreEmisor", factura.getEmisorRazonSocial() != null && !factura.getEmisorRazonSocial().trim().isEmpty()
                ? factura.getEmisorRazonSocial() : datosFactura.get("rfcEmisor"));
            datosFactura.put("nombreReceptor", factura.getReceptorRazonSocial() != null && !factura.getReceptorRazonSocial().trim().isEmpty()
                ? factura.getReceptorRazonSocial() : datosFactura.get("rfcReceptor"));
            datosFactura.put("tipoComprobante", "R"); // R para Retención
            datosFactura.put("moneda", "MXN");
            
            // Construir conceptos con la información de retención (versión simplificada y profesional)
            List<Map<String, Object>> conceptos = new ArrayList<>();
            
            if (retencionConsulta != null) {
                // Construir descripción del tipo de retención
                String tipoRetencionDesc = obtenerDescripcionTipoRetencion(retencionConsulta.tipoRetencion());
                BigDecimal baseRetencion = retencionConsulta.baseRetencion() != null ? retencionConsulta.baseRetencion() : java.math.BigDecimal.ZERO;
                BigDecimal montoRetenido = retencionConsulta.montoRetenido() != null ? retencionConsulta.montoRetenido() : java.math.BigDecimal.ZERO;
                
                // Determinar qué impuestos aplican según el tipo de retención
                boolean aplicaIsr = retencionConsulta.tipoRetencion() != null && 
                                   (retencionConsulta.tipoRetencion().startsWith("ISR_") || 
                                    retencionConsulta.impuesto() != null && retencionConsulta.impuesto().contains("ISR"));
                boolean aplicaIva = retencionConsulta.tipoRetencion() != null && 
                                   (retencionConsulta.tipoRetencion().equals("IVA") ||
                                    (aplicaIsr && (retencionConsulta.tipoRetencion().equals("ISR_SERVICIOS") || 
                                                   retencionConsulta.tipoRetencion().equals("ISR_ARRENDAMIENTO"))));
                
                // Construir descripción profesional del concepto
                StringBuilder descripcionConcepto = new StringBuilder();
                if (aplicaIsr && aplicaIva) {
                    descripcionConcepto.append("Retención de ISR e IVA - ").append(tipoRetencionDesc);
                } else if (aplicaIsr) {
                    descripcionConcepto.append("Retención de ISR - ").append(tipoRetencionDesc);
                } else if (aplicaIva) {
                    descripcionConcepto.append("Retención de IVA - ").append(tipoRetencionDesc);
                } else {
                    descripcionConcepto.append("Retención de Pagos - ").append(tipoRetencionDesc);
                }
                
                // Concepto único y profesional
                Map<String, Object> concepto = new HashMap<>();
                concepto.put("cantidad", java.math.BigDecimal.ONE);
                concepto.put("descripcion", descripcionConcepto.toString());
                concepto.put("valorUnitario", baseRetencion);
                concepto.put("importe", baseRetencion);
                concepto.put("iva", java.math.BigDecimal.ZERO);
                conceptos.add(concepto);
                
            } else {
                // Fallback: usar datos de la factura
                Map<String, Object> conceptoFallback = new HashMap<>();
                conceptoFallback.put("cantidad", java.math.BigDecimal.ONE);
                conceptoFallback.put("descripcion", "CFDI de Retención de Pagos");
                conceptoFallback.put("valorUnitario", factura.getSubtotal() != null ? factura.getSubtotal() : java.math.BigDecimal.ZERO);
                conceptoFallback.put("importe", factura.getSubtotal() != null ? factura.getSubtotal() : java.math.BigDecimal.ZERO);
                conceptoFallback.put("iva", java.math.BigDecimal.ZERO);
                conceptos.add(conceptoFallback);
            }
            
            datosFactura.put("conceptos", conceptos);
            
            // Calcular totales para retención
            BigDecimal baseRetencionCalc = java.math.BigDecimal.ZERO;
            BigDecimal montoRetenidoCalc = java.math.BigDecimal.ZERO;
            
            if (retencionConsulta != null) {
                baseRetencionCalc = retencionConsulta.baseRetencion() != null ? retencionConsulta.baseRetencion() : java.math.BigDecimal.ZERO;
                montoRetenidoCalc = retencionConsulta.montoRetenido() != null ? retencionConsulta.montoRetenido() : java.math.BigDecimal.ZERO;
            } else {
                baseRetencionCalc = factura.getSubtotal() != null ? factura.getSubtotal() : java.math.BigDecimal.ZERO;
                montoRetenidoCalc = factura.getTotal() != null ? factura.getTotal() : java.math.BigDecimal.ZERO;
            }
            
            // Para retenciones: Subtotal = monto base, Total = monto retenido
            BigDecimal subtotal = baseRetencionCalc;
            BigDecimal total = montoRetenidoCalc;
            
            datosFactura.put("subtotal", subtotal);
            datosFactura.put("iva", java.math.BigDecimal.ZERO); // Las retenciones no tienen IVA adicional
            datosFactura.put("total", total);
            
            // Construir datosRetencion estructurado para el diseño específico (igual que generarPdfPreview)
            Map<String, Object> datosRetencion = new HashMap<>();
            if (retencionConsulta != null) {
                String tipoRetencion = retencionConsulta.tipoRetencion() != null ? retencionConsulta.tipoRetencion() : null;
                String claveRetencion = retencionConsulta.claveRetencion() != null ? retencionConsulta.claveRetencion() : null;
                
                // Determinar ISR e IVA retenidos basándose en el campo impuesto
                java.math.BigDecimal isrRetenido = java.math.BigDecimal.ZERO;
                java.math.BigDecimal ivaRetenido = java.math.BigDecimal.ZERO;
                String impuesto = retencionConsulta.impuesto() != null ? retencionConsulta.impuesto() : "";
                if (impuesto.contains("ISR") || impuesto.contains("001")) {
                    isrRetenido = montoRetenidoCalc; // Si el impuesto es ISR, todo el monto retenido es ISR
                } else if (impuesto.contains("IVA") || impuesto.contains("002")) {
                    ivaRetenido = montoRetenidoCalc; // Si el impuesto es IVA, todo el monto retenido es IVA
                }
                
                // Formatear período
                String periodoMes = null;
                if (retencionConsulta.periodoMesIni() != null) {
                    periodoMes = String.format("%02d", retencionConsulta.periodoMesIni());
                }
                String periodoAnio = null;
                if (retencionConsulta.periodoEjercicio() != null) {
                    periodoAnio = retencionConsulta.periodoEjercicio().toString();
                }
                
                // Formatear fecha de pago
                String fechaPago = null;
                if (retencionConsulta.fechaRetencion() != null) {
                    fechaPago = retencionConsulta.fechaRetencion().toString();
                }
                
                datosRetencion.put("tipoRetencion", tipoRetencion);
                datosRetencion.put("cveRetenc", claveRetencion);
                datosRetencion.put("descripcionTipo", tipoRetencion != null ? obtenerDescripcionTipoRetencion(tipoRetencion) : "");
                datosRetencion.put("concepto", retencionConsulta.comentarios() != null ? retencionConsulta.comentarios() : "");
                datosRetencion.put("montoBase", baseRetencionCalc);
                // Estos campos no están disponibles en RetencionConsulta, usar valores por defecto
                datosRetencion.put("montoTotGravado", baseRetencionCalc); // Asumir que todo es gravado
                datosRetencion.put("montoTotExento", java.math.BigDecimal.ZERO);
                datosRetencion.put("periodoMes", periodoMes);
                datosRetencion.put("periodoAnio", periodoAnio);
                datosRetencion.put("fechaPago", fechaPago);
                datosRetencion.put("isrRetenido", isrRetenido);
                datosRetencion.put("ivaRetenido", ivaRetenido);
                datosRetencion.put("montoRetenido", montoRetenidoCalc);
                // Nota: impRetenidos no está disponible en RetencionConsulta, se puede construir si es necesario
            } else {
                // Fallback con datos mínimos
                datosRetencion.put("tipoRetencion", null);
                datosRetencion.put("cveRetenc", null);
                datosRetencion.put("descripcionTipo", "");
                datosRetencion.put("concepto", "");
                datosRetencion.put("montoBase", baseRetencionCalc);
                datosRetencion.put("montoTotGravado", baseRetencionCalc);
                datosRetencion.put("montoTotExento", java.math.BigDecimal.ZERO);
                datosRetencion.put("periodoMes", null);
                datosRetencion.put("periodoAnio", null);
                datosRetencion.put("fechaPago", null);
                datosRetencion.put("isrRetenido", java.math.BigDecimal.ZERO);
                datosRetencion.put("ivaRetenido", java.math.BigDecimal.ZERO);
                datosRetencion.put("montoRetenido", montoRetenidoCalc);
            }
            datosFactura.put("datosRetencion", datosRetencion);
            
            // Generar PDF usando ITextPdfService
            try {
                byte[] pdfBytes = iTextPdfService.generarPdfConLogo(datosFactura, logoConfig != null ? logoConfig : new HashMap<>());
                if (pdfBytes == null || pdfBytes.length < 100) {
                    logger.error("PDF generado está vacío o es demasiado pequeño para retención {}", uuid);
                    throw new IllegalStateException("PDF generado está vacío o inválido");
                }
                logger.info("PDF de la retención generado exitosamente: {} bytes", pdfBytes.length);
                return pdfBytes;
            } catch (Exception e) {
                logger.error("Error al generar PDF usando ITextPdfService para retención {}: {}", uuid, e.getMessage(), e);
                throw e;
            }
        } catch (IllegalStateException e) {
            logger.error("Estado inválido al generar PDF de la retención {}: {}", uuid, e.getMessage(), e);
            return generarPdfPrueba(uuid, logoConfig);
        } catch (Exception e) {
            logger.error("Error inesperado al generar PDF de la retención {}: {}", uuid, e.getMessage(), e);
            try {
                return generarPdfPrueba(uuid, logoConfig);
            } catch (Exception fallbackError) {
                logger.error("Error crítico: no se pudo generar ni PDF de la retención ni PDF de prueba: {}", fallbackError.getMessage(), fallbackError);
                throw new RuntimeException("Error crítico al generar PDF de la retención: " + fallbackError.getMessage(), fallbackError);
            }
        }
    }
    
    private String obtenerDescripcionTipoRetencion(String tipoRetencion) {
        if (tipoRetencion == null || tipoRetencion.isBlank()) {
            return "N/A";
        }
        
        Map<String, String> descripciones = new HashMap<>();
        descripciones.put("ISR_SERVICIOS", "ISR - Servicios profesionales (honorarios)");
        descripciones.put("ISR_ARRENDAMIENTO", "ISR - Arrendamiento");
        descripciones.put("ISR_ENAJENACION", "ISR - Enajenación de bienes");
        descripciones.put("ISR_REGALIAS", "ISR - Regalías");
        descripciones.put("ISR_SUELDOS", "ISR - Sueldos y salarios");
        descripciones.put("IVA", "IVA - Retención de IVA");
        descripciones.put("DIVIDENDOS", "Pagos - Dividendos o utilidades distribuidas");
        descripciones.put("INTERESES", "Pagos - Intereses");
        descripciones.put("FIDEICOMISOS", "Pagos - Fideicomisos");
        descripciones.put("REMANENTE", "Pagos - Remanente distribuible");
        descripciones.put("PLANES_RETIRO", "Pagos - Planes de retiro");
        descripciones.put("ENAJENACION_ACCIONES", "Pagos - Enajenación de acciones");
        descripciones.put("OTROS", "Otros ingresos regulados");
        
        return descripciones.getOrDefault(tipoRetencion.trim(), tipoRetencion);
    }
    

    private String mapearTipoComprobante(Integer tipoFactura) {
        if (tipoFactura == null) {
            return "I";
        }
        return switch (tipoFactura) {
            case 2 -> "E";
            case 3 -> "T";
            case 4 -> "N";
            case 5 -> "P";
            case 6 -> "R"; // R para Retención
            default -> "I";
        };
    }

    /**
     * Genera el folio consecutivo por serie desde el sistema (Oracle)
     */
    private String generarFolioConsecutivo(String serie) {
        Integer max = facturaRepository.findMaxFolioBySerie(serie);
        int next = (max == null ? 0 : max) + 1;
        return String.valueOf(next);
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
     * Escapa caracteres especiales XML
     */
    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Genera un PDF de vista previa sin timbrar a partir de FacturaFrontendRequest
     */
    public byte[] generarPdfPreview(FacturaFrontendRequest request, Map<String, Object> logoConfig) {
        try {
            logger.info("Generando PDF de vista previa para RFC receptor: {}", request.getRfc());

            // Calcular totales desde conceptos si están disponibles (modo manual)
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal iva = BigDecimal.ZERO;
            BigDecimal total = BigDecimal.ZERO;
            
            List<Map<String, Object>> conceptos = new ArrayList<>();

            // Si hay conceptos en el request (modo manual), calcular desde ellos
            if (request.getConceptos() != null && !request.getConceptos().isEmpty()) {
                logger.info("Calculando totales desde conceptos del request (modo manual)");
                for (FacturaFrontendRequest.Concepto concepto : request.getConceptos()) {
                    BigDecimal cantidad = concepto.getCantidad() != null ? concepto.getCantidad() : BigDecimal.ONE;
                    BigDecimal precioUnitario = concepto.getPrecioUnitario() != null ? concepto.getPrecioUnitario() : BigDecimal.ZERO;
                    BigDecimal importe = concepto.getImporte() != null ? concepto.getImporte() : precioUnitario.multiply(cantidad);
                    BigDecimal tasaIva = concepto.getTasaIva() != null ? concepto.getTasaIva() : BigDecimal.ZERO;
                    BigDecimal ivaConcepto = importe.multiply(tasaIva);
                    
                    subtotal = subtotal.add(importe);
                    iva = iva.add(ivaConcepto);
                    
                    // Construir mapa del concepto para el PDF
                    Map<String, Object> c = new HashMap<>();
                    c.put("cantidad", cantidad);
                    c.put("descripcion", concepto.getDescripcion() != null ? concepto.getDescripcion() : "Producto/Servicio");
                    c.put("unidad", concepto.getUnidad() != null ? concepto.getUnidad() : "");
                    c.put("claveUnidad", concepto.getClaveUnidad() != null ? concepto.getClaveUnidad() : "");
                    c.put("claveProdServ", concepto.getClaveProdServ() != null ? concepto.getClaveProdServ() : "");
                    c.put("valorUnitario", precioUnitario);
                    c.put("importe", importe);
                    c.put("iva", ivaConcepto);
                    c.put("tasaIva", tasaIva);
                    c.put("objetoImp", concepto.getObjetoImp() != null ? concepto.getObjetoImp() : "02");
                    conceptos.add(c);
                }
                total = subtotal.add(iva);
            } else {
                // Modo automático: intentar obtener desde ticket
                try {
                    if (ticketService != null && request.getTienda() != null && request.getBoleta() != null) {
                        TicketSearchRequest tsr = new TicketSearchRequest();
                        tsr.setCodigoTienda(request.getTienda());
                        tsr.setTerminalId(toInt(request.getTerminal()));
                        tsr.setFecha(request.getFecha() != null ? request.getFecha().toString() : null);
                        tsr.setFolio(toInt(request.getBoleta()));

                        List<TicketDto> tickets = ticketService.buscarTickets(tsr);
                        if (tickets != null && !tickets.isEmpty()) {
                            TicketDto t = tickets.get(0);
                            if (t.getSubtotal() != null) subtotal = t.getSubtotal();
                            if (t.getIva() != null) {
                                iva = t.getIva();
                            } else {
                                iva = subtotal.multiply(new BigDecimal("0.16"));
                            }
                            if (t.getTotal() != null) {
                                total = t.getTotal();
                            } else {
                                total = subtotal.add(iva);
                            }
                            
                            // Obtener detalles del ticket
                            if (ticketDetalleService != null && t.getIdTicket() != null) {
                                List<com.cibercom.facturacion_back.dto.TicketDetalleDto> detalles = 
                                    ticketDetalleService.buscarDetallesPorIdTicket(t.getIdTicket());
                                if (detalles != null && !detalles.isEmpty()) {
                                    for (com.cibercom.facturacion_back.dto.TicketDetalleDto d : detalles) {
                                        Map<String, Object> c = new HashMap<>();
                                        c.put("cantidad", d.getCantidad() != null ? d.getCantidad() : BigDecimal.ONE);
                                        c.put("descripcion", d.getDescripcion() != null ? d.getDescripcion() : "Producto");
                                        String unidad = (d.getUnidadCatalogo() != null && !d.getUnidadCatalogo().trim().isEmpty()) 
                                                ? d.getUnidadCatalogo() : d.getUnidad();
                                        c.put("unidad", unidad);
                                        if (d.getClaveUnidad() != null && !d.getClaveUnidad().trim().isEmpty()) {
                                            c.put("claveUnidad", d.getClaveUnidad());
                                        }
                                        c.put("valorUnitario", d.getPrecioUnitario() != null ? d.getPrecioUnitario() : BigDecimal.ZERO);
                                        c.put("importe", d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO);
                                        c.put("iva", d.getIvaImporte() != null ? d.getIvaImporte() : BigDecimal.ZERO);
                                        if (d.getClaveProdServ() != null && !d.getClaveProdServ().trim().isEmpty()) {
                                            c.put("claveProdServ", d.getClaveProdServ());
                                        }
                                        if (d.getTasaIva() != null) {
                                            c.put("tasaIva", d.getTasaIva());
                                        } else if (d.getIvaPorcentaje() != null) {
                                            c.put("tasaIva", d.getIvaPorcentaje());
                                        }
                                        if (d.getObjetoImpuesto() != null && !d.getObjetoImpuesto().trim().isEmpty()) {
                                            c.put("objetoImp", d.getObjetoImpuesto());
                                        }
                                        conceptos.add(c);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Fallo al buscar ticket para totales en preview: {}", e.getMessage());
                }
            }

            // Si no hay conceptos, agregar uno genérico
            if (conceptos.isEmpty()) {
                subtotal = new BigDecimal("1000.00"); // Valor por defecto
                iva = subtotal.multiply(new BigDecimal("0.16"));
                total = subtotal.add(iva);
                
                Map<String, Object> concepto = new HashMap<>();
                concepto.put("cantidad", BigDecimal.ONE);
                concepto.put("descripcion", "Producto/Servicio");
                concepto.put("valorUnitario", subtotal);
                concepto.put("importe", subtotal);
                concepto.put("iva", iva);
                concepto.put("tasaIva", new BigDecimal("0.16"));
                conceptos.add(concepto);
            }

            // Construir datos de factura para el PDF
            Map<String, Object> datosFactura = new HashMap<>();
            datosFactura.put("uuid", "PREVIEW-" + java.util.UUID.randomUUID().toString());
            datosFactura.put("serie", "A");
            datosFactura.put("folio", "PREVIEW");
            datosFactura.put("fechaTimbrado", LocalDateTime.now());
            datosFactura.put("rfcEmisor", rfcEmisorDefault);
            datosFactura.put("nombreEmisor", nombreEmisorDefault);
            datosFactura.put("rfcReceptor", request.getRfc());
            
            // Determinar nombre del receptor
            String nombreReceptor = "";
            if (request.getRfc() != null && request.getRfc().length() == 12) {
                // Persona moral
                nombreReceptor = request.getRazonSocial() != null ? request.getRazonSocial() : "";
            } else {
                // Persona física
                StringBuilder nombreBuilder = new StringBuilder();
                if (request.getNombre() != null) nombreBuilder.append(request.getNombre());
                if (request.getPaterno() != null) nombreBuilder.append(" ").append(request.getPaterno());
                if (request.getMaterno() != null) nombreBuilder.append(" ").append(request.getMaterno());
                nombreReceptor = nombreBuilder.toString().trim();
            }
            datosFactura.put("nombreReceptor", nombreReceptor);
            datosFactura.put("tipoComprobante", "I"); // Ingreso
            datosFactura.put("subtotal", subtotal);
            datosFactura.put("iva", iva);
            datosFactura.put("total", total);
            datosFactura.put("conceptos", conceptos);
            if (request.getTipoDocumento() != null && !request.getTipoDocumento().trim().isEmpty()) {
                datosFactura.put("tipoDocumento", request.getTipoDocumento().trim());
            }
            if (request.getUuidCfdiRelacionado() != null && !request.getUuidCfdiRelacionado().trim().isEmpty()) {
                datosFactura.put("uuidCfdiRelacionado", request.getUuidCfdiRelacionado().trim());
            }

            // Generar PDF
            byte[] pdfBytes = iTextPdfService.generarPdfConLogo(datosFactura, logoConfig != null ? logoConfig : new HashMap<>());
            logger.info("PDF de vista previa generado exitosamente: {} bytes", pdfBytes != null ? pdfBytes.length : 0);
            return pdfBytes;
        } catch (Exception e) {
            logger.error("Error al generar PDF de vista previa: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF de vista previa: " + e.getMessage(), e);
        }
    }
}