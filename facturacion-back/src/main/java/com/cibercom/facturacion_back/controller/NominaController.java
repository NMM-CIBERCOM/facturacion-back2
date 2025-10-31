package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.NominaSaveRequest;
import com.cibercom.facturacion_back.dto.NominaSaveResponse;
import com.cibercom.facturacion_back.service.NominaService;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nominas")
public class NominaController {

    private final NominaService nominaService;
    private static final Logger logger = LoggerFactory.getLogger(NominaController.class);

    public NominaController(NominaService nominaService) {
        this.nominaService = nominaService;
    }

    @PostMapping("/guardar")
    public ResponseEntity<NominaSaveResponse> guardar(@RequestBody NominaSaveRequest request) {
        NominaSaveResponse resp = nominaService.guardar(request);
        if (resp.isOk()) return ResponseEntity.ok(resp);
        // Log de error para facilitar diagnóstico en consola
        logger.error("Fallo guardando nómina: {} | errores: {}", resp.getMessage(), resp.getErrors());
        return ResponseEntity.badRequest().body(resp);
    }

    @GetMapping("/historial")
    public ResponseEntity<java.util.List<com.cibercom.facturacion_back.dto.NominaHistorialDTO>> historial(@RequestParam("idEmpleado") String idEmpleado) {
        java.util.List<com.cibercom.facturacion_back.dto.NominaHistorialDTO> lista = nominaService.consultarHistorial(idEmpleado);
        return ResponseEntity.ok(lista);
    }
}