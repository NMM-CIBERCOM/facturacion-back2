package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.ConceptoOracleDAO;
import com.cibercom.facturacion_back.dao.PagoOracleDAO;
import com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO;
import com.cibercom.facturacion_back.dto.PacTimbradoRequest;
import com.cibercom.facturacion_back.dto.PacTimbradoResponse;
import com.cibercom.facturacion_back.dto.PagoComplementoEnvioRequest;
import com.cibercom.facturacion_back.dto.PagoComplementoRequest;
import com.cibercom.facturacion_back.dto.PagoComplementoResponse;
import com.cibercom.facturacion_back.dto.PagoDetalleRequest;
import com.cibercom.facturacion_back.integration.PacClient;
import com.cibercom.facturacion_back.model.ClienteCatalogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("oracle")
public class PagoService {

    private static final Logger logger = LoggerFactory.getLogger(PagoService.class);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final java.util.Map<String, String> FORMA_PAGO_DESCRIPCION = crearCatalogoFormaPago();

    private final ConceptoOracleDAO conceptoOracleDAO;
    private final PagoOracleDAO pagoOracleDAO;
    private final UuidFacturaOracleDAO uuidFacturaOracleDAO;
    private final ClienteCatalogoService clienteCatalogoService;
    private final PacClient pacClient;
    private final ITextPdfService iTextPdfService;
    private final CorreoService correoService;
    private final FormatoCorreoService formatoCorreoService;
    private final Environment environment;
    private final CfdiFirmaService cfdiFirmaService;

    // Inyectar valores de application.yml usando @Value (igual que FacturaService)
    @Value("${facturacion.emisor.rfc:IVD920810GU2}")
    private String rfcEmisorDefault;
    
    @Value("${facturacion.emisor.nombre:INNOVACION VALOR Y DESARROLLO SA}")
    private String nombreEmisorDefault;
    
    @Value("${facturacion.emisor.regimen:601}")
    private String regimenFiscalEmisorDefault;
    
    @Value("${facturacion.emisor.cp:58000}")
    private String codigoPostalEmisorDefault;

    public PagoService(ConceptoOracleDAO conceptoOracleDAO,
                       PagoOracleDAO pagoOracleDAO,
                       UuidFacturaOracleDAO uuidFacturaOracleDAO,
                       ClienteCatalogoService clienteCatalogoService,
                       PacClient pacClient,
                       ITextPdfService iTextPdfService,
                       CorreoService correoService,
                       FormatoCorreoService formatoCorreoService,
                       Environment environment,
                       org.springframework.beans.factory.ObjectProvider<CfdiFirmaService> cfdiFirmaServiceProvider) {
        this.conceptoOracleDAO = conceptoOracleDAO;
        this.pagoOracleDAO = pagoOracleDAO;
        this.uuidFacturaOracleDAO = uuidFacturaOracleDAO;
        this.clienteCatalogoService = clienteCatalogoService;
        this.pacClient = pacClient;
        this.iTextPdfService = iTextPdfService;
        this.correoService = correoService;
        this.formatoCorreoService = formatoCorreoService;
        this.environment = environment;
        this.cfdiFirmaService = cfdiFirmaServiceProvider.getIfAvailable();
    }

    public Optional<Long> buscarFacturaIdPorUuid(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return Optional.empty();
        }
        if (conceptoOracleDAO == null) {
            return Optional.empty();
        }
        return conceptoOracleDAO.obtenerIdFacturaPorUuid(uuid.trim());
    }

    public PagoComplementoResponse registrarComplemento(PagoComplementoRequest request) {
        PagoComplementoResponse response = new PagoComplementoResponse();
        List<String> errores = new ArrayList<>();
        response.setErrors(errores);

        if (request == null) {
            errores.add("Solicitud vacía.");
            response.setSuccess(false);
            response.setMessage("Solicitud inválida.");
            return response;
        }

        String facturaUuid = safeTrim(request.getFacturaUuid());
        if (facturaUuid == null) {
            errores.add("El UUID de la factura original es obligatorio.");
            response.setSuccess(false);
            response.setMessage("Datos incompletos.");
            return response;
        }

        String correoReceptor = safeTrim(request.getCorreoReceptor());
        if (correoReceptor == null || !correoReceptor.contains("@")) {
            errores.add("El correo del receptor es obligatorio y debe tener un formato válido.");
            response.setSuccess(false);
            response.setMessage("Correo del receptor inválido.");
            return response;
        }

        List<PagoDetalleRequest> pagos = request.getPagos();
        if (pagos == null || pagos.isEmpty()) {
            errores.add("Debe proporcionar al menos un pago.");
            response.setSuccess(false);
            response.setMessage("Sin pagos para registrar.");
            return response;
        }

        Optional<UuidFacturaOracleDAO.Result> facturaOriginalOpt = uuidFacturaOracleDAO.obtenerBasicosPorUuid(facturaUuid);
        if (facturaOriginalOpt.isEmpty()) {
            errores.add("No se encontró información de la factura original en Oracle.");
            response.setSuccess(false);
            response.setMessage("Factura original inexistente.");
            return response;
        }
        UuidFacturaOracleDAO.Result facturaOriginal = facturaOriginalOpt.get();

        Long facturaId = buscarFacturaIdPorUuid(facturaUuid).orElse(request.getFacturaId());
        if (facturaId == null) {
            errores.add("No se encontró FACTURA_ID para el UUID proporcionado.");
            response.setSuccess(false);
            response.setMessage("No se pudo resolver la factura destino.");
            return response;
        }
        response.setFacturaId(facturaId);

        Long usuarioRegistro = parseUsuario(request.getUsuarioRegistro());

        BigDecimal totalPagos = pagos.stream()
                .map(PagoDetalleRequest::getMonto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String serieComplemento = "REP";
        String folioComplemento = generarFolioComplemento();
        String usoCfdi = "CP01";
        // FormaPago: obtener del primer pago o usar "99" (Por definir) como default
        String formaCfdi = "99";
        if (pagos != null && !pagos.isEmpty()) {
            String formaPagoPrimerPago = safeTrim(pagos.get(0).getFormaPago());
            if (formaPagoPrimerPago != null && !formaPagoPrimerPago.isEmpty()) {
                formaCfdi = formaPagoPrimerPago;
            }
        }
        // MetodoPago: usar "PPD" (Pago en parcialidades o diferido) para complementos de pago
        String metodoCfdi = "PPD";

        String uuidComplemento = UUID.randomUUID().toString().toUpperCase();

        // Obtener RFC del emisor: primero del certificado, luego de application.yml, luego de la factura original
        String rfcEmisor = null;
        try {
            if (cfdiFirmaService != null) {
                java.security.cert.X509Certificate certificado = cfdiFirmaService.cargarCertificado();
                if (certificado != null) {
                    String subjectDN = certificado.getSubjectX500Principal().getName();
                    // Extraer RFC del SubjectDN (formato: CN=XXXX123456XXX, O=..., etc.)
                    String rfcCertificado = extraerRFCDeSubjectDN(subjectDN);
                    if (rfcCertificado != null && !rfcCertificado.trim().isEmpty()) {
                        rfcEmisor = rfcCertificado.trim();
                        logger.info("✓ RFC del emisor obtenido del certificado: {}", rfcEmisor);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("No se pudo obtener RFC del certificado: {}", e.getMessage());
        }
        
        // Si no se obtuvo del certificado, usar el de application.yml o de la factura original
        if (rfcEmisor == null || rfcEmisor.isEmpty()) {
            rfcEmisor = defaultString(facturaOriginal.rfcEmisor, rfcEmisorDefault);
            logger.info("✓ RFC del emisor obtenido de configuración/factura: {}", rfcEmisor);
        }
        
        String rfcReceptor = defaultString(facturaOriginal.rfcReceptor, "XAXX010101000");

        // Extraer datos del receptor desde el XML original
        CfdiDatosBasicos datosCfdi = extraerDatosBasicosDesdeXml(facturaOriginal.xmlContent);
        String nombreReceptorReal = datosCfdi != null ? datosCfdi.receptorNombre : null;
        String regimenReceptorReal = datosCfdi != null ? datosCfdi.receptorRegimen : null;
        String domicilioReceptorReal = datosCfdi != null ? datosCfdi.receptorDomicilio : null;
        
        // Extraer moneda de la factura original del XML
        String monedaFacturaOriginal = extraerMonedaDesdeXml(facturaOriginal.xmlContent);
        if (monedaFacturaOriginal == null || monedaFacturaOriginal.isEmpty()) {
            // Si no se encuentra, asumir MXN (moneda nacional por defecto)
            monedaFacturaOriginal = "MXN";
        }
        logger.info("Moneda de la factura original: {}", monedaFacturaOriginal);

        // CRÍTICO: Usar nombre del emisor desde @Value (application.yml) - igual que FacturaService
        // El nombre debe coincidir EXACTAMENTE con el registrado en Finkok (demo) o SAT (producción)
        String nombreEmisorFinal = nombreEmisorDefault != null && !nombreEmisorDefault.trim().isEmpty() 
                ? nombreEmisorDefault.trim() 
                : defaultString(facturaOriginal.rfcEmisor, rfcEmisor);
        String regimenEmisorFinal = regimenFiscalEmisorDefault != null && !regimenFiscalEmisorDefault.trim().isEmpty()
                ? regimenFiscalEmisorDefault.trim()
                : "601";

        String nombreReceptorFinal = defaultString(nombreReceptorReal, defaultString(facturaOriginal.rfcReceptor, "XAXX010101000"));
        String regimenReceptorFinal = defaultString(regimenReceptorReal, "601");
        String domicilioReceptorFinal = defaultString(domicilioReceptorReal, "00000");
        
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("📋 DATOS DEL EMISOR PARA COMPLEMENTO DE PAGO:");
        logger.info("  RFC: {}", rfcEmisor);
        logger.info("  Nombre: '{}' (desde application.yml)", nombreEmisorFinal);
        logger.info("  Régimen Fiscal: {}", regimenEmisorFinal);
        logger.info("═══════════════════════════════════════════════════════════════");

        // Construir XML del complemento de pago según XSD Pagos20.xsd.xml
        String xmlComplemento = construirXmlComplementoPago(
                uuidComplemento,
                serieComplemento,
                folioComplemento,
                rfcEmisor,
                rfcReceptor,
                nombreEmisorFinal,
                regimenEmisorFinal,
                nombreReceptorFinal,
                regimenReceptorFinal,
                domicilioReceptorFinal,
                correoReceptor,
                usoCfdi,
                formaCfdi,
                metodoCfdi,
                totalPagos,
                facturaOriginal.total,
                facturaUuid,
                monedaFacturaOriginal,
                pagos,
                null,
                null
        );
        
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("📋 XML COMPLEMENTO DE PAGO GENERADO (antes de firmar)");
        logger.info("  UUID Complemento: {}", uuidComplemento);
        logger.info("  Serie: {}, Folio: {}", serieComplemento, folioComplemento);
        logger.info("  Total Pagos: {}", totalPagos);
        logger.info("  UUID Factura Relacionada: {}", facturaUuid);
        logger.info("═══════════════════════════════════════════════════════════════");

        // Preparar request para timbrado con Finkok
        // El PacClient se encargará de firmar el XML antes de enviarlo a Finkok
        PacTimbradoRequest pacRequest = PacTimbradoRequest.builder()
                .xmlContent(xmlComplemento)
                .rfcEmisor(defaultString(facturaOriginal.rfcEmisor, "AAA010101AAA"))
                .rfcReceptor(defaultString(facturaOriginal.rfcReceptor, "XAXX010101000"))
                .total(totalPagos.setScale(2, RoundingMode.HALF_UP).doubleValue())
                .tipo("PAGO")
                .fechaFactura(LocalDateTime.now().toString())
                .publicoGeneral(false)
                .serie(serieComplemento)
                .folio(folioComplemento)
                .medioPago(metodoCfdi)
                .formaPago(null) // No incluir FormaPago en el Comprobante principal para TipoDeComprobante="P"
                .usoCFDI(usoCfdi)
                .relacionadosUuids(facturaUuid)
                .build();

        logger.info("Enviando complemento de pago a Finkok para timbrado...");
        PacTimbradoResponse pacResp = pacClient.solicitarTimbrado(pacRequest);
        if (pacResp == null || Boolean.FALSE.equals(pacResp.getOk())) {
            errores.add(pacResp != null && pacResp.getMessage() != null
                    ? pacResp.getMessage()
                    : "PAC no disponible para timbrado.");
            response.setSuccess(false);
            response.setMessage("Error al timbrar complemento.");
            return response;
        }

        String uuidComplementoPac = safeTrim(pacResp.getUuid());
        if (uuidComplementoPac != null) {
            uuidComplemento = uuidComplementoPac;
        }
        response.setUuidComplemento(uuidComplemento);

        LocalDateTime fechaTimbrado = pacResp != null && pacResp.getFechaTimbrado() != null
                ? pacResp.getFechaTimbrado()
                : LocalDateTime.now();

        String xmlTimbrado = pacResp.getXmlTimbrado();
        if (xmlTimbrado == null || xmlTimbrado.isBlank()) {
            xmlTimbrado = construirXmlComplementoPago(
                    uuidComplemento,
                    serieComplemento,
                    folioComplemento,
                    rfcEmisor,
                    rfcReceptor,
                    nombreEmisorFinal,
                    regimenEmisorFinal,
                    nombreReceptorFinal,
                    regimenReceptorFinal,
                    domicilioReceptorFinal,
                    correoReceptor,
                    usoCfdi,
                    formaCfdi,
                    metodoCfdi,
                    totalPagos,
                    facturaOriginal.total,
                    facturaUuid,
                    monedaFacturaOriginal,
                    pagos,
                    pacResp,
                    fechaTimbrado
            );
        }
        response.setXmlTimbrado(xmlTimbrado);
        response.setSerieComplemento(serieComplemento);
        response.setFolioComplemento(folioComplemento);
        response.setFechaTimbrado(fechaTimbrado.format(FECHA_HORA));
        response.setTotalPagado(totalPagos.setScale(2, RoundingMode.HALF_UP));
        response.setCorreoReceptor(correoReceptor);
        response.setRfcReceptor(rfcReceptor);
        response.setRfcEmisor(rfcEmisor);

        Long idReceptor = resolverIdReceptorPorRfc(facturaOriginal.rfcReceptor, correoReceptor);

        boolean insercionFactura = uuidFacturaOracleDAO.insertarBasicoConIdReceptor(
                uuidComplemento,
                xmlTimbrado,
                serieComplemento,
                folioComplemento,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                formaCfdi,
                usoCfdi,
                "TIMBRADA",
                "Complemento de pago timbrado",
                metodoCfdi,
                defaultString(facturaOriginal.rfcReceptor, "XAXX010101000"),
                defaultString(facturaOriginal.rfcEmisor, "AAA010101AAA"),
                correoReceptor,
                idReceptor,
                Integer.valueOf(5)
        );

        if (!insercionFactura) {
            String detalle = uuidFacturaOracleDAO.getLastInsertError();
            errores.add("FACTURAS: no se pudo insertar el complemento. " + (detalle != null ? detalle : ""));
            response.setSuccess(false);
            response.setMessage("Error al registrar el complemento en FACTURAS.");
            return response;
        }

        int insertados = 0;
        for (int i = 0; i < pagos.size(); i++) {
            PagoDetalleRequest det = pagos.get(i);
            int idx = i + 1;

            LocalDate fechaPago = parseFecha(det.getFechaPago()).orElse(null);
            if (fechaPago == null) {
                errores.add("Pago #" + idx + ": fecha de pago inválida o vacía.");
                continue;
            }

            BigDecimal monto = det.getMonto();
            if (monto == null) {
                errores.add("Pago #" + idx + ": el monto es obligatorio.");
                continue;
            }

            String formaPago = safeTrim(det.getFormaPago());
            if (formaPago == null) {
                errores.add("Pago #" + idx + ": la forma de pago es obligatoria.");
                continue;
            }

            String moneda = safeTrim(det.getMoneda());
            if (moneda == null) {
                errores.add("Pago #" + idx + ": la moneda es obligatoria.");
                continue;
            }

            PagoOracleDAO.PagoRegistro registro = new PagoOracleDAO.PagoRegistro(
                    facturaId,
                    fechaPago,
                    monto,
                    formaPago.toUpperCase(),
                    moneda.toUpperCase(),
                    uuidComplemento,
                    facturaUuid,
                    usuarioRegistro
            );

            boolean ok = pagoOracleDAO.insertarPago(registro);
            if (ok) {
                insertados++;
            } else {
                String detalle = pagoOracleDAO.getLastInsertError();
                errores.add("Pago #" + idx + ": no se pudo insertar." + (detalle != null ? " Detalle: " + detalle : ""));
                logger.warn("No se pudo insertar pago {} para factura {}: {}", idx, facturaUuid, detalle);
            }
        }

        response.setPagosInsertados(insertados);
        if (errores.isEmpty()) {
            response.setSuccess(true);
            response.setMessage("Complemento de pagos timbrado y registrado. UUID: " + uuidComplemento);
        } else {
            response.setSuccess(insertados > 0);
            response.setMessage("Complemento registrado con incidencias. Exitosos: " + insertados + ", errores: " + errores.size());
        }

        response.setSerieComplemento(serieComplemento);
        response.setFolioComplemento(folioComplemento);
        response.setFechaTimbrado(fechaTimbrado.format(FECHA_HORA));
        response.setTotalPagado(totalPagos.setScale(2, RoundingMode.HALF_UP));
        response.setCorreoReceptor(correoReceptor);
        response.setRfcReceptor(defaultString(facturaOriginal.rfcReceptor, "XAXX010101000"));
        response.setRfcEmisor(defaultString(facturaOriginal.rfcEmisor, "AAA010101AAA"));

        return response;
    }

    public PagoComplementoResponse enviarComplementoPorCorreo(PagoComplementoEnvioRequest request) {
        PagoComplementoResponse response = new PagoComplementoResponse();
        List<String> errores = new ArrayList<>();
        response.setErrors(errores);

        if (request == null) {
            errores.add("Solicitud vacía.");
            response.setSuccess(false);
            response.setMessage("Solicitud inválida.");
            return response;
        }

        String uuidComplemento = safeTrim(request.getUuidComplemento());
        if (uuidComplemento == null) {
            errores.add("El UUID del complemento es obligatorio.");
            response.setSuccess(false);
            response.setMessage("UUID de complemento inválido.");
            return response;
        }

        String correo = safeTrim(request.getCorreoReceptor());
        if (correo == null || !correo.contains("@")) {
            errores.add("El correo receptor es obligatorio y debe tener un formato válido.");
            response.setSuccess(false);
            response.setMessage("Correo del receptor inválido.");
            return response;
        }

        List<PagoDetalleRequest> pagos = request.getPagos();
        if (pagos == null || pagos.isEmpty()) {
            errores.add("Debe proporcionar al menos un pago.");
            response.setSuccess(false);
            response.setMessage("Sin información de pagos para el complemento.");
            return response;
        }

        try {
            List<ITextPdfService.ComplementoPagoPdfData.PagoDetalle> detalles = new ArrayList<>();
            int parcialidad = 1;
            BigDecimal total = BigDecimal.ZERO;
            for (PagoDetalleRequest det : pagos) {
                LocalDate fechaPago = parseFecha(det.getFechaPago()).orElse(LocalDate.now());
                BigDecimal monto = det.getMonto() != null ? det.getMonto() : BigDecimal.ZERO;
                total = total.add(monto);

                ITextPdfService.ComplementoPagoPdfData.PagoDetalle detalle = new ITextPdfService.ComplementoPagoPdfData.PagoDetalle();
                detalle.fechaPago = formatFecha(fechaPago);
                detalle.formaPago = describirFormaPago(det.getFormaPago());
                detalle.moneda = defaultString(det.getMoneda(), "MXN");
                detalle.monto = formatMonto(monto);
                detalle.parcialidad = parcialidad;
                detalle.saldoAnterior = formatMonto(monto);
                detalle.importePagado = formatMonto(monto);
                detalle.saldoInsoluto = formatMonto(BigDecimal.ZERO);
                detalle.uuidRelacionado = defaultString(request.getFacturaUuid(), "");
                detalles.add(detalle);
                parcialidad++;
            }

            ITextPdfService.ComplementoPagoPdfData data = new ITextPdfService.ComplementoPagoPdfData();
            data.uuidComplemento = uuidComplemento;
            data.facturaUuid = defaultString(request.getFacturaUuid(), "");
            data.serieComplemento = defaultString(request.getSerieComplemento(), "REP");
            data.folioComplemento = defaultString(request.getFolioComplemento(), "");
            data.fechaTimbrado = defaultString(request.getFechaTimbrado(), FECHA_HORA.format(LocalDateTime.now()));
            data.rfcEmisor = defaultString(request.getRfcEmisor(), "AAA010101AAA");
            data.rfcReceptor = defaultString(request.getRfcReceptor(), "XAXX010101000");
            data.nombreEmisor = defaultString(request.getNombreEmisor(), data.rfcEmisor);
            data.nombreReceptor = defaultString(request.getNombreReceptor(), data.rfcReceptor);
            data.correoReceptor = correo;
            data.metodoCfdi = defaultString(request.getMetodoCfdi(), "PPD");
            data.formaCfdi = defaultString(request.getFormaCfdi(), "99");
            data.totalPagado = defaultString(request.getTotalPagado(), formatMonto(total));
            data.moneda = defaultString(request.getMoneda(), !detalles.isEmpty() ? detalles.get(0).moneda : "MXN");
            data.cadenaOriginal = "";
            data.selloDigital = "";
            data.selloSat = "";
            data.pagos = detalles;

            // Obtener configuración de logo y color para el PDF
            Map<String, Object> logoConfig = obtenerLogoConfig();

            byte[] pdfBytes = iTextPdfService.generarPdfComplementoPago(data, logoConfig);
            
            // Obtener XML del complemento desde la base de datos
            byte[] xmlBytes = null;
            String nombreXml = "ComplementoPago-" + uuidComplemento + ".xml";
            try {
                Optional<UuidFacturaOracleDAO.Result> optComplemento = uuidFacturaOracleDAO.obtenerBasicosPorUuid(uuidComplemento);
                if (optComplemento.isPresent() && optComplemento.get().xmlContent != null && !optComplemento.get().xmlContent.trim().isEmpty()) {
                    xmlBytes = optComplemento.get().xmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    logger.info("XML del complemento obtenido desde BD. Tamaño: {} bytes", xmlBytes.length);
                } else {
                    logger.warn("No se encontró XML del complemento {} en la base de datos", uuidComplemento);
                }
            } catch (Exception e) {
                logger.error("Error al obtener XML del complemento desde BD: {}", e.getMessage(), e);
            }
            
            String asunto = "Complemento de Pago " + uuidComplemento;
            String mensaje = "Se adjunta el complemento de pago relacionado con el UUID " + defaultString(request.getFacturaUuid(), "") + ".";
            
            // Enviar correo con PDF y XML como adjuntos
            if (xmlBytes != null && xmlBytes.length > 0) {
                correoService.enviarCorreoConAdjuntosDirecto(
                    correo,
                    asunto,
                    mensaje,
                    new HashMap<>(), // templateVars vacío
                    pdfBytes,
                    "ComplementoPago-" + uuidComplemento + ".pdf",
                    xmlBytes,
                    nombreXml
                );
                logger.info("Correo enviado con PDF y XML adjuntos");
            } else {
                // Si no hay XML, enviar solo PDF (comportamiento anterior)
                correoService.enviarCorreoConPdfDirecto(correo, asunto, mensaje, pdfBytes, "ComplementoPago-" + uuidComplemento + ".pdf");
                logger.warn("Correo enviado solo con PDF (XML no disponible)");
            }

            response.setSuccess(true);
            response.setMessage("Complemento enviado por correo correctamente.");
            response.setUuidComplemento(uuidComplemento);
            response.setCorreoReceptor(correo);
            response.setTotalPagado(total.setScale(2, RoundingMode.HALF_UP));
            response.setSerieComplemento(defaultString(request.getSerieComplemento(), "REP"));
            response.setFolioComplemento(defaultString(request.getFolioComplemento(), ""));
            response.setFechaTimbrado(defaultString(request.getFechaTimbrado(), FECHA_HORA.format(LocalDateTime.now())));
            response.setRfcReceptor(defaultString(request.getRfcReceptor(), "XAXX010101000"));
            response.setRfcEmisor(defaultString(request.getRfcEmisor(), "AAA010101AAA"));
        } catch (Exception e) {
            logger.error("Error al enviar complemento de pago por correo", e);
            errores.add(e.getMessage());
            response.setSuccess(false);
            response.setMessage("Error al enviar complemento por correo: " + e.getMessage());
        }

        return response;
    }

    private static String safeTrim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private Optional<LocalDate> parseFecha(String fecha) {
        try {
            if (fecha == null || fecha.trim().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(LocalDate.parse(fecha.trim()));
        } catch (Exception e) {
            logger.debug("Fecha de pago inválida: {}", fecha);
            return Optional.empty();
        }
    }

    private Long parseUsuario(String usuarioRegistro) {
        if (usuarioRegistro == null) {
            return null;
        }
        try {
            String trimmed = usuarioRegistro.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ex) {
            logger.debug("Usuario de registro no numérico: {}", usuarioRegistro);
            return null;
        }
    }

    private String generarFolioComplemento() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));
    }

    private String formatMonto(BigDecimal monto) {
        return monto == null
                ? "0.00"
                : monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String defaultString(String value, String fallback) {
        String trimmed = safeTrim(value);
        return trimmed != null ? trimmed : fallback;
    }

    private String construirXmlComplementoPago(String uuidComplemento,
                                               String serie,
                                               String folio,
                                               String rfcEmisor,
                                               String rfcReceptor,
                                               String nombreEmisor,
                                               String regimenFiscalEmisor,
                                               String nombreReceptor,
                                               String regimenFiscalReceptor,
                                               String domicilioFiscalReceptor,
                                               String correoReceptor,
                                               String usoCfdi,
                                               String formaCfdi,
                                               String metodoCfdi,
                                               BigDecimal totalPagos,
                                               BigDecimal totalFacturaOriginal,
                                               String uuidRelacionado,
                                               String monedaFacturaOriginal,
                                               List<PagoDetalleRequest> pagos,
                                               PacTimbradoResponse pacResp,
                                               LocalDateTime fechaTimbrado) {

        String fechaEmision = LocalDateTime.now().format(FECHA_HORA);
        String fechaTimbradoStr = fechaTimbrado != null
                ? fechaTimbrado.format(FECHA_HORA)
                : fechaEmision;

        StringBuilder pagosXml = new StringBuilder();
        int parcialidad = 1;
        BigDecimal saldoFactura = totalFacturaOriginal != null ? totalFacturaOriginal : totalPagos;
        if (saldoFactura == null) {
            saldoFactura = BigDecimal.ZERO;
        }
        saldoFactura = saldoFactura.setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldoAnteriorActual = saldoFactura;

        for (PagoDetalleRequest det : pagos) {
            LocalDate fechaPago = parseFecha(det.getFechaPago()).orElse(LocalDate.now());
            // Según XSD: FechaPago debe ser formato ISO 8601 aaaa-mm-ddThh:mm:ss
            // Si no se proporciona hora, usar 12:00:00 según documentación del XSD
            String fechaPagoStr = fechaPago.atTime(12, 0, 0).format(FECHA_HORA);
            BigDecimal monto = det.getMonto() != null ? det.getMonto() : BigDecimal.ZERO;
            monto = monto.setScale(2, RoundingMode.HALF_UP);
            BigDecimal saldoAnt = saldoAnteriorActual != null ? saldoAnteriorActual : BigDecimal.ZERO;
            if (saldoAnt.compareTo(BigDecimal.ZERO) < 0) {
                saldoAnt = BigDecimal.ZERO;
            }
            BigDecimal saldoInsoluto = saldoAnt.subtract(monto);
            if (saldoInsoluto.compareTo(BigDecimal.ZERO) < 0) {
                saldoInsoluto = BigDecimal.ZERO;
            }
            saldoInsoluto = saldoInsoluto.setScale(2, RoundingMode.HALF_UP);
            String monedaPago = defaultString(safeTrim(det.getMoneda()), "MXN");
            
            // CRÍTICO: Según Finkok, TipoCambioP debe ser "1" cuando MonedaP es MXN
            // Cuando MonedaP es diferente de MXN, TipoCambioP es requerido con el tipo de cambio real
            String tipoCambioAttr;
            if ("MXN".equalsIgnoreCase(monedaPago)) {
                // Finkok requiere TipoCambioP="1" cuando la moneda es MXN
                tipoCambioAttr = " TipoCambioP=\"1\"";
            } else {
                // Si la moneda no es MXN, TipoCambioP es requerido con el tipo de cambio real
                // Por defecto usar 1.000000 si no se proporciona (debería calcularse según tipo de cambio real)
                tipoCambioAttr = " TipoCambioP=\"1.000000\"";
            }

            // Moneda del documento relacionado (factura original)
            String monedaDR = defaultString(monedaFacturaOriginal, "MXN");
            
            pagosXml.append("      <pago20:Pago FechaPago=\"")
                    .append(fechaPagoStr)
                    .append("\" FormaDePagoP=\"")
                    .append(safeTrim(det.getFormaPago()))
                    .append("\" MonedaP=\"")
                    .append(monedaPago)
                    .append("\"")
                    .append(tipoCambioAttr)
                    .append(" Monto=\"")
                    .append(formatMonto(monto))
                    .append("\">\n")
                    .append("        <pago20:DoctoRelacionado IdDocumento=\"")
                    .append(uuidRelacionado)
                    .append("\" MonedaDR=\"")
                    .append(monedaDR)
                    .append("\"");
            
            // CRÍTICO: Según Finkok, EquivalenciaDR debe ser "1" (sin decimales) cuando 
            // la moneda del documento relacionado es igual a la moneda de pago
            // Si son diferentes, EquivalenciaDR debe tener el tipo de cambio con 10 decimales
            String equivalenciaDR;
            if (monedaDR.equalsIgnoreCase(monedaPago)) {
                // Monedas iguales: usar "1" sin decimales (requerido por Finkok)
                equivalenciaDR = "1";
            } else {
                // Monedas diferentes: usar tipo de cambio con 10 decimales (por defecto 1.0000000000)
                // NOTA: En producción debería calcularse el tipo de cambio real
                equivalenciaDR = "1.0000000000";
            }
            pagosXml.append(" EquivalenciaDR=\"").append(equivalenciaDR).append("\"");
            
            pagosXml.append(" NumParcialidad=\"")
                    .append(parcialidad++)
                    .append("\" ImpSaldoAnt=\"")
                    .append(formatMonto(saldoAnt))
                    .append("\" ImpPagado=\"")
                    .append(formatMonto(monto))
                    .append("\" ImpSaldoInsoluto=\"")
                    .append(formatMonto(saldoInsoluto))
                    .append("\" ObjetoImpDR=\"01\"/>\n")
                    .append("      </pago20:Pago>\n");

            saldoAnteriorActual = saldoInsoluto;
        }

        if (pagosXml.length() == 0) {
            // Caso fallback: si no hay pagos, crear un pago por defecto
            // Nota: Este caso no debería ocurrir en producción ya que se valida antes
            pagosXml.append("      <pago20:Pago FechaPago=\"")
                    .append(fechaEmision)
                    .append("\" FormaDePagoP=\"99\" MonedaP=\"MXN\" TipoCambioP=\"1\" Monto=\"0.00\">\n")
                    .append("        <pago20:DoctoRelacionado IdDocumento=\"")
                    .append(uuidRelacionado)
                    .append("\" MonedaDR=\"MXN\" EquivalenciaDR=\"1\" NumParcialidad=\"1\" ImpSaldoAnt=\"")
                    .append(formatMonto(saldoFactura))
                    .append("\" ImpPagado=\"0.00\" ImpSaldoInsoluto=\"")
                    .append(formatMonto(saldoFactura))
                    .append("\" ObjetoImpDR=\"01\"/>\n")
                    .append("      </pago20:Pago>\n");
        }

        String totalPagosStr = formatMonto(totalPagos);

        String nombreEmisorAttr = escapeXml(defaultString(nombreEmisor, rfcEmisor));
        String regimenEmisorAttr = defaultString(regimenFiscalEmisor, "601");
        String nombreReceptorAttr = escapeXml(defaultString(nombreReceptor, rfcReceptor));
        String regimenReceptorAttr = defaultString(regimenFiscalReceptor, "601");
        String domicilioReceptorAttr = defaultString(domicilioFiscalReceptor, "00000");

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<cfdi:Comprobante Version=\"4.0\" Serie=\"").append(serie).append("\" Folio=\"").append(folio).append("\" ");
        xml.append("Fecha=\"").append(fechaEmision).append("\" SubTotal=\"0.00\" Moneda=\"XXX\" Total=\"0.00\" ");
        // IMPORTANTE: Para TipoDeComprobante="P" (complemento de pago), NO deben existir FormaPago ni MetodoPago en el Comprobante principal
        // Según CFDI40103: FormaPago no debe existir cuando TipoDeComprobante es T, N o P
        // Según CFDI40125: MetodoPago no debe existir cuando TipoDeComprobante es T o P
        // La FormaPago debe estar en el elemento pago20:Pago dentro del complemento (FormaDePagoP)
        xml.append("TipoDeComprobante=\"P\" Exportacion=\"01\" LugarExpedicion=\"").append(codigoPostalEmisorDefault).append("\" ");
        xml.append("xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\" ");
        xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:pago20=\"http://www.sat.gob.mx/Pagos20\" ");
        xml.append("xmlns:tfd=\"http://www.sat.gob.mx/TimbreFiscalDigital\" ");
        xml.append("xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/4 http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd ");
        xml.append("http://www.sat.gob.mx/Pagos20 http://www.sat.gob.mx/sitio_internet/cfd/Pagos/Pagos20.xsd ");
        xml.append("http://www.sat.gob.mx/TimbreFiscalDigital http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd\">\n");
        xml.append("  <cfdi:Emisor Rfc=\"").append(rfcEmisor).append("\" Nombre=\"").append(nombreEmisorAttr)
                .append("\" RegimenFiscal=\"").append(regimenEmisorAttr).append("\"/>\n");
        xml.append("  <cfdi:Receptor Rfc=\"").append(rfcReceptor).append("\" Nombre=\"").append(nombreReceptorAttr).append("\" ");
        xml.append("DomicilioFiscalReceptor=\"").append(domicilioReceptorAttr).append("\" RegimenFiscalReceptor=\"")
                .append(regimenReceptorAttr).append("\" UsoCFDI=\"").append(usoCfdi).append("\"/>\n");
        xml.append("  <cfdi:Conceptos>\n");
        xml.append("    <cfdi:Concepto ClaveProdServ=\"84111506\" Cantidad=\"1\" ClaveUnidad=\"ACT\" Descripcion=\"Pago\" ValorUnitario=\"0.00\" Importe=\"0.00\" ObjetoImp=\"01\"/>\n");
        xml.append("  </cfdi:Conceptos>\n");
        xml.append("  <cfdi:Complemento>\n");
        xml.append("    <pago20:Pagos Version=\"2.0\">\n");
        // Según XSD: MontoTotalPagos es requerido y debe expresarse en MXN
        // Convertir totalPagos a MXN si es necesario (por ahora asumimos que ya está en MXN)
        xml.append("      <pago20:Totales MontoTotalPagos=\"").append(totalPagosStr).append("\"/>\n");
        xml.append(pagosXml);
        xml.append("    </pago20:Pagos>\n");

        if (pacResp != null) {
            String uuidTimbre = safeTrim(pacResp.getUuid());
            if (uuidTimbre == null || uuidTimbre.isEmpty()) {
                uuidTimbre = uuidComplemento != null ? uuidComplemento : UUID.randomUUID().toString().toUpperCase();
            }
            String selloCfd = defaultString(pacResp.getSelloDigital(), "SELLO_CFD_COMPLEMENTO");
            String selloSat = defaultString(pacResp.getSatSeal(), "SELLO_SAT_COMPLEMENTO");
            String noCertSat = defaultString(pacResp.getNoCertificadoSAT(), "00001000000504465028");
            xml.append("    <tfd:TimbreFiscalDigital Version=\"1.1\" UUID=\"").append(uuidTimbre).append("\" ");
            xml.append("FechaTimbrado=\"").append(fechaTimbradoStr).append("\" SelloCFD=\"").append(selloCfd).append("\" ");
            xml.append("NoCertificadoSAT=\"").append(noCertSat).append("\" SelloSAT=\"").append(selloSat).append("\"/>\n");
        }

        xml.append("  </cfdi:Complemento>\n");
        xml.append("</cfdi:Comprobante>");

        return xml.toString();
    }

    private Long resolverIdReceptorPorRfc(String rfc, String correoReceptor) {
        String normalized = safeTrim(rfc);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase();
        Optional<ClienteCatalogo> existente = clienteCatalogoService.buscarPorRfc(normalized);
        if (existente.isPresent()) {
            ClienteCatalogo cliente = existente.get();
            if (correoReceptor != null && (cliente.getCorreoElectronico() == null || cliente.getCorreoElectronico().isBlank())) {
                cliente.setCorreoElectronico(correoReceptor);
                clienteCatalogoService.guardar(cliente);
            }
            return cliente.getIdCliente();
        }
        ClienteCatalogo nuevo = new ClienteCatalogo();
        nuevo.setRfc(normalized);
        nuevo.setRazonSocial(normalized);
        if (correoReceptor != null) {
            nuevo.setCorreoElectronico(correoReceptor);
        }
        ClienteCatalogo guardado = clienteCatalogoService.guardar(nuevo);
        return guardado != null ? guardado.getIdCliente() : null;
    }

    private static java.util.Map<String, String> crearCatalogoFormaPago() {
        java.util.Map<String, String> map = new HashMap<>();
        map.put("01", "01 - Efectivo");
        map.put("02", "02 - Cheque nominativo");
        map.put("03", "03 - Transferencia electrónica");
        map.put("04", "04 - Tarjeta de crédito");
        map.put("05", "05 - Monedero electrónico");
        map.put("06", "06 - Dinero electrónico");
        map.put("08", "08 - Vales de despensa");
        map.put("12", "12 - Dación en pago");
        map.put("13", "13 - Pago por subrogación");
        map.put("14", "14 - Pago por consignación");
        map.put("15", "15 - Condonación");
        map.put("17", "17 - Compensación");
        map.put("23", "23 - Novación");
        map.put("24", "24 - Confusión");
        map.put("25", "25 - Remisión de deuda");
        map.put("26", "26 - Prescripción o caducidad");
        map.put("27", "27 - A satisfacción del acreedor");
        map.put("28", "28 - Tarjeta de débito");
        map.put("29", "29 - Tarjeta de servicios");
        map.put("30", "30 - Aplicación de anticipos");
        map.put("31", "31 - Intermediario pagos");
        map.put("99", "99 - Por definir");
        return map;
    }

    private String describirFormaPago(String clave) {
        String normalized = safeTrim(clave);
        if (normalized == null) {
            return "Sin definir";
        }
        return FORMA_PAGO_DESCRIPCION.getOrDefault(normalized, normalized + " - Sin descripción");
    }

    private String formatFecha(LocalDate date) {
        if (date == null) {
            return LocalDate.now().toString();
        }
        return date.format(DateTimeFormatter.ISO_DATE);
    }

    /**
     * Obtiene la configuración de logo y colores para el PDF, similar a CorreoService
     */
    private Map<String, Object> obtenerLogoConfig() {
        Map<String, Object> logoConfig = new HashMap<>();
        Map<String, Object> customColors = new HashMap<>();
        
        // Obtener configuración de formato (color)
        com.cibercom.facturacion_back.dto.FormatoCorreoDto configuracionFormato = null;
        try {
            com.cibercom.facturacion_back.dto.ConfiguracionCorreoResponseDto configResponse = correoService.obtenerConfiguracionMensajes();
            if (configResponse != null && configResponse.getFormatoCorreo() != null) {
                configuracionFormato = configResponse.getFormatoCorreo();
                logger.info("Usando color de formato de configuración de mensajes: {}", configuracionFormato.getColorTexto());
            } else {
                configuracionFormato = formatoCorreoService.obtenerConfiguracionActiva();
                logger.info("Usando color de formato activo (archivo): {}", configuracionFormato != null ? configuracionFormato.getColorTexto() : null);
            }
        } catch (Exception e) {
            logger.warn("No se pudo obtener FormatoCorreo: {}", e.getMessage());
        }
        
        // Establecer color primario
        String colorPrimario = (configuracionFormato != null && configuracionFormato.getColorTexto() != null && !configuracionFormato.getColorTexto().isEmpty())
            ? configuracionFormato.getColorTexto().trim()
            : "#1d4ed8";
        customColors.put("primary", colorPrimario);
        logoConfig.put("customColors", customColors);
        
        // Obtener logo (intentar desde endpoint del backend)
        try {
            String port = (environment != null) 
                ? environment.getProperty("local.server.port", environment.getProperty("server.port", "8080")) 
                : "8080";
            String logoEndpoint = "http://localhost:" + port + "/api/logos/cibercom-png";
            logoConfig.put("logoUrl", logoEndpoint);
            logger.info("Incluyendo logoUrl en configuración de PDF: {}", logoEndpoint);
        } catch (Exception e) {
            logger.warn("No se pudo configurar logoUrl: {}", e.getMessage());
        }
        
        logger.info("Color primario seleccionado para PDF de complemento: {}", colorPrimario);
        return logoConfig;
    }

    private CfdiDatosBasicos extraerDatosBasicosDesdeXml(String xmlContent) {
        String xml = safeTrim(xmlContent);
        if (xml == null) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            CfdiDatosBasicos datos = new CfdiDatosBasicos();
            Element emisor = obtenerPrimerElemento(doc, "Emisor");
            if (emisor != null) {
                datos.emisorNombre = safeTrim(emisor.getAttribute("Nombre"));
                datos.emisorRegimen = safeTrim(emisor.getAttribute("RegimenFiscal"));
            }

            Element receptor = obtenerPrimerElemento(doc, "Receptor");
            if (receptor != null) {
                datos.receptorNombre = safeTrim(receptor.getAttribute("Nombre"));
                datos.receptorRegimen = safeTrim(receptor.getAttribute("RegimenFiscalReceptor"));
                datos.receptorDomicilio = safeTrim(receptor.getAttribute("DomicilioFiscalReceptor"));
            }
            return datos;
        } catch (Exception e) {
            logger.warn("No se pudieron extraer datos del CFDI original para el complemento: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrae la moneda del CFDI desde el XML
     */
    private String extraerMonedaDesdeXml(String xmlContent) {
        String xml = safeTrim(xmlContent);
        if (xml == null) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            // Buscar el elemento Comprobante
            Element comprobante = obtenerPrimerElemento(doc, "Comprobante");
            if (comprobante != null) {
                String moneda = safeTrim(comprobante.getAttribute("Moneda"));
                if (moneda != null && !moneda.isEmpty()) {
                    return moneda;
                }
            }
            return null;
        } catch (Exception e) {
            logger.warn("No se pudo extraer la moneda del CFDI original: {}", e.getMessage());
            return null;
        }
    }

    private Element obtenerPrimerElemento(Document doc, String localName) {
        org.w3c.dom.NodeList list = doc.getElementsByTagNameNS("http://www.sat.gob.mx/cfd/4", localName);
        if (list == null || list.getLength() == 0) {
            list = doc.getElementsByTagName(localName);
        }
        return (list != null && list.getLength() > 0) ? (Element) list.item(0) : null;
    }

    private static class CfdiDatosBasicos {
        @SuppressWarnings("unused")
        String emisorNombre;
        @SuppressWarnings("unused")
        String emisorRegimen;
        String receptorNombre;
        String receptorRegimen;
        String receptorDomicilio;
    }

    /**
     * Extrae el RFC del SubjectDN del certificado
     * Basado en el método de CfdiFirmaService
     */
    private String extraerRFCDeSubjectDN(String subjectDN) {
        if (subjectDN == null || subjectDN.isEmpty()) {
            return null;
        }
        
        // Buscar RFC en formato 2.5.4.45 (RFC del SAT)
        java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("2\\.5\\.4\\.45=#([0-9A-Fa-f]+)");
        java.util.regex.Matcher matcher1 = pattern1.matcher(subjectDN);
        if (matcher1.find()) {
            String hexRfc = matcher1.group(1);
            // Convertir de hexadecimal a ASCII
            try {
                StringBuilder rfc = new StringBuilder();
                for (int i = 0; i < hexRfc.length(); i += 2) {
                    String hex = hexRfc.substring(i, Math.min(i + 2, hexRfc.length()));
                    int charCode = Integer.parseInt(hex, 16);
                    if (charCode >= 32 && charCode <= 126) { // Caracteres imprimibles ASCII
                        rfc.append((char) charCode);
                    }
                }
                String rfcStr = rfc.toString().trim();
                // Extraer RFC (normalmente está al inicio y tiene 12-13 caracteres)
                java.util.regex.Pattern rfcPattern = java.util.regex.Pattern.compile("([A-Z&Ñ]{3,4}\\d{6}[A-Z0-9]{3})");
                java.util.regex.Matcher rfcMatcher = rfcPattern.matcher(rfcStr);
                if (rfcMatcher.find()) {
                    return rfcMatcher.group(1);
                }
                return rfcStr;
            } catch (Exception e) {
                logger.debug("Error al convertir hex a ASCII para RFC: {}", e.getMessage());
            }
        }
        
        // Buscar RFC en formato estándar (RFC=...)
        java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("RFC=([A-Z&Ñ0-9]{12,13})", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher2 = pattern2.matcher(subjectDN);
        if (matcher2.find()) {
            return matcher2.group(1).toUpperCase();
        }
        
        // Buscar RFC en formato CN o OU (puede estar como XIA190128J61)
        java.util.regex.Pattern pattern3 = java.util.regex.Pattern.compile("([A-Z&Ñ]{3,4}\\d{6}[A-Z0-9]{3})");
        java.util.regex.Matcher matcher3 = pattern3.matcher(subjectDN);
        if (matcher3.find()) {
            return matcher3.group(1).toUpperCase();
        }
        
        return null;
    }
}
