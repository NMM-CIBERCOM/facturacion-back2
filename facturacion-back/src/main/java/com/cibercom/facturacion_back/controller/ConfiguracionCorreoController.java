package com.cibercom.facturacion_back.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cibercom.facturacion_back.dto.ConfiguracionCorreoDto;
import com.cibercom.facturacion_back.dto.ConfiguracionCorreoResponseDto;
import com.cibercom.facturacion_back.dto.FormatoCorreoDto;
import com.cibercom.facturacion_back.dto.MensajePredefinidoDto;
import com.cibercom.facturacion_back.service.CorreoService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para la configuración de correos electrónicos
 */
@RestController
@RequestMapping("/api/configuracion-correo")
@CrossOrigin(origins = "*")
public class ConfiguracionCorreoController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfiguracionCorreoController.class);
    
    @Autowired
    private CorreoService correoService;
    
    /**
     * Obtiene la configuración de mensajes para el correo
     * 
     * @return ResponseEntity con la configuración de mensajes
     */
    @GetMapping("/obtener-configuracion-mensaje")
    public ResponseEntity<Map<String, Object>> obtenerConfiguracionMensaje() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Obtener configuración de mensajes (incluye formato)
            ConfiguracionCorreoResponseDto configuracionMensajes = correoService.obtenerConfiguracionMensajes();
            
            // Preparar respuesta con formato incluido
            response.put("success", true);
            response.put("data", Map.of(
                "asunto", configuracionMensajes.getMensajeSeleccionado() != null ? 
                    configuracionMensajes.getMensajeSeleccionado() : "Factura Electrónica",
                "mensaje", configuracionMensajes.getMensajeSeleccionado() != null ? 
                    configuracionMensajes.getMensajeSeleccionado() : "completo",
                "mensajePersonalizado", "",
                "esPersonalizado", false,
                "formatoCorreo", Map.of(
                    "tipoFuente", configuracionMensajes.getFormatoCorreo() != null ? configuracionMensajes.getFormatoCorreo().getTipoFuente() : "Arial",
                    "tamanoFuente", configuracionMensajes.getFormatoCorreo() != null ? configuracionMensajes.getFormatoCorreo().getTamanoFuente() : 14,
                    "esCursiva", configuracionMensajes.getFormatoCorreo() != null ? configuracionMensajes.getFormatoCorreo().getEsCursiva() : false,
                    "esSubrayado", configuracionMensajes.getFormatoCorreo() != null ? configuracionMensajes.getFormatoCorreo().getEsSubrayado() : false,
                    "esNegrita", configuracionMensajes.getFormatoCorreo() != null ? configuracionMensajes.getFormatoCorreo().getEsNegrita() : false,
                    "colorTexto", configuracionMensajes.getFormatoCorreo() != null ? configuracionMensajes.getFormatoCorreo().getColorTexto() : "#000000"
                )
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al obtener configuración de mensaje: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error al obtener la configuración: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Guarda la configuración de mensajes para el correo
     * 
     * @param configuracionRequest Configuración a guardar
     * @return ResponseEntity con el resultado
     */
    @PostMapping("/guardar-configuracion-mensaje")
    public ResponseEntity<Map<String, Object>> guardarConfiguracionMensaje(
            @RequestBody Map<String, Object> configuracionRequest) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extraer datos de la configuración
            String asunto = (String) configuracionRequest.get("asunto");
            String mensaje = (String) configuracionRequest.get("mensaje");
            String mensajePersonalizado = (String) configuracionRequest.get("mensajePersonalizado");
            Boolean esPersonalizado = (Boolean) configuracionRequest.get("esPersonalizado");
            
            // Crear configuración para guardar
            ConfiguracionCorreoDto configuracion = ConfiguracionCorreoDto.builder()
                .mensajeSeleccionado(esPersonalizado != null && esPersonalizado ? "personalizado" : mensaje)
                .mensajesPersonalizados(new ArrayList<>())
                .build();
            
            // Si es personalizado, agregar el mensaje personalizado
            if (esPersonalizado != null && esPersonalizado && mensajePersonalizado != null) {
                MensajePredefinidoDto mensajePersonalizadoDto = MensajePredefinidoDto.builder()
                    .nombre("personalizado")
                    .asunto(asunto)
                    .mensaje(mensajePersonalizado)
                    .build();
                configuracion.getMensajesPersonalizados().add(mensajePersonalizadoDto);
            }
            
            // Agregar configuración de formato si se proporciona
            @SuppressWarnings("unchecked")
            Map<String, Object> formatoCorreoMap = (Map<String, Object>) configuracionRequest.get("formatoCorreo");
            if (formatoCorreoMap != null) {
                FormatoCorreoDto formatoCorreo = new FormatoCorreoDto();
                formatoCorreo.setTipoFuente((String) formatoCorreoMap.get("tipoFuente"));
                formatoCorreo.setTamanoFuente((Integer) formatoCorreoMap.get("tamanoFuente"));
                formatoCorreo.setEsCursiva((Boolean) formatoCorreoMap.get("esCursiva"));
                formatoCorreo.setEsSubrayado((Boolean) formatoCorreoMap.get("esSubrayado"));
                formatoCorreo.setEsNegrita((Boolean) formatoCorreoMap.get("esNegrita"));
                formatoCorreo.setColorTexto((String) formatoCorreoMap.get("colorTexto"));
                configuracion.setFormatoCorreo(formatoCorreo);
            }
            
            // Guardar configuración de mensajes
            ConfiguracionCorreoResponseDto resultado = correoService.guardarConfiguracionMensajes(configuracion);
            
            response.put("success", resultado.isExitoso());
            response.put("message", resultado.isExitoso() ? 
                "Configuración guardada exitosamente" : 
                resultado.getMensaje());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al guardar configuración de mensaje: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error al guardar la configuración: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}