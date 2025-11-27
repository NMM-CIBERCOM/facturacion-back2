package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.CartaPorteSaveRequest;
import com.cibercom.facturacion_back.service.CartaPorteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Profile("oracle")
@RequestMapping("/api/carta-porte")
@CrossOrigin(origins = "*")
public class CartaPorteController {

    private static final Logger logger = LoggerFactory.getLogger(CartaPorteController.class);

    @Autowired
    private CartaPorteService cartaPorteService;

    @PostMapping("/guardar")
    public ResponseEntity<?> guardar(@RequestBody CartaPorteSaveRequest request) {
        try {
            logger.info("Guardando carta porte con RFC: {}", request.getRfcCompleto());
            
            // DEBUG: Verificar remolques en el request
            if (request.getComplemento() != null && 
                request.getComplemento().getMercancias() != null &&
                request.getComplemento().getMercancias().getAutotransporte() != null) {
                var autotransporte = request.getComplemento().getMercancias().getAutotransporte();
                var remolques = autotransporte.getRemolques();
                logger.info("DEBUG - Remolques recibidos: {}", remolques == null ? "null" : remolques.size());
                if (remolques != null) {
                    for (int i = 0; i < remolques.size(); i++) {
                        var r = remolques.get(i);
                        logger.info("DEBUG - Remolque[{}]: subTipoRem={}, placa={}", 
                            i, 
                            r == null ? "null" : r.getSubTipoRem(),
                            r == null ? "null" : r.getPlaca());
                    }
                }
            }
            
            CartaPorteService.SaveResult result = cartaPorteService.guardar(request);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", true);
            response.put("id", result.getCartaPorteId());
            response.put("uuid", result.getUuidTimbrado());
            response.put("xmlTimbrado", result.getPacResponse() != null ? result.getPacResponse().getXmlTimbrado() : null);
            response.put("message", result.getPacResponse() != null && Boolean.TRUE.equals(result.getPacResponse().getOk())
                    ? "Carta porte timbrada exitosamente"
                    : "Carta porte guardada");
            response.put("pacResponse", result.getPacResponse());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error guardando carta porte: {}", e.getMessage(), e);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/preview-xml")
    public ResponseEntity<?> preview(@RequestBody CartaPorteSaveRequest request) {
        try {
            String xml = cartaPorteService.renderXml(request);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", true);
            response.put("xml", xml);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generando XML de vista previa: {}", e.getMessage(), e);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

}
