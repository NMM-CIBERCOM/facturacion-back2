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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * Endpoint de prueba para verificar que el servicio esté funcionando
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check solicitado para FacturaController");
        return ResponseEntity.ok("FacturaService funcionando correctamente");
    }
}
