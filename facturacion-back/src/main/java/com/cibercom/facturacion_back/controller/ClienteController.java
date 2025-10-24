package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.ClienteResponse;
import com.cibercom.facturacion_back.model.Factura;
import com.cibercom.facturacion_back.model.FacturaMongo;
import com.cibercom.facturacion_back.repository.FacturaMongoRepository;
import com.cibercom.facturacion_back.repository.FacturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.Comparator;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired(required = false)
    private FacturaMongoRepository facturaMongoRepository;

    @Autowired
    private Environment environment;

    @GetMapping("/{rfc}")
    public ResponseEntity<ClienteResponse> obtenerClientePorRfc(@PathVariable("rfc") String rfc) {
        if (!StringUtils.hasText(rfc)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String[] profiles = environment.getActiveProfiles();
        boolean useMongo = Arrays.stream(profiles).anyMatch(p -> p.equalsIgnoreCase("mongo"));

        ClienteResponse.Cliente cliente = null;

        if (useMongo && facturaMongoRepository != null) {
            List<FacturaMongo> facturas = facturaMongoRepository.findByReceptorRfc(rfc);
            if (facturas != null && !facturas.isEmpty()) {
                FacturaMongo latest = facturas.stream()
                        .sorted(Comparator.comparing(FacturaMongo::getFechaGeneracion, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .findFirst().orElse(facturas.get(0));
                cliente = fromMongo(latest);
            }
        } else {
            List<Factura> facturas = facturaRepository.findByReceptorRfc(rfc);
            if (facturas != null && !facturas.isEmpty()) {
                Factura latest = facturas.stream()
                        .sorted(Comparator.comparing(Factura::getFechaGeneracion, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .findFirst().orElse(facturas.get(0));
                cliente = fromOracle(latest);
            }
        }

        if (cliente == null) {
            // No encontrado: devolver 200 con payload estándar para que el frontend no reciba 404
            return ResponseEntity.ok(notFound());
        }

        ClienteResponse resp = new ClienteResponse();
        resp.setEncontrado(true);
        resp.setCliente(cliente);
        return ResponseEntity.ok(resp);
    }

    private ClienteResponse notFound() {
        ClienteResponse r = new ClienteResponse();
        r.setEncontrado(false);
        r.setCliente(null);
        return r;
    }

    private ClienteResponse.Cliente fromOracle(Factura f) {
        ClienteResponse.Cliente c = new ClienteResponse.Cliente();
        c.setRfc(safe(f.getReceptorRfc()));
        c.setRazonSocial(safe(f.getReceptorRazonSocial()));
        c.setNombre(safe(f.getReceptorNombre()));
        c.setPaterno(safe(f.getReceptorPaterno()));
        c.setMaterno(safe(f.getReceptorMaterno()));
        c.setPais(safe(f.getReceptorPais()));
        c.setDomicilioFiscal(safe(f.getReceptorDomicilioFiscal()));
        c.setRegimenFiscal(safe(f.getReceptorRegimenFiscal()));
        c.setUsoCfdi(safe(f.getReceptorUsoCfdi()));
        return c;
    }

    private ClienteResponse.Cliente fromMongo(FacturaMongo fm) {
        Map<String, Object> r = fm.getReceptor();
        String rfc = fm.getReceptorRfc();
        ClienteResponse.Cliente c = new ClienteResponse.Cliente();
        c.setRfc(safe(rfc));
        c.setRazonSocial(safe(asString(r.get("razonSocial"))));
        c.setNombre(safe(asString(r.get("nombre"))));
        c.setPaterno(safe(asString(r.get("paterno"))));
        c.setMaterno(safe(asString(r.get("materno"))));
        c.setPais(safe(asString(r.get("pais"))));
        c.setDomicilioFiscal(safe(asString(r.get("domicilioFiscal"))));
        c.setRegimenFiscal(safe(asString(r.get("regimenFiscal"))));
        c.setUsoCfdi(safe(asString(r.get("usoCfdi"))));
        return c;
    }

    private String safe(String s) { return s == null ? null : s.trim(); }
    private String asString(Object o) { return o == null ? null : String.valueOf(o); }
}