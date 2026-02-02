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
import java.security.PrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfoBuilder;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEOutputEncryptorBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Cliente para timbrado con Finkok usando método stamp
 * Basado en documentación oficial: https://facturacion.finkok.com/servicios/soap/stamp.wsdl
 */
@Component
public class PacClient {
    private static final Logger logger = LoggerFactory.getLogger(PacClient.class);
    
    // Bloque estático para deshabilitar proxy ANTES de cualquier uso
    static {
        System.setProperty("java.net.useSystemProxies", "false");
        System.setProperty("socksProxyHost", "");
        System.setProperty("socksProxyPort", "");
        System.setProperty("http.proxyHost", "");
        System.setProperty("http.proxyPort", "");
        System.setProperty("https.proxyHost", "");
        System.setProperty("https.proxyPort", "");
        System.setProperty("ftp.proxyHost", "");
        System.setProperty("ftp.proxyPort", "");
        logger.info("Propiedades de proxy deshabilitadas en bloque estático");
    }
    
    @Value("${finkok.enabled:true}")
    private boolean finkokEnabled;
    
    @Value("${finkok.username:integrador@finkok.com}")
    private String finkokUsername;
    
    @Value("${finkok.password:Fin2023kok*}")
    private String finkokPassword;
    
    @Value("${finkok.stamp.url:https://demo-facturacion.finkok.com/servicios/soap/stamp}")
    private String finkokStampUrl;
    
    @Value("${finkok.cancel.url:https://demo-facturacion.finkok.com/servicios/soap/cancel}")
    private String finkokCancelUrl;
    
    @Value("${finkok.connection.connect-timeout:30000}")
    private int connectTimeout;
    
    @Value("${finkok.connection.read-timeout:120000}")
    private int readTimeout;
    
    @Value("${finkok.connection.use-ipv4-only:false}")
    private boolean useIpv4Only;
    
    @Value("${finkok.tunnel.enabled:false}")
    private boolean tunnelEnabled;
    
    @Value("${finkok.tunnel.local-port:8443}")
    private int tunnelLocalPort;
    
    @Value("${facturacion.csd.certificado.path:classpath:certificados/CSD.cer}")
    private String certificadoPath;
    
    @Value("${facturacion.csd.llave.path:classpath:certificados/CSD.key}")
    private String llavePath;
    
    @Value("${facturacion.csd.llave.password:}")
    private String llavePassword;
    
    @Value("${facturacion.emisor.rfc:}")
    private String rfcEmisor;
    
    private final CfdiFirmaService cfdiFirmaService;
    
    public PacClient(org.springframework.beans.factory.ObjectProvider<CfdiFirmaService> cfdiFirmaServiceProvider) {
        this.cfdiFirmaService = cfdiFirmaServiceProvider.getIfAvailable();
        logger.info("PacClient inicializado. Servicio de firma disponible: {}", this.cfdiFirmaService != null);
    }

    public PacResponse solicitarCancelacion(PacRequest req) {
        logger.info("=== INICIANDO CANCELACIÓN CON FINKOK ===");
        logger.info("UUID: {}, Motivo: {}, RFC Emisor: {}", req.uuid, req.motivo, req.rfcEmisor);
        
        PacResponse response = new PacResponse();
        
        try {
            // Validaciones básicas
            if (req.uuid == null || req.uuid.trim().isEmpty()) {
                response.setOk(false);
                response.setStatus("ERROR");
                response.setMessage("UUID requerido para cancelación");
                return response;
            }
            
            if (req.motivo == null || !("01".equals(req.motivo) || "02".equals(req.motivo) || 
                    "03".equals(req.motivo) || "04".equals(req.motivo))) {
                response.setOk(false);
                response.setStatus("ERROR");
                response.setMessage("Motivo de cancelación inválido. Debe ser 01, 02, 03 o 04");
                return response;
            }
            
            // Validar motivo 01 requiere UUID sustituto
            if ("01".equals(req.motivo) && (req.uuidSustituto == null || req.uuidSustituto.trim().isEmpty())) {
                response.setOk(false);
                response.setStatus("ERROR");
                response.setMessage("Motivo 01 requiere UUID sustituto");
                return response;
            }
            
            // Obtener RFC emisor (del request o configuración)
            String taxpayerId = req.rfcEmisor != null && !req.rfcEmisor.trim().isEmpty() 
                    ? req.rfcEmisor.trim() 
                    : (rfcEmisor != null && !rfcEmisor.trim().isEmpty() ? rfcEmisor.trim() : null);
            
            if (taxpayerId == null) {
                response.setOk(false);
                response.setStatus("ERROR");
                response.setMessage("RFC emisor no configurado");
                return response;
            }
            
            // Obtener credenciales
            String username = finkokUsername;
            String password = finkokPassword;
            
            // Cargar certificado y llave en formato PEM Base64
            String cerBase64 = cargarCertificadoBase64();
            String keyBase64 = cargarLlaveBase64();
            
            if (cerBase64 == null || keyBase64 == null) {
                response.setOk(false);
                response.setStatus("ERROR");
                response.setMessage("No se pudieron cargar los certificados. Verifique la configuración.");
                return response;
            }
            
            if (!finkokEnabled) {
                logger.warn("⚠️ PAC DESHABILITADO (finkok.enabled=false). Se omitirá la cancelación real.");
                response.setOk(true);
                response.setStatus("SIMULADO");
                response.setMessage("Cancelación simulada localmente. No se llamó al PAC de Finkok.");
                return response;
            }
            
            // Crear request SOAP
            SOAPMessage soapRequest = createCancelSOAPRequest(req, username, password, taxpayerId, cerBase64, keyBase64);
            String soapResponse = sendCancelSOAPRequest(soapRequest);
            
            // Parsear respuesta
            response = parseCancelSOAPResponse(soapResponse, req.uuid);
            
            logger.info("=== FIN CANCELACIÓN ===");
            logger.info("Resultado: ok={}, status={}, message={}", 
                    response.getOk(), response.getStatus(), response.getMessage());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error en cancelación con Finkok: {}", e.getMessage(), e);
            response.setOk(false);
            response.setStatus("ERROR");
            response.setMessage("Error en cancelación: " + e.getMessage());
            return response;
        }
    }

    /**
     * Solicita el timbrado de un CFDI usando el método stamp de Finkok
     * Según documentación oficial: https://facturacion.finkok.com/servicios/soap/stamp.wsdl
     */
    public PacTimbradoResponse solicitarTimbrado(PacTimbradoRequest req) {
        try {
            logger.info("=== INICIANDO TIMBRADO CON FINKOK ===");
            logger.info("URL: {}, UUID: {}, Tipo: {}", finkokStampUrl, req.getUuid(), req.getTipo());
            
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
            
            // 3. Firmar XML con CSD si es CFDI 4.0
            boolean esCFDI40 = xmlPlainText.contains("xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\"");
            if (esCFDI40) {
                logger.info("CFDI 4.0 detectado - Firmando XML con CSD antes del timbrado...");
                if (cfdiFirmaService != null) {
                    try {
                        String xmlAntesFirma = xmlPlainText;
                        xmlPlainText = cfdiFirmaService.firmarXml(xmlPlainText);
                        
                        // Verificar si el XML cambió (si no cambió, significa que no se firmó)
                        if (xmlPlainText.equals(xmlAntesFirma)) {
                            logger.error("✗✗✗ El XML no se modificó después de intentar firmarlo.");
                            logger.error("✗✗✗ Esto significa que el servicio de firma está DESHABILITADO o no pudo cargar los certificados.");
                            logger.error("✗✗✗ SOLUCIÓN:");
                            logger.error("   1. Verifica que facturacion.csd.enabled=true en application.yml");
                            logger.error("   2. Verifica que los certificados estén en src/main/resources/certificados/");
                            logger.error("   3. REINICIA la aplicación después de cambiar application.yml");
                            logger.error("   4. Revisa los logs de INICIO para ver si el certificado y llave se cargaron correctamente");
                            throw new IllegalStateException(
                                "El servicio de firma está deshabilitado o no pudo cargar los certificados. " +
                                "Verifica que facturacion.csd.enabled=true en application.yml y que los certificados estén en " +
                                "src/main/resources/certificados/. REINICIA la aplicación después de cambiar application.yml."
                            );
                        }
                        
                        // Verificar que se firmó correctamente
                        boolean tieneCertificado = xmlPlainText.contains("Certificado=\"") && 
                                                   !xmlPlainText.contains("Certificado=\"\"");
                        boolean tieneSello = xmlPlainText.contains("Sello=\"") && 
                                             !xmlPlainText.contains("Sello=\"\"");
                        boolean tieneNoCertificado = xmlPlainText.contains("NoCertificado=\"") && 
                                                     !xmlPlainText.contains("NoCertificado=\"\"");
                        
                        if (tieneCertificado && tieneSello && tieneNoCertificado) {
                            logger.info("✓✓✓ XML firmado exitosamente con CSD");
                            logger.info("  - Certificado presente: ✓");
                            logger.info("  - Sello presente: ✓");
                            logger.info("  - NoCertificado presente: ✓");
                            
                            // Extraer y mostrar NoCertificado del XML firmado para diagnóstico
                            try {
                                int noCertPos = xmlPlainText.indexOf("NoCertificado=\"");
                                if (noCertPos >= 0) {
                                    int inicioValor = noCertPos + "NoCertificado=\"".length();
                                    int finValor = xmlPlainText.indexOf("\"", inicioValor);
                                    if (finValor > inicioValor) {
                                        String noCertificadoEnXML = xmlPlainText.substring(inicioValor, finValor);
                                        logger.info("  - NoCertificado en XML firmado: {}", noCertificadoEnXML);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("Error al extraer NoCertificado del XML: {}", e.getMessage());
                            }
                        } else {
                            logger.error("✗✗✗ XML NO se firmó correctamente.");
                            logger.error("  - Certificado presente: {}", tieneCertificado);
                            logger.error("  - Sello presente: {}", tieneSello);
                            logger.error("  - NoCertificado presente: {}", tieneNoCertificado);
                            throw new IllegalStateException(
                                "El XML no se firmó correctamente. Faltan atributos de firma. " +
                                "Verifica que los certificados se hayan cargado correctamente al inicio de la aplicación."
                            );
                        }
                    } catch (IllegalStateException e) {
                        // Re-lanzar excepciones de estado (ya tienen mensajes claros)
                        throw e;
                    } catch (Exception e) {
                        logger.error("✗✗✗ Error al firmar XML con CSD: {}", e.getMessage(), e);
                        throw new IllegalStateException(
                            "No se pudo firmar el XML con CSD: " + e.getMessage() + 
                            ". Revisa los logs de inicio para ver si hay errores al cargar los certificados.", e
                        );
                    }
                } else {
                    throw new IllegalStateException(
                        "CFDI 4.0 requiere firma con CSD, pero el servicio de firma no está disponible. " +
                        "Configure facturacion.csd.enabled=true y los certificados en application.yml. " +
                        "REINICIA la aplicación después de cambiar application.yml."
                    );
                }
            }
            
            // 4. Validar XML antes de enviar (extraer información para diagnóstico)
            String rfcEmisorEnviado = null;
            String nombreEmisorEnviado = null;
            String regimenEmisorEnviado = null;
            String noCertificadoEnviado = null;
            
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlPlainText.getBytes(StandardCharsets.UTF_8)));
                
                // Extraer información del emisor para diagnóstico
                org.w3c.dom.NodeList emisoresList = doc.getElementsByTagNameNS("http://www.sat.gob.mx/cfd/4", "Emisor");
                if (emisoresList.getLength() == 0) {
                    emisoresList = doc.getElementsByTagName("Emisor");
                }
                if (emisoresList.getLength() > 0) {
                    Element emisor = (Element) emisoresList.item(0);
                    rfcEmisorEnviado = emisor.getAttribute("Rfc");
                    nombreEmisorEnviado = emisor.getAttribute("Nombre");
                    regimenEmisorEnviado = emisor.getAttribute("RegimenFiscal");
                    logger.info("═══════════════════════════════════════════════════════════════");
                    logger.info("📋 INFORMACIÓN DEL EMISOR EN EL XML:");
                    logger.info("  RFC: {}", rfcEmisorEnviado);
                    logger.info("  Nombre: {}", nombreEmisorEnviado);
                    logger.info("  Régimen Fiscal: {}", regimenEmisorEnviado);
                    logger.info("═══════════════════════════════════════════════════════════════");
                    logger.info("⚠️ IMPORTANTE: Verifica que estos datos coincidan EXACTAMENTE con los registrados en tu cuenta demo de Finkok");
                }
                
                // Verificar que tenga certificado y sello
                org.w3c.dom.NodeList comprobantesList = doc.getElementsByTagNameNS("http://www.sat.gob.mx/cfd/4", "Comprobante");
                if (comprobantesList.getLength() == 0) {
                    comprobantesList = doc.getElementsByTagName("Comprobante");
                }
                if (comprobantesList.getLength() > 0) {
                    Element comprobante = (Element) comprobantesList.item(0);
                    String certificado = comprobante.getAttribute("Certificado");
                    String sello = comprobante.getAttribute("Sello");
                    noCertificadoEnviado = comprobante.getAttribute("NoCertificado");
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
            
            logger.info("=== FIN TIMBRADO ===");
            logger.info("Resultado: ok={}, CodEstatus={}, UUID={}", 
                    response.getOk(), response.getCodEstatus(), response.getUuid());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error en timbrado con Finkok: {}", e.getMessage(), e);
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
     * Crea el mensaje SOAP para el método stamp según documentación oficial
     * Estructura: <SOAP-ENV:Envelope><ns0:Body><ns1:stamp><ns1:xml>...</ns1:xml>...</ns1:stamp></ns0:Body></SOAP-ENV:Envelope>
     */
    private SOAPMessage createStampSOAPRequest(String xmlBase64, String username, String password) throws Exception {
        System.setProperty("jakarta.xml.soap.MessageFactory", "com.sun.xml.messaging.saaj.soap.ver1_1.SOAPMessageFactory1_1Impl");
        MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        
        // Namespaces según documentación oficial
        String serverURI = "http://facturacion.finkok.com/stamp";
        String envelopeURI = "http://schemas.xmlsoap.org/soap/envelope/";
        
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("ns0", envelopeURI);
        envelope.addNamespaceDeclaration("ns1", serverURI);
        envelope.addNamespaceDeclaration("SOAP-ENV", envelopeURI);
        envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        
        // Headers SOAP
        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", serverURI + "/stamp");
        headers.addHeader("Content-Type", "text/xml; charset=utf-8");
        
        SOAPBody soapBody = envelope.getBody();
        
        // Elemento raíz: <ns1:stamp>
        SOAPElement stampElement = soapBody.addChildElement("stamp", "ns1");
        
        // Elemento: <ns1:xml> (Base64 del XML)
        SOAPElement xmlElement = stampElement.addChildElement("xml", "ns1");
        xmlElement.addTextNode(xmlBase64);
        
        // Elemento: <ns1:username>
        SOAPElement usernameElement = stampElement.addChildElement("username", "ns1");
        usernameElement.addTextNode(username);
        
        // Elemento: <ns1:password>
        SOAPElement passwordElement = stampElement.addChildElement("password", "ns1");
        passwordElement.addTextNode(password);
        
        soapMessage.saveChanges();
        
        // Log del request SOAP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapMessage.writeTo(baos);
        String soapRequestStr = baos.toString(StandardCharsets.UTF_8);
            logger.info("=== SOAP REQUEST (stamp) ===");
        logger.info("{}", soapRequestStr);
        logger.info("=== FIN SOAP REQUEST ===");
        
        return soapMessage;
    }
    
    /**
     * Envía el request SOAP al endpoint de Finkok
     */
    private String sendSOAPRequest(SOAPMessage soapMessage) throws Exception {
        // Convertir SOAPMessage a String
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapMessage.writeTo(baos);
        String soapRequest = baos.toString(StandardCharsets.UTF_8);
        
        // Ajustar URL (remover .wsdl si existe)
        String endpointUrl = finkokStampUrl;
        if (endpointUrl.endsWith(".wsdl")) {
            endpointUrl = endpointUrl.replace(".wsdl", "");
        }
        
        // Si el túnel SSH está habilitado, usar localhost:8443 en lugar de la URL directa
        if (tunnelEnabled) {
            // Reemplazar la URL de Finkok con localhost:puerto del túnel
            try {
                java.net.URL originalUrl = new java.net.URL(endpointUrl);
                endpointUrl = String.format("https://localhost:%d%s", tunnelLocalPort, originalUrl.getPath());
                logger.info("Túnel SSH habilitado - Usando túnel local: {}", endpointUrl);
            } catch (Exception e) {
                logger.warn("Error al construir URL del túnel, usando URL original: {}", e.getMessage());
            }
        }
        
        logger.info("Enviando request SOAP a: {}", endpointUrl);
        
        // Configurar IPv4 si está habilitado
        if (useIpv4Only) {
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("java.net.preferIPv6Addresses", "false");
            logger.info("Forzando uso de IPv4 solamente");
        }
        
        // Deshabilitar TODA detección automática de proxy (HTTP, HTTPS, SOCKS)
        // IMPORTANTE: Estas propiedades deben establecerse ANTES de crear el HttpClient
        System.setProperty("java.net.useSystemProxies", "false");
        System.setProperty("http.proxyHost", "");
        System.setProperty("http.proxyPort", "");
        System.setProperty("https.proxyHost", "");
        System.setProperty("https.proxyPort", "");
        System.setProperty("socksProxyHost", "");
        System.setProperty("socksProxyPort", "");
        System.setProperty("socksNonProxyHosts", "*");
        // Forzar que NO use proxy SOCKS a nivel de JVM
        System.setProperty("java.net.socks.username", "");
        System.setProperty("java.net.socks.password", "");
        
        // Crear un RoutePlanner personalizado que NUNCA use proxy
        // Esto evita que HttpClient detecte proxies automáticamente
        HttpRoutePlanner noProxyRoutePlanner = new HttpRoutePlanner() {
            @Override
            public HttpRoute determineRoute(
                    HttpHost target, HttpContext context) {
                // Asegurar que el puerto esté correctamente establecido
                int port = target.getPort();
                if (port < 0) {
                    // Si no hay puerto explícito, usar el puerto por defecto del esquema
                    String schemeName = target.getSchemeName();
                    if ("https".equalsIgnoreCase(schemeName)) {
                        port = 443;
                    } else if ("http".equalsIgnoreCase(schemeName)) {
                        port = 80;
                    } else {
                        port = 443; // Por defecto HTTPS
                    }
                    // Crear un nuevo HttpHost con el puerto correcto
                    HttpHost targetWithPort = new HttpHost(target.getSchemeName(), target.getHostName(), port);
                    // Siempre retornar ruta directa sin proxy
                    return new HttpRoute(targetWithPort);
                }
                // Si el puerto ya está establecido, usar el target original
                return new HttpRoute(target);
            }
        };
        
        // Usar Apache HttpClient con configuración explícita SIN proxy
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
            .setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout))
            .build();
        
        // Configurar HttpClient sin proxy de ninguna clase
        // El RoutePlanner personalizado asegura que nunca se use proxy
        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setRoutePlanner(noProxyRoutePlanner)
            .disableAutomaticRetries()
            .build();
        
        try {
            HttpPost httpPost = new HttpPost(endpointUrl);
            httpPost.setHeader("Content-Type", "text/xml; charset=utf-8");
            httpPost.setHeader("SOAPAction", "http://facturacion.finkok.com/stamp/stamp");
            httpPost.setEntity(new StringEntity(soapRequest, StandardCharsets.UTF_8));
            
            logger.info("Conexión usando Apache HttpClient (sin proxy automático)");
            
            return httpClient.execute(httpPost, response -> {
                try {
                    int statusCode = response.getCode();
                    String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                    
                    if (statusCode >= 400) {
                        logger.error("Error HTTP {} desde Finkok: {}", statusCode, responseBody);
                        throw new RuntimeException("Error HTTP " + statusCode + " desde Finkok: " + responseBody);
                    }
                    
                    logger.debug("Respuesta recibida de Finkok: {} caracteres", responseBody.length());
                    return responseBody;
                } catch (IOException e) {
                    throw new RuntimeException("Error leyendo respuesta de Finkok", e);
                }
            });
        } catch (IOException e) {
            logger.error("═══════════════════════════════════════════════════════════════");
            logger.error("✗✗✗ ERROR DE CONEXIÓN CON FINKOK ✗✗✗");
            logger.error("═══════════════════════════════════════════════════════════════");
            logger.error("No se pudo establecer conexión con: {}", endpointUrl);
            logger.error("Error: {}", e.getMessage());
            logger.error("");
            logger.error("POSIBLES CAUSAS:");
            logger.error("1. Firewall bloqueando conexiones HTTPS salientes desde el servidor");
            logger.error("2. Problemas de conectividad de red desde el servidor");
            logger.error("3. Finkok temporalmente no disponible");
            logger.error("4. Verificar conectividad de red");
            logger.error("");
            logger.error("SOLUCIONES:");
            logger.error("1. Verifica conectividad desde el servidor:");
            logger.error("   curl -v https://demo-facturacion.finkok.com/servicios/soap/stamp");
            logger.error("2. Verifica reglas de firewall para permitir HTTPS saliente (puerto 443)");
            logger.error("3. Verificar firewall y reglas de red del servidor");
            logger.error("4. Verifica que el servidor tenga acceso a Internet");
            logger.error("═══════════════════════════════════════════════════════════════");
            throw new Exception("Error de conexión con Finkok: " + e.getMessage() + 
                ". Verifica conectividad de red y firewall desde el servidor.", e);
        } finally {
            httpClient.close();
        }
    }
    
    /**
     * Parsea la respuesta SOAP del método stamp según documentación oficial
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
        String nsTns = "http://facturacion.finkok.com/stamp";
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
            response.setXmlTimbrado(xml);
        }
        
        response.setUuid(uuid);
        response.setFecha(fecha);
        response.setCodEstatus(codEstatus);
        response.setSatSeal(satSeal);
        response.setNoCertificadoSAT(noCertificadoSAT);
        
        // Validar éxito basado en CodEstatus (según documentación oficial)
        // "Comprobante timbrado satisfactoriamente" = éxito
        // "Comprobante timbrado previamente" = éxito (timbre previo, código 307)
        boolean esExitoso = codEstatus != null && (
                codEstatus.equals("Comprobante timbrado satisfactoriamente") || 
                codEstatus.equals("Comprobante timbrado previamente") ||
                codEstatus.contains("timbrado satisfactoriamente") ||
                codEstatus.contains("timbrado previamente")
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
                logger.error("✗✗✗ ERROR EN TIMBRADO CON FINKOK ✗✗✗");
                logger.error("═══════════════════════════════════════════════════════════════");
                logger.error("Código de Error: {}", codigoError);
                logger.error("Mensaje: {}", mensajeIncidencia);
                logger.error("RFC Emisor (en incidencia): {}", rfcEmisor != null && !rfcEmisor.isEmpty() ? rfcEmisor : "NO ESPECIFICADO");
                logger.error("ExtraInfo: {}", extraInfo != null && !extraInfo.isEmpty() ? extraInfo : "(vacío)");
                logger.error("WorkProcessId: {}", workProcessId);
                logger.error("NoCertificadoPac: {}", noCertificadoPac);
                logger.error("Fecha Registro: {}", fechaRegistro);
                
                // Diagnóstico específico para CRP20999
                if ("CRP20999".equals(codigoError)) {
                    logger.error("═══════════════════════════════════════════════════════════════");
                    logger.error("⚠️ DIAGNÓSTICO PARA CRP20999 (Error no clasificado):");
                    logger.error("═══════════════════════════════════════════════════════════════");
                    logger.error("Este error genérico puede deberse a:");
                    logger.error("1. El RFC del emisor NO está registrado en tu cuenta demo de Finkok");
                    logger.error("2. El nombre del emisor NO coincide EXACTAMENTE con el registrado en Finkok");
                    logger.error("3. El certificado (NoCertificado) NO corresponde al RFC registrado");
                    logger.error("4. Problema con la estructura del XML (campos faltantes o inválidos)");
                    logger.error("5. Problema con la firma digital del XML");
                    logger.error("");
                    logger.error("═══════════════════════════════════════════════════════════════");
                    logger.error("📋 DATOS DEL EMISOR QUE SE ENVIARON:");
                    logger.error("═══════════════════════════════════════════════════════════════");
                    if (rfcEmisorEnviado != null && !rfcEmisorEnviado.isEmpty()) {
                        logger.error("  RFC enviado: '{}'", rfcEmisorEnviado);
                    } else {
                        logger.error("  RFC enviado: NO DISPONIBLE");
                    }
                    if (nombreEmisorEnviado != null && !nombreEmisorEnviado.isEmpty()) {
                        logger.error("  Nombre enviado: '{}'", nombreEmisorEnviado);
                        logger.error("  Longitud del nombre: {} caracteres", nombreEmisorEnviado.length());
                    } else {
                        logger.error("  Nombre enviado: NO DISPONIBLE");
                    }
                    if (noCertificadoEnviado != null && !noCertificadoEnviado.isEmpty()) {
                        logger.error("  NoCertificado enviado: '{}'", noCertificadoEnviado);
                    } else {
                        logger.error("  NoCertificado enviado: NO DISPONIBLE");
                    }
                    logger.error("");
                    logger.error("⚠️ VERIFICA EN TU CUENTA DEMO DE FINKOK:");
                    if (rfcEmisorEnviado != null && !rfcEmisorEnviado.isEmpty()) {
                        logger.error("  1. Que el RFC '{}' esté registrado", rfcEmisorEnviado);
                    } else {
                        logger.error("  1. Que el RFC del emisor esté registrado");
                    }
                    if (nombreEmisorEnviado != null && !nombreEmisorEnviado.isEmpty()) {
                        logger.error("  2. Que el nombre registrado sea EXACTAMENTE: '{}'", nombreEmisorEnviado);
                        logger.error("     (Debe coincidir carácter por carácter, incluyendo espacios y mayúsculas)");
                        logger.error("     Si en Finkok el nombre es diferente, actualiza application.yml con el nombre exacto");
                    } else {
                        logger.error("  2. Que el nombre del emisor coincida EXACTAMENTE con el registrado");
                    }
                    if (noCertificadoPac != null && !noCertificadoPac.isEmpty()) {
                        logger.error("  3. Que el certificado NoCertificado '{}' corresponda a ese RFC", noCertificadoPac);
                    } else {
                        logger.error("  3. Que el certificado corresponda al RFC registrado");
                    }
                    logger.error("═══════════════════════════════════════════════════════════════");
                    logger.error("SOLUCIONES:");
                    logger.error("1. Ve a https://demo-facturacion.finkok.com y verifica/registra el RFC del emisor");
                    logger.error("2. Compara el nombre exacto en Finkok con el de application.yml (facturacion.emisor.nombre)");
                    logger.error("3. Si el nombre en Finkok es diferente, actualiza application.yml y REINICIA la aplicación");
                    logger.error("4. Revisa el XML firmado guardado en el archivo temporal para validarlo manualmente");
                    logger.error("═══════════════════════════════════════════════════════════════");
                }
                
                response.setIdIncidencia(idIncidencia);
                response.setCodigoError(codigoError);
                response.setMensajeIncidencia(mensajeIncidencia);
                response.setWorkProcessId(workProcessId);
                response.setExtraInfo(extraInfo);
                response.setRfcEmisor(rfcEmisor);
                response.setFechaRegistro(fechaRegistro);
                response.setNoCertificadoPac(noCertificadoPac);
                
                // IMPORTANTE: Según documentación, validar CodEstatus, no Incidencias
                // El código 307 (timbre previo) es exitoso si CodEstatus lo indica
                if ("307".equals(codigoError) && esExitoso) {
                    // Timbre previo pero exitoso
                    response.setOk(true);
                    response.setStatus("TIMBRADO_PREVIAMENTE");
                    response.setMessage("El CFDI contiene un timbre previo");
                } else if (codigoError != null && !codigoError.isBlank() && !esExitoso) {
                    // Error real
                    response.setOk(false);
                    response.setStatus("ERROR");
                    response.setMessage(mensajeIncidencia != null ? mensajeIncidencia : "Error en timbrado");
                    if (extraInfo != null && !extraInfo.isBlank()) {
                        response.setMessage(response.getMessage() + " - " + extraInfo);
                    }
                }
            }
        } else {
            // Si no hay incidencias, validar por CodEstatus
            if (codEstatus != null && !codEstatus.contains("timbrado")) {
                response.setOk(false);
                response.setStatus("ERROR");
                response.setMessage(codEstatus);
            }
        }
        
        // Parsear fecha de timbrado
        if (fecha != null && !fecha.isBlank()) {
            try {
                LocalDateTime fechaTimbrado = LocalDateTime.parse(fecha, DateTimeFormatter.ISO_DATE_TIME);
                response.setFechaTimbrado(fechaTimbrado);
            } catch (Exception e) {
                logger.warn("No se pudo parsear fecha de timbrado: {}", fecha);
            }
        }
        
        return response;
    }
    
    private String getElementText(Element parent, String tagName, String namespace) {
        NodeList nodeList = parent.getElementsByTagNameNS(namespace, tagName);
        if (nodeList.getLength() == 0) {
            nodeList = parent.getElementsByTagName(tagName);
        }
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }
    
    /**
     * Formatea un UUID agregando guiones en el formato estándar (8-4-4-4-12)
     * Finkok requiere el UUID en este formato para la cancelación
     * 
     * @param uuid UUID sin guiones o con guiones (se normaliza)
     * @return UUID formateado con guiones: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
     */
    private String formatearUUIDConGuiones(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return uuid;
        }
        
        // Remover guiones existentes y espacios
        String uuidLimpio = uuid.replace("-", "").replace(" ", "").trim().toUpperCase();
        
        // Validar que tenga 32 caracteres (UUID estándar)
        if (uuidLimpio.length() != 32) {
            logger.warn("⚠️ UUID con longitud inválida: {} (esperado: 32 caracteres). Se enviará tal cual.", uuidLimpio);
            return uuid; // Retornar original si no tiene formato válido
        }
        
        // Formatear con guiones: 8-4-4-4-12
        return uuidLimpio.substring(0, 8) + "-" +
               uuidLimpio.substring(8, 12) + "-" +
               uuidLimpio.substring(12, 16) + "-" +
               uuidLimpio.substring(16, 20) + "-" +
               uuidLimpio.substring(20, 32);
    }

    public static class PacRequest {
        public String uuid;
        public String motivo;
        public String rfcEmisor;
        public String rfcReceptor;
        public Double total;
        public String tipo;
        public String fechaFactura;
        public Boolean publicoGeneral;
        public Boolean tieneRelaciones;
        public String uuidSustituto;
    }

    public static class PacResponse {
        private Boolean ok;
        private String status;
        private String receiptId;
        private String message;

        public Boolean getOk() {
            return ok;
        }

        public void setOk(Boolean ok) {
            this.ok = ok;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getReceiptId() {
            return receiptId;
        }

        public void setReceiptId(String receiptId) {
            this.receiptId = receiptId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    /**
     * Carga el certificado usando CfdiFirmaService y lo codifica en Base64
     */
    private String cargarCertificadoBase64() {
        try {
            logger.info("Cargando certificado usando CfdiFirmaService...");
            
            // Usar CfdiFirmaService para cargar el certificado (ya maneja DER, PEM, etc.)
            java.security.cert.X509Certificate cert = null;
            if (cfdiFirmaService != null) {
                try {
                    cert = cfdiFirmaService.cargarCertificado();
                    if (cert != null) {
                        logger.info("✓ Certificado cargado exitosamente usando CfdiFirmaService");
                    } else {
                        logger.warn("CfdiFirmaService no pudo cargar el certificado");
                    }
                } catch (Exception e) {
                    logger.error("Error al cargar certificado con CfdiFirmaService: {}", e.getMessage(), e);
                }
            }
            
            if (cert == null) {
                throw new Exception("No se pudo cargar el certificado. Verifica que CfdiFirmaService esté disponible y configurado correctamente.");
            }
            
            // Convertir certificado a formato PEM
            String certPem = "-----BEGIN CERTIFICATE-----\n";
            String base64Cert = Base64.getEncoder().encodeToString(cert.getEncoded());
            // Dividir en líneas de 64 caracteres (estándar PEM)
            for (int i = 0; i < base64Cert.length(); i += 64) {
                int end = Math.min(i + 64, base64Cert.length());
                certPem += base64Cert.substring(i, end) + "\n";
            }
            certPem += "-----END CERTIFICATE-----\n";
            
            // Convertir a Base64 UNA SOLA VEZ (Finkok espera el certificado completo en Base64, incluyendo headers PEM)
            String certBase64 = Base64.getEncoder().encodeToString(certPem.getBytes(StandardCharsets.UTF_8));
            logger.debug("Certificado Base64 (primeros 50 chars): {}", certBase64.length() > 50 ? certBase64.substring(0, 50) : certBase64);
            logger.info("✓ Certificado procesado: PEM -> Base64 (una sola vez)");
            return certBase64;
            
        } catch (Exception e) {
            logger.error("Error al cargar certificado: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Carga la llave privada siguiendo el proceso de Finkok:
     * 1. Usar CfdiFirmaService para cargar la llave (ya maneja DER, PEM, encriptada, etc.)
     * 2. Convertir a PEM si no lo está
     * 3. Encriptar en DES3 usando la contraseña de Finkok (finkok.password)
     * 4. Convertir a Base64 UNA SOLA VEZ
     */
    private String cargarLlaveBase64() {
        try {
            logger.info("Cargando llave privada usando CfdiFirmaService...");
            
            // Usar CfdiFirmaService para cargar la llave (ya maneja todos los formatos correctamente)
            PrivateKey privateKey = null;
            if (cfdiFirmaService != null) {
                try {
                    privateKey = cfdiFirmaService.cargarLlavePrivada();
                    if (privateKey != null) {
                        logger.info("✓ Llave privada cargada exitosamente usando CfdiFirmaService");
                    } else {
                        logger.warn("CfdiFirmaService no pudo cargar la llave privada");
                    }
                } catch (Exception e) {
                    logger.error("Error al cargar llave con CfdiFirmaService: {}", e.getMessage(), e);
                }
            }
            
            if (privateKey == null) {
                throw new Exception("No se pudo cargar la llave privada. Verifica que CfdiFirmaService esté disponible y configurado correctamente.");
            }
            
            // Convertir la llave privada a formato PEM PKCS#8
            String keyPem = "-----BEGIN PRIVATE KEY-----\n";
            String base64Key = Base64.getEncoder().encodeToString(privateKey.getEncoded());
            for (int i = 0; i < base64Key.length(); i += 64) {
                int end = Math.min(i + 64, base64Key.length());
                keyPem += base64Key.substring(i, end) + "\n";
            }
            keyPem += "-----END PRIVATE KEY-----\n";
            
            byte[] keyBytesFinal = keyPem.getBytes(StandardCharsets.UTF_8);
            logger.info("✓ Llave privada convertida a PEM PKCS#8 ({} bytes)", keyBytesFinal.length);
            
            // PASO 2: Encriptar la llave privada en DES3 usando la contraseña de Finkok (finkok.password)
            try {
                logger.info("Encriptando llave privada en DES3 usando contraseña de Finkok...");
                keyBytesFinal = encriptarLlavePrivadaDES3(keyBytesFinal, finkokPassword);
                logger.info("✓ Llave privada encriptada exitosamente en DES3");
            } catch (Exception e) {
                logger.error("Error al encriptar llave privada en DES3: {}", e.getMessage(), e);
                throw new Exception("No se pudo encriptar la llave privada en DES3: " + e.getMessage(), e);
            }
            
            // PASO 3: Convertir a Base64 UNA SOLA VEZ (Finkok espera la llave en Base64)
            String keyBase64 = Base64.getEncoder().encodeToString(keyBytesFinal);
            logger.debug("Llave Base64 (primeros 50 chars): {}", keyBase64.length() > 50 ? keyBase64.substring(0, 50) : keyBase64);
            logger.info("✓ Llave privada procesada: PEM -> DES3 -> Base64 (una sola vez)");
            return keyBase64;
            
        } catch (Exception e) {
            logger.error("Error al cargar llave privada: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Descifra la llave privada encriptada usando la contraseña propia de la llave
     * Paso 1 del proceso de Finkok: convertir a PEM usando la contraseña propia
     */
    private byte[] descifrarLlavePrivada(byte[] keyPemBytes, String password) throws Exception {
        try {
            // Asegurar que BouncyCastle esté registrado
            if (java.security.Security.getProvider("BC") == null) {
                java.security.Security.addProvider(new BouncyCastleProvider());
            }
            
            // Leer la llave privada encriptada desde PEM
            String keyPemText = new String(keyPemBytes, StandardCharsets.UTF_8);
            PEMParser pemParser = new PEMParser(new java.io.StringReader(keyPemText));
            Object pemObject = pemParser.readObject();
            pemParser.close();
            
            PrivateKey privateKey = null;
            
            // Si es una llave encriptada, descifrarla
            if (pemObject instanceof org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo) {
                org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo encryptedKeyInfo = 
                    (org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo) pemObject;
                
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
                org.bouncycastle.operator.InputDecryptorProvider decryptorProvider = 
                    new JcePKCSPBEInputDecryptorProviderBuilder().setProvider("BC")
                        .build(password.toCharArray());
                
                org.bouncycastle.asn1.pkcs.PrivateKeyInfo keyInfo = encryptedKeyInfo.decryptPrivateKeyInfo(decryptorProvider);
                privateKey = converter.getPrivateKey(keyInfo);
                logger.info("✓ Llave privada descifrada exitosamente");
            } else if (pemObject instanceof org.bouncycastle.openssl.PEMKeyPair) {
                // Si ya está descifrada, solo convertirla
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
                privateKey = converter.getPrivateKey(((org.bouncycastle.openssl.PEMKeyPair) pemObject).getPrivateKeyInfo());
            } else if (pemObject instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
                privateKey = converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) pemObject);
            } else {
                throw new IllegalArgumentException("Formato de llave privada encriptada no soportado");
            }
            
            // Convertir la llave descifrada a formato PEM sin encriptar
            String keyPem = "-----BEGIN PRIVATE KEY-----\n";
            String base64Key = Base64.getEncoder().encodeToString(privateKey.getEncoded());
            for (int i = 0; i < base64Key.length(); i += 64) {
                int end = Math.min(i + 64, base64Key.length());
                keyPem += base64Key.substring(i, end) + "\n";
            }
            keyPem += "-----END PRIVATE KEY-----\n";
            
            return keyPem.getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Error al descifrar llave privada: {}", e.getMessage(), e);
            throw new Exception("No se pudo descifrar la llave privada: " + e.getMessage(), e);
        }
    }
    
    /**
     * Encripta la llave privada en formato PEM usando DES3 con la contraseña de Finkok
     * Paso 2 del proceso de Finkok: encriptar en DES3 usando finkok.password
     */
    private byte[] encriptarLlavePrivadaDES3(byte[] keyPemBytes, String passphrase) throws Exception {
        try {
            // Asegurar que BouncyCastle esté registrado
            if (java.security.Security.getProvider("BC") == null) {
                java.security.Security.addProvider(new BouncyCastleProvider());
            }
            
            // Leer la llave privada desde PEM
            String keyPemText = new String(keyPemBytes, StandardCharsets.UTF_8);
            
            // Verificar que realmente esté en formato PEM
            if (!keyPemText.contains("-----BEGIN") || !keyPemText.contains("-----END")) {
                throw new IllegalArgumentException("La llave privada debe estar en formato PEM para encriptarla en DES3");
            }
            
            PEMParser pemParser = new PEMParser(new java.io.StringReader(keyPemText));
            Object pemObject = pemParser.readObject();
            pemParser.close();
            
            PrivateKey privateKey = null;
            
            // Manejar diferentes formatos PEM
            if (pemObject instanceof org.bouncycastle.openssl.PEMKeyPair) {
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
                privateKey = converter.getPrivateKey(((org.bouncycastle.openssl.PEMKeyPair) pemObject).getPrivateKeyInfo());
                logger.debug("✓ Llave cargada desde PEMKeyPair");
            } else if (pemObject instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
                privateKey = converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) pemObject);
                logger.debug("✓ Llave cargada desde PrivateKeyInfo");
            } else if (pemObject instanceof org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo) {
                throw new IllegalArgumentException("La llave privada ya está encriptada. Debe descifrarse primero con la contraseña propia.");
            } else {
                throw new IllegalArgumentException("Formato de llave privada no soportado para encriptación DES3. Tipo: " + 
                    (pemObject != null ? pemObject.getClass().getName() : "null"));
            }
            
            if (privateKey == null) {
                throw new IllegalArgumentException("No se pudo extraer la llave privada del objeto PEM");
            }
            
            // Crear el builder para encriptar la llave privada
            PKCS8EncryptedPrivateKeyInfoBuilder encryptedKeyBuilder = new PKCS8EncryptedPrivateKeyInfoBuilder(
                org.bouncycastle.asn1.pkcs.PrivateKeyInfo.getInstance(privateKey.getEncoded())
            );
            
            // Encriptar usando DES-EDE3-CBC (Triple DES) exactamente como OpenSSL con -des3
            // OpenSSL "openssl rsa -des3" usa: PBEWithSHA1AndDESede (OID: 1.2.840.113549.1.12.1.3)
            // Este es el mismo algoritmo que pbeWithSHAAnd3_KeyTripleDES_CBC en BouncyCastle
            org.bouncycastle.operator.OutputEncryptor encryptor;
            try {
                // Usar el OID correcto para PBEWithSHA1AndDESede (equivalente a OpenSSL -des3)
                encryptor = new JcePKCSPBEOutputEncryptorBuilder(
                    org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC
                ).setProvider("BC").build(passphrase.toCharArray());
                logger.info("✓ Usando algoritmo PBEWithSHA1AndDESede (equivalente a OpenSSL -des3)");
            } catch (Exception e) {
                logger.error("Error al crear encriptador DES-EDE3-CBC: {}", e.getMessage(), e);
                throw new Exception("No se pudo crear el encriptador DES3: " + e.getMessage(), e);
            }
            
            org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo encryptedKeyInfo = encryptedKeyBuilder.build(encryptor);
            
            // Convertir a formato PEM encriptado
            PemObject pemObj = new PemObject("ENCRYPTED PRIVATE KEY", encryptedKeyInfo.getEncoded());
            java.io.StringWriter stringWriter = new java.io.StringWriter();
            PemWriter pemWriter = new PemWriter(stringWriter);
            pemWriter.writeObject(pemObj);
            pemWriter.close();
            
            String encryptedPem = stringWriter.toString();
            logger.info("✓ Llave privada encriptada en formato PEM ({} bytes)", encryptedPem.length());
            
            return encryptedPem.getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Error al encriptar llave privada: {}", e.getMessage(), e);
            throw new Exception("No se pudo encriptar la llave privada en DES3: " + e.getMessage(), e);
        }
    }
    
    /**
     * Crea el mensaje SOAP para el método cancel según documentación oficial de Finkok
     */
    private SOAPMessage createCancelSOAPRequest(PacRequest req, String username, String password, 
            String taxpayerId, String cerBase64, String keyBase64) throws Exception {
        System.setProperty("jakarta.xml.soap.MessageFactory", "com.sun.xml.messaging.saaj.soap.ver1_1.SOAPMessageFactory1_1Impl");
        MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        
        // Namespaces según documentación oficial
        String serverURI = "http://facturacion.finkok.com/cancel";
        String envelopeURI = "http://schemas.xmlsoap.org/soap/envelope/";
        String appsURI = "apps.services.soap.core.views";
        
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("soapenv", envelopeURI);
        envelope.addNamespaceDeclaration("can", serverURI);
        envelope.addNamespaceDeclaration("apps", appsURI);
        
        // Headers SOAP
        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", "cancel");
        headers.addHeader("Content-Type", "text/xml; charset=utf-8");
        
        SOAPBody soapBody = envelope.getBody();
        
        // Elemento raíz: <can:cancel>
        SOAPElement cancelElement = soapBody.addChildElement("cancel", "can");
        
        // Elemento: <can:UUIDS>
        SOAPElement uuidsElement = cancelElement.addChildElement("UUIDS", "can");
        
        // Elemento: <apps:UUID> con atributos
        // Formatear UUID con guiones (Finkok requiere formato estándar: 8-4-4-4-12)
        String uuidFormateado = formatearUUIDConGuiones(req.uuid);
        String uuidSustitutoFormateado = null;
        if (req.uuidSustituto != null && !req.uuidSustituto.trim().isEmpty()) {
            uuidSustitutoFormateado = formatearUUIDConGuiones(req.uuidSustituto.trim());
        }
        
        SOAPElement uuidElement = uuidsElement.addChildElement("UUID", "apps");
        uuidElement.setAttribute("UUID", uuidFormateado);
        uuidElement.setAttribute("Motivo", req.motivo);
        if (uuidSustitutoFormateado != null) {
            uuidElement.setAttribute("FolioSustitucion", uuidSustitutoFormateado);
        }
        
        // Elemento: <can:username>
        SOAPElement usernameElement = cancelElement.addChildElement("username", "can");
        usernameElement.addTextNode(username);
        
        // Elemento: <can:password>
        SOAPElement passwordElement = cancelElement.addChildElement("password", "can");
        passwordElement.addTextNode(password);
        
        // Elemento: <can:taxpayer_id>
        SOAPElement taxpayerElement = cancelElement.addChildElement("taxpayer_id", "can");
        taxpayerElement.addTextNode(taxpayerId);
        
        // Elemento: <can:cer> (certificado en Base64)
        SOAPElement cerElement = cancelElement.addChildElement("cer", "can");
        cerElement.addTextNode(cerBase64);
        
        // Elemento: <can:key> (llave privada en Base64)
        SOAPElement keyElement = cancelElement.addChildElement("key", "can");
        keyElement.addTextNode(keyBase64);
        
        // Elemento: <can:store_pending> (0 = no guardar en pending buffer)
        SOAPElement storePendingElement = cancelElement.addChildElement("store_pending", "can");
        storePendingElement.addTextNode("0");
        
        soapMessage.saveChanges();
        
        // Log del request SOAP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapMessage.writeTo(baos);
        String soapRequestStr = baos.toString(StandardCharsets.UTF_8);
        logger.info("=== SOAP REQUEST (cancel) ===");
        logger.info("{}", soapRequestStr);
        logger.info("=== FIN SOAP REQUEST ===");
        
        return soapMessage;
    }
    
    /**
     * Envía el request SOAP al endpoint de cancelación de Finkok
     */
    private String sendCancelSOAPRequest(SOAPMessage soapMessage) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapMessage.writeTo(baos);
        String soapRequest = baos.toString(StandardCharsets.UTF_8);
        
        String endpointUrl = finkokCancelUrl;
        if (endpointUrl.endsWith(".wsdl")) {
            endpointUrl = endpointUrl.replace(".wsdl", "");
        }
        
        // Si el túnel SSH está habilitado, usar localhost:8443 en lugar de la URL directa
        if (tunnelEnabled) {
            // Reemplazar la URL de Finkok con localhost:puerto del túnel
            try {
                java.net.URL originalUrl = new java.net.URL(endpointUrl);
                endpointUrl = String.format("https://localhost:%d%s", tunnelLocalPort, originalUrl.getPath());
                logger.info("Túnel SSH habilitado - Usando túnel local para cancelación: {}", endpointUrl);
            } catch (Exception e) {
                logger.warn("Error al construir URL del túnel, usando URL original: {}", e.getMessage());
            }
        }
        
        logger.info("Enviando request SOAP de cancelación a: {}", endpointUrl);
        
        // Configurar IPv4 si está habilitado
        if (useIpv4Only) {
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("java.net.preferIPv6Addresses", "false");
        }
        
        // Deshabilitar TODA detección automática de proxy (HTTP, HTTPS, SOCKS)
        // IMPORTANTE: Estas propiedades deben establecerse ANTES de crear el HttpClient
        System.setProperty("java.net.useSystemProxies", "false");
        System.setProperty("http.proxyHost", "");
        System.setProperty("http.proxyPort", "");
        System.setProperty("https.proxyHost", "");
        System.setProperty("https.proxyPort", "");
        System.setProperty("socksProxyHost", "");
        System.setProperty("socksProxyPort", "");
        System.setProperty("socksNonProxyHosts", "*");
        // Forzar que NO use proxy SOCKS a nivel de JVM
        System.setProperty("java.net.socks.username", "");
        System.setProperty("java.net.socks.password", "");
        
        // Crear un RoutePlanner personalizado que NUNCA use proxy
        // Esto evita que HttpClient detecte proxies automáticamente
        HttpRoutePlanner noProxyRoutePlanner = new HttpRoutePlanner() {
            @Override
            public HttpRoute determineRoute(
                    HttpHost target, HttpContext context) {
                // Asegurar que el puerto esté correctamente establecido
                int port = target.getPort();
                if (port < 0) {
                    // Si no hay puerto explícito, usar el puerto por defecto del esquema
                    String schemeName = target.getSchemeName();
                    if ("https".equalsIgnoreCase(schemeName)) {
                        port = 443;
                    } else if ("http".equalsIgnoreCase(schemeName)) {
                        port = 80;
                    } else {
                        port = 443; // Por defecto HTTPS
                    }
                    // Crear un nuevo HttpHost con el puerto correcto
                    HttpHost targetWithPort = new HttpHost(target.getSchemeName(), target.getHostName(), port);
                    // Siempre retornar ruta directa sin proxy
                    return new HttpRoute(targetWithPort);
                }
                // Si el puerto ya está establecido, usar el target original
                return new HttpRoute(target);
            }
        };
        
        // Usar Apache HttpClient con configuración explícita SIN proxy
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
            .setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout))
            .build();
        
        // Configurar HttpClient sin proxy de ninguna clase
        // El RoutePlanner personalizado asegura que nunca se use proxy
        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setRoutePlanner(noProxyRoutePlanner)
            .disableAutomaticRetries()
            .build();
        
        try {
            HttpPost httpPost = new HttpPost(endpointUrl);
            httpPost.setHeader("Content-Type", "text/xml; charset=utf-8");
            httpPost.setHeader("SOAPAction", "cancel");
            httpPost.setEntity(new StringEntity(soapRequest, StandardCharsets.UTF_8));
            
            logger.info("Conexión usando Apache HttpClient para cancelación (sin proxy automático)");
            
            return httpClient.execute(httpPost, response -> {
                try {
                    int statusCode = response.getCode();
                    String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                    
                    if (statusCode >= 400) {
                        logger.error("Error HTTP {} desde Finkok (cancelación): {}", statusCode, responseBody);
                        throw new RuntimeException("Error HTTP " + statusCode + " desde Finkok: " + responseBody);
                    }
                    
                    logger.debug("Respuesta de cancelación recibida: {} caracteres", responseBody.length());
                    return responseBody;
                } catch (IOException e) {
                    throw new RuntimeException("Error leyendo respuesta de cancelación de Finkok", e);
                }
            });
        } catch (IOException e) {
            logger.error("Error de conexión con Finkok (cancelación): {}", e.getMessage());
            throw new Exception("Error de conexión con Finkok: " + e.getMessage() + 
                ". Verifica conectividad de red y firewall desde el servidor.", e);
        } finally {
            httpClient.close();
        }
    }
    
    /**
     * Parsea la respuesta SOAP del método cancel
     */
    private PacResponse parseCancelSOAPResponse(String soapResponse, String uuid) throws Exception {
        PacResponse response = new PacResponse();
        
        // Parsear XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new java.io.ByteArrayInputStream(soapResponse.getBytes(StandardCharsets.UTF_8)));
        
        // Namespaces
        String nsTns = "http://facturacion.finkok.com/cancel";
        String nsS0 = "apps.services.soap.core.views";
        
        // Buscar cancelResult
        NodeList resultList = doc.getElementsByTagNameNS(nsTns, "cancelResult");
        if (resultList.getLength() == 0) {
            resultList = doc.getElementsByTagName("cancelResult");
        }
        
        if (resultList.getLength() == 0) {
            // Buscar CodEstatus directamente (puede estar en la raíz del resultado)
            NodeList codEstatusList = doc.getElementsByTagNameNS(nsS0, "CodEstatus");
            if (codEstatusList.getLength() == 0) {
                codEstatusList = doc.getElementsByTagName("CodEstatus");
            }
            if (codEstatusList.getLength() > 0) {
                String codEstatus = codEstatusList.item(0).getTextContent();
                response.setOk(false);
                response.setStatus("ERROR");
                response.setMessage(codEstatus);
                return response;
            }
            throw new Exception("No se encontró cancelResult ni CodEstatus en la respuesta SOAP");
        }
        
        Element cancelResult = (Element) resultList.item(0);
        
        // Buscar CodEstatus (puede indicar error)
        String codEstatus = getElementText(cancelResult, "CodEstatus", nsS0);
        if (codEstatus != null && !codEstatus.trim().isEmpty()) {
            response.setOk(false);
            response.setStatus("ERROR");
            response.setMessage(codEstatus);
            return response;
        }
        
        // Buscar Folios (éxito)
        NodeList foliosList = cancelResult.getElementsByTagNameNS(nsS0, "Folios");
        if (foliosList.getLength() == 0) {
            foliosList = cancelResult.getElementsByTagName("Folios");
        }
        
        if (foliosList.getLength() > 0) {
            Element folios = (Element) foliosList.item(0);
            NodeList folioList = folios.getElementsByTagNameNS(nsS0, "Folio");
            if (folioList.getLength() == 0) {
                folioList = folios.getElementsByTagName("Folio");
            }
            
            if (folioList.getLength() > 0) {
                Element folio = (Element) folioList.item(0);
                String estatusUUID = getElementText(folio, "EstatusUUID", nsS0);
                String estatusCancelacion = getElementText(folio, "EstatusCancelacion", nsS0);
                
                // EstatusUUID 201 = Petición de cancelación realizada exitosamente
                if ("201".equals(estatusUUID)) {
                    response.setOk(true);
                    response.setStatus("CANCELADA");
                    response.setMessage(estatusCancelacion != null ? estatusCancelacion : "Cancelación exitosa");
                } else {
                    response.setOk(false);
                    response.setStatus("ERROR");
                    response.setMessage(estatusCancelacion != null ? estatusCancelacion : "Error en cancelación. EstatusUUID: " + estatusUUID);
                }
            } else {
                response.setOk(false);
                response.setStatus("ERROR");
                response.setMessage("No se encontró información de folio en la respuesta");
            }
        } else {
            response.setOk(false);
            response.setStatus("ERROR");
            response.setMessage("No se encontró información de folios en la respuesta");
        }
        
        return response;
    }
}
