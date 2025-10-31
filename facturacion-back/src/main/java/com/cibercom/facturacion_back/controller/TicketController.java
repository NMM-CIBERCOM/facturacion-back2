package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.TicketDto;
import com.cibercom.facturacion_back.dto.TicketSearchRequest;
import com.cibercom.facturacion_back.service.TicketService;
import com.cibercom.facturacion_back.service.TicketDetalleService;
import com.cibercom.facturacion_back.dto.TicketDetalleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
public class TicketController {

    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    @Autowired
    private TicketService ticketService;

    @Autowired(required = false)
    private TicketDetalleService ticketDetalleService;

    @PostMapping("/buscar")
    public ResponseEntity<Map<String, Object>> buscar(@RequestBody TicketSearchRequest request) {
        logger.info("Solicitud de búsqueda de tickets: {}", request);
        Map<String, Object> resp = new HashMap<>();
        try {
            List<TicketDto> lista = ticketService.buscarTickets(request);
            resp.put("success", true);
            resp.put("message", lista.isEmpty() ? "Sin resultados" : "Resultados encontrados");
            resp.put("data", lista);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Error al buscar tickets: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Error al buscar tickets: " + e.getMessage());
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @GetMapping("/{idTicket}/detalles")
    public ResponseEntity<Map<String, Object>> obtenerDetalles(@PathVariable("idTicket") Long idTicket) {
        logger.info("Solicitud de detalles de ticket ID_TICKET={}", idTicket);
        Map<String, Object> resp = new HashMap<>();
        try {
            if (idTicket == null || idTicket <= 0) {
                resp.put("success", false);
                resp.put("message", "ID_TICKET inválido");
                return ResponseEntity.badRequest().body(resp);
            }
            if (ticketDetalleService == null) {
                resp.put("success", false);
                resp.put("message", "Servicio de detalles no disponible (perfil Oracle no activo)");
                return ResponseEntity.badRequest().body(resp);
            }
            List<TicketDetalleDto> detalles = ticketDetalleService.buscarDetallesPorIdTicket(idTicket);
            resp.put("success", true);
            resp.put("message", detalles.isEmpty() ? "Sin detalles" : "Detalles encontrados");
            resp.put("data", detalles);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Error al obtener detalles de ticket: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Error al obtener detalles de ticket: " + e.getMessage());
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }
}