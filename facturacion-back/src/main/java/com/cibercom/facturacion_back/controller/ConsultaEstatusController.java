package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.ConsultaEstatusRequest;
import com.cibercom.facturacion_back.dto.ConsultaEstatusResponse;
import com.cibercom.facturacion_back.service.ConsultaEstatusService;
import com.cibercom.facturacion_back.dao.ConsultaFacturaDAO;
import com.cibercom.facturacion_back.model.Factura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class ConsultaEstatusController {

    private static final Logger logger = LoggerFactory.getLogger(ConsultaEstatusController.class);

    @Autowired
    private ConsultaEstatusService consultaEstatusService;

    @Autowired
    private ConsultaFacturaDAO consultaFacturaDAO;

    @PostMapping("/estatus")
    public ResponseEntity<ConsultaEstatusResponse> consultarEstatus(
            @RequestBody ConsultaEstatusRequest request) {

        logger.info("Recibida solicitud de consulta de estatus para UUID: {}", request.getUuid());

        try {
            ConsultaFacturaDAO.FacturaInfo factura = consultaFacturaDAO.obtenerFacturaPorUuid(request.getUuid());

            if (factura != null) {
                String esCancelable = determinarSiEsCancelable(factura.estatus);

                String estado = factura.estatus;
                String estadoXml = estado;
                String estatusCancelacion = "";

                if (estado.equals("CANCELADA")) {
                    estadoXml = "Cancelado";
                    estatusCancelacion = "Cancelado sin aceptación";
                } else if (estado.equals("RECHAZADA")) {
                    estadoXml = "Rechazado";
                    estatusCancelacion = "Rechazado sin aceptación";
                }

                ConsultaEstatusResponse response = ConsultaEstatusResponse.exito(
                        "S - Comprobante obtenido satisfactoriamente.",
                        estadoXml,
                        esCancelable,
                        estatusCancelacion);

                logger.info("Consulta de estatus exitosa para UUID: {}, Estado: {}",
                        request.getUuid(), response.getEstado());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Factura no encontrada con UUID: {}", request.getUuid());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ConsultaEstatusResponse.error("Factura no encontrada"));
            }

        } catch (Exception e) {
            logger.error("Error interno al consultar estatus de factura", e);
            ConsultaEstatusResponse errorResponse = ConsultaEstatusResponse.error(
                    "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping(value = "/estatus/xml", consumes = "text/xml", produces = "text/xml")
    public ResponseEntity<String> consultarEstatusXml(@RequestBody String xmlRequest) {
        logger.info("Recibida solicitud XML para consulta de estatus");

        try {

            String uuid = extraerValorXml(xmlRequest, "<uuid>", "</uuid>");
            if (uuid == null || uuid.isEmpty()) {
                uuid = extraerValorXml(xmlRequest, "<liv:uuid>", "</liv:uuid>");
            }

            if (uuid == null || uuid.isEmpty()) {
                logger.warn("UUID no encontrado en la solicitud XML");
                return ResponseEntity.badRequest().body(generarXmlError("UUID no encontrado en la solicitud"));
            }

            ConsultaFacturaDAO.FacturaInfo factura = consultaFacturaDAO.obtenerFacturaPorUuid(uuid);

            if (factura != null) {
                String esCancelable = determinarSiEsCancelable(factura.estatus);
                String estado = factura.estatus;

                String estadoXml = estado;
                String estatusCancelacion = "";

                if (estado.equals("CANCELADA")) {
                    estadoXml = "Cancelado";
                    estatusCancelacion = "Cancelado sin aceptación";
                } else if (estado.equals("RECHAZADA")) {
                    estadoXml = "Rechazado";
                    estatusCancelacion = "Rechazado sin aceptación";
                }

                String xmlResponse = generarXmlRespuesta(
                        esCancelable,
                        "S - Comprobante obtenido satisfactoriamente.",
                        estadoXml,
                        estatusCancelacion);

                logger.info("Consulta de estatus XML exitosa para UUID: {}, Estado: {}", uuid, estadoXml);
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_XML)
                        .body(xmlResponse);
            } else {
                logger.warn("Factura no encontrada con UUID: {}", uuid);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_XML)
                        .body(generarXmlError("Factura no encontrada"));
            }

        } catch (Exception e) {
            logger.error("Error al procesar solicitud XML", e);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_XML)
                    .body(generarXmlError("Error interno del servidor: " + e.getMessage()));
        }
    }

    private String determinarSiEsCancelable(String estado) {
        if ("VIGENTE".equalsIgnoreCase(estado)) {
            return "Cancelable sin aceptación";
        } else if ("CANCELADA".equalsIgnoreCase(estado)) {
            return "No cancelable";
        } else {
            return "Cancelable con aceptación";
        }
    }

    private String extraerValorXml(String xml, String tagInicio, String tagFin) {
        try {
            int inicio = xml.indexOf(tagInicio);
            if (inicio == -1)
                return null;

            inicio += tagInicio.length();
            int fin = xml.indexOf(tagFin, inicio);

            if (fin == -1)
                return null;

            return xml.substring(inicio, fin).trim();
        } catch (Exception e) {
            logger.error("Error al extraer valor del XML", e);
            return null;
        }
    }

    /**
     * Genera una respuesta XML de error
     */
    private String generarXmlError(String mensaje) {
        return "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<senv:Envelope xmlns:senv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:s0=\"apps.services.soap.core.views\">\n" +
                "    <senv:Body>\n" +
                "        <senv:Fault>\n" +
                "            <faultcode>senv:Server</faultcode>\n" +
                "            <faultstring>" + mensaje + "</faultstring>\n" +
                "        </senv:Fault>\n" +
                "    </senv:Body>\n" +
                "</senv:Envelope>";
    }

    private String generarXmlRespuesta(String esCancelable, String codigoEstatus, String estado,
            String estatusCancelacion) {
        return "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<senv:Envelope xmlns:senv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:s0=\"apps.services.soap.core.views\">\n" +
                "    <senv:Body>\n" +
                "        <obtenerEstatusResponse>\n" +
                "            <obtenerEstatusResult>\n" +
                "                <s0:sat>\n" +
                "                    <s0:EsCancelable>" + esCancelable + "</s0:EsCancelable>\n" +
                "                    <s0:CodigoEstatus>" + codigoEstatus + "</s0:CodigoEstatus>\n" +
                "                    <s0:Estado>" + estado + "</s0:Estado>\n" +
                "                    <s0:EstatusCancelacion>" + estatusCancelacion + "</s0:EstatusCancelacion>\n" +
                "                </s0:sat>\n" +
                "            </obtenerEstatusResult>\n" +
                "        </obtenerEstatusResponse>\n" +
                "    </senv:Body>\n" +
                "</senv:Envelope>";
    }
}