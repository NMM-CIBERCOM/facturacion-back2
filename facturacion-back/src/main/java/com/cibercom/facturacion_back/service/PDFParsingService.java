package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.model.FacturaInfo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PDFParsingService {

    private static final Logger logger = LoggerFactory.getLogger(PDFParsingService.class);

    private static final Pattern RFC_PATTERN = Pattern.compile("(?i)RFC\\s*:?\\s*([A-Z0-9]{12,13})");
    private static final Pattern CURP_PATTERN = Pattern.compile("(?i)CURP\\s*:?\\s*([A-Z0-9]{18})");
    private static final Pattern NOMBRE_PATTERN = Pattern.compile("(?i)Nombre\\s*:?\\s*([A-ZÁÉÍÓÚÑ\\s]+)");
    private static final Pattern PRIMER_AP_PATTERN = Pattern.compile("(?i)Primer\\s+apellido\\s*:?\\s*([A-ZÁÉÍÓÚÑ\\s]+)");
    private static final Pattern SEGUNDO_AP_PATTERN = Pattern.compile("(?i)Segundo\\s+apellido\\s*:?\\s*([A-ZÁÉÍÓÚÑ\\s]+)");

    private static final Pattern TIPO_VIALIDAD_PATTERN = Pattern.compile("(?i)Tipo\\s+de\\s+vialidad\\s*:?\\s*([A-ZÁÉÍÓÚÑ\\s]+)");
    private static final Pattern CALLE_PATTERN = Pattern.compile("(?i)(Nombre\\s+de\\s+vialidad|Calle)\\s*:?\\s*([A-Z0-9ÁÉÍÓÚÑ\\s\\.\\-]+)");
    private static final Pattern NUM_EXT_PATTERN = Pattern.compile("(?i)Número\\s+exterior\\s*:?\\s*([A-Z0-9\\-]+)");
    private static final Pattern NUM_INT_PATTERN = Pattern.compile("(?i)Número\\s+interior\\s*:?\\s*([A-Z0-9\\-]+)");
    private static final Pattern COLONIA_PATTERN = Pattern.compile("(?i)Colonia\\s*:?\\s*([A-Z0-9ÁÉÍÓÚÑ\\s\\.\\-]+)");
    private static final Pattern LOCALIDAD_PATTERN = Pattern.compile("(?i)Localidad\\s*:?\\s*([A-Z0-9ÁÉÍÓÚÑ\\s\\.\\-]+)");
    private static final Pattern MUNICIPIO_PATTERN = Pattern.compile("(?i)Municipio.*?\\s*:?\\s*([A-Z0-9ÁÉÍÓÚÑ\\s\\.\\-]+)");
    private static final Pattern ENTIDAD_PATTERN = Pattern.compile("(?i)Entidad\\s+federativa\\s*:?\\s*([A-ZÁÉÍÓÚÑ\\s]+)");
    private static final Pattern ENTRE_CALLE_PATTERN = Pattern.compile("(?i)Entre\\s+la\\s+calle\\s*:?\\s*([A-Z0-9ÁÉÍÓÚÑ\\s\\.\\-]+)");
    private static final Pattern Y_CALLE_PATTERN = Pattern.compile("(?i)Y\\s+la\\s+calle\\s*:?\\s*([A-Z0-9ÁÉÍÓÚÑ\\s\\.\\-]+)");
    private static final Pattern CP_PATTERN = Pattern.compile("(?i)(Código\\s+postal|C\\.P\\.)\\s*:?\\s*(\\d{5})");

    private static final Pattern REGIMEN_LINE_PATTERN = Pattern.compile("(?i)(Régimen(es)?\\s+fiscal(es)?|Regímenes\\s+Fiscales)\\s*:?\\s*(.+)");

    public FacturaInfo parsearPDF(String rutaArchivo) throws Exception {
        logger.info("Parseando PDF: {}", rutaArchivo);
        try (PDDocument doc = PDDocument.load(new File(rutaArchivo))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            return extraerInfoDesdeTexto(text);
        }
    }

    public List<FacturaInfo> parsearPDFs(List<String> rutas) throws Exception {
        List<FacturaInfo> list = new ArrayList<>();
        for (String ruta : rutas) {
            try {
                list.add(parsearPDF(ruta));
            } catch (Exception e) {
                logger.warn("No se pudo parsear PDF {}: {}", ruta, e.getMessage());
            }
        }
        return list;
    }

    public void guardarFacturaInfo(FacturaInfo facturaInfo) {
        // Por ahora solo log; en el futuro podría persistirse
        logger.info("Guardar FacturaInfo: {}", facturaInfo);
    }

    private FacturaInfo extraerInfoDesdeTexto(String text) {
        FacturaInfo info = new FacturaInfo();

        info.setRfc(matchFirst(text, RFC_PATTERN));
        info.setCurp(matchFirst(text, CURP_PATTERN));

        // Nombre y apellidos
        String nombre = matchFirst(text, NOMBRE_PATTERN);
        info.setNombre(nombre);
        info.setPrimerApellido(matchFirst(text, PRIMER_AP_PATTERN));
        info.setSegundoApellido(matchFirst(text, SEGUNDO_AP_PATTERN));

        // Dirección
        info.setTipoVialidad(matchFirst(text, TIPO_VIALIDAD_PATTERN));
        info.setCalle(matchGroup(text, CALLE_PATTERN, 2));
        info.setNumExt(matchFirst(text, NUM_EXT_PATTERN));
        info.setNumInt(matchFirst(text, NUM_INT_PATTERN));
        info.setColonia(matchFirst(text, COLONIA_PATTERN));
        info.setLocalidad(matchFirst(text, LOCALIDAD_PATTERN));
        info.setMunicipio(matchFirst(text, MUNICIPIO_PATTERN));
        info.setEntidadFederativa(matchFirst(text, ENTIDAD_PATTERN));
        info.setEntreCalle(matchFirst(text, ENTRE_CALLE_PATTERN));
        info.setYCalle(matchFirst(text, Y_CALLE_PATTERN));
        info.setCp(matchGroup(text, CP_PATTERN, 2));

        // Regímenes fiscales: buscar líneas que contengan la palabra clave y también intentar parseo por viñetas
        List<String> regimenes = new ArrayList<>();
        Matcher rl = REGIMEN_LINE_PATTERN.matcher(text);
        while (rl.find()) {
            String raw = rl.group(4);
            if (raw != null) {
                // Split por separadores comunes
                for (String part : raw.split("[,;\\n]\\s*")) {
                    String p = part.trim();
                    if (!p.isBlank() && p.length() > 2) {
                        regimenes.add(p);
                    }
                }
            }
        }
        // También intentar detectar líneas con viñetas o guiones
        for (String line : text.split("\\n")) {
            String l = line.trim();
            if (l.startsWith("-") || l.startsWith("•")) {
                String p = l.replaceFirst("^[•-]\\s*", "").trim();
                if (!p.isBlank()) regimenes.add(p);
            }
        }
        // De-duplicar
        List<String> dedup = new ArrayList<>();
        for (String r : regimenes) {
            if (dedup.stream().noneMatch(x -> x.equalsIgnoreCase(r))) {
                dedup.add(r);
            }
        }
        info.setRegimenesFiscales(dedup);

        // Fecha actualización
        info.setFechaUltimaActualizacion(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Normalizaciones básicas: campos obligatorios con fallback para CDP
        if (info.getNombre() == null || info.getNombre().isBlank()) {
            // Algunos formatos ponen el nombre completo en un solo campo sin apellidos separados
            // Intentar extraer de una etiqueta común "Nombre del contribuyente"
            Pattern altNombre = Pattern.compile("(?i)Nombre\\s+del\\s+contribuyente\\s*:?\\s*([A-ZÁÉÍÓÚÑ\\s]+)");
            info.setNombre(matchFirst(text, altNombre));
        }
        
        // Fallbacks para campos obligatorios del CDP
        if (info.getNombre() == null || info.getNombre().isBlank()) {
            // Si aún no tiene nombre, usar "N/A" como último recurso
            info.setNombre("N/A");
        }
        if (info.getCalle() == null || info.getCalle().isBlank()) {
            info.setCalle("N/A");
        }
        if (info.getNumExt() == null || info.getNumExt().isBlank()) {
            info.setNumExt("N/A");
        }
        if (info.getColonia() == null || info.getColonia().isBlank()) {
            info.setColonia("N/A");
        }
        if (info.getMunicipio() == null || info.getMunicipio().isBlank()) {
            info.setMunicipio("N/A");
        }
        if (info.getEntidadFederativa() == null || info.getEntidadFederativa().isBlank()) {
            info.setEntidadFederativa("N/A");
        }
        if (info.getCp() == null || info.getCp().isBlank()) {
            info.setCp("00000");
        }

        return info;
    }

    private String matchFirst(String text, Pattern p) {
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String matchGroup(String text, Pattern p, int group) {
        Matcher m = p.matcher(text);
        if (m.find() && m.groupCount() >= group) {
            String g = m.group(group);
            return g != null ? g.trim() : null;
        }
        return null;
    }
}