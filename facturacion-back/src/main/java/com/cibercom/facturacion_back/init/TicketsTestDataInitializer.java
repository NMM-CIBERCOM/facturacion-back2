package com.cibercom.facturacion_back.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("oracle")
public class TicketsTestDataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TicketsTestDataInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${tickets.test.insert.enabled:false}")
    private boolean enabled;

    @Value("${tickets.test.codigoTienda:S001}")
    private String codigoTienda;

    @Value("${tickets.test.fecha:2025-10-28}")
    private String fecha; // YYYY-MM-DD

    @Value("${tickets.test.folio:100245}")
    private Integer folio;

    @Value("${tickets.test.tiendaId:1}")
    private Integer tiendaId;

    @Value("${tickets.test.terminalId:1}")
    private Integer terminalId;

    // Segundo ticket de prueba (misma tienda, diferentes datos)
    @Value("${tickets.test2.enabled:false}")
    private boolean enabled2;

    @Value("${tickets.test2.codigoTienda:S001}")
    private String codigoTienda2;

    @Value("${tickets.test2.fecha:2025-10-29}")
    private String fecha2; // YYYY-MM-DD

    @Value("${tickets.test2.folio:100246}")
    private Integer folio2;

    @Value("${tickets.test2.tiendaId:1}")
    private Integer tiendaId2;

    @Value("${tickets.test2.terminalId:2}")
    private Integer terminalId2;

    public TicketsTestDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled && !enabled2) {
            logger.info("TicketsTestDataInitializer deshabilitado (ningún insert habilitado)");
            return;
        }

        if (enabled) {
            logger.info("[1/2] Verificando/inserta ticket de prueba #1...");
            Integer count1 = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM TICKETS WHERE CODIGO_TIENDA = ? AND TRUNC(FECHA) = TO_DATE(?, 'YYYY-MM-DD') AND FOLIO = ?",
                    Integer.class,
                    codigoTienda,
                    fecha,
                    folio
            );

            if (count1 != null && count1 > 0) {
                logger.info("Ticket de prueba #1 ya existe para tienda={}, fecha={}, folio={}", codigoTienda, fecha, folio);
            } else {
                logger.info("Insertando ticket de prueba #1 en TICKETS (sin ID_TICKET explícito; identidad generada por Oracle)...");

                BigDecimal subtotal = new BigDecimal("800.00");
                BigDecimal iva = new BigDecimal("128.00");
                BigDecimal total = new BigDecimal("928.00");
                String formaPago = "01"; // Efectivo
                String rfcCliente = "XAXX010101000";
                String nombreCliente = "Público en general";
                Integer status = 1;

                int rows = jdbcTemplate.update(
                        "INSERT INTO TICKETS (CODIGO_TIENDA, TIENDA_ID, TERMINAL_ID, FECHA, FOLIO, SUBTOTAL, IVA, TOTAL, FORMA_PAGO, RFC_CLIENTE, NOMBRE_CLIENTE, STATUS, ID_FACTURA) " +
                                "VALUES (?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        codigoTienda,
                        tiendaId,
                        terminalId,
                        fecha,
                        folio,
                        subtotal,
                        iva,
                        total,
                        formaPago,
                        rfcCliente,
                        nombreCliente,
                        status,
                        null
                );

                if (rows == 1) {
                    logger.info("Ticket de prueba #1 insertado correctamente: CODIGO_TIENDA={}, FECHA={}, FOLIO={}", codigoTienda, fecha, folio);
                } else {
                    logger.warn("No se insertó el ticket de prueba #1. Filas afectadas={}", rows);
                }
            }
        }

        if (enabled2) {
            logger.info("[2/2] Verificando/inserta ticket de prueba #2...");
            Integer count2 = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM TICKETS WHERE CODIGO_TIENDA = ? AND TRUNC(FECHA) = TO_DATE(?, 'YYYY-MM-DD') AND FOLIO = ?",
                    Integer.class,
                    codigoTienda2,
                    fecha2,
                    folio2
            );

            if (count2 != null && count2 > 0) {
                logger.info("Ticket de prueba #2 ya existe para tienda={}, fecha={}, folio={}", codigoTienda2, fecha2, folio2);
            } else {
                logger.info("Insertando ticket de prueba #2 en TICKETS (sin ID_TICKET explícito; identidad generada por Oracle)...");

                BigDecimal subtotal = new BigDecimal("650.00");
                BigDecimal iva = new BigDecimal("104.00");
                BigDecimal total = new BigDecimal("754.00");
                String formaPago = "03"; // Transferencia
                String rfcCliente = "XEXX010101000";
                String nombreCliente = "Cliente prueba #2";
                Integer status = 1;

                int rows = jdbcTemplate.update(
                        "INSERT INTO TICKETS (CODIGO_TIENDA, TIENDA_ID, TERMINAL_ID, FECHA, FOLIO, SUBTOTAL, IVA, TOTAL, FORMA_PAGO, RFC_CLIENTE, NOMBRE_CLIENTE, STATUS, ID_FACTURA) " +
                                "VALUES (?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        codigoTienda2,
                        tiendaId2,
                        terminalId2,
                        fecha2,
                        folio2,
                        subtotal,
                        iva,
                        total,
                        formaPago,
                        rfcCliente,
                        nombreCliente,
                        status,
                        null
                );

                if (rows == 1) {
                    logger.info("Ticket de prueba #2 insertado correctamente: CODIGO_TIENDA={}, FECHA={}, FOLIO={}", codigoTienda2, fecha2, folio2);
                } else {
                    logger.warn("No se insertó el ticket de prueba #2. Filas afectadas={}", rows);
                }
            }
        }
    }
}