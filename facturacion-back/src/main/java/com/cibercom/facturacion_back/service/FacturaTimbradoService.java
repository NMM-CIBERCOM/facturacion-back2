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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FacturaTimbradoService {

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

    /**
     * Inicia el proceso de timbrado asíncrono
     */
    public FacturaResponse iniciarTimbrado(FacturaRequest request) {
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
            
            // 3. Calcular totales
            BigDecimal subtotal = BigDecimal.ZERO;
            for (FacturaRequest.Concepto concepto : request.getConceptos()) {
                subtotal = subtotal.add(concepto.getImporte());
            }
            
            BigDecimal iva = subtotal.multiply(new BigDecimal("0.16")); // 16% IVA
            BigDecimal total = subtotal.add(iva);
            
            // 4. Generar XML según lineamientos del SAT
            String xml = generarXMLFactura(request, subtotal, iva, total);
            
            // 5. Generar UUID temporal para seguimiento
            String uuidTemporal = UUID.randomUUID().toString().toUpperCase();
            
                         // 6. Guardar factura en estado POR_TIMBRAR
             guardarFacturaEnProceso(request, xml, uuidTemporal, subtotal, iva, total);
            
            // 7. Enviar solicitud de timbrado al PAC
            PacTimbradoRequest pacRequest = construirPacTimbradoRequest(request, xml, total, uuidTemporal);
            PacTimbradoResponse pacResponse = pacClient.solicitarTimbrado(pacRequest);
            
                         // 8. Procesar respuesta del PAC
             if (pacResponse != null && Boolean.TRUE.equals(pacResponse.getOk())) {
                 if ("0".equals(pacResponse.getStatus())) {
                     // Timbrado inmediato exitoso (EMITIDA)
                     actualizarFacturaTimbrada(uuidTemporal, pacResponse);
                     return construirRespuestaExitosa(request, subtotal, iva, total, pacResponse);
                 } else if ("4".equals(pacResponse.getStatus())) {
                     // Timbrado en proceso (asíncrono) - EN_PROCESO_EMISION
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
                     actualizarFacturaRechazada(uuidTemporal, pacResponse);
                     return FacturaResponse.builder()
                             .exitoso(false)
                             .mensaje("Timbrado rechazado por PAC")
                             .timestamp(LocalDateTime.now())
                             .errores(pacResponse.getMessage())
                             .build();
                 }
            } else {
                // Error en comunicación con PAC
                actualizarFacturaError(uuidTemporal, pacResponse);
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
      */
     @Transactional
     public void actualizarFacturaTimbrada(String uuid, PacTimbradoResponse pacResponse) {
         String activeProfile = environment.getActiveProfiles().length > 0 ? 
             environment.getActiveProfiles()[0] : "oracle";
         
         if ("mongo".equals(activeProfile)) {
             FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
             if (facturaMongo != null) {
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
             }
         } else {
             Factura factura = facturaRepository.findById(uuid).orElse(null);
             if (factura != null) {
                 factura.setEstado(EstadoFactura.EMITIDA.getCodigo());
                 factura.setEstadoDescripcion(EstadoFactura.EMITIDA.getDescripcion());
                 factura.setFechaTimbrado(pacResponse.getFechaTimbrado());
                 factura.setXmlContent(pacResponse.getXmlTimbrado());
                 factura.setCadenaOriginal(pacResponse.getCadenaOriginal());
                 factura.setSelloDigital(pacResponse.getSelloDigital());
                 factura.setCertificado(pacResponse.getCertificado());
                 factura.setSerie(pacResponse.getSerie());
                 factura.setFolio(pacResponse.getFolio());
                 facturaRepository.save(factura);
             }
         }
     }
    
    /**
     * Construye la solicitud para el PAC
     */
    private PacTimbradoRequest construirPacTimbradoRequest(FacturaRequest request, String xml, BigDecimal total, String uuid) {
        return PacTimbradoRequest.builder()
                .uuid(uuid) // Incluir el UUID generado por el backend
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
      * Guarda la factura en estado POR_TIMBRAR
      */
     @Transactional
     private void guardarFacturaEnProceso(FacturaRequest request, String xml, String uuid, 
                                        BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
         String activeProfile = environment.getActiveProfiles().length > 0 ? 
             environment.getActiveProfiles()[0] : "oracle";
         
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
         String activeProfile = environment.getActiveProfiles().length > 0 ? 
             environment.getActiveProfiles()[0] : "oracle";
         
         if ("mongo".equals(activeProfile)) {
             FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
             if (facturaMongo != null) {
                 facturaMongo.setEstado(EstadoFactura.EN_PROCESO_EMISION.getCodigo());
                 facturaMongo.setEstadoDescripcion(EstadoFactura.EN_PROCESO_EMISION.getDescripcion());
                 facturaMongoRepository.save(facturaMongo);
             }
         } else {
             Factura factura = facturaRepository.findById(uuid).orElse(null);
             if (factura != null) {
                 factura.setEstado(EstadoFactura.EN_PROCESO_EMISION.getCodigo());
                 factura.setEstadoDescripcion(EstadoFactura.EN_PROCESO_EMISION.getDescripcion());
                 facturaRepository.save(factura);
             }
         }
     }
    
             /**
     * Actualiza factura como rechazada
     */
    @Transactional
    public void actualizarFacturaRechazada(String uuid, PacTimbradoResponse pacResponse) {
         String activeProfile = environment.getActiveProfiles().length > 0 ? 
             environment.getActiveProfiles()[0] : "oracle";
         
         if ("mongo".equals(activeProfile)) {
             FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
             if (facturaMongo != null) {
                 facturaMongo.setEstado(EstadoFactura.CANCELADA_SAT.getCodigo());
                 facturaMongo.setEstadoDescripcion(EstadoFactura.CANCELADA_SAT.getDescripcion());
                 facturaMongoRepository.save(facturaMongo);
             }
         } else {
             Factura factura = facturaRepository.findById(uuid).orElse(null);
             if (factura != null) {
                 factura.setEstado(EstadoFactura.CANCELADA_SAT.getCodigo());
                 factura.setEstadoDescripcion(EstadoFactura.CANCELADA_SAT.getDescripcion());
                 facturaRepository.save(factura);
             }
         }
     }
    
         /**
      * Actualiza factura con error
      */
     @Transactional
     private void actualizarFacturaError(String uuid, PacTimbradoResponse pacResponse) {
         String activeProfile = environment.getActiveProfiles().length > 0 ? 
             environment.getActiveProfiles()[0] : "oracle";
         
         if ("mongo".equals(activeProfile)) {
             FacturaMongo facturaMongo = facturaMongoRepository.findByUuid(uuid);
             if (facturaMongo != null) {
                 facturaMongo.setEstado(EstadoFactura.FACTURA_TEMPORAL.getCodigo());
                 facturaMongo.setEstadoDescripcion(EstadoFactura.FACTURA_TEMPORAL.getDescripcion());
                 facturaMongoRepository.save(facturaMongo);
             }
         } else {
             Factura factura = facturaRepository.findById(uuid).orElse(null);
             if (factura != null) {
                 factura.setEstado(EstadoFactura.FACTURA_TEMPORAL.getCodigo());
                 factura.setEstadoDescripcion(EstadoFactura.FACTURA_TEMPORAL.getDescripcion());
                 facturaRepository.save(factura);
             }
         }
     }
    
    /**
     * Construye respuesta exitosa para timbrado inmediato
     */
    private FacturaResponse construirRespuestaExitosa(FacturaRequest request, BigDecimal subtotal, 
                                                    BigDecimal iva, BigDecimal total, PacTimbradoResponse pacResponse) {
        return FacturaResponse.builder()
                .exitoso(true)
                .mensaje("Factura timbrada exitosamente")
                .timestamp(LocalDateTime.now())
                .uuid(pacResponse.getUuid())
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
        xml.append("  <cfdi:Receptor ");
        xml.append("Rfc=\"").append(request.getRfcReceptor()).append("\" ");
        xml.append("Nombre=\"").append(request.getNombreReceptor()).append("\" ");
        xml.append("DomicilioFiscalReceptor=\"").append(request.getCodigoPostalReceptor()).append("\" ");
        xml.append("RegimenFiscalReceptor=\"").append(request.getRegimenFiscalReceptor()).append("\" ");
        xml.append("UsoCFDI=\"").append(request.getUsoCFDI()).append("\"/>\n");
        
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
}
