package com.cibercom.facturacion_back.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Utilidad para ajustar el XML de Carta Porte antes de enviarlo al PAC.
 * El SAT indica que cuando existe Transporte Ferroviario y el atributo
 * {@code TipoEstacion} es igual a "02", no debe existir el nodo {@code Ubicacion:Domicilio}.
 * Este componente elimina dichos nodos para evitar rechazos en el timbrado.
 */
@Component
public class CartaPorteXmlSanitizer {

    private static final Logger logger = LoggerFactory.getLogger(CartaPorteXmlSanitizer.class);

    private final XPath xpath = XPathFactory.newInstance().newXPath();

    /**
     * Elimina los nodos Ubicacion:Domicilio cuando se trate de estaciones ferroviarias intermedias (TipoEstacion = "02").
     * Según la regla del SAT: cuando TipoEstacion="02" y existe TransporteFerroviario, no debe existir Ubicacion:Domicilio.
     * Este método elimina cualquier Domicilio en ubicaciones con TipoEstacion="02" para evitar rechazos del PAC.
     *
     * @param xml XML completo del CFDI.
     * @return XML sanitizado o el original en caso de no aplicar cambios o si ocurre un error.
     */
    public String sanitizeUbicacionDomicilioForFerroviario(String xml) {
        if (xml == null || xml.isBlank()) {
            logger.warn("XML nulo o vacío recibido para sanitizar");
            return xml;
        }

        if (!xml.contains("CartaPorte")) {
            logger.debug("XML no contiene CartaPorte, no se requiere sanitización");
            return xml;
        }

        // VERIFICACIÓN PRELIMINAR: Verificar condiciones antes de parsear
        boolean tieneTipoEstacion02String = xml.contains("TipoEstacion=\"02\"") || xml.contains("TipoEstacion='02'");
        boolean tieneTransporteFerroviarioString = xml.contains("TransporteFerroviario") || 
                                                   xml.contains("cartaporte31:TransporteFerroviario") ||
                                                   xml.contains("cartaporte30:TransporteFerroviario") ||
                                                   xml.contains("cartaporte20:TransporteFerroviario");
        boolean tieneDomicilioString = xml.contains("<Domicilio") || xml.contains(":Domicilio");
        
        logger.warn("🔍 VERIFICACIÓN PRELIMINAR (string): TipoEstacion='02': {}, TransporteFerroviario: {}, Domicilio: {}", 
                tieneTipoEstacion02String, tieneTransporteFerroviarioString, tieneDomicilioString);
        
        // Si hay TransporteFerroviario y TipoEstacion="02" y Domicilio, aplicar eliminación inmediata con regex
        if (tieneTransporteFerroviarioString && tieneTipoEstacion02String && tieneDomicilioString) {
            logger.error("🚨🚨🚨 CRÍTICO: XML contiene TipoEstacion='02', TransporteFerroviario Y nodos Domicilio. " +
                    "Aplicando eliminación inmediata con regex antes de parsear...");
            
            // Eliminar nodos Domicilio usando regex antes de parsear
            String xmlAntesRegex = xml;
            xml = xml.replaceAll("<[^:]*:Domicilio[^>]*>.*?</[^:]*:Domicilio>", "");
            xml = xml.replaceAll("<[^:]*:Domicilio[^>]*/>", "");
            
            if (!xml.equals(xmlAntesRegex)) {
                logger.warn("✓ Eliminados nodos Domicilio usando regex preliminar. Continuando con sanitización normal...");
            }
        }

        try {
            logger.info("=== INICIANDO SANITIZACIÓN DE CARTA PORTE ===");
            
            Document document = parseXml(xml);
            if (document == null) {
                logger.error("No se pudo parsear el XML, retornando XML original");
                return xml;
            }

            // Verificar si existe TransporteFerroviario
            boolean tieneTransporteFerroviario = containsTransporteFerroviario(document);
            logger.info("🔍 Verificación inicial: XML contiene TransporteFerroviario: {}", tieneTransporteFerroviario);

            // CRÍTICO: Si existe TransporteFerroviario, buscar TODAS las ubicaciones (no solo TipoEstacion="02")
            // y eliminar TODOS los nodos Domicilio de TODAS las ubicaciones cuando TipoEstacion="02"
            // Buscar TODAS las ubicaciones con TipoEstacion="02"
            // Según la regla del SAT: cuando TipoEstacion="02" y existe TransporteFerroviario,
            // NO debe existir el nodo Ubicacion:Domicilio
            // IMPORTANTE: Si hay TipoEstacion="02", debemos verificar y eliminar Domicilio
            // incluso si no hay TransporteFerroviario todavía (por precaución)
            NodeList ubicacionesConTipo02 = (NodeList) xpath.evaluate(
                    "//*[local-name()='Ubicacion' and @TipoEstacion='02']",
                    document,
                    XPathConstants.NODESET
            );

            // También buscar TODAS las ubicaciones si hay TransporteFerroviario (para ser más agresivo)
            NodeList todasLasUbicaciones = null;
            if (tieneTransporteFerroviario) {
                todasLasUbicaciones = (NodeList) xpath.evaluate(
                        "//*[local-name()='Ubicacion']",
                        document,
                        XPathConstants.NODESET
                );
                logger.info("🔍 TransporteFerroviario detectado. Verificando TODAS las {} ubicaciones para nodos Domicilio.", 
                        todasLasUbicaciones != null ? todasLasUbicaciones.getLength() : 0);
            }

            if ((ubicacionesConTipo02 == null || ubicacionesConTipo02.getLength() == 0) && 
                (todasLasUbicaciones == null || todasLasUbicaciones.getLength() == 0)) {
                if (!tieneTransporteFerroviario) {
                    logger.debug("No hay ubicaciones con TipoEstacion='02' ni TransporteFerroviario. No se requiere sanitización.");
                } else {
                    logger.debug("Hay TransporteFerroviario pero no hay ubicaciones. No se requiere sanitización.");
                }
                return xml;
            }

            // Si no hay TransporteFerroviario pero hay TipoEstacion="02", solo advertir
            if (!tieneTransporteFerroviario) {
                logger.warn("⚠️ ADVERTENCIA: Se detectaron ubicaciones con TipoEstacion='02' pero NO hay TransporteFerroviario. " +
                        "Eliminando nodos Domicilio por precaución para evitar rechazos del SAT.");
            } else {
                logger.info("⚠️ ADVERTENCIA: Se detectó TransporteFerroviario. Aplicando reglas del SAT para TipoEstacion='02'...");
            }

            logger.warn("🚨 CRÍTICO: Se encontraron {} ubicaciones con TipoEstacion='02'. " +
                    "Según reglas del SAT, NO debe existir nodo Domicilio en estas ubicaciones cuando existe TransporteFerroviario. " +
                    "TransporteFerroviario presente: {}. Verificando y eliminando nodos Domicilio...", 
                    ubicacionesConTipo02.getLength(), tieneTransporteFerroviario);

            boolean removed = false;
            int domiciliosEliminados = 0;
            
            // CRÍTICO: Si hay TransporteFerroviario, procesar TODAS las ubicaciones, no solo las con TipoEstacion="02"
            // Esto asegura que eliminemos cualquier Domicilio que pueda estar presente
            NodeList ubicacionesAProcesar = ubicacionesConTipo02;
            if (tieneTransporteFerroviario && todasLasUbicaciones != null && todasLasUbicaciones.getLength() > 0) {
                // Procesar TODAS las ubicaciones cuando hay TransporteFerroviario
                ubicacionesAProcesar = todasLasUbicaciones;
                logger.warn("🚨 CRÍTICO: TransporteFerroviario detectado. Procesando TODAS las {} ubicaciones para eliminar nodos Domicilio.", 
                        ubicacionesAProcesar.getLength());
            }
            
            if (ubicacionesAProcesar == null || ubicacionesAProcesar.getLength() == 0) {
                logger.debug("No hay ubicaciones a procesar.");
                return xml;
            }
            
            // Iterar sobre TODAS las ubicaciones a procesar
            for (int i = 0; i < ubicacionesAProcesar.getLength(); i++) {
                Node ubicacion = ubicacionesAProcesar.item(i);
                
                // Obtener el atributo TipoEstacion para verificación adicional
                String tipoEstacionAttr = null;
                String tipoUbicacionAttr = null;
                if (ubicacion.getAttributes() != null) {
                    Node tipoEstacionNode = ubicacion.getAttributes().getNamedItem("TipoEstacion");
                    Node tipoUbicacionNode = ubicacion.getAttributes().getNamedItem("TipoUbicacion");
                    if (tipoEstacionNode != null) {
                        tipoEstacionAttr = tipoEstacionNode.getNodeValue();
                    }
                    if (tipoUbicacionNode != null) {
                        tipoUbicacionAttr = tipoUbicacionNode.getNodeValue();
                    }
                }
                
            // CRÍTICO: Si hay TransporteFerroviario y TipoEstacion="02", eliminar TODOS los nodos Domicilio
            // Regla del SAT: NO debe existir nodo Ubicacion:Domicilio cuando TipoEstacion="02" y existe TransporteFerroviario
            boolean esTipoEstacion02 = "02".equals(tipoEstacionAttr);
            boolean debeEliminarDomicilio = tieneTransporteFerroviario && esTipoEstacion02;
            
            // ADICIONAL: Si TipoEstacion="02", eliminar Domicilio incluso si no hay TransporteFerroviario detectado
            // (por precaución, ya que la regla del SAT es específica sobre TipoEstacion="02")
            if (esTipoEstacion02 && !debeEliminarDomicilio) {
                logger.warn("⚠️ PRECAUCIÓN: Ubicación {} tiene TipoEstacion='02' pero TransporteFerroviario no detectado. " +
                        "Eliminando Domicilio por precaución para cumplir con reglas del SAT.", i + 1);
                debeEliminarDomicilio = true;
            }
            
            logger.info("📍 Procesando ubicación {}: TipoUbicacion='{}', TipoEstacion='{}', TransporteFerroviario={}, DebeEliminarDomicilio={}", 
                    i + 1, tipoUbicacionAttr, tipoEstacionAttr, tieneTransporteFerroviario, debeEliminarDomicilio);
                
                // Buscar TODOS los nodos Domicilio dentro de esta ubicación (hijos directos y descendientes)
                // Usar múltiples XPath para asegurar detección completa
                NodeList domiciliosDescendientes = (NodeList) xpath.evaluate(
                        ".//*[local-name()='Domicilio']",
                        ubicacion,
                        XPathConstants.NODESET
                );
                
                // Búsqueda adicional por namespace
                NodeList domiciliosPorNamespace = (NodeList) xpath.evaluate(
                        ".//*[contains(local-name(), 'Domicilio')]",
                        ubicacion,
                        XPathConstants.NODESET
                );
                
                // Combinar ambas búsquedas
                if (domiciliosPorNamespace != null && domiciliosPorNamespace.getLength() > 0) {
                    // Si encontramos más nodos por namespace, usar esa lista
                    if (domiciliosDescendientes == null || domiciliosPorNamespace.getLength() > domiciliosDescendientes.getLength()) {
                        domiciliosDescendientes = domiciliosPorNamespace;
                    }
                }
                
                int totalDomicilios = (domiciliosDescendientes != null) ? domiciliosDescendientes.getLength() : 0;
                
                // CRÍTICO: Si debeEliminarDomicilio es true, eliminar TODOS los nodos Domicilio
                if (debeEliminarDomicilio) {
                    if (totalDomicilios > 0) {
                        logger.error("❌ ERROR CRÍTICO: Ubicación {} (TipoEstacion='{}') contiene {} nodos Domicilio. " +
                                "Esto viola las reglas del SAT cuando TipoEstacion='02' y existe TransporteFerroviario. " +
                                "TransporteFerroviario presente: {}. Eliminando TODOS los nodos Domicilio...", 
                                i + 1, tipoEstacionAttr, totalDomicilios, tieneTransporteFerroviario);
                        
                        // Usar método agresivo para eliminar TODOS los nodos Domicilio
                        int eliminadosEnEstaUbicacion = eliminarTodosLosDomiciliosDeUbicacion(ubicacion, i + 1);
                        if (eliminadosEnEstaUbicacion > 0) {
                            removed = true;
                            domiciliosEliminados += eliminadosEnEstaUbicacion;
                            logger.warn("✓✓ Eliminados {} nodos Domicilio de ubicación {} usando método agresivo", 
                                    eliminadosEnEstaUbicacion, i + 1);
                        }
                        
                        // Verificar nuevamente después de la eliminación
                        NodeList domiciliosRestantes = (NodeList) xpath.evaluate(
                                ".//*[local-name()='Domicilio']",
                                ubicacion,
                                XPathConstants.NODESET
                        );
                        int restantes = (domiciliosRestantes != null) ? domiciliosRestantes.getLength() : 0;
                        if (restantes > 0) {
                            logger.error("❌❌ ADVERTENCIA: Aún quedan {} nodos Domicilio en ubicación {} después de eliminación agresiva", 
                                    restantes, i + 1);
                            // Intentar una vez más con el método original
                            for (int j = domiciliosRestantes.getLength() - 1; j >= 0; j--) {
                                Node domicilio = domiciliosRestantes.item(j);
                                Node parent = domicilio.getParentNode();
                                if (parent != null) {
                                    try {
                                        parent.removeChild(domicilio);
                                        removed = true;
                                        domiciliosEliminados++;
                                        logger.warn("✓ Eliminado nodo Domicilio restante '{}' de ubicación {}", 
                                                domicilio.getNodeName(), i + 1);
                                    } catch (Exception e) {
                                        logger.error("Error al eliminar nodo Domicilio restante: {}", e.getMessage(), e);
                                    }
                                }
                            }
                        }
                    } else {
                        logger.info("✓ Ubicación {} (TipoEstacion='02') NO tiene nodos Domicilio. Cumple con reglas del SAT.", i + 1);
                    }
                } else if (totalDomicilios > 0) {
                    logger.debug("📍 Ubicación {} tiene {} nodos Domicilio pero no requiere eliminación (TipoEstacion='{}', TransporteFerroviario={})", 
                            i + 1, totalDomicilios, tipoEstacionAttr, tieneTransporteFerroviario);
                }
            }

            // CRÍTICO: Verificación adicional más agresiva
            // Si hay TransporteFerroviario, buscar y eliminar TODOS los nodos Domicilio en ubicaciones con TipoEstacion="02"
            // sin importar cómo estén estructurados
            if (tieneTransporteFerroviario && (ubicacionesConTipo02 != null && ubicacionesConTipo02.getLength() > 0)) {
                logger.warn("🚨 VERIFICACIÓN ADICIONAL: Buscando y eliminando TODOS los nodos Domicilio en ubicaciones con TipoEstacion='02'...");
                
                // Buscar TODOS los nodos Domicilio en el documento completo
                NodeList todosLosDomicilios = (NodeList) xpath.evaluate(
                        "//*[local-name()='Domicilio']",
                        document,
                        XPathConstants.NODESET
                );
                
                if (todosLosDomicilios != null && todosLosDomicilios.getLength() > 0) {
                    logger.warn("🔍 Se encontraron {} nodos Domicilio en total. Verificando si están en ubicaciones con TipoEstacion='02'...", 
                            todosLosDomicilios.getLength());
                    
                    // Verificar cada Domicilio y eliminar si está en una ubicación con TipoEstacion="02"
                    for (int k = todosLosDomicilios.getLength() - 1; k >= 0; k--) {
                        Node domicilio = todosLosDomicilios.item(k);
                        Node parent = domicilio.getParentNode();
                        
                        // Verificar si el padre es una Ubicacion con TipoEstacion="02"
                        if (parent != null && "Ubicacion".equals(parent.getLocalName())) {
                            Node tipoEstacionNode = parent.getAttributes() != null ? 
                                    parent.getAttributes().getNamedItem("TipoEstacion") : null;
                            
                            if (tipoEstacionNode != null && "02".equals(tipoEstacionNode.getNodeValue())) {
                                try {
                                    String nombreNodo = domicilio.getNodeName();
                                    parent.removeChild(domicilio);
                                    removed = true;
                                    domiciliosEliminados++;
                                    logger.warn("✓✓ Eliminado nodo Domicilio '{}' adicional de ubicación con TipoEstacion='02'", nombreNodo);
                                } catch (Exception e) {
                                    logger.error("Error al eliminar nodo Domicilio adicional: {}", e.getMessage(), e);
                                }
                            }
                        }
                    }
                }
            }

            if (!removed) {
                logger.info("✓ No se encontraron nodos Domicilio en ubicaciones con TipoEstacion='02'. El XML cumple con las reglas del SAT.");
                // Aún así normalizar el XML para asegurar formato correcto
                boolean hadDeclaration = xml.stripLeading().startsWith("<?xml");
                String transformed = transformToString(document, hadDeclaration);
                return transformed != null ? transformed : xml;
            }

            // CRÍTICO: Verificación final ultra-agresiva
            // Si hay TransporteFerroviario y TipoEstacion="02", eliminar TODOS los nodos Domicilio sin excepción
            if (tieneTransporteFerroviario) {
                logger.warn("🚨 VERIFICACIÓN FINAL ULTRA-AGRESIVA: TransporteFerroviario detectado. " +
                        "Eliminando TODOS los nodos Domicilio de ubicaciones con TipoEstacion='02'...");
                
                // Buscar TODAS las ubicaciones con TipoEstacion="02" nuevamente después de las eliminaciones
                NodeList ubicacionesTipo02Final = (NodeList) xpath.evaluate(
                        "//*[local-name()='Ubicacion' and @TipoEstacion='02']",
                        document,
                        XPathConstants.NODESET
                );
                
                if (ubicacionesTipo02Final != null && ubicacionesTipo02Final.getLength() > 0) {
                    for (int m = 0; m < ubicacionesTipo02Final.getLength(); m++) {
                        Node ubicacionFinal = ubicacionesTipo02Final.item(m);
                        NodeList domiciliosFinales = (NodeList) xpath.evaluate(
                                ".//*[local-name()='Domicilio']",
                                ubicacionFinal,
                                XPathConstants.NODESET
                        );
                        
                        if (domiciliosFinales != null && domiciliosFinales.getLength() > 0) {
                            logger.error("❌❌❌ ERROR CRÍTICO: Aún quedan {} nodos Domicilio en ubicación {} con TipoEstacion='02'. " +
                                    "Eliminando de forma forzada usando método agresivo...", domiciliosFinales.getLength(), m + 1);
                            
                            // Usar método agresivo para eliminar TODOS los nodos Domicilio
                            int eliminadosFinales = eliminarTodosLosDomiciliosDeUbicacion(ubicacionFinal, m + 1);
                            if (eliminadosFinales > 0) {
                                removed = true;
                                domiciliosEliminados += eliminadosFinales;
                                logger.warn("✓✓✓ Eliminados {} nodos Domicilio forzados de ubicación {} con TipoEstacion='02'", 
                                        eliminadosFinales, m + 1);
                            }
                            
                            // Verificar una vez más después de la eliminación agresiva
                            NodeList domiciliosRestantesFinales = (NodeList) xpath.evaluate(
                                    ".//*[local-name()='Domicilio']",
                                    ubicacionFinal,
                                    XPathConstants.NODESET
                            );
                            int restantesFinales = (domiciliosRestantesFinales != null) ? domiciliosRestantesFinales.getLength() : 0;
                            if (restantesFinales > 0) {
                                logger.error("❌❌❌ ERROR CRÍTICO: Aún quedan {} nodos Domicilio después de eliminación agresiva. " +
                                        "Intentando eliminación manual...", restantesFinales);
                                
                                // Último intento: eliminación manual
                                for (int n = domiciliosRestantesFinales.getLength() - 1; n >= 0; n--) {
                                    Node domFinal = domiciliosRestantesFinales.item(n);
                                    Node parentFinal = domFinal.getParentNode();
                                    if (parentFinal != null) {
                                        try {
                                            parentFinal.removeChild(domFinal);
                                            removed = true;
                                            domiciliosEliminados++;
                                            logger.error("✓✓✓ Eliminado nodo Domicilio manual de ubicación {} con TipoEstacion='02'", m + 1);
                                        } catch (Exception e) {
                                            logger.error("Error al eliminar nodo Domicilio manual: {}", e.getMessage(), e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CRÍTICO: Eliminar atributos Colonia, Localidad y Municipio de todos los nodos Domicilio cuando el país es MEX
            // El SAT requiere que Colonia sea una clave válida del catálogo c_Colonia cuando país es MEX.
            // El SAT requiere que Localidad sea una clave válida del catálogo c_Localidad cuando país es MEX.
            // El SAT requiere que Municipio sea una clave válida del catálogo c_Municipio cuando país es MEX.
            // Para evitar errores, eliminamos los atributos Colonia, Localidad y Municipio de todos los Domicilio con país MEX.
            removeColoniaAttributeFromDomicilioNodes(document);
            
            // Normalizar y retornar el XML sanitizado
            boolean hadDeclaration = xml.stripLeading().startsWith("<?xml");
            String sanitized = transformToString(document, hadDeclaration);
            if (sanitized != null) {
                // Verificación adicional: eliminar atributos Colonia, Localidad y Municipio usando regex si aún existen
                // Buscar todos los patrones posibles de Pais="MEX" o Pais='MEX'
                if (sanitized.contains("Pais=\"MEX\"") || sanitized.contains("Pais='MEX'") || 
                    sanitized.contains("Pais=\"MEX\"") || sanitized.contains("Pais='MEX'") ||
                    sanitized.matches(".*Pais\\s*=\\s*[\"']MEX[\"'].*")) {
                    // Eliminar atributos Colonia, Localidad y Municipio de todos los nodos Domicilio cuando país es MEX
                    // Usar múltiples patrones para asegurar eliminación completa
                    String antesRegex = sanitized;
                    // Eliminar Colonia
                    sanitized = sanitized.replaceAll(" Colonia=\"[^\"]*\"", "");
                    sanitized = sanitized.replaceAll(" Colonia='[^']*'", "");
                    sanitized = sanitized.replaceAll(" Colonia\\s*=\\s*\"[^\"]*\"", "");
                    sanitized = sanitized.replaceAll(" Colonia\\s*=\\s*'[^']*'", "");
                    // Eliminar Localidad
                    sanitized = sanitized.replaceAll(" Localidad=\"[^\"]*\"", "");
                    sanitized = sanitized.replaceAll(" Localidad='[^']*'", "");
                    sanitized = sanitized.replaceAll(" Localidad\\s*=\\s*\"[^\"]*\"", "");
                    sanitized = sanitized.replaceAll(" Localidad\\s*=\\s*'[^']*'", "");
                    // Eliminar Municipio
                    sanitized = sanitized.replaceAll(" Municipio=\"[^\"]*\"", "");
                    sanitized = sanitized.replaceAll(" Municipio='[^']*'", "");
                    sanitized = sanitized.replaceAll(" Municipio\\s*=\\s*\"[^\"]*\"", "");
                    sanitized = sanitized.replaceAll(" Municipio\\s*=\\s*'[^']*'", "");
                    if (!sanitized.equals(antesRegex)) {
                        logger.info("✓ Eliminados atributos Colonia, Localidad y Municipio de nodos Domicilio con país MEX usando regex");
                    }
                }
                // Verificación final exhaustiva: asegurarse de que no queden nodos Domicilio
                if (tieneTransporteFerroviario && sanitized.contains("TipoEstacion=\"02\"")) {
                    // Buscar cualquier patrón de Domicilio cerca de TipoEstacion="02"
                    String pattern = "TipoEstacion=\"02\"";
                    int idx = sanitized.indexOf(pattern);
                    if (idx >= 0) {
                        // Buscar desde el inicio de la Ubicacion hasta el cierre
                        int inicioUbicacion = sanitized.lastIndexOf("<", idx);
                        int finUbicacion = sanitized.indexOf(">", idx);
                        if (finUbicacion < 0) {
                            finUbicacion = sanitized.indexOf("/>", idx);
                            if (finUbicacion >= 0) finUbicacion += 2;
                        } else {
                            // Buscar el cierre completo de la Ubicacion
                            String afterApertura = sanitized.substring(finUbicacion + 1);
                            int cierreUbicacion = afterApertura.indexOf("</");
                            if (cierreUbicacion >= 0) {
                                int cierreCompleto = afterApertura.indexOf(">", cierreUbicacion);
                                if (cierreCompleto >= 0) {
                                    finUbicacion = finUbicacion + 1 + cierreCompleto + 1;
                                }
                            } else {
                                // Si no hay cierre, buscar el siguiente > o />
                                int siguienteCierre = afterApertura.indexOf("/>");
                                if (siguienteCierre >= 0) {
                                    finUbicacion = finUbicacion + 1 + siguienteCierre + 2;
                                }
                            }
                        }
                        
                        if (inicioUbicacion >= 0 && finUbicacion > inicioUbicacion) {
                            String seccionUbicacion = sanitized.substring(inicioUbicacion, Math.min(finUbicacion, sanitized.length()));
                            
                            // Verificar múltiples patrones de Domicilio
                            boolean tieneDomicilio = seccionUbicacion.contains("<Domicilio") || 
                                                   seccionUbicacion.contains(":Domicilio") ||
                                                   seccionUbicacion.matches(".*<[^:]*:Domicilio.*") ||
                                                   seccionUbicacion.matches(".*<[^>]*Domicilio[^>]*>.*");
                            
                            if (tieneDomicilio) {
                                logger.error("❌❌❌ ERROR CRÍTICO: El XML sanitizado TODAVÍA contiene nodos Domicilio cerca de TipoEstacion='02'");
                                logger.error("Sección del XML problemática: {}", seccionUbicacion.substring(0, Math.min(500, seccionUbicacion.length())));
                                
                                // Intentar eliminar usando regex como último recurso
                                String sanitizedAntes = sanitized;
                                sanitized = sanitized.replaceAll(
                                    "(<[^:]*:Ubicacion[^>]*TipoEstacion=\"02\"[^>]*>)([\\s\\S]*?)(<[^:]*:Domicilio[^>]*>)([\\s\\S]*?)(</[^:]*:Domicilio>)([\\s\\S]*?)(</[^:]*:Ubicacion>)",
                                    "$1$2$6$7"
                                );
                                sanitized = sanitized.replaceAll(
                                    "(<[^:]*:Ubicacion[^>]*TipoEstacion=\"02\"[^>]*>)([\\s\\S]*?)(<[^:]*:Domicilio[^>]*/>)([\\s\\S]*?)(</[^:]*:Ubicacion>)",
                                    "$1$2$4$5"
                                );
                                
                                if (!sanitized.equals(sanitizedAntes)) {
                                    logger.warn("✓ Se eliminaron nodos Domicilio usando regex como último recurso. Re-parseando XML para validar...");
                                    // Re-parsear el XML para asegurar que esté bien formado
                                    try {
                                        Document docRevalidado = parseXml(sanitized);
                                        if (docRevalidado != null) {
                                            sanitized = transformToString(docRevalidado, hadDeclaration);
                                            if (sanitized == null) {
                                                logger.error("❌ Error al re-transformar XML después de regex. Usando versión anterior.");
                                                sanitized = sanitizedAntes;
                                            } else {
                                                logger.info("✓ XML re-parseado y validado correctamente después de eliminación con regex");
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.error("❌ Error al re-parsear XML después de regex: {}. Usando versión anterior.", e.getMessage());
                                        sanitized = sanitizedAntes;
                                    }
                                }
                            }
                        }
                    }
                }
                
                logger.warn("✅ SANITIZACIÓN COMPLETADA: Se eliminaron {} nodos Ubicacion:Domicilio de ubicaciones con TipoEstacion='02'. " +
                        "TransporteFerroviario presente: {}. El XML ahora cumple con las reglas del SAT.", 
                        domiciliosEliminados, tieneTransporteFerroviario);
                logger.info("=== FIN SANITIZACIÓN ===");
                return sanitized;
            } else {
                logger.error("❌ ERROR: No se pudo convertir el documento a String después de eliminar nodos Domicilio. Retornando XML original.");
            }

        } catch (Exception e) {
            logger.error("❌ ERROR CRÍTICO al sanitizar el XML de Carta Porte: {}", e.getMessage(), e);
            logger.error("Stack trace completo:", e);
        }

        logger.warn("Retornando XML original debido a errores en el proceso de sanitización");
        return xml;
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        disableExternalEntities(factory);

        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(xml));
        return builder.parse(inputSource);
    }

    private void disableExternalEntities(DocumentBuilderFactory factory) {
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {
            logger.debug("No se pudo deshabilitar DOCTYPE en DocumentBuilderFactory");
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (Exception ignored) {
            logger.debug("No se pudo deshabilitar external-general-entities en DocumentBuilderFactory");
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignored) {
            logger.debug("No se pudo deshabilitar external-parameter-entities en DocumentBuilderFactory");
        }
        try {
            factory.setXIncludeAware(false);
        } catch (Exception ignored) {
            logger.debug("No se pudo deshabilitar XInclude en DocumentBuilderFactory");
        }
        try {
            factory.setExpandEntityReferences(false);
        } catch (Exception ignored) {
            logger.debug("No se pudo deshabilitar expandEntityReferences en DocumentBuilderFactory");
        }
    }


    private boolean containsTransporteFerroviario(Document document) {
        try {
            // Buscar usando XPath para ser más robusto con namespaces
            NodeList nodes = (NodeList) xpath.evaluate(
                    "//*[local-name()='TransporteFerroviario']",
                    document,
                    XPathConstants.NODESET
            );
            if (nodes != null && nodes.getLength() > 0) {
                logger.debug("TransporteFerroviario encontrado: {} nodos", nodes.getLength());
                return true;
            }
            
            // Método alternativo: buscar por tag name sin namespace
            nodes = document.getElementsByTagNameNS("*", "TransporteFerroviario");
            if (nodes != null && nodes.getLength() > 0) {
                logger.debug("TransporteFerroviario encontrado por getElementsByTagNameNS: {} nodos", nodes.getLength());
                return true;
            }
            
            // Último recurso: buscar por tag name sin considerar namespace
            nodes = document.getElementsByTagName("TransporteFerroviario");
            boolean found = nodes != null && nodes.getLength() > 0;
            if (found) {
                logger.debug("TransporteFerroviario encontrado por getElementsByTagName: {} nodos", nodes.getLength());
            }
            return found;
        } catch (Exception e) {
            logger.debug("Error verificando TransporteFerroviario: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Método auxiliar para eliminar TODOS los nodos Domicilio de una ubicación de forma agresiva.
     * Usa múltiples métodos para asegurar que se eliminen todos los nodos, incluso si están anidados.
     */
    private int eliminarTodosLosDomiciliosDeUbicacion(Node ubicacion, int ubicacionIndex) {
        int eliminados = 0;
        
        // Método 1: Buscar por local-name (ignora namespace)
        try {
            NodeList domicilios = (NodeList) xpath.evaluate(
                    ".//*[local-name()='Domicilio']",
                    ubicacion,
                    XPathConstants.NODESET
            );
            
            if (domicilios != null && domicilios.getLength() > 0) {
                logger.warn("🔍 Método 1: Encontrados {} nodos Domicilio en ubicación {}", domicilios.getLength(), ubicacionIndex);
                for (int i = domicilios.getLength() - 1; i >= 0; i--) {
                    Node dom = domicilios.item(i);
                    Node parent = dom.getParentNode();
                    if (parent != null) {
                        try {
                            parent.removeChild(dom);
                            eliminados++;
                            logger.warn("✓ Eliminado nodo Domicilio (método 1) de ubicación {}", ubicacionIndex);
                        } catch (Exception e) {
                            logger.error("Error eliminando Domicilio (método 1): {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error en método 1 de eliminación: {}", e.getMessage());
        }
        
        // Método 2: Buscar por nombre completo con namespace
        try {
            NodeList allNodes = (NodeList) xpath.evaluate(".//*", ubicacion, XPathConstants.NODESET);
            if (allNodes != null) {
                for (int i = allNodes.getLength() - 1; i >= 0; i--) {
                    Node node = allNodes.item(i);
                    String localName = node.getLocalName();
                    String nodeName = node.getNodeName();
                    
                    if ((localName != null && localName.equals("Domicilio")) || 
                        (nodeName != null && nodeName.contains("Domicilio"))) {
                        Node parent = node.getParentNode();
                        if (parent != null) {
                            try {
                                parent.removeChild(node);
                                eliminados++;
                                logger.warn("✓ Eliminado nodo Domicilio (método 2) de ubicación {}", ubicacionIndex);
                            } catch (Exception e) {
                                logger.error("Error eliminando Domicilio (método 2): {}", e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error en método 2 de eliminación: {}", e.getMessage());
        }
        
        // Método 3: Buscar recursivamente en todos los hijos
        try {
            eliminarDomiciliosRecursivo(ubicacion, ubicacionIndex, 0);
        } catch (Exception e) {
            logger.error("Error en método 3 (recursivo) de eliminación: {}", e.getMessage());
        }
        
        return eliminados;
    }
    
    /**
     * Método recursivo para eliminar nodos Domicilio
     */
    private void eliminarDomiciliosRecursivo(Node node, int ubicacionIndex, int depth) {
        if (node == null || depth > 10) { // Limitar profundidad para evitar loops infinitos
            return;
        }
        
        // Verificar si este nodo es un Domicilio
        String localName = node.getLocalName();
        String nodeName = node.getNodeName();
        
        if ((localName != null && localName.equals("Domicilio")) || 
            (nodeName != null && nodeName.contains("Domicilio"))) {
            Node parent = node.getParentNode();
            if (parent != null) {
                try {
                    parent.removeChild(node);
                    logger.warn("✓ Eliminado nodo Domicilio (método recursivo, profundidad {}) de ubicación {}", depth, ubicacionIndex);
                } catch (Exception e) {
                    logger.error("Error eliminando Domicilio (recursivo): {}", e.getMessage());
                }
                return; // No procesar hijos de un nodo eliminado
            }
        }
        
        // Procesar hijos
        NodeList children = node.getChildNodes();
        if (children != null) {
            // Crear lista de hijos antes de iterar (para evitar problemas al modificar durante iteración)
            java.util.List<Node> childrenList = new java.util.ArrayList<>();
            for (int i = 0; i < children.getLength(); i++) {
                childrenList.add(children.item(i));
            }
            
            for (Node child : childrenList) {
                eliminarDomiciliosRecursivo(child, ubicacionIndex, depth + 1);
            }
        }
    }

    /**
     * Elimina los atributos Colonia, Localidad y Municipio de todos los nodos Domicilio que tengan país MEX.
     * El SAT requiere que Colonia sea una clave válida del catálogo c_Colonia cuando el país es MEX.
     * El SAT requiere que Localidad sea una clave válida del catálogo c_Localidad cuando el país es MEX.
     * El SAT requiere que Municipio sea una clave válida del catálogo c_Municipio cuando el país es MEX.
     */
    private void removeColoniaAttributeFromDomicilioNodes(Document document) {
        try {
            // Buscar todos los nodos Domicilio en el documento
            NodeList domicilios = (NodeList) xpath.evaluate(
                    "//*[local-name()='Domicilio']",
                    document,
                    XPathConstants.NODESET
            );
            
            if (domicilios == null || domicilios.getLength() == 0) {
                return;
            }
            
            int coloniasEliminadas = 0;
            int localidadesEliminadas = 0;
            int municipiosEliminados = 0;
            for (int i = 0; i < domicilios.getLength(); i++) {
                Node domicilio = domicilios.item(i);
                
                if (domicilio.getAttributes() == null) {
                    continue;
                }
                
                // Verificar si el nodo Domicilio tiene atributo Pais con valor MEX
                Node paisAttr = domicilio.getAttributes().getNamedItem("Pais");
                String paisValue = null;
                if (paisAttr != null) {
                    paisValue = paisAttr.getNodeValue();
                }
                
                // Si el país es MEX, eliminar los atributos Colonia, Localidad y Municipio
                if (paisValue != null && "MEX".equalsIgnoreCase(paisValue.trim())) {
                    // Eliminar Colonia
                    Node coloniaAttr = domicilio.getAttributes().getNamedItem("Colonia");
                    if (coloniaAttr != null) {
                        try {
                            domicilio.getAttributes().removeNamedItem("Colonia");
                            coloniasEliminadas++;
                            logger.info("✓ Eliminado atributo Colonia de nodo Domicilio con país MEX");
                        } catch (Exception e) {
                            logger.warn("Error al eliminar atributo Colonia: {}", e.getMessage());
                        }
                    }
                    
                    // Eliminar Localidad
                    Node localidadAttr = domicilio.getAttributes().getNamedItem("Localidad");
                    if (localidadAttr != null) {
                        try {
                            domicilio.getAttributes().removeNamedItem("Localidad");
                            localidadesEliminadas++;
                            logger.info("✓ Eliminado atributo Localidad de nodo Domicilio con país MEX");
                        } catch (Exception e) {
                            logger.warn("Error al eliminar atributo Localidad: {}", e.getMessage());
                        }
                    }
                    
                    // Eliminar Municipio
                    Node municipioAttr = domicilio.getAttributes().getNamedItem("Municipio");
                    if (municipioAttr != null) {
                        try {
                            domicilio.getAttributes().removeNamedItem("Municipio");
                            municipiosEliminados++;
                            logger.info("✓ Eliminado atributo Municipio de nodo Domicilio con país MEX");
                        } catch (Exception e) {
                            logger.warn("Error al eliminar atributo Municipio: {}", e.getMessage());
                        }
                    }
                }
            }
            
            if (coloniasEliminadas > 0 || localidadesEliminadas > 0 || municipiosEliminados > 0) {
                logger.info("✓✓ Eliminados {} atributos Colonia, {} atributos Localidad y {} atributos Municipio de nodos Domicilio con país MEX", 
                        coloniasEliminadas, localidadesEliminadas, municipiosEliminados);
            }
        } catch (Exception e) {
            logger.warn("Error al eliminar atributos Colonia/Localidad/Municipio: {}", e.getMessage());
        }
    }

    private String transformToString(Document document, boolean hadDeclaration) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, hadDeclaration ? "no" : "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            logger.warn("Error al convertir Document a String: {}", e.getMessage());
            return null;
        }
    }
}

