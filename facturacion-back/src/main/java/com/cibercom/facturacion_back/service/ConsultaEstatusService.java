package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.ConsultaEstatusRequest;
import com.cibercom.facturacion_back.dto.ConsultaEstatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class ConsultaEstatusService {

    private static final Logger logger = LoggerFactory.getLogger(ConsultaEstatusService.class);

    @Value("${facturacion.soap.url:http://facturacion.finkok.com/liverpool}")
    private String soapEndpoint;

    private final RestTemplate restTemplate;

    public ConsultaEstatusService() {
        this.restTemplate = new RestTemplate();
    }

    public ConsultaEstatusResponse consultarEstatus(ConsultaEstatusRequest request) {
        try {
            String soapRequest = buildSoapRequest(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);

            HttpEntity<String> httpEntity = new HttpEntity<>(soapRequest, headers);

            String response = restTemplate.postForObject(soapEndpoint, httpEntity, String.class);

            return procesarRespuesta(response);

        } catch (HttpClientErrorException e) {
            logger.error("Error al consultar estatus de factura: {}", e.getMessage());
            return ConsultaEstatusResponse.error("Error al comunicarse con el servicio: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al consultar estatus: {}", e.getMessage());
            return ConsultaEstatusResponse.error("Error inesperado: " + e.getMessage());
        }
    }

    private String buildSoapRequest(ConsultaEstatusRequest request) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:liv=\"http://facturacion.finkok.com/liverpool\">\n"
                +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <liv:obtenerEstatus>\n" +
                "         <liv:username>" + request.getUsername() + "</liv:username>\n" +
                "         <liv:password>" + request.getPassword() + "</liv:password>\n" +
                "         <liv:uuid>" + request.getUuid() + "</liv:uuid>\n" +
                "         <liv:rfcEmisor>" + request.getRfcEmisor() + "</liv:rfcEmisor>\n" +
                "         <liv:rfcReceptor>" + request.getRfcReceptor() + "</liv:rfcReceptor>\n" +
                "         <liv:total>" + request.getTotal() + "</liv:total>\n" +
                "      </liv:obtenerEstatus>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private ConsultaEstatusResponse procesarRespuesta(String xmlResponse) {
        try {

            String codigoEstatus = extraerValor(xmlResponse, "<s0:CodigoEstatus>", "</s0:CodigoEstatus>");

            String estado = extraerValor(xmlResponse, "<s0:Estado>", "</s0:Estado>");

            String esCancelable = extraerValor(xmlResponse, "<s0:EsCancelable>", "</s0:EsCancelable>");

            String estatusCancelacion = extraerValor(xmlResponse, "<s0:EstatusCancelacion>",
                    "</s0:EstatusCancelacion>");

            return ConsultaEstatusResponse.exito(codigoEstatus, estado, esCancelable, estatusCancelacion);

        } catch (Exception e) {
            logger.error("Error al procesar respuesta XML: {}", e.getMessage());
            return ConsultaEstatusResponse.error("Error al procesar la respuesta: " + e.getMessage());
        }
    }

    private String extraerValor(String xml, String tagInicio, String tagFin) {
        try {
            int inicio = xml.indexOf(tagInicio) + tagInicio.length();
            int fin = xml.indexOf(tagFin);
            if (inicio >= 0 && fin >= 0) {
                return xml.substring(inicio, fin);
            }
            return "";
        } catch (Exception e) {
            logger.warn("No se pudo extraer el valor entre {} y {}", tagInicio, tagFin);
            return "";
        }
    }
}