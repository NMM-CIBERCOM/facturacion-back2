package com.cibercom.facturacion_back.controller;

// import com.cibercom.facturacion_back.dto.RegimenFiscalDTO;
import com.cibercom.facturacion_back.model.RegimenFiscal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class RegimenFiscalController {

    @GetMapping("/regimenes-fiscales")
    public List<String> getAllRegimenesFiscales() {
        return Arrays.stream(RegimenFiscal.values())
                .map(r -> r.getClave() + " - " + r.getDescripcion())
                .collect(Collectors.toList());
    }
}

