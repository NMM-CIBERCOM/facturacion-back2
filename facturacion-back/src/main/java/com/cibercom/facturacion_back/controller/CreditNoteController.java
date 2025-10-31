package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.model.CreditNote;
import com.cibercom.facturacion_back.model.CreditNoteItem;
import com.cibercom.facturacion_back.model.CreditNoteLink;
import com.cibercom.facturacion_back.service.CreditNoteService;
import com.cibercom.facturacion_back.service.CreditNoteOracleSaveService;
import com.cibercom.facturacion_back.dto.CreditNoteSaveRequest;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/credit-notes")
@CrossOrigin(origins = "*")
public class CreditNoteController {

    private static final Logger logger = LoggerFactory.getLogger(CreditNoteController.class);

    private final CreditNoteService service;
    private final ObjectProvider<CreditNoteOracleSaveService> oracleSaveProvider;
    public CreditNoteController(CreditNoteService service,
                                ObjectProvider<CreditNoteOracleSaveService> oracleSaveProvider) {
        this.service = service;
        this.oracleSaveProvider = oracleSaveProvider;
    }

    @Data
    public static class MonederosGenerateRequest {
        private String fecha; // periodo a cerrar (yyyy-MM)
        private Map<String, BigDecimal> montos; // opcional: uuid_monedero -> monto
        private List<String> uuidsFacturasGlobalMes; // opcional: relaciones predefinidas

        public String getFecha() { return fecha; }
        public Map<String, BigDecimal> getMontos() { return montos; }
        public List<String> getUuidsFacturasGlobalMes() { return uuidsFacturasGlobalMes; }
        public void setFecha(String fecha) { this.fecha = fecha; }
        public void setMontos(Map<String, BigDecimal> montos) { this.montos = montos; }
        public void setUuidsFacturasGlobalMes(List<String> uuids) { this.uuidsFacturasGlobalMes = uuids; }
    }

    @PostMapping("/monederos/generate")
    public ResponseEntity<?> generateMonederos(@RequestBody MonederosGenerateRequest req) {
        try {
            LocalDate periodo = parsePeriodo(req.getFecha());
            Map<String, BigDecimal> montos = req.getMontos() != null ? req.getMontos() : Collections.emptyMap();
            List<String> uuids = req.getUuidsFacturasGlobalMes() != null ? req.getUuidsFacturasGlobalMes() : obtenerFacturasGlobalesDelMes(periodo);

            CreditNote cn = service.generateMonederos(periodo, montos, uuids);
            return ResponseEntity.ok(toResponse(cn));
        } catch (Exception e) {
            logger.error("Error generando monederos: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Data
    public static class GlobalGenerateRequest {
        private String fecha; // día a procesar (yyyy-MM-dd)
        public String getFecha() { return fecha; }
        public void setFecha(String fecha) { this.fecha = fecha; }
    }

    @PostMapping("/global/generate")
    public ResponseEntity<?> generateGlobal(@RequestBody GlobalGenerateRequest req) {
        try {
            LocalDate dia = LocalDate.parse(req.getFecha());
            CreditNote cn = service.generateGlobal(dia);
            return ResponseEntity.ok(toResponse(cn));
        } catch (Exception e) {
            logger.error("Error generando control global: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{uuidNc}")
    public ResponseEntity<?> getByUuid(@PathVariable("uuidNc") String uuidNc) {
        return service.getByUuid(uuidNc)
                .map(cn -> ResponseEntity.ok(toResponse(cn)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(value = "uuidFactura", required = false) String uuidFactura) {
        List<CreditNote> list = service.search(fechaInicio, fechaFin, uuidFactura);
        return ResponseEntity.ok(list.stream().map(this::toResponse).toList());
    }

    /** Guardar Nota de Crédito en Oracle: FACTURAS y NOTAS_CREDITO */
    @PostMapping("/guardar")
    public ResponseEntity<?> guardar(@RequestBody CreditNoteSaveRequest request) {
        CreditNoteOracleSaveService svc = oracleSaveProvider.getIfAvailable();
        if (svc == null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ok", false);
            m.put("message", "Oracle no disponible (perfil 'oracle' inactivo)");
            return ResponseEntity.status(501).body(m);
        }
        CreditNoteOracleSaveService.SaveResult res = svc.guardar(request);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", res.ok);
        m.put("uuidNc", res.uuidNc);
        m.put("errors", res.errors);
        return ResponseEntity.ok(m);
    }

    private LocalDate parsePeriodo(String periodo) {
        // admite formatos yyyy-MM y yyyy-MM-dd (se toma inicio del mes)
        if (periodo == null || periodo.isBlank()) {
            throw new IllegalArgumentException("fecha (periodo) es requerida");
        }
        if (periodo.length() == 7) {
            String d = periodo + "-01";
            return LocalDate.parse(d);
        }
        return LocalDate.parse(periodo).withDayOfMonth(1);
    }

    private List<String> obtenerFacturasGlobalesDelMes(LocalDate periodo) {
        // Placeholder: en ausencia de regla específica, obtener facturas timbradas del mes
        LocalDateTime inicio = periodo.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fin = periodo.withDayOfMonth(periodo.lengthOfMonth()).atTime(23, 59, 59);
        return service.search(inicio, fin, null).stream()
                .flatMap(cn -> cn.getLinks().stream().map(CreditNoteLink::getUuidFacturaOrigen))
                .distinct()
                .toList();
    }

    private Map<String, Object> toResponse(CreditNote cn) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("uuid_nc", cn.getUuidNc());
        m.put("fecha_emision", cn.getFechaEmision());
        m.put("serie", cn.getSerie());
        m.put("folio", cn.getFolio());
        m.put("estatus", cn.getEstatus());
        m.put("total", cn.getTotal());
        m.put("subtotal", cn.getSubtotal());
        m.put("iva", cn.getIva());
        m.put("tipo_comprobante", cn.getTipoComprobante());
        m.put("uso_cfdi", cn.getUsoCfdi());
        m.put("tipo_relacion", cn.getTipoRelacion());
        m.put("xml_base64", cn.getXmlBase64());
        m.put("html_base64", cn.getHtmlBase64());
        m.put("links", cn.getLinks().stream().map(CreditNoteLink::getUuidFacturaOrigen).toList());
        m.put("items", cn.getItems().stream().map(this::toItem).toList());
        return m;
    }

    private Map<String, Object> toItem(CreditNoteItem it) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clave_prod_serv", it.getClaveProdServ());
        m.put("descripcion", it.getDescripcion());
        m.put("cantidad", it.getCantidad());
        m.put("valor_unitario", it.getValorUnitario());
        m.put("importe", it.getImporte());
        m.put("iva_tasa", it.getIvaTasa());
        m.put("iva_importe", it.getIvaImporte());
        return m;
    }
}