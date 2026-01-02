package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.ConsultaFacturaRequest;
import com.cibercom.facturacion_back.dto.ConsultaFacturaResponse;
import com.cibercom.facturacion_back.service.ConsultaFacturaService;
import com.cibercom.facturacion_back.service.FacturaService;
import com.cibercom.facturacion_back.service.FormatoCorreoService;
import com.cibercom.facturacion_back.service.CorreoService;
import com.cibercom.facturacion_back.dao.ConsultaFacturaDAO;
import com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO;
import com.cibercom.facturacion_back.dao.ConsultaXmlOracleDAO;
import com.cibercom.facturacion_back.dto.CancelFacturaRequest;
import com.cibercom.facturacion_back.dto.FormatoCorreoDto;
import com.cibercom.facturacion_back.dto.ConfiguracionCorreoResponseDto;
import com.cibercom.facturacion_back.model.FacturaMongo;
import com.cibercom.facturacion_back.repository.FacturaMongoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/consulta-facturas")
@CrossOrigin(origins = "*")
public class ConsultaFacturaController {

    private static final Logger logger = LoggerFactory.getLogger(ConsultaFacturaController.class);

    @Autowired
    private ConsultaFacturaService consultaFacturaService;
    @Autowired
    private ConsultaFacturaDAO consultaFacturaDAO;
    @Autowired
    private FacturaService facturaService;
    @Autowired
    private Environment environment;
    
    @Value("${server.base-url:http://localhost:8080}")
    private String serverBaseUrl;
    
    @Autowired(required = false)
    private UuidFacturaOracleDAO uuidFacturaOracleDAO;
    @Autowired(required = false)
    private ConsultaXmlOracleDAO consultaXmlOracleDAO;
    @Autowired
    private FormatoCorreoService formatoCorreoService;
    @Autowired
    private CorreoService correoService;
    @Autowired(required = false)
    private FacturaMongoRepository facturaMongoRepository;

    @PostMapping("/buscar")
    public ResponseEntity<ConsultaFacturaResponse> consultarFacturas(
            @Valid @RequestBody ConsultaFacturaRequest request) {

        logger.info("Recibida solicitud de consulta de facturas: {}", request);

        try {
            ConsultaFacturaResponse response = consultaFacturaService.consultarFacturas(request);

            logger.info("Respuesta del servicio: exitoso={}, mensaje={}", response.isExitoso(), response.getMensaje());

            if (response.isExitoso()) {
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Consulta fallida: {}", response.getMensaje());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Error interno del servidor al consultar facturas", e);
            ConsultaFacturaResponse errorResponse = ConsultaFacturaResponse.error(
                    "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    public static class CancelCallback {
        public String uuid;
        public String status;
    }

    @PostMapping("/cancelacion/callback")
    public ResponseEntity<ConsultaFacturaResponse> pacCallback(@RequestBody CancelCallback cb) {
        if (cb == null || cb.uuid == null || cb.uuid.isBlank() || cb.status == null) {
            return ResponseEntity.badRequest().body(ConsultaFacturaResponse.error("Callback inválido"));
        }
        boolean ok = consultaFacturaDAO.actualizarEstado(cb.uuid, cb.status.toUpperCase());
        if (ok)
            return ResponseEntity.ok(ConsultaFacturaResponse.exito(java.util.Collections.emptyList()));
        return ResponseEntity.badRequest().body(ConsultaFacturaResponse.error("No se actualizó estado en BD"));
    }

    @PostMapping("/cancelar")
    public ResponseEntity<ConsultaFacturaResponse> cancelarFactura(
            @RequestBody CancelFacturaRequest request) {
        logger.info("Recibida solicitud de cancelación: uuid={}, usuario={}", request.getUuid(), request.getUsuario());
        try {
            ConsultaFacturaResponse response = consultaFacturaService.cancelarFactura(request);
            if (response.isExitoso()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Error interno al cancelar factura", e);
            return ResponseEntity.internalServerError().body(
                    ConsultaFacturaResponse.error("Error interno del servidor: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check solicitado");
        return ResponseEntity.ok("ConsultaFacturaService funcionando correctamente");
    }

    @GetMapping("/descargar-pdf/{uuid}")
    public ResponseEntity<byte[]> descargarPdfPorUuid(@PathVariable String uuid) {
        try {
            logger.info("Descargando PDF para UUID: {}", uuid);
            
            // Obtener color primario desde Configuración de Mensajes
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
                String logoEndpoint = serverBaseUrl + "/api/logos/cibercom-png";
                logoConfig.put("logoUrl", logoEndpoint);
                logger.info("Logo para descarga de PDF (fallback URL): {}", logoEndpoint);
            }

            byte[] pdfBytes = facturaService.obtenerPdfComoBytes(uuid, logoConfig);

            // Construir nombre de archivo usando serie-folio si están disponibles
            String nombreArchivo = "Factura_" + uuid + ".pdf";
            try {
                com.cibercom.facturacion_back.model.Factura factura = facturaService.buscarPorUuid(uuid);
                if (factura != null && factura.getSerie() != null && factura.getFolio() != null) {
                    nombreArchivo = "Factura_" + factura.getSerie() + "-" + factura.getFolio() + ".pdf";
                }
            } catch (Exception ignored) {}

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", nombreArchivo);
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            logger.error("Error generando PDF por UUID: {}", uuid, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/timbrado/status/{uuid}")
    public ResponseEntity<byte[]> consultarEstadoTimbrado(@PathVariable String uuid) {
        logger.info("Consultando estado de timbrado para UUID: {}", uuid);

        try {
            String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0]
                    : "oracle";

            String xmlContent = null;
            if ("mongo".equals(activeProfile)) {
                if (facturaMongoRepository == null) {
                    logger.error("FacturaMongoRepository no está disponible para obtener XML del UUID: {}", uuid);
                    return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
                FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
                if (facturaMongo == null) {
                    logger.warn("Factura no encontrada en MongoDB para UUID: {}", uuid);
                    return ResponseEntity.notFound().build();
                }
                xmlContent = facturaMongo.getXmlContent();
            } else {
                // Verificar que uuidFacturaOracleDAO esté disponible
                if (uuidFacturaOracleDAO == null) {
                    logger.error("UuidFacturaOracleDAO no está disponible para obtener XML del UUID: {}", uuid);
                    return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
                }

                try {
                    Optional<UuidFacturaOracleDAO.Result> opt = uuidFacturaOracleDAO.obtenerBasicosPorUuid(uuid);
                    if (!opt.isPresent()) {
                        logger.warn("Factura no encontrada en Oracle para UUID: {}", uuid);
                        // No encontrada en FACTURAS: intentar CONSULTAS
                        if (consultaXmlOracleDAO != null) {
                            try {
                                Optional<String> xmlOpt = consultaXmlOracleDAO.obtenerXmlPorUuid(uuid);
                                if (xmlOpt.isPresent()) {
                                    xmlContent = xmlOpt.get();
                                }
                            } catch (Exception e) {
                                logger.error("Error al obtener XML desde CONSULTAS para UUID {}: {}", uuid, e.getMessage(), e);
                            }
                        }
                    } else {
                        xmlContent = opt.get().xmlContent;
                        // Si FACTURAS no tiene XML, intentar CONSULTAS como fallback
                        if (xmlContent == null || xmlContent.isEmpty()) {
                            if (consultaXmlOracleDAO != null) {
                                try {
                                    Optional<String> xmlOpt = consultaXmlOracleDAO.obtenerXmlPorUuid(uuid);
                                    if (xmlOpt.isPresent()) {
                                        xmlContent = xmlOpt.get();
                                    }
                                } catch (Exception e) {
                                    logger.error("Error al obtener XML desde CONSULTAS para UUID {}: {}", uuid, e.getMessage(), e);
                                }
                            }
                            // Fallback final: intentar construir un XML mínimo con datos disponibles
                            if (xmlContent == null || xmlContent.isEmpty()) {
                                try {
                                    String generado = construirXmlMinimo(uuid, opt.get());
                                    if (generado != null && !generado.isEmpty()) {
                                        xmlContent = generado;
                                    }
                                } catch (Exception e) {
                                    logger.error("Error al construir XML mínimo para UUID {}: {}", uuid, e.getMessage(), e);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error al obtener datos de Oracle para UUID {}: {}", uuid, e.getMessage(), e);
                    // Intentar obtener desde CONSULTAS como último recurso
                    if (consultaXmlOracleDAO != null) {
                        try {
                            Optional<String> xmlOpt = consultaXmlOracleDAO.obtenerXmlPorUuid(uuid);
                            if (xmlOpt.isPresent()) {
                                xmlContent = xmlOpt.get();
                            }
                        } catch (Exception e2) {
                            logger.error("Error al obtener XML desde CONSULTAS como último recurso para UUID {}: {}", uuid, e2.getMessage(), e2);
                        }
                    }
                }
            }

            if (xmlContent == null || xmlContent.isEmpty()) {
                logger.warn("XML no disponible para UUID: {} en perfil: {}", uuid, activeProfile);
                return ResponseEntity.notFound().build();
            }

            byte[] xmlBytes = xmlContent.getBytes("UTF-8");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDispositionFormData("attachment", "FACTURA_" + uuid + ".xml");
            headers.setContentLength(xmlBytes.length);

            logger.info("XML descargado exitosamente para UUID: {} desde perfil: {}", uuid, activeProfile);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(xmlBytes);

        } catch (NullPointerException e) {
            logger.error("Error NullPointerException al descargar XML para UUID {}: {}", uuid, e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Error inesperado al descargar XML para UUID {}: {}", uuid, e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String leerLogoBase64Activo() {
        try {
            // Usar la misma lógica que LogoController para encontrar el archivo
            java.nio.file.Path p = getLogoBase64Path();
            if (java.nio.file.Files.exists(p)) {
                String content = java.nio.file.Files.readString(p);
                if (content != null && !content.trim().isEmpty()) {
                    logger.info("Logo activo leído desde: {}", p.toAbsolutePath());
                    return content.trim();
                }
            } else {
                logger.warn("Archivo de logo no encontrado en: {}", p.toAbsolutePath());
            }
        } catch (Exception e) {
            logger.warn("No se pudo leer logo activo para PDF: {}", e.getMessage());
        }
        return null;
    }
    
    private java.nio.file.Path getLogoBase64Path() {
        // Intentar usar variable de entorno o propiedad del sistema
        String tomcatBase = System.getProperty("catalina.base");
        if (tomcatBase != null && !tomcatBase.isEmpty()) {
            // Si estamos en Tomcat, guardar en conf/ dentro de Tomcat
            return java.nio.file.Paths.get(tomcatBase, "conf", "logo-base64.txt");
        }
        
        // Fallback: usar directorio de trabajo actual/config
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isEmpty()) {
            return java.nio.file.Paths.get(userDir, "config", "logo-base64.txt");
        }
        
        // Último fallback: ruta relativa
        return java.nio.file.Paths.get("config", "logo-base64.txt");
    }

    private String construirXmlMinimo(String uuid, UuidFacturaOracleDAO.Result r) {
        try {
            String serie = r.serie != null ? r.serie : "";
            String folio = r.folio != null ? r.folio : "";
            String total = r.total != null ? r.total.toPlainString() : "0.00";
            
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                   "<cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\" " +
                   "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                   "xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/4 http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd\" " +
                   "Serie=\"" + serie + "\" " +
                   "Folio=\"" + folio + "\" " +
                   "Total=\"" + total + "\" " +
                   "UUID=\"" + uuid + "\">\n" +
                   "</cfdi:Comprobante>";
        } catch (Exception e) {
            logger.error("Error al construir XML mínimo para UUID {}: {}", uuid, e.getMessage(), e);
            return null;
        }
    }
}
