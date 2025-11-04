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
    private final String baseUrl;

    public PacClient() {
        String defecto = "http://localhost:8085/api/pac";
        String env = System.getenv("PAC_BASE_URL");
        String prop = System.getProperty("pac.base-url");
        String elegido = (env != null && !env.isBlank()) ? env : ((prop != null && !prop.isBlank()) ? prop : defecto);
        // Normalizar quitando slashes finales
        this.baseUrl = elegido.replaceAll("/+$", "");
        logger.info("PacClient baseUrl={}", this.baseUrl);
    }

    public PacResponse solicitarCancelacion(PacRequest req) {
        try {
            String url = baseUrl + "/cancel";
            logger.info("HTTP -> PAC cancelacion: url={} uuid={} motivo={} total={} tipo={} fecha={}",
                    url, req.uuid, req.motivo, req.total, req.tipo, req.fechaFactura);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PacRequest> entity = new HttpEntity<>(req, headers);
            ResponseEntity<PacResponse> response = restTemplate.postForEntity(url, entity, PacResponse.class);
            PacResponse body = response.getBody();
            logger.info("HTTP <- PAC cancelacion: statusCode={} ok={} status={} receiptId={} message={}",
                    response.getStatusCodeValue(),
                    body != null ? body.getOk() : null,
                    body != null ? body.getStatus() : null,
                    body != null ? body.getReceiptId() : null,
                    body != null ? body.getMessage() : null);
            return body;
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
            logger.info("HTTP -> PAC timbrado: url={} uuid={} tipo={} total={} relacionados={}",
                    url, req.getUuid(), req.getTipo(), req.getTotal(), req.getRelacionadosUuids());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PacTimbradoRequest> entity = new HttpEntity<>(req, headers);
            ResponseEntity<PacTimbradoResponse> response = restTemplate.postForEntity(url, entity,
                    PacTimbradoResponse.class);
            PacTimbradoResponse body = response.getBody();
            logger.info("HTTP <- PAC timbrado: statusCode={} ok={} status={} uuid={} receiptId={} message={}",
                    response.getStatusCodeValue(),
                    body != null ? body.getOk() : null,
                    body != null ? body.getStatus() : null,
                    body != null ? body.getUuid() : null,
                    body != null ? body.getReceiptId() : null,
                    body != null ? body.getMessage() : null);
            return body;
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
        public String motivo;
        public String rfcEmisor;
        public String rfcReceptor;
        public Double total;
        public String tipo; // INGRESO, EGRESO, NOMINA, TRASLADO
        public String fechaFactura;
        public Boolean publicoGeneral;
        public Boolean tieneRelaciones;
        public String uuidSustituto;
    }

    public static class PacResponse {
        private Boolean ok;
        private String status; // CANCELADA, EN_PROCESO, RECHAZADA, ERROR
        private String receiptId;
        private String message;

        public Boolean getOk() {
            return ok;
        }

        public void setOk(Boolean ok) {
            this.ok = ok;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getReceiptId() {
            return receiptId;
        }

        public void setReceiptId(String receiptId) {
            this.receiptId = receiptId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}



