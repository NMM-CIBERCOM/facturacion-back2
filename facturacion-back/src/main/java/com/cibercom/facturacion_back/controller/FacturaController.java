package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.FacturaRequest;
import com.cibercom.facturacion_back.dto.FacturaResponse;
import com.cibercom.facturacion_back.dto.ConsultaFacturaRequest;
import com.cibercom.facturacion_back.dto.ConsultaFacturaResponse;
import com.cibercom.facturacion_back.dto.PacTimbradoResponse;
import com.cibercom.facturacion_back.model.Factura;
import com.cibercom.facturacion_back.model.FacturaMongo;
import com.cibercom.facturacion_back.repository.FacturaRepository;
import com.cibercom.facturacion_back.repository.FacturaMongoRepository;
import com.cibercom.facturacion_back.service.FacturaService;
import com.cibercom.facturacion_back.service.ConsultaFacturaService;
import com.cibercom.facturacion_back.service.FacturaTimbradoService;
import com.cibercom.facturacion_back.service.ITextPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

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
    
    /**
     * Endpoint para generar PDF de factura usando iText
     */
    @PostMapping("/generar-pdf")
    public ResponseEntity<byte[]> generarPDF(@RequestBody Map<String, Object> request) {
        logger.info("Recibida solicitud de generación de PDF: {}", request);
        
        try {
            // Extraer datos de la factura y configuración del logo
            Map<String, Object> facturaData = (Map<String, Object>) request.get("facturaData");
            Map<String, Object> logoConfig = (Map<String, Object>) request.get("logoConfig");
            
            // Generar PDF usando iText
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
    
    /**
     * Método auxiliar para generar HTML de la factura
     */
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
        
        // Usar logoBase64 si está disponible, sino usar logoUrl
        if (logoBase64 != null && !logoBase64.isEmpty()) {
            html.append("<img src='data:image/svg+xml;base64,").append(logoBase64).append("' alt='Logo' class='logo' />\n");
        } else if (logoUrl != null && !logoUrl.isEmpty()) {
            html.append("<img src='").append(logoUrl).append("' alt='Logo' class='logo' />\n");
        }
        
        html.append("</div>\n");
        html.append("<div class='factura-info'>\n");
        html.append("<h1>FACTURA ELECTRÓNICA</h1>\n");
        html.append("<p>Serie-Folio: ").append(facturaData.get("serie")).append("-").append(facturaData.get("folio")).append("</p>\n");
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
    
    /**
     * Endpoint para generar y timbrar una factura (asíncrono)
     */
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
    
    /**
     * Endpoint para generar solo el XML de una factura (sin timbrar) - Acepta JSON
     */
    @PostMapping("/generar/xml")
    public ResponseEntity<FacturaResponse> generarXmlFactura(
            @Valid @RequestBody FacturaRequest request) {
        
        logger.info("Recibida solicitud de generación de XML: {}", request);
        
        try {
            FacturaResponse response = facturaService.procesarFactura(request);
            
            logger.info("Respuesta del servicio XML: exitoso={}, mensaje={}", response.isExitoso(), response.getMensaje());
            
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
    
    /**
     * Endpoint para generar XML desde XML - Acepta XML y devuelve XML
     */
    @PostMapping(value = "/generar/xml-to-xml", consumes = {"application/xml", "text/xml", "text/xml;charset=UTF-8", "application/xml;charset=UTF-8"}, produces = "application/xml")
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
    
    /**
     * Webhook de PAC para actualizar estado final de timbrado
     */
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
                // Timbrado exitoso - EMITIDA
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
                    .fechaTimbrado(cb.fechaTimbrado != null ? 
                        java.time.LocalDateTime.parse(cb.fechaTimbrado) : java.time.LocalDateTime.now())
                    .build();
                
                facturaTimbradoService.actualizarFacturaTimbrada(cb.uuid, pacResponse);
                logger.info("✅ Factura {} actualizada automáticamente a EMITIDA por callback del PAC", cb.uuid);
                
            } else if ("2".equals(cb.status)) {
                // Timbrado rechazado - CANCELADA_SAT
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
    
    /**
     * Endpoint para consultar todas las facturas (sin filtros)
     */
    @GetMapping("/consultar-por-empresa")
    public ResponseEntity<ConsultaFacturaResponse> consultarTodasFacturas() {
        logger.info("Consultando todas las facturas");
        
        try {
            // Crear un request con un campo válido para pasar la validación
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
                ConsultaFacturaResponse.error("Error interno del servidor: " + e.getMessage())
            );
        }
    }
    
    /**
     * Endpoint para consultar estado de timbrado de una factura
     */
    @GetMapping("/timbrado/status/{uuid}")
    public ResponseEntity<FacturaResponse> consultarEstadoTimbrado(@PathVariable String uuid) {
        logger.info("🔍 Consultando estado de timbrado para UUID: {}", uuid);
        
        try {
            String activeProfile = environment.getActiveProfiles().length > 0 ? 
                environment.getActiveProfiles()[0] : "oracle";
            
            if ("mongo".equals(activeProfile)) {
                FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
                if (facturaMongo == null) {
                    return ResponseEntity.notFound().build();
                }
                
                String mensajeEstado = obtenerMensajeEstado(facturaMongo.getEstado());
                
                return ResponseEntity.ok(FacturaResponse.builder()
                    .exitoso(true)
                    .mensaje(mensajeEstado)
                    .timestamp(java.time.LocalDateTime.now())
                    .uuid(facturaMongo.getUuid())
                    .xmlTimbrado(facturaMongo.getXmlContent())
                    .datosFactura(FacturaResponse.DatosFactura.builder()
                        .folioFiscal(facturaMongo.getUuid())
                        .serie(facturaMongo.getSerie())
                        .folio(facturaMongo.getFolio())
                        .fechaTimbrado(facturaMongo.getFechaTimbrado())
                        .subtotal(facturaMongo.getSubtotal())
                        .iva(facturaMongo.getIva())
                        .total(facturaMongo.getTotal())
                        .cadenaOriginal(facturaMongo.getCadenaOriginal())
                        .selloDigital(facturaMongo.getSelloDigital())
                        .certificado(facturaMongo.getCertificado())
                        .build())
                    .build());
                    
            } else {
                Factura factura = facturaRepository.findById(uuid).orElse(null);
                if (factura == null) {
                    return ResponseEntity.notFound().build();
                }
                
                String mensajeEstado = obtenerMensajeEstado(factura.getEstado());
                
                return ResponseEntity.ok(FacturaResponse.builder()
                    .exitoso(true)
                    .mensaje(mensajeEstado)
                    .timestamp(java.time.LocalDateTime.now())
                    .uuid(factura.getUuid())
                    .xmlTimbrado(factura.getXmlContent())
                    .datosFactura(FacturaResponse.DatosFactura.builder()
                        .folioFiscal(factura.getUuid())
                        .serie(factura.getSerie())
                        .folio(factura.getFolio())
                        .fechaTimbrado(factura.getFechaTimbrado())
                        .subtotal(factura.getSubtotal())
                        .iva(factura.getIva())
                        .total(factura.getTotal())
                        .cadenaOriginal(factura.getCadenaOriginal())
                        .selloDigital(factura.getSelloDigital())
                        .certificado(factura.getCertificado())
                        .build())
                    .build());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error consultando estado de timbrado para UUID: {}", uuid, e);
            return ResponseEntity.internalServerError().body(FacturaResponse.builder()
                .exitoso(false)
                .mensaje("Error consultando estado: " + e.getMessage())
                .timestamp(java.time.LocalDateTime.now())
                .build());
        }
    }
    
    /**
     * Obtiene mensaje descriptivo del estado de la factura
     */
    private String obtenerMensajeEstado(String estado) {
        if (estado == null) {
            return "Estado no definido";
        }
        
        switch (estado) {
            case "66":
                return "Factura creada, pendiente de timbrado (POR_TIMBRAR)";
            case "4":
                return "Factura en proceso de emisión - El PAC consultará automáticamente al SAT en 60 segundos";
            case "0":
                return "Factura timbrada exitosamente (EMITIDA)";
            case "2":
                return "Factura cancelada por el SAT (CANCELADA_SAT)";
            case "99":
                return "Factura temporal - Error en comunicación";
            default:
                return "Estado: " + estado;
        }
    }
    
    /**
     * Endpoint para descargar el XML de una factura
     */
    @GetMapping("/descargar-xml/{uuid}")
    public ResponseEntity<byte[]> descargarXml(@PathVariable String uuid) {
        logger.info("Solicitud de descarga de XML para UUID: {}", uuid);
        
        try {
            String activeProfile = environment.getActiveProfiles().length > 0 ? 
                environment.getActiveProfiles()[0] : "oracle";
            
            String xmlContent = null;
            
            if ("mongo".equals(activeProfile)) {
                // Buscar en MongoDB
                FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
                if (facturaMongo == null) {
                    logger.warn("Factura no encontrada en MongoDB para UUID: {}", uuid);
                    return ResponseEntity.notFound().build();
                }
                xmlContent = facturaMongo.getXmlContent();
            } else {
                // Buscar en Oracle
                java.util.Optional<Factura> facturaOpt = facturaRepository.findByUuid(uuid);
                if (!facturaOpt.isPresent()) {
                    logger.warn("Factura no encontrada en Oracle para UUID: {}", uuid);
                    return ResponseEntity.notFound().build();
                }
                Factura factura = facturaOpt.get();
                xmlContent = factura.getXmlContent();
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
    
    /**
     * Endpoint de prueba para verificar que el servicio esté funcionando
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check solicitado para FacturaController");
        return ResponseEntity.ok("FacturaService funcionando correctamente");
    }
}
