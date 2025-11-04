package com.cibercom.facturacion_back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CatalogosController {

    @GetMapping("/regimenes-fiscales")
    public ResponseEntity<List<String>> obtenerRegimenesFiscales() {
        // Lista basada en catálogos SAT comunes. Se puede ampliar/parametrizar.
        List<String> regimenes = List.of(
                "601 - General de Ley Personas Morales",
                "603 - Personas Morales con Fines no Lucrativos",
                "605 - Sueldos y Salarios e Ingresos Asimilados a Salarios",
                "606 - Arrendamiento",
                "607 - Régimen de Enajenación de Bienes",
                "608 - Demás Ingresos",
                "609 - Consolidación",
                "610 - Residentes en el Extranjero sin Establecimiento Permanente en México",
                "611 - Ingresos por Dividendos (socios y accionistas)",
                "612 - Personas Físicas con Actividades Empresariales y Profesionales",
                "614 - Régimen Simplificado de Confianza",
                "615 - Régimen de los Ingresos por Obtención de Premios",
                "616 - Sin Obligaciones Fiscales",
                "620 - Sociedades Cooperativas de Producción que optan por diferir el ISR",
                "621 - Incorporación Fiscal",
                "622 - Actividades Agrícolas, Ganaderas, Silvícolas y Pesqueras",
                "623 - Opcional para Grupos de Sociedades",
                "624 - Coordinados",
                "625 - Régimen de las Actividades Empresariales con Ingresos a través de Plataformas Tecnológicas",
                "626 - Régimen Simplificado de Confianza (Personas Morales)"
        );
        return ResponseEntity.ok(regimenes);
    }
}