package com.cibercom.facturacion_back.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cibercom.facturacion_back.dto.FacturaDTO;
import com.cibercom.facturacion_back.dto.ConfiguracionCorreoDto;
import com.cibercom.facturacion_back.dto.ConfiguracionCorreoResponseDto;
import com.cibercom.facturacion_back.model.Factura;
import com.cibercom.facturacion_back.service.CorreoService;
import com.cibercom.facturacion_back.service.FacturaService;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para el envío de correos electrónicos
 */
@RestController
@RequestMapping("/api/correo")
@CrossOrigin(origins = "*")
public class CorreoController {
    
    private static final Logger logger = LoggerFactory.getLogger(CorreoController.class);
    
    @Autowired
    private CorreoService correoService;
    
    @Autowired
    private FacturaService facturaService;
    

    
    /**
     * Envía correo de factura por UUID de factura
     * 
     * @param facturaUuid UUID de la factura
     * @param correoReceptor Correo del receptor
     * @return ResponseEntity con el resultado
     */
    @PostMapping("/enviar-factura/{facturaUuid}")
    public ResponseEntity<Map<String, Object>> enviarCorreoFactura(
            @PathVariable String facturaUuid,
            @RequestParam String correoReceptor) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validar formato del correo
            if (!correoService.validarEmail(correoReceptor)) {
                response.put("success", false);
                response.put("message", "El formato del correo electrónico no es válido");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verificar configuración de correo, permitir envío simulado si incompleta
            boolean configuracionCompleta = correoService.verificarConfiguracionCorreo();
            if (!configuracionCompleta) {
                logger.warn("La configuración de correo no está completa; se continuará con envío simulado.");
                response.put("success", true);
                response.put("simulado", true);
                response.put("message", "Envío de correo simulado por configuración incompleta");
                response.put("facturaUuid", facturaUuid);
                response.put("correoReceptor", correoReceptor);
                return ResponseEntity.ok(response);
            }
            
            // Buscar la factura
            Factura factura = facturaService.buscarPorUuid(facturaUuid);
            if (factura == null) {
                response.put("success", false);
                response.put("message", "Factura no encontrada");
                return ResponseEntity.notFound().build();
            }
            
            // Enviar correo
            correoService.enviarCorreoFactura(factura, correoReceptor);
            
            response.put("success", true);
            response.put("message", "Correo enviado exitosamente");
            response.put("facturaUuid", facturaUuid);
            response.put("correoReceptor", correoReceptor);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al enviar correo de factura: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno al enviar el correo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Envía correo de factura con parámetros directos
     * 
     * @param request Datos de la factura y correo
     * @return ResponseEntity con el resultado
     */
    @PostMapping("/enviar-factura-directa")
    public ResponseEntity<Map<String, Object>> enviarCorreoFacturaDirecta(
            @RequestBody EnvioCorreoRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validar formato del correo
            if (!correoService.validarEmail(request.getCorreoReceptor())) {
                response.put("success", false);
                response.put("message", "El formato del correo electrónico no es válido");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verificar configuración de correo, permitir envío simulado si incompleta
            boolean configuracionCompletaDirecta = correoService.verificarConfiguracionCorreo();
            if (!configuracionCompletaDirecta) {
                logger.warn("La configuración de correo no está completa; se continuará con envío simulado (directo).");
                response.put("success", true);
                response.put("simulado", true);
                response.put("message", "Envío de correo simulado por configuración incompleta");
                response.put("factura", request.getSerieFactura() + request.getFolioFactura());
                response.put("correoReceptor", request.getCorreoReceptor());
                return ResponseEntity.ok(response);
            }
            
            // Enviar correo
            correoService.enviarCorreoFactura(
                request.getSerieFactura(),
                request.getFolioFactura(),
                request.getUuidFactura(),
                request.getRfcEmisor(),
                request.getCorreoReceptor()
            );
            
            response.put("success", true);
            response.put("message", "Correo enviado exitosamente");
            response.put("factura", request.getSerieFactura() + request.getFolioFactura());
            response.put("correoReceptor", request.getCorreoReceptor());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al enviar correo de factura: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno al enviar el correo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Envía correo de factura con PDF adjunto
     * 
     * @param request Datos del correo con PDF adjunto
     * @return ResponseEntity con el resultado
     */
    @PostMapping("/enviar-con-pdf")
    public ResponseEntity<Map<String, Object>> enviarCorreoConPdf(
            @RequestBody EnvioCorreoPdfRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validar formato del correo
            if (!correoService.validarEmail(request.getCorreoReceptor())) {
                response.put("success", false);
                response.put("message", "El formato del correo electrónico no es válido");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verificar configuración de correo, permitir envío simulado si incompleta
            boolean configuracionCompletaPdf = correoService.verificarConfiguracionCorreo();
            if (!configuracionCompletaPdf) {
                logger.warn("La configuración de correo no está completa; se continuará con envío simulado (PDF adjunto).");
                response.put("success", true);
                response.put("simulado", true);
                response.put("message", "Envío de correo con PDF simulado por configuración incompleta");
                response.put("facturaUuid", request.getUuidFactura());
                response.put("correoReceptor", request.getCorreoReceptor());
                return ResponseEntity.ok(response);
            }
            
            // Enviar correo con PDF adjunto
            // Usar mensaje si cuerpo es nulo
            String contenidoCorreo = request.getCuerpo();
            if (contenidoCorreo == null || contenidoCorreo.trim().isEmpty()) {
                contenidoCorreo = request.getMensaje();
                logger.info("Usando campo 'mensaje' en lugar de 'cuerpo' que está vacío");
            }
            
            logger.info("Enviando correo con contenido: {}", contenidoCorreo);
            
            // Obtener el PDF directamente y verificar que sea válido
            byte[] pdfBytes = facturaService.obtenerPdfComoBytes(request.getUuidFactura());
            if (pdfBytes == null || pdfBytes.length < 100) {
                logger.error("PDF inválido o demasiado pequeño: {} bytes", pdfBytes != null ? pdfBytes.length : 0);
                response.put("success", false);
                response.put("message", "No se pudo generar el PDF para adjuntar al correo");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            logger.info("PDF obtenido correctamente: {} bytes", pdfBytes.length);
            correoService.enviarCorreoConPdfAdjunto(
                request.getUuidFactura(),
                request.getCorreoReceptor(),
                request.getAsunto(),
                contenidoCorreo
            );
            
            response.put("success", true);
            response.put("message", "Correo con PDF adjunto enviado exitosamente");
            response.put("facturaUuid", request.getUuidFactura());
            response.put("correoReceptor", request.getCorreoReceptor());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al enviar correo con PDF adjunto: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno al enviar el correo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Verifica la configuración del correo
     * 
     * @return ResponseEntity con el estado de la configuración
     */
    @GetMapping("/verificar-configuracion")
    public ResponseEntity<Map<String, Object>> verificarConfiguracion() {
        Map<String, Object> response = new HashMap<>();
        
        boolean configuracionCompleta = correoService.verificarConfiguracionCorreo();
        
        response.put("configuracionCompleta", configuracionCompleta);
        response.put("message", configuracionCompleta ? 
            "Configuración de correo completa" : 
            "Configuración de correo incompleta");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint de prueba para enviar correo simple sin adjuntos
     * 
     * @param request Datos básicos del correo
     * @return ResponseEntity con el resultado
     */
    @PostMapping("/prueba-correo-simple")
    public ResponseEntity<Map<String, Object>> pruebaCorreoSimple(
            @RequestBody PruebaCorreoRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validar formato del correo
            if (!correoService.validarEmail(request.getCorreoReceptor())) {
                response.put("success", false);
                response.put("message", "El formato del correo electrónico no es válido");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verificar configuración de correo
            if (!correoService.verificarConfiguracionCorreo()) {
                response.put("success", false);
                response.put("message", "La configuración de correo no está completa");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            // Enviar correo simple sin adjuntos
            correoService.enviarCorreoSimple(
                request.getCorreoReceptor(),
                request.getAsunto(),
                request.getMensaje()
            );
            
            response.put("success", true);
            response.put("message", "Correo de prueba enviado exitosamente");
            response.put("correoReceptor", request.getCorreoReceptor());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al enviar correo de prueba: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno al enviar el correo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Endpoint de prueba para verificar la generación de PDF y adjunto
     * 
     * @param request Datos del correo con PDF adjunto
     * @return ResponseEntity con el resultado
     */
    @PostMapping("/prueba-pdf-adjunto")
    public ResponseEntity<Map<String, Object>> pruebaPdfAdjunto(
            @RequestBody EnvioCorreoPdfRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validar formato del correo
            if (!correoService.validarEmail(request.getCorreoReceptor())) {
                response.put("success", false);
                response.put("message", "El formato del correo electrónico no es válido");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verificar configuración de correo
            if (!correoService.verificarConfiguracionCorreo()) {
                response.put("success", false);
                response.put("message", "La configuración de correo no está completa");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            // Probar la generación del PDF
            correoService.probarGeneracionPdfYEnvio(
                request.getUuidFactura(),
                request.getCorreoReceptor(),
                request.getAsunto(),
                request.getMensaje()
            );
            
            response.put("success", true);
            response.put("message", "Prueba de PDF y envío completada exitosamente");
            response.put("correoReceptor", request.getCorreoReceptor());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error en prueba de PDF adjunto: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error en la prueba: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Clase interna para el request de envío de correo directo
     */
    public static class EnvioCorreoRequest {
        private String serieFactura;
        private String folioFactura;
        private String uuidFactura;
        private String rfcEmisor;
        private String correoReceptor;
        
        // Getters y setters
        public String getSerieFactura() { return serieFactura; }
        public void setSerieFactura(String serieFactura) { this.serieFactura = serieFactura; }
        
        public String getFolioFactura() { return folioFactura; }
        public void setFolioFactura(String folioFactura) { this.folioFactura = folioFactura; }
        
        public String getUuidFactura() { return uuidFactura; }
        public void setUuidFactura(String uuidFactura) { this.uuidFactura = uuidFactura; }
        
        public String getRfcEmisor() { return rfcEmisor; }
        public void setRfcEmisor(String rfcEmisor) { this.rfcEmisor = rfcEmisor; }
        
        public String getCorreoReceptor() { return correoReceptor; }
        public void setCorreoReceptor(String correoReceptor) { this.correoReceptor = correoReceptor; }
    }
    
    /**
     * Clase interna para el request de envío de correo con PDF adjunto
     */
    public static class EnvioCorreoPdfRequest {
        private String uuidFactura;
        private String correoReceptor;
        private String asunto;
        private String mensaje;
        private String cuerpo;
        
        // Getters y setters
        public String getUuidFactura() { return uuidFactura; }
        public void setUuidFactura(String uuidFactura) { this.uuidFactura = uuidFactura; }
        
        public String getCorreoReceptor() { return correoReceptor; }
        public void setCorreoReceptor(String correoReceptor) { this.correoReceptor = correoReceptor; }
        
        public String getAsunto() { return asunto; }
        public void setAsunto(String asunto) { this.asunto = asunto; }
        
        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
        
        public String getCuerpo() { return cuerpo != null ? cuerpo : mensaje; }
        public void setCuerpo(String cuerpo) { this.cuerpo = cuerpo; }
        
        // Getter for mensaje already defined above
        // Setter for mensaje already defined above
    }
    
    /**
     * Clase interna para el request de prueba de correo simple
     */
    public static class PruebaCorreoRequest {
        private String correoReceptor;
        private String asunto;
        private String mensaje;
        
        // Getters y setters
        public String getCorreoReceptor() { return correoReceptor; }
        public void setCorreoReceptor(String correoReceptor) { this.correoReceptor = correoReceptor; }
        
        public String getAsunto() { return asunto; }
        public void setAsunto(String asunto) { this.asunto = asunto; }
        
        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    }
    
    /**
     * Obtiene la configuración de mensajes predeterminados
     * 
     * @return ResponseEntity con la configuración
     */
    @GetMapping("/configuracion-mensajes")
    public ResponseEntity<ConfiguracionCorreoResponseDto> obtenerConfiguracionMensajes() {
        try {
            // Usar CorreoService para obtener la configuración de mensajes
            ConfiguracionCorreoResponseDto configuracion = correoService.obtenerConfiguracionMensajes();
            return ResponseEntity.ok(configuracion);
        } catch (Exception e) {
            logger.error("Error al obtener configuración de mensajes: {}", e.getMessage(), e);
            
            // Retornar configuración predeterminada en caso de error
            ConfiguracionCorreoResponseDto configuracionDefault = ConfiguracionCorreoResponseDto.builder()
                .exitoso(true)
                .mensajeSeleccionado("completo")
                .mensajesPersonalizados(new ArrayList<>())
                .build();
            
            return ResponseEntity.ok(configuracionDefault);
        }
    }
    
    /**
     * Guarda la configuración de mensajes predeterminados
     * 
     * @param configuracion Configuración a guardar
     * @return ResponseEntity con el resultado
     */
    @PostMapping("/configuracion-mensajes")
    public ResponseEntity<ConfiguracionCorreoResponseDto> guardarConfiguracionMensajes(
            @RequestBody ConfiguracionCorreoDto configuracion) {
        try {
            ConfiguracionCorreoResponseDto response = correoService.guardarConfiguracionMensajes(configuracion);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al guardar configuración de mensajes: {}", e.getMessage(), e);
            
            ConfiguracionCorreoResponseDto errorResponse = ConfiguracionCorreoResponseDto.builder()
                .exitoso(false)
                .mensaje("Error al guardar la configuración: " + e.getMessage())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Obtiene un mensaje procesado para envío
     * 
     * @param request Datos para procesar el mensaje
     * @return ResponseEntity con el mensaje procesado
     */
    @PostMapping("/mensaje-para-envio")
    public ResponseEntity<Map<String, Object>> obtenerMensajeParaEnvio(
            @RequestBody Map<String, Object> request) {
        try {
            // Obtener configuración actual
            ConfiguracionCorreoResponseDto configuracion = correoService.obtenerConfiguracionMensajes();
            
            // Preparar variables
            Map<String, String> variables = new HashMap<>();
            variables.put("facturaInfo", (String) request.get("facturaInfo"));
            variables.put("serie", (String) request.get("serie"));
            variables.put("folio", (String) request.get("folio"));
            variables.put("uuid", (String) request.get("uuid"));
            variables.put("rfcEmisor", (String) request.get("rfcEmisor"));
            
            // Obtener mensaje procesado
            Map<String, String> mensajeProcesado = correoService.obtenerMensajeParaEnvio(
                configuracion.getMensajeSeleccionado(),
                configuracion.getMensajesPersonalizados(),
                variables
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("asunto", mensajeProcesado.get("asunto"));
            response.put("mensaje", mensajeProcesado.get("mensaje"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al obtener mensaje para envío: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al procesar el mensaje: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Obtiene el mensaje predeterminado completo
     * 
     * @return ResponseEntity con el mensaje predeterminado
     */
    @GetMapping("/mensaje/completo")
    public ResponseEntity<Map<String, Object>> obtenerMensajeCompleto() {
        try {
            logger.info("Obteniendo mensaje predeterminado completo");
            
            Map<String, Object> response = new HashMap<>();
            response.put("asunto", "Factura Electrónica - {facturaInfo}");
            response.put("mensaje", "Estimado cliente,\n\nSe ha generado su factura electrónica con los siguientes datos:\n\nSerie de la factura: {serie}\nFolio de la factura: {folio}\nUUID de la factura: {uuid}\nRFC del emisor: {rfcEmisor}\n\nPuede descargar su factura desde nuestro portal web.\n\nGracias por su preferencia.\n\nAtentamente,\nSistema de Facturación Cibercom");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al obtener mensaje completo: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error interno del servidor");
            errorResponse.put("message", "Ocurrió un error al obtener el mensaje predeterminado");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}