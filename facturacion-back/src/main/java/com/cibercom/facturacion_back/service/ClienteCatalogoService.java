package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.model.ClienteCatalogo;
import com.cibercom.facturacion_back.repository.ClienteCatalogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ClienteCatalogoService {

    @Autowired
    private ClienteCatalogoRepository clienteCatalogoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger log = LoggerFactory.getLogger(ClienteCatalogoService.class);

    public Optional<ClienteCatalogo> buscarPorRfc(String rfc) {
        if (!StringUtils.hasText(rfc)) {
            log.warn("Búsqueda cliente catálogo: RFC vacío o nulo");
            return Optional.empty();
        }
        String normalized = rfc.trim().toUpperCase();
        log.info("Consultando tabla CLIENTES por RFC: {}", normalized);
        Optional<ClienteCatalogo> result = clienteCatalogoRepository.findByRfcNormalized(normalized);
        if (result.isEmpty()) {
            log.info("CLIENTES RFC {} no encontrado con igualdad exacta; probando ignorar prefijo", normalized);
            result = clienteCatalogoRepository.findByRfcOrPrefixed(normalized);
        }
        log.info("Resultado CLIENTES RFC {}: {}", normalized, result.isPresent() ? "ENCONTRADO" : "NO_ENCONTRADO");
        return result;
    }

    public long contar() {
        return clienteCatalogoRepository.count();
    }

    public Map<String, Object> diagnosticoDb() {
        Map<String, Object> info = new HashMap<>();
        try {
            String user = jdbcTemplate.queryForObject("select user from dual", String.class);
            String conName = jdbcTemplate.queryForObject("select sys_context('USERENV','CON_NAME') from dual", String.class);
            String currentSchema = jdbcTemplate.queryForObject("select sys_context('USERENV','CURRENT_SCHEMA') from dual", String.class);
            info.put("user", user);
            info.put("pdb", conName);
            info.put("currentSchema", currentSchema);
        } catch (Exception e) {
            info.put("user_error", e.getMessage());
        }
        try {
            Long countNative = jdbcTemplate.queryForObject("select count(*) from CLIENTES", Long.class);
            info.put("clientesCountNative", countNative);
        } catch (Exception e) {
            info.put("clientesCountError", e.getMessage());
        }
        try {
            List<Map<String, Object>> owners = jdbcTemplate.queryForList("select owner, table_name from all_tables where table_name = 'CLIENTES'");
            info.put("owners", owners);
        } catch (Exception e) {
            info.put("owners_error", e.getMessage());
        }
        try {
            List<Map<String, Object>> synonyms = jdbcTemplate.queryForList("select owner, synonym_name, table_owner, table_name from all_synonyms where synonym_name = 'CLIENTES'");
            info.put("synonyms", synonyms);
        } catch (Exception e) {
            info.put("synonyms_error", e.getMessage());
        }
        log.info("Diagnóstico DB: {}", info);
        return info;
    }

    public ClienteCatalogo guardar(ClienteCatalogo cliente) {
        log.info("Guardando cliente en CLIENTES RFC: {}", cliente != null ? cliente.getRfc() : "null");
        return clienteCatalogoRepository.save(cliente);
    }

    public Map<String, Object> clientesMuestra(int limit) {
        Map<String, Object> out = new HashMap<>();
        int n = Math.max(limit, 1);
        try {
            Long countNative = jdbcTemplate.queryForObject("select count(*) from CLIENTES", Long.class);
            out.put("count", countNative);
        } catch (Exception e) {
            out.put("count_error", e.getMessage());
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.query(
                    "select ID_CLIENTE, RFC, RAZON_SOCIAL from CLIENTES where ROWNUM <= ?",
                    ps -> ps.setInt(1, n),
                    (rs, rowNum) -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("idCliente", rs.getObject("ID_CLIENTE"));
                        m.put("rfc", rs.getString("RFC"));
                        m.put("razonSocial", rs.getString("RAZON_SOCIAL"));
                        return m;
                    }
            );
            out.put("rows", rows);
        } catch (Exception e) {
            out.put("rows_error", e.getMessage());
        }
        try {
            List<Map<String, Object>> owners = jdbcTemplate.queryForList("select owner, table_name from all_tables where table_name = 'CLIENTES'");
            out.put("owners", owners);
        } catch (Exception e) {
            out.put("owners_error", e.getMessage());
        }
        log.info("CLIENTES muestra: {}", out);
        return out;
    }
}