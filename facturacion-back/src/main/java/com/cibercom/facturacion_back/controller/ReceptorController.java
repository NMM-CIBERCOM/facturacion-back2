package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.BuscarReceptorRequest;
import com.cibercom.facturacion_back.dto.BuscarReceptorResponse;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/receptor")
@CrossOrigin(origins = "*")
public class ReceptorController {

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired(required = false)
    private FacturaMongoRepository facturaMongoRepository;

    @Autowired
    private Environment environment;

    @PostMapping("/buscar")
    public ResponseEntity<BuscarReceptorResponse> buscarReceptor(@RequestBody BuscarReceptorRequest request) {
        String rfc = request != null ? request.getRfc() : null;
        if (!StringUtils.hasText(rfc)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String[] profiles = environment.getActiveProfiles();
        boolean useMongo = Arrays.stream(profiles).anyMatch(p -> p.equalsIgnoreCase("mongo"));

        BuscarReceptorResponse.Receptor receptor = null;
        boolean encontrado = false;

        if (useMongo && facturaMongoRepository != null) {
            List<FacturaMongo> facturas = facturaMongoRepository.findByReceptorRfc(rfc);
            if (facturas != null && !facturas.isEmpty()) {
                FacturaMongo latest = facturas.stream()
                        .sorted(Comparator.comparing(FacturaMongo::getFechaGeneracion, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .findFirst().orElse(facturas.get(0));
                receptor = buildReceptorFromMongo(latest);
                encontrado = true;
            }
        } else {
            List<Factura> facturas = facturaRepository.findByReceptorRfc(rfc);
            if (facturas != null && !facturas.isEmpty()) {
                Factura latest = facturas.stream()
                        .sorted(Comparator.comparing(Factura::getFechaGeneracion, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .findFirst().orElse(facturas.get(0));
                receptor = buildReceptorFromOracle(latest);
                encontrado = true;
            }
        }

        if (!encontrado || receptor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<String> faltantes = validarFaltantes(receptor, request.getCorreoElectronico());
        boolean completoCFDI40 = faltantes.isEmpty();

        BuscarReceptorResponse response = BuscarReceptorResponse.builder()
                .encontrado(true)
                .completoCFDI40(completoCFDI40)
                .idReceptor(rfc)
                .receptor(receptor)
                .faltantes(faltantes)
                .build();

        return ResponseEntity.ok(response);
    }

    private BuscarReceptorResponse.Receptor buildReceptorFromOracle(Factura f) {
        return BuscarReceptorResponse.Receptor.builder()
                .rfc(safe(f.getReceptorRfc()))
                .razonSocial(safe(f.getReceptorRazonSocial()))
                .nombre(safe(f.getReceptorNombre()))
                .paterno(safe(f.getReceptorPaterno()))
                .materno(safe(f.getReceptorMaterno()))
                .pais(safe(f.getReceptorPais()))
                .domicilioFiscal(safe(f.getReceptorDomicilioFiscal()))
                .regimenFiscal(safe(f.getReceptorRegimenFiscal()))
                .usoCfdi(safe(f.getReceptorUsoCfdi()))
                .build();
    }

    private BuscarReceptorResponse.Receptor buildReceptorFromMongo(FacturaMongo fm) {
        Map<String, Object> r = fm.getReceptor();
        String rfc = fm.getReceptorRfc();
        return BuscarReceptorResponse.Receptor.builder()
                .rfc(safe(rfc))
                .razonSocial(safe(asString(r.get("razonSocial"))))
                .nombre(safe(asString(r.get("nombre"))))
                .paterno(safe(asString(r.get("paterno"))))
                .materno(safe(asString(r.get("materno"))))
                .pais(safe(asString(r.get("pais"))))
                .domicilioFiscal(safe(asString(r.get("domicilioFiscal"))))
                .regimenFiscal(safe(asString(r.get("regimenFiscal"))))
                .usoCfdi(safe(asString(r.get("usoCfdi"))))
                .build();
    }

    private List<String> validarFaltantes(BuscarReceptorResponse.Receptor r, String correoElectronico) {
        List<String> faltantes = new ArrayList<>();
        if (!hasText(r.getRazonSocial()) && !hasText(r.getNombre())) {
            // Si no hay razón social ni nombre, pedimos razón social por defecto
            faltantes.add("razonSocial");
        }
        if (!hasText(r.getDomicilioFiscal())) {
            faltantes.add("domicilioFiscal");
        }
        if (!hasText(r.getRegimenFiscal())) {
            faltantes.add("regimenFiscal");
        }
        if (!hasText(r.getUsoCfdi())) {
            faltantes.add("usoCfdi");
        }
        if (!hasText(correoElectronico)) {
            faltantes.add("correoElectronico");
        }
        return faltantes;
    }

    private String safe(String s) {
        return s == null ? null : s.trim();
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}