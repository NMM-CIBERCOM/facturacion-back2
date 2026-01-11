package com.cibercom.facturacion_back.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

@Service
public class CodigoPostalService {

    private static final Logger log = LoggerFactory.getLogger(CodigoPostalService.class);
    private static final String XML_FILE = "CPdescarga.xml";
    private Map<String, List<CodigoPostalData>> cache = new HashMap<>();
    private boolean cacheLoaded = false;

    public static class CodigoPostalData {
        private String codigoPostal;
        private String estado;
        private String municipio;
        private String colonia;

        public CodigoPostalData(String codigoPostal, String estado, String municipio, String colonia) {
            this.codigoPostal = codigoPostal;
            this.estado = estado;
            this.municipio = municipio;
            this.colonia = colonia;
        }

        public String getCodigoPostal() { return codigoPostal; }
        public String getEstado() { return estado; }
        public String getMunicipio() { return municipio; }
        public String getColonia() { return colonia; }
    }

    /**
     * Carga el XML en memoria si no está cargado
     */
    private synchronized void loadCacheIfNeeded() {
        if (cacheLoaded) {
            return;
        }

        try {
            log.info("Cargando archivo XML de códigos postales: {}", XML_FILE);
            ClassPathResource resource = new ClassPathResource(XML_FILE);
            
            if (!resource.exists()) {
                log.warn("Archivo {} no encontrado en classpath", XML_FILE);
                cacheLoaded = true;
                return;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(inputStream);
                
                doc.getDocumentElement().normalize();
                NodeList nodeList = doc.getElementsByTagName("table");
                
                log.info("Procesando {} registros de código postal", nodeList.getLength());
                
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        
                        String codigo = getTextContent(element, "d_codigo");
                        String estado = getTextContent(element, "d_estado");
                        String municipio = getTextContent(element, "D_mnpio");
                        String colonia = getTextContent(element, "d_asenta");
                        
                        if (codigo != null && codigo.length() == 5) {
                            CodigoPostalData data = new CodigoPostalData(codigo, estado, municipio, colonia);
                            cache.computeIfAbsent(codigo, k -> new ArrayList<>()).add(data);
                        }
                    }
                }
                
                log.info("Cache de códigos postales cargado: {} códigos únicos", cache.size());
                cacheLoaded = true;
            }
        } catch (Exception e) {
            log.error("Error al cargar archivo XML de códigos postales", e);
            cacheLoaded = true; // Marcar como cargado para evitar reintentos infinitos
        }
    }

    /**
     * Obtiene el texto contenido de un elemento XML
     */
    private String getTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null) {
                return node.getTextContent().trim();
            }
        }
        return null;
    }

    /**
     * Busca datos de un código postal
     */
    public Optional<CodigoPostalResult> buscarPorCodigo(String codigoPostal) {
        if (codigoPostal == null || codigoPostal.length() != 5 || !codigoPostal.matches("\\d{5}")) {
            return Optional.empty();
        }

        loadCacheIfNeeded();
        
        List<CodigoPostalData> datos = cache.get(codigoPostal);
        if (datos == null || datos.isEmpty()) {
            return Optional.empty();
        }

        // El estado y municipio son iguales para todas las colonias del mismo CP
        String estado = datos.get(0).getEstado();
        String municipio = datos.get(0).getMunicipio();
        
        // Extraer colonias únicas y ordenadas
        List<String> colonias = datos.stream()
                .map(CodigoPostalData::getColonia)
                .filter(c -> c != null && !c.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        CodigoPostalResult result = new CodigoPostalResult(codigoPostal, estado, municipio, colonias);
        return Optional.of(result);
    }

    public static class CodigoPostalResult {
        private String codigoPostal;
        private String estado;
        private String municipio;
        private List<String> colonias;

        public CodigoPostalResult(String codigoPostal, String estado, String municipio, List<String> colonias) {
            this.codigoPostal = codigoPostal;
            this.estado = estado;
            this.municipio = municipio;
            this.colonias = colonias;
        }

        public String getCodigoPostal() { return codigoPostal; }
        public String getEstado() { return estado; }
        public String getMunicipio() { return municipio; }
        public List<String> getColonias() { return colonias; }
    }
}
