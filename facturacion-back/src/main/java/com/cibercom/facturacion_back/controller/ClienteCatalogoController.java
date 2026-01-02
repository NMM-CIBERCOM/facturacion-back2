package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.ClienteCatalogoResponse;
import com.cibercom.facturacion_back.model.ClienteCatalogo;
import com.cibercom.facturacion_back.service.ClienteCatalogoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/catalogo-clientes")
@CrossOrigin(origins = "*")
public class ClienteCatalogoController {

    @Autowired
    private ClienteCatalogoService clienteCatalogoService;

    private static final Logger log = LoggerFactory.getLogger(ClienteCatalogoController.class);

    @GetMapping("/{rfc}")
    public ResponseEntity<ClienteCatalogoResponse> obtenerPorRfc(@PathVariable("rfc") String rfc) {
        log.info("Solicitud GET catálogo clientes por RFC: {}", rfc);
        if (!StringUtils.hasText(rfc)) {
            log.warn("RFC vacío en solicitud GET catálogo clientes");
            return ResponseEntity.ok(ClienteCatalogoResponse.notFound());
        }
        String normalized = rfc.trim().toUpperCase();
        return clienteCatalogoService.buscarPorRfc(normalized)
                .map(c -> {
                    ClienteCatalogoResponse resp = toResponse(c);
                    log.info("CLIENTES: RFC {} encontrado, razón social: {}", normalized, resp.getCliente() != null ? resp.getCliente().getRazonSocial() : "N/A");
                    return ResponseEntity.ok(resp);
                })
                .orElseGet(() -> {
                    log.info("CLIENTES: RFC {} no encontrado", normalized);
                    return ResponseEntity.ok(ClienteCatalogoResponse.notFound());
                });
    }

    @GetMapping("/_debug/count")
    public ResponseEntity<Long> contarClientes() {
        long count = clienteCatalogoService.contar();
        log.info("CLIENTES: count= {}", count);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/_debug/db")
    public ResponseEntity<Map<String, Object>> diagnosticoDb() {
        Map<String, Object> info = clienteCatalogoService.diagnosticoDb();
        return ResponseEntity.ok(info);
    }

    @GetMapping("/_debug/clientes")
    public ResponseEntity<Map<String, Object>> clientesMuestra(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        Map<String, Object> info = clienteCatalogoService.clientesMuestra(limit);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/buscar")
    public ResponseEntity<Map<String, Object>> buscarPorRFCParcial(
            @RequestParam("rfc") String rfc,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        log.info("Búsqueda parcial RFC: {}", rfc);
        Map<String, Object> response = new HashMap<>();
        try {
            List<ClienteCatalogo> clientes = clienteCatalogoService.buscarPorRFCParcial(rfc, limit);
            List<Map<String, Object>> clientesDto = clientes.stream().map(c -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("rfc", c.getRfc());
                dto.put("razonSocial", c.getRazonSocial());
                dto.put("nombre", c.getNombre());
                dto.put("paterno", c.getPaterno());
                dto.put("materno", c.getMaterno());
                return dto;
            }).toList();
            response.put("clientes", clientesDto);
            response.put("total", clientesDto.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error en búsqueda parcial RFC", e);
            response.put("error", e.getMessage());
            response.put("clientes", List.of());
            response.put("total", 0);
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearCliente(@RequestBody Map<String, Object> body) {
        log.info("Creando nuevo cliente");
        Map<String, Object> out = new HashMap<>();
        try {
            ClienteCatalogo c = new ClienteCatalogo();
            String rfc = body != null && body.get("rfc") != null ? body.get("rfc").toString().trim().toUpperCase() : null;
            if (rfc == null || rfc.isEmpty()) {
                out.put("error", "RFC es obligatorio");
                return ResponseEntity.badRequest().body(out);
            }
            
            String razon = body != null && body.get("razon_social") != null ? body.get("razon_social").toString() : null;
            if (razon == null || razon.trim().isEmpty()) {
                out.put("error", "Razón Social es obligatoria");
                return ResponseEntity.badRequest().body(out);
            }
            
            // Construir domicilio fiscal a partir de campos individuales
            StringBuilder domicilioBuilder = new StringBuilder();
            if (body.get("calle") != null) domicilioBuilder.append(body.get("calle").toString().trim());
            if (body.get("numero_exterior") != null) {
                if (domicilioBuilder.length() > 0) domicilioBuilder.append(" ");
                domicilioBuilder.append(body.get("numero_exterior").toString().trim());
            }
            if (body.get("numero_interior") != null) {
                if (domicilioBuilder.length() > 0) domicilioBuilder.append(" ");
                domicilioBuilder.append("Int. ").append(body.get("numero_interior").toString().trim());
            }
            if (body.get("colonia") != null) {
                if (domicilioBuilder.length() > 0) domicilioBuilder.append(", ");
                domicilioBuilder.append(body.get("colonia").toString().trim());
            }
            if (body.get("municipio") != null) {
                if (domicilioBuilder.length() > 0) domicilioBuilder.append(", ");
                domicilioBuilder.append(body.get("municipio").toString().trim());
            }
            if (body.get("estado") != null) {
                if (domicilioBuilder.length() > 0) domicilioBuilder.append(", ");
                domicilioBuilder.append(body.get("estado").toString().trim());
            }
            if (body.get("codigo_postal") != null) {
                if (domicilioBuilder.length() > 0) domicilioBuilder.append(", C.P. ");
                domicilioBuilder.append(body.get("codigo_postal").toString().trim());
            }
            if (body.get("pais") != null) {
                if (domicilioBuilder.length() > 0) domicilioBuilder.append(", ");
                domicilioBuilder.append(body.get("pais").toString().trim());
            }
            
            String domicilioFiscal = domicilioBuilder.length() > 0 
                ? domicilioBuilder.toString() 
                : (body.get("domicilio_fiscal") != null ? body.get("domicilio_fiscal").toString() : null);
            
            c.setRfc(rfc);
            c.setRazonSocial(razon);
            c.setNombre(body != null && body.get("nombre") != null ? body.get("nombre").toString() : null);
            c.setPaterno(body != null && body.get("paterno") != null ? body.get("paterno").toString() : null);
            c.setMaterno(body != null && body.get("materno") != null ? body.get("materno").toString() : null);
            c.setCorreoElectronico(body != null && body.get("correo_electronico") != null ? body.get("correo_electronico").toString() : null);
            c.setDomicilioFiscal(domicilioFiscal);
            c.setRegimenFiscal(body != null && body.get("regimen_fiscal") != null ? body.get("regimen_fiscal").toString() : null);
            c.setPais(body != null && body.get("pais") != null ? body.get("pais").toString() : null);
            c.setRegistroTributario(body != null && body.get("registro_tributario") != null ? body.get("registro_tributario").toString() : null);
            c.setUsoCfdi(body != null && body.get("uso_cfdi") != null ? body.get("uso_cfdi").toString() : null);
            c.setFechaAlta(LocalDateTime.now());
            
            ClienteCatalogo saved = clienteCatalogoService.guardar(c);
            out.put("idCliente", saved.getIdCliente());
            out.put("rfc", saved.getRfc());
            out.put("razonSocial", saved.getRazonSocial());
            out.put("mensaje", "Cliente guardado exitosamente");
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            log.error("Error al crear cliente", e);
            out.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(out);
        }
    }

    @PostMapping("/_debug/insert")
    public ResponseEntity<Map<String, Object>> insertarClienteDebug(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> out = new HashMap<>();
        try {
            ClienteCatalogo c = new ClienteCatalogo();
            String rfc = body != null && body.get("rfc") != null ? body.get("rfc").toString().trim().toUpperCase() : "EEJ920629TE3";
            String razon = body != null && body.get("razon_social") != null ? body.get("razon_social").toString() : "Empresa Ejemplo S.A. de C.V.";
            c.setRfc(rfc);
            c.setRazonSocial(razon);
            c.setNombre(body != null && body.get("nombre") != null ? body.get("nombre").toString() : null);
            c.setPaterno(body != null && body.get("paterno") != null ? body.get("paterno").toString() : null);
            c.setMaterno(body != null && body.get("materno") != null ? body.get("materno").toString() : null);
            c.setCorreoElectronico(body != null && body.get("correo_electronico") != null ? body.get("correo_electronico").toString() : null);
            c.setDomicilioFiscal(body != null && body.get("domicilio_fiscal") != null ? body.get("domicilio_fiscal").toString() : null);
            c.setRegimenFiscal(body != null && body.get("regimen_fiscal") != null ? body.get("regimen_fiscal").toString() : null);
            c.setPais(body != null && body.get("pais") != null ? body.get("pais").toString() : null);
            c.setRegistroTributario(body != null && body.get("registro_tributario") != null ? body.get("registro_tributario").toString() : null);
            c.setUsoCfdi(body != null && body.get("uso_cfdi") != null ? body.get("uso_cfdi").toString() : null);
            c.setFechaAlta(LocalDateTime.now());
            ClienteCatalogo saved = clienteCatalogoService.guardar(c);
            out.put("idCliente", saved.getIdCliente());
            out.put("rfc", saved.getRfc());
            out.put("razonSocial", saved.getRazonSocial());
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            out.put("error", e.getMessage());
            return ResponseEntity.ok(out);
        }
    }

    private ClienteCatalogoResponse toResponse(ClienteCatalogo c) {
        ClienteCatalogoResponse.Cliente dto = new ClienteCatalogoResponse.Cliente();
        dto.setRfc(safe(c.getRfc()));
        dto.setRazonSocial(safe(c.getRazonSocial()));
        dto.setNombre(safe(c.getNombre()));
        dto.setPaterno(safe(c.getPaterno()));
        dto.setMaterno(safe(c.getMaterno()));
        dto.setCorreoElectronico(safe(c.getCorreoElectronico()));
        dto.setPais(safe(c.getPais()));
        dto.setDomicilioFiscal(safe(c.getDomicilioFiscal()));
        dto.setRegimenFiscal(safe(c.getRegimenFiscal()));
        dto.setRegistroTributario(safe(c.getRegistroTributario()));
        dto.setUsoCfdi(safe(c.getUsoCfdi()));
        return ClienteCatalogoResponse.ok(dto);
    }

    private String safe(String s) { return s == null ? null : s.trim(); }
}