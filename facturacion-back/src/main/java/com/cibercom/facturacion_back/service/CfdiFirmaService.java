package com.cibercom.facturacion_back.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Arrays;
import java.util.Enumeration;

// BouncyCastle para soporte de múltiples formatos de llaves privadas
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;

/**
 * Servicio para firmar XML CFDI 4.0 con CSD (Certificado de Sello Digital) del emisor
 * Según lineamientos del SAT para CFDI 4.0
 */
@Service
public class CfdiFirmaService {
    private static final Logger logger = LoggerFactory.getLogger(CfdiFirmaService.class);
    private static final ZoneId ZONA_HORARIA_MX = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter CFDI_FECHA_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Duration MAX_DESVIACION_FECHA = Duration.ofMinutes(5);
    
    @Autowired
    private Environment environment;
    
    @Value("${facturacion.csd.certificado.path:classpath:certificados/CSD.cer}")
    private String certificadoPath;
    
    @Value("${facturacion.csd.llave.path:classpath:certificados/CSD.key}")
    private String llavePath;
    
    @Value("${facturacion.csd.llave.password:}")
    private String llavePassword;
    
    @Value("${facturacion.emisor.rfc:}")
    private String rfcEmisorConfigurado;
    
    // NO usar @Value aquí - leer directamente del Environment para evitar problemas de caché
    private boolean firmaEnabled;
    
    private boolean llaveCargada = false;
    private PrivateKey llavePrivadaCache = null;
    private X509Certificate certificadoCache = null;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        logger.info("=== INICIALIZANDO CfdiFirmaService ===");
        
        // Registrar BouncyCastle como proveedor JCE (necesario para algoritmos como RC2-CBC)
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
                logger.info("✓ BouncyCastle JCE Provider registrado exitosamente");
            } else {
                logger.info("✓ BouncyCastle JCE Provider ya estaba registrado");
            }
        } catch (Exception e) {
            logger.warn("⚠️ No se pudo registrar BouncyCastle JCE Provider: {}", e.getMessage());
            logger.warn("⚠️ Algunos algoritmos de cifrado (como RC2-CBC) pueden no funcionar correctamente");
        }
        
        // Leer DIRECTAMENTE del Environment (no usar @Value que puede estar cacheando)
        String enabledStr = environment.getProperty("facturacion.csd.enabled", "false");
        firmaEnabled = Boolean.parseBoolean(enabledStr);
        
        // Leer RFC del emisor desde application.yml
        if (rfcEmisorConfigurado == null || rfcEmisorConfigurado.trim().isEmpty()) {
            rfcEmisorConfigurado = environment.getProperty("facturacion.emisor.rfc", "");
        }
        
        logger.info("✓ Propiedad facturacion.csd.enabled leída DIRECTAMENTE del Environment: {}", firmaEnabled);
        logger.info("✓ RFC Emisor configurado en application.yml: {}", 
                rfcEmisorConfigurado != null && !rfcEmisorConfigurado.trim().isEmpty() ? rfcEmisorConfigurado : "NO CONFIGURADO");
        logger.info("✓ Ruta certificado: {}", certificadoPath);
        logger.info("✓ Ruta llave: {}", llavePath != null && !llavePath.isEmpty() ? "***CONFIGURADA***" : "NO CONFIGURADA");
        logger.info("✓ Contraseña llave: {}", llavePassword != null && !llavePassword.isEmpty() ? "***CONFIGURADA***" : "NO CONFIGURADA");
        
        if (firmaEnabled) {
            logger.info("Configuración CSD - Certificado: {}, Llave: {}", certificadoPath, llavePath);
            
            // Intentar cargar certificado y llave al inicio para validar configuración
            try {
                certificadoCache = cargarCertificado();
                llavePrivadaCache = cargarLlavePrivada();
                if (certificadoCache != null && llavePrivadaCache != null) {
                    llaveCargada = true;
                    logger.info("✓✓✓ Certificado y llave privada cargados exitosamente al inicio");
                    logger.info("=== CfdiFirmaService LISTO PARA FIRMAR XML ===");
                } else {
                    llaveCargada = false;
                    logger.error("✗✗✗ No se pudieron cargar el certificado o la llave privada al inicio");
                    logger.error("✗✗✗ La firma digital se deshabilitará automáticamente. El XML se enviará sin firma.");
                    logger.error("✗✗✗ Para resolver: Verifique las rutas de certificado y llave en application.yml");
                    firmaEnabled = false; // Deshabilitar automáticamente si falla
                    logger.error("=== CfdiFirmaService DESHABILITADO ===");
                }
            } catch (Exception e) {
                logger.error("✗✗✗ Error al cargar certificado o llave privada al inicio: {}", e.getMessage(), e);
                logger.error("✗✗✗ La firma digital se deshabilitará automáticamente. El XML se enviará sin firma.");
                logger.error("✗✗✗ Para resolver: Verifique las rutas de certificado y llave en application.yml");
                llaveCargada = false;
                firmaEnabled = false; // Deshabilitar automáticamente si falla
                logger.error("=== CfdiFirmaService DESHABILITADO ===");
            }
        } else {
            logger.error("✗✗✗ Firma digital DESHABILITADA (facturacion.csd.enabled=false)");
            logger.error("✗✗✗ Para habilitar, configure facturacion.csd.enabled=true en application.yml");
            logger.error("✗✗✗ Sin firma digital, CFDI 4.0 será rechazado por Finkok con error 302");
            logger.error("=== CfdiFirmaService DESHABILITADO ===");
        }
    }
    
    /**
     * Firma un XML CFDI 4.0 con el CSD del emisor
     * Agrega los atributos: Certificado, NoCertificado y Sello
     * 
     * @param xmlContent XML sin firmar
     * @return XML firmado con los atributos de certificado y sello, o XML original si falla
     */
    public String firmarXml(String xmlContent) {
        // Verificar estado de la firma digital
        logger.info("Estado de firma digital - enabled: {}, llaveCargada: {}, certificadoCache: {}, llavePrivadaCache: {}", 
                firmaEnabled, llaveCargada, certificadoCache != null, llavePrivadaCache != null);
        
        // Si está deshabilitado, verificar nuevamente desde Environment (por si cambió sin reiniciar)
        if (!firmaEnabled) {
            String enabledStr = environment.getProperty("facturacion.csd.enabled", "false");
            boolean enabledFromEnv = Boolean.parseBoolean(enabledStr);
            if (enabledFromEnv != firmaEnabled) {
                logger.warn("⚠️ Valor cambió en Environment. enabled anterior: {}, nuevo: {}", firmaEnabled, enabledFromEnv);
                firmaEnabled = enabledFromEnv;
            }
        }
        
        if (!firmaEnabled || !llaveCargada) {
            if (!firmaEnabled) {
                logger.error("✗✗✗ Firma digital DESHABILITADA en configuración (facturacion.csd.enabled=false)");
                logger.error("✗✗✗ Verificado desde Environment: {}", environment.getProperty("facturacion.csd.enabled", "NO ENCONTRADO"));
                logger.error("✗✗✗ SOLUCIÓN: Cambia facturacion.csd.enabled=true en application.yml y REINICIA la aplicación");
            } else {
                logger.error("✗✗✗ Firma digital habilitada pero certificado/llave NO se pudieron cargar al inicio.");
                logger.error("✗✗✗ Revisa los logs de inicio de la aplicación para ver el error al cargar los certificados.");
                logger.error("✗✗✗ Verifica que los archivos existan en src/main/resources/certificados/");
            }
            return xmlContent;
        }
        
        try {
            logger.info("=== INICIANDO FIRMA DIGITAL DEL XML CON CSD ===");
            
            // Usar certificado y llave del cache (ya cargados en init)
            X509Certificate certificado = certificadoCache;
            PrivateKey llavePrivada = llavePrivadaCache;
            
            if (certificado == null || llavePrivada == null) {
                logger.warn("Certificado o llave privada no disponibles en cache. Intentando recargar...");
                try {
                    certificado = cargarCertificado();
                    llavePrivada = cargarLlavePrivada();
                    if (certificado != null && llavePrivada != null) {
                        certificadoCache = certificado;
                        llavePrivadaCache = llavePrivada;
                        logger.info("✓ Certificado y llave recargados exitosamente");
                    } else {
                        logger.error("❌ No se pudo recargar certificado/llave. Continuando sin firma.");
                        return xmlContent;
                    }
                } catch (Exception e) {
                    logger.error("❌ Error al recargar certificado/llave: {}", e.getMessage(), e);
                    return xmlContent;
                }
            }
            
            // Verificar que tenemos certificado y llave válidos
            if (certificado == null || llavePrivada == null) {
                logger.error("❌ Certificado o llave privada son null después de recargar. Continuando sin firma.");
                return xmlContent;
            }
            
            logger.info("✓ Certificado disponible: Subject={}, SerialNumber={}", 
                    certificado.getSubjectX500Principal().getName(), certificado.getSerialNumber());
            logger.info("✓ Llave privada disponible: Algoritmo={}, Formato={}", 
                    llavePrivada.getAlgorithm(), llavePrivada.getFormat());
            
            // Validar que la llave privada corresponde al certificado
            try {
                PublicKey publicKey = certificado.getPublicKey();
                if (publicKey.getAlgorithm().equals(llavePrivada.getAlgorithm())) {
                    // Hacer una prueba de firma/verificación
                    String testData = "test";
                    Signature testSig = Signature.getInstance("SHA256withRSA");
                    testSig.initSign(llavePrivada);
                    testSig.update(testData.getBytes(StandardCharsets.UTF_8));
                    byte[] signature = testSig.sign();
                    
                    testSig.initVerify(publicKey);
                    testSig.update(testData.getBytes(StandardCharsets.UTF_8));
                    boolean verified = testSig.verify(signature);
                    
                    if (verified) {
                        logger.info("✓ Validación: La llave privada corresponde al certificado");
                    } else {
                        logger.error("✗✗✗ ERROR CRÍTICO: La llave privada NO corresponde al certificado. Esto causará error CFDI40102.");
                        throw new IllegalStateException("La llave privada no corresponde al certificado. Verifica que los archivos .cer y .key sean del mismo CSD.");
                    }
                } else {
                    logger.warn("⚠️ Algoritmos diferentes: Certificado={}, Llave={}", 
                            publicKey.getAlgorithm(), llavePrivada.getAlgorithm());
                }
            } catch (Exception e) {
                logger.error("✗✗✗ ERROR al validar correspondencia llave/certificado: {}", e.getMessage());
                logger.error("✗✗✗ Esto puede causar error CFDI40102. Verifica que la llave privada corresponda al certificado.");
                throw new IllegalStateException("Error al validar correspondencia entre llave privada y certificado: " + e.getMessage(), e);
            }
            
            // Extraer RFC del certificado para validación
            String subjectDN = certificado.getSubjectX500Principal().getName();
            String rfcCertificado = extraerRFCDeSubjectDN(subjectDN);
            logger.info("✓ RFC extraído del certificado: {}", rfcCertificado != null ? rfcCertificado : "NO ENCONTRADO");
            
            // 2. Obtener número de serie del certificado (NoCertificado)
            String noCertificado = obtenerNoCertificado(certificado);
            logger.info("✓ NoCertificado obtenido: {}", noCertificado);
            
            // 3. Obtener certificado en Base64 (sin headers PEM)
            String certificadoBase64 = obtenerCertificadoBase64(certificado);
            logger.info("✓ Certificado en Base64: {} caracteres", certificadoBase64.length());
            
            // 4. Parsear XML
            logger.info("Parseando XML para firmar...");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc;
            try {
                doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
                logger.info("✓ XML parseado exitosamente");
            } catch (Exception e) {
                logger.error("❌ Error al parsear XML: {}", e.getMessage(), e);
                throw new IllegalStateException("Error al parsear XML: " + e.getMessage(), e);
            }
            
            // Verificar que el XML tenga el elemento Comprobante (CFDI 4.0) o Retenciones (Retenciones)
            Element elementoRaiz = null;
            boolean esRetencion = false;
            
            // Buscar Comprobante (CFDI 4.0)
            elementoRaiz = (Element) doc.getElementsByTagNameNS("http://www.sat.gob.mx/cfd/4", "Comprobante").item(0);
            if (elementoRaiz == null) {
                elementoRaiz = (Element) doc.getElementsByTagName("Comprobante").item(0);
            }
            
            // Si no es CFDI 4.0, buscar Retenciones
            if (elementoRaiz == null) {
                elementoRaiz = (Element) doc.getElementsByTagNameNS("http://www.sat.gob.mx/esquemas/retencionpago/2", "Retenciones").item(0);
                if (elementoRaiz == null) {
                    elementoRaiz = (Element) doc.getElementsByTagName("Retenciones").item(0);
                }
                if (elementoRaiz != null) {
                    esRetencion = true;
                    logger.info("✓ Elemento Retenciones encontrado en XML (CFDI de Retenciones)");
                }
            } else {
                logger.info("✓ Elemento Comprobante encontrado en XML (CFDI 4.0)");
            }
            
            if (elementoRaiz == null) {
                logger.error("❌ No se encontró el elemento Comprobante ni Retenciones en el XML antes de generar cadena original");
                throw new IllegalStateException("No se encontró el elemento Comprobante ni Retenciones en el XML");
            }

            // Normalizar la fecha del comprobante/retenciones para evitar errores de fecha fuera de rango
            if (esRetencion) {
                // Para retenciones, normalizar FechaExp
                String fechaExp = elementoRaiz.getAttribute("FechaExp");
                if (fechaExp != null && !fechaExp.isEmpty()) {
                    try {
                        // Validar formato de fecha (debe ser ISO 8601)
                        logger.info("FechaExp en Retenciones: {}", fechaExp);
                    } catch (Exception e) {
                        logger.warn("⚠️ FechaExp en formato inválido: {}", fechaExp);
                    }
                }
            } else {
                normalizarFechaComprobante(elementoRaiz);
                // Aplicar reglas especiales para Complemento de Pagos (TipoDeComprobante = P)
                normalizarAtributosComplementoPago(doc, elementoRaiz);
            }

            // CRÍTICO: El RFC del emisor debe tomarse del application.yml, no del XML
            // Si el XML tiene un RFC diferente, se sobrescribe con el configurado en application.yml
            if (rfcCertificado != null && !rfcCertificado.isEmpty()) {
                // Buscar el elemento Emisor en el XML (CFDI 4.0 o Retenciones)
                Element emisorXML = null;
                org.w3c.dom.NodeList emisoresList = null;
                
                if (esRetencion) {
                    // Para retenciones, buscar en namespace de retenciones
                    emisoresList = doc.getElementsByTagNameNS("http://www.sat.gob.mx/esquemas/retencionpago/2", "Emisor");
                    if (emisoresList.getLength() == 0) {
                        emisoresList = doc.getElementsByTagName("Emisor");
                    }
                } else {
                    // Para CFDI 4.0
                    emisoresList = doc.getElementsByTagNameNS("http://www.sat.gob.mx/cfd/4", "Emisor");
                    if (emisoresList.getLength() == 0) {
                        emisoresList = doc.getElementsByTagName("Emisor");
                    }
                }
                
                if (emisoresList != null && emisoresList.getLength() > 0) {
                    emisorXML = (Element) emisoresList.item(0);
                }
                
                if (emisorXML != null) {
                    // Para retenciones, el atributo es RfcE; para CFDI 4.0 es Rfc
                    String rfcEmisorXML = esRetencion ? emisorXML.getAttribute("RfcE") : emisorXML.getAttribute("Rfc");
                    if (rfcEmisorXML != null && !rfcEmisorXML.isEmpty()) {
                        logger.info("RFC Emisor en XML: {}", rfcEmisorXML);
                        
                        // Determinar el RFC que debe usarse (prioridad: application.yml > certificado > XML)
                        String rfcEmisorFinal = rfcCertificado; // Por defecto usar el del certificado
                        if (rfcEmisorConfigurado != null && !rfcEmisorConfigurado.trim().isEmpty()) {
                            rfcEmisorFinal = rfcEmisorConfigurado.trim();
                            logger.info("✓ RFC Emisor configurado en application.yml: {}", rfcEmisorFinal);
                        }
                        
                        // Si el RFC del XML es diferente, sobrescribirlo con el configurado
                        if (!rfcEmisorFinal.equalsIgnoreCase(rfcEmisorXML)) {
                            logger.warn("⚠️ El RFC del emisor en el XML ({}) es diferente al configurado ({}). Corrigiendo automáticamente...", 
                                    rfcEmisorXML, rfcEmisorFinal);
                            // Para retenciones usar RfcE, para CFDI 4.0 usar Rfc
                            if (esRetencion) {
                                emisorXML.setAttribute("RfcE", rfcEmisorFinal);
                            } else {
                                emisorXML.setAttribute("Rfc", rfcEmisorFinal);
                            }
                            logger.info("✓ RFC del emisor corregido en el XML: {} -> {}", rfcEmisorXML, rfcEmisorFinal);
                        }
                        
                        // Validar que el RFC final coincida con el del certificado
                        if (!rfcCertificado.equalsIgnoreCase(rfcEmisorFinal)) {
                            logger.error("✗✗✗ ERROR: El RFC configurado en application.yml ({}) NO coincide con el RFC del certificado ({}).", 
                                    rfcEmisorFinal, rfcCertificado);
                            logger.error("✗✗✗ Esto causará error 712 en Finkok: 'El atributo noCertificado no corresponde al certificado'");
                            logger.error("✗✗✗ SOLUCIÓN: Configura facturacion.emisor.rfc={} en application.yml o usa un certificado para el RFC {}", 
                                    rfcCertificado, rfcEmisorFinal);
                            throw new IllegalStateException(
                                String.format("El RFC configurado en application.yml (%s) no coincide con el RFC del certificado (%s). " +
                                    "El certificado debe corresponder al RFC configurado.", rfcEmisorFinal, rfcCertificado)
                            );
                        } else {
                            logger.info("✓ RFC del emisor (configurado: {}) coincide con el RFC del certificado: {}", 
                                    rfcEmisorFinal, rfcCertificado);
                        }
                    } else {
                        // Si no hay RFC en el XML, agregar el configurado
                        String rfcEmisorFinal = rfcCertificado;
                        if (rfcEmisorConfigurado != null && !rfcEmisorConfigurado.trim().isEmpty()) {
                            rfcEmisorFinal = rfcEmisorConfigurado.trim();
                        }
                        emisorXML.setAttribute("Rfc", rfcEmisorFinal);
                        logger.info("✓ RFC del emisor agregado al XML desde configuración: {}", rfcEmisorFinal);
                    }
                } else {
                    logger.warn("⚠️ No se encontró el elemento Emisor en el XML");
                }
            } else {
                logger.warn("⚠️ No se pudo extraer el RFC del certificado. No se puede validar coincidencia.");
            }
            
            // CRÍTICO: Corregir LugarExpedicion si es "00000" (inválido según SAT)
            // Leer el código postal del application.yml
            String cpEmisorConfigurado = environment.getProperty("facturacion.emisor.cp", "58000");
            if (cpEmisorConfigurado == null || cpEmisorConfigurado.trim().isEmpty() || "00000".equals(cpEmisorConfigurado.trim())) {
                cpEmisorConfigurado = "58000"; // Valor por defecto válido
            }
            
            // CRÍTICO: El nombre del emisor debe tomarse del application.yml
            String nombreEmisorConfigurado = environment.getProperty("facturacion.emisor.nombre", "");
            
            // Obtener el elemento raíz para corregir LugarExpedicion (solo para CFDI 4.0, no retenciones)
            if (!esRetencion) {
                Element comprobanteParaLugar = elementoRaiz;
                if (comprobanteParaLugar != null) {
                    String lugarExpedicionXML = comprobanteParaLugar.getAttribute("LugarExpedicion");
                    if (lugarExpedicionXML != null && !lugarExpedicionXML.isEmpty()) {
                        logger.info("LugarExpedicion en XML: '{}'", lugarExpedicionXML);
                        if ("00000".equals(lugarExpedicionXML)) {
                            logger.warn("⚠️ LugarExpedicion es '00000' (inválido según SAT). Corrigiendo a '{}'", cpEmisorConfigurado);
                            comprobanteParaLugar.setAttribute("LugarExpedicion", cpEmisorConfigurado.trim());
                            logger.info("✓ LugarExpedicion corregido en el XML: '00000' -> '{}'", cpEmisorConfigurado.trim());
                        }
                    } else {
                        // Si no hay LugarExpedicion, agregarlo
                        logger.warn("⚠️ No se encontró LugarExpedicion en el XML. Agregando '{}'", cpEmisorConfigurado);
                        comprobanteParaLugar.setAttribute("LugarExpedicion", cpEmisorConfigurado.trim());
                        logger.info("✓ LugarExpedicion agregado al XML: '{}'", cpEmisorConfigurado.trim());
                    }
                }
            } else {
                // Para retenciones, verificar/corregir LugarExpRetenc
                String lugarExpRetencXML = elementoRaiz.getAttribute("LugarExpRetenc");
                if (lugarExpRetencXML != null && !lugarExpRetencXML.isEmpty()) {
                    logger.info("LugarExpRetenc en XML: '{}'", lugarExpRetencXML);
                    if ("00000".equals(lugarExpRetencXML)) {
                        logger.warn("⚠️ LugarExpRetenc es '00000' (inválido según SAT). Corrigiendo a '{}'", cpEmisorConfigurado);
                        elementoRaiz.setAttribute("LugarExpRetenc", cpEmisorConfigurado.trim());
                        logger.info("✓ LugarExpRetenc corregido en el XML: '00000' -> '{}'", cpEmisorConfigurado.trim());
                    }
                } else {
                    logger.warn("⚠️ No se encontró LugarExpRetenc en el XML. Agregando '{}'", cpEmisorConfigurado);
                    elementoRaiz.setAttribute("LugarExpRetenc", cpEmisorConfigurado.trim());
                    logger.info("✓ LugarExpRetenc agregado al XML: '{}'", cpEmisorConfigurado.trim());
                }
            }
            
            // CRÍTICO: Corregir el nombre del emisor si es diferente al configurado
            // Buscar el elemento Emisor nuevamente para corregir el nombre
            Element emisorParaNombre = null;
            org.w3c.dom.NodeList emisoresListParaNombre = null;
            if (esRetencion) {
                emisoresListParaNombre = doc.getElementsByTagNameNS("http://www.sat.gob.mx/esquemas/retencionpago/2", "Emisor");
                if (emisoresListParaNombre.getLength() == 0) {
                    emisoresListParaNombre = doc.getElementsByTagName("Emisor");
                }
            } else {
                emisoresListParaNombre = doc.getElementsByTagNameNS("http://www.sat.gob.mx/cfd/4", "Emisor");
                if (emisoresListParaNombre.getLength() == 0) {
                    emisoresListParaNombre = doc.getElementsByTagName("Emisor");
                }
            }
            if (emisoresListParaNombre != null && emisoresListParaNombre.getLength() > 0) {
                emisorParaNombre = (Element) emisoresListParaNombre.item(0);
            }
            
            if (emisorParaNombre != null && nombreEmisorConfigurado != null && !nombreEmisorConfigurado.trim().isEmpty()) {
                // Para retenciones el atributo es NomDenRazSocE, para CFDI 4.0 es Nombre
                String nombreEmisorXML = esRetencion ? emisorParaNombre.getAttribute("NomDenRazSocE") : emisorParaNombre.getAttribute("Nombre");
                if (nombreEmisorXML != null && !nombreEmisorXML.isEmpty()) {
                    logger.info("Nombre Emisor en XML: '{}'", nombreEmisorXML);
                    if (!nombreEmisorConfigurado.trim().equalsIgnoreCase(nombreEmisorXML.trim())) {
                        logger.warn("⚠️ El nombre del emisor en el XML ('{}') es diferente al configurado ('{}'). Corrigiendo automáticamente...", 
                                nombreEmisorXML, nombreEmisorConfigurado);
                        // Para retenciones usar NomDenRazSocE, para CFDI 4.0 usar Nombre
                        if (esRetencion) {
                            emisorParaNombre.setAttribute("NomDenRazSocE", nombreEmisorConfigurado.trim());
                        } else {
                            emisorParaNombre.setAttribute("Nombre", nombreEmisorConfigurado.trim());
                        }
                        logger.info("✓ Nombre del emisor corregido en el XML: '{}' -> '{}'", nombreEmisorXML, nombreEmisorConfigurado.trim());
                    }
                } else {
                    // Si no hay Nombre/NomDenRazSocE, agregarlo
                    logger.warn("⚠️ No se encontró {} en el elemento Emisor. Agregando '{}'", 
                            esRetencion ? "NomDenRazSocE" : "Nombre", nombreEmisorConfigurado);
                    if (esRetencion) {
                        emisorParaNombre.setAttribute("NomDenRazSocE", nombreEmisorConfigurado.trim());
                    } else {
                        emisorParaNombre.setAttribute("Nombre", nombreEmisorConfigurado.trim());
                    }
                    logger.info("✓ Nombre del emisor agregado al XML: '{}'", nombreEmisorConfigurado.trim());
                }
            }
            
            // CRÍTICO: Corregir UsoCFDI si no corresponde con el tipo de persona y régimen fiscal
            // Buscar el elemento Receptor para corregir el UsoCFDI
            Element receptorParaUsoCFDI = null;
            org.w3c.dom.NodeList receptoresList = doc.getElementsByTagNameNS("http://www.sat.gob.mx/cfd/4", "Receptor");
            if (receptoresList.getLength() == 0) {
                receptoresList = doc.getElementsByTagName("Receptor");
            }
            if (receptoresList.getLength() > 0) {
                receptorParaUsoCFDI = (Element) receptoresList.item(0);
            }
            
            if (receptorParaUsoCFDI != null) {
                String rfcReceptor = receptorParaUsoCFDI.getAttribute("Rfc");
                String regimenFiscalReceptor = receptorParaUsoCFDI.getAttribute("RegimenFiscalReceptor");
                String usoCfdiXML = receptorParaUsoCFDI.getAttribute("UsoCFDI");
                
                if (rfcReceptor != null && !rfcReceptor.trim().isEmpty() && 
                    usoCfdiXML != null && !usoCfdiXML.trim().isEmpty()) {
                    String usoCfdiCorregido = validarYCorregirUsoCFDI(usoCfdiXML, rfcReceptor, regimenFiscalReceptor);
                    if (!usoCfdiXML.equals(usoCfdiCorregido)) {
                        logger.warn("⚠️ UsoCFDI '{}' no es válido para RFC {} (régimen: {}). Corrigiendo a '{}'", 
                                usoCfdiXML, rfcReceptor, regimenFiscalReceptor, usoCfdiCorregido);
                        receptorParaUsoCFDI.setAttribute("UsoCFDI", usoCfdiCorregido);
                        logger.info("✓ UsoCFDI corregido en el XML: '{}' -> '{}'", usoCfdiXML, usoCfdiCorregido);
                    }
                }
            }
            
            // 4.5. Agregar NoCertificado ANTES de generar cadena original (requerido por el XSLT del SAT)
            // IMPORTANTE: El XSLT oficial del SAT requiere que NoCertificado esté presente en el XML
            // antes de generar la cadena original, pero NO debe tener Certificado ni Sello aún
            // Usar elementoRaiz que ya fue detectado (Comprobante para CFDI 4.0 o Retenciones para retenciones)
            Element elementoParaCadena = elementoRaiz;
            if (elementoParaCadena != null) {
                // CORRECCIÓN AUTOMÁTICA DE FORMA PAGO Y METODO PAGO (solo para CFDI 4.0, no retenciones)
                // Detectar y corregir valores intercambiados entre FormaPago y MetodoPago
                if (!esRetencion) {
                    String formaPago = elementoParaCadena.hasAttribute("FormaPago") 
                        ? elementoParaCadena.getAttribute("FormaPago") : "";
                    String metodoPago = elementoParaCadena.hasAttribute("MetodoPago") 
                        ? elementoParaCadena.getAttribute("MetodoPago") : "";
                    
                    boolean necesitaCorreccion = false;
                    
                    // Si MetodoPago='03' (que es un código de FormaPago), corregir a 'PUE'
                    if ("03".equals(metodoPago)) {
                        logger.warn("⚠️ DETECTADO ERROR SEMÁNTICO: MetodoPago='03'. Corrigiendo automáticamente a 'PUE' ANTES de generar cadena original.");
                        elementoParaCadena.setAttribute("MetodoPago", "PUE");
                        metodoPago = "PUE";
                        necesitaCorreccion = true;
                        logger.info("✓ MetodoPago corregido a 'PUE' antes de generar cadena original");
                        
                        // Si FormaPago está vacío o tiene un valor incorrecto, establecer '03'
                        if (formaPago.isEmpty() || "PUE".equals(formaPago) || "PPD".equals(formaPago)) {
                            elementoParaCadena.setAttribute("FormaPago", "03");
                            logger.info("✓ FormaPago establecido a '03' (Transferencia electrónica de fondos)");
                        }
                    }
                    
                    // Si FormaPago='PUE' o 'PPD' (que son valores de MetodoPago), corregir
                    if ("PUE".equals(formaPago) || "PPD".equals(formaPago)) {
                        logger.warn("⚠️ DETECTADO ERROR SEMÁNTICO: FormaPago='{}'. Corrigiendo automáticamente a '03' ANTES de generar cadena original.", formaPago);
                        elementoParaCadena.setAttribute("FormaPago", "03");
                        logger.info("✓ FormaPago corregido a '03' antes de generar cadena original");
                        necesitaCorreccion = true;
                        
                        // Si MetodoPago está vacío, establecer el valor que estaba en FormaPago
                        if (metodoPago.isEmpty() || "03".equals(metodoPago)) {
                            String nuevoMetodoPago = "PUE".equals(formaPago) ? "PUE" : "PPD";
                            elementoParaCadena.setAttribute("MetodoPago", nuevoMetodoPago);
                            logger.info("✓ MetodoPago establecido a '{}'", nuevoMetodoPago);
                        }
                    }
                    
                    if (necesitaCorreccion) {
                        logger.info("✓ Correcciones aplicadas: FormaPago='{}', MetodoPago='{}'", 
                            elementoParaCadena.getAttribute("FormaPago"),
                            elementoParaCadena.getAttribute("MetodoPago"));
                    }
                }
                
                // Verificar si ya tiene NoCertificado (no debería, pero por si acaso)
                if (!elementoParaCadena.hasAttribute("NoCertificado")) {
                    elementoParaCadena.setAttribute("NoCertificado", noCertificado);
                    logger.info("✓ NoCertificado agregado al XML antes de generar cadena original: {}", noCertificado);
                } else {
                    logger.debug("NoCertificado ya estaba presente en el XML");
                }
                // Asegurar que NO tenga Certificado ni Sello aún
                if (elementoParaCadena.hasAttribute("Certificado")) {
                    elementoParaCadena.removeAttribute("Certificado");
                    logger.warn("⚠️ Certificado removido temporalmente para generar cadena original");
                }
                if (elementoParaCadena.hasAttribute("Sello")) {
                    elementoParaCadena.removeAttribute("Sello");
                    logger.warn("⚠️ Sello removido temporalmente para generar cadena original");
                }
            }
            
            // 5. Reordenar atributos del elemento raíz ANTES de generar la cadena original
            // CRÍTICO: El orden de los atributos DEBE coincidir con el orden del XSLT para que la cadena original sea correcta
            if (esRetencion && elementoRaiz != null) {
                reordenarAtributosRetencion(elementoRaiz);
                logger.debug("✓ Atributos reordenados antes de generar cadena original");
            }
            
            // 6. Generar cadena original
            logger.info("Generando cadena original...");
            
            // DEBUG: Log del XML que se usará para generar la cadena original (sin Certificado ni Sello)
            // CRÍTICO: Este XML debe tener exactamente el mismo formato que el XML final (excepto Certificado y Sello)
            try {
                String xmlParaCadena = documentToString(doc);
                // Remover Certificado y Sello si existen temporalmente para el log
                xmlParaCadena = xmlParaCadena.replaceAll(" Certificado=\"[^\"]*\"", "");
                xmlParaCadena = xmlParaCadena.replaceAll(" Sello=\"[^\"]*\"", "");
                // Normalizar espacios para consistencia (debe ser idéntico al XML final)
                xmlParaCadena = xmlParaCadena.replaceAll(">\\s+<", "><").trim();
                logger.debug("=== XML USADO PARA GENERAR CADENA ORIGINAL (primeros 500 chars) ===");
                logger.debug("{}", xmlParaCadena.length() > 500 ? xmlParaCadena.substring(0, 500) + "..." : xmlParaCadena);
            } catch (Exception e) {
                logger.debug("No se pudo generar log del XML para cadena original: {}", e.getMessage());
            }
            
            String cadenaOriginal;
            try {
                cadenaOriginal = generarCadenaOriginal(doc);
                logger.info("✓ Cadena original generada: {} caracteres", cadenaOriginal.length());
                
                // ============================================================
                // CADENA ORIGINAL PARA COMPARAR CON FINKOK
                // ============================================================
                logger.info("");
                logger.info("═══════════════════════════════════════════════════════════════════════════════");
                logger.info("═══════════════════ CADENA ORIGINAL GENERADA ════════════════════");
                logger.info("═══════════════════════════════════════════════════════════════════════════════");
                logger.info("");
                logger.info("COPIA ESTA CADENA ORIGINAL COMPLETA:");
                logger.info("");
                logger.info("{}", cadenaOriginal);
                logger.info("");
                logger.info("═══════════════════════════════════════════════════════════════════════════════");
                logger.info("═══════════════════════════════════════════════════════════════════════════════");
                logger.info("");
                
                // DEBUG: Imprimir HEX de la cadena para verificar encoding
                StringBuilder hex = new StringBuilder();
                byte[] bytes = cadenaOriginal.getBytes(StandardCharsets.UTF_8);
                logger.info("=== INFORMACIÓN TÉCNICA DE LA CADENA ORIGINAL ===");
                logger.info("Longitud: {} caracteres", cadenaOriginal.length());
                logger.info("Bytes UTF-8: {} bytes", bytes.length);
                logger.info("Primeros 200 bytes en HEX:");
                for (int i = 0; i < Math.min(bytes.length, 200); i++) {
                    hex.append(String.format("%02X ", bytes[i]));
                    if ((i + 1) % 40 == 0) hex.append("\n");
                }
                if (bytes.length > 200) {
                    hex.append("... (truncado, total: ").append(bytes.length).append(" bytes)");
                }
                logger.info("{}", hex.toString());
                logger.info("=== FIN INFORMACIÓN TÉCNICA ===");
                logger.info("");
                logger.info("⚠️ INSTRUCCIONES PARA COMPARAR CON FINKOK:");
                logger.info("   1. Copia la cadena original completa de arriba (entre las líneas de ═══)");
                logger.info("   2. Ve a https://validador.finkok.com");
                logger.info("   3. Pega tu XML firmado en el validador");
                logger.info("   4. Copia la cadena original que genera Finkok");
                logger.info("   5. Compara carácter por carácter ambas cadenas");
                logger.info("   6. Cualquier diferencia (espacios, orden, formato) causará CFDI40102");
                logger.info("");
                
                // Intentar guardar la cadena original en un archivo temporal para facilitar la comparación
                try {
                    guardarCadenaOriginalEnArchivo(cadenaOriginal);
                } catch (Exception e) {
                    logger.debug("No se pudo guardar la cadena original en archivo: {}", e.getMessage());
                }
            } catch (Exception e) {
                logger.error("❌ Error al generar cadena original: {}", e.getMessage(), e);
                throw e;
            }
            
            // 6. Firmar cadena original con SHA256
            logger.info("Firmando cadena original con SHA256...");
            String selloBase64;
            try {
                selloBase64 = firmarCadenaOriginal(cadenaOriginal, llavePrivada);
                logger.info("✓ Sello generado: {} caracteres", selloBase64.length());
                logger.debug("Primeros 50 caracteres del sello: {}", 
                        selloBase64.length() > 50 ? selloBase64.substring(0, 50) + "..." : selloBase64);
            } catch (Exception e) {
                logger.error("❌ Error al firmar cadena original: {}", e.getMessage(), e);
                logger.error("✗✗✗ Esto puede deberse a:");
                logger.error("   1. Contraseña incorrecta de la llave privada (verifica facturacion.csd.llave.password)");
                logger.error("   2. La llave privada no corresponde al certificado");
                logger.error("   3. La llave privada está corrupta o en formato incorrecto");
                throw e;
            }
            
            // 7. Agregar atributos al elemento raíz (Comprobante para CFDI 4.0 o Retenciones para retenciones)
            // CRÍTICO: Agregar los atributos en el orden correcto según el Anexo 20 del SAT
            // El orden debe ser: Certificado, NoCertificado, Sello (aunque NoCertificado ya debería estar)
            // IMPORTANTE: NoCertificado DEBE estar presente ANTES de generar la cadena original
            // y NO debe moverse ni cambiar después de generar la cadena original
            // Usar elementoRaiz que ya fue detectado y verificado anteriormente
            Element elementoParaFirma = elementoRaiz;
            
            // elementoRaiz ya fue verificado anteriormente, así que siempre será != null aquí
            if (elementoParaFirma != null) {
                // CRÍTICO: Asegurar que NoCertificado esté presente antes de agregar Certificado y Sello
                // Esto es importante porque el XSLT requiere NoCertificado para generar la cadena original
                // IMPORTANTE: No mover ni reordenar NoCertificado después de generar la cadena original
                if (!elementoParaFirma.hasAttribute("NoCertificado")) {
                    elementoParaFirma.setAttribute("NoCertificado", noCertificado);
                    logger.warn("⚠️ NoCertificado no estaba presente, agregado antes de Certificado y Sello");
                } else {
                    // Verificar que NoCertificado no haya cambiado
                    String noCertificadoActual = elementoParaFirma.getAttribute("NoCertificado");
                    if (!noCertificado.equals(noCertificadoActual)) {
                        logger.warn("⚠️ NoCertificado cambió de '{}' a '{}'. Corrigiendo...", noCertificadoActual, noCertificado);
                        elementoParaFirma.setAttribute("NoCertificado", noCertificado);
                    }
                }
                
                // CRÍTICO: Agregar Certificado y Sello SIN modificar el orden de los demás atributos
                // El orden de los atributos en el XML final debe ser el mismo que el usado para generar la cadena original
                // excepto por Certificado y Sello que se agregan al final
                // Usar setAttribute que preserva el orden relativo de los atributos existentes
                elementoParaFirma.setAttribute("Certificado", certificadoBase64);
                elementoParaFirma.setAttribute("Sello", selloBase64);
                
                // Verificar que el certificado decodificado tenga el mismo número de serie
                try {
                    byte[] certBytesDecoded = Base64.getDecoder().decode(certificadoBase64);
                    CertificateFactory certFactoryCheck = CertificateFactory.getInstance("X.509");
                    X509Certificate certCheck = (X509Certificate) certFactoryCheck.generateCertificate(
                        new ByteArrayInputStream(certBytesDecoded));
                    String serialNumberFromCert = obtenerNoCertificado(certCheck);
                    logger.info("✓ Verificación: Certificado Base64 decodificado - SerialNumber={}", serialNumberFromCert);
                    if (!serialNumberFromCert.equals(noCertificado)) {
                        logger.error("✗✗✗ ERROR: El NoCertificado ({}) NO coincide con el SerialNumber del Certificado ({})", 
                                noCertificado, serialNumberFromCert);
                        logger.error("✗✗✗ Esto causará error 712 en Finkok");
                        // Corregir el NoCertificado para que coincida
                        noCertificado = serialNumberFromCert;
                        elementoParaFirma.setAttribute("NoCertificado", noCertificado);
                        logger.warn("⚠️ NoCertificado corregido a: {}", noCertificado);
                    } else {
                        logger.info("✓ Verificación: NoCertificado coincide con el SerialNumber del Certificado");
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ No se pudo verificar el certificado decodificado: {}", e.getMessage());
                }
                
                logger.info("✓ Atributos agregados al XML: NoCertificado={}, Certificado={} chars, Sello={} chars", 
                        noCertificado, certificadoBase64.length(), selloBase64.length());
                
                // Verificar que los atributos se agregaron correctamente
                String certificadoVerificar = elementoParaFirma.getAttribute("Certificado");
                String selloVerificar = elementoParaFirma.getAttribute("Sello");
                if (certificadoVerificar.isEmpty() || selloVerificar.isEmpty()) {
                    logger.error("❌ ERROR: Los atributos Certificado o Sello están vacíos después de agregarlos!");
                    throw new IllegalStateException("Los atributos Certificado o Sello están vacíos después de agregarlos");
                }
                logger.info("✓ Verificación: Certificado y Sello presentes en el elemento {}", esRetencion ? "Retenciones" : "Comprobante");
            } else {
                logger.error("❌ No se encontró el elemento {} para agregar atributos", esRetencion ? "Retenciones" : "Comprobante");
                throw new IllegalStateException("No se encontró el elemento " + (esRetencion ? "Retenciones" : "Comprobante") + " en el XML");
            }
            
            // 8. Reordenar atributos del elemento raíz DESPUÉS de agregar Certificado y Sello
            // CRÍTICO: Reordenar nuevamente para asegurar que el orden sea correcto en el XML final
            if (esRetencion && elementoRaiz != null) {
                reordenarAtributosRetencion(elementoRaiz);
                logger.debug("✓ Atributos reordenados después de agregar Certificado y Sello");
            }
            
            // 9. Convertir Document a String
            logger.info("Convirtiendo Document firmado a String...");
            String xmlFirmado;
            try {
                // CRÍTICO: Generar el XML final usando el mismo método que se usó para generar la cadena original
                // Esto asegura que el formato sea idéntico
                xmlFirmado = documentToString(doc);
                logger.info("✓ XML convertido a String: {} caracteres", xmlFirmado.length());
                
                // CRÍTICO: Validar que el XML enviado a Finkok tenga el mismo formato que el usado para generar la cadena original
                // Remover Certificado y Sello del XML firmado para comparar con el XML usado para la cadena original
                // IMPORTANTE: Usar el mismo método de normalización que se usó para generar la cadena original
                String xmlSinFirma = xmlFirmado.replaceAll(" Certificado=\"[^\"]*\"", "");
                xmlSinFirma = xmlSinFirma.replaceAll(" Sello=\"[^\"]*\"", "");
                
                // Normalizar espacios en blanco para comparación (debe ser idéntico al usado para cadena original)
                String xmlNormalizado = xmlSinFirma.replaceAll(">\\s+<", "><").trim();
                
                // Generar el XML sin Certificado y Sello desde el Document para comparar
                // Esto nos permite verificar que el formato sea idéntico
                Document docSinFirma = (Document) doc.cloneNode(true);
                Element comprobanteSinFirma = (Element) docSinFirma.getElementsByTagNameNS("http://www.sat.gob.mx/cfd/4", "Comprobante").item(0);
                if (comprobanteSinFirma == null) {
                    comprobanteSinFirma = (Element) docSinFirma.getElementsByTagName("Comprobante").item(0);
                }
                if (comprobanteSinFirma != null) {
                    if (comprobanteSinFirma.hasAttribute("Certificado")) {
                        comprobanteSinFirma.removeAttribute("Certificado");
                    }
                    if (comprobanteSinFirma.hasAttribute("Sello")) {
                        comprobanteSinFirma.removeAttribute("Sello");
                    }
                    String xmlSinFirmaDesdeDoc = documentToString(docSinFirma);
                    String xmlNormalizadoDesdeDoc = xmlSinFirmaDesdeDoc.replaceAll(">\\s+<", "><").trim();
                    
                    // Comparar ambos XMLs normalizados
                    if (!xmlNormalizado.equals(xmlNormalizadoDesdeDoc)) {
                        logger.warn("⚠️ ADVERTENCIA: El XML sin firma extraído del XML firmado no coincide exactamente con el XML usado para generar la cadena original");
                        logger.warn("Esto puede causar CFDI40102 si hay diferencias en el formato");
                        logger.debug("XML extraído (primeros 200 chars): {}", xmlNormalizado.length() > 200 ? xmlNormalizado.substring(0, 200) + "..." : xmlNormalizado);
                        logger.debug("XML desde Document (primeros 200 chars): {}", xmlNormalizadoDesdeDoc.length() > 200 ? xmlNormalizadoDesdeDoc.substring(0, 200) + "..." : xmlNormalizadoDesdeDoc);
                    } else {
                        logger.debug("✓ Validación: El XML sin firma coincide con el usado para generar la cadena original");
                    }
                }
                
                logger.debug("=== VALIDACIÓN: XML sin firma (primeros 300 chars) ===");
                logger.debug("{}", xmlNormalizado.length() > 300 ? xmlNormalizado.substring(0, 300) + "..." : xmlNormalizado);
                
                // Verificar que el XML resultante contiene los atributos
                if (!xmlFirmado.contains("Certificado=\"") || !xmlFirmado.contains("Sello=\"")) {
                    logger.error("❌ ERROR: El XML resultante NO contiene los atributos Certificado o Sello!");
                    logger.error("XML resultante (primeros 500 chars): {}", 
                            xmlFirmado.length() > 500 ? xmlFirmado.substring(0, 500) + "..." : xmlFirmado);
                    throw new IllegalStateException("El XML resultante no contiene los atributos de firma");
                }
                
                // Verificar que los atributos no estén vacíos
                if (xmlFirmado.contains("Certificado=\"\"") || xmlFirmado.contains("Sello=\"\"")) {
                    logger.error("❌ ERROR: Los atributos Certificado o Sello están vacíos en el XML resultante!");
                    throw new IllegalStateException("Los atributos Certificado o Sello están vacíos");
                }
                
                logger.info("✓ XML firmado exitosamente. Certificado y Sello presentes y no vacíos.");
                
                // ============================================================
                // XML FIRMADO COMPLETO PARA VALIDAR EN FINKOK
                // ============================================================
                logger.info("");
                logger.info("═══════════════════════════════════════════════════════════════════════════════");
                logger.info("═══════════════════ XML FIRMADO COMPLETO ════════════════════");
                logger.info("═══════════════════════════════════════════════════════════════════════════════");
                logger.info("");
                logger.info("COPIA ESTE XML FIRMADO PARA VALIDAR EN FINKOK:");
                logger.info("");
                logger.info("{}", xmlFirmado);
                logger.info("");
                logger.info("═══════════════════════════════════════════════════════════════════════════════");
                logger.info("═══════════════════════════════════════════════════════════════════════════════");
                logger.info("");
                logger.info("⚠️ INSTRUCCIONES:");
                logger.info("   1. Copia el XML firmado completo de arriba (entre las líneas de ═══)");
                logger.info("   2. Ve a https://validador.finkok.com");
                logger.info("   3. Pega el XML firmado en el validador");
                logger.info("   4. El validador mostrará la cadena original que genera Finkok");
                logger.info("   5. Compara esa cadena con la cadena original generada arriba");
                logger.info("");
                
                // Guardar XML firmado en archivo
                try {
                    guardarXmlFirmadoEnArchivo(xmlFirmado);
                } catch (Exception e) {
                    logger.debug("No se pudo guardar el XML firmado en archivo: {}", e.getMessage());
                }
            } catch (Exception e) {
                logger.error("❌ Error al convertir Document a String: {}", e.getMessage(), e);
                throw e;
            }
            
            logger.info("=== FIRMA DIGITAL COMPLETADA EXITOSAMENTE ===");
            return xmlFirmado;
            
        } catch (Exception e) {
            logger.error("❌ ERROR CRÍTICO al firmar XML: {}", e.getMessage(), e);
            logger.error("El XML se enviará sin firma. Esto causará error 302 en Finkok para CFDI 4.0.");
            // En lugar de lanzar excepción, retornar XML sin firmar
            // Esto permite que el proceso continúe y Finkok puede intentar firmar
            return xmlContent;
        }
    }
    
    /**
     * Carga el certificado X509 desde el archivo
     */
    public X509Certificate cargarCertificado() throws Exception {
        logger.info("Cargando certificado desde: {}", certificadoPath);
        
        byte[] certBytes = null;
        if (certificadoPath.startsWith("classpath:")) {
            String resourcePath = certificadoPath.substring("classpath:".length());
            logger.debug("Buscando certificado en classpath: {}", resourcePath);
            
            // Intentar cargar el recurso sin extensión primero
            try (var is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    certBytes = is.readAllBytes();
                    logger.info("Certificado leído exitosamente: {} bytes", certBytes.length);
                }
            }
            
            // Si no se encontró, intentar con extensión .cer
            if (certBytes == null && !resourcePath.endsWith(".cer") && !resourcePath.endsWith(".crt") && !resourcePath.endsWith(".pem")) {
                String resourcePathConExtension = resourcePath + ".cer";
                logger.info("No se encontró sin extensión, intentando con .cer: {}", resourcePathConExtension);
                try (var is2 = getClass().getClassLoader().getResourceAsStream(resourcePathConExtension)) {
                    if (is2 != null) {
                        certBytes = is2.readAllBytes();
                        logger.info("Certificado encontrado con extensión .cer: {} bytes", certBytes.length);
                    }
                }
            }
            
            // Si aún no se encontró, mostrar error con diagnóstico
            if (certBytes == null) {
                listarRecursosDisponibles("certificados");
                String errorMsg = String.format(
                    "No se encontró el certificado en: %s. " +
                    "Verifica que el archivo esté en src/main/resources/certificados/ y que el nombre coincida exactamente. " +
                    "El archivo puede tener extensión .cer o no tener extensión.",
                    certificadoPath);
                logger.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
        } else {
            Path path = Paths.get(certificadoPath);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("No se encontró el certificado en la ruta absoluta: " + certificadoPath);
            }
            certBytes = Files.readAllBytes(path);
            logger.info("Certificado leído desde ruta absoluta: {} bytes", certBytes.length);
        }
        
        // Remover headers PEM si existen
        String certPem = new String(certBytes, StandardCharsets.UTF_8);
        if (certPem.contains("-----BEGIN CERTIFICATE-----")) {
            certPem = certPem.replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");
            certBytes = Base64.getDecoder().decode(certPem);
        }
        
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
        
        logger.info("Certificado cargado: Subject={}, SerialNumber={}", 
                cert.getSubjectX500Principal().getName(), cert.getSerialNumber());
        
        return cert;
    }
    
    /**
     * Carga la llave privada desde el archivo
     * Versión simplificada que usa solo APIs estándar de Java
     * @return PrivateKey si se carga exitosamente, null si no se puede cargar (no lanza excepción)
     */
    public PrivateKey cargarLlavePrivada() {
        logger.info("Cargando llave privada desde: {}", llavePath);
        
        byte[] keyBytes;
        if (llavePath.startsWith("classpath:")) {
            String resourcePath = llavePath.substring("classpath:".length());
            logger.debug("Buscando llave privada en classpath: {}", resourcePath);
            try (var is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    logger.error("No se encontró la llave privada en: {}. Verifica que el archivo esté en src/main/resources/{}", 
                        llavePath, resourcePath);
                    return null;
                }
                keyBytes = is.readAllBytes();
                logger.info("Llave privada leída exitosamente: {} bytes", keyBytes.length);
            } catch (Exception e) {
                logger.error("Error al leer la llave privada desde classpath: {}", e.getMessage());
                return null;
            }
        } else {
            try {
                Path path = Paths.get(llavePath);
                keyBytes = Files.readAllBytes(path);
            } catch (Exception e) {
                logger.error("Error al leer la llave privada desde ruta absoluta: {}", e.getMessage());
                return null;
            }
        }
        
        // Convertir a String para análisis
        String keyText = new String(keyBytes, StandardCharsets.UTF_8);
        String trimmed = keyText.trim();
        
        // 1. Intentar como PEM con headers (formato más común y estándar)
        if (trimmed.contains("-----BEGIN") && trimmed.contains("-----END")) {
            logger.info("Detectado formato PEM con headers");
            
            if (trimmed.contains("-----BEGIN PRIVATE KEY-----")) {
                // PKCS#8 PEM
                logger.info("Procesando como PKCS#8 PEM");
                String base64Content = trimmed
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
                
                try {
                    byte[] decoded = Base64.getDecoder().decode(base64Content);
                    java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(decoded);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    PrivateKey key = keyFactory.generatePrivate(keySpec);
                    logger.info("✓ Llave privada cargada exitosamente como PKCS#8 PEM");
                    return key;
                } catch (Exception e) {
                    logger.warn("Error al cargar como PKCS#8 PEM: {}", e.getMessage());
                }
            }
            
            if (trimmed.contains("-----BEGIN RSA PRIVATE KEY-----")) {
                // PKCS#1 PEM - intentar con BouncyCastle primero
                logger.info("Procesando como PKCS#1 PEM (RSA PRIVATE KEY)");
                try {
                    PrivateKey key = tryLoadWithBouncyCastle(keyBytes, keyText);
                    if (key != null) {
                        logger.info("✓ Llave privada cargada exitosamente como PKCS#1 PEM usando BouncyCastle");
                        return key;
                    }
                } catch (Exception e) {
                    logger.debug("BouncyCastle no disponible o falló para PKCS#1: {}", e.getMessage());
                }
                
                // Si BouncyCastle no está disponible, deshabilitar la firma
                logger.error("PKCS#1 PEM requiere BouncyCastle. Agregue la dependencia o convierta la llave a PKCS#8.");
                throw new IllegalArgumentException(
                    "No se puede cargar llave PKCS#1 sin BouncyCastle. " +
                    "Solución: Agregue BouncyCastle al proyecto o convierta la llave a formato PKCS#8.");
            }
        }
        
        // 2. Intentar como Base64 sin headers (texto plano) - CASO MÁS COMÚN PARA ARCHIVOS .key DEL SAT
        // El archivo puede ser Base64 codificado que al decodificarse es DER binario
        if (trimmed.matches("^[A-Za-z0-9+/=\\s]+$") && trimmed.length() > 100) {
            logger.info("Detectado posible formato Base64 sin headers ({} caracteres). Intentando decodificar...", trimmed.length());
            String base64Content = trimmed.replaceAll("\\s", "");
            
            try {
                byte[] decoded = Base64.getDecoder().decode(base64Content);
                logger.info("✓ Base64 decodificado exitosamente: {} bytes. Primer byte: 0x{:02X}", 
                    decoded.length, decoded.length > 0 ? (decoded[0] & 0xFF) : 0);
                
                // Ahora intentar cargar los bytes decodificados como DER
                // Intentar como PKCS#8 primero (formato estándar)
                try {
                    java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(decoded);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    PrivateKey key = keyFactory.generatePrivate(keySpec);
                    logger.info("✓ Llave privada cargada exitosamente como PKCS#8 desde Base64 decodificado");
                    return key;
                } catch (Exception e) {
                    logger.debug("Los bytes decodificados no son PKCS#8: {}", e.getMessage());
                }
                
                // Si los bytes decodificados empiezan con 0x30, intentar como DER binario con BouncyCastle
                if (decoded.length > 0 && (decoded[0] & 0xFF) == 0x30) {
                    logger.info("Los bytes decodificados empiezan con 0x30 (DER), intentando con BouncyCastle...");
                    
                    // Intentar con BouncyCastle usando los bytes DECODIFICADOS (no los originales)
                    // BouncyCastle intentará PKCS#8 primero y luego PKCS#1
                    try {
                        PrivateKey key = tryLoadWithBouncyCastle(decoded, null);
                        if (key != null) {
                            logger.info("✓ Llave privada cargada exitosamente desde Base64 decodificado usando BouncyCastle");
                            return key;
                        } else {
                            logger.warn("BouncyCastle no pudo cargar la llave desde bytes decodificados. " +
                                "El archivo puede ser un certificado o un formato no estándar. " +
                                "Verifique que el archivo .key sea realmente una llave privada.");
                        }
                    } catch (Exception e) {
                        logger.warn("BouncyCastle no pudo cargar desde bytes decodificados: {}. " +
                            "El archivo puede no ser una llave privada válida.", e.getMessage());
                    }
                } else {
                    logger.debug("Los bytes decodificados no empiezan con 0x30 (primer byte: 0x{:02X}), " +
                        "no es formato DER válido", decoded.length > 0 ? (decoded[0] & 0xFF) : 0);
                }
                
                // También intentar con BouncyCastle usando el texto original (por si acaso)
                try {
                    PrivateKey key = tryLoadWithBouncyCastle(keyBytes, keyText);
                    if (key != null) {
                        logger.info("✓ Llave privada cargada exitosamente desde Base64 usando BouncyCastle");
                        return key;
                    }
                } catch (Exception e) {
                    logger.debug("BouncyCastle no disponible o falló con texto original: {}", e.getMessage());
                }
            } catch (IllegalArgumentException e) {
                logger.debug("No es Base64 válido: {}", e.getMessage());
            }
        }
        
            // 3. Intentar como DER binario directo (sin decodificar Base64 primero)
        if (keyBytes.length > 0 && (keyBytes[0] & 0xFF) == 0x30) {
            logger.info("Detectado posible formato DER binario directo (empieza con 0x30)");
            
            // PRIMERO: Intentar como PKCS#8 SIN encriptar (muchas llaves del SAT vienen sin encriptar)
            try {
                logger.info("Intentando cargar como PKCS#8 DER sin encriptar...");
                java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PrivateKey key = keyFactory.generatePrivate(keySpec);
                logger.info("✓✓✓ Llave privada cargada exitosamente como PKCS#8 DER SIN encriptar");
                return key;
            } catch (Exception e) {
                logger.debug("No es PKCS#8 DER sin encriptar: {}", e.getMessage());
            }
            
            // SEGUNDO: Intentar con BouncyCastle como DER sin encriptar (puede manejar PKCS#1)
            try {
                logger.info("Intentando cargar con BouncyCastle como DER sin encriptar...");
                PrivateKey key = tryLoadWithBouncyCastle(keyBytes, null);
                if (key != null) {
                    logger.info("✓✓✓ Llave privada cargada con BouncyCastle desde DER binario SIN encriptar");
                    return key;
                }
            } catch (Exception e) {
                logger.debug("BouncyCastle no pudo cargar desde DER binario sin encriptar: {}", e.getMessage());
            }
            
            // TERCERO: Si hay contraseña, intentar descifrar como DER ENCRIPTADO con BouncyCastle
            if (llavePassword != null && !llavePassword.isEmpty()) {
                logger.info("Intentando descifrar llave DER ENCRIPTADA con contraseña usando BouncyCastle...");
                try {
                    PrivateKey key = tryLoadWithBouncyCastle(keyBytes, null);
                    if (key != null) {
                        logger.info("✓✓✓ Llave privada DER encriptada descifrada exitosamente con BouncyCastle");
                        return key;
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ No se pudo descifrar como DER encriptado: {}", e.getMessage());
                    if (e.getMessage() != null && (e.getMessage().contains("password") || 
                        e.getMessage().contains("wrong") || e.getMessage().contains("incorrect"))) {
                        logger.error("✗✗✗ ERROR: La contraseña puede ser INCORRECTA. Verifica facturacion.csd.llave.password en application.yml");
                        logger.error("✗✗✗ Contraseña actual configurada: {} caracteres", llavePassword.length());
                    }
                }
            } else {
                logger.warn("⚠️ Llave DER detectada pero NO se puede cargar sin encriptar y NO hay contraseña configurada.");
                logger.warn("⚠️ La llave puede estar encriptada. Agrega facturacion.csd.llave.password en application.yml");
            }
        }
        
        // 4. Intentar como PKCS#12 (.p12 o .pfx)
        if (llavePath.endsWith(".p12") || llavePath.endsWith(".pfx")) {
            logger.info("Procesando como PKCS#12");
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                char[] password = llavePassword != null && !llavePassword.isEmpty() 
                    ? llavePassword.toCharArray() 
                    : new char[0];
                keyStore.load(new ByteArrayInputStream(keyBytes), password);
                
                Enumeration<String> aliases = keyStore.aliases();
                if (!aliases.hasMoreElements()) {
                    logger.error("No se encontró ningún alias en el keystore PKCS#12");
                    return null;
                }
                
                String alias = aliases.nextElement();
                PrivateKey key = (PrivateKey) keyStore.getKey(alias, password);
                logger.info("✓ Llave privada cargada exitosamente desde PKCS#12");
                return key;
            } catch (Exception e) {
                logger.error("Error al cargar llave privada desde PKCS#12: {}", e.getMessage());
                return null;
            }
        }
        
        // Si todo falló, loguear el error pero NO lanzar excepción
        // El método firmarXml manejará el null y continuará sin firma
        String primerByte = keyBytes.length > 0 ? String.format("0x%02X", keyBytes[0] & 0xFF) : "N/A";
        String errorMsg = String.format(
            "No se pudo cargar la llave privada desde: %s. " +
            "Tamaño del archivo: %d bytes. Primer byte: %s. " +
            "Formatos intentados: PEM con headers, Base64 sin headers, DER binario (PKCS#8 y PKCS#1), PKCS#12. " +
            "SOLUCIÓN: " +
            "1) Verifique que el archivo .key sea realmente una llave privada (no un certificado). " +
            "2) Convierta la llave a PKCS#8 PEM usando: openssl pkcs8 -topk8 -inform DER -in archivo.key -out archivo_pkcs8.pem -nocrypt " +
            "3) O deshabilite la firma local (facturacion.csd.enabled=false).",
            llavePath, keyBytes.length, primerByte);
        
        logger.error(errorMsg);
        logger.warn("La firma digital se deshabilitará automáticamente. El XML se enviará sin firma.");
        return null; // Retornar null en lugar de lanzar excepción
    }

    /**
     * Garantiza que el atributo Fecha del Comprobante esté en hora local de México y dentro
     * de la ventana permitida por el SAT/PAC (±5 minutos).
     */
    private void normalizarFechaComprobante(Element comprobante) {
        if (comprobante == null) {
            return;
        }

        String fechaAttr = comprobante.getAttribute("Fecha");
        ZonedDateTime ahoraMexico = ZonedDateTime.now(ZONA_HORARIA_MX);

        if (!StringUtils.hasText(fechaAttr)) {
            String nuevaFecha = CFDI_FECHA_FORMATTER.format(ahoraMexico);
            comprobante.setAttribute("Fecha", nuevaFecha);
            logger.warn("⚠️ El Comprobante no tenía atributo Fecha. Se asignó la hora actual de México: {}", nuevaFecha);
            return;
        }

        ZonedDateTime fechaXml = parseFechaCfdi(fechaAttr);
        if (fechaXml == null) {
            String nuevaFecha = CFDI_FECHA_FORMATTER.format(ahoraMexico);
            comprobante.setAttribute("Fecha", nuevaFecha);
            logger.warn("⚠️ No se pudo interpretar la Fecha '{}' del Comprobante. Se asignó la hora actual de México: {}", fechaAttr, nuevaFecha);
            return;
        }

        ZonedDateTime fechaXmlEnMexico = fechaXml.withZoneSameInstant(ZONA_HORARIA_MX);
        Duration diferencia = Duration.between(fechaXmlEnMexico, ahoraMexico).abs();

        if (diferencia.compareTo(MAX_DESVIACION_FECHA) > 0) {
            String nuevaFecha = CFDI_FECHA_FORMATTER.format(ahoraMexico);
            comprobante.setAttribute("Fecha", nuevaFecha);
            logger.warn("⚠️ Fecha del Comprobante fuera de rango ({} minutos de diferencia). Corrigiendo a hora actual de México: {}",
                    diferencia.toMinutes(), nuevaFecha);
        } else {
            String fechaNormalizada = CFDI_FECHA_FORMATTER.format(fechaXmlEnMexico);
            if (!fechaAttr.equals(fechaNormalizada)) {
                comprobante.setAttribute("Fecha", fechaNormalizada);
                logger.info("✓ Fecha del Comprobante normalizada a zona México sin offset: {} -> {}", fechaAttr, fechaNormalizada);
            } else {
                logger.info("✓ Fecha del Comprobante dentro del rango permitido ({})", fechaNormalizada);
            }
        }
    }

    /**
     * Ajusta los atributos obligatorios para CFDI de tipo "P" con moneda "XXX".
     * Evita errores CFDI40103 (FormaPago) y CFDI40107 (decimales no permitidos).
     */
    private void normalizarAtributosComplementoPago(Document doc, Element comprobante) {
        if (doc == null || comprobante == null) {
            return;
        }

        String tipo = comprobante.getAttribute("TipoDeComprobante");
        if (tipo == null || !tipo.equalsIgnoreCase("P")) {
            return;
        }

        logger.info("Aplicando reglas SAT para complemento de pagos (TipoDeComprobante=P).");

        String moneda = comprobante.getAttribute("Moneda");
        if (!"XXX".equalsIgnoreCase(moneda)) {
            logger.warn("Moneda para complemento de pagos debe ser 'XXX'. Valor actual: '{}'. Corrigiendo automáticamente.", moneda);
            comprobante.setAttribute("Moneda", "XXX");
        }

        asegurarAtributoCero(comprobante, "SubTotal", "SubTotal del Comprobante (Tipo P)");
        asegurarAtributoCero(comprobante, "Total", "Total del Comprobante (Tipo P)");

        if (comprobante.hasAttribute("TipoCambio")) {
            logger.warn("TipoCambio no aplica cuando Moneda=XXX. Eliminando atributo.");
            comprobante.removeAttribute("TipoCambio");
        }

        // IMPORTANTE: Según CFDI40103, cuando TipoDeComprobante="P" (complemento de pago), 
        // FormaPago NO debe existir en el Comprobante principal
        // La FormaPago debe estar en el elemento pago20:Pago dentro del complemento
        if (comprobante.hasAttribute("FormaPago")) {
            logger.warn("⚠️ FormaPago no debe existir cuando TipoDeComprobante es P. Eliminando para evitar CFDI40103.");
            comprobante.removeAttribute("FormaPago");
        }
        
        // IMPORTANTE: Según CFDI40125, cuando TipoDeComprobante="P" (complemento de pago),
        // MetodoPago NO debe existir en el Comprobante principal
        if (comprobante.hasAttribute("MetodoPago")) {
            logger.warn("⚠️ MetodoPago no debe existir cuando TipoDeComprobante es P. Eliminando para evitar CFDI40125.");
            comprobante.removeAttribute("MetodoPago");
        }
        
        logger.info("✓ FormaPago y MetodoPago eliminados del Comprobante principal (requerido para TipoDeComprobante=P). FormaPago debe estar en pago20:Pago.");

        org.w3c.dom.NodeList conceptosList = doc.getElementsByTagNameNS("http://www.sat.gob.mx/cfd/4", "Concepto");
        if (conceptosList.getLength() == 0) {
            conceptosList = doc.getElementsByTagName("Concepto");
        }

        for (int i = 0; i < conceptosList.getLength(); i++) {
            org.w3c.dom.Node node = conceptosList.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element concepto = (Element) node;
            asegurarAtributoCero(concepto, "ValorUnitario", "ValorUnitario del Concepto (Tipo P)");
            asegurarAtributoCero(concepto, "Importe", "Importe del Concepto (Tipo P)");

            String objetoImp = concepto.getAttribute("ObjetoImp");
            if (!"01".equals(objetoImp)) {
                logger.warn("ObjetoImp en conceptos de complemento de pagos debe ser '01'. Valor actual: '{}'. Corrigiendo automáticamente.", objetoImp);
                concepto.setAttribute("ObjetoImp", "01");
            }

            if (concepto.hasAttribute("Descuento")) {
                logger.warn("Descuento no debe existir en conceptos de complemento de pagos. Eliminando atributo.");
                concepto.removeAttribute("Descuento");
            }
        }
    }

    private void asegurarAtributoCero(Element element, String attributeName, String contexto) {
        String valor = element.getAttribute(attributeName);
        if ("0".equals(valor)) {
            return;
        }

        if (StringUtils.hasText(valor)) {
            logger.warn("{} tenía valor '{}'. Corrigiendo a '0'.", contexto, valor);
        } else {
            logger.warn("{} no estaba presente. Asignando '0'.", contexto);
        }
        element.setAttribute(attributeName, "0");
    }

    private ZonedDateTime parseFechaCfdi(String fechaAttr) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(fechaAttr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return localDateTime.atZone(ZONA_HORARIA_MX);
        } catch (DateTimeParseException e) {
            try {
                return ZonedDateTime.parse(fechaAttr, DateTimeFormatter.ISO_DATE_TIME)
                        .withZoneSameInstant(ZONA_HORARIA_MX);
            } catch (DateTimeParseException e2) {
                try {
                    return ZonedDateTime.parse(fechaAttr).withZoneSameInstant(ZONA_HORARIA_MX);
                } catch (DateTimeParseException e3) {
                    logger.warn("⚠️ No se pudo parsear la Fecha '{}' del Comprobante: {}", fechaAttr, e3.getMessage());
                    return null;
                }
            }
        }
    }
    
    /**
     * Obtiene el número de serie del certificado (NoCertificado)
     * Para los CSD del SAT el serial va codificado como caracteres ASCII numéricos (ej. "3000...").
     * Cuando Java lo interpreta como BigInteger y se convierte directamente a decimal se obtiene un
     * valor enorme (2922...) que no corresponde. Por eso detectamos explícitamente la forma ASCII.
     */
    private String obtenerNoCertificado(X509Certificate cert) {
        java.math.BigInteger serialNumber = cert.getSerialNumber();
        byte[] serialBytes = serialNumber.toByteArray();
        
        // Eliminar byte de signo si existe
        if (serialBytes.length > 1 && serialBytes[0] == 0x00) {
            serialBytes = Arrays.copyOfRange(serialBytes, 1, serialBytes.length);
        }
        
        boolean asciiDigits = serialBytes.length > 0;
        StringBuilder asciiBuilder = new StringBuilder();
        for (byte b : serialBytes) {
            if (b >= 0x30 && b <= 0x39) { // '0'..'9'
                asciiBuilder.append((char) b);
            } else {
                asciiDigits = false;
                break;
            }
        }
        
        if (asciiDigits && asciiBuilder.length() > 0) {
            String asciiSerial = asciiBuilder.toString();
            logger.info("Serial del certificado detectado como ASCII ({} dígitos): {}", asciiSerial.length(), asciiSerial);
            return asciiSerial;
        }
        
        // Si no es ASCII puro, usar representación hexadecimal en mayúsculas como último recurso
        String hexSerial = serialNumber.toString(16).toUpperCase();
        logger.warn("Serial del certificado no está en ASCII numérico, usando HEX: {}", hexSerial);
        return hexSerial;
    }
    
    /**
     * Extrae el RFC del SubjectDN del certificado
     * El RFC puede estar en diferentes campos: 2.5.4.45, CN, o en el formato estándar del SAT
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
    
    /**
     * Obtiene el certificado en Base64 sin headers PEM
     */
    private String obtenerCertificadoBase64(X509Certificate cert) throws Exception {
        byte[] certBytes = cert.getEncoded();
        return Base64.getEncoder().encodeToString(certBytes);
    }
    
    /**
     * Genera la cadena original del CFDI 4.0 según algoritmo del SAT
     * Para CFDI 4.0, la cadena original se genera normalizando el XML
     * IMPORTANTE: La cadena original debe generarse ANTES de agregar los atributos
     * Certificado, NoCertificado y Sello al XML
     * 
     * NOTA CRÍTICA: El algoritmo exacto requiere el XSLT oficial del SAT (cadenaoriginal_4_0.xslt)
     * Esta implementación genera una cadena normalizada, pero puede no coincidir exactamente
     * con el XSLT del SAT, causando el error CFDI40102.
     * 
     * Para obtener el XSLT oficial:
     * 1. Visita https://www.sat.gob.mx/cfd/4
     * 2. Descarga el archivo cadenaoriginal_4_0.xslt
     * 3. Colócalo en src/main/resources/xslt/cadenaoriginal_4_0.xslt
     * 4. El código intentará usarlo automáticamente
     */
    private String generarCadenaOriginal(Document doc) throws Exception {
        // Detectar si es retención o CFDI 4.0
        boolean esRetencion = doc.getElementsByTagNameNS("http://www.sat.gob.mx/esquemas/retencionpago/2", "Retenciones").getLength() > 0 ||
                             doc.getElementsByTagName("Retenciones").getLength() > 0;
        
        // Intentar usar el XSLT oficial del SAT si está disponible
        // Buscar en múltiples ubicaciones posibles
        // PRIORIDAD: librerias/ (archivos oficiales de Finkok) > xslt/ > certificados/
        String[] xsltPaths;
        if (esRetencion) {
            // Para retenciones, buscar XSLT específico del SAT para retenciones
            // El SAT tiene un XSLT específico diferente al de CFDI 4.0
            xsltPaths = new String[]{
                "librerias/cadenaoriginal_retenciones.xslt",  // XSLT específico para retenciones
                "librerias/RETEN/cadenaoriginal_retenciones.xslt",  // En carpeta RETEN
                "librerias/cadenaoriginal_retencionpago.xslt",  // Variante del nombre
                "xslt/cadenaoriginal_retenciones.xslt",
                "certificados/cadenaoriginal_retenciones.xslt",
                "librerias/cadenaoriginal.xslt",  // Fallback genérico (puede no funcionar)
                "librerias/cadenaoriginal_4_0.xslt"  // Último fallback (definitivamente no funcionará pero lo intentamos)
            };
            logger.info("Retención detectada - Buscando XSLT específico para generar cadena original de retención");
        } else {
            xsltPaths = new String[]{
                "librerias/cadenaoriginal_4_0.xslt",  // PRIMERO: Archivos oficiales de Finkok
                "xslt/cadenaoriginal_4_0.xslt",
                "certificados/cadenaoriginal_4_0.xslt"
            };
        }
        
        for (String xsltPath : xsltPaths) {
            try {
                java.io.InputStream xsltStream = getClass().getClassLoader()
                    .getResourceAsStream(xsltPath);
                if (xsltStream != null) {
                    logger.info("✓ XSLT oficial del SAT encontrado en: {}. Usando para generar cadena original.", xsltPath);
                    try {
                        String resultado = generarCadenaOriginalConXslt(doc, xsltStream, xsltPath);
                        // Verificar que el resultado no sea XML (debe ser cadena con pipes)
                        if (resultado != null && !resultado.trim().startsWith("<")) {
                            logger.info("✓ Cadena original generada correctamente con XSLT (formato con pipes)");
                            return resultado;
                        } else {
                            logger.warn("⚠️ El XSLT generó XML en lugar de cadena con pipes. Esto indica un problema.");
                            throw new Exception("El XSLT no generó el formato correcto de cadena original");
                        }
                    } catch (Exception e) {
                        logger.error("❌ Error al procesar XSLT desde {}: {}", xsltPath, e.getMessage(), e);
                        logger.warn("⚠️ Usando normalización manual como fallback.");
                        // Continuar al siguiente intento o usar normalización manual
                        break;
                    }
                } else {
                    logger.debug("XSLT no encontrado en: {}", xsltPath);
                }
            } catch (Exception e) {
                logger.debug("No se pudo cargar XSLT desde {}: {}", xsltPath, e.getMessage());
            }
        }
        
        // Si no hay XSLT, lanzar error específico según el tipo
        if (esRetencion) {
            logger.error("❌ ERROR CRÍTICO: No se encontró el XSLT oficial del SAT para RETENCIONES.");
            logger.error("❌ Para retenciones, el SAT requiere un XSLT específico diferente al de CFDI 4.0.");
            logger.error("❌ El XSLT debe procesar el elemento 'retenciones:Retenciones' con namespace 'http://www.sat.gob.mx/esquemas/retencionpago/2'.");
            logger.error("❌ SOLUCIÓN:");
            logger.error("   1. Descarga el XSLT oficial de retenciones del SAT desde:");
            logger.error("      https://www.sat.gob.mx/esquemas/retencionpago/2");
            logger.error("   2. Guárdalo como: src/main/resources/librerias/cadenaoriginal_retenciones.xslt");
            logger.error("   3. El XSLT debe incluir el complemento operacionesconderivados.xslt si lo usas");
            logger.error("   4. Reinicia la aplicación");
            throw new Exception("No se pudo generar la Cadena Original de RETENCIÓN. " +
                "Falta el XSLT oficial del SAT para retenciones (cadenaoriginal_retenciones.xslt). " +
                "Descárgalo desde https://www.sat.gob.mx/esquemas/retencionpago/2");
        } else {
            logger.warn("⚠️ ADVERTENCIA: No se encontró el XSLT oficial del SAT o falló su procesamiento.");
            logger.warn("⚠️ La generación manual está deshabilitada porque no cumple con el estándar CFDI 4.0.");
            throw new Exception("No se pudo generar la Cadena Original. Verifique que cadenaoriginal_4_0.xslt existe y es válido.");
        }
    }
    
    /**
     * Genera la cadena original usando el XSLT oficial del SAT
     * @param xsltPath La ruta donde se encontró el XSLT (para configurar correctamente el systemId)
     */
    private String generarCadenaOriginalConXslt(Document doc, java.io.InputStream xsltStream, String xsltPath) throws Exception {
        try {
            // Crear una nueva instancia de TransformerFactory para evitar caché
            // Esto asegura que los cambios en los XSLT se reflejen inmediatamente
            TransformerFactory factory = TransformerFactory.newInstance();
            try {
                factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, false);
            } catch (Exception e) {
                logger.debug("No se pudo configurar FEATURE_SECURE_PROCESSING: {}", e.getMessage());
            }
            
            // Deshabilitar caché de atributos si es posible
            try {
                factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            } catch (Exception e) {
                logger.debug("No se pudo configurar atributos de caché: {}", e.getMessage());
            }
            
            // Configurar URIResolver para buscar archivos complementarios en múltiples ubicaciones
            factory.setURIResolver(new javax.xml.transform.URIResolver() {
                @Override
                public javax.xml.transform.Source resolve(String href, String base) throws javax.xml.transform.TransformerException {
                    logger.debug("Resolviendo archivo complementario: href={}, base={}", href, base);
                    
                    // Determinar la carpeta base desde el systemId (base)
                    String baseFolder = "librerias"; // Por defecto
                    if (base != null) {
                        if (base.contains("librerias/")) {
                            baseFolder = "librerias";
                        } else if (base.contains("certificados/")) {
                            baseFolder = "certificados";
                        } else if (base.contains("xslt/")) {
                            baseFolder = "xslt";
                        }
                    }
                    
                    // Limpiar la ruta: eliminar ./ y normalizar
                    String cleanHref = href;
                    if (cleanHref.startsWith("./")) {
                        cleanHref = cleanHref.substring(2);
                    }
                    if (cleanHref.startsWith("/")) {
                        cleanHref = cleanHref.substring(1);
                    }
                    
                    // Si href es una URI resource:, extraer la ruta
                    if (cleanHref.startsWith("resource:/")) {
                        cleanHref = cleanHref.substring(10);
                    }
                    
                    // Extraer solo el nombre del archivo si es una ruta completa
                    String fileName = cleanHref;
                    if (cleanHref.contains("/")) {
                        fileName = cleanHref.substring(cleanHref.lastIndexOf("/") + 1);
                    }
                    
                    // Buscar en múltiples ubicaciones con diferentes variantes
                    // CRÍTICO: Priorizar la carpeta base (donde está el XSLT principal) sobre otras ubicaciones
                    // IMPORTANTE: Para retenciones, también buscar en carpeta RETEN y "retenciones-complementos"
                    String[] searchPaths = {
                        // PRIMERO: Buscar en la carpeta base (donde está el XSLT principal)
                        baseFolder + "/complements/" + fileName,
                        baseFolder + "/" + cleanHref,
                        // SEGUNDO: Para retenciones, buscar en carpeta "retenciones-complementos" (donde están los XSLT de complementos)
                        "librerias/retenciones-complementos/" + fileName,
                        "librerias/retenciones-complementos/" + cleanHref,
                        // TERCERO: Para retenciones, buscar en carpeta RETEN
                        "librerias/RETEN/" + fileName,
                        "librerias/RETEN/" + cleanHref,
                        baseFolder + "/RETEN/" + fileName,
                        baseFolder + "/RETEN/" + cleanHref,
                        // CUARTO: Buscar en librerias/ (archivos oficiales de Finkok descargados)
                        "librerias/complements/" + fileName,
                        "librerias/" + cleanHref,
                        // QUINTO: Buscar en certificados/ (fallback)
                        "certificados/complements/" + fileName,
                        "certificados/" + cleanHref,
                        // SEXTO: Buscar en xslt/complements/ (solo como fallback)
                        "xslt/complements/" + fileName,
                        "xslt/" + cleanHref,
                        // SÉPTIMO: Otras ubicaciones
                        cleanHref,
                        "complements/" + fileName,
                        // Si cleanHref ya incluye complements/, buscar directamente
                        cleanHref.startsWith("complements/") ? baseFolder + "/" + cleanHref : null,
                        cleanHref.startsWith("complements/") ? "librerias/" + cleanHref : null,
                        cleanHref.startsWith("complements/") ? "certificados/" + cleanHref : null,
                        cleanHref.startsWith("complements/") ? "xslt/" + cleanHref : null
                    };
                    
                    for (String path : searchPaths) {
                        if (path == null) continue;
                        java.io.InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
                        if (stream != null) {
                            logger.info("✓ Archivo complementario encontrado: {} en {}", href, path);
                            javax.xml.transform.stream.StreamSource source = new javax.xml.transform.stream.StreamSource(stream);
                            // Establecer systemId como URI relativa para que el URIResolver pueda resolver referencias anidadas
                            // Usar una URI que el procesador pueda manejar correctamente
                            source.setSystemId("resource:/" + path);
                            return source;
                        }
                    }
                    
                    // Si es utilerias.xslt, es crítico y debe existir
                    if (fileName.equals("utilerias.xslt")) {
                        logger.error("❌ CRÍTICO: No se encontró utilerias.xslt. Este archivo es requerido.");
                        logger.error("Buscado en las siguientes rutas:");
                        for (String path : searchPaths) {
                            if (path != null) {
                                logger.error("  - {}", path);
                            }
                        }
                        throw new javax.xml.transform.TransformerException("Archivo requerido no encontrado: utilerias.xslt");
                    }
                    
                    // Para otros complementos, crear un XSLT stub vacío para evitar errores
                    // Estos complementos solo se usan si el CFDI los incluye, así que un stub vacío es seguro
                    logger.debug("⚠️ Archivo complementario no encontrado (creando stub vacío): {} (buscado como: {})", href, cleanHref);
                    
                    // Crear un XSLT stub vacío para complementos opcionales
                    String stubXslt = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                     "<xsl:stylesheet version=\"1.1\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                                     "  <!-- Stub vacío para complemento opcional: " + fileName + " -->\n" +
                                     "</xsl:stylesheet>";
                    
                    java.io.ByteArrayInputStream stubStream = new java.io.ByteArrayInputStream(
                        stubXslt.getBytes(StandardCharsets.UTF_8));
                    javax.xml.transform.stream.StreamSource stubSource = new javax.xml.transform.stream.StreamSource(stubStream);
                    // Usar URI relativa para que el URIResolver pueda resolver referencias anidadas
                    stubSource.setSystemId("resource:/xslt/complements/" + fileName);
                    return stubSource;
                }
            });
            
            // Establecer systemId en el StreamSource para que las rutas relativas se resuelvan correctamente
            // Usar URI relativa para que el URIResolver pueda resolver todas las referencias
            // El systemId debe apuntar a la ubicación real del XSLT para que las rutas relativas (./complements/) se resuelvan correctamente
            javax.xml.transform.stream.StreamSource xsltSource = new javax.xml.transform.stream.StreamSource(xsltStream);
            // Usar la ruta real del XSLT como systemId para que las rutas relativas funcionen
            String systemId = "resource:/" + xsltPath;
            xsltSource.setSystemId(systemId);
            logger.debug("SystemId configurado para XSLT: {}", systemId);
            
            Transformer transformer = factory.newTransformer(xsltSource);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(baos));
            
            String cadenaOriginal = baos.toString(StandardCharsets.UTF_8).trim();
            
            // Validar que la cadena original tenga el formato correcto (debe empezar y terminar con |)
            if (!cadenaOriginal.startsWith("|") || !cadenaOriginal.endsWith("||")) {
                logger.warn("⚠️ ADVERTENCIA: La cadena original no tiene el formato esperado (debe empezar con | y terminar con ||)");
                logger.warn("Cadena generada: {}", cadenaOriginal.length() > 100 ? cadenaOriginal.substring(0, 100) + "..." : cadenaOriginal);
            }
            
            // Extraer NoCertificado del documento para validación
            Element comprobanteCheck = (Element) doc.getElementsByTagNameNS("http://www.sat.gob.mx/cfd/4", "Comprobante").item(0);
            if (comprobanteCheck == null) {
                comprobanteCheck = (Element) doc.getElementsByTagName("Comprobante").item(0);
            }
            if (comprobanteCheck != null && comprobanteCheck.hasAttribute("NoCertificado")) {
                String noCertificadoEnDoc = comprobanteCheck.getAttribute("NoCertificado");
                // Verificar que NoCertificado esté presente en la cadena
                if (!cadenaOriginal.contains(noCertificadoEnDoc)) {
                    logger.error("❌ ERROR CRÍTICO: NoCertificado ({}) NO está presente en la cadena original generada!", noCertificadoEnDoc);
                    logger.error("Esto causará CFDI40102 porque Finkok espera que NoCertificado esté en la cadena original.");
                    logger.error("Cadena original (primeros 300 chars): {}", 
                            cadenaOriginal.length() > 300 ? cadenaOriginal.substring(0, 300) + "..." : cadenaOriginal);
                } else {
                    logger.debug("✓ NoCertificado ({}) verificado en la cadena original", noCertificadoEnDoc);
                }
            }
            
            logger.info("✓ Cadena original generada con XSLT oficial: {} caracteres", cadenaOriginal.length());
            logger.debug("Primeros 200 caracteres de cadena original: {}", 
                    cadenaOriginal.length() > 200 ? cadenaOriginal.substring(0, 200) + "..." : cadenaOriginal);
            return cadenaOriginal;
        } catch (Exception e) {
            logger.error("❌ Error al procesar XSLT oficial: {}", e.getMessage(), e);
            logger.warn("⚠️ El XSLT falló. Esto puede deberse a:");
            logger.warn("   - Archivos complementarios faltantes (si tu CFDI no usa complementos, esto es normal)");
            logger.warn("   - Error en el procesamiento del XSLT");
            logger.warn("⚠️ Se abortará el proceso porque no se puede generar una cadena original válida sin XSLT.");
            throw e; // Re-lanzar para que el método padre use el fallback
        }
    }
    
    /**
     * Firma la cadena original con SHA256 y RSA
     */
    private String firmarCadenaOriginal(String cadenaOriginal, PrivateKey llavePrivada) throws Exception {
        Signature signature;
        try {
            // Intentar usar BouncyCastle explícitamente para asegurar compatibilidad
            signature = Signature.getInstance("SHA256withRSA", "BC");
            logger.info("✓ Firmando con SHA256withRSA usando proveedor: {} versión {}", 
                signature.getProvider().getName(), signature.getProvider().getVersionStr());
        } catch (Exception e) {
            logger.warn("⚠️ No se pudo instanciar SHA256withRSA con BC, usando proveedor por defecto: {}", e.getMessage());
            signature = Signature.getInstance("SHA256withRSA");
            logger.info("✓ Firmando con SHA256withRSA usando proveedor por defecto: {}", signature.getProvider().getName());
        }
        
        signature.initSign(llavePrivada);
        signature.update(cadenaOriginal.getBytes(StandardCharsets.UTF_8));
        
        byte[] firmaBytes = signature.sign();
        return Base64.getEncoder().encodeToString(firmaBytes);
    }
    
    /**
     * Convierte un Document a String
     * CRÍTICO: El formato debe ser consistente para que Finkok genere la misma cadena original
     * IMPORTANTE: El XML resultante debe tener exactamente el mismo formato que el usado para generar la cadena original
     * 
     * NOTA: Este método debe generar el XML con el mismo formato que se usa para generar la cadena original.
     * Cualquier diferencia (espacios, orden de atributos, etc.) causará CFDI40102.
     */
    private String documentToString(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        
        // CRÍTICO: Deshabilitar cualquier optimización que pueda cambiar el formato
        try {
            transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, false);
        } catch (Exception e) {
            logger.debug("No se pudo configurar FEATURE_SECURE_PROCESSING: {}", e.getMessage());
        }
        
        Transformer transformer = transformerFactory.newTransformer();
        
        // CRÍTICO: Configurar propiedades para generar XML sin espacios adicionales
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "no"); // CRÍTICO: Sin indentación
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        
        // CRÍTICO: No agregar espacios adicionales entre elementos
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        
        String xmlString = baos.toString(StandardCharsets.UTF_8);
        
        // CRÍTICO: Normalizar espacios en blanco entre tags para consistencia
        // Esto asegura que el XML tenga el mismo formato que el usado para generar la cadena original
        // Solo normalizar espacios ENTRE tags, no dentro de atributos o contenido
        // IMPORTANTE: Esta normalización debe aplicarse TANTO al XML usado para generar la cadena original
        // como al XML final que se envía a Finkok
        xmlString = xmlString.replaceAll(">\\s+<", "><");
        
        // CRÍTICO: Asegurar que no haya espacios al inicio o final del XML (excepto en el contenido de los elementos)
        xmlString = xmlString.trim();
        
        return xmlString;
    }
    
    /**
     * Reordena los atributos del elemento raíz de retención según el orden esperado por el XSLT
     * Orden esperado por el XSLT: Version, NoCertificado, FolioInt, FechaExp, LugarExpRetenc, CveRetenc, DescRetenc
     * Luego: namespaces, Certificado, Sello, xsi:schemaLocation
     * 
     * @param elementoRaiz El elemento Retenciones
     */
    private void reordenarAtributosRetencion(Element elementoRaiz) {
        try {
            // Obtener todos los atributos y sus valores
            java.util.Map<String, String> atributos = new java.util.LinkedHashMap<>();
            org.w3c.dom.NamedNodeMap attrs = elementoRaiz.getAttributes();
            
            // Guardar todos los atributos
            for (int i = 0; i < attrs.getLength(); i++) {
                org.w3c.dom.Node attr = attrs.item(i);
                atributos.put(attr.getNodeName(), attr.getNodeValue());
            }
            
            // Remover todos los atributos
            while (attrs.getLength() > 0) {
                elementoRaiz.removeAttribute(attrs.item(0).getNodeName());
            }
            
            // Orden esperado por el XSLT para retenciones:
            // 1. Version
            // 2. NoCertificado
            // 3. FolioInt
            // 4. FechaExp
            // 5. LugarExpRetenc
            // 6. CveRetenc
            // 7. DescRetenc
            // Luego: namespaces, Certificado, Sello, xsi:schemaLocation
            
            // Agregar atributos en el orden correcto
            String[] ordenAtributos = {
                "Version",
                "NoCertificado",
                "FolioInt",
                "FechaExp",
                "LugarExpRetenc",
                "CveRetenc",
                "DescRetenc"
            };
            
            // Agregar atributos en el orden esperado por el XSLT
            for (String nombreAttr : ordenAtributos) {
                if (atributos.containsKey(nombreAttr)) {
                    elementoRaiz.setAttribute(nombreAttr, atributos.get(nombreAttr));
                }
            }
            
            // Agregar namespaces en el orden correcto (importante para la cadena original)
            // Orden: xmlns:retenciones, xmlns:xsi, luego otros namespaces de complementos
            String[] ordenNamespaces = {
                "xmlns:retenciones",
                "xmlns:xsi"
            };
            
            // Agregar namespaces principales primero
            for (String nombreNs : ordenNamespaces) {
                if (atributos.containsKey(nombreNs)) {
                    elementoRaiz.setAttribute(nombreNs, atributos.get(nombreNs));
                }
            }
            
            // Agregar otros namespaces de complementos (xmlns:sectorfinanciero, etc.)
            for (java.util.Map.Entry<String, String> entry : atributos.entrySet()) {
                String nombre = entry.getKey();
                if ((nombre.startsWith("xmlns:") || nombre.equals("xmlns")) && 
                    !nombre.equals("xmlns:retenciones") && !nombre.equals("xmlns:xsi")) {
                    elementoRaiz.setAttribute(nombre, entry.getValue());
                }
            }
            
            // Agregar Certificado y Sello (si existen)
            if (atributos.containsKey("Certificado")) {
                elementoRaiz.setAttribute("Certificado", atributos.get("Certificado"));
            }
            if (atributos.containsKey("Sello")) {
                elementoRaiz.setAttribute("Sello", atributos.get("Sello"));
            }
            
            // Agregar xsi:schemaLocation al final
            if (atributos.containsKey("xsi:schemaLocation")) {
                elementoRaiz.setAttribute("xsi:schemaLocation", atributos.get("xsi:schemaLocation"));
            }
            
            // Agregar cualquier otro atributo que no haya sido procesado
            for (java.util.Map.Entry<String, String> entry : atributos.entrySet()) {
                String nombre = entry.getKey();
                if (!elementoRaiz.hasAttribute(nombre)) {
                    elementoRaiz.setAttribute(nombre, entry.getValue());
                }
            }
            
            logger.debug("✓ Atributos del elemento Retenciones reordenados según el orden del XSLT");
        } catch (Exception e) {
            logger.warn("⚠️ No se pudo reordenar los atributos: {}", e.getMessage());
            // Continuar sin reordenar si hay error
        }
    }
    
    
    /**
     * Intenta cargar la llave privada usando BouncyCastle
     * BouncyCastle soporta múltiples formatos: PKCS#1, PKCS#8, PEM, DER, etc.
     * @return PrivateKey si se carga exitosamente, null si falla
     */
    private PrivateKey tryLoadWithBouncyCastle(byte[] keyBytes, String keyPem) {
        try {
            logger.debug("Intentando cargar llave privada con BouncyCastle...");
            
            // 1. Intentar como PEM (texto con headers)
            if (keyPem != null && (keyPem.contains("-----BEGIN") || keyPem.length() > 100)) {
                try {
                    java.io.StringReader reader = new java.io.StringReader(keyPem);
                    PEMParser pemParser = new PEMParser(reader);
                    Object parsedObject = pemParser.readObject();
                    pemParser.close();
                    
                    if (parsedObject != null) {
                        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                        
                        // Manejar diferentes tipos de objetos PEM
                        if (parsedObject instanceof org.bouncycastle.openssl.PEMKeyPair) {
                            org.bouncycastle.openssl.PEMKeyPair keyPair = (org.bouncycastle.openssl.PEMKeyPair) parsedObject;
                            java.security.KeyPair kp = converter.getKeyPair(keyPair);
                            logger.info("✓ Llave privada cargada con BouncyCastle desde PEM KeyPair");
                            return kp.getPrivate();
                        } else if (parsedObject instanceof PrivateKeyInfo) {
                            PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) parsedObject;
                            PrivateKey key = converter.getPrivateKey(privateKeyInfo);
                            logger.info("✓ Llave privada cargada con BouncyCastle desde PEM PrivateKeyInfo");
                            return key;
                        } else if (parsedObject instanceof PKCS8EncryptedPrivateKeyInfo) {
                            // Llave encriptada con contraseña
                            PKCS8EncryptedPrivateKeyInfo encryptedInfo = (PKCS8EncryptedPrivateKeyInfo) parsedObject;
                            if (llavePassword != null && !llavePassword.isEmpty()) {
                                // Usar BouncyCastle Provider explícitamente para soportar algoritmos como RC2-CBC
                                InputDecryptorProvider decryptorProvider = 
                                    new JcePKCSPBEInputDecryptorProviderBuilder()
                                        .setProvider("BC")  // Usar BouncyCastle Provider explícitamente
                                        .build(llavePassword.toCharArray());
                                PrivateKeyInfo privateKeyInfo = encryptedInfo.decryptPrivateKeyInfo(decryptorProvider);
                                PrivateKey key = converter.getPrivateKey(privateKeyInfo);
                                logger.info("✓ Llave privada encriptada cargada con BouncyCastle");
                                return key;
                            } else {
                                logger.warn("Llave privada está encriptada pero no se proporcionó contraseña");
                            }
                        } else {
                            // Tipo de objeto PEM no reconocido como llave privada
                            logger.debug("Tipo de objeto PEM no reconocido como llave privada: {}. " +
                                "Puede ser un certificado u otro tipo de objeto.", parsedObject.getClass().getName());
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Error al parsear PEM con BouncyCastle: {}", e.getMessage());
                }
            }
            
            // 2. Intentar como DER binario (PKCS#1 o PKCS#8)
            // Primero verificar que los bytes empiecen con 0x30 (SEQUENCE) para DER válido
            if (keyBytes.length > 0 && (keyBytes[0] & 0xFF) == 0x30) {
                try {
                    // Si hay contraseña, intentar descifrar como PKCS#8 encriptado primero
                    if (llavePassword != null && !llavePassword.isEmpty()) {
                        try {
                            logger.info("Intentando descifrar llave DER encriptada con contraseña ({} caracteres)...", llavePassword.length());
                            // Intentar parsear como EncryptedPrivateKeyInfo usando ASN1InputStream
                            ByteArrayInputStream bais = new ByteArrayInputStream(keyBytes);
                            ASN1InputStream asn1In = new ASN1InputStream(bais);
                            ASN1Sequence seq = (ASN1Sequence) asn1In.readObject();
                            asn1In.close();
                            
                            // Intentar crear EncryptedPrivateKeyInfo desde la secuencia
                            try {
                                EncryptedPrivateKeyInfo encInfo = EncryptedPrivateKeyInfo.getInstance(seq);
                                PKCS8EncryptedPrivateKeyInfo encryptedInfo = new PKCS8EncryptedPrivateKeyInfo(encInfo);
                                
                                logger.info("✓ EncryptedPrivateKeyInfo creado exitosamente. Intentando descifrar con contraseña...");
                                // Usar BouncyCastle Provider explícitamente para soportar algoritmos como RC2-CBC
                                InputDecryptorProvider decryptorProvider = 
                                    new JcePKCSPBEInputDecryptorProviderBuilder()
                                        .setProvider("BC")  // Usar BouncyCastle Provider explícitamente
                                        .build(llavePassword.toCharArray());
                                PrivateKeyInfo privateKeyInfo = encryptedInfo.decryptPrivateKeyInfo(decryptorProvider);
                                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                                PrivateKey key = converter.getPrivateKey(privateKeyInfo);
                                logger.info("✓✓✓ Llave privada DER encriptada descifrada exitosamente con BouncyCastle");
                                return key;
                            } catch (Exception eEnc) {
                                logger.error("✗✗✗ Error al descifrar EncryptedPrivateKeyInfo DER: {}", eEnc.getMessage());
                                logger.error("✗✗✗ Detalles del error: {}", eEnc.getClass().getSimpleName());
                                if (eEnc.getMessage() != null && eEnc.getMessage().contains("password") || 
                                    eEnc.getMessage() != null && eEnc.getMessage().contains("contraseña") ||
                                    eEnc.getMessage() != null && eEnc.getMessage().contains("wrong") ||
                                    eEnc.getMessage() != null && eEnc.getMessage().contains("incorrect")) {
                                    logger.error("✗✗✗ ERROR: La contraseña puede ser INCORRECTA. Verifica facturacion.csd.llave.password en application.yml");
                                }
                            }
                        } catch (Exception eParse) {
                            logger.warn("⚠️ No es formato EncryptedPrivateKeyInfo DER: {}", eParse.getMessage());
                            logger.debug("Error al parsear DER como encriptado (detalles): ", eParse);
                        }
                    }
                    
                    // Intentar como PKCS#8 DER sin encriptar (formato más común y estándar)
                    try {
                        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(keyBytes);
                        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                        PrivateKey key = converter.getPrivateKey(privateKeyInfo);
                        logger.info("✓ Llave privada cargada con BouncyCastle como PKCS#8 DER");
                        return key;
                    } catch (Exception e2) {
                        logger.debug("No es PKCS#8 DER: {}", e2.getMessage());
                    }
                    
                    // Intentar como PKCS#1 (RSA Private Key) solo si PKCS#8 falló
                    try {
                        org.bouncycastle.asn1.pkcs.RSAPrivateKey rsaPrivateKey = 
                            org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(keyBytes);
                        
                        java.security.spec.RSAPrivateCrtKeySpec keySpec = new java.security.spec.RSAPrivateCrtKeySpec(
                            rsaPrivateKey.getModulus(),
                            rsaPrivateKey.getPublicExponent(),
                            rsaPrivateKey.getPrivateExponent(),
                            rsaPrivateKey.getPrime1(),
                            rsaPrivateKey.getPrime2(),
                            rsaPrivateKey.getExponent1(),
                            rsaPrivateKey.getExponent2(),
                            rsaPrivateKey.getCoefficient()
                        );
                        
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        PrivateKey key = keyFactory.generatePrivate(keySpec);
                        logger.info("✓ Llave privada cargada con BouncyCastle como PKCS#1 DER");
                        return key;
                    } catch (Exception e) {
                        logger.debug("No es PKCS#1 DER: {}", e.getMessage());
                        // El error "Se esperaba INTEGER (0x02), se encontró: 0x30" indica que el archivo
                        // puede ser un certificado o un formato no estándar, no una llave privada
                        if (e.getMessage() != null && e.getMessage().contains("INTEGER")) {
                            logger.warn("El archivo parece ser DER pero no es una llave privada válida. " +
                                "Puede ser un certificado o formato no estándar. " +
                                "Verifique que el archivo .key sea realmente una llave privada.");
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Error general al parsear DER: {}", e.getMessage());
                }
            } else {
                logger.debug("Los bytes no empiezan con 0x30 (SEQUENCE), no es formato DER binario válido");
            }
            
            // 3. Intentar decodificar Base64 primero y luego parsear como DER
            if (keyPem != null) {
                String trimmed = keyPem.trim().replaceAll("\\s", "");
                if (trimmed.matches("^[A-Za-z0-9+/=]+$") && trimmed.length() > 100) {
                    try {
                        byte[] decoded = Base64.getDecoder().decode(trimmed);
                        // Intentar parsear los bytes decodificados como DER
                        try {
                            // Intentar como PKCS#1
                            org.bouncycastle.asn1.pkcs.RSAPrivateKey rsaPrivateKey = 
                                org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(decoded);
                            
                            java.security.spec.RSAPrivateCrtKeySpec keySpec = new java.security.spec.RSAPrivateCrtKeySpec(
                                rsaPrivateKey.getModulus(),
                                rsaPrivateKey.getPublicExponent(),
                                rsaPrivateKey.getPrivateExponent(),
                                rsaPrivateKey.getPrime1(),
                                rsaPrivateKey.getPrime2(),
                                rsaPrivateKey.getExponent1(),
                                rsaPrivateKey.getExponent2(),
                                rsaPrivateKey.getCoefficient()
                            );
                            
                            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                            PrivateKey key = keyFactory.generatePrivate(keySpec);
                            logger.info("✓ Llave privada cargada con BouncyCastle desde Base64 decodificado (PKCS#1)");
                            return key;
                        } catch (Exception e1) {
                            // Intentar como PKCS#8
                            try {
                                PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(decoded);
                                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                                PrivateKey key = converter.getPrivateKey(privateKeyInfo);
                                logger.info("✓ Llave privada cargada con BouncyCastle desde Base64 decodificado (PKCS#8)");
                                return key;
                            } catch (Exception e2) {
                                logger.debug("No es PKCS#8 DER decodificado: {}", e2.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("No se pudo decodificar Base64: {}", e.getMessage());
                    }
                }
            }
            
            return null;
            
        } catch (NoClassDefFoundError | Exception e) {
            logger.debug("BouncyCastle no disponible o error al cargar: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Lista los recursos disponibles en una carpeta del classpath para diagnóstico
     */
    private void listarRecursosDisponibles(String carpeta) {
        try {
            logger.warn("=== DIAGNÓSTICO: Recursos disponibles en classpath:{} ===", carpeta);
            var resources = getClass().getClassLoader().getResources(carpeta);
            if (resources.hasMoreElements()) {
                var resource = resources.nextElement();
                logger.warn("Recurso encontrado: {}", resource);
            } else {
                logger.warn("No se encontraron recursos en la carpeta: {}", carpeta);
                logger.warn("Asegúrate de que los archivos estén en: src/main/resources/{}/", carpeta);
            }
        } catch (Exception e) {
            logger.warn("No se pudieron listar los recursos disponibles: {}", e.getMessage());
        }
    }
    
    /**
     * Guarda la cadena original en un archivo temporal para facilitar la comparación con Finkok
     */
    private void guardarCadenaOriginalEnArchivo(String cadenaOriginal) {
        try {
            // Crear directorio temporal si no existe
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "cfdi_cadenas_originales");
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            
            // Crear archivo con timestamp
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path archivo = tempDir.resolve("cadena_original_" + timestamp + ".txt");
            
            // Escribir la cadena original
            Files.write(archivo, cadenaOriginal.getBytes(StandardCharsets.UTF_8));
            
            logger.info("✓ Cadena original guardada en archivo: {}", archivo.toAbsolutePath());
            logger.info("  Puedes abrir este archivo para copiar la cadena original y compararla con Finkok");
        } catch (Exception e) {
            logger.debug("No se pudo guardar la cadena original en archivo: {}", e.getMessage());
        }
    }
    
    /**
     * Guarda el XML firmado en un archivo temporal para facilitar la validación en Finkok
     */
    private void guardarXmlFirmadoEnArchivo(String xmlFirmado) {
        try {
            // Crear directorio temporal si no existe
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "cfdi_xml_firmados");
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            
            // Crear archivo con timestamp
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path archivo = tempDir.resolve("xml_firmado_" + timestamp + ".xml");
            
            // Escribir el XML firmado
            Files.write(archivo, xmlFirmado.getBytes(StandardCharsets.UTF_8));
            
            logger.info("✓ XML firmado guardado en archivo: {}", archivo.toAbsolutePath());
            logger.info("  Puedes abrir este archivo para copiar el XML y validarlo en https://validador.finkok.com");
        } catch (Exception e) {
            logger.debug("No se pudo guardar el XML firmado en archivo: {}", e.getMessage());
        }
    }

    /**
     * Valida y corrige el UsoCFDI según el tipo de persona (física o moral) y régimen fiscal
     * CRÍTICO: El UsoCFDI debe corresponder con el tipo de persona y régimen conforme al catálogo c_UsoCFDI
     * 
     * @param usoCfdi UsoCFDI proporcionado
     * @param rfc RFC del receptor (para determinar tipo de persona)
     * @param regimenFiscal Régimen fiscal del receptor
     * @return UsoCFDI válido corregido si es necesario
     */
    private String validarYCorregirUsoCFDI(String usoCfdi, String rfc, String regimenFiscal) {
        if (usoCfdi == null || usoCfdi.trim().isEmpty()) {
            logger.warn("⚠️ UsoCFDI vacío, usando valor por defecto según tipo de persona");
            // Determinar tipo de persona por longitud del RFC
            boolean esPersonaFisica = rfc != null && rfc.length() == 13;
            return esPersonaFisica ? "D01" : "G01"; // D01 para física, G01 para moral
        }

        String usoCfdiUpper = usoCfdi.trim().toUpperCase();
        
        // Determinar tipo de persona por longitud del RFC
        boolean esPersonaFisica = rfc != null && rfc.length() == 13;
        boolean esPersonaMoral = rfc != null && rfc.length() == 12;
        
        // Regímenes fiscales de persona física (principalmente)
        String[] regimenesPersonaFisica = {"605", "606", "607", "608", "610", "611", "612", "614", "615", "616", "621", "625", "626"};
        boolean esRegimenPersonaFisica = false;
        if (regimenFiscal != null) {
            for (String regimen : regimenesPersonaFisica) {
                if (regimen.equals(regimenFiscal)) {
                    esRegimenPersonaFisica = true;
                    break;
                }
            }
        }

        // Validar UsoCFDI según tipo de persona
        if (esPersonaFisica || esRegimenPersonaFisica) {
            // Persona Física: UsoCFDI válidos son principalmente D01-D10 y algunos G específicos (NO G01)
            if (usoCfdiUpper.startsWith("D") || 
                usoCfdiUpper.equals("G02") || usoCfdiUpper.equals("G03") || 
                usoCfdiUpper.equals("CP01") || usoCfdiUpper.equals("CN01")) {
                logger.debug("✓ UsoCFDI válido para persona física: {}", usoCfdiUpper);
                return usoCfdiUpper;
            } else if (usoCfdiUpper.equals("G01")) {
                // G01 NO es válido para persona física
                logger.warn("⚠️ UsoCFDI G01 no es válido para persona física. Corrigiendo a D01 (Gastos en general).");
                logger.warn("⚠️ Para persona física con régimen {}, los UsoCFDI válidos son: D01-D10, G02, G03, CP01, CN01", regimenFiscal);
                return "D01"; // Valor por defecto seguro para persona física
            } else {
                logger.warn("⚠️ UsoCFDI '{}' puede no ser válido para persona física. Verificando...", usoCfdiUpper);
                // Permitir otros códigos pero advertir
                return usoCfdiUpper;
            }
        } else if (esPersonaMoral) {
            // Persona Moral: UsoCFDI válidos son principalmente G01, G02, G03, etc.
            if (usoCfdiUpper.startsWith("G") || 
                usoCfdiUpper.equals("CP01") || usoCfdiUpper.equals("CN01")) {
                logger.debug("✓ UsoCFDI válido para persona moral: {}", usoCfdiUpper);
                return usoCfdiUpper;
            } else if (usoCfdiUpper.startsWith("D")) {
                // D01-D10 son principalmente para persona física
                logger.warn("⚠️ UsoCFDI '{}' (deducciones) generalmente es para persona física. Para persona moral se recomienda G01, G02, G03.", usoCfdiUpper);
                // Permitir pero advertir
                return usoCfdiUpper;
            } else {
                logger.warn("⚠️ UsoCFDI '{}' puede no ser válido para persona moral. Verificando...", usoCfdiUpper);
                return usoCfdiUpper;
            }
        } else {
            // Tipo de persona no determinado, usar el valor proporcionado pero advertir
            logger.warn("⚠️ No se pudo determinar el tipo de persona del RFC: {}. Usando UsoCFDI proporcionado: {}", rfc, usoCfdiUpper);
            return usoCfdiUpper;
        }
    }
}