package com.cibercom.facturacion_back.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("oracle")
public class ITextPdfServiceConceptosFallbackTest {

    @Autowired
    private ITextPdfService pdfService;

    @Test
    void cuandoNoHayConceptos_usaTotalesParaConstruirConceptoUnico() throws Exception {
        Map<String, Object> facturaData = new HashMap<>();
        facturaData.put("nombreEmisor", "Empresa Ejemplo");
        facturaData.put("rfcEmisor", "EEM123456789");
        facturaData.put("nombreReceptor", "Cliente Ejemplo");
        facturaData.put("rfcReceptor", "XEXX010101000");
        facturaData.put("subtotal", "1200.00");
        facturaData.put("iva", "16.00");
        facturaData.put("total", "1216.00");

        byte[] pdf = pdfService.generarPdf(facturaData);
        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(500);

        // Los PDFs de iText suelen incluir el texto en claro; verificamos presencia
        String content = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(content).contains("CONCEPTOS");
        assertThat(content).contains("Subtotal: $1200.00");
        assertThat(content).contains("IVA: $16.00");
        // Al menos un importe con 1200.00 debe aparecer en tabla
        assertThat(content).contains("$1200.00");
    }
}