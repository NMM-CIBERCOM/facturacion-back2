package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.NotasCreditoOracleDAO;
import com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO;
import com.cibercom.facturacion_back.dto.CreditNoteSaveRequest;
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

    public CreditNoteOracleSaveService(UuidFacturaOracleDAO uuidFacturaOracleDAO,
                                       NotasCreditoOracleDAO notasCreditoOracleDAO) {
        this.uuidFacturaOracleDAO = uuidFacturaOracleDAO;
        this.notasCreditoOracleDAO = notasCreditoOracleDAO;
    }

    public SaveResult guardar(CreditNoteSaveRequest req) {
        SaveResult result = new SaveResult();
        result.uuidNc = req.getUuidNc();
        List<String> errors = new ArrayList<>();

        // Defaults
        if (req.getFechaEmision() == null) req.setFechaEmision(LocalDateTime.now());
        if (req.getUsoCfdi() == null || req.getUsoCfdi().isBlank()) req.setUsoCfdi("G02");
        if (req.getFormaPago() == null || req.getFormaPago().isBlank()) req.setFormaPago("01");
        if (req.getMetodoPago() == null || req.getMetodoPago().isBlank()) req.setMetodoPago("PUE");

        // Insertar básico en FACTURAS para asegurar compatibilidad con reportes/consultas
        boolean okFactura = uuidFacturaOracleDAO.insertarBasico(
                req.getUuidNc(),
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
                req.getRfcEmisor()
        );
        if (!okFactura) {
            String err = uuidFacturaOracleDAO.getLastInsertError();
            if (err != null && !err.isBlank()) errors.add("FACTURAS: " + err);
        }

        // Asegurar existencia de FACTURA origen si es requerida por NOTAS_CREDITO
        if (req.getUuidFacturaOrig() != null && !req.getUuidFacturaOrig().isBlank()) {
            try {
                java.util.Optional<com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO.Result> orig = uuidFacturaOracleDAO.obtenerBasicosPorUuid(req.getUuidFacturaOrig());
                if (orig.isEmpty()) {
                    boolean okFacturaOrig = uuidFacturaOracleDAO.insertarBasico(
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
                            req.getRfcEmisor()
                    );
                    if (!okFacturaOrig) {
                        String err = uuidFacturaOracleDAO.getLastInsertError();
                        if (err != null && !err.isBlank()) errors.add("FACTURAS (ORIG): " + err);
                    }
                }
            } catch (Exception e) {
                errors.add("FACTURAS (ORIG): " + e.getMessage());
            }
        }

        // Insertar detalle en NOTAS_CREDITO
        boolean okNc = notasCreditoOracleDAO.insertar(req);
        if (!okNc) {
            String err = notasCreditoOracleDAO.getLastInsertError();
            if (err != null && !err.isBlank()) errors.add("NOTAS_CREDITO: " + err);
        }

        result.ok = okFactura && okNc;
        result.errors = errors;
        return result;
    }

    private java.math.BigDecimal nullSafe(java.math.BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public static class SaveResult {
        public boolean ok;
        public String uuidNc;
        public List<String> errors;
    }
}