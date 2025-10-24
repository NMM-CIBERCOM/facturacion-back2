package com.cibercom.facturacion_back.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class BitacoraClient {
    private static final Logger logger = LoggerFactory.getLogger(BitacoraClient.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${bitacora.baseUrl:}")
    private String baseUrl;

    public void registrarEvento(String modulo, String operacion, String estatus, String mensaje, Map<String, Object> detalles) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("timestamp", OffsetDateTime.now().toString());
        payload.put("modulo", modulo);
        payload.put("operacion", operacion);
        payload.put("estatus", estatus);
        payload.put("mensaje", mensaje);
        payload.put("detalles", detalles);

        try {
            if (baseUrl != null && !baseUrl.isBlank()) {
                String url = baseUrl.endsWith("/") ? baseUrl + "registrar" : baseUrl + "/registrar";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                logger.info("Bitácora enviada: modulo={} operacion={} estatus={} statusCode={}", modulo, operacion, estatus, response.getStatusCodeValue());
            } else {
                logger.info("Bitácora (local): modulo={} operacion={} estatus={} mensaje={} detalles={}", modulo, operacion, estatus, mensaje, safeDetalles(detalles));
            }
        } catch (Exception e) {
            logger.warn("Bitácora no disponible, registrando localmente: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            logger.info("Bitácora (fallback): modulo={} operacion={} estatus={} mensaje={} detalles={}", modulo, operacion, estatus, mensaje, safeDetalles(detalles));
        }
    }

    private String safeDetalles(Map<String, Object> detalles) {
        try {
            return detalles != null ? detalles.toString() : "{}";
        } catch (Exception ignored) {
            return "{}";
        }
    }
}