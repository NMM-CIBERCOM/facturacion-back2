package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.FacturaRequest;
import com.cibercom.facturacion_back.dto.FacturaResponse;
import com.cibercom.facturacion_back.dto.ConsultaFacturaRequest;
import com.cibercom.facturacion_back.dto.ConsultaFacturaResponse;
import com.cibercom.facturacion_back.dto.PacTimbradoResponse;
import com.cibercom.facturacion_back.dto.FacturaFrontendRequest;
import com.cibercom.facturacion_back.model.Factura;
import com.cibercom.facturacion_back.model.FacturaMongo;
import com.cibercom.facturacion_back.repository.FacturaRepository;
import com.cibercom.facturacion_back.repository.FacturaMongoRepository;
import com.cibercom.facturacion_back.service.FacturaService;
import com.cibercom.facturacion_back.service.ConsultaFacturaService;
import com.cibercom.facturacion_back.service.FacturaTimbradoService;
import com.cibercom.facturacion_back.service.ITextPdfService;
import com.cibercom.facturacion_back.dto.CfdiConsultaResponse;
import com.cibercom.facturacion_back.service.CfdiXmlParserService;
import com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import com.cibercom.facturacion_back.service.FormatoCorreoService;
import com.cibercom.facturacion_back.dto.FormatoCorreoDto;
import com.cibercom.facturacion_back.service.CorreoService;
import com.cibercom.facturacion_back.dto.ConfiguracionCorreoResponseDto;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/factura")
@CrossOrigin(origins = "*")
public class FacturaController {

    private static final Logger logger = LoggerFactory.getLogger(FacturaController.class);

    @Autowired
    private FacturaService facturaService;

    @Autowired
    private ConsultaFacturaService consultaFacturaService;

    @Autowired
    private FacturaTimbradoService facturaTimbradoService;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private FacturaMongoRepository facturaMongoRepository;

    @Autowired
    private Environment environment;

    @Autowired
    private ITextPdfService iTextPdfService;

    @Autowired
    private CfdiXmlParserService cfdiXmlParserService;

    @Autowired
    private UuidFacturaOracleDAO uuidFacturaOracleDAO;

    @Autowired
    private FormatoCorreoService formatoCorreoService;

    @Autowired
    private CorreoService correoService;

    @PostMapping("/generar-pdf")
    public ResponseEntity<byte[]> generarPDF(@RequestBody Map<String, Object> request) {
        logger.info("Recibida solicitud de generación de PDF: {}", request);

        try {

            Map<String, Object> facturaData = (Map<String, Object>) request.get("facturaData");
            Map<String, Object> logoConfig = (Map<String, Object>) request.get("logoConfig");

            byte[] pdfBytes = iTextPdfService.generarPdfConLogo(facturaData, logoConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "factura.pdf");

            logger.info("PDF generado exitosamente con iText. Tamaño: {} bytes", pdfBytes.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            logger.error("Error generando PDF con iText", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/descargar-pdf/{uuid}")
    public ResponseEntity<byte[]> descargarPdfPorUuid(@PathVariable String uuid) {
        try {
            // Obtener color primario desde Configuración de Mensajes (consistente con email)
            String colorPrimario = null;
            try {
                ConfiguracionCorreoResponseDto configMensajes = correoService.obtenerConfiguracionMensajes();
                if (configMensajes != null && configMensajes.getFormatoCorreo() != null &&
                    configMensajes.getFormatoCorreo().getColorTexto() != null &&
                    !configMensajes.getFormatoCorreo().getColorTexto().isBlank()) {
                    colorPrimario = configMensajes.getFormatoCorreo().getColorTexto();
                }
            } catch (Exception e) {
                logger.warn("No se pudo obtener Configuración de Mensajes: {}", e.getMessage());
            }

            // Fallback al formato activo si no hay color en Configuración de Mensajes
            if (colorPrimario == null) {
                try {
                    FormatoCorreoDto formatoActivo = formatoCorreoService.obtenerConfiguracionActiva();
                    if (formatoActivo != null && formatoActivo.getColorTexto() != null && !formatoActivo.getColorTexto().isBlank()) {
                        colorPrimario = formatoActivo.getColorTexto();
                    }
                } catch (Exception e) {
                    logger.warn("No se pudo obtener formato de correo activo: {}", e.getMessage());
                }
            }

            Map<String, Object> logoConfig = new HashMap<>();
            Map<String, Object> customColors = new HashMap<>();
            if (colorPrimario != null) {
                customColors.put("primary", colorPrimario.trim());
            }
            logoConfig.put("customColors", customColors);

            // Intentar usar logoBase64 activo persistido
            String logoBase64Activo = leerLogoBase64Activo();
            if (logoBase64Activo != null && !logoBase64Activo.isBlank()) {
                logoConfig.put("logoBase64", logoBase64Activo.trim());
                logger.info("Usando logoBase64 activo para descarga de PDF");
            } else {
                // Fallback: usar el mismo endpoint PNG que en el correo
                String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8085"));
                String logoEndpoint = "http://localhost:" + port + "/api/logos/cibercom-png";
                logoConfig.put("logoUrl", logoEndpoint);
                logger.info("Logo para descarga de PDF (fallback URL): {}", logoEndpoint);
            }

            byte[] pdfBytes = facturaService.obtenerPdfComoBytes(uuid, logoConfig);

            // Construir nombre de archivo usando serie-folio si están disponibles
            String nombreArchivo = "Factura_" + uuid + ".pdf";
            try {
                Factura factura = facturaService.buscarPorUuid(uuid);
                if (factura != null && factura.getSerie() != null && factura.getFolio() != null) {
                    nombreArchivo = "Factura_" + factura.getSerie() + "-" + factura.getFolio() + ".pdf";
                }
            } catch (Exception ignored) {}

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", nombreArchivo);
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            logger.error("Error generando PDF por UUID", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/generar/frontend")
    public ResponseEntity<Map<String, Object>> generarDesdeFrontend(
            @Valid @RequestBody FacturaFrontendRequest request) {
        logger.info("Recibida solicitud de generación desde frontend: {}", request);

        try {
            Map<String, Object> result = facturaService.procesarFormularioFrontend(request);
            boolean exitoso = Boolean.TRUE.equals(result.get("exitoso"));
            if (exitoso) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("Error interno del servidor al procesar formulario frontend", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("exitoso", false);
            errorResponse.put("mensaje", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private String generarHTMLFactura(Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        String logoBase64 = (String) logoConfig.get("logoBase64");
        String logoUrl = (String) logoConfig.get("logoUrl");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>Factura</title>\n");
        html.append("<style>\n");
        html.append(".factura-container { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; }\n");
        html.append(".header { display: flex; justify-content: space-between; margin-bottom: 20px; }\n");
        html.append(".logo { max-height: 80px; max-width: 200px; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        html.append("<div class='factura-container'>\n");
        html.append("<div class='header'>\n");
        html.append("<div class='logo-section'>\n");

        if (logoBase64 != null && !logoBase64.isEmpty()) {
            html.append("<img src='data:image/svg+xml;base64,").append(logoBase64)
                    .append("' alt='Logo' class='logo' />\n");
        } else if (logoUrl != null && !logoUrl.isEmpty()) {
            html.append("<img src='").append(logoUrl).append("' alt='Logo' class='logo' />\n");
        }

        html.append("</div>\n");
        html.append("<div class='factura-info'>\n");
        html.append("<h1>FACTURA ELECTRÓNICA</h1>\n");
        html.append("<p>Serie-Folio: ").append(facturaData.get("serie")).append("-").append(facturaData.get("folio"))
                .append("</p>\n");
        html.append("<p>UUID: ").append(facturaData.get("uuid")).append("</p>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        html.append("<p>Emisor: ").append(facturaData.get("nombreEmisor")).append("</p>\n");
        html.append("<p>Receptor: ").append(facturaData.get("nombreReceptor")).append("</p>\n");
        html.append("<p>Total: $").append(facturaData.get("importe")).append("</p>\n");
        html.append("</div>\n");
        html.append("</body>\n</html>");

        return html.toString();
    }

    private String leerLogoBase64Activo() {
        try {
            java.nio.file.Path p = java.nio.file.Paths.get("config/logo-base64.txt");
            if (java.nio.file.Files.exists(p)) {
                String content = java.nio.file.Files.readString(p);
                if (content != null && !content.trim().isEmpty()) {
                    return content.trim();
                }
            }
        } catch (Exception e) {
            logger.warn("No se pudo leer logo activo para PDF: {}", e.getMessage());
        }
        return null;
    }

    @PostMapping("/generar")
    public ResponseEntity<FacturaResponse> generarFactura(
            @Valid @RequestBody FacturaRequest request) {

        logger.info("Recibida solicitud de generación de factura: {}", request);

        try {
            FacturaResponse response = facturaTimbradoService.iniciarTimbrado(request);

            logger.info("Respuesta del servicio: exitoso={}, mensaje={}", response.isExitoso(), response.getMensaje());

            if (response.isExitoso()) {
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Generación de factura fallida: {}", response.getMensaje());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Error interno del servidor al generar factura", e);
            FacturaResponse errorResponse = FacturaResponse.builder()
                    .exitoso(false)
                    .mensaje("Error interno del servidor: " + e.getMessage())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/generar/xml")
    public ResponseEntity<FacturaResponse> generarXmlFactura(
            @Valid @RequestBody FacturaRequest request) {

        logger.info("Recibida solicitud de generación de XML: {}", request);

        try {
            FacturaResponse response = facturaService.procesarFactura(request);

            logger.info("Respuesta del servicio XML: exitoso={}, mensaje={}", response.isExitoso(),
                    response.getMensaje());

            if (response.isExitoso()) {
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Generación de XML fallida: {}", response.getMensaje());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Error interno del servidor al generar XML", e);
            FacturaResponse errorResponse = FacturaResponse.builder()
                    .exitoso(false)
                    .mensaje("Error interno del servidor: " + e.getMessage())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping(value = "/generar/xml-to-xml", consumes = { "application/xml", "text/xml", "text/xml;charset=UTF-8",
            "application/xml;charset=UTF-8" }, produces = "application/xml")
    public ResponseEntity<String> generarXmlDesdeXml(@RequestBody String xmlRequest) {

        logger.info("Recibida solicitud XML: {}", xmlRequest);

        try {
            FacturaRequest request = facturaService.convertirXmlAFacturaRequest(xmlRequest);
            FacturaResponse response = facturaService.procesarFactura(request);

            if (response.isExitoso()) {
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.APPLICATION_XML)
                        .body(response.getXmlTimbrado());
            } else {
                String errorXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<error>\n" +
                        "  <mensaje>" + response.getMensaje() + "</mensaje>\n" +
                        "  <errores>" + (response.getErrores() != null ? response.getErrores() : "") + "</errores>\n" +
                        "</error>";
                return ResponseEntity.badRequest()
                        .contentType(org.springframework.http.MediaType.APPLICATION_XML)
                        .body(errorXml);
            }

        } catch (Exception e) {
            logger.error("Error procesando XML", e);
            String errorXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<error>\n" +
                    "  <mensaje>Error interno del servidor: " + e.getMessage() + "</mensaje>\n" +
                    "</error>";
            return ResponseEntity.internalServerError()
                    .contentType(org.springframework.http.MediaType.APPLICATION_XML)
                    .body(errorXml);
        }
    }

    public static class TimbradoCallback {
        public String uuid;
        public String status;
        public String xmlTimbrado;
        public String cadenaOriginal;
        public String selloDigital;
        public String certificado;
        public String folioFiscal;
        public String serie;
        public String folio;
        public String fechaTimbrado;
    }

    @PostMapping("/timbrado/callback")
    public ResponseEntity<FacturaResponse> timbradoCallback(@RequestBody TimbradoCallback cb) {
        if (cb == null || cb.uuid == null || cb.uuid.isBlank() || cb.status == null) {
            return ResponseEntity.badRequest().body(FacturaResponse.builder()
                    .exitoso(false)
                    .mensaje("Callback inválido")
                    .timestamp(java.time.LocalDateTime.now())
                    .build());
        }

        try {
            logger.info("🔄 Recibido callback automático del PAC - UUID: {}, Status: {}", cb.uuid, cb.status);

            if ("0".equals(cb.status)) {

                PacTimbradoResponse pacResponse = PacTimbradoResponse.builder()
                        .ok(true)
                        .status(cb.status)
                        .uuid(cb.uuid)
                        .xmlTimbrado(cb.xmlTimbrado)
                        .cadenaOriginal(cb.cadenaOriginal)
                        .selloDigital(cb.selloDigital)
                        .certificado(cb.certificado)
                        .folioFiscal(cb.folioFiscal)
                        .serie(cb.serie)
                        .folio(cb.folio)
                        .fechaTimbrado(cb.fechaTimbrado != null ? java.time.LocalDateTime.parse(cb.fechaTimbrado)
                                : java.time.LocalDateTime.now())
                        .build();

                facturaTimbradoService.actualizarFacturaTimbrada(cb.uuid, pacResponse);
                logger.info("✅ Factura {} actualizada automáticamente a EMITIDA por callback del PAC", cb.uuid);

            } else if ("2".equals(cb.status)) {

                PacTimbradoResponse pacResponse = PacTimbradoResponse.builder()
                        .ok(false)
                        .status(cb.status)
                        .uuid(cb.uuid)
                        .message("Timbrado rechazado por SAT")
                        .build();

                facturaTimbradoService.actualizarFacturaRechazada(cb.uuid, pacResponse);
                logger.info("❌ Factura {} actualizada automáticamente a CANCELADA_SAT por callback del PAC", cb.uuid);

            } else {
                logger.warn("⚠️ Status desconocido en callback: {} para UUID: {}", cb.status, cb.uuid);
            }

            return ResponseEntity.ok(FacturaResponse.builder()
                    .exitoso(true)
                    .mensaje("Factura actualizada automáticamente por callback del PAC")
                    .timestamp(java.time.LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            logger.error("❌ Error procesando callback automático del PAC: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(FacturaResponse.builder()
                    .exitoso(false)
                    .mensaje("Error al procesar callback automático: " + e.getMessage())
                    .timestamp(java.time.LocalDateTime.now())
                    .build());
        }
    }

    @GetMapping("/consultar-por-empresa")
    public ResponseEntity<ConsultaFacturaResponse> consultarTodasFacturas() {
        logger.info("Consultando todas las facturas");

        try {
            ConsultaFacturaRequest request = new ConsultaFacturaRequest();
            request.setRfcReceptor("TODAS"); // Valor especial para consultar todas

            ConsultaFacturaResponse response = consultaFacturaService.consultarFacturas(request);

            if (response.isExitoso()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Error al consultar todas las facturas", e);
            return ResponseEntity.internalServerError().body(
                    ConsultaFacturaResponse.error("Error interno del servidor: " + e.getMessage()));
        }
    }

    @GetMapping("/timbrado/status/{uuid}")
    public ResponseEntity<byte[]> consultarEstadoTimbrado(@PathVariable String uuid) {
        logger.info("🔍 Consultando estado de timbrado para UUID: {}", uuid);

        try {
            String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0]
                    : "oracle";

            String xmlContent = null;
            if ("mongo".equals(activeProfile)) {
                FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
                if (facturaMongo == null) {
                    logger.warn("Factura no encontrada en MongoDB para UUID: {}", uuid);
                    return ResponseEntity.notFound().build();
                }
                xmlContent = facturaMongo.getXmlContent();
            } else {

                java.util.Optional<com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO.Result> opt = uuidFacturaOracleDAO.obtenerBasicosPorUuid(uuid);
                if (!opt.isPresent()) {
                    logger.warn("Factura no encontrada en Oracle para UUID: {}", uuid);
                    return ResponseEntity.notFound().build();
                }
                xmlContent = opt.get().xmlContent;
            }

            if (xmlContent == null || xmlContent.isEmpty()) {
                logger.warn("XML no disponible para UUID: {} en perfil: {}", uuid, activeProfile);
                return ResponseEntity.notFound().build();
            }

            byte[] xmlBytes = xmlContent.getBytes("UTF-8");

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_XML);
            headers.setContentDispositionFormData("attachment", "FACTURA_" + uuid + ".xml");
            headers.setContentLength(xmlBytes.length);

            logger.info("XML descargado exitosamente para UUID: {} desde perfil: {}", uuid, activeProfile);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(xmlBytes);

        } catch (Exception e) {
            logger.error("Error al descargar XML para UUID: {}", uuid, e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/oracle/columns/{uuid}")
    public ResponseEntity<Map<String, Object>> consultarColumnasOracle(@PathVariable String uuid) {
        try {
            java.util.Optional<Factura> opt = facturaRepository.findByUuid(uuid);
            if (!opt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            Factura f = opt.get();
            Map<String, Object> r = new HashMap<>();
            r.put("uuid", f.getUuid());
            r.put("serie", f.getSerie());
            r.put("folio", f.getFolio());
            r.put("tiendaOrigen", f.getTiendaOrigen());
            r.put("terminalBol", f.getTerminalBol());
            r.put("boletaBol", f.getBoletaBol());
            r.put("tienda", f.getTienda());
            r.put("terminal", f.getTerminal());
            r.put("boleta", f.getBoleta());
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            logger.error("Error consultando columnas Oracle para UUID {}: {}", uuid, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "exitoso", false,
                "mensaje", "Error consultando columnas Oracle",
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check solicitado para FacturaController");
        return ResponseEntity.ok("FacturaService funcionando correctamente");
    }

    @PostMapping("/insertar-uuid")
    public ResponseEntity<Map<String, Object>> insertarUuid(@RequestBody Map<String, Object> req) {
        String uuid = req.get("uuid") != null ? String.valueOf(req.get("uuid")) : java.util.UUID.randomUUID().toString();
        String rfcReceptor = req.get("rfcReceptor") != null ? String.valueOf(req.get("rfcReceptor")) : "R";
        String rfcEmisor = req.get("rfcEmisor") != null ? String.valueOf(req.get("rfcEmisor")) : "R";
        String serie = req.get("serie") != null ? String.valueOf(req.get("serie")) : "PR";
        String folio = req.get("folio") != null ? String.valueOf(req.get("folio")) : "1";
        java.math.BigDecimal subtotal = new java.math.BigDecimal(String.valueOf(req.getOrDefault("subtotal", "100.00")));
        java.math.BigDecimal iva = new java.math.BigDecimal(String.valueOf(req.getOrDefault("iva", "16.00")));
        java.math.BigDecimal ieps = new java.math.BigDecimal(String.valueOf(req.getOrDefault("ieps", "0.00")));
        java.math.BigDecimal total = new java.math.BigDecimal(String.valueOf(req.getOrDefault("total", "116.00")));
        String formaPago = req.get("formaPago") != null ? String.valueOf(req.get("formaPago")) : "03";
        String usoCfdi = req.get("usoCfdi") != null ? String.valueOf(req.get("usoCfdi")) : "G03";
        String estado = req.get("estado") != null ? String.valueOf(req.get("estado")) : "TIMBRADA";
        String estadoDescripcion = req.get("estadoDescripcion") != null ? String.valueOf(req.get("estadoDescripcion")) : "VIGENTE";
        String medioPago = req.get("medioPago") != null ? String.valueOf(req.get("medioPago")) : "PUE";

        String xml = "<cfdi:Comprobante Serie='" + serie + "' Folio='" + folio + "' SubTotal='" + subtotal + "' Total='" + total + "' MetodoPago='" + medioPago + "' FormaPago='" + formaPago + "' xmlns:cfdi='http://www.sat.gob.mx/cfd/4'>" +
                "<cfdi:Receptor Rfc='" + rfcReceptor + "' UsoCFDI='" + usoCfdi + "'/></cfdi:Comprobante>";
        boolean ok = false;
        boolean oracleOk = false;
        try {
            ok = uuidFacturaOracleDAO.insertarBasico(
                    uuid,
                    xml,
                    serie,
                    folio,
                    subtotal,
                    iva,
                    ieps,
                    total,
                    formaPago,
                    usoCfdi,
                    estado,
                    estadoDescripcion,
                    medioPago,
                    rfcReceptor,
                    rfcEmisor
            );
            oracleOk = ok;
        } catch (Exception e) {
            logger.warn("Fallo insertando en Oracle para UUID {}: {}", uuid, e.getMessage());
        }

        if (!ok) {
            try {
                com.cibercom.facturacion_back.model.FacturaMongo fm = new com.cibercom.facturacion_back.model.FacturaMongo();
                fm.setUuid(uuid);
                fm.setXmlContent(xml);
                fm.setSerie(serie);
                fm.setFolio(folio);
                fm.setSubtotal(subtotal);
                fm.setIva(iva);
                fm.setTotal(total);
                fm.setEstado("0");
                fm.setEstadoDescripcion(estadoDescripcion);
                facturaMongoRepository.save(fm);
                ok = true;
            } catch (Exception e) {
                logger.error("Fallo insertando en Mongo para UUID {}: {}", uuid, e.getMessage());
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("exitoso", ok);
        resp.put("uuid", uuid);
        resp.put("origen", oracleOk ? "oracle" : "mongo");
        resp.put("mensaje", ok ? "Insertado correctamente" : "No fue posible insertar en Oracle ni en Mongo");
        if (!oracleOk) {
            resp.put("oracleError", uuidFacturaOracleDAO.getLastInsertError());
        }
        return ok ? ResponseEntity.ok(resp) : ResponseEntity.status(500).body(resp);
    }

// Nuevo endpoint: consulta CFDI por UUID con validaciones y extracción por tipo
@GetMapping("/consultar-uuid")
public ResponseEntity<CfdiConsultaResponse> consultarPorUuid(
        @RequestParam String uuid,
        @RequestParam(required = false) String rfcReceptor,
        @RequestParam(defaultValue = "I") String tipo) {
    logger.info("Consulta CFDI por UUID: {} tipo: {}", uuid, tipo);
    try {
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "oracle";

        String xmlContent = null;
        String estadoCodigo = null;
        String estadoDescripcion = null;
        String serie = null;
        String folio = null;
        java.math.BigDecimal subtotalDb = null;
        java.math.BigDecimal ivaDb = null;
        java.math.BigDecimal totalDb = null;
        String metodoPagoDb = null;
        String formaPagoDb = null;
        String usoCfdiDb = null;
        java.math.BigDecimal descuentoDb = null;
        java.math.BigDecimal iepsDb = null;
        // Desgloses desde BD si existen
        java.math.BigDecimal iva16Db = null;
        java.math.BigDecimal iva8Db = null;
        java.math.BigDecimal iva0Db = null;
        java.math.BigDecimal ivaExentoDb = null;
        java.math.BigDecimal ieps26Db = null;
        java.math.BigDecimal ieps160Db = null;
        java.math.BigDecimal ieps8Db = null;
        java.math.BigDecimal ieps30Db = null;
        java.math.BigDecimal ieps304Db = null;
        java.math.BigDecimal ieps7Db = null;
        java.math.BigDecimal ieps53Db = null;
        java.math.BigDecimal ieps25Db = null;
        java.math.BigDecimal ieps6Db = null;
        java.math.BigDecimal ieps50Db = null;
        java.math.BigDecimal ieps9Db = null;
        java.math.BigDecimal ieps3Db = null;
        java.math.BigDecimal ieps43Db = null;

        // Intentar primero en Oracle; si no existe, caer a Mongo
        java.util.Optional<com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO.Result> opt = java.util.Optional.empty();
        try {
            opt = uuidFacturaOracleDAO.obtenerBasicosPorUuid(uuid);
        } catch (Exception e) {
            logger.warn("Fallo consultando Oracle por UUID {}: {}", uuid, e.getMessage());
        }
        if (opt.isPresent()) {
            com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO.Result r = opt.get();
            xmlContent = r.xmlContent;
            estadoCodigo = r.estadoCodigo;
            estadoDescripcion = r.estadoDescripcion;
            serie = r.serie;
            folio = r.folio;
            subtotalDb = r.subtotal;
            ivaDb = r.iva;
            totalDb = r.total;
            metodoPagoDb = r.metodoPago;
            formaPagoDb = r.formaPago;
            usoCfdiDb = r.usoCfdi;
            descuentoDb = r.descuento;
            iepsDb = r.ieps;
            // Desgloses
            iva16Db = r.iva16;
            iva8Db = r.iva8;
            iva0Db = r.iva0;
            ivaExentoDb = r.ivaExento;
            ieps26Db = r.ieps26;
            ieps160Db = r.ieps160;
            ieps8Db = r.ieps8;
            ieps30Db = r.ieps30;
            ieps304Db = r.ieps304;
            ieps7Db = r.ieps7;
            ieps53Db = r.ieps53;
            ieps25Db = r.ieps25;
            ieps6Db = r.ieps6;
            ieps50Db = r.ieps50;
            ieps9Db = r.ieps9;
            ieps3Db = r.ieps3;
            ieps43Db = r.ieps43;
        } else {
            com.cibercom.facturacion_back.model.FacturaMongo fm = null;
            try {
                fm = facturaMongoRepository.findByUuid(uuid);
            } catch (Exception e) {
                logger.warn("Fallo consultando Mongo por UUID {}: {}", uuid, e.getMessage());
            }
            if (fm == null) {
                return ResponseEntity.ok(CfdiConsultaResponse.builder()
                        .exitoso(false)
                        .mensaje("UUID no encontrado")
                        .uuid(uuid)
                        .estado("DESCONOCIDO")
                        .build());
            }
            xmlContent = fm.getXmlContent();
            estadoCodigo = fm.getEstado();
            estadoDescripcion = fm.getEstadoDescripcion();
            serie = fm.getSerie();
            folio = fm.getFolio();
            subtotalDb = fm.getSubtotal();
            ivaDb = fm.getIva();
            totalDb = fm.getTotal();
        }

        if (xmlContent == null || xmlContent.isEmpty()) {
            return ResponseEntity.ok(CfdiConsultaResponse.builder()
                    .exitoso(false)
                    .mensaje("XML no disponible para el UUID")
                    .uuid(uuid)
                    .estado("DESCONOCIDO")
                    .build());
        }

        String estadoMsg = obtenerMensajeEstado(estadoCodigo);
        String estado = (estadoDescripcion != null && !estadoDescripcion.isEmpty()) ? estadoDescripcion : estadoMsg;
        boolean cancelado = estado != null && estado.toUpperCase().contains("CANCEL");

        // RFC Receptor desde XML
        String rfcXml = cfdiXmlParserService.parseRfcReceptor(xmlContent);
        if (rfcReceptor != null && !rfcReceptor.isEmpty()) {
            if (rfcXml == null || !rfcXml.equalsIgnoreCase(rfcReceptor)) {
                return ResponseEntity.ok(CfdiConsultaResponse.builder()
                        .exitoso(false)
                        .mensaje("El RFC receptor no coincide con el CFDI consultado")
                        .uuid(uuid)
                        .estado(estado != null ? estado : "DESCONOCIDO")
                        .rfcReceptor(rfcXml)
                        .build());
            }
        }

        // Datos básicos
        CfdiConsultaResponse.Basicos basicos = cfdiXmlParserService.parseBasicos(xmlContent);
        // Fallback serie/folio y totales desde BD si el XML no los trae
        if (basicos.getSerie() == null) basicos.setSerie(serie);
        if (basicos.getFolio() == null) basicos.setFolio(folio);
        if (basicos.getSubtotal() == null) basicos.setSubtotal(subtotalDb);
        if (basicos.getDescuento() == null) basicos.setDescuento(descuentoDb);
        if (basicos.getIva() == null) basicos.setIva(ivaDb);
        if (basicos.getIeps() == null) basicos.setIeps(iepsDb);
        if (basicos.getTotal() == null) basicos.setTotal(totalDb);
        if (basicos.getMetodoPago() == null) basicos.setMetodoPago(metodoPagoDb);
        if (basicos.getFormaPago() == null) basicos.setFormaPago(formaPagoDb);
        if (basicos.getUsoCfdi() == null) basicos.setUsoCfdi(usoCfdiDb);

        // Relacionados (para I/E y también útil si fue sustituido)
        CfdiConsultaResponse.Relacionados relacionados = cfdiXmlParserService.parseRelacionados(xmlContent);

        // Complemento de pago (sólo si tipo=P)
        CfdiConsultaResponse.Pago pago = null;
        if ("P".equalsIgnoreCase(tipo)) {
            pago = cfdiXmlParserService.parseComplementoPago(xmlContent);
        }

        return ResponseEntity.ok(CfdiConsultaResponse.builder()
                .exitoso(!cancelado)
                .mensaje(cancelado ? "CFDI cancelado" : "CFDI vigente")
                .uuid(uuid)
                .estado(estado != null ? estado : (cancelado ? "CANCELADO" : "VIGENTE"))
                .rfcReceptor(rfcXml)
                .basicos(basicos)
                .relacionados(relacionados)
                .pago(pago)
                .build());

    } catch (Exception e) {
        logger.error("Error consultando CFDI por UUID {}", uuid, e);
        return ResponseEntity.internalServerError().body(
                CfdiConsultaResponse.builder()
                        .exitoso(false)
                        .mensaje("Error interno: " + e.getMessage())
                        .uuid(uuid)
                        .estado("ERROR")
                        .build()
        );
    }
}

    private String obtenerMensajeEstado(String estadoCodigo) {
        if (estadoCodigo == null || estadoCodigo.trim().isEmpty()) {
            return "DESCONOCIDO";
        }
        String codigo = estadoCodigo.trim();
        // Intentar por código numérico
        try {
            com.cibercom.facturacion_back.model.EstadoFactura estado = com.cibercom.facturacion_back.model.EstadoFactura.fromCodigo(codigo);
            return estado.getDescripcion();
        } catch (IllegalArgumentException ignored) { }
        // Intentar por descripción exacta
        try {
            com.cibercom.facturacion_back.model.EstadoFactura estado = com.cibercom.facturacion_back.model.EstadoFactura.fromDescripcion(codigo);
            return estado.getDescripcion();
        } catch (IllegalArgumentException ignored) { }
        // Normalizar algunas variantes comunes
        String upper = codigo.toUpperCase();
        switch (upper) {
            case "VIGENTE":
                return "VIGENTE";
            case "CANCELADA":
                return "CANCELADA";
            case "ACTIVA":
                return "EMITIDA";
            case "EMITIDA":
                return "EMITIDA";
            case "EN PROCESO DE CANCELACION":
                return "EN PROCESO DE CANCELACION";
            case "EN PROCESO DE EMISION":
            case "EN PROCESO EMISION":
                return "EN PROCESO DE EMISION";
            case "CANCELADA EN SAT":
                return "CANCELADA EN SAT";
            case "EN ESPERA DE CANCELACION BOLETA QUE SUSTITUYE":
                return "EN ESPERA DE CANCELACION BOLETA QUE SUSTITUYE";
            default:
                // Retornar tal cual para que el flujo no falle; el consumo downstream maneja cancelado por substring
                return codigo;
        }
    }
}
