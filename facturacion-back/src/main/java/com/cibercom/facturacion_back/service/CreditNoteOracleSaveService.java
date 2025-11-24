package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.NotasCreditoOracleDAO;
import com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO;
import com.cibercom.facturacion_back.dto.CreditNoteSaveRequest;
import com.cibercom.facturacion_back.model.ClienteCatalogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile("oracle")
public class CreditNoteOracleSaveService {

    private final UuidFacturaOracleDAO uuidFacturaOracleDAO;
    private final NotasCreditoOracleDAO notasCreditoOracleDAO;
    private final ClienteCatalogoService clienteCatalogoService;
    private static final Logger logger = LoggerFactory.getLogger(CreditNoteOracleSaveService.class);

    public CreditNoteOracleSaveService(UuidFacturaOracleDAO uuidFacturaOracleDAO,
                                       NotasCreditoOracleDAO notasCreditoOracleDAO,
                                       ClienteCatalogoService clienteCatalogoService) {
        this.uuidFacturaOracleDAO = uuidFacturaOracleDAO;
        this.notasCreditoOracleDAO = notasCreditoOracleDAO;
        this.clienteCatalogoService = clienteCatalogoService;
    }

    public SaveResult guardar(CreditNoteSaveRequest req) {
        SaveResult result = new SaveResult();
        // Asegurar UUID_NC: si no viene, generar uno y usarlo en todo el flujo
        String uuidNc = req.getUuidNc();
        if (uuidNc == null || uuidNc.isBlank()) {
            uuidNc = java.util.UUID.randomUUID().toString().toUpperCase();
            req.setUuidNc(uuidNc);
            logger.info("NC Oracle Guardar: UUID_NC generado {}", uuidNc);
        }
        result.uuidNc = uuidNc;
        List<String> errors = new ArrayList<>();

        // Defaults
        if (req.getFechaEmision() == null) req.setFechaEmision(LocalDateTime.now());
        if (req.getUsoCfdi() == null || req.getUsoCfdi().isBlank()) req.setUsoCfdi("G02");
        if (req.getFormaPago() == null || req.getFormaPago().isBlank()) req.setFormaPago("01");
        if (req.getMetodoPago() == null || req.getMetodoPago().isBlank()) req.setMetodoPago("PUE");

        // Resolver ID_RECEPTOR por RFC (CLIENTES); crear si no existe
        Long idReceptor = resolverIdReceptorPorRfc(req.getRfcReceptor());
        logger.info("NC Oracle Guardar: rfcReceptor={} idReceptor={} uuidNc={}", req.getRfcReceptor(), idReceptor, req.getUuidNc());

        // Insertar básico en FACTURAS para asegurar compatibilidad con reportes/consultas
        boolean okFactura = uuidFacturaOracleDAO.insertarBasicoConIdReceptor(
                uuidNc,
                req.getXmlContent(),
                req.getSerieNc(),
                req.getFolioNc(),
                nullSafe(req.getSubtotal()),
                nullSafe(req.getIvaImporte()),
                nullSafe(req.getIepsImporte()),
                nullSafe(req.getTotal()),
                req.getFormaPago(),
                req.getUsoCfdi(),
                "EMITIDA",
                "NOTA_CREDITO",
                req.getMetodoPago(),
                req.getRfcReceptor(),
                req.getRfcEmisor(),
                null,
                idReceptor,
                Integer.valueOf(2)
        );
        if (!okFactura) {
            String err = uuidFacturaOracleDAO.getLastInsertError();
            if (err != null && !err.isBlank()) errors.add("FACTURAS: " + err);
            logger.error("NC Oracle Guardar: error insertando FACTURAS -> {}", err);
        }

        // Asegurar existencia de FACTURA origen si es requerida por NOTAS_CREDITO
        if (req.getUuidFacturaOrig() != null && !req.getUuidFacturaOrig().isBlank()) {
            try {
                java.util.Optional<com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO.Result> orig = uuidFacturaOracleDAO.obtenerBasicosPorUuid(req.getUuidFacturaOrig());
                if (orig.isEmpty()) {
                    logger.info("NC Oracle Guardar: FACTURA origen no encontrada, insertando básica uuidOrig={} serie={} folio={} idReceptor={}",
                            req.getUuidFacturaOrig(), req.getSerieFacturaOrig(), req.getFolioFacturaOrig(), idReceptor);
                    boolean okFacturaOrig = uuidFacturaOracleDAO.insertarBasicoConIdReceptor(
                            req.getUuidFacturaOrig(),
                            null,
                            req.getSerieFacturaOrig(),
                            req.getFolioFacturaOrig(),
                            java.math.BigDecimal.ZERO,
                            java.math.BigDecimal.ZERO,
                            java.math.BigDecimal.ZERO,
                            java.math.BigDecimal.ZERO,
                            req.getFormaPago(),
                            req.getUsoCfdi(),
                            "EMITIDA",
                            "FACTURA_ORIGEN",
                            req.getMetodoPago(),
                            req.getRfcReceptor(),
                            req.getRfcEmisor(),
                            null,
                            idReceptor,
                            null
                    );
                    if (!okFacturaOrig) {
                        String err = uuidFacturaOracleDAO.getLastInsertError();
                        if (err != null && !err.isBlank()) errors.add("FACTURAS (ORIG): " + err);
                        logger.error("NC Oracle Guardar: error insertando FACTURAS (ORIG) -> {}", err);
                    }
                }
            } catch (Exception e) {
                errors.add("FACTURAS (ORIG): " + e.getMessage());
                logger.error("NC Oracle Guardar: excepción preparando FACTURA origen -> {}", e.getMessage());
            }
        }

        // Insertar detalle en NOTAS_CREDITO
        boolean okNc = notasCreditoOracleDAO.insertar(req);
        if (!okNc) {
            String err = notasCreditoOracleDAO.getLastInsertError();
            if (err != null && !err.isBlank()) errors.add("NOTAS_CREDITO: " + err);
            logger.error("NC Oracle Guardar: error insertando NOTAS_CREDITO -> {}", err);
        }

        result.ok = okFactura && okNc;
        result.errors = errors;
        logger.info("NC Oracle Guardar: resultado ok={} uuidNc={} errors={}", result.ok, result.uuidNc, errors);
        return result;
    }

    private java.math.BigDecimal nullSafe(java.math.BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * Resuelve el ID_RECEPTOR en la tabla CLIENTES por RFC.
     * Si no existe, crea un registro mínimo usando el RFC como razón social.
     */
    private Long resolverIdReceptorPorRfc(String rfc) {
        if (rfc == null || rfc.trim().isEmpty()) return null;
        String normalized = rfc.trim().toUpperCase();
        java.util.Optional<ClienteCatalogo> existente = clienteCatalogoService.buscarPorRfc(normalized);
        if (existente.isPresent()) {
            return existente.get().getIdCliente();
        }
        ClienteCatalogo nuevo = new ClienteCatalogo();
        nuevo.setRfc(normalized);
        nuevo.setRazonSocial(normalized);
        ClienteCatalogo guardado = clienteCatalogoService.guardar(nuevo);
        return guardado != null ? guardado.getIdCliente() : null;
    }

    public static class SaveResult {
        public boolean ok;
        public String uuidNc;
        public List<String> errors;
    }
}