package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.DashboardEstadisticasResponse;
import com.cibercom.facturacion_back.dto.FacturaSustituidaResponse;
import com.cibercom.facturacion_back.dto.FacturasPorUsuarioResponse;
import com.cibercom.facturacion_back.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired(required = false)
    private DashboardService dashboardService;

    @GetMapping("/estadisticas")
    public ResponseEntity<DashboardEstadisticasResponse> obtenerEstadisticas() {
        logger.info("Solicitud de estadísticas del dashboard");
        
        if (dashboardService == null) {
            logger.warn("DashboardService no está disponible (perfil Oracle no activo)");
            return ResponseEntity.ok(DashboardEstadisticasResponse.builder()
                    .exitoso(false)
                    .mensaje("Servicio de dashboard no disponible")
                    .build());
        }

        try {
            DashboardEstadisticasResponse response = dashboardService.obtenerEstadisticas();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas del dashboard: {}", e.getMessage(), e);
            return ResponseEntity.ok(DashboardEstadisticasResponse.builder()
                    .exitoso(false)
                    .mensaje("Error al obtener estadísticas: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/facturas-sustituidas")
    public ResponseEntity<FacturaSustituidaResponse> consultarFacturasSustituidas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        logger.info("Solicitud de facturas sustituidas - fechaInicio: {}, fechaFin: {}", fechaInicio, fechaFin);
        
        if (dashboardService == null) {
            logger.warn("DashboardService no está disponible (perfil Oracle no activo)");
            return ResponseEntity.ok(FacturaSustituidaResponse.builder()
                    .exitoso(false)
                    .mensaje("Servicio de dashboard no disponible")
                    .facturas(new java.util.ArrayList<>())
                    .build());
        }

        try {
            FacturaSustituidaResponse response = dashboardService.consultarFacturasSustituidas(fechaInicio, fechaFin);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error consultando facturas sustituidas: {}", e.getMessage(), e);
            return ResponseEntity.ok(FacturaSustituidaResponse.builder()
                    .exitoso(false)
                    .mensaje("Error al consultar facturas sustituidas: " + e.getMessage())
                    .facturas(new java.util.ArrayList<>())
                    .build());
        }
    }

    @GetMapping("/facturas-por-usuario")
    public ResponseEntity<FacturasPorUsuarioResponse> consultarFacturasPorUsuario(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        logger.info("Solicitud de facturas por usuario - usuario: {}, fechaInicio: {}, fechaFin: {}", usuario, fechaInicio, fechaFin);
        
        if (dashboardService == null) {
            logger.warn("DashboardService no está disponible (perfil Oracle no activo)");
            return ResponseEntity.ok(FacturasPorUsuarioResponse.builder()
                    .exitoso(false)
                    .mensaje("Servicio de dashboard no disponible")
                    .usuarios(new java.util.ArrayList<>())
                    .build());
        }

        try {
            FacturasPorUsuarioResponse response = dashboardService.consultarFacturasPorUsuario(usuario, fechaInicio, fechaFin);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error consultando facturas por usuario: {}", e.getMessage(), e);
            return ResponseEntity.ok(FacturasPorUsuarioResponse.builder()
                    .exitoso(false)
                    .mensaje("Error al consultar facturas por usuario: " + e.getMessage())
                    .usuarios(new java.util.ArrayList<>())
                    .build());
        }
    }
}

