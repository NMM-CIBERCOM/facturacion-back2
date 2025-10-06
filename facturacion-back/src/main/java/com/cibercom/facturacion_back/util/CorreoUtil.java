// Clase: CorreoUtil
package com.cibercom.facturacion_back.util;

import com.cibercom.facturacion_back.dto.CorreoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.activation.*;
import jakarta.mail.util.ByteArrayDataSource;

/**
 * Utilidad para envío de correos electrónicos.
 */
public class CorreoUtil {

    private static final Logger logger = LoggerFactory.getLogger(CorreoUtil.class);

    public static void enviaCorreo(CorreoDto correo) {
        if (correo == null) {
            throw new IllegalArgumentException("El objeto CorreoDto no puede ser nulo");
        }
        if (correo.getTo() == null || correo.getTo().trim().isEmpty()) {
            throw new IllegalArgumentException("El destinatario (to) es obligatorio");
        }
        if (correo.getSubject() == null) {
            correo.setSubject("");
        }
        if (correo.getMensaje() == null) {
            correo.setMensaje("");
        }

        logger.info("Enviando correo:");
        logger.info("De: {}", correo.getFrom());
        logger.info("Para: {}", correo.getTo());
        logger.info("Asunto: {}", correo.getSubject());
        logger.info("Mensaje ({} chars)", correo.getMensaje().length());
        int adjuntos = (correo.getAdjuntos() != null) ? correo.getAdjuntos().size() : 0;
        logger.info("Adjuntos: {}", adjuntos);
        logger.info("SMTP: {}:{} (usuario={})", correo.getSmtpHost(), correo.getPort(), correo.getUsername());

        try {
            // Configuración de propiedades para el servidor SMTP
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", correo.getSmtpHost());
            props.put("mail.smtp.port", correo.getPort());
            
            // Crear sesión con autenticación
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(correo.getUsername(), correo.getPassword());
                }
            });
            
            // Crear mensaje
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(correo.getFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(correo.getTo()));
            message.setSubject(correo.getSubject());
            
            // Si hay adjuntos, crear multipart
            if (adjuntos > 0) {
                MimeMultipart multipart = new MimeMultipart("mixed"); // Especificar tipo "mixed" para adjuntos
                
                // Parte del texto
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setContent(correo.getMensaje(), "text/html; charset=utf-8");
                multipart.addBodyPart(textPart);
                
                // Agregar adjuntos
                for (CorreoDto.AdjuntoDto adjunto : correo.getAdjuntos()) {
                    if (adjunto.getContenido() != null && adjunto.getContenido().length > 0) {
                        MimeBodyPart attachmentPart = new MimeBodyPart();
                        DataSource source = new ByteArrayDataSource(adjunto.getContenido(), adjunto.getTipoMime());
                        attachmentPart.setDataHandler(new DataHandler(source));
                        attachmentPart.setFileName(adjunto.getNombre());
                        attachmentPart.setDisposition(MimeBodyPart.ATTACHMENT); // Forzar como adjunto
                        multipart.addBodyPart(attachmentPart);
                        logger.info("Adjunto añadido: {} ({} bytes)", adjunto.getNombre(), adjunto.getContenido().length);
                    } else {
                        logger.warn("Adjunto ignorado porque está vacío: {}", adjunto.getNombre());
                    }
                }
                
                message.setContent(multipart);
            } else {
                // Sin adjuntos, solo texto
                message.setContent(correo.getMensaje(), "text/html; charset=utf-8");
            }
            
            // Enviar mensaje
            Transport.send(message);
            logger.info("Correo enviado exitosamente a {}", correo.getTo());
        } catch (Exception e) {
            logger.error("Error al enviar correo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    // NUEVO: genera el objeto CorreoDto rellenando configuración SMTP y remitente desde el mapa
    public static CorreoDto generaCorreo(CorreoDto correo, Map<String, String> configCorreo) {
        if (correo == null) {
            correo = new CorreoDto();
        }
        if (configCorreo != null) {
            // Rellenar FROM y configuración SMTP
            if (configCorreo.containsKey("FROM")) {
                correo.setFrom(configCorreo.get("FROM"));
            }
            if (configCorreo.containsKey("SMTPHOST")) {
                correo.setSmtpHost(configCorreo.get("SMTPHOST"));
            }
            if (configCorreo.containsKey("PORT")) {
                correo.setPort(configCorreo.get("PORT"));
            }
            if (configCorreo.containsKey("USERNAME")) {
                correo.setUsername(configCorreo.get("USERNAME"));
            }
            if (configCorreo.containsKey("PASSWORD")) {
                correo.setPassword(configCorreo.get("PASSWORD"));
            }
        }
        return correo;
    }
}