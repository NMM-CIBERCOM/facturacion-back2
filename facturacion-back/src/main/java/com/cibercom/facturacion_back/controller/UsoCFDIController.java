package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.model.UsoCFDI;
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
public class UsoCFDIController {

    @GetMapping("/usos-cfdi")
    public List<Object> getAllUsosCFDI() {
        return Arrays.stream(UsoCFDI.values())
                .map(u -> new Object() {
                    public final String clave = u.getClave();
                    public final String descripcion = u.getDescripcion();
                })
                .collect(Collectors.toList());
    }
}

