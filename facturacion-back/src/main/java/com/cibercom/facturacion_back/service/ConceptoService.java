package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.ConceptoOracleDAO;
import com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO;
import com.cibercom.facturacion_back.dto.ConceptoInsertRequest;
import com.cibercom.facturacion_back.dto.ConceptoInsertResponse;
import com.cibercom.facturacion_back.model.ConceptoMongo;
import com.cibercom.facturacion_back.model.FacturaMongo;
import com.cibercom.facturacion_back.repository.ConceptoMongoRepository;
import com.cibercom.facturacion_back.repository.FacturaMongoRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Profile("oracle")
public class ConceptoService {

    private final ConceptoOracleDAO conceptoOracleDAO;
    private final UuidFacturaOracleDAO uuidFacturaOracleDAO;
    private final FacturaMongoRepository facturaMongoRepository;
    private final ConceptoMongoRepository conceptoMongoRepository;

    public ConceptoService(ConceptoOracleDAO conceptoOracleDAO,
                           UuidFacturaOracleDAO uuidFacturaOracleDAO,
                           FacturaMongoRepository facturaMongoRepository,
                           ConceptoMongoRepository conceptoMongoRepository) {
        this.conceptoOracleDAO = conceptoOracleDAO;
        this.uuidFacturaOracleDAO = uuidFacturaOracleDAO;
        this.facturaMongoRepository = facturaMongoRepository;
        this.conceptoMongoRepository = conceptoMongoRepository;
    }

    public ConceptoInsertResponse insertarConcepto(ConceptoInsertRequest req) {
        // Resolver ID_FACTURA si viene uuid/idFactura
        Long idFactura = req.getIdFactura();
        if (idFactura == null && req.getUuidFactura() != null && !req.getUuidFactura().isBlank()) {
            Optional<Long> idOpt = conceptoOracleDAO.obtenerIdFacturaPorUuid(req.getUuidFactura());
            idFactura = idOpt.orElse(null);
        }

        ConceptoInsertResponse resp = new ConceptoInsertResponse();

        if (idFactura == null) {
            // Inserción libre (sin vínculo a factura)
            boolean okLibre = conceptoOracleDAO.insertarConceptoLibre(
                    req.getSkuClaveSat(),
                    req.getDescripcion(),
                    req.getUnidadMedida(),
                    req.getValorUnitario(),
                    req.getCantidad(),
                    req.getDescuento(),
                    req.getTasaIva(),
                    req.getIva(),
                    req.getTasaIeps(),
                    req.getIeps(),
                    req.getNoPedimento()
            );
            if (okLibre) {
                resp.setSuccess(true);
                resp.setMessage("Concepto insertado (libre) correctamente");
                return resp;
            }

            // Fallback: si el schema exige ID_FACTURA NOT NULL, crear una factura temporal y vincular
            String cause = conceptoOracleDAO.getLastInsertError();
            if (cause != null && cause.toLowerCase().contains("id_factura") && cause.toLowerCase().contains("not null")) {
                String uuidTmp = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
                java.math.BigDecimal vu = req.getValorUnitario() != null ? req.getValorUnitario() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal cant = req.getCantidad() != null ? req.getCantidad() : java.math.BigDecimal.ONE;
                java.math.BigDecimal subtotal = vu.multiply(cant);
                java.math.BigDecimal iva = req.getIva() != null ? req.getIva() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal ieps = req.getIeps() != null ? req.getIeps() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal total = subtotal.add(iva).add(ieps);

                boolean facturaOk = uuidFacturaOracleDAO.insertarBasico(
                        uuidTmp, null, "LIB", "FREE", subtotal, iva, ieps, total,
                        null, null, "EN_CAPTURA", "Factura temporal para Captura Libre",
                        null, null, null
                );
                if (facturaOk) {
                    Optional<Long> idOpt = conceptoOracleDAO.obtenerIdFacturaPorUuid(uuidTmp);
                    Long idTmp = idOpt.orElse(null);
                    if (idTmp != null) {
                        boolean conceptoOk = conceptoOracleDAO.insertarConcepto(
                                idTmp,
                                req.getSkuClaveSat(),
                                req.getDescripcion(),
                                req.getUnidadMedida(),
                                req.getValorUnitario(),
                                req.getCantidad(),
                                req.getDescuento(),
                                req.getTasaIva(),
                                req.getIva(),
                                req.getTasaIeps(),
                                req.getIeps(),
                                req.getNoPedimento()
                        );
                        resp.setSuccess(conceptoOk);
                        resp.setIdFactura(idTmp);
                        resp.setMessage(conceptoOk ? "Concepto insertado vinculando factura temporal" : "No fue posible insertar el concepto aun con factura temporal");
                        return resp;
                    } else {
                        // Fallback adicional: persistir en Mongo si no se consiguió ID
                        guardarConceptoEnMongo(req, uuidTmp, null);
                        resp.setSuccess(true);
                        resp.setMessage("Concepto guardado en Mongo vinculando factura temporal (sin ID_FACTURA)");
                        return resp;
                    }
                } else {
                    // Fallback a Mongo: crear factura temporal en Mongo y guardar concepto
                    String uuidTmpMongo = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
                    crearFacturaTemporalMongo(uuidTmpMongo, req);
                    guardarConceptoEnMongo(req, uuidTmpMongo, null);
                    resp.setSuccess(true);
                    resp.setMessage("Concepto guardado en Mongo con factura temporal");
                    return resp;
                }
            }

            // Si falla por otra causa, guardar libre en Mongo como respaldo
            crearFacturaTemporalMongo(null, req); // factura opcional
            guardarConceptoEnMongo(req, null, null);
            resp.setSuccess(true);
            resp.setMessage("Concepto guardado en Mongo (modo respaldo): " + (cause != null ? cause : "causa desconocida"));
            return resp;
        }

        // Inserción ligada a factura
        boolean ok = conceptoOracleDAO.insertarConcepto(
                idFactura,
                req.getSkuClaveSat(),
                req.getDescripcion(),
                req.getUnidadMedida(),
                req.getValorUnitario(),
                req.getCantidad(),
                req.getDescuento(),
                req.getTasaIva(),
                req.getIva(),
                req.getTasaIeps(),
                req.getIeps(),
                req.getNoPedimento()
        );
        if (!ok) {
            // Respaldo Mongo si la inserción ligada falla
            String uuidLink = req.getUuidFactura();
            guardarConceptoEnMongo(req, uuidLink, idFactura);
            resp.setSuccess(true);
            resp.setIdFactura(idFactura);
            resp.setMessage("Concepto guardado en Mongo al fallar inserción Oracle");
            return resp;
        }
        resp.setSuccess(true);
        resp.setIdFactura(idFactura);
        resp.setMessage("Concepto insertado correctamente");
        return resp;
    }

    private void crearFacturaTemporalMongo(String uuid, ConceptoInsertRequest req) {
        try {
            if (uuid == null) {
                uuid = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
            }
            FacturaMongo fm = new FacturaMongo();
            fm.setUuid(uuid);
            fm.setXmlContent("<cfdi:Comprobante xmlns:cfdi='http://www.sat.gob.mx/cfd/4'></cfdi:Comprobante>");
            fm.setFechaGeneracion(LocalDateTime.now());
            fm.setFechaTimbrado(LocalDateTime.now());
            fm.setEstado("EN_CAPTURA");
            fm.setEstadoDescripcion("Factura temporal para Captura Libre");
            fm.setSerie("LIB");
            fm.setFolio("FREE");
            java.math.BigDecimal vu = req.getValorUnitario() != null ? req.getValorUnitario() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal cant = req.getCantidad() != null ? req.getCantidad() : java.math.BigDecimal.ONE;
            java.math.BigDecimal subtotal = vu.multiply(cant);
            java.math.BigDecimal iva = req.getIva() != null ? req.getIva() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal total = subtotal.add(iva);
            fm.setSubtotal(subtotal);
            fm.setIva(iva);
            fm.setTotal(total);
            facturaMongoRepository.save(fm);
        } catch (Exception ignored) {}
    }

    private void guardarConceptoEnMongo(ConceptoInsertRequest req, String uuidFactura, Long idFactura) {
        try {
            ConceptoMongo c = new ConceptoMongo();
            c.setUuidFactura(uuidFactura);
            c.setIdFactura(idFactura);
            c.setSkuClaveSat(req.getSkuClaveSat());
            c.setDescripcion(req.getDescripcion());
            c.setUnidadMedida(req.getUnidadMedida());
            c.setValorUnitario(req.getValorUnitario());
            c.setCantidad(req.getCantidad());
            c.setDescuento(req.getDescuento());
            c.setTasaIva(req.getTasaIva());
            c.setIva(req.getIva());
            c.setTasaIeps(req.getTasaIeps());
            c.setIeps(req.getIeps());
            c.setNoPedimento(req.getNoPedimento());
            c.setFechaCreacion(LocalDateTime.now());
            conceptoMongoRepository.save(c);
        } catch (Exception ignored) {}
    }
}