package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.FacturaFrontendRequest;
import com.cibercom.facturacion_back.dto.TicketDto;
import com.cibercom.facturacion_back.dto.TicketSearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("oracle")
public class FacturaServiceFrontendTest {

    @Autowired
    private FacturaService facturaService;

    @MockBean
    private TicketService ticketService;

    @Test
    void generarDesdeFrontend_UsaTotalesDelTicketEnXML() throws Exception {
        // Arrange: stub ticket values
        TicketDto ticket = new TicketDto();
        ticket.setSubtotal(new BigDecimal("650.00"));
        ticket.setIva(new BigDecimal("104.00"));
        ticket.setTotal(new BigDecimal("754.00"));
        ticket.setFormaPago("03");
        when(ticketService.buscarTickets(any(TicketSearchRequest.class)))
                .thenReturn(List.of(ticket));

        // Prepare request (use reflection to set fields because explicit setters may not exist)
        FacturaFrontendRequest req = new FacturaFrontendRequest();
        set(req, "rfc", "XEXX010101000");
        set(req, "correoElectronico", "test@example.com");
        set(req, "razonSocial", "Cliente prueba #2");
        set(req, "domicilioFiscal", "Calle Principal 123, Colonia Centro, CP 00000");
        set(req, "regimenFiscal", "601");
        set(req, "usoCfdi", "G03");
        set(req, "codigoFacturacion", "S001-2025-10-29-2-100246");
        set(req, "tienda", "S001");
        set(req, "fecha", LocalDate.of(2025, 10, 29));
        set(req, "terminal", "2");
        set(req, "boleta", "100246");
        set(req, "medioPago", "PUE");
        set(req, "formaPago", "03");
        set(req, "iepsDesglosado", Boolean.FALSE);
        set(req, "guardarEnMongo", Boolean.TRUE);

        // Act
        Map<String, Object> result = facturaService.procesarFormularioFrontend(req);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("exitoso")).isEqualTo(Boolean.TRUE);
        String xml = (String) result.get("xmlGenerado");
        assertThat(xml).contains("SubTotal=\"650.00\"");
        assertThat(xml).contains("Total=\"754.00\"");
        assertThat(xml).contains("TotalImpuestosTrasladados=\"104.00\"");
        assertThat(xml).contains("FormaPago=\"03\"");
        assertThat(xml).contains("LugarExpedicion=\"00000\"");
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}