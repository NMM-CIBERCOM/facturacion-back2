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
        
        // Primero intentar búsqueda exacta normalizada
        Optional<ClienteCatalogo> result = clienteCatalogoRepository.findByRfcNormalized(normalized);
        if (result.isPresent()) {
            log.info("CLIENTES RFC {} encontrado con igualdad exacta normalizada", normalized);
            return result;
        }
        
        // Si no se encuentra, intentar ignorar prefijo
        log.info("CLIENTES RFC {} no encontrado con igualdad exacta; probando ignorar prefijo", normalized);
        result = clienteCatalogoRepository.findByRfcOrPrefixed(normalized);
        if (result.isPresent()) {
            log.info("CLIENTES RFC {} encontrado con búsqueda que ignora prefijo", normalized);
            return result;
        }
        
        // Si aún no se encuentra, intentar búsqueda parcial (solo si el RFC tiene al menos 3 caracteres)
        if (normalized.length() >= 3) {
            log.info("CLIENTES RFC {} no encontrado con búsqueda exacta; probando búsqueda parcial", normalized);
            List<ClienteCatalogo> resultadosParciales = buscarPorRFCParcial(normalized, 10);
            // Buscar coincidencia exacta en los resultados parciales
            for (ClienteCatalogo c : resultadosParciales) {
                if (c.getRfc() != null && c.getRfc().trim().toUpperCase().equals(normalized)) {
                    log.info("CLIENTES RFC {} encontrado en búsqueda parcial con coincidencia exacta", normalized);
                    return Optional.of(c);
                }
            }
            // Si hay un solo resultado parcial y es muy similar, usarlo
            if (resultadosParciales.size() == 1) {
                ClienteCatalogo unico = resultadosParciales.get(0);
                String rfcUnico = unico.getRfc() != null ? unico.getRfc().trim().toUpperCase() : "";
                // Verificar si el RFC buscado está contenido en el encontrado o viceversa
                if (rfcUnico.contains(normalized) || normalized.contains(rfcUnico)) {
                    log.info("CLIENTES RFC {} encontrado en búsqueda parcial con coincidencia parcial (único resultado)", normalized);
                    return Optional.of(unico);
                }
            }
        }
        
        log.info("Resultado CLIENTES RFC {}: NO_ENCONTRADO", normalized);
        return Optional.empty();
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

    /**
     * Busca clientes por RFC parcial (para autocompletado)
     * @param rfcParcial RFC parcial a buscar (mínimo 3 caracteres)
     * @param limit Límite de resultados (default 10)
     * @return Lista de clientes que coinciden con el RFC parcial
     */
    public List<ClienteCatalogo> buscarPorRFCParcial(String rfcParcial, int limit) {
        if (!StringUtils.hasText(rfcParcial) || rfcParcial.trim().length() < 3) {
            log.warn("Búsqueda parcial RFC: texto muy corto o vacío");
            return List.of();
        }
        String normalized = rfcParcial.trim().toUpperCase();
        int maxResults = Math.max(1, Math.min(limit, 50)); // Limitar entre 1 y 50
        
        try {
            List<ClienteCatalogo> resultados = jdbcTemplate.query(
                    "SELECT * FROM (SELECT * FROM CLIENTES WHERE UPPER(RFC) LIKE ? ORDER BY RFC) WHERE ROWNUM <= ?",
                    ps -> {
                        ps.setString(1, "%" + normalized + "%");
                        ps.setInt(2, maxResults);
                    },
                    (rs, rowNum) -> {
                        ClienteCatalogo c = new ClienteCatalogo();
                        c.setIdCliente(rs.getLong("ID_CLIENTE"));
                        c.setRfc(rs.getString("RFC"));
                        c.setRazonSocial(rs.getString("RAZON_SOCIAL"));
                        c.setNombre(rs.getString("NOMBRE"));
                        c.setPaterno(rs.getString("PATERNO"));
                        c.setMaterno(rs.getString("MATERNO"));
                        c.setCorreoElectronico(rs.getString("CORREO_ELECTRONICO"));
                        c.setDomicilioFiscal(rs.getString("DOMICILIO_FISCAL"));
                        c.setRegimenFiscal(rs.getString("REGIMEN_FISCAL"));
                        c.setPais(rs.getString("PAIS"));
                        c.setRegistroTributario(rs.getString("REGISTRO_TRIBUTARIO"));
                        c.setUsoCfdi(rs.getString("USO_CFDI"));
                        return c;
                    }
            );
            log.info("Búsqueda parcial RFC '{}': {} resultados", normalized, resultados.size());
            return resultados;
        } catch (Exception e) {
            log.error("Error en búsqueda parcial RFC", e);
            return List.of();
        }
    }
}