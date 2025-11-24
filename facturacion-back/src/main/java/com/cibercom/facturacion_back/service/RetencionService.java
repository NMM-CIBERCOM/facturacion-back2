package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.ConceptoOracleDAO;
import com.cibercom.facturacion_back.dao.RetencionOracleDAO;
import com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO;
import com.cibercom.facturacion_back.dto.PacTimbradoRequest;
import com.cibercom.facturacion_back.dto.PacTimbradoResponse;
import com.cibercom.facturacion_back.dto.RetencionRequest;
import com.cibercom.facturacion_back.dto.RetencionResponse;
import com.cibercom.facturacion_back.integration.RetentionsPacClient;
import com.cibercom.facturacion_back.model.ClienteCatalogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class RetencionService {

    private static final Logger logger = LoggerFactory.getLogger(RetencionService.class);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    // Mapeo de tipos de retención a claves del SAT
    private static final Map<String, String> CLAVE_RETENCION_MAP = crearMapaClavesRetencion();
    
    private final ConceptoOracleDAO conceptoOracleDAO;
    private final RetencionOracleDAO retencionOracleDAO;
    private final UuidFacturaOracleDAO uuidFacturaOracleDAO;
    private final ClienteCatalogoService clienteCatalogoService;
    private final RetentionsPacClient retentionsPacClient;
    private final ITextPdfService iTextPdfService;
    private final CorreoService correoService;
    private final FormatoCorreoService formatoCorreoService;
    private final Environment environment;
    private final com.cibercom.facturacion_back.service.FacturaService facturaService;

    // Inyectar valores de application.yml usando @Value (igual que FacturaService y PagoService)
    @Value("${facturacion.emisor.rfc:IVD920810GU2}")
    private String rfcEmisorDefault;
    
    @Value("${facturacion.emisor.nombre:INNOVACION VALOR Y DESARROLLO SA}")
    private String nombreEmisorDefault;
    
    @Value("${facturacion.emisor.regimen:601}")
    private String regimenFiscalEmisorDefault;
    
    @Value("${facturacion.emisor.cp:58000}")
    private String codigoPostalEmisorDefault;

    public RetencionService(ConceptoOracleDAO conceptoOracleDAO,
                           RetencionOracleDAO retencionOracleDAO,
                           UuidFacturaOracleDAO uuidFacturaOracleDAO,
                           ClienteCatalogoService clienteCatalogoService,
                           RetentionsPacClient retentionsPacClient,
                           ITextPdfService iTextPdfService,
                           CorreoService correoService,
                           FormatoCorreoService formatoCorreoService,
                           Environment environment,
                           com.cibercom.facturacion_back.service.FacturaService facturaService) {
        this.conceptoOracleDAO = conceptoOracleDAO;
        this.retencionOracleDAO = retencionOracleDAO;
        this.uuidFacturaOracleDAO = uuidFacturaOracleDAO;
        this.clienteCatalogoService = clienteCatalogoService;
        this.retentionsPacClient = retentionsPacClient;
        this.iTextPdfService = iTextPdfService;
        this.correoService = correoService;
        this.formatoCorreoService = formatoCorreoService;
        this.environment = environment;
        this.facturaService = facturaService;
    }

    public RetencionResponse registrarRetencion(RetencionRequest request) {
        RetencionResponse response = new RetencionResponse();
        List<String> errores = new ArrayList<>();
        response.setErrors(errores);

        if (request == null) {
            errores.add("Solicitud vacía.");
            response.setSuccess(false);
            response.setMessage("Solicitud inválida.");
            return response;
        }

        // Validar datos obligatorios
        // Usar valores de application.yml (igual que FacturaService y PagoService)
        String rfcEmisor = safeTrim(request.getRfcEmisor());
        if (rfcEmisor == null || rfcEmisor.isEmpty()) {
            rfcEmisor = rfcEmisorDefault;
        }
        String rfcReceptor = safeTrim(request.getRfcReceptor());
        String tipoRetencion = safeTrim(request.getTipoRetencion());
        BigDecimal montoBase = request.getMontoBase();
        String correoReceptor = safeTrim(request.getCorreoReceptor());
        String periodoMes = safeTrim(request.getPeriodoMes());
        String periodoAnio = safeTrim(request.getPeriodoAnio());
        String fechaPago = safeTrim(request.getFechaPago());
        String concepto = safeTrim(request.getConcepto());

        if (rfcEmisor == null || rfcReceptor == null || tipoRetencion == null || 
            montoBase == null || montoBase.compareTo(BigDecimal.ZERO) <= 0) {
            errores.add("Datos incompletos: RFC emisor, RFC receptor, tipo de retención y monto base son obligatorios.");
            response.setSuccess(false);
            response.setMessage("Datos incompletos.");
            return response;
        }

        if (correoReceptor == null || !correoReceptor.contains("@")) {
            errores.add("El correo del receptor es obligatorio y debe tener un formato válido.");
            response.setSuccess(false);
            response.setMessage("Correo del receptor inválido.");
            return response;
        }

        // Calcular montos retenidos
        BigDecimal isrRetenido = request.getIsrRetenido() != null ? request.getIsrRetenido() : BigDecimal.ZERO;
        BigDecimal ivaRetenido = request.getIvaRetenido() != null ? request.getIvaRetenido() : BigDecimal.ZERO;
        BigDecimal montoRetenido = isrRetenido.add(ivaRetenido);
        
        if (montoRetenido.compareTo(BigDecimal.ZERO) <= 0) {
            errores.add("El monto retenido debe ser mayor a cero.");
            response.setSuccess(false);
            response.setMessage("Monto retenido inválido.");
            return response;
        }

        // Determinar impuesto principal
        String impuesto = determinarImpuesto(tipoRetencion, isrRetenido, ivaRetenido);
        String claveRetencion = CLAVE_RETENCION_MAP.getOrDefault(tipoRetencion, "25");

        // Parsear fechas y períodos
        LocalDate fechaPagoDate = parseFecha(fechaPago).orElse(LocalDate.now());
        Integer periodoMesInt = parseInteger(periodoMes);
        Integer periodoAnioInt = parseInteger(periodoAnio);
        
        if (periodoMesInt == null || periodoAnioInt == null) {
            errores.add("El período (mes y año) es obligatorio.");
            response.setSuccess(false);
            response.setMessage("Período inválido.");
            return response;
        }

        Long usuarioRegistro = parseUsuario(request.getUsuarioRegistro());

        // Resolver ID del receptor
        String razonSocialReceptor = construirRazonSocial(request);
        Long idReceptor = resolverIdReceptorPorRfc(rfcReceptor, razonSocialReceptor, correoReceptor);

        // Generar serie y folio
        String serieRetencion = "RET";
        String folioRetencion = generarFolioRetencion();
        
        // Construir XML de retención
        // CRÍTICO: Usar nombre del emisor desde @Value (application.yml) - igual que FacturaService y PagoService
        // El nombre debe coincidir EXACTAMENTE con el registrado en Finkok (demo) o SAT (producción)
        String nombreEmisorFinal = nombreEmisorDefault != null && !nombreEmisorDefault.trim().isEmpty() 
                ? nombreEmisorDefault.trim() 
                : rfcEmisor;
        String regimenEmisorFinal = regimenFiscalEmisorDefault != null && !regimenFiscalEmisorDefault.trim().isEmpty()
                ? regimenFiscalEmisorDefault.trim()
                : "601";
        
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("📋 DATOS DEL EMISOR PARA RETENCIÓN:");
        logger.info("  RFC: {}", rfcEmisor);
        logger.info("  Nombre: '{}' (desde application.yml)", nombreEmisorFinal);
        logger.info("  Régimen Fiscal: {}", regimenEmisorFinal);
        logger.info("═══════════════════════════════════════════════════════════════");
        
        String xmlRetencion = construirXmlRetencion(
                rfcEmisor,
                nombreEmisorFinal,
                regimenEmisorFinal,
                rfcReceptor,
                razonSocialReceptor,
                correoReceptor,
                tipoRetencion,
                claveRetencion,
                montoBase,
                impuesto,
                isrRetenido,
                ivaRetenido,
                montoRetenido,
                periodoMesInt,
                periodoAnioInt,
                fechaPagoDate,
                concepto,
                serieRetencion,
                folioRetencion
        );

        // Enviar al PAC
        PacTimbradoRequest pacRequest = PacTimbradoRequest.builder()
                .xmlContent(xmlRetencion)
                .rfcEmisor(rfcEmisor)
                .rfcReceptor(rfcReceptor)
                .total(montoRetenido.setScale(2, RoundingMode.HALF_UP).doubleValue())
                .tipo("RETENCION")
                .fechaFactura(LocalDateTime.now().toString())
                .publicoGeneral(false)
                .serie(serieRetencion)
                .folio(folioRetencion)
                .medioPago("99")
                .formaPago("99")
                .usoCFDI("G03")
                .relacionadosUuids(null)
                .build();

        PacTimbradoResponse pacResp = retentionsPacClient.solicitarTimbrado(pacRequest);
        if (pacResp == null || Boolean.FALSE.equals(pacResp.getOk())) {
            errores.add(pacResp != null && pacResp.getMessage() != null
                    ? pacResp.getMessage()
                    : "PAC no disponible para timbrado.");
            response.setSuccess(false);
            response.setMessage("Error al timbrar retención.");
            return response;
        }

        String uuidRetencion = safeTrim(pacResp.getUuid());
        if (uuidRetencion == null) {
            uuidRetencion = UUID.randomUUID().toString().toUpperCase();
        }
        response.setUuidRetencion(uuidRetencion);

        LocalDateTime fechaTimbrado = pacResp != null && pacResp.getFechaTimbrado() != null
                ? pacResp.getFechaTimbrado()
                : LocalDateTime.now();

        // Actualizar XML con datos del PAC
        String xmlTimbrado = actualizarXmlConDatosPac(xmlRetencion, pacResp, uuidRetencion, fechaTimbrado);
        response.setXmlTimbrado(xmlTimbrado);
        response.setSerieRetencion(serieRetencion);
        response.setFolioRetencion(folioRetencion);
        response.setFechaTimbrado(fechaTimbrado.format(FECHA_HORA));
        response.setMontoRetenido(montoRetenido.setScale(2, RoundingMode.HALF_UP));
        response.setBaseRetencion(montoBase.setScale(2, RoundingMode.HALF_UP));
        response.setCorreoReceptor(correoReceptor);
        response.setRfcReceptor(rfcReceptor);
        response.setRfcEmisor(rfcEmisor);

        // Insertar en FACTURAS (tipo_factura = 6 para retenciones)
        boolean insercionFactura = uuidFacturaOracleDAO.insertarBasicoConIdReceptor(
                uuidRetencion,
                xmlTimbrado,
                serieRetencion,
                folioRetencion,
                montoBase,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                montoRetenido,
                "99",
                "G03",
                "TIMBRADA",
                "CFDI de Retenciones e Información de Pagos",
                "99",
                rfcReceptor,
                rfcEmisor,
                correoReceptor,
                idReceptor,
                Integer.valueOf(6) // tipo_factura = 6 para retenciones
        );

        if (!insercionFactura) {
            String detalle = uuidFacturaOracleDAO.getLastInsertError();
            errores.add("FACTURAS: no se pudo insertar la retención. " + (detalle != null ? detalle : ""));
            response.setSuccess(false);
            response.setMessage("Error al registrar la retención en FACTURAS.");
            return response;
        }

        // Obtener ID_FACTURA recién insertado
        Optional<Long> facturaIdOpt = conceptoOracleDAO.obtenerIdFacturaPorUuid(uuidRetencion);
        Long facturaId = facturaIdOpt.orElse(null);
        
        if (facturaId == null) {
            errores.add("No se pudo obtener el ID_FACTURA de la retención insertada.");
            response.setSuccess(false);
            response.setMessage("Error al obtener ID_FACTURA.");
            return response;
        }

        response.setFacturaId(facturaId);

        // Insertar en RETENCIONES
        RetencionOracleDAO.RetencionRegistro registro = new RetencionOracleDAO.RetencionRegistro(
                facturaId,
                tipoRetencion,
                claveRetencion,
                montoBase.setScale(2, RoundingMode.HALF_UP),
                impuesto,
                montoRetenido.setScale(2, RoundingMode.HALF_UP),
                periodoMesInt,
                periodoMesInt, // PERIODO_MES_FIN igual a PERIODO_MES_INI si es un solo mes
                periodoAnioInt,
                uuidRetencion,
                fechaPagoDate,
                concepto,
                usuarioRegistro
        );

        Optional<Long> idRetencionOpt = retencionOracleDAO.insertarRetencion(registro);
        if (idRetencionOpt.isEmpty()) {
            String detalle = retencionOracleDAO.getLastInsertError();
            errores.add("RETENCIONES: no se pudo insertar. " + (detalle != null ? detalle : ""));
            response.setSuccess(false);
            response.setMessage("Error al registrar en RETENCIONES.");
            return response;
        }

        response.setIdRetencion(idRetencionOpt.get());
        response.setSuccess(true);
        response.setMessage("Retención timbrada y registrada correctamente. UUID: " + uuidRetencion);

        return response;
    }

    // Métodos auxiliares
    private String safeTrim(String s) {
        return s != null ? s.trim() : null;
    }

    private String defaultString(String s, String def) {
        return s != null && !s.isBlank() ? s : def;
    }

    private Optional<LocalDate> parseFecha(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(fechaStr));
        } catch (Exception e) {
            logger.warn("Error al parsear fecha: {}", fechaStr);
            return Optional.empty();
        }
    }

    private Integer parseInteger(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseUsuario(String usuarioStr) {
        if (usuarioStr == null || usuarioStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(usuarioStr);
        } catch (Exception e) {
            return null;
        }
    }

    private String determinarImpuesto(String tipoRetencion, BigDecimal isrRetenido, BigDecimal ivaRetenido) {
        if (tipoRetencion != null && tipoRetencion.startsWith("ISR_")) {
            return "ISR";
        } else if ("IVA".equals(tipoRetencion)) {
            return "IVA";
        } else if (isrRetenido != null && isrRetenido.compareTo(BigDecimal.ZERO) > 0) {
            return "ISR";
        } else if (ivaRetenido != null && ivaRetenido.compareTo(BigDecimal.ZERO) > 0) {
            return "IVA";
        }
        return "ISR"; // Por defecto
    }

    private String construirRazonSocial(RetencionRequest request) {
        if ("moral".equalsIgnoreCase(request.getTipoPersona())) {
            return defaultString(request.getRazonSocial(), "");
        } else {
            // Persona física: nombre + paterno + materno
            String nombre = defaultString(request.getNombre(), "");
            String paterno = defaultString(request.getPaterno(), "");
            String materno = defaultString(request.getMaterno(), "");
            return (nombre + " " + paterno + " " + materno).trim();
        }
    }

    private Long resolverIdReceptorPorRfc(String rfc, String razonSocial, String correo) {
        if (rfc == null || rfc.isBlank()) {
            return null;
        }
        try {
            Optional<ClienteCatalogo> clienteOpt = clienteCatalogoService.buscarPorRfc(rfc.trim().toUpperCase());
            if (clienteOpt.isPresent()) {
                return clienteOpt.get().getIdCliente();
            }
            // Crear nuevo cliente si no existe
            ClienteCatalogo nuevo = new ClienteCatalogo();
            nuevo.setRfc(rfc.trim().toUpperCase());
            nuevo.setRazonSocial(razonSocial != null ? razonSocial : rfc);
            nuevo.setCorreoElectronico(correo);
            ClienteCatalogo guardado = clienteCatalogoService.guardar(nuevo);
            return guardado != null ? guardado.getIdCliente() : null;
        } catch (Exception e) {
            logger.error("Error al resolver ID receptor por RFC {}: {}", rfc, e.getMessage(), e);
            return null;
        }
    }

    private String generarFolioRetencion() {
        return String.valueOf(System.currentTimeMillis() % 1000000);
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

    private String construirXmlRetencion(String rfcEmisor, String nombreEmisor, String regimenFiscalEmisor,
                                        String rfcReceptor, String razonSocialReceptor,
                                        String correoReceptor, String tipoRetencion, String claveRetencion,
                                        BigDecimal montoBase, String impuesto, BigDecimal isrRetenido,
                                        BigDecimal ivaRetenido, BigDecimal montoRetenido,
                                        Integer periodoMes, Integer periodoAnio, LocalDate fechaPago,
                                        String concepto, String serie, String folio) {
        // Construir XML básico de retención (CFDI de Retenciones e Información de Pagos 2.0)
        // Este es un esqueleto que debe completarse según el formato exacto del SAT
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        // LugarExpRetenc: Código postal del lugar de expedición (obligatorio)
        String lugarExpRetenc = codigoPostalEmisorDefault != null && !codigoPostalEmisorDefault.trim().isEmpty() 
                ? codigoPostalEmisorDefault.trim() 
                : "58000";
        
        xml.append("<retenciones:Retenciones xmlns:retenciones=\"http://www.sat.gob.mx/esquemas/retencionpago/2\" ")
           .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
           .append("xsi:schemaLocation=\"http://www.sat.gob.mx/esquemas/retencionpago/2 http://www.sat.gob.mx/esquemas/retencionpago/2/retencionpagov2.xsd\" ")
           .append("Version=\"2.0\" ")
           .append("FolioInt=\"").append(folio).append("\" ")
           .append("FechaExp=\"").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))).append("\" ")
           .append("CveRetenc=\"").append(claveRetencion).append("\" ")
           .append("DescRetenc=\"").append(escapeXml(concepto != null ? concepto : "")).append("\" ")
           .append("LugarExpRetenc=\"").append(lugarExpRetenc).append("\">\n");
        
        xml.append("  <retenciones:Emisor RfcE=\"").append(rfcEmisor).append("\" ")
           .append("NomDenRazSocE=\"").append(escapeXml(nombreEmisor)).append("\" ")
           .append("RegimenFiscalE=\"").append(regimenFiscalEmisor).append("\"/>\n");
        xml.append("  <retenciones:Receptor Nacionalidad=\"").append("Nacional").append("\" ")
           .append("RfcR=\"").append(rfcReceptor).append("\"/>\n");
        
        xml.append("  <retenciones:Periodo MesIni=\"").append(String.format("%02d", periodoMes)).append("\" ")
           .append("MesFin=\"").append(String.format("%02d", periodoMes)).append("\" ")
           .append("Ejerc=\"").append(periodoAnio).append("\"/>\n");
        
        // Totales según esquema oficial de retenciones
        // MontoTotOperacion: Monto total de la operación (base de retención)
        // MontoTotGrav: Monto total gravado
        // MontoTotExent: Monto total exento
        // MontoTotRet: Monto total retenido (ISR + IVA)
        BigDecimal montoTotGrav = montoBase != null ? montoBase : BigDecimal.ZERO;
        BigDecimal montoTotExent = BigDecimal.ZERO;
        BigDecimal montoTotRet = montoRetenido != null ? montoRetenido : BigDecimal.ZERO;
        
        xml.append("  <retenciones:Totales ")
           .append("MontoTotOperacion=\"").append(formatMonto(montoBase)).append("\" ")
           .append("MontoTotGrav=\"").append(formatMonto(montoTotGrav)).append("\" ")
           .append("MontoTotExent=\"").append(formatMonto(montoTotExent)).append("\" ")
           .append("MontoTotRet=\"").append(formatMonto(montoTotRet)).append("\">\n");
        
        // ImpRetenidos: Para cada tipo de impuesto retenido
        // BaseRet: Base de retención
        // ImpuestoRet: Código del impuesto (002=IVA, 001=ISR)
        // MontoRet: Monto retenido
        // TipoPagoRet: Tipo de pago (01=Definitivo, 02=Provisional)
        if (isrRetenido != null && isrRetenido.compareTo(BigDecimal.ZERO) > 0) {
            xml.append("    <retenciones:ImpRetenidos ")
               .append("BaseRet=\"").append(formatMonto(montoBase)).append("\" ")
               .append("ImpuestoRet=\"001\" ")  // 001 = ISR
               .append("MontoRet=\"").append(formatMonto(isrRetenido)).append("\" ")
               .append("TipoPagoRet=\"01\"/>\n");  // 01 = Definitivo
        }
        if (ivaRetenido != null && ivaRetenido.compareTo(BigDecimal.ZERO) > 0) {
            xml.append("    <retenciones:ImpRetenidos ")
               .append("BaseRet=\"").append(formatMonto(montoBase)).append("\" ")
               .append("ImpuestoRet=\"002\" ")  // 002 = IVA
               .append("MontoRet=\"").append(formatMonto(ivaRetenido)).append("\" ")
               .append("TipoPagoRet=\"01\"/>\n");  // 01 = Definitivo
        }
        xml.append("  </retenciones:Totales>\n");
        
        xml.append("</retenciones:Retenciones>");
        
        return xml.toString();
    }

    private String actualizarXmlConDatosPac(String xmlOriginal, PacTimbradoResponse pacResp,
                                           String uuid, LocalDateTime fechaTimbrado) {
        // Actualizar XML con UUID, sello digital y certificado del SAT
        if (pacResp == null) {
            return xmlOriginal;
        }
        // Por ahora retornamos el XML original, pero debería actualizarse con los datos del PAC
        return xmlOriginal;
    }

    private String formatMonto(BigDecimal monto) {
        return monto != null ? monto.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
    }

    public RetencionResponse enviarRetencionPorCorreo(com.cibercom.facturacion_back.dto.RetencionEnvioRequest request) {
        RetencionResponse response = new RetencionResponse();
        List<String> errores = new ArrayList<>();
        response.setErrors(errores);

        if (request == null) {
            errores.add("Solicitud vacía.");
            response.setSuccess(false);
            response.setMessage("Solicitud inválida.");
            return response;
        }

        String uuidRetencion = safeTrim(request.getUuidRetencion());
        if (uuidRetencion == null) {
            errores.add("El UUID de la retención es obligatorio.");
            response.setSuccess(false);
            response.setMessage("UUID de retención inválido.");
            return response;
        }

        String correo = safeTrim(request.getCorreoReceptor());
        if (correo == null || !correo.contains("@")) {
            errores.add("El correo receptor es obligatorio y debe tener un formato válido.");
            response.setSuccess(false);
            response.setMessage("Correo del receptor inválido.");
            return response;
        }

        try {
            // Obtener configuración de logo y color para el PDF
            Map<String, Object> logoConfig = obtenerLogoConfig();

            // Obtener PDF usando FacturaService (que ya maneja diferentes tipos de facturas)
            byte[] pdfBytes = facturaService.obtenerPdfComoBytes(uuidRetencion, logoConfig);
            
            if (pdfBytes == null || pdfBytes.length < 100) {
                errores.add("No se pudo generar el PDF de la retención.");
                response.setSuccess(false);
                response.setMessage("Error al generar PDF.");
                return response;
            }
            
            // Obtener XML de la retención desde la base de datos
            byte[] xmlBytes = null;
            String nombreXml = "Retencion-" + uuidRetencion + ".xml";
            try {
                Optional<UuidFacturaOracleDAO.Result> optRetencion = uuidFacturaOracleDAO.obtenerBasicosPorUuid(uuidRetencion);
                if (optRetencion.isPresent() && optRetencion.get().xmlContent != null && !optRetencion.get().xmlContent.trim().isEmpty()) {
                    xmlBytes = optRetencion.get().xmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    logger.info("XML de la retención obtenido desde BD. Tamaño: {} bytes", xmlBytes.length);
                } else {
                    logger.warn("No se encontró XML de la retención {} en la base de datos", uuidRetencion);
                }
            } catch (Exception e) {
                logger.error("Error al obtener XML de la retención desde BD: {}", e.getMessage(), e);
            }
            
            String asunto = "CFDI de Retención de Pagos - " + uuidRetencion;
            
            // Obtener descripción del tipo de retención
            String tipoRetencionDesc = obtenerDescripcionTipoRetencion(request.getTipoRetencion());
            
            // Construir mensaje descriptivo
            StringBuilder mensajeBuilder = new StringBuilder();
            mensajeBuilder.append("Estimado(a) ").append(defaultString(request.getNombreReceptor(), "Cliente")).append(",\n\n");
            mensajeBuilder.append("Por este medio le hacemos llegar el CFDI de Retención de Pagos correspondiente.\n\n");
            mensajeBuilder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            mensajeBuilder.append("INFORMACIÓN DEL COMPROBANTE\n");
            mensajeBuilder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            
            if (request.getSerieRetencion() != null && !request.getSerieRetencion().isBlank() && 
                request.getFolioRetencion() != null && !request.getFolioRetencion().isBlank()) {
                mensajeBuilder.append("Serie y Folio: ").append(request.getSerieRetencion()).append("-").append(request.getFolioRetencion()).append("\n");
            }
            mensajeBuilder.append("UUID: ").append(uuidRetencion).append("\n");
            
            if (request.getFechaTimbrado() != null && !request.getFechaTimbrado().isBlank()) {
                mensajeBuilder.append("Fecha de Timbrado: ").append(request.getFechaTimbrado()).append("\n");
            }
            
            mensajeBuilder.append("\n");
            mensajeBuilder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            mensajeBuilder.append("DATOS DE LA RETENCIÓN\n");
            mensajeBuilder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            
            mensajeBuilder.append("Tipo de Retención: ").append(tipoRetencionDesc).append("\n");
            mensajeBuilder.append("Monto Base: $").append(formatMonto(new BigDecimal(defaultString(request.getBaseRetencion(), "0.00")))).append("\n");
            mensajeBuilder.append("Monto Total Retenido: $").append(formatMonto(new BigDecimal(defaultString(request.getMontoRetenido(), "0.00")))).append("\n");
            
            mensajeBuilder.append("\n");
            mensajeBuilder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            mensajeBuilder.append("DATOS DEL EMISOR\n");
            mensajeBuilder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            mensajeBuilder.append("Razón Social: ").append(defaultString(request.getNombreEmisor(), "N/A")).append("\n");
            mensajeBuilder.append("RFC: ").append(defaultString(request.getRfcEmisor(), "N/A")).append("\n");
            
            mensajeBuilder.append("\n");
            mensajeBuilder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            mensajeBuilder.append("DATOS DEL RECEPTOR\n");
            mensajeBuilder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            mensajeBuilder.append("Razón Social: ").append(defaultString(request.getNombreReceptor(), "N/A")).append("\n");
            mensajeBuilder.append("RFC: ").append(defaultString(request.getRfcReceptor(), "N/A")).append("\n");
            
            mensajeBuilder.append("\n");
            mensajeBuilder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            mensajeBuilder.append("Se adjuntan los archivos PDF y XML del comprobante fiscal digital.\n\n");
            mensajeBuilder.append("Este es un correo generado automáticamente. Por favor, no responda a este mensaje.\n\n");
            mensajeBuilder.append("Atentamente,\n");
            mensajeBuilder.append(defaultString(request.getNombreEmisor(), "Sistema de Facturación"));
            
            String mensaje = mensajeBuilder.toString();
            
            // Enviar correo con PDF y XML como adjuntos
            if (xmlBytes != null && xmlBytes.length > 0) {
                correoService.enviarCorreoConAdjuntosDirecto(
                    correo,
                    asunto,
                    mensaje,
                    new HashMap<>(), // templateVars vacío
                    pdfBytes,
                    "Retencion-" + uuidRetencion + ".pdf",
                    xmlBytes,
                    nombreXml
                );
                logger.info("Correo enviado con PDF y XML adjuntos");
            } else {
                // Si no hay XML, enviar solo PDF
                correoService.enviarCorreoConPdfDirecto(correo, asunto, mensaje, pdfBytes, "Retencion-" + uuidRetencion + ".pdf");
                logger.warn("Correo enviado solo con PDF (XML no disponible)");
            }

            response.setSuccess(true);
            response.setMessage("Retención enviada por correo correctamente.");
            response.setUuidRetencion(uuidRetencion);
            response.setCorreoReceptor(correo);
            response.setRfcReceptor(defaultString(request.getRfcReceptor(), "XAXX010101000"));
            response.setRfcEmisor(defaultString(request.getRfcEmisor(), "AAA010101AAA"));
        } catch (Exception e) {
            logger.error("Error al enviar retención por correo", e);
            errores.add(e.getMessage());
            response.setSuccess(false);
            response.setMessage("Error al enviar retención por correo: " + e.getMessage());
        }

        return response;
    }

    private Map<String, Object> obtenerLogoConfig() {
        Map<String, Object> logoConfig = new HashMap<>();
        try {
            com.cibercom.facturacion_back.dto.ConfiguracionCorreoResponseDto configMensajes = 
                correoService.obtenerConfiguracionMensajes();
            if (configMensajes != null && configMensajes.getFormatoCorreo() != null) {
                String colorPrimario = configMensajes.getFormatoCorreo().getColorTexto();
                if (colorPrimario != null && !colorPrimario.isBlank()) {
                    Map<String, Object> customColors = new HashMap<>();
                    customColors.put("primary", colorPrimario.trim());
                    logoConfig.put("customColors", customColors);
                }
            }
        } catch (Exception e) {
            logger.warn("No se pudo obtener Configuración de Mensajes: {}", e.getMessage());
        }

        if (!logoConfig.containsKey("customColors")) {
            try {
                com.cibercom.facturacion_back.dto.FormatoCorreoDto formatoActivo = formatoCorreoService.obtenerConfiguracionActiva();
                if (formatoActivo != null && formatoActivo.getColorTexto() != null && !formatoActivo.getColorTexto().isBlank()) {
                    Map<String, Object> customColors = new HashMap<>();
                    customColors.put("primary", formatoActivo.getColorTexto().trim());
                    logoConfig.put("customColors", customColors);
                }
            } catch (Exception e) {
                logger.warn("No se pudo obtener formato de correo activo: {}", e.getMessage());
            }
        }

        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        String logoEndpoint = "http://localhost:" + port + "/api/logos/cibercom-png";
        logoConfig.put("logoUrl", logoEndpoint);

        return logoConfig;
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

    private static Map<String, String> crearMapaClavesRetencion() {
        Map<String, String> mapa = new HashMap<>();
        mapa.put("ISR_SERVICIOS", "25");
        mapa.put("ISR_ARRENDAMIENTO", "23");
        mapa.put("ISR_ENAJENACION", "24");
        mapa.put("ISR_REGALIAS", "26");
        mapa.put("ISR_SUELDOS", "27");
        mapa.put("IVA", "14");
        mapa.put("DIVIDENDOS", "28");
        mapa.put("INTERESES", "29");
        mapa.put("FIDEICOMISOS", "30");
        mapa.put("REMANENTE", "31");
        mapa.put("PLANES_RETIRO", "32");
        mapa.put("ENAJENACION_ACCIONES", "33");
        mapa.put("OTROS", "25");
        return mapa;
    }
}

