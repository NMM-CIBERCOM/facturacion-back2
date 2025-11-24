package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.model.CreditNote;
import com.cibercom.facturacion_back.model.CreditNoteItem;
import com.cibercom.facturacion_back.model.CreditNoteLink;
import com.cibercom.facturacion_back.service.CreditNoteService;
import com.cibercom.facturacion_back.service.CreditNoteOracleSaveService;
import com.cibercom.facturacion_back.dto.CreditNoteSaveRequest;
import com.cibercom.facturacion_back.dto.PacTimbradoRequest;
import com.cibercom.facturacion_back.dto.PacTimbradoResponse;
import com.cibercom.facturacion_back.service.CreditNoteXmlBuilder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

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
    private final CreditNoteXmlBuilder creditNoteXmlBuilder;
    @Value("${facturacion.regimenFiscalEmisor:601}")
    private String regimenFiscalEmisorDefault;
    @Value("${facturacion.regimenFiscalReceptor:601}")
    private String regimenFiscalReceptorDefault;

    public CreditNoteController(CreditNoteService service,
                                ObjectProvider<CreditNoteOracleSaveService> oracleSaveProvider,
                                CreditNoteXmlBuilder creditNoteXmlBuilder) {
        this.service = service;
        this.oracleSaveProvider = oracleSaveProvider;
        this.creditNoteXmlBuilder = creditNoteXmlBuilder;
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
        ensureXmlContent(request);
        CreditNoteOracleSaveService svc = oracleSaveProvider.getIfAvailable();
        if (svc == null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ok", false);
            m.put("message", "Oracle no disponible (perfil 'oracle' inactivo)");
            return ResponseEntity.status(501).body(m);
        }
        logger.info("NC Controller Guardar: uuidNc={} rfcEmisor={} rfcReceptor={} total={}", request.getUuidNc(), request.getRfcEmisor(), request.getRfcReceptor(), request.getTotal());
        CreditNoteOracleSaveService.SaveResult res = svc.guardar(request);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", res.ok);
        m.put("uuidNc", res.uuidNc);
        m.put("errors", res.errors);
        logger.info("NC Controller Guardar: ok={} uuidNc={} errors={}", res.ok, res.uuidNc, res.errors);
        return ResponseEntity.ok(m);
    }

    /** Timbrar Nota de Crédito vía PAC (EGRESO) */
    @PostMapping("/timbrar")
    public ResponseEntity<?> timbrar(@RequestBody CreditNoteSaveRequest request) {
        try {
            String xmlGenerado = ensureXmlContent(request);
            logger.info("NC Controller Timbrar: solicitud uuidNc={} rfcEmisor={} rfcReceptor={} total={} serie={} folio={} uuidFacturaOrig={}",
                    request.getUuidNc(), request.getRfcEmisor(), request.getRfcReceptor(), request.getTotal(), request.getSerieNc(), request.getFolioNc(), request.getUuidFacturaOrig());
            // Construir solicitud al PAC usando los datos de la Nota de Crédito del frontend
            String uuid = request.getUuidNc() != null && !request.getUuidNc().isBlank()
                    ? request.getUuidNc()
                    : java.util.UUID.randomUUID().toString().toUpperCase();

            String serie = request.getSerieNc() != null && !request.getSerieNc().isBlank() ? request.getSerieNc() : "NC";
            String folio = request.getFolioNc() != null && !request.getFolioNc().isBlank() ? request.getFolioNc() : "1";
            String fecha = request.getFechaEmision() != null ? request.getFechaEmision().toString() : java.time.LocalDateTime.now().toString();

            PacTimbradoRequest req = PacTimbradoRequest.builder()
                    .uuid(uuid)
                    .xmlContent(xmlGenerado)
                    .rfcEmisor(request.getRfcEmisor())
                    .rfcReceptor(request.getRfcReceptor())
                    .total(request.getTotal() != null ? request.getTotal().doubleValue() : null)
                    .tipo("EGRESO")
                    .fechaFactura(fecha)
                    .publicoGeneral(false)
                    .serie(serie)
                    .folio(folio)
                    .medioPago(request.getMetodoPago())
                    .formaPago(request.getFormaPago())
                    .usoCFDI(request.getUsoCfdi())
                    .regimenFiscalEmisor(StringUtils.hasText(regimenFiscalEmisorDefault) ? regimenFiscalEmisorDefault : "601")
                    .regimenFiscalReceptor(StringUtils.hasText(request.getRegimenFiscal())
                            ? request.getRegimenFiscal()
                            : (StringUtils.hasText(regimenFiscalReceptorDefault) ? regimenFiscalReceptorDefault : "601"))
                    .relacionadosUuids(request.getUuidFacturaOrig())
                    .build();

            PacTimbradoResponse resp = service.timbrarManual(req);
            if (resp == null) {
                logger.error("NC Controller Timbrar: el servicio devolvió una respuesta nula para uuid={}", uuid);
                return ResponseEntity.status(502).body(Map.of(
                        "ok", false,
                        "error", "El PAC no regresó respuesta",
                        "uuid", uuid
                ));
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ok", resp.getOk());
            m.put("status", resp.getStatus());
            m.put("uuid", resp.getUuid());
            m.put("serie", resp.getSerie() != null ? resp.getSerie() : serie);
            m.put("folio", resp.getFolio() != null ? resp.getFolio() : folio);
            m.put("xmlTimbrado", resp.getXmlTimbrado());
            m.put("xmlGenerado", xmlGenerado);
            m.put("message", resp.getMessage());
            logger.info("NC Controller Timbrar: respuesta ok={} status={} uuid={} message={}", resp.getOk(), resp.getStatus(), resp.getUuid(), resp.getMessage());
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            logger.error("Error timbrando nota de crédito: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    private String ensureXmlContent(CreditNoteSaveRequest request) {
        if (request == null) {
            return null;
        }
        if (StringUtils.hasText(request.getXmlContent())) {
            return request.getXmlContent();
        }
        String xml = creditNoteXmlBuilder.buildXml(request);
        request.setXmlContent(xml);
        return xml;
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