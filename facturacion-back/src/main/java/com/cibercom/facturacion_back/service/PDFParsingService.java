package com.cibercom.facturacion_back.service;

// import com.cibercom.facturacion_back.dto.RegimenFiscalDTO;
import com.cibercom.facturacion_back.model.FacturaInfo;
import com.cibercom.facturacion_back.model.RegimenFiscal;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para parsear archivos PDF de constancias fiscales del SAT
 * Extrae información de personas físicas y morales
 */
@Service
public class PDFParsingService {

    private static final Logger log = LoggerFactory.getLogger(PDFParsingService.class);

    // Patrones de regex para extraer la información deseada
    private static final Pattern RFC_PATTERN = Pattern.compile("RFC:\\s+([A-Z&0-9]{12,13})");

    // Etiquetas y patrones para datos de domicilio
    private static final Pattern CODIGO_POSTAL_PATTERN = Pattern.compile("(?<=Código Postal:)\\s*(\\d{5})");
    private static final Pattern NOMBRE_VIALIDAD_PATTERN = Pattern.compile("(?<=Nombre de Vialidad:)\\s*(.+?)(?=\\s*Número Exterior|\\n|$)");
    private static final Pattern NUMERO_EXTERIOR_PATTERN = Pattern.compile("(?<=Número Exterior:)\\s+([^\\s]+)");
    private static final Pattern NUMERO_INTERIOR_PATTERN = Pattern.compile("(?<=Número Interior:)\\s*([^\\n]*?)(?=\\s*Nombre de la Colonia:|$)");
    private static final Pattern NOMBRE_COLONIA_PATTERN = Pattern.compile("(?<=Nombre de la Colonia:)\\s+(.+?)(?=\\s*Nombre de la Localidad|\\s*Nombre del Municipio|\\n|$)");
    private static final Pattern NOMBRE_MUNICIPIO_PATTERN = Pattern.compile("(?<=Nombre del Municipio o Demarcación Territorial:)\\s+(.+?)(?=\\s*Nombre de la Entidad Federativa|\\n|$)");
    private static final Pattern NOMBRE_ENTIDAD_PATTERN = Pattern.compile("(?<=Nombre de la Entidad Federativa:)\\s+(.+?)(?=\\s*Entre Calle|\\s*Y Calle|\\n|$)");
    private static final Pattern ENTRE_CALLE_PATTERN = Pattern.compile(
            "(?<=Entre Calle:)\\s*([^\\n]*?)(?=\\s*Y Calle|\\s*Página \\[\\d+\\] de \\[\\d+\\]|\\n|$)"
    );
    private static final Pattern Y_CALLE_PATTERN = Pattern.compile("(?<=Y Calle:)\\s*(.+?)(?=\\s*Correo Electrónico|\\s*Actividades Económicas|\\s*Página \\[\\d+\\] de \\[\\d+\\]|\\n|$)");

    // Patrón para regímenes fiscales y fechas
    private static final Pattern REGIMENES_FISCALES_PATTERN = Pattern.compile(
            "([\\w\\sáéíóúÁÉÍÓÚñÑ]+)\\s+(\\d{2}/\\d{2}/\\d{4})(?:\\s+(\\d{2}/\\d{2}/\\d{4}))?"
    );

    // Suponiendo que "Regímenes:" sea siempre el inicio y "Obligaciones:" el fin de la sección de interés
    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "(Regímenes:\\s*Régimen Fecha Inicio Fecha Fin[\\s\\S]*?)(?=Obligaciones:)"
    );

    /**
     * Método para procesar PDFs dependiendo del tipo de contribuyente
     */
    public FacturaInfo parsearPDF(String rutaArchivo) {
        FacturaInfo facturaInfo = new FacturaInfo();
        try (PDDocument documento = PDDocument.load(new File(rutaArchivo))) {
            PDFTextStripper stripper = new PDFTextStripper();

            String textoExtraido = stripper.getText(documento);
            log.debug("Texto extraído del PDF:\n{}", textoExtraido);

            String rfc = extractValue(textoExtraido, RFC_PATTERN, "RFC");
            facturaInfo.setRfc(rfc);

            // Determinar el tipo de contribuyente en base al RFC extraído
            if (rfc.length() == 12) {
                // Persona Moral
                parsearPersonaMoral(textoExtraido, facturaInfo);
            } else if (rfc.length() == 13) {
                // Persona Física
                parsearPersonaFisica(textoExtraido, facturaInfo);
            } else {
                // Manejar el caso en que el RFC no tenga una longitud válida
                log.warn("RFC extraído no tiene una longitud válida: {}", rfc);
            }

            // Extraer datos del domicilio comunes a ambos tipos
            extractDatosDomicilio(textoExtraido, facturaInfo);

            // Extraer regímenes fiscales y filtrar por fecha de fin
            extractRegimenesFiscales(textoExtraido, facturaInfo);
        } catch (IOException e) {
            log.error("Error al cargar o procesar el documento PDF", e);
        }
        return facturaInfo;
    }

    private void parsearPersonaFisica(String texto, FacturaInfo facturaInfo) {
        facturaInfo.setRfc(extractValue(texto, Pattern.compile("RFC:\\s+([A-Z&0-9]{13})"), "RFC"));
        facturaInfo.setCurp(extractValue(texto, Pattern.compile("CURP:\\s+([A-Z0-9]+)"), "CURP"));
        facturaInfo.setNombre(extractValue(texto, Pattern.compile("Nombre\\s\\(s\\):\\s+([\\p{L} .]+)\\s+Primer Apellido:"), "Nombre"));
        facturaInfo.setPrimerApellido(extractValue(texto, Pattern.compile("Primer Apellido:\\s+([\\p{L}]+)"), "Primer Apellido"));
        facturaInfo.setSegundoApellido(extractValue(texto, Pattern.compile("Segundo Apellido:\\s+([\\p{L}]+)"), "Segundo Apellido"));
    }

    private void parsearPersonaMoral(String texto, FacturaInfo facturaInfo) {
        facturaInfo.setRfc(extractValue(texto, Pattern.compile("RFC:\\s+([A-Z&0-9]{12})"), "RFC"));
        facturaInfo.setNombre(extractValue(texto, Pattern.compile("Denominación/Razón Social:\\s+(.*)"), "Denominación/Razón Social"));
    }

    private void extractDatosDomicilio(String texto, FacturaInfo facturaInfo) {
        log.debug("Starting address data extraction...");

        facturaInfo.setCp(extractValue(texto, CODIGO_POSTAL_PATTERN, "Código Postal"));
        facturaInfo.setCalle(extractValue(texto, NOMBRE_VIALIDAD_PATTERN, "Nombre de Vialidad"));
        facturaInfo.setNumExt(extractValue(texto, NUMERO_EXTERIOR_PATTERN, "Número Exterior"));
        facturaInfo.setNumInt(extractValue(texto, NUMERO_INTERIOR_PATTERN, "Número Interior"));
        facturaInfo.setColonia(extractValue(texto, NOMBRE_COLONIA_PATTERN, "Nombre de la Colonia"));
        facturaInfo.setMunicipio(extractValue(texto, NOMBRE_MUNICIPIO_PATTERN, "Nombre del Municipio o Demarcación Territorial"));
        facturaInfo.setEntidadFederativa(extractValue(texto, NOMBRE_ENTIDAD_PATTERN, "Nombre de la Entidad Federativa"));
        facturaInfo.setEntreCalle(extractValue(texto, ENTRE_CALLE_PATTERN, "Entre Calle"));
        facturaInfo.setYCalle(extractValue(texto, Y_CALLE_PATTERN, "Y Calle"));
    }

    private String extractValue(String text, Pattern pattern, String fieldName) {
        log.debug("Extracting value for field: {}", fieldName);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String matchedValue = matcher.group(1).trim();
            if (matchedValue.isEmpty()) {
                log.debug("Field {} is present but empty.", fieldName);
            } else {
                log.debug("Found value for {}: {}", fieldName, matchedValue);
            }
            return matchedValue;
        } else {
            log.warn("No value found for {}", fieldName);
            return "";
        }
    }

    /**
     * Método para procesar múltiples PDFs
     */
    public List<FacturaInfo> parsearPDFs(List<String> rutasArchivos) {
        List<FacturaInfo> facturas = new ArrayList<>();

        for (String rutaArchivo : rutasArchivos) {
            FacturaInfo facturaInfo = parsearPDF(rutaArchivo);
            facturas.add(facturaInfo);
        }

        return facturas;
    }

    /**
     * Guarda la información de la factura
     * Por ahora solo registra en logs, pero aquí se puede agregar lógica de persistencia
     */
    public void guardarFacturaInfo(FacturaInfo facturaInfo) {
        // Aquí iría la lógica para guardar la información en la base de datos
        // Por ahora, solo imprimimos la información en consola
        facturaInfo.setFechaUltimaActualizacion(LocalDateTime.now());

        log.info("Factura Info: {}", facturaInfo);
        for (String regimen : facturaInfo.getRegimenesFiscales()) {
            log.info("Regimen: {}", regimen);
        }
    }

    private void extractRegimenesFiscales(String texto, FacturaInfo facturaInfo) {
        List<String> regimenesEncontrados = new ArrayList<>();

        Matcher sectionMatcher = SECTION_PATTERN.matcher(texto);
        if (sectionMatcher.find()) {
            String regimenesText = sectionMatcher.group(1);
            // Limpieza previa para asegurar que el encabezado no sea procesado
            regimenesText = regimenesText.replaceFirst("^Regímenes:\\s*Régimen Fecha Inicio Fecha Fin\\s*", "");

            Matcher matcher = REGIMENES_FISCALES_PATTERN.matcher(regimenesText);

            while (matcher.find()) {
                String nombreRegimen = matcher.group(1).trim();

                RegimenFiscal regimenEnum = findRegimenFiscalByName(nombreRegimen);
                if (regimenEnum != null) {
                    String regimenString = regimenEnum.getClave() + " - " + regimenEnum.getDescripcion();
                    regimenesEncontrados.add(regimenString);
                    log.debug("Regimen fiscal coincidente agregado: {}", regimenEnum.getDescripcion());
                } else {
                    log.warn("No se encontró una coincidencia de enum para el régimen extraído: {}", nombreRegimen);
                }
            }

        } else {
            log.warn("No se pudo encontrar la sección de regímenes fiscales en el texto.");
        }

        facturaInfo.setRegimenesFiscales(regimenesEncontrados);
    }

    private RegimenFiscal findRegimenFiscalByName(String nombreRegimen) {
        for (RegimenFiscal regimen : RegimenFiscal.values()) {
            if (regimen.getDescripcion().equalsIgnoreCase(nombreRegimen)) {
                return regimen;
            }
        }
        return null;
    }
}

