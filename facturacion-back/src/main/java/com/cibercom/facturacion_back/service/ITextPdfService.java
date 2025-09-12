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
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
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
import java.util.Base64;
import java.util.Map;

@Service
public class ITextPdfService {
    
    private static final Logger logger = LoggerFactory.getLogger(ITextPdfService.class);
    
    public byte[] generarPdfConLogo(Map<String, Object> facturaData, Map<String, Object> logoConfig) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            // Crear el documento PDF
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Configurar márgenes más compactos
            document.setMargins(20, 20, 20, 20);
            
            // Agregar encabezado moderno
            agregarEncabezadoModerno(document, facturaData, logoConfig);
            
            // Agregar información de empresa y cliente
            agregarInformacionEmpresaCliente(document, facturaData);
            
            // Agregar conceptos con diseño moderno
            agregarConceptosModerno(document, facturaData);
            
            // Agregar totales modernos
            agregarTotalesModerno(document, facturaData);
            
            // Agregar información fiscal moderna
            agregarInformacionFiscalModerna(document, facturaData);
            
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
        Map<String, Object> factura = facturaData;
        
        try {
            // Agregar encabezado con fondo
            agregarEncabezadoConFondo(document, facturaData);
            
        } catch (Exception e) {
            logger.error("Error agregando encabezado moderno: ", e);
        }
    }
    
    private void agregarEncabezadoConFondo(Document document, Map<String, Object> factura) {
        try {
            // Contenedor del encabezado con fondo azul
            Table headerContainer = new Table(2);
            headerContainer.setWidth(UnitValue.createPercentValue(100));
            headerContainer.setBackgroundColor(new DeviceRgb(30, 64, 175));
            headerContainer.setPadding(8);
            
            // Celda del título
            Cell tituloCell = new Cell()
                .setBorder(null)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .setWidth(UnitValue.createPercentValue(70));
            
            Paragraph titulo = new Paragraph("FACTURA ELECTRÓNICA")
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
                .setMarginTop(3)
                .setMarginBottom(0);
            tituloCell.add(infoFactura);
            
            headerContainer.addCell(tituloCell);
            
            // Celda del logo
            Cell logoCell = new Cell()
                .setBorder(null)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setWidth(UnitValue.createPercentValue(30));
            
            agregarLogoCompactoHeader(logoCell);
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
                
                // Si no hay Base64, intentar cargar desde URL
                if (logoBytes == null && logoConfig.containsKey("logoUrl")) {
                    String logoUrl = (String) logoConfig.get("logoUrl");
                    Path logoPath = Paths.get("src/main/resources/static" + logoUrl);
                    if (Files.exists(logoPath)) {
                        logoBytes = Files.readAllBytes(logoPath);
                        logger.info("Logo cargado desde archivo: {}", logoPath);
                    } else {
                        logger.warn("Logo no encontrado en: {}", logoPath);
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
    
    private void agregarInformacionEmpresaCliente(Document document, Map<String, Object> facturaData) {
        try {
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
                .setFontColor(new DeviceRgb(30, 64, 175))
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
                .setFontColor(new DeviceRgb(30, 64, 175))
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
    
    private void agregarConceptosModerno(Document document, Map<String, Object> facturaData) {
        try {
            // Título de la sección más compacto
            Paragraph conceptosTitulo = new Paragraph("CONCEPTOS")
                .setBold()
                .setFontSize(11)
                .setFontColor(new DeviceRgb(30, 64, 175))
                .setMarginBottom(6)
                .setMarginTop(8)
                .setCharacterSpacing(0.3f);
            document.add(conceptosTitulo);
            
            // Crear tabla de conceptos más compacta
            Table conceptosTable = new Table(new float[]{0.8f, 3.5f, 1, 1, 0.7f});
            conceptosTable.setWidth(UnitValue.createPercentValue(100));
            
            // Encabezados de la tabla más compactos
            String[] headers = {"Cant", "Descripción", "P.Unit", "Importe", "IVA"};
            for (String header : headers) {
                Cell headerCell = new Cell()
                    .add(new Paragraph(header).setBold().setFontSize(8))
                    .setBackgroundColor(new DeviceRgb(30, 64, 175))
                    .setFontColor(ColorConstants.WHITE)
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER);
                conceptosTable.addHeaderCell(headerCell);
            }
            
            // Obtener conceptos
            java.util.List<java.util.Map<String, Object>> conceptos = getListValue(facturaData, "conceptos");
            if (conceptos == null || conceptos.isEmpty()) {
                // Crear concepto de ejemplo
                conceptos = new java.util.ArrayList<>();
                java.util.Map<String, Object> conceptoEjemplo = new java.util.HashMap<>();
                conceptoEjemplo.put("cantidad", "1");
                conceptoEjemplo.put("descripcion", "Producto de ejemplo");
                conceptoEjemplo.put("valorUnitario", "100.00");
                conceptoEjemplo.put("importe", "100.00");
                conceptoEjemplo.put("iva", "16.00");
                conceptos.add(conceptoEjemplo);
            }
            
            // Agregar filas de conceptos
            for (java.util.Map<String, Object> concepto : conceptos) {
                // Cantidad
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph(getString(concepto, "cantidad", "1")).setFontSize(8))
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER));
                
                // Descripción
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph(getString(concepto, "descripcion", "Producto/Servicio")).setFontSize(8))
                    .setPadding(6));
                
                // Precio unitario
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph("$" + getString(concepto, "valorUnitario", "0.00")).setFontSize(8))
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.RIGHT));
                
                // Importe
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph("$" + getString(concepto, "importe", "0.00")).setFontSize(8))
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.RIGHT));
                
                // IVA
                conceptosTable.addCell(new Cell()
                    .add(new Paragraph("$" + getString(concepto, "iva", "0.00")).setFontSize(8))
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.RIGHT));
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
    
    private void agregarTotalesModerno(Document document, Map<String, Object> facturaData) {
        try {
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
                .setBorder(new SolidBorder(new DeviceRgb(30, 64, 175), 1))
                .setBackgroundColor(new DeviceRgb(248, 250, 252))
                .setPadding(8)
                .setWidth(UnitValue.createPercentValue(35));
            
            // Subtotal
            Paragraph subtotal = new Paragraph("Subtotal: $" + getString(facturaData, "subtotal", "100.00"))
                .setFontSize(9)
                .setMarginBottom(3)
                .setTextAlignment(TextAlignment.RIGHT);
            totalesCell.add(subtotal);
            
            // IVA
            Paragraph iva = new Paragraph("IVA: $" + getString(facturaData, "iva", "16.00"))
                .setFontSize(9)
                .setMarginBottom(3)
                .setTextAlignment(TextAlignment.RIGHT);
            totalesCell.add(iva);
            
            // Total
            Paragraph total = new Paragraph("TOTAL: $" + getString(facturaData, "total", "116.00"))
                .setBold()
                .setFontSize(11)
                .setFontColor(new DeviceRgb(30, 64, 175))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(3);
            totalesCell.add(total);
            
            resumenContainer.addCell(totalesCell);
            document.add(resumenContainer);
            
        } catch (Exception e) {
            logger.error("Error agregando totales modernos: ", e);
        }
    }
    
    private void agregarInformacionFiscalModerna(Document document, Map<String, Object> facturaData) {
        try {
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
                .setFontColor(new DeviceRgb(30, 64, 175))
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
            agregarPiePagina(document);
            
        } catch (Exception e) {
            logger.error("Error agregando información fiscal moderna: ", e);
        }
    }
    
    private void agregarPiePagina(Document document) {
        try {
            // Pie de página con diseño moderno
            Table pieContainer = new Table(1);
            pieContainer.setWidth(UnitValue.createPercentValue(100));
            pieContainer.setMarginTop(15);
            
            Cell pieCell = new Cell()
                .setBorder(null)
                .setBackgroundColor(new DeviceRgb(30, 64, 175))
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
}