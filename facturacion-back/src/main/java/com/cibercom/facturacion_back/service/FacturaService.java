package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.FacturaRequest;
import com.cibercom.facturacion_back.dto.FacturaResponse;
import com.cibercom.facturacion_back.dto.FacturaFrontendRequest;
import com.cibercom.facturacion_back.dto.SatValidationRequest;
import com.cibercom.facturacion_back.dto.SatValidationResponse;
import com.cibercom.facturacion_back.model.Factura;
import com.cibercom.facturacion_back.model.FacturaMongo;
import com.cibercom.facturacion_back.repository.FacturaRepository;
import com.cibercom.facturacion_back.repository.FacturaMongoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FacturaService {

    @Autowired
    private SatValidationService satValidationService;
    
    @Autowired
    private FacturaRepository facturaRepository;
    
    @Autowired
    private FacturaMongoRepository facturaMongoRepository;

    /**
     * Genera y timbra una factura según los lineamientos del SAT
     * AHORA TAMBIÉN GUARDA EN BASE DE DATOS
     */
    public FacturaResponse generarYTimbrarFactura(FacturaRequest request) {
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
            
            // 5. Simular timbrado (en ambiente real se conectaría con PAC)
            String uuid = simularTimbrado();
            
            // 6. GUARDAR EN BASE DE DATOS (NUEVO)
            guardarFacturaEnBD(request, xml, uuid, subtotal, iva, total);
            
            // 7. Construir respuesta
            return FacturaResponse.builder()
                    .exitoso(true)
                    .mensaje("Factura generada, timbrada y GUARDADA en BD exitosamente")
                    .timestamp(LocalDateTime.now())
                    .uuid(uuid)
                    .xmlTimbrado(xml)
                    .datosFactura(construirDatosFactura(request, subtotal, iva, total, uuid))
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
    public Map<String, Object> procesarFormularioFrontend(FacturaFrontendRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. Validar datos del emisor con SAT
            SatValidationRequest emisorRequest = new SatValidationRequest();
            emisorRequest.setNombre(request.getRazonSocial());
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
            
            // 2. Generar XML de la factura
            String xml = generarXMLDesdeFrontend(request);
            
            // 3. Generar UUID único
            String uuid = UUID.randomUUID().toString().toUpperCase();
            
            // 4. Calcular totales (ejemplo básico)
            BigDecimal subtotal = new BigDecimal("1000.00"); // Valor por defecto
            BigDecimal iva = subtotal.multiply(new BigDecimal("0.16"));
            BigDecimal total = subtotal.add(iva);
            
            // 5. Guardar en Oracle (siempre)
            Factura facturaOracle = guardarEnOracle(request, xml, uuid, subtotal, iva, total);
            
            // 6. Guardar en MongoDB (siempre)
            guardarEnMongo(request, xml, uuid, subtotal, iva, total);
            
            // 7. Construir respuesta exitosa
            response.put("exitoso", true);
            response.put("mensaje", "Factura procesada y guardada exitosamente");
            response.put("uuid", uuid);
            response.put("xmlGenerado", xml);
            response.put("datosFactura", construirDatosFacturaFrontend(request, subtotal, iva, total, uuid));
            
        } catch (Exception e) {
            response.put("exitoso", false);
            response.put("mensaje", "Error al procesar la factura");
            response.put("error", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Genera el XML de la factura según los lineamientos del SAT
     */
    private String generarXMLFactura(FacturaRequest request, BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
        StringBuilder xml = new StringBuilder();
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/3\" ");
        xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        xml.append("xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/3 http://www.sat.gob.mx/sitio_internet/cfd/3/cfdv33.xsd\" ");
        xml.append("Version=\"3.3\" ");
        xml.append("Fecha=\"").append(fechaActual).append("\" ");
        xml.append("Sello=\"\" ");
        xml.append("FormaPago=\"").append(request.getFormaPago()).append("\" ");
        xml.append("NoCertificado=\"\" ");
        xml.append("Certificado=\"\" ");
        xml.append("CondicionesDePago=\"\" ");
        xml.append("SubTotal=\"").append(subtotal).append("\" ");
        xml.append("Moneda=\"MXN\" ");
        xml.append("TipoCambio=\"1\" ");
        xml.append("Total=\"").append(total).append("\" ");
        xml.append("TipoDeComprobante=\"I\" ");
        xml.append("Exportacion=\"01\" ");
        xml.append("MetodoPago=\"").append(request.getMetodoPago()).append("\" ");
        xml.append("LugarExpedicion=\"").append(request.getCodigoPostalEmisor()).append("\">\n");
        
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
                                                             String uuid) {
        return FacturaResponse.DatosFactura.builder()
                .folioFiscal(uuid)
                .serie("A")
                .folio("1")
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
     * Genera XML desde el formulario del frontend
     */
    private String generarXMLDesdeFrontend(FacturaFrontendRequest request) {
        StringBuilder xml = new StringBuilder();
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/3\" ");
        xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        xml.append("xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/3 http://www.sat.gob.mx/sitio_internet/cfd/3/cfdv33.xsd\" ");
        xml.append("Version=\"3.3\" ");
        xml.append("Fecha=\"").append(fechaActual).append("\" ");
        xml.append("Sello=\"\" ");
        xml.append("FormaPago=\"").append(request.getFormaPago()).append("\" ");
        xml.append("NoCertificado=\"\" ");
        xml.append("Certificado=\"\" ");
        xml.append("CondicionesDePago=\"\" ");
        xml.append("SubTotal=\"1000.00\" ");
        xml.append("Moneda=\"MXN\" ");
        xml.append("TipoCambio=\"1\" ");
        xml.append("Total=\"1160.00\" ");
        xml.append("TipoDeComprobante=\"I\" ");
        xml.append("Exportacion=\"01\" ");
        xml.append("MetodoPago=\"").append(request.getMedioPago()).append("\" ");
        xml.append("LugarExpedicion=\"00000\">\n");
        
        // Emisor
        xml.append("  <cfdi:Emisor ");
        xml.append("Rfc=\"").append(request.getRfc()).append("\" ");
        xml.append("Nombre=\"").append(request.getRazonSocial()).append("\" ");
        xml.append("RegimenFiscal=\"").append(request.getRegimenFiscal()).append("\"/>\n");
        
        // Receptor
        xml.append("  <cfdi:Receptor ");
        xml.append("Rfc=\"").append(request.getRfc()).append("\" ");
        xml.append("Nombre=\"").append(request.getRazonSocial()).append("\" ");
        xml.append("DomicilioFiscalReceptor=\"00000\" ");
        xml.append("RegimenFiscalReceptor=\"").append(request.getRegimenFiscal()).append("\" ");
        xml.append("UsoCFDI=\"").append(request.getUsoCfdi()).append("\"/>\n");
        
        // Conceptos
        xml.append("  <cfdi:Conceptos>\n");
        xml.append("    <cfdi:Concepto ");
        xml.append("ClaveProdServ=\"01010101\" ");
        xml.append("NoIdentificacion=\"\" ");
        xml.append("Cantidad=\"1\" ");
        xml.append("ClaveUnidad=\"H87\" ");
        xml.append("Unidad=\"Hora\" ");
        xml.append("Descripcion=\"Servicio de Facturación\" ");
        xml.append("ValorUnitario=\"1000.00\" ");
        xml.append("Importe=\"1000.00\" ");
        xml.append("Descuento=\"0.00\">\n");
        
        xml.append("      <cfdi:Impuestos>\n");
        xml.append("        <cfdi:Traslados>\n");
        xml.append("          <cfdi:Traslado ");
        xml.append("Base=\"1000.00\" ");
        xml.append("Impuesto=\"002\" ");
        xml.append("TipoFactor=\"Tasa\" ");
        xml.append("TasaOCuota=\"0.160000\" ");
        xml.append("Importe=\"160.00\"/>\n");
        xml.append("        </cfdi:Traslados>\n");
        xml.append("      </cfdi:Impuestos>\n");
        xml.append("    </cfdi:Concepto>\n");
        xml.append("  </cfdi:Conceptos>\n");
        
        // Impuestos
        xml.append("  <cfdi:Impuestos ");
        xml.append("TotalImpuestosTrasladados=\"160.00\">\n");
        xml.append("    <cfdi:Traslados>\n");
        xml.append("      <cfdi:Traslado ");
        xml.append("Impuesto=\"002\" ");
        xml.append("TasaOCuota=\"0.160000\" ");
        xml.append("Importe=\"160.00\"/>\n");
        xml.append("    </cfdi:Traslados>\n");
        xml.append("  </cfdi:Impuestos>\n");
        
        xml.append("</cfdi:Comprobante>");
        
        return xml.toString();
    }
    
    /**
     * Guarda la factura en Oracle
     */
    private Factura guardarEnOracle(FacturaFrontendRequest request, String xml, String uuid, 
                                   BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
        
        Factura factura = Factura.builder()
                .uuid(uuid)
                .xmlContent(xml)
                .fechaGeneracion(LocalDateTime.now())
                .fechaTimbrado(LocalDateTime.now())
                .estado("TIMBRADA")
                .serie("A")
                .folio("1")
                .cadenaOriginal("Simulada para ambiente de pruebas")
                .selloDigital("Simulado para ambiente de pruebas")
                .certificado("Simulado para ambiente de pruebas")
                // Datos del Emisor
                .emisorRfc(request.getRfc())
                .emisorRazonSocial(request.getRazonSocial())
                .emisorNombre(request.getNombre())
                .emisorPaterno(request.getPaterno())
                .emisorMaterno(request.getMaterno())
                .emisorCorreo(request.getCorreoElectronico())
                .emisorPais(request.getPais())
                .emisorDomicilioFiscal(request.getDomicilioFiscal())
                .emisorRegimenFiscal(request.getRegimenFiscal())
                // Datos del Receptor (usando datos del emisor como ejemplo)
                .receptorRfc(request.getRfc())
                .receptorRazonSocial(request.getRazonSocial())
                .receptorNombre(request.getNombre())
                .receptorPaterno(request.getPaterno())
                .receptorMaterno(request.getMaterno())
                .receptorCorreo(request.getCorreoElectronico())
                .receptorPais(request.getPais())
                .receptorDomicilioFiscal(request.getDomicilioFiscal())
                .receptorRegimenFiscal(request.getRegimenFiscal())
                .receptorUsoCfdi(request.getUsoCfdi())
                // Datos de la Factura
                .codigoFacturacion(request.getCodigoFacturacion())
                .tienda(request.getTienda())
                .fechaFactura(LocalDateTime.now())
                .terminal(request.getTerminal())
                .boleta(request.getBoleta())
                .medioPago(request.getMedioPago())
                .formaPago(request.getFormaPago())
                .iepsDesglosado(request.getIepsDesglosado())
                // Totales
                .subtotal(subtotal)
                .iva(iva)
                .ieps(BigDecimal.ZERO)
                .total(total)
                .build();
        
        return facturaRepository.save(factura);
    }
    
    /**
     * Guarda la factura en MongoDB
     */
    private void guardarEnMongo(FacturaFrontendRequest request, String xml, String uuid, 
                               BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
        
        // Crear mapas para emisor y receptor
        Map<String, Object> emisor = new HashMap<>();
        emisor.put("rfc", request.getRfc());
        emisor.put("razonSocial", request.getRazonSocial());
        emisor.put("nombre", request.getNombre());
        emisor.put("paterno", request.getPaterno());
        emisor.put("materno", request.getMaterno());
        emisor.put("correo", request.getCorreoElectronico());
        emisor.put("pais", request.getPais());
        emisor.put("domicilioFiscal", request.getDomicilioFiscal());
        emisor.put("regimenFiscal", request.getRegimenFiscal());
        
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
                .fechaTimbrado(LocalDateTime.now())
                .estado("TIMBRADA")
                .serie("A")
                .folio("1")
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
                .iepsDesglosado(request.getIepsDesglosado())
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
                                   BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
        
        // Crear entidad Factura para Oracle
        Factura facturaOracle = Factura.builder()
                .uuid(uuid)
                .xmlContent(xml)
                .fechaGeneracion(LocalDateTime.now())
                .fechaTimbrado(LocalDateTime.now())
                .estado("TIMBRADA")
                .serie("A")
                .folio("1")
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
        
        // Guardar en MongoDB también
        guardarEnMongoDesdeFacturaRequest(request, xml, uuid, subtotal, iva, total);
    }
    
    /**
     * Guarda la factura en MongoDB desde FacturaRequest
     */
    private void guardarEnMongoDesdeFacturaRequest(FacturaRequest request, String xml, String uuid, 
                                                  BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
        
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
                .estado("TIMBRADA")
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
    
    /**
     * Construye los datos de la factura para la respuesta del frontend
     */
    private Map<String, Object> construirDatosFacturaFrontend(FacturaFrontendRequest request, 
                                                             BigDecimal subtotal, 
                                                             BigDecimal iva, 
                                                             BigDecimal total, 
                                                             String uuid) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("folioFiscal", uuid);
        datos.put("serie", "A");
        datos.put("folio", "1");
        datos.put("fechaTimbrado", LocalDateTime.now());
        datos.put("subtotal", subtotal);
        datos.put("iva", iva);
        datos.put("total", total);
        datos.put("cadenaOriginal", "Simulada para ambiente de pruebas");
        datos.put("selloDigital", "Simulado para ambiente de pruebas");
        datos.put("certificado", "Simulado para ambiente de pruebas");
        return datos;
    }

    /**
     * Consulta facturas por empresa/perfil con filtros opcionales
     */
    public Map<String, Object> consultarFacturasPorEmpresa(String rfcEmpresa, String tienda, String fechaInicio, String fechaFin) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Por ahora, simulamos datos de facturas
            // En implementación real, se consultaría la base de datos
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
            factura1.put("estado", "TIMBRADA");
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
            factura2.put("estado", "TIMBRADA");
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
            factura3.put("estado", "TIMBRADA");
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
} 