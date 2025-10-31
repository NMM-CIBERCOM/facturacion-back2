package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.PantallaConfigDto;
import com.cibercom.facturacion_back.service.PantallaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu-config")
@CrossOrigin(origins = "http://localhost:5173")
public class PantallaConfigController {

    @Autowired
    private PantallaConfigService pantallaConfigService;

    @GetMapping("/pantallas/{idPerfil}")
    public ResponseEntity<List<PantallaConfigDto>> getPantallasByPerfil(@PathVariable Integer idPerfil) {
        try {
            List<PantallaConfigDto> pantallas = pantallaConfigService.getPantallasByPerfil(idPerfil);
            return ResponseEntity.ok(pantallas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/pantalla-visibilidad/{idConfig}")
    public ResponseEntity<?> updatePantallaVisibilidad(
            @PathVariable Integer idConfig,
            @RequestBody Map<String, Boolean> requestBody,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        try {
            Boolean isVisible = requestBody.get("isVisible");
            if (isVisible == null) {
                return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"El campo 'isVisible' es requerido\"}");
            }
            
            boolean success = pantallaConfigService.updatePantallaVisibilidad(idConfig, isVisible, usuario);
            if (success) {
                return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Visibilidad de pantalla actualizada correctamente\"}");
            } else {
                return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"Error al actualizar visibilidad de pantalla\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("{\"success\": false, \"message\": \"Error interno del servidor\"}");
        }
    }

    @PutMapping("/pantalla-orden/{idConfig}")
    public ResponseEntity<?> updatePantallaOrden(
            @PathVariable Integer idConfig,
            @RequestBody PantallaConfigDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        try {
            boolean success = pantallaConfigService.updatePantallaOrden(idConfig, request.getOrden(), usuario);
            if (success) {
                return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Orden de pantalla actualizado correctamente\"}");
            } else {
                return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"Error al actualizar orden de pantalla\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"success\": false, \"message\": \"Error interno del servidor\"}");
        }
    }
}
