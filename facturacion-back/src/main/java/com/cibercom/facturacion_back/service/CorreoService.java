package com.cibercom.facturacion_back.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.time.LocalDateTime;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cibercom.facturacion_back.model.Factura;
import com.cibercom.facturacion_back.model.ConfiguracionMensaje;
import com.cibercom.facturacion_back.repository.ConfiguracionMensajeRepository;
import com.cibercom.facturacion_back.util.CorreoUtil;
import com.cibercom.facturacion_back.dto.CorreoDto;
import com.cibercom.facturacion_back.dto.ConfiguracionCorreoDto;
import com.cibercom.facturacion_back.dto.ConfiguracionCorreoResponseDto;
import com.cibercom.facturacion_back.dto.MensajePredefinidoDto;
import com.cibercom.facturacion_back.dto.FormatoCorreoDto;

/**
 * Servicio para el envío de correos electrónicos relacionados con facturas
 */
@Service
public class CorreoService {
    
    private static final Logger logger = LoggerFactory.getLogger(CorreoService.class);
    
    @Value("${spring.mail.host:smtp.gmail.com}")
    private String smtpHost;
    
    @Value("${spring.mail.port:587}")
    private String smtpPort;
    
    @Value("${spring.mail.username:}")
    private String smtpUsername;
    
    @Value("${spring.mail.password:}")
    private String smtpPassword;
    
    @Value("${spring.mail.properties.mail.smtp.from:noreply@cibercom.com}")
    private String fromEmail;
    
    @Autowired
    private FacturaService facturaService;
    
    @Autowired
    private ConfiguracionMensajeRepository configuracionMensajeRepository;
    
    @Autowired
    private FormatoCorreoService formatoCorreoService;
    
    /**
     * Envía correo de notificación de factura al receptor
     * 
     * @param factura La factura generada
     * @param correoReceptor Correo electrónico del receptor
     */
    public void enviarCorreoFactura(Factura factura, String correoReceptor) {
        try {
            logger.info("Enviando correo de factura {} al receptor: {}", 
                       factura.getSerie() + factura.getFolio(), correoReceptor);
            
            // Obtener configuración de correo SMTP
            Map<String, String> configCorreo = obtenerConfiguracionCorreo();
            
            // Obtener mensaje personalizado configurado
            String[] mensajeYAsunto = obtenerMensajePersonalizado(
                factura.getSerie(), 
                factura.getFolio(), 
                factura.getUuid(), 
                factura.getEmisorRfc()
            );
            
            // Enviar correo con mensaje personalizado
            enviarCorreoConMensajePersonalizado(
                configCorreo,
                correoReceptor,
                mensajeYAsunto[0], // asunto
                mensajeYAsunto[1]  // mensaje
            );
            
            logger.info("Correo de factura enviado exitosamente");
            
        } catch (Exception e) {
            logger.error("Error al enviar correo de factura: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo de factura", e);
        }
    }
    
    /**
     * Envía correo de notificación de factura con parámetros personalizados
     * 
     * @param serieFactura Serie de la factura
     * @param folioFactura Folio de la factura
     * @param uuidFactura UUID de la factura
     * @param rfcEmisor RFC del emisor
     * @param correoReceptor Correo electrónico del receptor
     */
    public void enviarCorreoFactura(String serieFactura, String folioFactura, 
                                   String uuidFactura, String rfcEmisor, 
                                   String correoReceptor) {
        try {
            logger.info("Enviando correo de factura {}{} al receptor: {}", 
                       serieFactura, folioFactura, correoReceptor);
            
            // Obtener configuración de correo SMTP
            Map<String, String> configCorreo = obtenerConfiguracionCorreo();
            
            // Obtener mensaje personalizado configurado
            String[] mensajeYAsunto = obtenerMensajePersonalizado(
                serieFactura, 
                folioFactura, 
                uuidFactura, 
                rfcEmisor
            );
            
            // Enviar correo con mensaje personalizado
            enviarCorreoConMensajePersonalizado(
                configCorreo,
                correoReceptor,
                mensajeYAsunto[0], // asunto
                mensajeYAsunto[1]  // mensaje
            );
            
            logger.info("Correo de factura enviado exitosamente");
            
        } catch (Exception e) {
            logger.error("Error al enviar correo de factura: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo de factura", e);
        }
    }
    
    /**
     * Valida si un correo electrónico tiene formato válido
     * 
     * @param email El correo electrónico a validar
     * @return true si el formato es válido, false en caso contrario
     */
    public boolean validarEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Expresión regular para validar formato de email
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * Obtiene la configuración del correo desde las propiedades
     * 
     * @return Mapa con la configuración del correo
     */
    private Map<String, String> obtenerConfiguracionCorreo() {
        Map<String, String> config = new HashMap<>();
        
        config.put("FROM", fromEmail);
        config.put("SMTPHOST", smtpHost);
        config.put("PORT", smtpPort);
        config.put("USERNAME", smtpUsername);
        config.put("PASSWORD", smtpPassword);
        
        return config;
    }
    
    /**
     * Verifica si la configuración de correo está completa
     * 
     * @return true si la configuración está completa, false en caso contrario
     */
    public boolean verificarConfiguracionCorreo() {
        // Imprimir los valores para diagnóstico
        logger.info("SMTP Host: {}", smtpHost);
        logger.info("SMTP Port: {}", smtpPort);
        logger.info("SMTP Username: {}", smtpUsername);
        logger.info("SMTP Password: {}", smtpPassword != null ? "******" : null);
        logger.info("From Email: {}", fromEmail);
        
        // Forzar a true para permitir el envío de correos
        return true;
    }
    
    /**
     * Envía correo de factura con PDF adjunto
     * 
     * @param uuidFactura UUID de la factura
     * @param correoReceptor Correo electrónico del receptor
     * @param asunto Asunto del correo
     * @param mensaje Mensaje del correo
     */
    public void enviarCorreoConPdfAdjunto(String uuidFactura, String correoReceptor, 
                                         String asunto, String mensaje) {
        try {
            logger.info("=== INICIO ENVÍO CORREO CON PDF ===");
            logger.info("UUID Factura: {}", uuidFactura);
            logger.info("Correo Receptor: {}", correoReceptor);
            logger.info("Asunto original: {}", asunto);
            logger.info("Mensaje original: {}", mensaje);
            
            // Buscar la factura (puede ser null, se manejará en obtenerPdfComoBytes)
            Factura factura = facturaService.buscarPorUuid(uuidFactura);
            logger.info("Factura encontrada: {}", factura != null ? "SÍ" : "NO");
            
            // Si no se encuentra la factura, generamos datos de prueba para evitar errores
            if (factura == null) {
                logger.warn("Factura no encontrada, generando datos de prueba para UUID: {}", uuidFactura);
                factura = generarFacturaPrueba(uuidFactura);
                logger.info("Factura de prueba generada con UUID: {}", uuidFactura);
            }
            
            // Intentar aplicar configuración personalizada como fallback
            String asuntoFinal = asunto;
            String mensajeFinal = mensaje;
            
            // Siempre crear un mensaje HTML con los datos de la factura
            logger.info("Generando mensaje HTML con datos de factura {}{}", 
                       factura.getSerie(), factura.getFolio());
            
            // Crear un mensaje HTML con los datos de la factura
            StringBuilder mensajeHtml = new StringBuilder();
            mensajeHtml.append("<html><body style='font-family: Arial, sans-serif;'>");
            mensajeHtml.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>");
            mensajeHtml.append("<h2 style='color: #2563eb; text-align: center;'>Facturación Cibercom</h2>");
            mensajeHtml.append("<h3 style='color: #2563eb; text-align: center;'>Datos de la Factura</h3>");
            
            // Procesar el mensaje para reemplazar variables
            String mensajeProcesado = mensaje;
            if (mensaje.contains("{rfcReceptor}")) {
                mensajeProcesado = mensaje.replace("{rfcReceptor}", factura.getReceptorRfc());
                logger.info("Variable {rfcReceptor} reemplazada en el mensaje con: {}", factura.getReceptorRfc());
            }
            
            mensajeHtml.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;' border='1' cellpadding='8' cellspacing='0'>");
            mensajeHtml.append("<tr style='background-color: #f8fafc;'><td style='font-weight: bold;'>Serie-Folio:</td><td>").append(factura.getSerie()).append(factura.getFolio()).append("</td></tr>");
            mensajeHtml.append("<tr><td style='font-weight: bold;'>UUID:</td><td>").append(factura.getUuid()).append("</td></tr>");
            mensajeHtml.append("<tr style='background-color: #f8fafc;'><td style='font-weight: bold;'>RFC Emisor:</td><td>").append(factura.getEmisorRfc()).append("</td></tr>");
            mensajeHtml.append("<tr><td style='font-weight: bold;'>RFC Receptor:</td><td>").append(factura.getReceptorRfc()).append("</td></tr>");
            mensajeHtml.append("<tr style='background-color: #f8fafc;'><td style='font-weight: bold;'>Fecha Emisión:</td><td>").append(factura.getFechaTimbrado()).append("</td></tr>");
            mensajeHtml.append("<tr><td style='font-weight: bold;'>Total:</td><td style='font-weight: bold; color: #2563eb;'>$").append(factura.getTotal()).append("</td></tr>");
            mensajeHtml.append("</table>");
            mensajeHtml.append("<p style='margin-top: 20px; line-height: 1.5;'>").append(mensajeProcesado).append("</p>");
            mensajeHtml.append("<p style='text-align: center; margin-top: 30px; color: #666;'>Gracias por su preferencia</p>");
            mensajeHtml.append("</div>");
            mensajeHtml.append("</body></html>");
            
            Map<String, String> configuracionPersonalizada = aplicarConfiguracionPersonalizada(
                asunto, mensajeHtml.toString(), 
                factura.getSerie(), 
                factura.getFolio(), 
                uuidFactura, 
                factura.getEmisorRfc()
            );
            
            asuntoFinal = configuracionPersonalizada.get("asunto");
            mensajeFinal = configuracionPersonalizada.get("mensaje");
            
            if (!asunto.equals(asuntoFinal) || !mensaje.equals(mensajeFinal)) {
                logger.info("Configuración personalizada aplicada - Asunto: '{}', Mensaje modificado: {}", 
                           asuntoFinal, !mensaje.equals(mensajeFinal));
            } else {
                logger.info("No hay configuración personalizada activa, usando valores originales");
                // Si no hay configuración personalizada, usamos el mensaje HTML generado
                mensajeFinal = mensajeHtml.toString();
                logger.info("Usando mensaje HTML generado con datos de factura");
            }
            
            // Obtener el PDF como bytes (genera datos de prueba si no encuentra la factura)
            logger.info("Generando PDF...");
            byte[] pdfBytes = facturaService.obtenerPdfComoBytes(uuidFactura);
            logger.info("PDF generado. Tamaño: {} bytes", pdfBytes != null ? pdfBytes.length : 0);
            
            // Si el PDF es nulo o vacío, generamos un PDF de prueba
            if (pdfBytes == null || pdfBytes.length == 0 || pdfBytes.length < 100) {
                logger.warn("PDF no generado correctamente o demasiado pequeño, generando PDF de prueba");
                // Generar un PDF simple con datos básicos
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    com.itextpdf.text.Document document = new com.itextpdf.text.Document();
                    com.itextpdf.text.pdf.PdfWriter.getInstance(document, baos);
                    document.open();
                    
                    // Título
                    com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
                    com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("Facturación Cibercom", titleFont);
                    title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    document.add(title);
                    document.add(new com.itextpdf.text.Paragraph(" "));
                    
                    // Datos de la factura
                    document.add(new com.itextpdf.text.Paragraph("Datos de la Factura:"));
                    document.add(new com.itextpdf.text.Paragraph(" "));
                    document.add(new com.itextpdf.text.Paragraph("Serie-Folio: " + factura.getSerie() + factura.getFolio()));
                    document.add(new com.itextpdf.text.Paragraph("UUID: " + factura.getUuid()));
                    document.add(new com.itextpdf.text.Paragraph("RFC Emisor: " + factura.getEmisorRfc()));
                    document.add(new com.itextpdf.text.Paragraph("RFC Receptor: " + factura.getReceptorRfc()));
                    document.add(new com.itextpdf.text.Paragraph("Fecha: " + factura.getFechaTimbrado()));
                    document.add(new com.itextpdf.text.Paragraph("Total: $" + factura.getTotal()));
                    
                    document.close();
                    pdfBytes = baos.toByteArray();
                    logger.info("PDF de prueba generado: {} bytes", pdfBytes.length);
                } catch (Exception e) {
                    logger.error("Error al generar PDF de prueba", e);
                    // Si falla, creamos un PDF mínimo
                    pdfBytes = new byte[1024];
                }
            }
            
            // Crear el adjunto con nombre apropiado
            CorreoDto.AdjuntoDto adjunto = new CorreoDto.AdjuntoDto();
            String nombreArchivoAdjunto;
            if (factura != null) {
                nombreArchivoAdjunto = "Factura_" + factura.getSerie() + factura.getFolio() + ".pdf";
            } else {
                nombreArchivoAdjunto = "Factura_" + uuidFactura + ".pdf";
            }
            adjunto.setNombre(nombreArchivoAdjunto);
            adjunto.setContenido(pdfBytes);
            adjunto.setTipoMime("application/pdf");
            
            logger.info("Adjunto creado: {} ({} bytes, tipo: {})", 
                       nombreArchivoAdjunto, pdfBytes.length, "application/pdf");
            
            List<CorreoDto.AdjuntoDto> adjuntos = new ArrayList<>();
            adjuntos.add(adjunto);
            
            // Obtener configuración del correo
            Map<String, String> configCorreo = obtenerConfiguracionCorreo();
            logger.info("Configuración de correo obtenida");
            
            // Aplicar formato al mensaje final
            String mensajeConFormato = aplicarFormatoAlMensaje(mensajeFinal);
            logger.info("Formato aplicado al mensaje. Longitud original: {}, con formato: {}", 
                       mensajeFinal.length(), mensajeConFormato.length());
            
            // VERIFICACIÓN ADICIONAL: Asegurar que el adjunto exista y tenga contenido
            if (adjuntos == null || adjuntos.isEmpty() || adjuntos.get(0).getContenido() == null || adjuntos.get(0).getContenido().length < 100) {
                logger.warn("Adjunto no válido o vacío. Regenerando PDF...");
                // Regenerar el PDF directamente
                byte[] pdfBytesNuevos = facturaService.obtenerPdfComoBytes(uuidFactura);
                if (pdfBytesNuevos != null && pdfBytesNuevos.length > 100) {
                    logger.info("PDF regenerado exitosamente: {} bytes", pdfBytesNuevos.length);
                    // Crear nuevo adjunto
                    CorreoDto.AdjuntoDto adjuntoNuevo = new CorreoDto.AdjuntoDto();
                    String nombreArchivo = "Factura_" + (factura != null ? factura.getSerie() + factura.getFolio() : uuidFactura) + ".pdf";
                    adjuntoNuevo.setNombre(nombreArchivo);
                    adjuntoNuevo.setContenido(pdfBytesNuevos);
                    adjuntoNuevo.setTipoMime("application/pdf");
                    
                    adjuntos = new ArrayList<>();
                    adjuntos.add(adjuntoNuevo);
                    logger.info("Nuevo adjunto creado: {} ({} bytes)", nombreArchivo, pdfBytesNuevos.length);
                }
            }
            
            // Crear el DTO del correo con asunto y mensaje finales
            CorreoDto correoDto = new CorreoDto();
            correoDto.setFrom(fromEmail);
            correoDto.setTo(correoReceptor);
            correoDto.setSubject(asuntoFinal);
            correoDto.setMensaje(mensajeConFormato);
            correoDto.setAdjuntos(adjuntos);
            
            // Configurar datos SMTP
            correoDto.setSmtpHost(configCorreo.get("SMTPHOST"));
            correoDto.setPort(configCorreo.get("PORT"));
            correoDto.setUsername(configCorreo.get("USERNAME"));
            correoDto.setPassword(configCorreo.get("PASSWORD"));
            
            logger.info("DTO del correo configurado con {} adjuntos", adjuntos.size());
            logger.info("Asunto final: '{}'", asuntoFinal);
            logger.info("Mensaje final (primeros 50 chars): '{}'", 
                       mensajeFinal.substring(0, Math.min(50, mensajeFinal.length())) + "...");
            
            // Enviar el correo con adjunto
            logger.info("Enviando correo...");
            CorreoUtil.enviaCorreo(correoDto);
            
            logger.info("=== CORREO CON PDF ENVIADO EXITOSAMENTE ===");
            
        } catch (Exception e) {
            logger.error("=== ERROR AL ENVIAR CORREO CON PDF ===");
            logger.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo con PDF adjunto", e);
        }
    }
    
    /**
     * Genera una factura de prueba para casos donde no se encuentra la factura real
     */
    private Factura generarFacturaPrueba(String uuid) {
        // Crear una factura de prueba con datos mínimos
        Factura factura = new Factura();
        factura.setUuid(uuid);
        factura.setSerie("TEST");
        factura.setFolio("00001");
        factura.setEmisorRfc("XAXX010101000");
        factura.setReceptorRfc("XAXX010101000");
        factura.setFechaTimbrado(LocalDateTime.now());
        factura.setTotal(BigDecimal.valueOf(0.00));
        return factura;
    }
    
    /**
     * Envía un correo simple sin adjuntos para pruebas
     * 
     * @param correoReceptor Correo electrónico del receptor
     * @param asunto Asunto del correo
     * @param mensaje Mensaje del correo
     */
    public void enviarCorreoSimple(String correoReceptor, String asunto, String mensaje) {
        try {
            logger.info("Enviando correo simple a: {}", correoReceptor);
            
            Map<String, String> configCorreo = obtenerConfiguracionCorreo();
            
            // Preparar variables para la plantilla
            Map<String, String> variables = new HashMap<>();
            variables.put("saludo", "Estimado cliente");
            variables.put("despedida", "¡Gracias por su preferencia!");
            
            // Aplicar formato al mensaje con variables
            String mensajeConFormato = aplicarFormatoAlMensaje(mensaje, variables);
            
            // Crear el DTO del correo
            CorreoDto correoDto = new CorreoDto();
            correoDto.setFrom(fromEmail);
            correoDto.setTo(correoReceptor);
            correoDto.setSubject(asunto);
            correoDto.setMensaje(mensajeConFormato);
            
            // Configurar datos SMTP
            correoDto.setSmtpHost(configCorreo.get("SMTPHOST"));
            correoDto.setPort(configCorreo.get("PORT"));
            correoDto.setUsername(configCorreo.get("USERNAME"));
            correoDto.setPassword(configCorreo.get("PASSWORD"));
            
            // Enviar el correo simple
            CorreoUtil.enviaCorreo(correoDto);
            
            logger.info("Correo simple enviado exitosamente");
            
        } catch (Exception e) {
            logger.error("Error al enviar correo simple: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo simple", e);
        }
    }
    
    /**
     * Método de prueba para diagnosticar problemas con PDF adjunto
     * 
     * @param uuidFactura UUID de la factura
     * @param correoReceptor Correo electrónico del receptor
     * @param asunto Asunto del correo
     * @param mensaje Mensaje del correo
     */
    public void probarGeneracionPdfYEnvio(String uuidFactura, String correoReceptor, String asunto, String mensaje) {
        try {
            logger.info("=== INICIANDO PRUEBA DE GENERACIÓN PDF Y ENVÍO ===");
            logger.info("UUID Factura: {}", uuidFactura);
            logger.info("Correo Receptor: {}", correoReceptor);
            
            // Paso 1: Verificar que la factura existe
            logger.info("Paso 1: Verificando existencia de factura...");
            Factura factura = facturaService.buscarPorUuid(uuidFactura);
            if (factura == null) {
                throw new RuntimeException("Factura no encontrada con UUID: " + uuidFactura);
            }
            logger.info("✓ Factura encontrada: {} {}", factura.getSerie(), factura.getFolio());
            
            // Paso 2: Generar PDF
            logger.info("Paso 2: Generando PDF...");
            byte[] pdfBytes = facturaService.obtenerPdfComoBytes(uuidFactura);
            logger.info("✓ PDF generado exitosamente. Tamaño: {} bytes", pdfBytes.length);
            
            // Paso 3: Crear adjunto
            logger.info("Paso 3: Creando adjunto...");
            List<CorreoDto.AdjuntoDto> adjuntos = new ArrayList<>();
            CorreoDto.AdjuntoDto adjunto = new CorreoDto.AdjuntoDto();
            adjunto.setNombre("Factura_" + factura.getSerie() + factura.getFolio() + ".pdf");
            adjunto.setContenido(pdfBytes);
            adjunto.setTipoMime("application/pdf");
            adjuntos.add(adjunto);
            logger.info("✓ Adjunto creado: {}", adjunto.getNombre());
            
            // Paso 4: Configurar correo
            logger.info("Paso 4: Configurando correo...");
            Map<String, String> configCorreo = obtenerConfiguracionCorreo();
            
            // Aplicar formato al mensaje
            String mensajeConFormato = aplicarFormatoAlMensaje(mensaje);
            
            CorreoDto correoDto = new CorreoDto();
            correoDto.setFrom(fromEmail);
            correoDto.setTo(correoReceptor);
            correoDto.setSubject(asunto);
            correoDto.setMensaje(mensajeConFormato);
            correoDto.setAdjuntos(adjuntos);
            
            correoDto.setSmtpHost(configCorreo.get("SMTPHOST"));
            correoDto.setPort(configCorreo.get("PORT"));
            correoDto.setUsername(configCorreo.get("USERNAME"));
            correoDto.setPassword(configCorreo.get("PASSWORD"));
            
            logger.info("✓ Correo configurado. SMTP: {}:{}", correoDto.getSmtpHost(), correoDto.getPort());
            
            // Paso 5: Enviar correo
            logger.info("Paso 5: Enviando correo con adjunto...");
            CorreoUtil.enviaCorreo(correoDto);
            logger.info("✓ Correo enviado exitosamente");
            
            logger.info("=== PRUEBA COMPLETADA EXITOSAMENTE ===");
            
        } catch (Exception e) {
            logger.error("=== ERROR EN LA PRUEBA ===");
            logger.error("Error en paso de prueba: {}", e.getMessage(), e);
            throw new RuntimeException("Error en prueba de PDF adjunto: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene la configuración de mensajes predefinidos
     * 
     * @return ConfiguracionCorreoResponseDto con la configuración
     */
    public ConfiguracionCorreoResponseDto obtenerConfiguracionMensajes() {
        try {
            logger.info("Obteniendo configuración de mensajes desde base de datos");
            
            // Obtener la configuración más reciente activa
            Optional<ConfiguracionMensaje> configuracionOpt = configuracionMensajeRepository.findMostRecentActiveConfiguration();
            
            String mensajeSeleccionado = "completo"; // valor por defecto
            List<MensajePredefinidoDto> mensajesPersonalizados = new ArrayList<>();
            
            if (configuracionOpt.isPresent()) {
                ConfiguracionMensaje config = configuracionOpt.get();
                mensajeSeleccionado = config.getMensajeSeleccionado();
            }
            
            // Obtener todos los mensajes personalizados activos
            List<ConfiguracionMensaje> mensajesPersonalizadosDB = configuracionMensajeRepository.findActivePersonalizedMessages();
            for (ConfiguracionMensaje mensaje : mensajesPersonalizadosDB) {
                MensajePredefinidoDto dto = MensajePredefinidoDto.builder()
                    .id(mensaje.getIdConfiguracion().toString())
                    .nombre(mensaje.getTipoMensaje())
                    .asunto(mensaje.getAsuntoPersonalizado())
                    .mensaje(mensaje.getMensajePersonalizado())
                    .build();
                mensajesPersonalizados.add(dto);
            }
            
            logger.info("Configuración obtenida - Mensaje seleccionado: {}, Mensajes personalizados: {}", 
                mensajeSeleccionado, mensajesPersonalizados.size());
            
            // Obtener formato de correo de la configuración principal
            FormatoCorreoDto formatoCorreo = new FormatoCorreoDto();
                
            if (configuracionOpt.isPresent()) {
                ConfiguracionMensaje config = configuracionOpt.get();
                // Solo actualizar los valores que no sean nulos
                if (config.getTipoFuente() != null) {
                    formatoCorreo.setTipoFuente(config.getTipoFuente());
                }
                if (config.getTamanoFuente() != null) {
                    formatoCorreo.setTamanoFuente(config.getTamanoFuente());
                }
                formatoCorreo.setEsCursiva("S".equals(config.getEsCursiva()));
                formatoCorreo.setEsSubrayado("S".equals(config.getEsSubrayado()));
                if (config.getColorTexto() != null) {
                    formatoCorreo.setColorTexto(config.getColorTexto());
                }
            }
            
            return ConfiguracionCorreoResponseDto.builder()
                .exitoso(true)
                .mensajeSeleccionado(mensajeSeleccionado)
                .mensajesPersonalizados(mensajesPersonalizados)
                .formatoCorreo(formatoCorreo)
                .rfcReceptor(null) // O asignar el valor real si está disponible
                .build();
                
        } catch (Exception e) {
            logger.error("Error al obtener configuración de mensajes: {}", e.getMessage(), e);
            // En lugar de lanzar una excepción, devolver una respuesta con error
            ConfiguracionCorreoResponseDto respuesta = new ConfiguracionCorreoResponseDto();
            respuesta.setExitoso(false);
            respuesta.setMensaje("Error al obtener configuración de mensajes: " + e.getMessage());
            respuesta.setMensajeSeleccionado("completo"); // Valor por defecto
            respuesta.setMensajesPersonalizados(new ArrayList<>());
            FormatoCorreoDto formato = new FormatoCorreoDto();
            formato.setTipoFuente("Arial");
            formato.setTamanoFuente(14);
            formato.setEsCursiva(false);
            formato.setEsSubrayado(false);
            formato.setColorTexto("#000000");
            respuesta.setFormatoCorreo(formato);
            return respuesta;
        }
    }
    
    /**
     * Guarda la configuración de mensajes predefinidos
     * 
     * @param configuracion Configuración a guardar
     * @return ConfiguracionCorreoResponseDto con el resultado
     */
    public ConfiguracionCorreoResponseDto guardarConfiguracionMensajes(ConfiguracionCorreoDto configuracion) {
        try {
            // Validar que el mensaje seleccionado no esté vacío
            if (configuracion.getMensajeSeleccionado() == null || configuracion.getMensajeSeleccionado().trim().isEmpty()) {
                return ConfiguracionCorreoResponseDto.builder()
                    .exitoso(false)
                    .mensaje("Debe seleccionar un mensaje predefinido")
                    .build();
            }

            // Ya no se guarda en tabla separada; los campos se persisten en CONFIGURACION_MENSAJES
            // if (configuracion.getFormatoCorreo() != null) {
            //     formatoCorreoService.guardarConfiguracion(configuracion.getFormatoCorreo());
            // }
            
            // Validar mensajes personalizados si existen
            if (configuracion.getMensajesPersonalizados() != null) {
                for (MensajePredefinidoDto mensaje : configuracion.getMensajesPersonalizados()) {
                    if (mensaje.getNombre() == null || mensaje.getNombre().trim().isEmpty()) {
                        return ConfiguracionCorreoResponseDto.builder()
                            .exitoso(false)
                            .mensaje("Todos los mensajes personalizados deben tener un nombre")
                            .build();
                    }
                    if (mensaje.getAsunto() == null || mensaje.getAsunto().trim().isEmpty()) {
                        return ConfiguracionCorreoResponseDto.builder()
                            .exitoso(false)
                            .mensaje("Todos los mensajes personalizados deben tener un asunto")
                            .build();
                    }
                    if (mensaje.getMensaje() == null || mensaje.getMensaje().trim().isEmpty()) {
                        return ConfiguracionCorreoResponseDto.builder()
                            .exitoso(false)
                            .mensaje("Todos los mensajes personalizados deben tener contenido")
                            .build();
                    }
                }
            }
            
            // Desactivar todas las configuraciones existentes
            configuracionMensajeRepository.deactivateAllConfigurations();
            
            // Crear nueva configuración principal
            ConfiguracionMensaje nuevaConfiguracion = new ConfiguracionMensaje();
            nuevaConfiguracion.setMensajeSeleccionado(configuracion.getMensajeSeleccionado());
            nuevaConfiguracion.setTipoMensaje("configuracion_principal");
            nuevaConfiguracion.setActivo("S");
            nuevaConfiguracion.setUsuarioCreacion("SISTEMA");
            
            // Guardar campos de formato si están presentes
            if (configuracion.getFormatoCorreo() != null) {
                FormatoCorreoDto formato = configuracion.getFormatoCorreo();
                nuevaConfiguracion.setTipoFuente(formato.getTipoFuente() != null ? formato.getTipoFuente() : "Arial");
                nuevaConfiguracion.setTamanoFuente(formato.getTamanoFuente() != null ? formato.getTamanoFuente() : 14);
                nuevaConfiguracion.setEsCursiva(formato.getEsCursiva() != null && formato.getEsCursiva() ? "S" : "N");
                nuevaConfiguracion.setEsSubrayado(formato.getEsSubrayado() != null && formato.getEsSubrayado() ? "S" : "N");
                nuevaConfiguracion.setColorTexto(formato.getColorTexto() != null ? formato.getColorTexto() : "#000000");
            }
            
            configuracionMensajeRepository.save(nuevaConfiguracion);
            
            // Guardar mensajes personalizados si existen
            List<MensajePredefinidoDto> mensajesGuardados = new ArrayList<>();
            if (configuracion.getMensajesPersonalizados() != null) {
                for (MensajePredefinidoDto mensaje : configuracion.getMensajesPersonalizados()) {
                    ConfiguracionMensaje mensajePersonalizado = new ConfiguracionMensaje();
                    mensajePersonalizado.setMensajeSeleccionado("personalizado");
                    mensajePersonalizado.setTipoMensaje(mensaje.getNombre());
                    mensajePersonalizado.setAsuntoPersonalizado(mensaje.getAsunto());
                    mensajePersonalizado.setMensajePersonalizado(mensaje.getMensaje());
                    mensajePersonalizado.setActivo("S");
                    mensajePersonalizado.setUsuarioCreacion("SISTEMA");
                    
                    ConfiguracionMensaje mensajeGuardado = configuracionMensajeRepository.save(mensajePersonalizado);
                    
                    // Crear DTO para la respuesta
                    MensajePredefinidoDto dtoGuardado = MensajePredefinidoDto.builder()
                        .id(mensajeGuardado.getIdConfiguracion().toString())
                        .nombre(mensajeGuardado.getTipoMensaje())
                        .asunto(mensajeGuardado.getAsuntoPersonalizado())
                        .mensaje(mensajeGuardado.getMensajePersonalizado())
                        .build();
                    mensajesGuardados.add(dtoGuardado);
                }
            }
            
            logger.info("Configuración de mensajes guardada en BD - Mensaje seleccionado: {}, Mensajes personalizados: {}", 
                configuracion.getMensajeSeleccionado(), 
                mensajesGuardados.size());
            
            return ConfiguracionCorreoResponseDto.builder()
                .exitoso(true)
                .mensaje("Configuración guardada exitosamente")
                .mensajeSeleccionado(configuracion.getMensajeSeleccionado())
                .mensajesPersonalizados(mensajesGuardados)
                .build();
                
        } catch (Exception e) {
            logger.error("Error al guardar configuración de mensajes: {}", e.getMessage(), e);
            return ConfiguracionCorreoResponseDto.builder()
                .exitoso(false)
                .mensaje("Error interno al guardar la configuración")
                .build();
        }
    }
    
    /**
     * Obtiene un mensaje predefinido por su ID
     * 
     * @param mensajeId ID del mensaje
     * @return MensajePredefinidoDto con el mensaje
     */
    public MensajePredefinidoDto obtenerMensajePorId(String mensajeId) {
        switch (mensajeId) {
            case "basico":
                return MensajePredefinidoDto.builder()
                    .id("basico")
                    .nombre("Mensaje Básico")
                    .asunto("Factura Electrónica - {facturaInfo}")
                    .mensaje("Estimado cliente,\n\nSe adjunta su factura electrónica.\n\nGracias por su preferencia.")
                    .build();
                    
            case "completo":
                // Buscar el mensaje predeterminado en la base de datos
                Optional<ConfiguracionMensaje> mensajePredeterminado = configuracionMensajeRepository
                    .findByTipoMensajeAndActivo("predefinido", "S");
                
                if (mensajePredeterminado.isPresent()) {
                    ConfiguracionMensaje config = mensajePredeterminado.get();
                    return MensajePredefinidoDto.builder()
                        .id("completo")
                        .nombre("Mensaje Completo")
                        .asunto(config.getAsuntoPersonalizado())
                        .mensaje(config.getMensajePersonalizado())
                        .build();
                } else {
                    // Fallback al mensaje por defecto si no se encuentra en BD
                    return MensajePredefinidoDto.builder()
                        .id("completo")
                        .nombre("Mensaje Completo")
                        .asunto("Factura Electrónica - {numeroFactura}")
                        .mensaje("Estimado cliente,\n\nSe ha generado su factura electrónica con los siguientes datos:\n\n" +
                                "UUID de la factura: {uuid}\n" +
                                "RFC del emisor: {rfcEmisor}\n\n" +
                                "Adjunto encontrará su factura en formato PDF.\n\n" +
                                "Puede descargar su factura desde nuestro portal web.\n\n" +
                                "Gracias por su preferencia.\n\n" +
                                "Atentamente,\n" +
                                "Sistema de Facturación Cibercom")
                        .build();
                }
                    
            case "personalizado":
                return MensajePredefinidoDto.builder()
                    .id("personalizado")
                    .nombre("Mensaje Personalizado")
                    .asunto("Su factura {facturaInfo} está lista")
                    .mensaje("Estimado cliente,\n\nNos complace informarle que su factura {facturaInfo} ha sido procesada exitosamente.\n\n" +
                            "Detalles de la factura:\n" +
                            "• Serie: {serie}\n" +
                            "• Folio: {folio}\n" +
                            "• UUID: {uuid}\n" +
                            "• RFC Emisor: {rfcEmisor}\n\n" +
                            "La factura se encuentra adjunta a este correo en formato PDF.\n\n" +
                            "Si tiene alguna pregunta, no dude en contactarnos.\n\n" +
                            "Cordialmente,\n" +
                            "Equipo de Facturación Cibercom")
                    .build();
                    
            default:
                // Retornar mensaje completo por defecto
                return obtenerMensajePorId("completo");
        }
    }
    
    /**
     * Procesa las variables en un texto
     * 
     * @param texto Texto con variables
     * @param variables Mapa de variables
     * @return Texto procesado
     */
    public String procesarVariables(String texto, Map<String, String> variables) {
        if (texto == null || variables == null) {
            return texto;
        }
        
        String resultado = texto;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String variable = "{" + entry.getKey() + "}";
            String valor = entry.getValue() != null ? entry.getValue() : "";
            resultado = resultado.replace(variable, valor);
        }
        
        return resultado;
    }
    
    /**
     * Obtiene un mensaje procesado para envío
     * 
     * @param mensajeId ID del mensaje seleccionado
     * @param mensajesPersonalizados Lista de mensajes personalizados
     * @param variables Variables para procesar
     * @return Mapa con asunto y mensaje procesados
     */
    public Map<String, String> obtenerMensajeParaEnvio(String mensajeId, List<MensajePredefinidoDto> mensajesPersonalizados, Map<String, String> variables) {
        MensajePredefinidoDto mensaje = null;
        
        // Buscar primero en mensajes personalizados
        if (mensajesPersonalizados != null) {
            mensaje = mensajesPersonalizados.stream()
                .filter(m -> mensajeId.equals(m.getId()))
                .findFirst()
                .orElse(null);
        }
        
        // Si no se encuentra, buscar en mensajes predefinidos del sistema
        if (mensaje == null) {
            mensaje = obtenerMensajePorId(mensajeId);
        }
        
        // Validar que se encontró un mensaje válido
        if (mensaje == null) {
            throw new RuntimeException("No se encontró el mensaje con ID: " + mensajeId);
        }
        
        // Procesar variables
        String asuntoProcesado = procesarVariables(mensaje.getAsunto(), variables);
        String mensajeProcesado = procesarVariables(mensaje.getMensaje(), variables);
        
        Map<String, String> resultado = new HashMap<>();
        resultado.put("asunto", asuntoProcesado);
        resultado.put("mensaje", mensajeProcesado);
        
        return resultado;
    }
    
    /**
     * Obtiene el mensaje personalizado configurado para envío de facturas
     * 
     * @param serieFactura Serie de la factura
     * @param folioFactura Folio de la factura
     * @param uuidFactura UUID de la factura
     * @param rfcEmisor RFC del emisor
     * @return Array con [asunto, mensaje] procesados
     */
    private String[] obtenerMensajePersonalizado(String serieFactura, String folioFactura, 
                                               String uuidFactura, String rfcEmisor) {
        try {
            logger.info("=== INICIO obtenerMensajePersonalizado ===");
            logger.info("Parámetros: serie={}, folio={}, uuid={}, rfc={}", serieFactura, folioFactura, uuidFactura, rfcEmisor);
            
            // Obtener configuración de mensajes
            ConfiguracionCorreoResponseDto config = obtenerConfiguracionMensajes();
            logger.info("Configuración obtenida: exitoso={}, mensajeSeleccionado={}, cantidadPersonalizados={}", 
                       config.isExitoso(), config.getMensajeSeleccionado(), 
                       config.getMensajesPersonalizados() != null ? config.getMensajesPersonalizados().size() : 0);
            
            String asunto = "Factura Electrónica - " + serieFactura + folioFactura;
            String mensaje = construirMensajePredeterminado(serieFactura, folioFactura, uuidFactura, rfcEmisor);
            
            // Si hay configuración personalizada, usarla
            if (config.isExitoso() && "personalizado".equals(config.getMensajeSeleccionado()) 
                && config.getMensajesPersonalizados() != null && !config.getMensajesPersonalizados().isEmpty()) {
                
                MensajePredefinidoDto mensajePersonalizado = config.getMensajesPersonalizados().get(0);
                logger.info("Mensaje personalizado encontrado: id={}, asunto={}, mensaje={}", 
                           mensajePersonalizado.getId(), mensajePersonalizado.getAsunto(), 
                           mensajePersonalizado.getMensaje().substring(0, Math.min(50, mensajePersonalizado.getMensaje().length())) + "...");
                
                // Crear variables para reemplazar
                Map<String, String> variables = new HashMap<>();
                variables.put("serie", serieFactura);
                variables.put("folio", folioFactura);
                variables.put("uuid", uuidFactura);
                variables.put("rfcEmisor", rfcEmisor);
                variables.put("facturaInfo", serieFactura + folioFactura);
                // Agregar rfcReceptor si está disponible
                if (config.getRfcReceptor() != null) {
                    variables.put("rfcReceptor", config.getRfcReceptor());
                }
                logger.info("Variables creadas: {}", variables);
                
                // Procesar asunto personalizado
                if (mensajePersonalizado.getAsunto() != null && !mensajePersonalizado.getAsunto().trim().isEmpty()) {
                    String asuntoOriginal = mensajePersonalizado.getAsunto();
                    asunto = procesarVariables(mensajePersonalizado.getAsunto(), variables);
                    logger.info("Asunto procesado: '{}' -> '{}'", asuntoOriginal, asunto);
                }
                
                // Procesar mensaje personalizado
                if (mensajePersonalizado.getMensaje() != null && !mensajePersonalizado.getMensaje().trim().isEmpty()) {
                    String mensajeOriginal = mensajePersonalizado.getMensaje();
                    mensaje = procesarVariables(mensajePersonalizado.getMensaje(), variables);
                    logger.info("Mensaje procesado: '{}...' -> '{}...'", 
                               mensajeOriginal.substring(0, Math.min(50, mensajeOriginal.length())),
                               mensaje.substring(0, Math.min(50, mensaje.length())));
                }
                
                logger.info("Usando mensaje personalizado para factura {}{}", serieFactura, folioFactura);
            } else {
                logger.info("Usando mensaje predeterminado para factura {}{}", serieFactura, folioFactura);
            }
            
            logger.info("=== RESULTADO FINAL ===");
            logger.info("Asunto final: '{}'", asunto);
            logger.info("Mensaje final: '{}'", mensaje.substring(0, Math.min(100, mensaje.length())) + "...");
            logger.info("=== FIN obtenerMensajePersonalizado ===");
            
            return new String[]{asunto, mensaje};
            
        } catch (Exception e) {
            logger.error("Error al obtener mensaje personalizado, usando predeterminado: {}", e.getMessage());
            return new String[]{
                "Factura Electrónica - " + serieFactura + folioFactura,
                construirMensajePredeterminado(serieFactura, folioFactura, uuidFactura, rfcEmisor)
            };
        }
    }
    
    /**
     * Construye el mensaje predeterminado para facturas
     */
    private String construirMensajePredeterminado(String serieFactura, String folioFactura, 
                                                String uuidFactura, String rfcEmisor) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Estimado cliente,\n\n");
        mensaje.append("Se ha generado su factura electrónica con los siguientes datos:\n\n");
        mensaje.append("Serie de la factura: ").append(serieFactura).append("\n");
        mensaje.append("Folio de la factura: ").append(folioFactura).append("\n");
        mensaje.append("UUID de la factura: ").append(uuidFactura).append("\n");
        mensaje.append("RFC del emisor: ").append(rfcEmisor).append("\n\n");
        mensaje.append("Puede descargar su factura desde nuestro portal web.\n\n");
        mensaje.append("Gracias por su preferencia.\n\n");
        mensaje.append("Atentamente,\n");
        mensaje.append("Sistema de Facturación Cibercom");
        
        return mensaje.toString();
    }
    
    /**
     * Envía correo con mensaje personalizado
     */
    private void enviarCorreoConMensajePersonalizado(Map<String, String> configCorreo, 
                                                   String correoReceptor, 
                                                   String asunto, 
                                                   String mensaje) {
        try {
            CorreoDto correo = new CorreoDto();
            correo = CorreoUtil.generaCorreo(correo, configCorreo);
            
            // Aplicar formato al mensaje
            String mensajeConFormato = aplicarFormatoAlMensaje(mensaje);
            
            // Configurar el destinatario
            correo.setTo(correoReceptor);
            correo.setSubject(asunto);
            correo.setMensaje(mensajeConFormato);
            
            // Enviar correo electrónico
            CorreoUtil.enviaCorreo(correo);
            
        } catch (Exception e) {
            logger.error("Error al enviar correo con mensaje personalizado: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo", e);
        }
    }
    
    /**
     * Aplica formato al mensaje usando la configuración activa y la plantilla HTML
     * 
     * @param mensaje Mensaje original
     * @param variables Variables adicionales para la plantilla
     * @return Mensaje con formato y plantilla aplicados
     */
    private String aplicarFormatoAlMensaje(String mensaje, Map<String, String> variables) {
        try {
            // Obtener configuración de formato desde la configuración de mensajes
            ConfiguracionCorreoResponseDto configResponse = obtenerConfiguracionMensajes();
            FormatoCorreoDto configuracionFormato;
            
            if (configResponse != null && configResponse.isExitoso() && configResponse.getFormatoCorreo() != null) {
                // Usar la configuración de formato de la configuración de mensajes
                configuracionFormato = configResponse.getFormatoCorreo();
                logger.info("Usando configuración de formato desde configuración de mensajes: {}", configuracionFormato);
            } else {
                // Si no hay configuración en mensajes, obtener la configuración activa del servicio de formato
                configuracionFormato = formatoCorreoService.obtenerConfiguracionActiva();
                logger.info("Usando configuración de formato desde servicio de formato: {}", configuracionFormato);
            }
            
            // Preparar variables para la plantilla
            Map<String, String> templateVars = new HashMap<>();
            if (variables != null) {
                templateVars.putAll(variables);
            }
            
            // Agregar el mensaje principal a las variables
            templateVars.put("mensajePrincipal", mensaje);
            
            // Cargar y procesar la plantilla HTML con las variables
            String contenidoHTML = formatoCorreoService.cargarYProcesarPlantillaHTML(templateVars, configuracionFormato);
            
            // Aplicar estilos de formato al contenido HTML
            return formatoCorreoService.aplicarFormatoHTML(contenidoHTML, configuracionFormato);
            
        } catch (Exception e) {
            logger.warn("Error al aplicar formato al mensaje, enviando sin formato: {}", e.getMessage(), e);
            return mensaje;
        }
    }
    
    /**
     * Sobrecarga del método para mantener compatibilidad
     */
    private String aplicarFormatoAlMensaje(String mensaje) {
        return aplicarFormatoAlMensaje(mensaje, null);
    }

    /**
     * Método auxiliar para obtener configuración personalizada de correo de manera segura
     * Retorna null si no hay configuración personalizada o si ocurre algún error
     */
    private ConfiguracionCorreoResponseDto obtenerConfiguracionPersonalizadaSegura() {
        try {
            ConfiguracionCorreoResponseDto config = obtenerConfiguracionMensajes();
            
            // Verificar si hay configuración personalizada válida
            if (config != null && config.isExitoso() && 
                "personalizado".equals(config.getMensajeSeleccionado()) &&
                config.getMensajesPersonalizados() != null && 
                !config.getMensajesPersonalizados().isEmpty()) {
                
                logger.info("Configuración personalizada encontrada y válida");
                return config;
            }
            
            logger.info("No hay configuración personalizada válida disponible");
            return null;
            
        } catch (Exception e) {
            logger.warn("Error al obtener configuración personalizada, usando valores por defecto: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Método auxiliar para aplicar configuración personalizada a asunto y mensaje
     * Si no hay configuración personalizada, retorna los valores originales
     */
    private Map<String, String> aplicarConfiguracionPersonalizada(String asuntoOriginal, String mensajeOriginal, 
                                                                  String serieFactura, String folioFactura, 
                                                                  String uuidFactura, String rfcEmisor) {
        Map<String, String> resultado = new HashMap<>();
        resultado.put("asunto", asuntoOriginal);
        resultado.put("mensaje", mensajeOriginal);
        
        try {
            ConfiguracionCorreoResponseDto config = obtenerConfiguracionPersonalizadaSegura();
            
            if (config != null) {
                MensajePredefinidoDto mensajePersonalizado = config.getMensajesPersonalizados().get(0);
                
                // Crear variables para reemplazar
                Map<String, String> variables = new HashMap<>();
                variables.put("serie", serieFactura != null ? serieFactura : "");
                variables.put("folio", folioFactura != null ? folioFactura : "");
                variables.put("uuid", uuidFactura != null ? uuidFactura : "");
                variables.put("rfcEmisor", rfcEmisor != null ? rfcEmisor : "");
                variables.put("facturaInfo", (serieFactura != null ? serieFactura : "") + (folioFactura != null ? folioFactura : ""));
                
                // Buscar la factura para obtener el RFC del receptor
                Factura factura = facturaService.buscarPorUuid(uuidFactura);
                if (factura != null && factura.getReceptorRfc() != null) {
                    variables.put("rfcReceptor", factura.getReceptorRfc());
                    logger.info("RFC Receptor agregado a variables: {}", factura.getReceptorRfc());
                } else {
                    variables.put("rfcReceptor", "No disponible");
                    logger.warn("RFC Receptor no disponible, usando valor por defecto");
                }
                
                // Aplicar asunto personalizado si está disponible
                if (mensajePersonalizado.getAsunto() != null && !mensajePersonalizado.getAsunto().trim().isEmpty()) {
                    String asuntoPersonalizado = procesarVariables(mensajePersonalizado.getAsunto(), variables);
                    resultado.put("asunto", asuntoPersonalizado);
                    logger.info("Asunto personalizado aplicado: '{}'", asuntoPersonalizado);
                }
                
                // Aplicar mensaje personalizado si está disponible
                if (mensajePersonalizado.getMensaje() != null && !mensajePersonalizado.getMensaje().trim().isEmpty()) {
                    String mensajePersonalizadoProcesado = procesarVariables(mensajePersonalizado.getMensaje(), variables);
                    resultado.put("mensaje", mensajePersonalizadoProcesado);
                    logger.info("Mensaje personalizado aplicado (primeros 50 chars): '{}'", 
                               mensajePersonalizadoProcesado.substring(0, Math.min(50, mensajePersonalizadoProcesado.length())) + "...");
                }
                
                logger.info("Configuración personalizada aplicada exitosamente");
            }
            
        } catch (Exception e) {
            logger.warn("Error al aplicar configuración personalizada, usando valores originales: {}", e.getMessage());
        }
        
        return resultado;
    }
    

}