package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.ConceptoInsertRequest;
import com.cibercom.facturacion_back.dto.ConceptoInsertResponse;
import com.cibercom.facturacion_back.service.ConceptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("oracle")
@RequestMapping("/api/conceptos")
@CrossOrigin(origins = "*")
public class ConceptoController {

    private static final Logger logger = LoggerFactory.getLogger(ConceptoController.class);

    private final ConceptoService conceptoService;

    public ConceptoController(ConceptoService conceptoService) {
        this.conceptoService = conceptoService;
    }

    @PostMapping
    public ResponseEntity<ConceptoInsertResponse> insertarConcepto(@RequestBody ConceptoInsertRequest request) {
        logger.info("Solicitud de inserción de concepto: uuid={}, idFactura={}", request.getUuidFactura(), request.getIdFactura());
        try {
            ConceptoInsertResponse response = conceptoService.insertarConcepto(request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error interno al insertar concepto", e);
            ConceptoInsertResponse error = new ConceptoInsertResponse(false, "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ConceptoController funcionando correctamente");
    }
}