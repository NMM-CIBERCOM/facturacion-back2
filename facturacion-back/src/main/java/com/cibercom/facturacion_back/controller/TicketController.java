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

    /**
     * Busca tickets vinculados a una factura por su ID_FACTURA.
     */
    @GetMapping("/por-id-factura/{idFactura}")
    public ResponseEntity<Map<String, Object>> buscarPorIdFactura(@PathVariable("idFactura") Long idFactura) {
        logger.info("Solicitud de tickets por ID_FACTURA={}", idFactura);
        Map<String, Object> resp = new HashMap<>();
        try {
            if (idFactura == null || idFactura <= 0) {
                resp.put("success", false);
                resp.put("message", "ID_FACTURA inválido");
                return ResponseEntity.badRequest().body(resp);
            }
            java.util.List<TicketDto> lista = ticketService.buscarTicketsPorIdFactura(idFactura);
            resp.put("success", true);
            resp.put("message", lista.isEmpty() ? "Sin resultados" : "Resultados encontrados");
            resp.put("data", lista);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Error al buscar tickets por ID_FACTURA: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Error al buscar tickets por ID_FACTURA: " + e.getMessage());
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    /**
     * Busca tickets por su ID_TICKET.
     */
    @GetMapping("/por-id-ticket/{idTicket}")
    public ResponseEntity<Map<String, Object>> buscarPorIdTicket(@PathVariable("idTicket") Long idTicket) {
        logger.info("Solicitud de tickets por ID_TICKET={}", idTicket);
        Map<String, Object> resp = new HashMap<>();
        try {
            if (idTicket == null || idTicket <= 0) {
                resp.put("success", false);
                resp.put("message", "ID_TICKET inválido");
                return ResponseEntity.badRequest().body(resp);
            }
            java.util.List<TicketDto> lista = ticketService.buscarTicketsPorIdTicket(idTicket);
            resp.put("success", true);
            resp.put("message", lista.isEmpty() ? "Sin resultados" : "Resultados encontrados");
            resp.put("data", lista);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Error al buscar tickets por ID_TICKET: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Error al buscar tickets por ID_TICKET: " + e.getMessage());
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

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

    /**
     * Obtiene detalles de tickets asociados a una factura por su ID_FACTURA.
     * Realiza JOIN entre TICKETS y TICKETS_DETALLE.
     */
    @GetMapping("/detalles/por-id-factura/{idFactura}")
    public ResponseEntity<Map<String, Object>> obtenerDetallesPorIdFactura(@PathVariable("idFactura") Long idFactura) {
        logger.info("Solicitud de detalles por ID_FACTURA={}", idFactura);
        Map<String, Object> resp = new HashMap<>();
        try {
            if (idFactura == null || idFactura <= 0) {
                resp.put("success", false);
                resp.put("message", "ID_FACTURA inválido");
                return ResponseEntity.badRequest().body(resp);
            }
            if (ticketDetalleService == null) {
                resp.put("success", false);
                resp.put("message", "Servicio de detalles no disponible (perfil Oracle no activo)");
                return ResponseEntity.badRequest().body(resp);
            }
            java.util.List<TicketDetalleDto> detalles = ticketDetalleService.buscarDetallesPorIdFactura(idFactura);
            resp.put("success", true);
            resp.put("message", detalles.isEmpty() ? "Sin detalles" : "Detalles encontrados");
            resp.put("data", detalles);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Error al obtener detalles por ID_FACTURA: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Error al obtener detalles por ID_FACTURA: " + e.getMessage());
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }
}