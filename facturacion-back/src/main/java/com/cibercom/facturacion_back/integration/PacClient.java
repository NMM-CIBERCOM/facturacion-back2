package com.cibercom.facturacion_back.integration;

import com.cibercom.facturacion_back.dto.PacTimbradoRequest;
import com.cibercom.facturacion_back.dto.PacTimbradoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class PacClient {
    private static final Logger logger = LoggerFactory.getLogger(PacClient.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "http://localhost:8085/api/pac";

    public PacResponse solicitarCancelacion(PacRequest req) {
        try {
            String url = baseUrl + "/cancel";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PacRequest> entity = new HttpEntity<>(req, headers);
            ResponseEntity<PacResponse> response = restTemplate.postForEntity(url, entity, PacResponse.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error llamando PAC: {}", e.getMessage());
            PacResponse r = new PacResponse();
            r.setOk(false);
            r.setStatus("ERROR");
            r.setMessage("PAC no disponible: " + e.getMessage());
            return r;
        }
    }

    public PacTimbradoResponse solicitarTimbrado(PacTimbradoRequest req) {
        try {
            String url = baseUrl + "/stamp";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PacTimbradoRequest> entity = new HttpEntity<>(req, headers);
            ResponseEntity<PacTimbradoResponse> response = restTemplate.postForEntity(url, entity, PacTimbradoResponse.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error llamando PAC para timbrado: {}", e.getMessage());
            PacTimbradoResponse r = new PacTimbradoResponse();
            r.setOk(false);
            r.setStatus("ERROR");
            r.setMessage("PAC no disponible para timbrado: " + e.getMessage());
            return r;
        }
    }

    public static class PacRequest {
        public String uuid;
        public String motivo; // 01..04
        public String rfcEmisor;
        public String rfcReceptor;
        public Double total;
        public String tipo; // INGRESO, EGRESO, NOMINA, TRASLADO
        public String fechaFactura; // ISO-8601
        public Boolean publicoGeneral;
        public Boolean tieneRelaciones;
        public String uuidSustituto;
    }

    public static class PacResponse {
        private Boolean ok;
        private String status; // CANCELADA, EN_PROCESO, RECHAZADA, ERROR
        private String receiptId;
        private String message;

        public Boolean getOk() { return ok; }
        public void setOk(Boolean ok) { this.ok = ok; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String receiptId) { this.receiptId = receiptId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}


