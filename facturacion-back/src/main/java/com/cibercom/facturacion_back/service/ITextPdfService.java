package com.cibercom.facturacion_back.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.List;

@Service
@SuppressWarnings({"unused", "unchecked"})
public class ITextPdfService {
    
    private static final Logger logger = LoggerFactory.getLogger(ITextPdfService.class);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    public byte[] generarPdf(Map<String, Object> facturaData) throws IOException {
        // Llamamos al método existente con un logoConfig vacío
        return generarPdfConLogo(facturaData, Map.of());
    }
    
    public byte[] generarPdfConLogo(Map<String, Object> facturaData, Map<String, Object> logoConfig) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            // Crear el documento PDF
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Configurar márgenes más compactos
            document.setMargins(20, 20, 20, 20);
            
            boolean esComplementoPago = esComplementoPago(facturaData);
            boolean esNomina = tieneNomina(facturaData);
            boolean esRetencion = esRetencion(facturaData);
            boolean esCartaPorte = esCartaPorte(facturaData);
            
            logger.info("Detección de tipo de documento - ComplementoPago: {}, Nomina: {}, Retencion: {}, CartaPorte: {}, tipoComprobante: {}", 
                esComplementoPago, esNomina, esRetencion, esCartaPorte, getString(facturaData, "tipoComprobante", "N/A"));

            // Agregar encabezado moderno
            agregarEncabezadoModerno(document, facturaData, logoConfig);
            
            // Agregar información de empresa y cliente
            agregarInformacionEmpresaCliente(document, facturaData, logoConfig);
            
            if (esComplementoPago) {
                agregarSeccionComplementoPago(document, facturaData, logoConfig);
            } else if (esNomina) {
                agregarSeccionNomina(document, facturaData, logoConfig);
            } else if (esRetencion) {
                agregarSeccionRetencion(document, facturaData, logoConfig);
            } else if (esCartaPorte) {
                logger.info("Usando diseño específico de Carta Porte (agregarSeccionCartaPorte)");
                agregarSeccionCartaPorte(document, facturaData, logoConfig);
            } else {
                logger.info("Usando diseño genérico de conceptos (agregarConceptosModerno)");
                // Agregar conceptos con diseño moderno
                agregarConceptosModerno(document, facturaData, logoConfig);
            }
            
            // Agregar complemento Carta Porte solo si no es complemento de pago y no es el comprobante principal de Carta Porte
            if (!esComplementoPago && !esCartaPorte) {
                agregarComplementoCartaPorte(document, facturaData, logoConfig);
            }
            
            // Totales: nómina, retención, carta porte, complemento de pago o factura clásica
            if (esNomina) {
                agregarTotalesNomina(document, facturaData, logoConfig);
            } else if (esComplementoPago) {
                agregarTotalesComplementoPago(document, facturaData, logoConfig);
            } else if (esRetencion) {
                agregarTotalesRetencion(document, facturaData, logoConfig);
            } else {
                agregarTotalesModerno(document, facturaData, logoConfig);
            }
            
            // Agregar información fiscal moderna
            agregarInformacionFiscalModerna(document, facturaData, logoConfig);
            
            // Cerrar el documento
            document.close();
            
            logger.info("PDF generado exitosamente con iText. Tamaño: {} bytes", baos.size());
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Error generando PDF con iText: ", e);
            throw new IOException("Error generando PDF: " + e.getMessage(), e);
        }
    }
    
    private void agregarLogoCompactoHeader(Cell logoCell) {
        try {
            // Intentar cargar el logo desde resources
            java.io.InputStream logoStream = getClass().getResourceAsStream("/static/images/logo.png");
            
            if (logoStream != null) {
                try {
                    ImageData imageData = ImageDataFactory.create(logoStream.readAllBytes());
                    Image logo = new Image(imageData);
                    
                    // Redimensionar el logo para el encabezado compacto
                    logo.setWidth(50);
                    logo.setHeight(50);
                    logo.setAutoScale(true);
                    
                    logoCell.add(logo);
                    logger.info("Logo agregado exitosamente al encabezado compacto");
                    return;
                    
                } catch (Exception e) {
                    logger.error("Error al procesar la imagen del logo: ", e);
                } finally {
                    logoStream.close();
                }
            }
            
            // Intentar cargar logo SVG
            java.io.InputStream logoSvgStream = getClass().getResourceAsStream("/static/images/logo.svg");
            if (logoSvgStream != null) {
                try {
                    // Para SVG, crear un placeholder con mejor diseño
                    Paragraph logoSvg = new Paragraph("🏢")
                        .setFontSize(30)
                        .setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(0);
                    logoCell.add(logoSvg);
                    
                    Paragraph logoText = new Paragraph("LOGO")
                        .setFontSize(8)
                        .setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(0);
                    logoCell.add(logoText);
                    
                    logger.info("Logo SVG placeholder agregado");
                    return;
                    
                } catch (Exception e) {
                    logger.error("Error al procesar logo SVG: ", e);
                } finally {
                    logoSvgStream.close();
                }
            }
            
            // Si no se puede cargar ningún logo, mostrar placeholder elegante
            Paragraph logoPlaceholder = new Paragraph("🏢")
                .setFontSize(25)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0);
            logoCell.add(logoPlaceholder);
            
            Paragraph logoText = new Paragraph("EMPRESA")
                .setFontSize(7)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(0);
            logoCell.add(logoText);
            
        } catch (Exception e) {
            logger.error("Error al agregar logo compacto: ", e);
            // En caso de error, agregar mensaje de error
            Paragraph logoError = new Paragraph("❌")
                .setFontSize(20)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER);
            logoCell.add(logoError);
        }
    }
    
    private void agregarEncabezadoModerno(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            // Agregar encabezado con fondo
            agregarEncabezadoConFondo(document, facturaData, logoConfig);
        } catch (Exception e) {
            logger.error("Error agregando encabezado moderno: ", e);
        }
    }
    
    private void agregarEncabezadoConFondo(Document document, Map<String, Object> factura, Map<String, Object> logoConfig) {
        try {
            // Contenedor del encabezado con fondo azul
            Table headerContainer = new Table(2);
            headerContainer.setWidth(UnitValue.createPercentValue(100));
            DeviceRgb headerColor = extraerColorPrimario(logoConfig);
            headerContainer.setBackgroundColor(headerColor);
            headerContainer.setPadding(8);
            try {
                Object cc = logoConfig != null ? logoConfig.get("customColors") : null;
                if (cc instanceof java.util.Map) {
                    Object primary = ((java.util.Map<?, ?>) cc).get("primary");
                    logger.info("Color primario aplicado al encabezado: {}", primary);
                } else {
                    logger.info("LogoConfig sin customColors, usando color por defecto");
                }
            } catch (Exception logEx) {
                logger.warn("No se pudo registrar color primario del encabezado: {}", logEx.getMessage());
            }
            
            // Celda del título
            Cell tituloCell = new Cell()
                .setBorder(null)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .setWidth(UnitValue.createPercentValue(70));
            
            String tituloTexto;
            if (tieneNomina(factura)) {
                tituloTexto = "RECIBO DE NÓMINA";
            } else {
                String tipoDocumento = getString(factura, "tipoDocumento", "").trim();
                String tipoComprobante = getString(factura, "tipoComprobante", "").toUpperCase();
                switch (tipoComprobante) {
                    case "I" -> tituloTexto = "COMPROBANTE DE INGRESO";
                    case "E" -> tituloTexto = "COMPROBANTE DE EGRESO";
                    case "T" -> tituloTexto = "COMPROBANTE DE TRASLADO";
                    case "R" -> tituloTexto = "COMPROBANTE DE RETENCIÓN DE PAGOS";
                    case "P" -> tituloTexto = "COMPLEMENTO DE PAGO";
                    default -> tituloTexto = "FACTURA ELECTRÓNICA";
                }
                if ("Refactura".equalsIgnoreCase(tipoDocumento)) {
                    tituloTexto = tituloTexto + " - REFACTURA";
                }
            }
            Paragraph titulo = new Paragraph(tituloTexto)
                .setBold()
                .setFontSize(16)
                .setFontColor(ColorConstants.WHITE)
                .setMarginBottom(0);
            tituloCell.add(titulo);
            
            // Información de serie, folio y fecha en una línea
            Paragraph infoFactura = new Paragraph()
                .add(new Text("Serie: " + getString(factura, "serie", "A") + "  ").setFontSize(9))
                .add(new Text("Folio: " + getString(factura, "folio", "001") + "  ").setFontSize(9))
                .add(new Text("Fecha: " + getString(factura, "fechaEmision", "2024-01-15")).setFontSize(9))
                .setFontColor(ColorConstants.WHITE)
                .setMarginTop(3);
            tituloCell.add(infoFactura);
            
            headerContainer.addCell(tituloCell);
            
            // Celda del logo
            Cell logoCell = new Cell()
                .setBorder(null)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setWidth(UnitValue.createPercentValue(30));
            
            // Usar el logo proporcionado en logoConfig (Base64 o URL)
            agregarLogoModerno(logoCell, logoConfig);
            headerContainer.addCell(logoCell);
            
            document.add(headerContainer);
            
        } catch (Exception e) {
            logger.error("Error al agregar encabezado con fondo: ", e);
        }
    }
    
    private void agregarLogoCompacto(Cell logoCell, Map<String, Object> logoConfig) {
        try {
            if (logoConfig != null && logoConfig.containsKey("logoPath")) {
                String logoPath = (String) logoConfig.get("logoPath");
                logger.info("Intentando cargar logo desde: {}", logoPath);
                
                Path path = Paths.get(logoPath);
                if (Files.exists(path)) {
                    byte[] logoBytes = Files.readAllBytes(path);
                    ImageData imageData = ImageDataFactory.create(logoBytes);
                    Image logo = new Image(imageData);
                    
                    // Logo más pequeño para diseño compacto
                    logo.setWidth(60);
                    logo.setHeight(60);
                    logo.setAutoScale(true);
                    
                    logoCell.add(logo);
                    logger.info("Logo compacto agregado exitosamente");
                } else {
                    // Placeholder más pequeño
                    Paragraph placeholder = new Paragraph("[LOGO]")
                        .setFontSize(8)
                        .setFontColor(new DeviceRgb(156, 163, 175))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBorder(new SolidBorder(new DeviceRgb(209, 213, 219), 1))
                        .setPadding(8)
                        .setWidth(60)
                        .setHeight(60);
                    logoCell.add(placeholder);
                }
            }
        } catch (Exception e) {
            logger.error("Error al cargar logo compacto: ", e);
            Paragraph error = new Paragraph("[ERROR]")
                .setFontSize(8)
                .setFontColor(new DeviceRgb(239, 68, 68))
                .setTextAlignment(TextAlignment.CENTER);
            logoCell.add(error);
        }
    }
    
    private void agregarLogoModerno(Cell logoCell, Map<String, Object> logoConfig) {
        try {
            if (logoConfig != null) {
                byte[] logoBytes = null;
                
                // Intentar cargar logo desde Base64 primero
                if (logoConfig.containsKey("logoBase64")) {
                    String logoBase64 = (String) logoConfig.get("logoBase64");
                    if (logoBase64 != null && !logoBase64.isEmpty()) {
                        // Remover el prefijo data:image/...;base64, si existe
                        if (logoBase64.contains(",")) {
                            logoBase64 = logoBase64.split(",")[1];
                        }
                        
                        byte[] decodedBytes = Base64.getDecoder().decode(logoBase64);
                        
                        // Verificar si es SVG y convertir a PNG si es necesario
                        if (isSvg(decodedBytes)) {
                            logger.info("Detectado logo SVG, convirtiendo a PNG...");
                            logoBytes = convertSvgToPng(decodedBytes);
                            logger.info("SVG convertido a PNG exitosamente, tamaño: {} bytes", logoBytes.length);
                        } else {
                            logoBytes = decodedBytes;
                            logger.info("Logo cargado desde Base64, tamaño: {} bytes", logoBytes.length);
                        }
                    }
                }
                
                // Si no hay Base64, intentar cargar desde URL o classpath
                if (logoBytes == null && logoConfig.containsKey("logoUrl")) {
                    String logoUrl = (String) logoConfig.get("logoUrl");
                    try {
                        if (logoUrl != null && (logoUrl.startsWith("http://") || logoUrl.startsWith("https://"))) {
                            try (java.io.InputStream in = new java.net.URL(logoUrl).openStream()) {
                                logoBytes = in.readAllBytes();
                                logger.info("Logo cargado desde URL: {} ({} bytes)", logoUrl, logoBytes.length);
                            }
                        } else {
                            java.nio.file.Path logoPath = java.nio.file.Paths.get("src/main/resources/static" + logoUrl);
                            if (java.nio.file.Files.exists(logoPath)) {
                                logoBytes = java.nio.file.Files.readAllBytes(logoPath);
                                logger.info("Logo cargado desde archivo: {}", logoPath);
                            } else {
                                logger.warn("Logo no encontrado en: {}", logoPath);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("No se pudo cargar el logo desde '{}': {}", logoUrl, e.getMessage());
                    }
                }
                
                // Fallback: Intentar cargar desde classpath si aún no se cargó
                if (logoBytes == null) {
                    try {
                        java.io.InputStream logoStream = getClass().getClassLoader()
                                .getResourceAsStream("static/images/Logo Cibercom.png");
                        if (logoStream != null) {
                            logoBytes = logoStream.readAllBytes();
                            logoStream.close();
                            logger.info("Logo PNG cargado desde classpath (fallback): {} bytes", logoBytes.length);
                        } else {
                            // Intentar SVG como último recurso
                            logoStream = getClass().getClassLoader()
                                    .getResourceAsStream("static/images/cibercom-logo.svg");
                            if (logoStream != null) {
                                byte[] svgBytes = logoStream.readAllBytes();
                                logoStream.close();
                                if (isSvg(svgBytes)) {
                                    logger.info("Logo SVG encontrado en classpath, convirtiendo a PNG...");
                                    logoBytes = convertSvgToPng(svgBytes);
                                    logger.info("SVG convertido a PNG exitosamente: {} bytes", logoBytes.length);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("No se pudo cargar logo desde classpath (fallback): {}", e.getMessage());
                    }
                }
                
                // Si se cargó el logo, agregarlo a la celda con diseño moderno
                if (logoBytes != null) {
                    ImageData imageData = ImageDataFactory.create(logoBytes);
                    Image logo = new Image(imageData);
                    
                    // Configuración moderna del logo con mejor simetría
                    logo.setWidth(100);
                    logo.setHeight(100);
                    logo.setAutoScale(true);
                    
                    logoCell.add(logo);
                    logger.info("Logo agregado exitosamente a la celda");
                    return;
                }
            }
            
            // Placeholder moderno si no hay logo
            logger.warn("No se pudo cargar el logo, agregando placeholder moderno");
            Paragraph logoPlaceholder = new Paragraph("LOGO\nEMPRESA")
                .setFontSize(14)
                .setBold()
                .setFontColor(new DeviceRgb(100, 116, 139))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(20)
                .setBackgroundColor(new DeviceRgb(248, 250, 252));
            logoCell.add(logoPlaceholder);
            
        } catch (Exception e) {
            logger.error("Error agregando logo moderno: ", e);
            // Mensaje de error con diseño moderno
            Paragraph logoError = new Paragraph("ERROR\nLOGO")
                .setFontSize(12)
                .setBold()
                .setFontColor(new DeviceRgb(239, 68, 68))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(20)
                .setBackgroundColor(new DeviceRgb(254, 242, 242));
            logoCell.add(logoError);
        }
    }
    
    private void agregarLineaSeparadora(Document document, DeviceRgb color) {
        Table lineTable = new Table(1);
        lineTable.setWidth(UnitValue.createPercentValue(100));
        lineTable.setMarginTop(15);
        lineTable.setMarginBottom(15);
        
        Cell lineCell = new Cell()
            .setHeight(3)
            .setBackgroundColor(color)
            .setBorder(null);
        
        lineTable.addCell(lineCell);
        document.add(lineTable);
    }
    
    private void agregarInformacionEmpresaCliente(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);
            // Contenedor compacto para datos en una sola línea
            Table datosContainer = new Table(2);
            datosContainer.setWidth(UnitValue.createPercentValue(100));
            datosContainer.setMarginBottom(8);
            
            // Datos del emisor (lado izquierdo)
            Cell emisorCell = new Cell()
                .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1))
                .setBackgroundColor(new DeviceRgb(248, 250, 252))
                .setPadding(8);
            
            Paragraph emisorTitulo = new Paragraph("EMISOR")
                .setBold()
                .setFontSize(10)
                .setFontColor(primaryColor)
                .setMarginBottom(4);
            emisorCell.add(emisorTitulo);
            
            agregarDatosCompactos(emisorCell, facturaData, "Emisor");
            datosContainer.addCell(emisorCell);
            
            // Datos del receptor (lado derecho)
            Cell receptorCell = new Cell()
                .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1))
                .setBackgroundColor(new DeviceRgb(252, 252, 254))
                .setPadding(8);
            
            Paragraph receptorTitulo = new Paragraph("RECEPTOR")
                .setBold()
                .setFontSize(10)
                .setFontColor(primaryColor)
                .setMarginBottom(4);
            receptorCell.add(receptorTitulo);
            
            agregarDatosCompactos(receptorCell, facturaData, "Receptor");
            datosContainer.addCell(receptorCell);
            
            document.add(datosContainer);
            
        } catch (Exception e) {
            logger.error("Error agregando información de empresa y cliente: ", e);
        }
    }
    
    private void agregarDatosCompactos(Cell cell, Map<String, Object> facturaData, String tipo) {
        if ("Emisor".equals(tipo)) {
            // Datos del emisor
            Paragraph nombreEmisor = new Paragraph(getString(facturaData, "nombreEmisor", "Empresa Ejemplo"))
                .setBold()
                .setFontSize(9)
                .setMarginBottom(2);
            cell.add(nombreEmisor);
            
            Paragraph rfcEmisor = new Paragraph("RFC: " + getString(facturaData, "rfcEmisor", "EEM123456789"))
                .setFontSize(8)
                .setFontColor(new DeviceRgb(100, 116, 139))
                .setMarginBottom(2);
            cell.add(rfcEmisor);
            
            Paragraph lugarExpedicion = new Paragraph("Lugar: " + getString(facturaData, "lugarExpedicion", "N/A"))
                .setFontSize(8)
                .setFontColor(new DeviceRgb(100, 116, 139));
            cell.add(lugarExpedicion);
        } else {
            // Datos del receptor
            Paragraph nombreReceptor = new Paragraph(getString(facturaData, "nombreReceptor", "Cliente Ejemplo"))
                .setBold()
                .setFontSize(9)
                .setMarginBottom(2);
            cell.add(nombreReceptor);
            
            Paragraph rfcReceptor = new Paragraph("RFC: " + getString(facturaData, "rfcReceptor", "XEXX010101000"))
                .setFontSize(8)
                .setFontColor(new DeviceRgb(100, 116, 139))
                .setMarginBottom(2);
            cell.add(rfcReceptor);
            
            Paragraph metodoPago = new Paragraph("Método: " + getString(facturaData, "metodoPago", "PUE"))
                .setFontSize(8)
                .setFontColor(new DeviceRgb(100, 116, 139));
            cell.add(metodoPago);
        }
    }
    

    
    private void agregarSeccionEmisor(Table table, Map<String, Object> facturaData) {
        // Los datos ya vienen directamente en facturaData, no anidados
        Map<String, Object> factura = facturaData;
        
        Cell emisorHeader = new Cell(1, 2)
            .add(new Paragraph("DATOS DEL EMISOR").setBold())
            .setBackgroundColor(new DeviceRgb(240, 240, 240));
        table.addCell(emisorHeader);
        
        table.addCell(new Cell().add(new Paragraph("Nombre:")));
        table.addCell(new Cell().add(new Paragraph(getString(factura, "nombreEmisor", "N/A"))));
        
        table.addCell(new Cell().add(new Paragraph("RFC:")));
        table.addCell(new Cell().add(new Paragraph(getString(factura, "rfcEmisor", "N/A"))));
    }
    
    private void agregarSeccionReceptor(Table table, Map<String, Object> facturaData) {
        // Los datos ya vienen directamente en facturaData, no anidados
        Map<String, Object> factura = facturaData;
        
        Cell receptorHeader = new Cell(1, 2)
            .add(new Paragraph("DATOS DEL RECEPTOR").setBold())
            .setBackgroundColor(new DeviceRgb(240, 240, 240));
        table.addCell(receptorHeader);
        
        table.addCell(new Cell().add(new Paragraph("Nombre:")));
        table.addCell(new Cell().add(new Paragraph(getString(factura, "nombreReceptor", "N/A"))));
        
        table.addCell(new Cell().add(new Paragraph("RFC:")));
        table.addCell(new Cell().add(new Paragraph(getString(factura, "rfcReceptor", "N/A"))));
        
        table.addCell(new Cell().add(new Paragraph("UUID:")));
        table.addCell(new Cell().add(new Paragraph(getString(factura, "uuid", "N/A"))));
        
        table.addCell(new Cell().add(new Paragraph("Serie:")));
        table.addCell(new Cell().add(new Paragraph(getString(factura, "serie", "N/A"))));
        
        table.addCell(new Cell().add(new Paragraph("Folio:")));
        table.addCell(new Cell().add(new Paragraph(getString(factura, "folio", "N/A"))));
    }
    
    private void agregarConceptosModerno(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);
            // Título de la sección más compacto
            Paragraph conceptosTitulo = new Paragraph("CONCEPTOS")
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setMarginBottom(6)
                .setMarginTop(8)
                .setCharacterSpacing(0.3f);
            document.add(conceptosTitulo);
            
            // Si es refactura, mostrar UUID CFDI relacionado
            String uuidCfdiRelacionado = getString(facturaData, "uuidCfdiRelacionado", "").trim();
            if (!uuidCfdiRelacionado.isEmpty()) {
                Paragraph uuidRelacionadoP = new Paragraph("UUID CFDI relacionado: " + uuidCfdiRelacionado)
                    .setFontSize(8)
                    .setFontColor(new DeviceRgb(75, 85, 99))
                    .setMarginBottom(6);
                document.add(uuidRelacionadoP);
            }
            
            // Crear tabla de conceptos con columnas adicionales para campos del catálogo
            // Columnas: Cant, Unidad, Descripción, ClaveProdServ, P.Unit, Importe, IVA, TasaIVA
            Table conceptosTable = new Table(new float[]{0.6f, 0.7f, 2.5f, 1.2f, 0.9f, 0.9f, 0.7f, 0.6f});
            conceptosTable.setWidth(UnitValue.createPercentValue(100));
            
            // Encabezados de la tabla con campos del catálogo
            String[] headers = {"Cant", "Unidad", "Descripción", "ClaveProdServ", "P.Unit", "Importe", "IVA", "Tasa"};
            for (String header : headers) {
                Cell headerCell = new Cell()
                    .add(new Paragraph(header).setBold().setFontSize(7))
                    .setBackgroundColor(primaryColor)
                    .setFontColor(ColorConstants.WHITE)
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER);
                conceptosTable.addHeaderCell(headerCell);
            }
            
            // Obtener conceptos.
            // Si no vienen en facturaData, construir uno dinámico con los totales para que
            // el PDF refleje los importes reales del ticket/factura y no el ejemplo.
            java.util.List<java.util.Map<String, Object>> conceptos = getListValue(facturaData, "conceptos");
            if (conceptos == null || conceptos.isEmpty()) {
                conceptos = new java.util.ArrayList<>();
                java.util.Map<String, Object> conceptoUnico = new java.util.HashMap<>();
                conceptoUnico.put("cantidad", "1");
                // Descripción genérica si no se proporcionó: se puede ajustar según el flujo
                conceptoUnico.put("descripcion", getString(facturaData, "descripcionConcepto", "Venta"));
                String subtotalStr = getString(facturaData, "subtotal", "0.00");
                String ivaStr = getString(facturaData, "iva", "0.00");
                // Usamos el subtotal como precio unitario e importe para un concepto único
                conceptoUnico.put("valorUnitario", subtotalStr);
                conceptoUnico.put("importe", subtotalStr);
                conceptoUnico.put("iva", ivaStr);
                conceptos.add(conceptoUnico);
            }
            
            // Agregar filas de conceptos con campos del catálogo
            for (java.util.Map<String, Object> concepto : conceptos) {
                // Cantidad - manejar BigDecimal
                String cantidadStr = "";
                Object cantidadObj = concepto.get("cantidad");
                if (cantidadObj != null) {
                    if (cantidadObj instanceof BigDecimal) {
                        cantidadStr = ((BigDecimal) cantidadObj).toPlainString();
                    } else {
                        cantidadStr = cantidadObj.toString();
                    }
                }
                if (cantidadStr.isEmpty()) {
                    cantidadStr = "1";
                }
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph(cantidadStr).setFontSize(7))
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER));
                
                // Unidad (del catálogo) - Priorizar claveUnidad sobre unidad descriptiva
                String claveUnidad = getString(concepto, "claveUnidad", "");
                String unidad = getString(concepto, "unidad", "");
                
                // Si claveUnidad está disponible, usarla (es código SAT válido)
                // Si no, y unidad es un texto descriptivo como "Opcional", usar claveUnidad vacía o "-"
                String unidadAMostrar = "";
                if (!claveUnidad.isEmpty()) {
                    unidadAMostrar = claveUnidad;
                } else {
                    // Si unidad es un texto descriptivo común, no mostrarlo
                    String unidadUpper = unidad.toUpperCase().trim();
                    String[] textosDescriptivos = {"OPCIONAL", "SERVICIO", "PIEZA", "UNIDAD", "N/A", "NA", "-"};
                    boolean esDescriptivo = false;
                    for (String texto : textosDescriptivos) {
                        if (unidadUpper.equals(texto)) {
                            esDescriptivo = true;
                            break;
                        }
                    }
                    if (!esDescriptivo && !unidad.isEmpty()) {
                        unidadAMostrar = unidad;
                    }
                }
                
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph(unidadAMostrar.isEmpty() ? "-" : unidadAMostrar).setFontSize(7))
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER));
                
                // Descripción - diseño especial para retenciones
                String tipoComprobante = getString(facturaData, "tipoComprobante", "");
                String descripcion = getString(concepto, "descripcion", "Producto/Servicio");
                Cell descripcionCell = new Cell().setPadding(5);
                
                if ("R".equals(tipoComprobante) && descripcion.contains("\n")) {
                    // Diseño especial estructurado para retenciones
                    Div descripcionDiv = new Div();
                    String[] lineas = descripcion.split("\n");
                    boolean primeraLinea = true;
                    
                    for (String linea : lineas) {
                        String lineaTrim = linea.trim();
                        if (lineaTrim.isEmpty()) continue;
                        
                        if (primeraLinea) {
                            // Primera línea: Tipo de retención (título principal)
                            Paragraph p = new Paragraph(lineaTrim)
                                .setFontSize(7.5f)
                                .setBold()
                                .setFontColor(primaryColor)
                                .setMarginBottom(4);
                            descripcionDiv.add(p);
                            primeraLinea = false;
                        } else if (lineaTrim.startsWith("Concepto:")) {
                            // Línea de concepto
                            Paragraph p = new Paragraph(lineaTrim.replace("Concepto: ", ""))
                                .setFontSize(6.5f)
                                .setMarginBottom(3)
                                .setMarginLeft(5);
                            descripcionDiv.add(p);
                        } else if (lineaTrim.startsWith("Monto Base:") || lineaTrim.contains("| Gravado:") || lineaTrim.contains("| Exento:")) {
                            // Líneas de montos
                            Paragraph p = new Paragraph(lineaTrim)
                                .setFontSize(6)
                                .setMarginBottom(2)
                                .setFontColor(new DeviceRgb(50, 50, 50));
                            descripcionDiv.add(p);
                        } else if (lineaTrim.startsWith("Período:") || lineaTrim.startsWith("Fecha de Pago:")) {
                            // Líneas de fechas
                            Paragraph p = new Paragraph(lineaTrim)
                                .setFontSize(6)
                                .setMarginBottom(2)
                                .setFontColor(new DeviceRgb(50, 50, 50));
                            descripcionDiv.add(p);
                        } else if (lineaTrim.startsWith("Impuestos Retenidos:")) {
                            // Encabezado de impuestos retenidos
                            Paragraph p = new Paragraph(lineaTrim.replace("Impuestos Retenidos:", "Imp. Retenidos:"))
                                .setFontSize(6.5f)
                                .setBold()
                                .setMarginTop(3)
                                .setMarginBottom(2)
                                .setFontColor(new DeviceRgb(80, 80, 80));
                            descripcionDiv.add(p);
                        } else if (lineaTrim.startsWith("Detalle de Retenciones:")) {
                            // Encabezado de detalle
                            Paragraph p = new Paragraph("Detalle:")
                                .setFontSize(6.5f)
                                .setBold()
                                .setMarginTop(3)
                                .setMarginBottom(2)
                                .setFontColor(new DeviceRgb(80, 80, 80));
                            descripcionDiv.add(p);
                        } else if (lineaTrim.startsWith("  - ")) {
                            // Líneas de detalle (con indentación)
                            Paragraph p = new Paragraph("• " + lineaTrim.substring(4).trim())
                                .setFontSize(6)
                                .setMarginLeft(8)
                                .setMarginBottom(1)
                                .setFontColor(new DeviceRgb(60, 60, 60));
                            descripcionDiv.add(p);
                        } else if (lineaTrim.contains("| Total Retenido:")) {
                            // Línea de total retenido
                            Paragraph p = new Paragraph(lineaTrim)
                                .setFontSize(6)
                                .setMarginBottom(2)
                                .setBold()
                                .setFontColor(new DeviceRgb(40, 40, 40));
                            descripcionDiv.add(p);
                        } else {
                            // Otras líneas
                            Paragraph p = new Paragraph(lineaTrim)
                                .setFontSize(6)
                                .setMarginBottom(2);
                            descripcionDiv.add(p);
                        }
                    }
                    
                    descripcionCell.add(descripcionDiv);
                } else {
                    // Descripción normal para otros tipos de comprobantes
                    descripcionCell.add(new Paragraph(descripcion).setFontSize(7));
                }
                
                conceptosTable.addCell(descripcionCell);
                
                // ClaveProdServ (del catálogo)
                String claveProdServ = getString(concepto, "claveProdServ", "");
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph(claveProdServ.isEmpty() ? "-" : claveProdServ).setFontSize(7))
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER));
                
                // Precio unitario - manejar BigDecimal
                BigDecimal valorUnitarioBD = getBigDecimal(concepto, "valorUnitario", BigDecimal.ZERO);
                String valorUnitarioStr = String.format("%.2f", valorUnitarioBD);
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph("$" + valorUnitarioStr).setFontSize(7))
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.RIGHT));
                
                // Importe - manejar BigDecimal
                BigDecimal importeBD = getBigDecimal(concepto, "importe", BigDecimal.ZERO);
                String importeStr = String.format("%.2f", importeBD);
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph("$" + importeStr).setFontSize(7))
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.RIGHT));
                
                // IVA - manejar BigDecimal
                BigDecimal ivaBD = getBigDecimal(concepto, "iva", BigDecimal.ZERO);
                String ivaStr = String.format("%.2f", ivaBD);
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph("$" + ivaStr).setFontSize(7))
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.RIGHT));
                
                // Tasa IVA (del catálogo)
                String tasaIva = "";
                Object tasaIvaObj = concepto.get("tasaIva");
                if (tasaIvaObj != null) {
                    if (tasaIvaObj instanceof BigDecimal) {
                        // Si viene como BigDecimal (0.16), convertir a porcentaje
                        BigDecimal tasaBD = (BigDecimal) tasaIvaObj;
                        tasaIva = String.format("%.2f%%", tasaBD.multiply(new BigDecimal("100")).doubleValue());
                    } else if (tasaIvaObj instanceof Number) {
                        // Si viene como Number, convertir a porcentaje
                        double tasa = ((Number) tasaIvaObj).doubleValue();
                        tasaIva = String.format("%.2f%%", tasa * 100);
                    } else {
                        // Si viene como String, intentar parsearlo
                        String tasaIvaStr = tasaIvaObj.toString();
                        if (!tasaIvaStr.isEmpty()) {
                            try {
                                double tasa = Double.parseDouble(tasaIvaStr);
                                // Si es menor que 1, asumir que es decimal (0.16), sino ya es porcentaje
                                if (tasa < 1) {
                                    tasaIva = String.format("%.2f%%", tasa * 100);
                                } else {
                                    tasaIva = String.format("%.2f%%", tasa);
                                }
                            } catch (Exception e) {
                                // Si ya viene como porcentaje con %, usar tal cual
                                tasaIva = tasaIvaStr;
                            }
                        }
                    }
                }
                
                // Si aún está vacío, intentar calcular desde IVA e Importe
                if (tasaIva.isEmpty()) {
                    try {
                        BigDecimal ivaBDCalc = getBigDecimal(concepto, "iva", BigDecimal.ZERO);
                        BigDecimal importeBDCalc = getBigDecimal(concepto, "importe", BigDecimal.ZERO);
                        if (importeBDCalc.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal tasa = ivaBDCalc.divide(importeBDCalc, 4, java.math.RoundingMode.HALF_UP);
                            tasaIva = String.format("%.2f%%", tasa.multiply(new BigDecimal("100")).doubleValue());
                        }
                    } catch (Exception e) {
                        // Ignorar errores de conversión
                    }
                }
                
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph(tasaIva.isEmpty() ? "-" : tasaIva).setFontSize(7))
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER));
            }
            
            document.add(conceptosTable);
            
        } catch (Exception e) {
            logger.error("Error agregando conceptos modernos: ", e);
        }
    }
    
    private java.util.List<java.util.Map<String, Object>> getListValue(java.util.Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof java.util.List) {
            return (java.util.List<java.util.Map<String, Object>>) value;
        }
        return null;
    }

    private boolean esComplementoPago(Map<String, Object> facturaData) {
        String tipo = getString(facturaData, "tipoComprobante", "");
        if ("P".equalsIgnoreCase(tipo)) {
            return true;
        }
        java.util.List<java.util.Map<String, Object>> pagos = getListValue(facturaData, "pagosComplemento");
        return pagos != null && !pagos.isEmpty();
    }

    private void agregarSeccionComplementoPago(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            java.util.List<java.util.Map<String, Object>> pagos = getListValue(facturaData, "pagosComplemento");
            if (pagos == null || pagos.isEmpty()) {
                return;
            }

            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);

            // Título de la sección de pagos
            Paragraph tituloPagos = new Paragraph("INFORMACIÓN DEL PAGO")
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setMarginBottom(6)
                .setMarginTop(8);
            document.add(tituloPagos);

            // Tabla de pagos con el mismo diseño que los conceptos
            Table pagosTable = new Table(4);
            pagosTable.setWidth(UnitValue.createPercentValue(100));
            pagosTable.setMarginBottom(12);

            // Encabezados de la tabla de pagos
            String[] headers = {"Fecha de pago", "Forma de pago", "Moneda", "Monto pagado"};
            for (String header : headers) {
                Cell headerCell = new Cell()
                    .add(new Paragraph(header).setBold().setFontSize(8))
                    .setBackgroundColor(primaryColor)
                    .setFontColor(ColorConstants.WHITE)
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER);
                pagosTable.addHeaderCell(headerCell);
            }

            // Agregar filas de pagos
            for (java.util.Map<String, Object> pago : pagos) {
                // Fecha de pago (extraer solo la fecha si viene con hora)
                String fechaPago = getString(pago, "fechaPago", "");
                if (fechaPago != null && !fechaPago.isEmpty()) {
                    // Si la fecha tiene más de 10 caracteres, extraer solo la parte de la fecha
                    if (fechaPago.length() > 10 && fechaPago.contains("T")) {
                        fechaPago = fechaPago.substring(0, 10); // Solo fecha, sin hora
                    } else if (fechaPago.length() > 10 && fechaPago.contains(" ")) {
                        fechaPago = fechaPago.substring(0, 10); // Solo fecha, sin hora
                    }
                } else {
                    fechaPago = java.time.LocalDate.now().toString(); // Fecha por defecto
                }
                pagosTable.addCell(new Cell()
                    .add(new Paragraph(fechaPago).setFontSize(8))
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER));

                // Forma de pago (con descripción si está disponible)
                String formaPago = getString(pago, "formaPago", "");
                String formaPagoDesc = obtenerDescripcionFormaPago(formaPago);
                pagosTable.addCell(new Cell()
                    .add(new Paragraph(formaPagoDesc).setFontSize(8))
                    .setPadding(6));

                // Moneda
                String moneda = getString(pago, "moneda", "MXN");
                pagosTable.addCell(new Cell()
                    .add(new Paragraph(moneda).setFontSize(8))
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER));

                // Monto pagado
                String monto = formatearMonto(getString(pago, "monto", "0.00"));
                pagosTable.addCell(new Cell()
                    .add(new Paragraph("$" + monto).setFontSize(8))
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.RIGHT));
            }

            document.add(pagosTable);

            // Sección de documento relacionado
            if (!pagos.isEmpty()) {
                java.util.Map<String, Object> primerPago = pagos.get(0);
                String uuidRelacionado = getString(primerPago, "uuidRelacionado", "");
                
                if (uuidRelacionado != null && !uuidRelacionado.isEmpty()) {
                    Paragraph tituloDoc = new Paragraph("DOCUMENTO RELACIONADO")
                        .setBold()
                        .setFontSize(11)
                        .setFontColor(primaryColor)
                        .setMarginBottom(6)
                        .setMarginTop(8);
                    document.add(tituloDoc);

                    // Tabla de documento relacionado
                    Table docTable = new Table(5);
                    docTable.setWidth(UnitValue.createPercentValue(100));
                    docTable.setMarginBottom(12);

                    // Encabezados
                    String[] docHeaders = {"UUID relacionado", "Parcialidad", "Saldo anterior", "Importe pagado", "Saldo insoluto"};
                    for (String header : docHeaders) {
                        Cell headerCell = new Cell()
                            .add(new Paragraph(header).setBold().setFontSize(8))
                            .setBackgroundColor(primaryColor)
                            .setFontColor(ColorConstants.WHITE)
                            .setPadding(6)
                            .setTextAlignment(TextAlignment.CENTER);
                        docTable.addHeaderCell(headerCell);
                    }

                    // Agregar filas de documentos relacionados
                    int parcialidad = 1;
                    for (java.util.Map<String, Object> pago : pagos) {
                        // UUID relacionado
                        docTable.addCell(new Cell()
                            .add(new Paragraph(getString(pago, "uuidRelacionado", uuidRelacionado)).setFontSize(7))
                            .setPadding(6));

                        // Parcialidad
                        Object parcObj = pago.get("parcialidad");
                        String parcialidadStr = parcObj != null ? String.valueOf(parcObj) : String.valueOf(parcialidad++);
                        docTable.addCell(new Cell()
                            .add(new Paragraph(parcialidadStr).setFontSize(8))
                            .setPadding(6)
                            .setTextAlignment(TextAlignment.CENTER));

                        // Saldo anterior
                        String saldoAnterior = formatearMonto(getString(pago, "saldoAnterior", "0.00"));
                        docTable.addCell(new Cell()
                            .add(new Paragraph("$" + saldoAnterior).setFontSize(8))
                            .setPadding(6)
                            .setTextAlignment(TextAlignment.RIGHT));

                        // Importe pagado
                        String importePagado = formatearMonto(getString(pago, "importePagado", getString(pago, "monto", "0.00")));
                        docTable.addCell(new Cell()
                            .add(new Paragraph("$" + importePagado).setFontSize(8))
                            .setPadding(6)
                            .setTextAlignment(TextAlignment.RIGHT));

                        // Saldo insoluto
                        String saldoInsoluto = formatearMonto(getString(pago, "saldoInsoluto", "0.00"));
                        docTable.addCell(new Cell()
                            .add(new Paragraph("$" + saldoInsoluto).setFontSize(8))
                            .setPadding(6)
                            .setTextAlignment(TextAlignment.RIGHT));
                    }

                    document.add(docTable);
                }
            }

        } catch (Exception e) {
            logger.error("Error agregando sección de complemento de pago: ", e);
        }
    }

    private String formatearMonto(String monto) {
        try {
            if (monto == null || monto.trim().isEmpty()) {
                return "0.00";
            }
            BigDecimal bd = new BigDecimal(monto.trim());
            return bd.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        } catch (Exception e) {
            return monto != null ? monto : "0.00";
        }
    }

    private String obtenerDescripcionFormaPago(String clave) {
        if (clave == null || clave.trim().isEmpty()) {
            return "N/A";
        }
        String claveTrim = clave.trim();
        
        // Si ya viene la descripción completa (contiene " - "), usarla directamente
        if (claveTrim.contains(" - ")) {
            return claveTrim;
        }
        
        // Si solo viene el código, convertir a descripción
        java.util.Map<String, String> formas = new java.util.HashMap<>();
        formas.put("01", "01 - Efectivo");
        formas.put("02", "02 - Cheque nominativo");
        formas.put("03", "03 - Transferencia electrónica");
        formas.put("04", "04 - Tarjeta de crédito");
        formas.put("05", "05 - Monedero electrónico");
        formas.put("06", "06 - Dinero electrónico");
        formas.put("08", "08 - Vales de despensa");
        formas.put("12", "12 - Dación en pago");
        formas.put("13", "13 - Pago por subrogación");
        formas.put("14", "14 - Pago por consignación");
        formas.put("15", "15 - Condonación");
        formas.put("17", "17 - Compensación");
        formas.put("23", "23 - Novación");
        formas.put("24", "24 - Confusión");
        formas.put("25", "25 - Remisión de deuda");
        formas.put("26", "26 - Prescripción o caducidad");
        formas.put("27", "27 - A satisfacción del acreedor");
        formas.put("28", "28 - Tarjeta de débito");
        formas.put("29", "29 - Tarjeta de servicios");
        formas.put("30", "30 - Aplicación de anticipos");
        formas.put("31", "31 - Intermediario pagos");
        formas.put("99", "99 - Por definir");
        
        return formas.getOrDefault(claveTrim, claveTrim + " - Forma de pago");
    }

    private void agregarTotalesComplementoPago(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);
            
            // Calcular el total pagado sumando todos los pagos si no viene en los datos
            BigDecimal totalPagado = getBigDecimal(facturaData, "totalPagadoComplemento", BigDecimal.ZERO);
            if (totalPagado.compareTo(BigDecimal.ZERO) == 0) {
                java.util.List<java.util.Map<String, Object>> pagos = getListValue(facturaData, "pagosComplemento");
                if (pagos != null && !pagos.isEmpty()) {
                    for (java.util.Map<String, Object> pago : pagos) {
                        BigDecimal monto = getBigDecimal(pago, "monto", BigDecimal.ZERO);
                        totalPagado = totalPagado.add(monto);
                    }
                }
            }
            
            String moneda = getString(facturaData, "monedaComplemento", "MXN");
            if (moneda == null || moneda.trim().isEmpty()) {
                moneda = "MXN";
            }
            
            // Contenedor del resumen con el mismo diseño que los totales modernos
            Table resumenContainer = new Table(2);
            resumenContainer.setWidth(UnitValue.createPercentValue(100));
            resumenContainer.setMarginTop(8);
            
            // Celda vacía para alinear a la derecha (mismo diseño que agregarTotalesModerno)
            Cell espacioCell = new Cell()
                .setBorder(null)
                .setWidth(UnitValue.createPercentValue(65));
            resumenContainer.addCell(espacioCell);
            
            // Celda con los totales (mismo diseño que agregarTotalesModerno)
            Cell totalesCell = new Cell()
                .setBorder(new SolidBorder(primaryColor, 1))
                .setBackgroundColor(new DeviceRgb(248, 250, 252))
                .setPadding(8)
                .setWidth(UnitValue.createPercentValue(35));
            
            // Total pagado (mismo estilo que el TOTAL en agregarTotalesModerno)
            String totalTexto = formatearMonto(totalPagado.toPlainString());
            Paragraph totalParrafo = new Paragraph("TOTAL PAGADO: $" + totalTexto)
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(3);
            totalesCell.add(totalParrafo);
            
            // Moneda (opcional, más pequeña)
            if (!"MXN".equalsIgnoreCase(moneda.trim())) {
                Paragraph monedaParrafo = new Paragraph("Moneda: " + moneda)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(2);
                totalesCell.add(monedaParrafo);
            }

            resumenContainer.addCell(totalesCell);
            document.add(resumenContainer);
        } catch (Exception e) {
            logger.error("Error agregando totales de complemento de pago: ", e);
        }
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key, BigDecimal defaultValue) {
        Object value = map.get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value != null) {
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Sección específica para recibo de nómina.
     */
    private void agregarSeccionNomina(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);
            Map<String, Object> nomina = (Map<String, Object>) facturaData.get("nomina");
            if (nomina == null) nomina = java.util.Collections.emptyMap();

            // Título principal
            Paragraph tituloPrincipal = new Paragraph("INFORMACIÓN DE NÓMINA")
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setMarginBottom(8)
                .setMarginTop(8);
            document.add(tituloPrincipal);

            // Sección 1: DATOS DEL EMPLEADO
            Paragraph tituloEmpleado = new Paragraph("DATOS DEL EMPLEADO")
                .setBold()
                .setFontSize(10)
                .setFontColor(primaryColor)
                .setMarginBottom(6)
                .setMarginTop(12);
            document.add(tituloEmpleado);

            Table tablaEmpleado = new Table(2);
            tablaEmpleado.setWidth(UnitValue.createPercentValue(100));
            tablaEmpleado.setMarginBottom(10);

            agregarFilaTabla(tablaEmpleado, "Nombre:", getNominaString(facturaData, "nombre", getString(facturaData, "nombreReceptor", "")), primaryColor);
            agregarFilaTabla(tablaEmpleado, "RFC Receptor:", getNominaString(facturaData, "rfcReceptor", getString(facturaData, "rfcReceptor", "")), primaryColor);
            agregarFilaTabla(tablaEmpleado, "CURP:", getNominaString(facturaData, "curp", ""), primaryColor);
            agregarFilaTabla(tablaEmpleado, "Número de Seguridad Social:", getNominaString(facturaData, "numSeguridadSocial", ""), primaryColor);
            agregarFilaTabla(tablaEmpleado, "ID Empleado:", getNominaString(facturaData, "idEmpleado", ""), primaryColor);
            agregarFilaTabla(tablaEmpleado, "Correo Electrónico:", getNominaString(facturaData, "correoElectronico", ""), primaryColor);
            agregarFilaTabla(tablaEmpleado, "Domicilio Fiscal:", getNominaString(facturaData, "domicilioFiscalReceptor", ""), primaryColor);

            document.add(tablaEmpleado);

            // Sección 2: DATOS LABORALES
            Paragraph tituloLaboral = new Paragraph("DATOS LABORALES")
                .setBold()
                .setFontSize(10)
                .setFontColor(primaryColor)
                .setMarginBottom(6)
                .setMarginTop(12);
            document.add(tituloLaboral);

            Table tablaLaboral = new Table(2);
            tablaLaboral.setWidth(UnitValue.createPercentValue(100));
            tablaLaboral.setMarginBottom(10);

            agregarFilaTabla(tablaLaboral, "Fecha de Inicio Relación Laboral:", getNominaString(facturaData, "fechaInicioRelLaboral", ""), primaryColor);
            agregarFilaTabla(tablaLaboral, "Antigüedad:", getNominaString(facturaData, "antiguedad", ""), primaryColor);
            agregarFilaTabla(tablaLaboral, "Riesgo del Puesto:", getNominaString(facturaData, "riesgoPuesto", ""), primaryColor);
            agregarFilaTabla(tablaLaboral, "Salario Diario Integrado:", getNominaString(facturaData, "salarioDiarioIntegrado", ""), primaryColor);

            document.add(tablaLaboral);

            // Sección 3: PERÍODO Y FECHAS
            Paragraph tituloPeriodo = new Paragraph("PERÍODO Y FECHAS")
                .setBold()
                .setFontSize(10)
                .setFontColor(primaryColor)
                .setMarginBottom(6)
                .setMarginTop(12);
            document.add(tituloPeriodo);

            Table tablaPeriodo = new Table(2);
            tablaPeriodo.setWidth(UnitValue.createPercentValue(100));
            tablaPeriodo.setMarginBottom(10);

            agregarFilaTabla(tablaPeriodo, "Tipo de Nómina:", getNominaString(facturaData, "tipoNomina", ""), primaryColor);
            agregarFilaTabla(tablaPeriodo, "Período de Pago:", getNominaString(facturaData, "periodoPago", ""), primaryColor);
            agregarFilaTabla(tablaPeriodo, "Fecha de Pago:", getNominaString(facturaData, "fechaPago", ""), primaryColor);
            agregarFilaTabla(tablaPeriodo, "Fecha de Nómina:", getNominaString(facturaData, "fechaNomina", ""), primaryColor);
            agregarFilaTabla(tablaPeriodo, "Uso CFDI:", getNominaString(facturaData, "usoCfdi", ""), primaryColor);

            document.add(tablaPeriodo);

        } catch (Exception e) {
            logger.error("Error agregando sección de nómina: ", e);
        }
    }

    private void agregarFilaTabla(Table tabla, String label, String value, DeviceRgb primaryColor) {
        Cell labelCell = new Cell()
            .add(new Paragraph(label).setBold().setFontSize(8))
            .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
            .setPadding(6)
            .setBackgroundColor(new DeviceRgb(248, 250, 252));
        tabla.addCell(labelCell);

        Cell valueCell = new Cell()
            .add(new Paragraph(value.isEmpty() ? "-" : value).setFontSize(8))
            .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
            .setPadding(6)
            .setBackgroundColor(new DeviceRgb(255, 255, 255));
        tabla.addCell(valueCell);
    }

    /**
     * Totales específicos para nómina (Percepciones, Deducciones, Neto a Pagar).
     */
    private void agregarTotalesNomina(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);

            Table resumenContainer = new Table(2);
            resumenContainer.setWidth(UnitValue.createPercentValue(100));
            resumenContainer.setMarginTop(8);

            // Espacio para alinear a la derecha
            Cell espacioCell = new Cell()
                .setBorder(null)
                .setWidth(UnitValue.createPercentValue(65));
            resumenContainer.addCell(espacioCell);

            // Totales
            Cell totalesCell = new Cell()
                .setBorder(new SolidBorder(primaryColor, 1))
                .setBackgroundColor(new DeviceRgb(248, 250, 252))
                .setPadding(8)
                .setWidth(UnitValue.createPercentValue(35));

            BigDecimal percepciones = getNominaBigDecimal(facturaData, "percepciones", BigDecimal.ZERO);
            BigDecimal deducciones = getNominaBigDecimal(facturaData, "deducciones", BigDecimal.ZERO);
            BigDecimal neto = percepciones.subtract(deducciones);

            Paragraph per = new Paragraph("Percepciones: $" + percepciones.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                .setFontSize(9)
                .setMarginBottom(3)
                .setTextAlignment(TextAlignment.RIGHT);
            totalesCell.add(per);

            Paragraph ded = new Paragraph("Deducciones: $" + deducciones.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                .setFontSize(9)
                .setMarginBottom(3)
                .setTextAlignment(TextAlignment.RIGHT);
            totalesCell.add(ded);

            Paragraph net = new Paragraph("NETO A PAGAR: $" + neto.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(3);
            totalesCell.add(net);

            resumenContainer.addCell(totalesCell);
            document.add(resumenContainer);
        } catch (Exception e) {
            logger.error("Error agregando totales de nómina: ", e);
        }
    }

    private boolean tieneNomina(Map<String, Object> facturaData) {
        try {
            Object n = facturaData != null ? facturaData.get("nomina") : null;
            boolean hasMap = (n instanceof Map) && !((Map<?,?>) n).isEmpty();
            String serie = getString(facturaData, "serie", "");
            return hasMap || (serie != null && serie.equalsIgnoreCase("NOM"));
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean esRetencion(Map<String, Object> facturaData) {
        String tipo = getString(facturaData, "tipoComprobante", "");
        if ("R".equalsIgnoreCase(tipo)) {
            return true;
        }
        Object datosRetencion = facturaData != null ? facturaData.get("datosRetencion") : null;
        return datosRetencion instanceof Map && !((Map<?,?>) datosRetencion).isEmpty();
    }
    
    private boolean esCartaPorte(Map<String, Object> facturaData) {
        if (facturaData == null) {
            logger.debug("esCartaPorte: facturaData es null, retornando false");
            return false;
        }
        // Primero verificar por tipoComprobante (más confiable)
        String tipo = getString(facturaData, "tipoComprobante", "");
        logger.debug("esCartaPorte: tipoComprobante='{}'", tipo);
        if ("T".equalsIgnoreCase(tipo)) {
            logger.info("✓ Carta Porte detectada por tipoComprobante=T");
            return true;
        }
        // Fallback: verificar si existe datosCartaPorte (incluso si está vacío, si tiene las listas ya es válido)
        Object datosCartaPorte = facturaData.get("datosCartaPorte");
        logger.debug("esCartaPorte: datosCartaPorte es null? {}, es Map? {}", datosCartaPorte == null, datosCartaPorte instanceof Map);
        if (datosCartaPorte instanceof Map) {
            Map<?, ?> datosMap = (Map<?, ?>) datosCartaPorte;
            // Si tiene al menos descripcion o alguna de las listas, es Carta Porte
            boolean tieneDatos = !datosMap.isEmpty() && (
                datosMap.containsKey("descripcion") || 
                datosMap.containsKey("ubicaciones") || 
                datosMap.containsKey("mercancias") || 
                datosMap.containsKey("figurasTransporte")
            );
            if (tieneDatos) {
                logger.info("✓ Carta Porte detectada por presencia de datosCartaPorte con datos");
            } else {
                logger.debug("esCartaPorte: datosCartaPorte existe pero está vacío o sin campos clave");
            }
            return tieneDatos;
        }
        logger.debug("esCartaPorte: no es Carta Porte (tipo='{}', datosCartaPorte no es Map válido)", tipo);
        return false;
    }
    
    /**
     * Sección específica para Retención de Pagos con diseño estructurado
     */
    private void agregarSeccionRetencion(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);
            Object datosRetencionObj = facturaData.get("datosRetencion");
            if (!(datosRetencionObj instanceof Map)) {
                return;
            }
            Map<String, Object> datosRetencion = (Map<String, Object>) datosRetencionObj;
            
            // Título de la sección
            Paragraph tituloRetencion = new Paragraph("INFORMACIÓN DE LA RETENCIÓN")
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setMarginBottom(8)
                .setMarginTop(8);
            document.add(tituloRetencion);
            
            // Contenedor principal con borde
            Table container = new Table(1);
            container.setWidth(UnitValue.createPercentValue(100));
            container.setBorder(new SolidBorder(primaryColor, 1));
            container.setBackgroundColor(new DeviceRgb(248, 250, 252));
            container.setPadding(10);
            container.setMarginBottom(12);
            
            // Información principal: Tipo de retención y concepto
            Paragraph tipoLabel = new Paragraph("Tipo de Retención:")
                .setBold()
                .setFontSize(9)
                .setFontColor(primaryColor)
                .setMarginBottom(2);
            container.addCell(new Cell().add(tipoLabel).setBorder(null).setPadding(5));
            
            String descripcionTipo = getString(datosRetencion, "descripcionTipo", "");
            String cveRetenc = getString(datosRetencion, "cveRetenc", "");
            String tipoRetencionText = descripcionTipo;
            if (!cveRetenc.isEmpty()) {
                tipoRetencionText += " (Clave: " + cveRetenc + ")";
            }
            Paragraph tipoValue = new Paragraph(tipoRetencionText.isEmpty() ? "Retención de Pagos" : tipoRetencionText)
                .setFontSize(9)
                .setMarginBottom(8);
            container.addCell(new Cell().add(tipoValue).setBorder(null).setPadding(5));
            
            // Concepto
            String concepto = getString(datosRetencion, "concepto", "");
            if (!concepto.isEmpty()) {
                Paragraph conceptoLabel = new Paragraph("Concepto:")
                    .setBold()
                    .setFontSize(9)
                    .setFontColor(primaryColor)
                    .setMarginBottom(2);
                container.addCell(new Cell().add(conceptoLabel).setBorder(null).setPadding(5));
                
                Paragraph conceptoValue = new Paragraph(concepto)
                    .setFontSize(9)
                    .setMarginBottom(8);
                container.addCell(new Cell().add(conceptoValue).setBorder(null).setPadding(5));
            }
            
            // Tabla de montos en dos columnas
            Table montosTable = new Table(2);
            montosTable.setWidth(UnitValue.createPercentValue(100));
            montosTable.setMarginBottom(8);
            
            BigDecimal montoBase = getBigDecimal(datosRetencion, "montoBase", BigDecimal.ZERO);
            BigDecimal montoTotGravado = getBigDecimal(datosRetencion, "montoTotGravado", BigDecimal.ZERO);
            BigDecimal montoTotExento = getBigDecimal(datosRetencion, "montoTotExento", BigDecimal.ZERO);
            
            // Monto Base
            Cell montoBaseLabel = new Cell()
                .add(new Paragraph("Monto Base:").setBold().setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            montosTable.addCell(montoBaseLabel);
            Cell montoBaseValue = new Cell()
                .add(new Paragraph("$" + String.format("%.2f", montoBase)).setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            montosTable.addCell(montoBaseValue);
            
            // Monto Gravado
            Cell montoGravLabel = new Cell()
                .add(new Paragraph("Monto Gravado:").setBold().setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            montosTable.addCell(montoGravLabel);
            Cell montoGravValue = new Cell()
                .add(new Paragraph("$" + String.format("%.2f", montoTotGravado)).setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            montosTable.addCell(montoGravValue);
            
            // Monto Exento
            Cell montoExentLabel = new Cell()
                .add(new Paragraph("Monto Exento:").setBold().setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            montosTable.addCell(montoExentLabel);
            Cell montoExentValue = new Cell()
                .add(new Paragraph("$" + String.format("%.2f", montoTotExento)).setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            montosTable.addCell(montoExentValue);
            
            Cell montosCell = new Cell().add(montosTable).setBorder(null).setPadding(5);
            container.addCell(montosCell);
            
            // Período y Fecha de Pago
            String periodoMes = getString(datosRetencion, "periodoMes", "");
            String periodoAnio = getString(datosRetencion, "periodoAnio", "");
            String fechaPago = getString(datosRetencion, "fechaPago", "");
            
            if (!periodoMes.isEmpty() || !fechaPago.isEmpty()) {
                Paragraph periodoLabel = new Paragraph("Período y Fecha:")
                    .setBold()
                    .setFontSize(9)
                    .setFontColor(primaryColor)
                    .setMarginBottom(2);
                container.addCell(new Cell().add(periodoLabel).setBorder(null).setPadding(5));
                
                StringBuilder periodoText = new StringBuilder();
                if (!periodoMes.isEmpty() && !periodoAnio.isEmpty()) {
                    String mesNombre = obtenerNombreMes(periodoMes);
                    periodoText.append("Período: ").append(mesNombre).append(" ").append(periodoAnio);
                }
                if (!fechaPago.isEmpty()) {
                    if (periodoText.length() > 0) {
                        periodoText.append(" | ");
                    }
                    try {
                        java.time.LocalDate fecha = java.time.LocalDate.parse(fechaPago);
                        periodoText.append("Fecha de Pago: ").append(fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    } catch (Exception e) {
                        periodoText.append("Fecha de Pago: ").append(fechaPago);
                    }
                }
                
                Paragraph periodoValue = new Paragraph(periodoText.toString())
                    .setFontSize(9)
                    .setMarginBottom(8);
                container.addCell(new Cell().add(periodoValue).setBorder(null).setPadding(5));
            }
            
            // Impuestos Retenidos
            BigDecimal isrRetenido = getBigDecimal(datosRetencion, "isrRetenido", BigDecimal.ZERO);
            BigDecimal ivaRetenido = getBigDecimal(datosRetencion, "ivaRetenido", BigDecimal.ZERO);
            BigDecimal montoRetenido = getBigDecimal(datosRetencion, "montoRetenido", BigDecimal.ZERO);
            
            if (montoRetenido.compareTo(BigDecimal.ZERO) > 0) {
                Paragraph impuestosLabel = new Paragraph("Impuestos Retenidos:")
                    .setBold()
                    .setFontSize(9)
                    .setFontColor(primaryColor)
                    .setMarginBottom(2);
                container.addCell(new Cell().add(impuestosLabel).setBorder(null).setPadding(5));
                
                Table impuestosTable = new Table(2);
                impuestosTable.setWidth(UnitValue.createPercentValue(100));
                impuestosTable.setMarginBottom(8);
                
                if (isrRetenido.compareTo(BigDecimal.ZERO) > 0) {
                    Cell isrLabel = new Cell()
                        .add(new Paragraph("ISR:").setBold().setFontSize(8))
                        .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                        .setPadding(6)
                        .setBackgroundColor(new DeviceRgb(255, 255, 255));
                    impuestosTable.addCell(isrLabel);
                    Cell isrValue = new Cell()
                        .add(new Paragraph("$" + String.format("%.2f", isrRetenido)).setFontSize(8))
                        .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                        .setPadding(6)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setBackgroundColor(new DeviceRgb(255, 255, 255));
                    impuestosTable.addCell(isrValue);
                }
                
                if (ivaRetenido.compareTo(BigDecimal.ZERO) > 0) {
                    Cell ivaLabel = new Cell()
                        .add(new Paragraph("IVA:").setBold().setFontSize(8))
                        .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                        .setPadding(6)
                        .setBackgroundColor(new DeviceRgb(255, 255, 255));
                    impuestosTable.addCell(ivaLabel);
                    Cell ivaValue = new Cell()
                        .add(new Paragraph("$" + String.format("%.2f", ivaRetenido)).setFontSize(8))
                        .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                        .setPadding(6)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setBackgroundColor(new DeviceRgb(255, 255, 255));
                    impuestosTable.addCell(ivaValue);
                }
                
                Cell totalRetLabel = new Cell()
                    .add(new Paragraph("Total Retenido:").setBold().setFontSize(9).setFontColor(primaryColor))
                    .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                    .setPadding(6)
                    .setBackgroundColor(new DeviceRgb(240, 248, 255));
                impuestosTable.addCell(totalRetLabel);
                Cell totalRetValue = new Cell()
                    .add(new Paragraph("$" + String.format("%.2f", montoRetenido)).setBold().setFontSize(9).setFontColor(primaryColor))
                    .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(new DeviceRgb(240, 248, 255));
                impuestosTable.addCell(totalRetValue);
                
                Cell impuestosCell = new Cell().add(impuestosTable).setBorder(null).setPadding(5);
                container.addCell(impuestosCell);
            }
            
            document.add(container);
            
        } catch (Exception e) {
            logger.error("Error agregando sección de retención: ", e);
        }
    }
    
    /**
     * Totales específicos para retención (Monto Base y Monto Retenido)
     */
    private void agregarTotalesRetencion(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);
            
            Table resumenContainer = new Table(2);
            resumenContainer.setWidth(UnitValue.createPercentValue(100));
            resumenContainer.setMarginTop(8);
            
            // Espacio para alinear a la derecha
            Cell espacioCell = new Cell()
                .setBorder(null)
                .setWidth(UnitValue.createPercentValue(65));
            resumenContainer.addCell(espacioCell);
            
            // Totales
            Cell totalesCell = new Cell()
                .setBorder(new SolidBorder(primaryColor, 1))
                .setBackgroundColor(new DeviceRgb(248, 250, 252))
                .setPadding(8)
                .setWidth(UnitValue.createPercentValue(35));
            
            BigDecimal subtotal = getBigDecimal(facturaData, "subtotal", BigDecimal.ZERO);
            BigDecimal total = getBigDecimal(facturaData, "total", BigDecimal.ZERO);
            
            Paragraph subtotalP = new Paragraph("Monto Base: $" + subtotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                .setFontSize(9)
                .setMarginBottom(3)
                .setTextAlignment(TextAlignment.RIGHT);
            totalesCell.add(subtotalP);
            
            Paragraph totalP = new Paragraph("TOTAL RETENIDO: $" + total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(3);
            totalesCell.add(totalP);
            
            resumenContainer.addCell(totalesCell);
            document.add(resumenContainer);
        } catch (Exception e) {
            logger.error("Error agregando totales de retención: ", e);
        }
    }
    
    /**
     * Helper para obtener nombre del mes
     */
    private String obtenerNombreMes(String mes) {
        if (mes == null || mes.trim().isEmpty()) {
            return mes;
        }
        try {
            int mesInt = Integer.parseInt(mes.trim());
            String[] meses = {
                "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            };
            if (mesInt >= 1 && mesInt <= 12) {
                return meses[mesInt];
            }
        } catch (NumberFormatException e) {
            // Si no es un número válido, devolver el valor original
        }
        return mes;
    }
    
    /**
     * Sección específica para Carta Porte con diseño estructurado
     */
    private void agregarSeccionCartaPorte(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            logger.info("agregarSeccionCartaPorte: Iniciando agregado de sección de Carta Porte");
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);
            Object datosCartaPorteObj = facturaData.get("datosCartaPorte");
            if (!(datosCartaPorteObj instanceof Map)) {
                logger.error("agregarSeccionCartaPorte: datosCartaPorte no es un Map, tipo: {}", datosCartaPorteObj != null ? datosCartaPorteObj.getClass().getName() : "null");
                return;
            }
            Map<String, Object> datosCartaPorte = (Map<String, Object>) datosCartaPorteObj;
            logger.info("agregarSeccionCartaPorte: datosCartaPorte tiene {} campos", datosCartaPorte.size());
            
            // Título de la sección
            Paragraph tituloCartaPorte = new Paragraph("INFORMACIÓN DEL SERVICIO DE TRANSPORTE")
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setMarginBottom(8)
                .setMarginTop(8);
            document.add(tituloCartaPorte);
            
            // Contenedor principal con borde
            Table container = new Table(1);
            container.setWidth(UnitValue.createPercentValue(100));
            container.setBorder(new SolidBorder(primaryColor, 1));
            container.setBackgroundColor(new DeviceRgb(248, 250, 252));
            container.setPadding(10);
            container.setMarginBottom(12);
            
            // Descripción del servicio - siempre mostrar
            String descripcion = getString(datosCartaPorte, "descripcion", "");
            Paragraph descripcionLabel = new Paragraph("Descripción del Servicio:")
                .setBold()
                .setFontSize(9)
                .setFontColor(primaryColor)
                .setMarginBottom(2);
            container.addCell(new Cell().add(descripcionLabel).setBorder(null).setPadding(5));
            
            Paragraph descripcionValue = new Paragraph(descripcion.isEmpty() ? "-" : descripcion)
                .setFontSize(9)
                .setMarginBottom(8);
            container.addCell(new Cell().add(descripcionValue).setBorder(null).setPadding(5));
            
            // Información del transporte en dos columnas - siempre mostrar campos principales
            Table transporteTable = new Table(2);
            transporteTable.setWidth(UnitValue.createPercentValue(100));
            transporteTable.setMarginBottom(8);
            
            // Tipo de Transporte - siempre mostrar
            String tipoTransporte = getString(datosCartaPorte, "tipoTransporte", "");
            Cell tipoLabel = new Cell()
                .add(new Paragraph("Tipo de Transporte:").setBold().setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(tipoLabel);
            Cell tipoValue = new Cell()
                .add(new Paragraph(tipoTransporte.isEmpty() ? "-" : tipoTransporte).setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(tipoValue);
            
            // Permiso SCT - siempre mostrar
            String permisoSCT = getString(datosCartaPorte, "permisoSCT", "");
            String numeroPermisoSCT = getString(datosCartaPorte, "numeroPermisoSCT", "");
            Cell permisoLabel = new Cell()
                .add(new Paragraph("Permiso SCT:").setBold().setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(permisoLabel);
            String permisoValue = permisoSCT;
            if (!numeroPermisoSCT.isEmpty()) {
                permisoValue += (!permisoValue.isEmpty() ? " - " : "") + numeroPermisoSCT;
            }
            Cell permisoCell = new Cell()
                .add(new Paragraph(permisoValue.isEmpty() ? "-" : permisoValue).setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(permisoCell);
            
            // Placas del Vehículo - siempre mostrar
            String placasVehiculo = getString(datosCartaPorte, "placasVehiculo", "");
            Cell placasLabel = new Cell()
                .add(new Paragraph("Placas del Vehículo:").setBold().setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(placasLabel);
            Cell placasValue = new Cell()
                .add(new Paragraph(placasVehiculo.isEmpty() ? "-" : placasVehiculo).setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(placasValue);
            
            // Configuración Vehicular - siempre mostrar
            String configVehicular = getString(datosCartaPorte, "configVehicular", "");
            Cell configLabel = new Cell()
                .add(new Paragraph("Configuración Vehicular:").setBold().setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(configLabel);
            Cell configValue = new Cell()
                .add(new Paragraph(configVehicular.isEmpty() ? "-" : configVehicular).setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(configValue);
            
            // Nombre del Transportista - siempre mostrar
            String nombreTransportista = getString(datosCartaPorte, "nombreTransportista", "");
            String rfcTransportista = getString(datosCartaPorte, "rfcTransportista", "");
            Cell transportistaLabel = new Cell()
                .add(new Paragraph("Transportista:").setBold().setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(transportistaLabel);
            String transportistaValue = nombreTransportista;
            if (!rfcTransportista.isEmpty()) {
                transportistaValue += (!transportistaValue.isEmpty() ? " (" : "") + rfcTransportista + (!transportistaValue.isEmpty() ? ")" : "");
            }
            Cell transportistaCell = new Cell()
                .add(new Paragraph(transportistaValue.isEmpty() ? "-" : transportistaValue).setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(transportistaCell);
            
            // Bienes Transportados - siempre mostrar
            String bienesTransportados = getString(datosCartaPorte, "bienesTransportados", "");
            Cell bienesLabel = new Cell()
                .add(new Paragraph("Bienes Transportados:").setBold().setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(bienesLabel);
            Cell bienesValue = new Cell()
                .add(new Paragraph(bienesTransportados.isEmpty() ? "-" : bienesTransportados).setFontSize(8))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));
            transporteTable.addCell(bienesValue);
            
            Cell transporteCell = new Cell().add(transporteTable).setBorder(null).setPadding(5);
            container.addCell(transporteCell);
            document.add(container);
            
            // Sección UBICACIONES
            agregarSeccionUbicaciones(document, datosCartaPorte, primaryColor);
            
            // Sección MERCANCIAS
            agregarSeccionMercancias(document, datosCartaPorte, primaryColor);
            
            // Sección FIGURAS DE TRANSPORTE
            agregarSeccionFigurasTransporte(document, datosCartaPorte, primaryColor);
            
        } catch (Exception e) {
            logger.error("Error agregando sección de carta porte: ", e);
        }
    }
    
    /**
     * Sección UBICACIONES
     */
    private void agregarSeccionUbicaciones(Document document, Map<String, Object> datosCartaPorte, DeviceRgb primaryColor) {
        try {
            Object ubicacionesObj = datosCartaPorte.get("ubicaciones");
            if (!(ubicacionesObj instanceof java.util.List)) {
                return;
            }
            java.util.List<Map<String, Object>> ubicaciones = (java.util.List<Map<String, Object>>) ubicacionesObj;
            if (ubicaciones == null || ubicaciones.isEmpty()) {
                return;
            }
            
            Paragraph titulo = new Paragraph("UBICACIONES")
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setMarginBottom(6)
                .setMarginTop(12);
            document.add(titulo);
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1.5f, 1.5f}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginBottom(10);
            
            // Encabezados
            table.addHeaderCell(new Cell().add(new Paragraph("Tipo").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            table.addHeaderCell(new Cell().add(new Paragraph("Nombre/RFC").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            table.addHeaderCell(new Cell().add(new Paragraph("Fecha/Hora").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            table.addHeaderCell(new Cell().add(new Paragraph("Domicilio").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            
            for (Map<String, Object> ubicacion : ubicaciones) {
                String tipoUbicacion = getString(ubicacion, "tipoUbicacion", "");
                String tipoTexto = "01".equals(tipoUbicacion) ? "Origen" : "02".equals(tipoUbicacion) ? "Destino" : tipoUbicacion;
                
                String nombre = getString(ubicacion, "nombreRemitenteDestinatario", "");
                String rfc = getString(ubicacion, "rfcRemitenteDestinatario", "");
                String nombreRfc = nombre;
                if (!rfc.isEmpty()) {
                    nombreRfc += (!nombreRfc.isEmpty() ? "\n" : "") + rfc;
                }
                
                String fechaHora = getString(ubicacion, "fechaHoraSalidaLlegada", "");
                
                String domicilioStr = "";
                Object domicilioObj = ubicacion.get("domicilio");
                if (domicilioObj instanceof Map) {
                    Map<String, Object> domicilio = (Map<String, Object>) domicilioObj;
                    StringBuilder sb = new StringBuilder();
                    appendIfNotEmpty(sb, getString(domicilio, "calle", ""));
                    appendIfNotEmpty(sb, getString(domicilio, "numeroExterior", ""));
                    appendIfNotEmpty(sb, getString(domicilio, "municipio", ""));
                    appendIfNotEmpty(sb, getString(domicilio, "estado", ""));
                    appendIfNotEmpty(sb, getString(domicilio, "codigoPostal", ""));
                    domicilioStr = sb.toString();
                }
                
                table.addCell(new Cell().add(new Paragraph(tipoTexto).setFontSize(7)).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(nombreRfc.isEmpty() ? "-" : nombreRfc).setFontSize(7)).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(fechaHora.isEmpty() ? "-" : fechaHora).setFontSize(7)).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(domicilioStr.isEmpty() ? "-" : domicilioStr).setFontSize(7)).setPadding(5));
            }
            
            document.add(table);
        } catch (Exception e) {
            logger.error("Error agregando sección de ubicaciones: ", e);
        }
    }
    
    /**
     * Sección MERCANCIAS
     */
    private void agregarSeccionMercancias(Document document, Map<String, Object> datosCartaPorte, DeviceRgb primaryColor) {
        try {
            Object mercanciasObj = datosCartaPorte.get("mercancias");
            if (!(mercanciasObj instanceof java.util.List)) {
                return;
            }
            java.util.List<Map<String, Object>> mercancias = (java.util.List<Map<String, Object>>) mercanciasObj;
            if (mercancias == null || mercancias.isEmpty()) {
                return;
            }
            
            Paragraph titulo = new Paragraph("MERCANCIAS")
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setMarginBottom(6)
                .setMarginTop(12);
            document.add(titulo);
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 2, 1, 1, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginBottom(10);
            
            // Encabezados
            table.addHeaderCell(new Cell().add(new Paragraph("Descripción").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            table.addHeaderCell(new Cell().add(new Paragraph("Clave Bienes").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            table.addHeaderCell(new Cell().add(new Paragraph("Cantidad").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            table.addHeaderCell(new Cell().add(new Paragraph("Peso (kg)").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            table.addHeaderCell(new Cell().add(new Paragraph("Valor").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            
            for (Map<String, Object> mercancia : mercancias) {
                String descripcion = getString(mercancia, "descripcion", "");
                String bienesTransp = getString(mercancia, "bienesTransp", "");
                String cantidad = getString(mercancia, "cantidad", "");
                String peso = getString(mercancia, "pesoEnKg", "");
                String valor = getString(mercancia, "valorMercancia", "");
                
                table.addCell(new Cell().add(new Paragraph(descripcion.isEmpty() ? "-" : descripcion).setFontSize(7)).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(bienesTransp.isEmpty() ? "-" : bienesTransp).setFontSize(7)).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(cantidad.isEmpty() ? "-" : cantidad).setFontSize(7)).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(peso.isEmpty() ? "-" : peso).setFontSize(7)).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(valor.isEmpty() ? "-" : valor).setFontSize(7)).setPadding(5));
            }
            
            document.add(table);
        } catch (Exception e) {
            logger.error("Error agregando sección de mercancías: ", e);
        }
    }
    
    /**
     * Sección FIGURAS DE TRANSPORTE
     */
    private void agregarSeccionFigurasTransporte(Document document, Map<String, Object> datosCartaPorte, DeviceRgb primaryColor) {
        try {
            Object figurasObj = datosCartaPorte.get("figurasTransporte");
            if (!(figurasObj instanceof java.util.List)) {
                return;
            }
            java.util.List<Map<String, Object>> figuras = (java.util.List<Map<String, Object>>) figurasObj;
            if (figuras == null || figuras.isEmpty()) {
                return;
            }
            
            Paragraph titulo = new Paragraph("FIGURAS DE TRANSPORTE")
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setMarginBottom(6)
                .setMarginTop(12);
            document.add(titulo);
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1.5f, 1.5f, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginBottom(10);
            
            // Encabezados
            table.addHeaderCell(new Cell().add(new Paragraph("Tipo").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            table.addHeaderCell(new Cell().add(new Paragraph("Nombre").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            table.addHeaderCell(new Cell().add(new Paragraph("RFC").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            table.addHeaderCell(new Cell().add(new Paragraph("Núm. Licencia").setBold().setFontSize(8)).setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE).setPadding(6));
            
            for (Map<String, Object> figura : figuras) {
                String tipoFigura = getString(figura, "tipoFigura", "");
                String tipoTexto = obtenerNombreTipoFigura(tipoFigura);
                String nombre = getString(figura, "nombreFigura", "");
                String rfc = getString(figura, "rfcFigura", "");
                String numLicencia = getString(figura, "numLicencia", "");
                
                table.addCell(new Cell().add(new Paragraph(tipoTexto.isEmpty() ? tipoFigura : tipoTexto).setFontSize(7)).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(nombre.isEmpty() ? "-" : nombre).setFontSize(7)).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(rfc.isEmpty() ? "-" : rfc).setFontSize(7)).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(numLicencia.isEmpty() ? "-" : numLicencia).setFontSize(7)).setPadding(5));
            }
            
            document.add(table);
        } catch (Exception e) {
            logger.error("Error agregando sección de figuras de transporte: ", e);
        }
    }
    
    private void appendIfNotEmpty(StringBuilder sb, String value) {
        if (value != null && !value.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(value.trim());
        }
    }
    
    private String obtenerNombreTipoFigura(String tipoFigura) {
        if (tipoFigura == null || tipoFigura.isEmpty()) {
            return "";
        }
        switch (tipoFigura) {
            case "01": return "Propietario";
            case "02": return "Transportista";
            case "03": return "Operador";
            case "04": return "Arrendatario";
            case "05": return "Notificado";
            default: return tipoFigura;
        }
    }
    
    /**
     * Helper para formatear fechas
     */
    private String formatearFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return fecha;
        }
        try {
            java.time.LocalDate fechaDate = java.time.LocalDate.parse(fecha);
            return fechaDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            // Si no se puede parsear, devolver el valor original
            return fecha;
        }
    }

    private String getNominaString(Map<String, Object> facturaData, String key, String defaultValue) {
        try {
            Object n = facturaData != null ? facturaData.get("nomina") : null;
            if (n instanceof Map) {
                Object v = ((Map<?,?>) n).get(key);
                return v != null ? v.toString() : defaultValue;
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal getNominaBigDecimal(Map<String, Object> facturaData, String key, BigDecimal defaultValue) {
        try {
            Object n = facturaData != null ? facturaData.get("nomina") : null;
            Object v = null;
            if (n instanceof Map) {
                v = ((Map<?,?>) n).get(key);
            }
            if (v == null) return defaultValue;
            if (v instanceof BigDecimal) return (BigDecimal) v;
            if (v instanceof Number) return new BigDecimal(((Number) v).toString());
            String s = v.toString();
            if (s == null || s.trim().isEmpty()) return defaultValue;
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private void agregarTotalesModerno(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);
            
            // Calcular totales desde los conceptos si están disponibles
            BigDecimal subtotalCalculado = BigDecimal.ZERO;
            BigDecimal ivaCalculado = BigDecimal.ZERO;
            
            java.util.List<java.util.Map<String, Object>> conceptos = getListValue(facturaData, "conceptos");
            if (conceptos != null && !conceptos.isEmpty()) {
                for (java.util.Map<String, Object> concepto : conceptos) {
                    BigDecimal importeBD = getBigDecimal(concepto, "importe", BigDecimal.ZERO);
                    BigDecimal ivaBD = getBigDecimal(concepto, "iva", BigDecimal.ZERO);
                    subtotalCalculado = subtotalCalculado.add(importeBD);
                    ivaCalculado = ivaCalculado.add(ivaBD);
                }
            } else {
                // Si no hay conceptos, usar valores de facturaData
                subtotalCalculado = getBigDecimal(facturaData, "subtotal", BigDecimal.ZERO);
                ivaCalculado = getBigDecimal(facturaData, "iva", BigDecimal.ZERO);
            }
            
            BigDecimal totalCalculado = subtotalCalculado.add(ivaCalculado);
            
            // Formatear valores para mostrar
            String subtotalStr = String.format("%.2f", subtotalCalculado);
            String ivaStr = String.format("%.2f", ivaCalculado);
            String totalStr = String.format("%.2f", totalCalculado);
            
            // Contenedor del resumen más compacto
            Table resumenContainer = new Table(2);
            resumenContainer.setWidth(UnitValue.createPercentValue(100));
            resumenContainer.setMarginTop(8);
            
            // Celda vacía para alinear a la derecha
            Cell espacioCell = new Cell()
                .setBorder(null)
                .setWidth(UnitValue.createPercentValue(65));
            resumenContainer.addCell(espacioCell);
            
            // Celda con los totales más compacta
            Cell totalesCell = new Cell()
                .setBorder(new SolidBorder(primaryColor, 1))
                .setBackgroundColor(new DeviceRgb(248, 250, 252))
                .setPadding(8)
                .setWidth(UnitValue.createPercentValue(35));
            
            // Subtotal
            Paragraph subtotal = new Paragraph("Subtotal: $" + subtotalStr)
                .setFontSize(9)
                .setMarginBottom(3)
                .setTextAlignment(TextAlignment.RIGHT);
            totalesCell.add(subtotal);
            
            // IVA
            Paragraph iva = new Paragraph("IVA: $" + ivaStr)
                .setFontSize(9)
                .setMarginBottom(3)
                .setTextAlignment(TextAlignment.RIGHT);
            totalesCell.add(iva);
            
            // Total
            Paragraph total = new Paragraph("TOTAL: $" + totalStr)
                .setBold()
                .setFontSize(11)
                .setFontColor(primaryColor)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(3);
            totalesCell.add(total);
            
            resumenContainer.addCell(totalesCell);
            document.add(resumenContainer);
            
        } catch (Exception e) {
            logger.error("Error agregando totales modernos: ", e);
        }
    }

    private void agregarComplementoCartaPorte(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);
            Map<String, Object> complemento = (Map<String, Object>) facturaData.get("complementoCartaPorte");
            if (complemento == null) {
                logger.info("Factura sin complemento Carta Porte, se omite sección.");
                return;
            }

            Table container = new Table(1);
            container.setWidth(UnitValue.createPercentValue(100));
            container.setMarginTop(8);

            Cell cell = new Cell()
                .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1))
                .setBackgroundColor(new DeviceRgb(248, 250, 252))
                .setPadding(8);

            Paragraph titulo = new Paragraph("CARTA PORTE")
                .setBold()
                .setFontSize(9)
                .setFontColor(primaryColor)
                .setMarginBottom(4);
            cell.add(titulo);

            String serie = getString(facturaData, "serie", "CP");
            String folio = getString(facturaData, "folio", "CP001");
            Paragraph folioInfo = new Paragraph("Folio: " + serie + "-" + folio)
                .setFontSize(8)
                .setFontColor(new DeviceRgb(100, 116, 139))
                .setMarginBottom(6);
            cell.add(folioInfo);

            // Sección MERCANCÍAS
            Map<String, Object> mercancia = (Map<String, Object>) complemento.get("mercancia");
            java.util.List<java.util.Map<String, Object>> conceptos = getListValue(facturaData, "conceptos");
            String descripcion = null;
            String valorStr = null;
            String numeroSerie = null;
            String claveProdServ = null;
            String unidad = null;
            String cantidad = null;
            if (conceptos != null && !conceptos.isEmpty()) {
                java.util.Map<String, Object> c0 = conceptos.get(0);
                descripcion = getString(c0, "descripcion", "Mercancía");
                valorStr = getString(c0, "importe", "0.00");
                numeroSerie = getString(c0, "noIdentificacion", "");
                claveProdServ = getString(c0, "claveProdServ", "");
                unidad = getString(c0, "unidad", "PIEZA");
                cantidad = getString(c0, "cantidad", "1");
            }
            if (mercancia != null) {
                if (descripcion == null) descripcion = getString(mercancia, "descripcion", "Mercancía");
                if (claveProdServ == null || claveProdServ.isEmpty()) claveProdServ = getString(mercancia, "claveProdServ", "");
                if (unidad == null || unidad.isEmpty()) unidad = getString(mercancia, "unidad", "PIEZA");
                if (cantidad == null || cantidad.isEmpty()) cantidad = getString(mercancia, "cantidad", "1");
                if (valorStr == null || valorStr.isEmpty()) valorStr = getString(mercancia, "valor", "0.00");
                String numSerieTmp = getString(mercancia, "numeroSerie", "");
                if (numSerieTmp != null && !numSerieTmp.isEmpty()) numeroSerie = numSerieTmp;
            }

            Paragraph mercanciasTitulo = new Paragraph("MERCANCÍAS")
                .setBold()
                .setFontSize(8)
                .setFontColor(primaryColor)
                .setMarginBottom(2);
            cell.add(mercanciasTitulo);
            cell.add(new Paragraph("Descripción: " + (descripcion != null ? descripcion : ""))
                .setFontSize(8));
            String peso = mercancia != null ? getString(mercancia, "peso", "") : "";
            cell.add(new Paragraph("ClaveProdServ: " + (claveProdServ != null ? claveProdServ : "")
                    + "    Cant: " + (cantidad != null ? cantidad : "1")
                    + "    Unidad: " + (unidad != null ? unidad : "")
                    + (peso.isEmpty() ? "" : "    Peso: " + peso))
                .setFontSize(8));
            cell.add(new Paragraph("Valor: $" + (valorStr != null ? valorStr : "0.00")
                    + (numeroSerie != null && !numeroSerie.isEmpty() ? "    Núm. Serie: " + numeroSerie : ""))
                .setFontSize(8));

            // Sección TRANSPORTE
            Paragraph transporteTitulo = new Paragraph("TRANSPORTE")
                .setBold()
                .setFontSize(8)
                .setFontColor(primaryColor)
                .setMarginTop(6)
                .setMarginBottom(2);
            cell.add(transporteTitulo);
            String tipoTransporte = getString(complemento, "tipoTransporte", "01");
            String tipoLabel = "Autotransporte";
            if ("02".equals(tipoTransporte)) tipoLabel = "Marítimo";
            else if ("03".equals(tipoTransporte)) tipoLabel = "Aéreo";
            else if ("04".equals(tipoTransporte)) tipoLabel = "Ferroviario";
            cell.add(new Paragraph("Tipo: " + tipoLabel
                    + (getString(complemento, "permisoSCT", "").isEmpty() ? "" : "    Permiso SCT: " + getString(complemento, "permisoSCT", "")))
                .setFontSize(8));
            String placas = getString(complemento, "placasVehiculo", "");
            String operadorNombre = getString(complemento, "operadorNombre", "");
            String operadorRfc = getString(complemento, "operadorRfc", "");
            cell.add(new Paragraph("Placas: " + placas + "    Operador: " + operadorNombre
                    + (operadorRfc.isEmpty() ? "" : "    RFC: " + operadorRfc))
                .setFontSize(8));

            // Sección UBICACIONES
            Paragraph ubicacionesTitulo = new Paragraph("UBICACIONES")
                .setBold()
                .setFontSize(8)
                .setFontColor(primaryColor)
                .setMarginTop(6)
                .setMarginBottom(2);
            cell.add(ubicacionesTitulo);
            String origen = getString(complemento, "origen", "");
            String destino = getString(complemento, "destino", "");
            String salida = getString(complemento, "fechaSalida", "");
            String llegada = getString(complemento, "fechaLlegada", "");
            String horaSalida = salida.length() >= 16 ? salida.substring(11, 16) : salida;
            String horaLlegada = llegada.length() >= 16 ? llegada.substring(11, 16) : llegada;
            cell.add(new Paragraph("Origen: " + origen + (horaSalida.isEmpty() ? "" : " (" + horaSalida + ")"))
                .setFontSize(8));
            cell.add(new Paragraph("Destino: " + destino + (horaLlegada.isEmpty() ? "" : " (" + horaLlegada + ")"))
                .setFontSize(8));

            container.addCell(cell);
            document.add(container);
        } catch (Exception e) {
            logger.error("Error agregando complemento Carta Porte: ", e);
        }
    }
    
    private void agregarInformacionFiscalModerna(Document document, Map<String, Object> facturaData, Map<String, Object> logoConfig) {
        try {
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);
            // Información fiscal más compacta
            Table fiscalContainer = new Table(1);
            fiscalContainer.setWidth(UnitValue.createPercentValue(100));
            fiscalContainer.setMarginTop(8);
            
            Cell fiscalCell = new Cell()
                .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1))
                .setBackgroundColor(new DeviceRgb(252, 252, 254))
                .setPadding(8);
            
            // Título fiscal compacto
            Paragraph tituloFiscal = new Paragraph("INFORMACIÓN FISCAL")
                .setBold()
                .setFontSize(9)
                .setFontColor(primaryColor)
                .setMarginBottom(4);
            fiscalCell.add(tituloFiscal);
            
            // UUID
            Paragraph uuid = new Paragraph("UUID: " + getString(facturaData, "uuid", "01EFEF6E-543A-4ED1-B0CC-E2464740D206"))
                .setFontSize(7)
                .setMarginBottom(2);
            fiscalCell.add(uuid);
            
            // Cadena original
            Paragraph cadenaOriginal = new Paragraph("Cadena: " + getString(facturaData, "cadenaOriginal", "||1.0|01EFEF6E-543A-4ED1-B0CC-E2464740D206|2025-01-28 19:00:01||"))
                .setFontSize(7)
                .setMarginBottom(2);
            fiscalCell.add(cadenaOriginal);
            
            // Sello digital
            Paragraph selloDigital = new Paragraph("Sello: " + getString(facturaData, "selloDigital", "ABC123EFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFG123"))
                .setFontSize(7);
            fiscalCell.add(selloDigital);
            
            fiscalContainer.addCell(fiscalCell);
            document.add(fiscalContainer);
            
            // Agregar pie de página
            agregarPiePagina(document, logoConfig);
            
        } catch (Exception e) {
            logger.error("Error agregando información fiscal moderna: ", e);
        }
    }
    
    private void agregarPiePagina(Document document, Map<String, Object> logoConfig) {
        try {
            DeviceRgb primaryColor = extraerColorPrimario(logoConfig);
            // Pie de página con diseño moderno
            Table pieContainer = new Table(1);
            pieContainer.setWidth(UnitValue.createPercentValue(100));
            pieContainer.setMarginTop(15);
            
            Cell pieCell = new Cell()
                .setBorder(null)
                .setBackgroundColor(primaryColor)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);
            
            Paragraph piePagina = new Paragraph("Este documento es una representación impresa de un CFDI")
                .setFontSize(8)
                .setFontColor(ColorConstants.WHITE)
                .setBold();
            pieCell.add(piePagina);
            
            pieContainer.addCell(pieCell);
            document.add(pieContainer);
            
        } catch (Exception e) {
            logger.error("Error al agregar pie de página: ", e);
        }
    }
    
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Verifica si los bytes corresponden a un archivo SVG
     */
    private boolean isSvg(byte[] bytes) {
        try {
            String content = new String(bytes, "UTF-8");
            return content.trim().startsWith("<svg") || content.contains("<svg");
        } catch (Exception e) {
            logger.warn("Error verificando si es SVG: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Convierte un SVG a PNG usando Apache Batik
     */
    private byte[] convertSvgToPng(byte[] svgBytes) throws Exception {
        try {
            // Crear el transcoder PNG
            PNGTranscoder transcoder = new PNGTranscoder();
            
            // Configurar el tamaño de salida (opcional)
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 200f);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 200f);
            
            // Crear input desde los bytes SVG
            String svgContent = new String(svgBytes, "UTF-8");
            TranscoderInput input = new TranscoderInput(new StringReader(svgContent));
            
            // Crear output stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(outputStream);
            
            // Realizar la conversión
            transcoder.transcode(input, output);
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            logger.error("Error convirtiendo SVG a PNG: {}", e.getMessage(), e);
            throw new Exception("No se pudo convertir SVG a PNG: " + e.getMessage());
        }
    }


private DeviceRgb getColorFromHex(String hex) {
    try {
        if (hex == null || !hex.startsWith("#") || (hex.length() != 7 && hex.length() != 9)) {
            logger.info("Hex inválido o nulo para color primario: {}", hex);
            return new DeviceRgb(30, 64, 175);
        }
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        logger.info("Color hex recibido: {} -> rgb({}, {}, {})", hex, r, g, b);
        return new DeviceRgb(r, g, b);
    } catch (Exception e) {
        logger.warn("Fallo convirtiendo hex a color: {}", e.getMessage());
        return new DeviceRgb(30, 64, 175);
    }
}


private DeviceRgb extraerColorPrimario(Map<String, Object> logoConfig) {
    try {
        if (logoConfig == null) {
            logger.info("LogoConfig nulo; usando color por defecto");
            return new DeviceRgb(30, 64, 175);
        }
        Object customColorsObj = logoConfig.get("customColors");
        if (customColorsObj instanceof Map) {
            Map<String, Object> customColors = (Map<String, Object>) customColorsObj;
            Object primaryObj = customColors.get("primary");
            String hex = (primaryObj instanceof String) ? (String) primaryObj : null;
            if (hex != null) {
                logger.info("extraerColorPrimario: hex recibido {}", hex);
                return getColorFromHex(hex);
            } else {
                logger.info("extraerColorPrimario: sin 'primary' válido; usando por defecto");
            }
        } else {
            logger.info("extraerColorPrimario: sin 'customColors'; usando por defecto");
        }
    } catch (Exception e) {
        logger.warn("extraerColorPrimario: error al leer logoConfig: {}", e.getMessage());
    }
    return new DeviceRgb(30, 64, 175);
}

    public byte[] generarPdfComplementoPago(ComplementoPagoPdfData data) throws IOException {
        // Mantener compatibilidad: llamar al método con logoConfig usando configuración vacía
        return generarPdfComplementoPago(data, Map.of());
    }

    public byte[] generarPdfComplementoPago(ComplementoPagoPdfData data, Map<String, Object> logoConfig) throws IOException {
        if (data == null) {
            throw new IOException("Datos del complemento vacíos");
        }

        java.util.Map<String, Object> facturaData = new java.util.HashMap<>();
        facturaData.put("tipoComprobante", "P");
        facturaData.put("uuid", valorO(data.uuidComplemento, ""));
        facturaData.put("facturaUuid", valorO(data.facturaUuid, ""));
        facturaData.put("serie", valorO(data.serieComplemento, "REP"));
        facturaData.put("folio", valorO(data.folioComplemento, ""));
        facturaData.put("fechaEmision", valorO(data.fechaTimbrado, java.time.LocalDateTime.now().format(FECHA_HORA)));
        facturaData.put("nombreEmisor", valorO(data.nombreEmisor, valorO(data.rfcEmisor, "AAA010101AAA")));
        facturaData.put("rfcEmisor", valorO(data.rfcEmisor, "AAA010101AAA"));
        facturaData.put("nombreReceptor", valorO(data.nombreReceptor, valorO(data.rfcReceptor, "XAXX010101000")));
        facturaData.put("rfcReceptor", valorO(data.rfcReceptor, "XAXX010101000"));
        facturaData.put("metodoPago", valorO(data.metodoCfdi, "PPD"));
        facturaData.put("formaPago", valorO(data.formaCfdi, "99"));
        facturaData.put("correoReceptor", valorO(data.correoReceptor, ""));
        facturaData.put("cadenaOriginal", valorO(data.cadenaOriginal, ""));
        facturaData.put("selloDigital", valorO(data.selloDigital, ""));
        facturaData.put("certificado", valorO(data.selloSat, ""));
        facturaData.put("totalPagadoComplemento", valorO(data.totalPagado, "0.00"));
        facturaData.put("monedaComplemento", valorO(data.moneda, "MXN"));

        java.util.List<java.util.Map<String, Object>> pagos = new java.util.ArrayList<>();
        if (data.pagos != null) {
            for (ComplementoPagoPdfData.PagoDetalle detalle : data.pagos) {
                java.util.Map<String, Object> pagoMap = new java.util.HashMap<>();
                pagoMap.put("fechaPago", valorO(detalle.fechaPago, ""));
                pagoMap.put("formaPago", valorO(detalle.formaPago, ""));
                pagoMap.put("moneda", valorO(detalle.moneda, "MXN"));
                pagoMap.put("monto", valorO(detalle.monto, "0.00"));
                pagoMap.put("uuidRelacionado", valorO(detalle.uuidRelacionado, ""));
                pagoMap.put("parcialidad", detalle.parcialidad);
                pagoMap.put("saldoAnterior", valorO(detalle.saldoAnterior, "0.00"));
                pagoMap.put("importePagado", valorO(detalle.importePagado, "0.00"));
                pagoMap.put("saldoInsoluto", valorO(detalle.saldoInsoluto, "0.00"));
                pagos.add(pagoMap);
            }
        }
        facturaData.put("pagosComplemento", pagos);

        return generarPdfConLogo(facturaData, logoConfig != null ? logoConfig : Map.of());
    }

    private String valorO(String valor, String fallback) {
        if (valor == null) {
            return fallback;
        }
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    public static class ComplementoPagoPdfData {
        public String uuidComplemento;
        public String facturaUuid;
        public String serieComplemento;
        public String folioComplemento;
        public String fechaTimbrado;
        public String rfcEmisor;
        public String rfcReceptor;
        public String correoReceptor;
        public String totalPagado;
        public String nombreEmisor;
        public String nombreReceptor;
        public String metodoCfdi;
        public String formaCfdi;
        public String cadenaOriginal;
        public String selloDigital;
        public String selloSat;
        public String moneda;
        public List<PagoDetalle> pagos = new java.util.ArrayList<>();

        public static class PagoDetalle {
            public String fechaPago;
            public String formaPago;
            public String moneda;
            public String monto;
            public int parcialidad;
            public String saldoAnterior;
            public String importePagado;
            public String saldoInsoluto;
            public String uuidRelacionado;
        }
    }
}