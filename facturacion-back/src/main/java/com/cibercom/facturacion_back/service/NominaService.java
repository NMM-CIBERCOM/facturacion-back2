package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.ConceptoOracleDAO;
import com.cibercom.facturacion_back.dao.NominaOracleDAO;
import com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO;
import com.cibercom.facturacion_back.dto.NominaSaveRequest;
import com.cibercom.facturacion_back.dto.NominaSaveResponse;
import com.cibercom.facturacion_back.model.ClienteCatalogo;
import com.cibercom.facturacion_back.service.ClienteCatalogoService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("oracle")
public class NominaService {

    private final UuidFacturaOracleDAO uuidFacturaOracleDAO;
    private final ConceptoOracleDAO conceptoOracleDAO;
    private final NominaOracleDAO nominaOracleDAO;
    private final ClienteCatalogoService clienteCatalogoService;

    public NominaService(UuidFacturaOracleDAO uuidFacturaOracleDAO,
                         ConceptoOracleDAO conceptoOracleDAO,
                         NominaOracleDAO nominaOracleDAO,
                         ClienteCatalogoService clienteCatalogoService) {
        this.uuidFacturaOracleDAO = uuidFacturaOracleDAO;
        this.conceptoOracleDAO = conceptoOracleDAO;
        this.nominaOracleDAO = nominaOracleDAO;
        this.clienteCatalogoService = clienteCatalogoService;
    }

    public NominaSaveResponse guardar(NominaSaveRequest req) {
        List<String> errors = new ArrayList<>();

        // Validaciones básicas
        if (isBlank(req.getRfcEmisor())) errors.add("RFC emisor requerido");
        if (isBlank(req.getRfcReceptor())) errors.add("RFC receptor requerido");
        if (isBlank(req.getNombre())) errors.add("Nombre requerido");
        if (isBlank(req.getIdEmpleado())) errors.add("ID empleado requerido");
        if (isBlank(req.getFechaPago())) errors.add("Fecha de pago requerida");
        if (!errors.isEmpty()) {
            NominaSaveResponse resp = new NominaSaveResponse();
            resp.setOk(false);
            resp.setMessage("Validación fallida");
            resp.setErrors(errors);
            return resp;
        }

        // Resolver valores monetarios
        BigDecimal percep = parse(req.getPercepciones());
        BigDecimal deduc = parse(req.getDeducciones());
        BigDecimal total = parse(req.getTotal());
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            total = percep.subtract(deduc);
        }
        if (percep == null) percep = BigDecimal.ZERO;
        if (deduc == null) deduc = BigDecimal.ZERO;

        String uuid = req.getUuidFactura();
        if (isBlank(uuid)) {
            uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        }

        // Resolver ID_RECEPTOR por RFC (CLIENTES); crear si no existe
        Long idReceptor = resolverIdReceptorPorRfc(req.getRfcReceptor(), req.getNombre());

        // Generar XML básico de nómina para almacenar en FACTURAS
        String xmlNomina = generarXmlNomina(req, percep, deduc, total);

        // Insertar FACTURAS (básico y tolerante) usando el DAO dinámico, incluyendo ID_RECEPTOR si es requerido
        boolean okFactura = uuidFacturaOracleDAO.insertarBasicoConIdReceptor(
                uuid,
                xmlNomina,
                "NOM", // serie sugerida
                null,
                percep.subtract(deduc), // subtotal aproximado
                BigDecimal.ZERO, // IVA
                BigDecimal.ZERO, // IEPS
                total, // total
                "99", // forma pago
                safe(req.getUsoCfdi(), "CN01"),
                "EN_CAPTURA",
                "Factura vinculada a Nómina",
                "PUE",
                req.getRfcReceptor(),
                req.getRfcEmisor(),
                idReceptor
        );
        if (!okFactura) {
            NominaSaveResponse resp = new NominaSaveResponse();
            resp.setOk(false);
            String err = uuidFacturaOracleDAO.getLastInsertError();
            resp.setMessage(err != null ? ("Error insertando FACTURAS: " + err) : "Error insertando FACTURAS");
            return resp;
        }

        // Obtener ID_FACTURA por UUID para FK
        Optional<Long> idOpt = conceptoOracleDAO.obtenerIdFacturaPorUuid(uuid);
        if (idOpt.isEmpty() || idOpt.get() == null) {
            NominaSaveResponse resp = new NominaSaveResponse();
            resp.setOk(false);
            resp.setMessage("No se pudo resolver ID_FACTURA por UUID");
            return resp;
        }
        Long idFactura = idOpt.get();

        // Insertar en NOMINAS
        Long idNomina = nominaOracleDAO.insertar(idFactura, req);
        if (idNomina == null) {
            NominaSaveResponse resp = new NominaSaveResponse();
            resp.setOk(false);
            String err = nominaOracleDAO.getLastInsertError();
            resp.setMessage(err != null ? ("Error insertando NOMINAS: " + err) : "Error insertando NOMINAS");
            return resp;
        }

        // Éxito
        NominaSaveResponse resp = new NominaSaveResponse();
        resp.setOk(true);
        resp.setMessage("Guardado correctamente en FACTURAS y NOMINAS");
        resp.setUuidFactura(uuid);
        resp.setIdFactura(idFactura);
        resp.setIdFacturaNomina(idNomina);
        return resp;
    }

    /**
     * Consulta historial de nóminas por empleado.
     */
    public java.util.List<com.cibercom.facturacion_back.dto.NominaHistorialDTO> consultarHistorial(String idEmpleado) {
        if (idEmpleado == null || idEmpleado.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return nominaOracleDAO.buscarHistorialPorEmpleado(idEmpleado.trim());
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private BigDecimal parse(String s) { try { return s == null ? BigDecimal.ZERO : new BigDecimal(s.trim()); } catch (Exception e) { return BigDecimal.ZERO; } }
    private String safe(String s, String def) { return isBlank(s) ? def : s; }

    private Long resolverIdReceptorPorRfc(String rfc, String nombre) {
        if (isBlank(rfc)) return null;
        String normalized = rfc.trim().toUpperCase();
        java.util.Optional<ClienteCatalogo> existente = clienteCatalogoService.buscarPorRfc(normalized);
        if (existente.isPresent()) {
            return existente.get().getIdCliente();
        }
        ClienteCatalogo nuevo = new ClienteCatalogo();
        nuevo.setRfc(normalized);
        String razonSocial = (nombre != null && !nombre.trim().isEmpty()) ? nombre.trim() : normalized;
        nuevo.setRazonSocial(razonSocial);
        ClienteCatalogo guardado = clienteCatalogoService.guardar(nuevo);
        return guardado != null ? guardado.getIdCliente() : null;
    }

    /**
     * Construye un XML básico de CFDI 4.0 con complemento Nómina (esqueleto) usando campos
     * del request. Este XML no timbrado sirve para almacenar y permitir descarga inicial.
     */
    private String generarXmlNomina(NominaSaveRequest req, BigDecimal percep, BigDecimal deduc, BigDecimal total) {
        String serie = "NOM";
        String folio = "1";
        String formaPago = "99";
        String metodoPago = "PUE";
        String usoCfdi = safe(req.getUsoCfdi(), "CN01");

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<cfdi:Comprobante Version=\"4.0\" ");
        xml.append("Serie=\""+serie+"\" ");
        xml.append("Folio=\""+folio+"\" ");
        xml.append("SubTotal=\""+percep.subtract(deduc)+"\" ");
        xml.append("Descuento=\""+deduc+"\" ");
        xml.append("Total=\""+total+"\" ");
        xml.append("Moneda=\"MXN\" ");
        xml.append("TipoDeComprobante=\"N\" ");
        xml.append("FormaPago=\""+formaPago+"\" ");
        xml.append("MetodoPago=\""+metodoPago+"\" ");
        xml.append("LugarExpedicion=\"00000\" "); // default simple para evitar depender de campos no presentes
        xml.append("xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\" ");
        xml.append("xmlns:nomina12=\"http://www.sat.gob.mx/nomina12\" ");
        xml.append(">\n");

        // Emisor y Receptor (usar valores disponibles en el request)
        xml.append("  <cfdi:Emisor Rfc=\""+req.getRfcEmisor()+"\" Nombre=\""+safe(req.getRfcEmisor(), req.getRfcEmisor())+"\" RegimenFiscal=\"601\"/>\n");
        xml.append("  <cfdi:Receptor Rfc=\""+req.getRfcReceptor()+"\" Nombre=\""+safe(req.getNombre(), req.getRfcReceptor())+"\" DomicilioFiscalReceptor=\"00000\" UsoCFDI=\""+usoCfdi+"\"/>\n");

        // Conceptos mínimos
        xml.append("  <cfdi:Conceptos>\n");
        xml.append("    <cfdi:Concepto ClaveProdServ=\"84111505\" Cantidad=\"1\" ClaveUnidad=\"ACT\" Descripcion=\"Pago de nómina\" ValorUnitario=\""+total+"\" Importe=\""+total+"\"/>\n");
        xml.append("  </cfdi:Conceptos>\n");

        // Complemento nómina básico
        xml.append("  <cfdi:Complemento>\n");
        xml.append("    <nomina12:Nomina Version=\"1.2\" TipoNomina=\""+safe(req.getTipoNomina(), "O")+"\" FechaPago=\""+safe(req.getFechaPago(), "2024-01-01")+"\" NumDiasPagados=\"1\" TotalPercepciones=\""+percep+"\" TotalDeducciones=\""+deduc+"\" TotalOtrosPagos=\"0\"/>\n");
        xml.append("  </cfdi:Complemento>\n");

        xml.append("</cfdi:Comprobante>");
        return xml.toString();
    }
}