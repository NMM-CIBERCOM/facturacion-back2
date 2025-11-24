package com.cibercom.facturacion_back.integration;

import com.cibercom.facturacion_back.dto.PacTimbradoRequest;
import com.cibercom.facturacion_back.dto.PacTimbradoResponse;
import com.cibercom.facturacion_back.service.CfdiFirmaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.xml.soap.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Cliente para timbrado de Retenciones con Finkok usando método stamp
 * Basado en documentación oficial: https://demo-facturacion.finkok.com/servicios/soap/retentions.wsdl
 */
@Component
public class RetentionsPacClient {
    private static final Logger logger = LoggerFactory.getLogger(RetentionsPacClient.class);
    
    @Value("${finkok.enabled:true}")
    private boolean finkokEnabled;
    
    @Value("${finkok.username:integrador@finkok.com}")
    private String finkokUsername;
    
    @Value("${finkok.password:Fin2023kok*}")
    private String finkokPassword;
    
    @Value("${finkok.retentions.url:https://demo-facturacion.finkok.com/servicios/soap/retentions}")
    private String finkokRetentionsUrl;
    
    private final CfdiFirmaService cfdiFirmaService;
    
    public RetentionsPacClient(org.springframework.beans.factory.ObjectProvider<CfdiFirmaService> cfdiFirmaServiceProvider) {
        this.cfdiFirmaService = cfdiFirmaServiceProvider.getIfAvailable();
        logger.info("RetentionsPacClient inicializado. Servicio de firma disponible: {}", this.cfdiFirmaService != null);
    }

    /**
     * Solicita el timbrado de una Retención usando el método stamp de Finkok
     * Según documentación oficial: https://demo-facturacion.finkok.com/servicios/soap/retentions.wsdl
     */
    public PacTimbradoResponse solicitarTimbrado(PacTimbradoRequest req) {
        try {
            logger.info("=== INICIANDO TIMBRADO DE RETENCIÓN CON FINKOK ===");
            logger.info("URL: {}, UUID: {}, Tipo: {}", finkokRetentionsUrl, req.getUuid(), req.getTipo());
            
            // 1. Obtener credenciales (del request o de configuración)
            String username = (req.getUsername() != null && !req.getUsername().isBlank()) 
                    ? req.getUsername() : finkokUsername;
            String password = (req.getPassword() != null && !req.getPassword().isBlank()) 
                    ? req.getPassword() : finkokPassword;
            
            // 2. Procesar XML
            String xmlContent = req.getXmlContent();
            if (xmlContent == null || xmlContent.isBlank()) {
                throw new IllegalArgumentException("XML content no puede estar vacío");
            }
            
            // Decodificar si viene en Base64
            String xmlPlainText;
            try {
                byte[] decoded = Base64.getDecoder().decode(xmlContent);
                xmlPlainText = new String(decoded, StandardCharsets.UTF_8);
                logger.info("XML recibido estaba en Base64, decodificado: {} caracteres", xmlPlainText.length());
            } catch (IllegalArgumentException e) {
                xmlPlainText = xmlContent;
                logger.info("XML recibido no estaba en Base64: {} caracteres", xmlPlainText.length());
            }
            
            // Validaciones básicas
            xmlPlainText = xmlPlainText.trim();
            if (!xmlPlainText.startsWith("<?xml")) {
                throw new IllegalArgumentException("El XML debe comenzar con <?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            }
            
            if (!finkokEnabled) {
                logger.warn("⚠️ PAC DESHABILITADO (finkok.enabled=false). Se omitirá el timbrado real y se devolverá un resultado simulado.");
                return crearRespuestaSimulada(req, xmlPlainText);
            }
            
            // 3. Firmar XML con CSD (las retenciones REQUIEREN firma digital)
            // CRÍTICO: Las retenciones deben estar firmadas antes de enviarlas a Finkok
            // Si no están firmadas, Finkok rechazará con error 302: "Sello mal formado o inválido"
            boolean esRetencion = xmlPlainText.contains("retenciones:Retenciones") || 
                                 xmlPlainText.contains("xmlns:retenciones") ||
                                 xmlPlainText.contains("<retenciones:Retenciones") ||
                                 xmlPlainText.contains("http://www.sat.gob.mx/esquemas/retencionpago/2");
            
            if (esRetencion) {
                logger.info("═══════════════════════════════════════════════════════════════");
                logger.info("📋 RETENCIÓN DETECTADA - FIRMANDO XML CON CSD");
                logger.info("═══════════════════════════════════════════════════════════════");
                
                if (cfdiFirmaService == null) {
                    logger.error("✗✗✗ ERROR CRÍTICO: CfdiFirmaService no está disponible");
                    logger.error("✗✗✗ Las retenciones REQUIEREN firma digital. Sin firma, Finkok rechazará con error 302");
                    logger.error("✗✗✗ SOLUCIÓN: Verifica que facturacion.csd.enabled=true en application.yml");
                    throw new IllegalStateException("CfdiFirmaService no está disponible. Las retenciones requieren firma digital.");
                }
                
                try {
                    String xmlAntesFirma = xmlPlainText;
                    logger.info("Intentando firmar XML de retención con CfdiFirmaService...");
                    xmlPlainText = cfdiFirmaService.firmarXml(xmlPlainText);
                    
                    if (xmlPlainText.equals(xmlAntesFirma)) {
                        logger.error("✗✗✗ ERROR CRÍTICO: El XML no se modificó después de intentar firmarlo");
                        logger.error("✗✗✗ Esto significa que CfdiFirmaService no pudo firmar el XML de retención");
                        logger.error("✗✗✗ Finkok rechazará este XML con error 302: 'Sello mal formado o inválido'");
                        logger.error("✗✗✗ SOLUCIÓN: Verifica que CfdiFirmaService soporte retenciones o implementa firma específica para retenciones");
                        throw new IllegalStateException("El XML de retención no se firmó. Las retenciones requieren firma digital antes del timbrado.");
                    } else {
                        logger.info("✓✓✓ XML de retención firmado exitosamente con CSD");
                        
                        // Verificar que se firmó correctamente
                        boolean tieneCertificado = xmlPlainText.contains("Certificado=\"") && 
                                                   !xmlPlainText.contains("Certificado=\"\"");
                        boolean tieneSello = xmlPlainText.contains("Sello=\"") && 
                                             !xmlPlainText.contains("Sello=\"\"");
                        boolean tieneNoCertificado = xmlPlainText.contains("NoCertificado=\"") && 
                                                     !xmlPlainText.contains("NoCertificado=\"\"");
                        
                        if (tieneCertificado && tieneSello && tieneNoCertificado) {
                            logger.info("✓ Verificación de firma: Certificado=✓, Sello=✓, NoCertificado=✓");
                        } else {
                            logger.error("✗✗✗ ADVERTENCIA: La firma puede estar incompleta");
                            logger.error("  Certificado presente: {}", tieneCertificado);
                            logger.error("  Sello presente: {}", tieneSello);
                            logger.error("  NoCertificado presente: {}", tieneNoCertificado);
                        }
                    }
                } catch (IllegalStateException e) {
                    // Re-lanzar excepciones de estado (ya tienen mensajes claros)
                    throw e;
                } catch (Exception e) {
                    logger.error("✗✗✗ ERROR al firmar XML de retención: {}", e.getMessage(), e);
                    logger.error("✗✗✗ Finkok rechazará este XML con error 302: 'Sello mal formado o inválido'");
                    throw new IllegalStateException("Error al firmar XML de retención: " + e.getMessage() + 
                        ". Las retenciones requieren firma digital antes del timbrado.", e);
                }
            } else {
                logger.warn("⚠️ No se detectó retención en el XML. Continuando sin verificar firma.");
            }
            
            // 4. Validar XML antes de enviar (extraer información para diagnóstico)
            String rfcEmisorEnviado = null;
            String nombreEmisorEnviado = null;
            String noCertificadoEnviado = null;
            
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlPlainText.getBytes(StandardCharsets.UTF_8)));
                
                // Extraer información del emisor para diagnóstico
                org.w3c.dom.NodeList emisoresList = doc.getElementsByTagNameNS("http://www.sat.gob.mx/esquemas/retencionpago/2", "Emisor");
                if (emisoresList.getLength() == 0) {
                    emisoresList = doc.getElementsByTagName("Emisor");
                }
                if (emisoresList.getLength() > 0) {
                    Element emisor = (Element) emisoresList.item(0);
                    rfcEmisorEnviado = emisor.getAttribute("RfcE");
                    nombreEmisorEnviado = emisor.getAttribute("NomDenRazSocE");
                    logger.info("═══════════════════════════════════════════════════════════════");
                    logger.info("📋 INFORMACIÓN DEL EMISOR EN EL XML DE RETENCIÓN:");
                    logger.info("  RFC: {}", rfcEmisorEnviado);
                    logger.info("  Nombre: {}", nombreEmisorEnviado);
                    logger.info("═══════════════════════════════════════════════════════════════");
                }
                
                // Verificar que tenga certificado y sello
                org.w3c.dom.NodeList retencionesList = doc.getElementsByTagNameNS("http://www.sat.gob.mx/esquemas/retencionpago/2", "Retenciones");
                if (retencionesList.getLength() == 0) {
                    retencionesList = doc.getElementsByTagName("Retenciones");
                }
                if (retencionesList.getLength() > 0) {
                    Element retenciones = (Element) retencionesList.item(0);
                    String certificado = retenciones.getAttribute("Certificado");
                    String sello = retenciones.getAttribute("Sello");
                    noCertificadoEnviado = retenciones.getAttribute("NoCertificado");
                    logger.info("📋 INFORMACIÓN DE FIRMA EN EL XML:");
                    logger.info("  Certificado presente: {}", certificado != null && !certificado.isEmpty() ? "✓ SÍ" : "✗ NO");
                    logger.info("  Sello presente: {}", sello != null && !sello.isEmpty() ? "✓ SÍ" : "✗ NO");
                    logger.info("  NoCertificado: {}", noCertificadoEnviado != null && !noCertificadoEnviado.isEmpty() ? noCertificadoEnviado : "NO ESPECIFICADO");
                }
            } catch (Exception e) {
                logger.warn("No se pudo validar el XML antes de enviar (continuando de todas formas): {}", e.getMessage());
            }
            
            // 5. Convertir XML a Base64 (Finkok requiere Base64)
            String xmlBase64 = Base64.getEncoder().encodeToString(xmlPlainText.getBytes(StandardCharsets.UTF_8));
            logger.info("XML codificado a Base64: {} caracteres", xmlBase64.length());
            
            // 6. Crear y enviar request SOAP usando método stamp
            SOAPMessage soapRequest = createStampSOAPRequest(xmlBase64, username, password);
            String soapResponse = sendSOAPRequest(soapRequest);
            
            // 7. Parsear respuesta SOAP (pasar información del emisor para diagnóstico)
            PacTimbradoResponse response = parseStampSOAPResponse(soapResponse, rfcEmisorEnviado, nombreEmisorEnviado, noCertificadoEnviado);
            
            logger.info("=== FIN TIMBRADO DE RETENCIÓN ===");
            logger.info("Resultado: ok={}, CodEstatus={}, UUID={}", 
                    response.getOk(), response.getCodEstatus(), response.getUuid());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error en timbrado de retención con Finkok: {}", e.getMessage(), e);
            PacTimbradoResponse r = new PacTimbradoResponse();
            r.setOk(false);
            r.setStatus("ERROR");
            r.setMessage("Error en timbrado: " + e.getMessage());
            r.setCodigoError("EXCEPTION");
            r.setMensajeIncidencia(e.getMessage());
            return r;
        }
    }
    
    private PacTimbradoResponse crearRespuestaSimulada(PacTimbradoRequest req, String xmlPlainText) {
        String uuid = (req.getUuid() != null && !req.getUuid().isBlank())
                ? req.getUuid()
                : UUID.randomUUID().toString().toUpperCase();
        LocalDateTime ahora = LocalDateTime.now();
        
        PacTimbradoResponse response = new PacTimbradoResponse();
        response.setOk(true);
        response.setStatus("SIMULADO");
        response.setCodEstatus("Comprobante timbrado satisfactoriamente (simulado)");
        response.setUuid(uuid);
        response.setFechaTimbrado(ahora);
        response.setFecha(ahora.toString());
        response.setMessage("Timbrado simulado localmente. No se llamó al PAC de Finkok.");
        response.setReceiptId("SIM-" + uuid.substring(0, 8));
        response.setXmlTimbrado(xmlPlainText);
        response.setCadenaOriginal("CADENA-ORIGINAL-SIMULADA");
        response.setSelloDigital("SELLO-CFD-SIMULADO");
        response.setSatSeal("SELLO-SAT-SIMULADO");
        response.setNoCertificadoSAT("00001000000504465028");
        response.setNoCertificadoPac("SIMULADO");
        return response;
    }
    
    /**
     * Crea el mensaje SOAP para el método stamp de retenciones según documentación oficial
     * Estructura: <SOAP-ENV:Envelope><ns0:Body><ns1:stamp><ns1:xml>...</ns1:xml>...</ns1:stamp></ns0:Body></SOAP-ENV:Envelope>
     */
    private SOAPMessage createStampSOAPRequest(String xmlBase64, String username, String password) throws Exception {
        System.setProperty("jakarta.xml.soap.MessageFactory", "com.sun.xml.messaging.saaj.soap.ver1_1.SOAPMessageFactory1_1Impl");
        MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        
        // Namespaces según documentación oficial de retentions
        String serverURI = "http://facturacion.finkok.com/retentions";
        String envelopeURI = "http://schemas.xmlsoap.org/soap/envelope/";
        
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("ns0", serverURI);
        envelope.addNamespaceDeclaration("ns1", envelopeURI);
        envelope.addNamespaceDeclaration("SOAP-ENV", envelopeURI);
        envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        
        // Headers SOAP
        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", "stamp");
        headers.addHeader("Content-Type", "text/xml; charset=utf-8");
        
        SOAPBody soapBody = envelope.getBody();
        
        // Elemento raíz: <ns0:stamp>
        SOAPElement stampElement = soapBody.addChildElement("stamp", "ns0");
        
        // Elemento: <ns0:xml> (Base64 del XML)
        SOAPElement xmlElement = stampElement.addChildElement("xml", "ns0");
        xmlElement.addTextNode(xmlBase64);
        
        // Elemento: <ns0:username>
        SOAPElement usernameElement = stampElement.addChildElement("username", "ns0");
        usernameElement.addTextNode(username);
        
        // Elemento: <ns0:password>
        SOAPElement passwordElement = stampElement.addChildElement("password", "ns0");
        passwordElement.addTextNode(password);
        
        soapMessage.saveChanges();
        
        // Log del request SOAP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapMessage.writeTo(baos);
        logger.info("=== SOAP REQUEST ===");
        logger.info("{}", baos.toString(StandardCharsets.UTF_8));
        logger.info("=== FIN SOAP REQUEST ===");
        
        return soapMessage;
    }
    
    /**
     * Envía el request SOAP al endpoint de Finkok para retenciones
     */
    private String sendSOAPRequest(SOAPMessage soapMessage) throws Exception {
        // Convertir SOAPMessage a String
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapMessage.writeTo(baos);
        String soapRequest = baos.toString(StandardCharsets.UTF_8);
        
        // Ajustar URL (remover .wsdl si existe)
        String endpointUrl = finkokRetentionsUrl;
        if (endpointUrl.endsWith(".wsdl")) {
            endpointUrl = endpointUrl.replace(".wsdl", "");
        }
        
        logger.info("Enviando request SOAP a: {}", endpointUrl);
        
        URL serviceUrl = new URL(endpointUrl);
        HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        connection.setRequestProperty("SOAPAction", "stamp");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        
        // Enviar request
        connection.getOutputStream().write(soapRequest.getBytes(StandardCharsets.UTF_8));
        
        // Leer respuesta
        int responseCode = connection.getResponseCode();
        String soapResponse;
        
        if (responseCode >= 400) {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                ByteArrayOutputStream errorBaos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = errorStream.read(buffer)) != -1) {
                    errorBaos.write(buffer, 0, length);
                }
                soapResponse = errorBaos.toString(StandardCharsets.UTF_8);
                logger.error("Error HTTP {}: {}", responseCode, soapResponse);
            } else {
                soapResponse = "Error HTTP " + responseCode;
            }
            throw new Exception("Error HTTP " + responseCode + ": " + soapResponse);
        } else {
            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream responseBaos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                responseBaos.write(buffer, 0, length);
            }
            soapResponse = responseBaos.toString(StandardCharsets.UTF_8);
        }
        
        logger.info("=== SOAP RESPONSE ===");
        logger.info("{}", soapResponse);
        logger.info("=== FIN SOAP RESPONSE ===");
        
        connection.disconnect();
        return soapResponse;
    }
    
    /**
     * Parsea la respuesta SOAP del método stamp de retenciones según documentación oficial
     * Estructura: <senv:Envelope><senv:Body><tns:stampResponse><tns:stampResult>...</tns:stampResult></tns:stampResponse></senv:Body></senv:Envelope>
     */
    private PacTimbradoResponse parseStampSOAPResponse(String soapResponse, String rfcEmisorEnviado, String nombreEmisorEnviado, String noCertificadoEnviado) throws Exception {
        PacTimbradoResponse response = new PacTimbradoResponse();
        
        // Parsear XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new java.io.ByteArrayInputStream(soapResponse.getBytes(StandardCharsets.UTF_8)));
        
        // Namespaces según documentación
        String nsTns = "http://facturacion.finkok.com/retentions";
        String nsS0 = "apps.services.soap.core.views";
        
        // Buscar stampResult
        NodeList resultList = doc.getElementsByTagNameNS(nsTns, "stampResult");
        if (resultList.getLength() == 0) {
            resultList = doc.getElementsByTagName("stampResult");
        }
        
        if (resultList.getLength() == 0) {
            throw new Exception("No se encontró stampResult en la respuesta SOAP");
        }
        
        Element stampResult = (Element) resultList.item(0);
        
        // Extraer campos principales
        String xml = getElementText(stampResult, "xml", nsS0);
        String uuid = getElementText(stampResult, "UUID", nsS0);
        String fecha = getElementText(stampResult, "Fecha", nsS0);
        String codEstatus = getElementText(stampResult, "CodEstatus", nsS0);
        String satSeal = getElementText(stampResult, "SatSeal", nsS0);
        String noCertificadoSAT = getElementText(stampResult, "NoCertificadoSAT", nsS0);
        
        // Procesar XML timbrado (puede venir en CDATA)
        if (xml != null && !xml.isBlank()) {
            xml = xml.replace("<![CDATA[", "").replace("]]>", "").trim();
            // Decodificar entidades HTML
            xml = xml.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("&quot;", "\"").replace("&apos;", "'");
            response.setXmlTimbrado(xml);
        }
        
        response.setUuid(uuid);
        response.setFecha(fecha);
        response.setCodEstatus(codEstatus);
        response.setSatSeal(satSeal);
        response.setNoCertificadoSAT(noCertificadoSAT);
        
        // Parsear fecha para fechaTimbrado
        if (fecha != null && !fecha.isBlank()) {
            try {
                LocalDateTime fechaTimbrado = LocalDateTime.parse(fecha, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                response.setFechaTimbrado(fechaTimbrado);
            } catch (Exception e) {
                logger.warn("No se pudo parsear fecha de timbrado: {}", fecha);
            }
        }
        
        // Validar éxito basado en CodEstatus (según documentación oficial)
        // "Comprobante timbrado satisfactoriamente" = éxito
        boolean esExitoso = codEstatus != null && (
                codEstatus.equals("Comprobante timbrado satisfactoriamente") || 
                codEstatus.contains("timbrado satisfactoriamente")
        );
        
        response.setOk(esExitoso);
        response.setStatus(esExitoso ? "TIMBRADO" : "ERROR");
        
        // Procesar incidencias si existen
        NodeList incidenciasList = stampResult.getElementsByTagNameNS(nsS0, "Incidencias");
        if (incidenciasList.getLength() == 0) {
            incidenciasList = stampResult.getElementsByTagName("Incidencias");
        }
        
        if (incidenciasList.getLength() > 0) {
            Element incidencias = (Element) incidenciasList.item(0);
            NodeList incidenciaList = incidencias.getElementsByTagNameNS(nsS0, "Incidencia");
            if (incidenciaList.getLength() == 0) {
                incidenciaList = incidencias.getElementsByTagName("Incidencia");
            }
            
            if (incidenciaList.getLength() > 0) {
                Element incidencia = (Element) incidenciaList.item(0);
                
                String idIncidencia = getElementText(incidencia, "IdIncidencia", nsS0);
                String rfcEmisor = getElementText(incidencia, "RfcEmisor", nsS0);
                String codigoError = getElementText(incidencia, "CodigoError", nsS0);
                String workProcessId = getElementText(incidencia, "WorkProcessId", nsS0);
                String mensajeIncidencia = getElementText(incidencia, "MensajeIncidencia", nsS0);
                String extraInfo = getElementText(incidencia, "ExtraInfo", nsS0);
                String noCertificadoPac = getElementText(incidencia, "NoCertificadoPac", nsS0);
                String fechaRegistro = getElementText(incidencia, "FechaRegistro", nsS0);
                
                logger.error("═══════════════════════════════════════════════════════════════");
                logger.error("✗✗✗ ERROR EN TIMBRADO DE RETENCIÓN CON FINKOK ✗✗✗");
                logger.error("═══════════════════════════════════════════════════════════════");
                logger.error("Código de Error: {}", codigoError);
                logger.error("Mensaje: {}", mensajeIncidencia);
                logger.error("RFC Emisor (en incidencia): {}", rfcEmisor != null && !rfcEmisor.isEmpty() ? rfcEmisor : "NO ESPECIFICADO");
                logger.error("ExtraInfo: {}", extraInfo != null && !extraInfo.isEmpty() ? extraInfo : "(vacío)");
                logger.error("WorkProcessId: {}", workProcessId);
                logger.error("NoCertificadoPac: {}", noCertificadoPac);
                logger.error("Fecha Registro: {}", fechaRegistro);
                
                // Diagnóstico específico para errores comunes
                if ("300".equals(codigoError) || (codigoError != null && codigoError.contains("300"))) {
                    logger.error("═══════════════════════════════════════════════════════════════");
                    logger.error("⚠️ DIAGNÓSTICO PARA ERROR 300:");
                    logger.error("═══════════════════════════════════════════════════════════════");
                    logger.error("El usuario o contraseña son inválidos");
                    logger.error("Verifica las credenciales en application.yml (finkok.username y finkok.password)");
                    logger.error("═══════════════════════════════════════════════════════════════");
                }
                
                response.setIdIncidencia(idIncidencia);
                response.setCodigoError(codigoError);
                response.setMensajeIncidencia(mensajeIncidencia);
                response.setExtraInfo(extraInfo);
                response.setWorkProcessId(workProcessId);
                response.setNoCertificadoPac(noCertificadoPac);
                response.setFechaRegistro(fechaRegistro);
            }
        }
        
        return response;
    }
    
    /**
     * Helper para extraer texto de un elemento XML
     */
    private String getElementText(Element parent, String tagName, String namespace) {
        NodeList nodeList = parent.getElementsByTagNameNS(namespace, tagName);
        if (nodeList.getLength() == 0) {
            nodeList = parent.getElementsByTagName(tagName);
        }
        if (nodeList.getLength() > 0) {
            Element element = (Element) nodeList.item(0);
            return element.getTextContent() != null ? element.getTextContent().trim() : null;
        }
        return null;
    }
}

