package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.PacTimbradoRequest;
import com.cibercom.facturacion_back.dto.PacTimbradoResponse;
import com.cibercom.facturacion_back.integration.PacClient;
import com.cibercom.facturacion_back.integration.BitacoraClient;
import com.cibercom.facturacion_back.model.CreditNote;
import com.cibercom.facturacion_back.model.CreditNoteItem;
import com.cibercom.facturacion_back.model.CreditNoteLink;
import com.cibercom.facturacion_back.model.Factura;
import com.cibercom.facturacion_back.repository.CreditNoteRepository;
import com.cibercom.facturacion_back.repository.FacturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CreditNoteService {

    private static final Logger logger = LoggerFactory.getLogger(CreditNoteService.class);

    private final CreditNoteRepository creditNoteRepo;
    private final FacturaRepository facturaRepo;
    private final PacClient pacClient;
    private final BitacoraClient bitacoraClient;

    @Value("${facturacion.ivaTasa:0.16}")
    private BigDecimal ivaTasa;

    @Value("${facturacion.creditnote.serie:NC}")
    private String defaultSerie;

    @Value("${facturacion.regimenFiscalEmisor:601}")
    private String regimenFiscalEmisor;

    @Value("${facturacion.regimenFiscalReceptor:601}")
    private String regimenFiscalReceptor;

    @Value("${facturacion.creditnote.usoCfdi:G02}")
    private String usoCfdi;

    @Value("${facturacion.creditnote.formaPago:01}")
    private String formaPago;

    @Value("${facturacion.creditnote.metodoPago:PUE}")
    private String metodoPago;

    @Value("${facturacion.emisor.rfc:EEM123456789}")
    private String rfcEmisor;

    public CreditNoteService(CreditNoteRepository creditNoteRepo, FacturaRepository facturaRepo, PacClient pacClient, BitacoraClient bitacoraClient) {
        this.creditNoteRepo = creditNoteRepo;
        this.facturaRepo = facturaRepo;
        this.pacClient = pacClient;
        this.bitacoraClient = bitacoraClient;
    }

    @Transactional
    public CreditNote generateMonederos(LocalDate periodo, Map<String, BigDecimal> montosPorMonedero, List<String> uuidsFacturasGlobalMes) {
        // Construir items a partir de montos
        String periodoLabel = periodo.format(DateTimeFormatter.ofPattern("MM-yyyy"));
        List<CreditNoteItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : montosPorMonedero.entrySet()) {
            BigDecimal monto = entry.getValue();
            if (monto == null) continue;
            BigDecimal ivaImporte = monto.multiply(ivaTasa).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal importe = monto.setScale(2, BigDecimal.ROUND_HALF_UP);

            CreditNoteItem item = CreditNoteItem.builder()
                    .claveProdServ("84111506")
                    .descripcion("DESCUENTO VENTA DE MONEDEROS ELECTRONICOS " + periodoLabel)
                    .cantidad(BigDecimal.ONE)
                    .valorUnitario(importe)
                    .importe(importe)
                    .ivaTasa(ivaTasa)
                    .ivaImporte(ivaImporte)
                    .build();
            items.add(item);
            subtotal = subtotal.add(importe);
            totalIva = totalIva.add(ivaImporte);
        }

        BigDecimal total = subtotal.add(totalIva).setScale(2, BigDecimal.ROUND_HALF_UP);

        // Relacionar facturas globales del mes
        List<CreditNoteLink> links = uuidsFacturasGlobalMes.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(u -> CreditNoteLink.builder().uuidFacturaOrigen(u).build())
                .collect(Collectors.toList());

        // Idempotencia
        String uniqueKey = buildUniqueKey(periodo.atStartOfDay(), uuidsFacturasGlobalMes);
        Optional<CreditNote> existing = creditNoteRepo.findByUniqueKey(uniqueKey);
        if (existing.isPresent()) {
            logger.info("Idempotencia: ya existe nota de crédito para periodo={} y UUIDs, uuid_nc={}", periodo, existing.get().getUuidNc());
            return existing.get();
        }

        // Serie/folio simples
        String serie = defaultSerie;
        String folio = String.valueOf(Math.abs(System.currentTimeMillis() % 100000));

        // Timbrado PAC con receptor genérico
        PacTimbradoRequest req = PacTimbradoRequest.builder()
                .uuid(UUID.randomUUID().toString().toUpperCase())
                .xmlContent(null)
                .rfcEmisor(rfcEmisor)
                .rfcReceptor("XAXX010101000")
                .total(total.doubleValue())
                .tipo("EGRESO")
                .fechaFactura(periodo.atStartOfDay().toString())
                .publicoGeneral(true)
                .serie(serie)
                .folio(folio)
                .tienda(null)
                .terminal(null)
                .boleta(null)
                .medioPago(metodoPago)
                .formaPago(formaPago)
                .usoCFDI(usoCfdi)
                .regimenFiscalEmisor(regimenFiscalEmisor)
                .regimenFiscalReceptor(regimenFiscalReceptor)
                .relacionadosUuids(String.join(",", uuidsFacturasGlobalMes))
                .build();

        PacTimbradoResponse resp = solicitarTimbradoConReintento(req);

        CreditNote cn = CreditNote.builder()
                .uuidNc(resp.getUuid() != null ? resp.getUuid() : req.getUuid())
                .fechaEmision(LocalDateTime.now())
                .serie(resp.getSerie() != null ? resp.getSerie() : serie)
                .folio(resp.getFolio() != null ? resp.getFolio() : folio)
                .estatus(resp.getStatus())
                .xmlBase64(base64(resp.getXmlTimbrado()))
                .htmlBase64(base64(generarHtmlBasico(items, subtotal, totalIva, total, cnRelacionadosAsString(uuidsFacturasGlobalMes))))
                .total(total)
                .subtotal(subtotal)
                .iva(totalIva)
                .tipoComprobante("E")
                .usoCfdi(usoCfdi)
                .tipoRelacion("01")
                .rfcEmisor(rfcEmisor)
                .rfcReceptor("XAXX010101000")
                .uniqueKey(uniqueKey)
                .build();

        // Enlazar relaciones e items
        links.forEach(l -> l.setCreditNote(cn));
        items.forEach(i -> i.setCreditNote(cn));
        cn.setLinks(links);
        cn.setItems(items);

        CreditNote saved = creditNoteRepo.save(cn);
        logger.info("Nota de crédito (monederos) generada uuid_nc={} total={} items={} links={}", saved.getUuidNc(), saved.getTotal(), saved.getItems().size(), saved.getLinks().size());
        return saved;
    }

    @Transactional
    public CreditNote generateGlobal(LocalDate dia) {
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fin = dia.atTime(LocalTime.MAX);

        List<Factura> facturasDelDia = facturaRepo.findByFechaFacturaBetween(inicio, fin);
        List<String> uuids = facturasDelDia.stream()
                .map(Factura::getUuid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // Construir un único renglón por control global (ajustable por reglas)
        BigDecimal base = facturasDelDia.stream()
                .map(Factura::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ivaImporte = base.multiply(ivaTasa).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal total = base.add(ivaImporte).setScale(2, BigDecimal.ROUND_HALF_UP);

        List<CreditNoteItem> items = new ArrayList<>();
        items.add(CreditNoteItem.builder()
                .claveProdServ("84111506")
                .descripcion("CONTROL GLOBAL EGRESO " + dia)
                .cantidad(BigDecimal.ONE)
                .valorUnitario(base.setScale(2, BigDecimal.ROUND_HALF_UP))
                .importe(base.setScale(2, BigDecimal.ROUND_HALF_UP))
                .ivaTasa(ivaTasa)
                .ivaImporte(ivaImporte)
                .build());

        String uniqueKey = buildUniqueKey(inicio, uuids);
        Optional<CreditNote> existing = creditNoteRepo.findByUniqueKey(uniqueKey);
        if (existing.isPresent()) {
            logger.info("Idempotencia: ya existe nota de crédito global para día={} uuid_nc={}", dia, existing.get().getUuidNc());
            return existing.get();
        }

        String serie = defaultSerie;
        String folio = String.valueOf(Math.abs(System.currentTimeMillis() % 100000));
        BigDecimal subtotal = base.setScale(2, BigDecimal.ROUND_HALF_UP);

        PacTimbradoRequest req = PacTimbradoRequest.builder()
                .uuid(UUID.randomUUID().toString().toUpperCase())
                .xmlContent(null)
                .rfcEmisor(rfcEmisor)
                .rfcReceptor("XAXX010101000")
                .total(total.doubleValue())
                .tipo("EGRESO")
                .fechaFactura(dia.toString())
                .publicoGeneral(true)
                .serie(serie)
                .folio(folio)
                .medioPago(metodoPago)
                .formaPago(formaPago)
                .usoCFDI(usoCfdi)
                .regimenFiscalEmisor(regimenFiscalEmisor)
                .regimenFiscalReceptor(regimenFiscalReceptor)
                .relacionadosUuids(String.join(",", uuids))
                .build();

        PacTimbradoResponse resp = solicitarTimbradoConReintento(req);

        CreditNote cn = CreditNote.builder()
                .uuidNc(resp.getUuid() != null ? resp.getUuid() : req.getUuid())
                .fechaEmision(LocalDateTime.now())
                .serie(resp.getSerie() != null ? resp.getSerie() : serie)
                .folio(resp.getFolio() != null ? resp.getFolio() : folio)
                .estatus(resp.getStatus())
                .xmlBase64(base64(resp.getXmlTimbrado()))
                .htmlBase64(base64(generarHtmlBasico(items, subtotal, ivaImporte, total, cnRelacionadosAsString(uuids))))
                .total(total)
                .subtotal(subtotal)
                .iva(ivaImporte)
                .tipoComprobante("E")
                .usoCfdi(usoCfdi)
                .tipoRelacion("01")
                .rfcEmisor(rfcEmisor)
                .rfcReceptor("XAXX010101000")
                .uniqueKey(uniqueKey)
                .build();

        List<CreditNoteLink> links = uuids.stream()
                .map(u -> CreditNoteLink.builder().uuidFacturaOrigen(u).build())
                .collect(Collectors.toList());
        links.forEach(l -> l.setCreditNote(cn));
        items.forEach(i -> i.setCreditNote(cn));
        cn.setLinks(links);
        cn.setItems(items);

        CreditNote saved = creditNoteRepo.save(cn);
        logger.info("Nota de crédito (global) generada uuid_nc={} total={} items={} links={}", saved.getUuidNc(), saved.getTotal(), saved.getItems().size(), saved.getLinks().size());
        return saved;
    }

    public Optional<CreditNote> getByUuid(String uuidNc) {
        return creditNoteRepo.findByUuidNc(uuidNc);
    }

    public List<CreditNote> search(LocalDateTime inicio, LocalDateTime fin, String uuidFactura) {
        if (uuidFactura != null && !uuidFactura.isBlank()) {
            return creditNoteRepo.findByUuidFacturaOrigen(uuidFactura).stream()
                    .filter(cn -> (inicio == null || !cn.getFechaEmision().isBefore(inicio)) && (fin == null || !cn.getFechaEmision().isAfter(fin)))
                    .collect(Collectors.toList());
        }
        if (inicio != null && fin != null) {
            return creditNoteRepo.findByFechaEmisionBetween(inicio, fin);
        }
        // Si no hay filtros, devolver últimas 50
        return creditNoteRepo.findAll().stream()
                .sorted(Comparator.comparing(CreditNote::getFechaEmision).reversed())
                .limit(50)
                .collect(Collectors.toList());
    }

    private PacTimbradoResponse solicitarTimbradoConReintento(PacTimbradoRequest req) {
        final int MAX_RETRIES = 3;
        bitacoraClient.registrarEvento("CreditNotes", "Timbrado", "INICIO", "Solicitud de timbrado", Map.of(
                "uuid", req.getUuid(),
                "tipo", req.getTipo(),
                "total", req.getTotal(),
                "relacionadosUuids", req.getRelacionadosUuids()
        ));
        PacTimbradoResponse resp = pacClient.solicitarTimbrado(req);
        int intentos = 1;
        while (intentos < MAX_RETRIES && (resp == null || resp.getOk() == null || !resp.getOk() || (resp.getStatus() != null && !"0".equals(resp.getStatus())))) {
            bitacoraClient.registrarEvento("CreditNotes", "Timbrado", "REINTENTO", "Intento de timbrado", Map.of(
                    "intento", intentos + 1,
                    "maxRetries", MAX_RETRIES,
                    "status", resp != null ? resp.getStatus() : null,
                    "message", resp != null ? resp.getMessage() : null
            ));
            logger.warn("Reintentando timbrado PAC intento {}/{} - status={} - message={}", intentos + 1, MAX_RETRIES, resp != null ? resp.getStatus() : null, resp != null ? resp.getMessage() : null);
            try { Thread.sleep(1000L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            resp = pacClient.solicitarTimbrado(req);
            intentos++;
        }
        if (resp == null) {
            resp = PacTimbradoResponse.builder().ok(false).status("ERROR").message("PAC sin respuesta").build();
        }
        bitacoraClient.registrarEvento("CreditNotes", "Timbrado", "FIN", "Resultado de timbrado", Map.of(
                "ok", resp.getOk(),
                "status", resp.getStatus(),
                "uuid", resp.getUuid(),
                "message", resp.getMessage()
        ));
        return resp;
    }

    /**
     * Exponer timbrado para notas de crédito construidas manualmente desde el controlador.
     */
    public PacTimbradoResponse timbrarManual(PacTimbradoRequest req) {
        return solicitarTimbradoConReintento(req);
    }

    private String buildUniqueKey(LocalDateTime periodoClave, List<String> uuids) {
        String fechaKey = periodoClave.toLocalDate().toString();
        List<String> sorted = uuids == null ? Collections.emptyList() : uuids.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        return fechaKey + "|" + String.join(",", sorted);
    }

    private String base64(String s) {
        if (s == null) return null;
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private String generarHtmlBasico(List<CreditNoteItem> items, BigDecimal subtotal, BigDecimal iva, BigDecimal total, String relacionados) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset=\"utf-8\"><title>Nota de Crédito</title></head><body>");
        sb.append("<h2>Nota de Crédito (EGRESO)</h2>");
        sb.append("<p>Tipo relación: 01</p>");
        sb.append("<p>Facturas relacionadas: ").append(relacionados).append("</p>");
        sb.append("<table border=1 cellspacing=0 cellpadding=4>");
        sb.append("<tr><th>Descripción</th><th>Cantidad</th><th>V.Unitario</th><th>Importe</th><th>IVA</th></tr>");
        for (CreditNoteItem it : items) {
            sb.append("<tr>");
            sb.append("<td>").append(it.getDescripcion()).append("</td>");
            sb.append("<td>").append(it.getCantidad()).append("</td>");
            sb.append("<td>").append(it.getValorUnitario()).append("</td>");
            sb.append("<td>").append(it.getImporte()).append("</td>");
            sb.append("<td>").append(it.getIvaImporte()).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        sb.append("<p>Subtotal: ").append(subtotal).append("</p>");
        sb.append("<p>IVA: ").append(iva).append("</p>");
        sb.append("<p>Total: ").append(total).append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String cnRelacionadosAsString(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) return "(sin relaciones)";
        return String.join(", ", uuids);
    }
}