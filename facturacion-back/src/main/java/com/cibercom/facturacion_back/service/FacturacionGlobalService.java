package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.FacturaGlobalPreviewRequest;
import com.cibercom.facturacion_back.dto.FacturaGlobalPreviewResponse;
import com.cibercom.facturacion_back.dto.FacturaGlobalConsultaRequest;
import com.cibercom.facturacion_back.dto.FacturaGlobalConsultaResponse;
import com.cibercom.facturacion_back.dto.FacturaGlobalRegistro;
import com.cibercom.facturacion_back.dto.FacturaGlobalFacturaDTO;
import com.cibercom.facturacion_back.dto.FacturaGlobalTicketDTO;
import com.cibercom.facturacion_back.dto.FacturaGlobalGuardarRequest;
import com.cibercom.facturacion_back.dto.FacturaGlobalGuardarResponse;
import com.cibercom.facturacion_back.dto.ConsultaFacturaRequest;
import com.cibercom.facturacion_back.dto.ConsultaFacturaResponse.FacturaConsultaDTO;
import com.cibercom.facturacion_back.dto.TicketDto;
import com.cibercom.facturacion_back.dto.TicketDetalleDto;
import com.cibercom.facturacion_back.dto.TicketSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Service
public class FacturacionGlobalService {
    private static final Logger logger = LoggerFactory.getLogger(FacturacionGlobalService.class);

    @Autowired(required = false)
    private TicketService ticketService;

    @Autowired(required = false)
    private TicketDetalleService ticketDetalleService;

    @Autowired(required = false)
    private com.cibercom.facturacion_back.dao.ConsultaFacturaDAO consultaFacturaDAO;

    @Autowired(required = false)
    private com.cibercom.facturacion_back.dao.CartaPorteConsultaDAO cartaPorteConsultaDAO;

    @Autowired(required = false)
    private com.cibercom.facturacion_back.dao.ConceptoOracleDAO conceptoOracleDAO;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /**
     * Calcula preview agregando totales de TICKETS y TICKETS_DETALLE por fecha/tienda.
     */
    public FacturaGlobalPreviewResponse preview(FacturaGlobalPreviewRequest req) {
        if (ticketService == null || ticketDetalleService == null) {
            throw new IllegalStateException("Servicios de tickets no disponibles (perfil Oracle no activo)");
        }

        TicketSearchRequest filtros = new TicketSearchRequest();
        filtros.setCodigoTienda(req.getCodigoTienda());
        filtros.setFecha(req.getFecha());
        filtros.setTerminalId(req.getTerminalId());

        List<TicketDto> tickets = ticketService.buscarTickets(filtros);
        List<TicketDto> elegibles = new ArrayList<>();
        for (TicketDto t : tickets) {
            // Excluir ya facturados si se solicita
            if (Boolean.TRUE.equals(req.getExcluirFacturados()) && t.getIdFactura() != null) {
                continue;
            }
            // Opcional: filtrar cancelados si STATUS == 0 (convención común); si no hay STATUS, incluir
            if (t.getStatus() != null && t.getStatus() == 0) {
                continue;
            }
            elegibles.add(t);
        }

        Map<BigDecimal, MontoAcumulado> porTasa = new HashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (TicketDto t : elegibles) {
            // Sumar totales de cabecera si vienen informados
            if (t.getSubtotal() != null) subtotal = subtotal.add(t.getSubtotal());
            if (t.getIva() != null) iva = iva.add(t.getIva());
            if (t.getTotal() != null) total = total.add(t.getTotal());

            // Desglose por tasa a partir del detalle
            try {
                if (t.getIdTicket() != null) {
                    List<TicketDetalleDto> detalles = ticketDetalleService.buscarDetallesPorIdTicket(t.getIdTicket());
                    for (TicketDetalleDto d : detalles) {
                        BigDecimal tasa = normalizarTasa(d.getIvaPorcentaje());
                        MontoAcumulado acc = porTasa.computeIfAbsent(tasa, k -> new MontoAcumulado());
                        BigDecimal base = safeBD(d.getSubtotal());
                        BigDecimal ivaImp = safeBD(d.getIvaImporte());
                        BigDecimal tot = safeBD(d.getTotal());
                        // Fallback si faltan columnas
                        if (base == null && tot != null && ivaImp != null) {
                            base = tot.subtract(ivaImp);
                        }
                        acc.base = acc.base.add(base != null ? base : BigDecimal.ZERO);
                        acc.iva = acc.iva.add(ivaImp != null ? ivaImp : BigDecimal.ZERO);
                        acc.total = acc.total.add(tot != null ? tot : (base != null ? base : BigDecimal.ZERO));
                    }
                }
            } catch (Exception e) {
                logger.warn("Error acumulando detalle para ticket {}: {}", t.getIdTicket(), e.getMessage());
            }
        }

        FacturaGlobalPreviewResponse resp = new FacturaGlobalPreviewResponse();
        resp.setFecha(req.getFecha());
        resp.setCodigoTienda(req.getCodigoTienda());
        resp.setTicketsSeleccionados(elegibles.size());
        resp.setSubtotal(subtotal);
        resp.setIva(iva);
        resp.setTotal(total);

        List<FacturaGlobalPreviewResponse.MontoPorTasa> lista = new ArrayList<>();
        // Ordenar tasas ascendente para consistencia visual
        List<BigDecimal> tasasOrdenadas = new ArrayList<>(porTasa.keySet());
        tasasOrdenadas.sort(Comparator.nullsLast(Comparator.naturalOrder()));
        for (BigDecimal tasa : tasasOrdenadas) {
            MontoAcumulado acc = porTasa.get(tasa);
            FacturaGlobalPreviewResponse.MontoPorTasa m = new FacturaGlobalPreviewResponse.MontoPorTasa();
            m.setTasa(tasa != null ? tasa : BigDecimal.ZERO);
            m.setBase(acc.base);
            m.setIva(acc.iva);
            m.setTotal(acc.total);
            lista.add(m);
        }
        resp.setPorTasa(lista);
        return resp;
    }

    /**
     * Consulta combinada de FACTURAS, TICKETS y CARTA_PORTE por periodo (DIA/SEMANA/MES).
     * Permite fecha nula usando la fecha actual como ancla.
     */
    public FacturaGlobalConsultaResponse consulta(FacturaGlobalConsultaRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Parámetros inválidos: request nulo");
        }

        LocalDate fechaAncla = (req.getFecha() != null) ? req.getFecha() : LocalDate.now();

        LocalDate inicio;
        LocalDate fin;
        if ("SEMANA".equalsIgnoreCase(req.getPeriodo())) {
            // Semana natural: Lunes a Domingo de la semana que contiene la fecha ancla
            inicio = fechaAncla.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            fin = fechaAncla.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        } else if ("MES".equalsIgnoreCase(req.getPeriodo())) {
            // Mes natural: primer y último día del mes de la fecha ancla
            inicio = fechaAncla.with(TemporalAdjusters.firstDayOfMonth());
            fin = fechaAncla.with(TemporalAdjusters.lastDayOfMonth());
        } else {
            inicio = fechaAncla;
            fin = fechaAncla;
        }

        List<FacturaGlobalRegistro> registros = new ArrayList<>();
        int cntFact = 0, cntTick = 0, cntCp = 0;

        // 1) FACTURAS via ConsultaFacturaDAO
        if (consultaFacturaDAO != null) {
            try {
                ConsultaFacturaRequest cfReq = new ConsultaFacturaRequest();
                cfReq.setFechaInicio(inicio);
                cfReq.setFechaFin(fin);
                cfReq.setTienda(req.getCodigoTienda());
                List<FacturaConsultaDTO> facturas = consultaFacturaDAO.buscarFacturas(cfReq);

                // Construir registros planos y estructura agregada por factura
                List<FacturaGlobalFacturaDTO> agregadas = new ArrayList<>();
                for (FacturaConsultaDTO f : facturas) {
                    FacturaGlobalRegistro r = new FacturaGlobalRegistro();
                    r.setTipo("FACTURA");
                    r.setTienda(f.getTienda());
                    r.setFecha(f.getFechaEmision());
                    String folio = (f.getSerie() != null ? f.getSerie() : "") +
                            (f.getFolio() != null ? (f.getSerie() != null ? "-" : "") + f.getFolio() : "");
                    r.setFolio(folio);
                    r.setUuid(f.getUuid());
                    r.setTotal(f.getImporte());
                    r.setEstado(f.getEstatusFacturacion());
                    registros.add(r);

                    // Agregada: factura con sus tickets y detalles
                    FacturaGlobalFacturaDTO agg = new FacturaGlobalFacturaDTO();
                    agg.setUuid(f.getUuid());
                    agg.setSerie(f.getSerie());
                    agg.setFolio(f.getFolio());
                    agg.setFechaEmision(f.getFechaEmision());
                    agg.setImporte(f.getImporte());
                    agg.setEstatusFacturacion(f.getEstatusFacturacion());
                    agg.setEstatusSat(f.getEstatusSat());
                    agg.setTienda(f.getTienda());

                    List<FacturaGlobalTicketDTO> ticketsAgg = new ArrayList<>();
                    try {
                        if (conceptoOracleDAO != null && ticketService != null && ticketDetalleService != null) {
                            java.util.Optional<Long> idOpt = conceptoOracleDAO.obtenerIdFacturaPorUuid(f.getUuid());
                            if (idOpt.isPresent() && idOpt.get() != null) {
                                Long idFactura = idOpt.get();
                                List<TicketDto> vinculados = ticketService.buscarTicketsPorIdFactura(idFactura);
                                for (TicketDto t : vinculados) {
                                    FacturaGlobalTicketDTO tAgg = new FacturaGlobalTicketDTO();
                                    tAgg.setIdTicket(t.getIdTicket());
                                    tAgg.setFecha(t.getFecha());
                                    tAgg.setFolio(t.getFolio());
                                    tAgg.setTotal(t.getTotal());
                                    tAgg.setFormaPago(t.getFormaPago());
                                    try {
                                        List<TicketDetalleDto> dets = ticketDetalleService.buscarDetallesPorIdTicket(t.getIdTicket());
                                        tAgg.setDetalles(dets != null ? dets : java.util.Collections.emptyList());
                                    } catch (Exception eDet) {
                                        logger.warn("Fallo al consultar detalles de ticket {}: {}", t.getIdTicket(), eDet.getMessage());
                                        tAgg.setDetalles(java.util.Collections.emptyList());
                                    }
                                    ticketsAgg.add(tAgg);
                                }
                            }
                        }
                    } catch (Exception eAgg) {
                        logger.warn("Fallo al construir agregados para factura {}: {}", f.getUuid(), eAgg.getMessage());
                    }
                    agg.setTickets(ticketsAgg);
                    agregadas.add(agg);
                }
                cntFact = facturas != null ? facturas.size() : 0;
                // Asignar estructura agregada en respuesta al final
                // Se completará más abajo junto con totales
                // Guardamos temporalmente en variable para setear en resp
                // Usamos un campo fuera del try/catch
                // -> manejado al construir resp
                // Para mantener código claro, usamos variable local agregadas
                // y la aplicamos post ordenamiento
                // Nota: si DAO no disponible, la lista quedará vacía
                // (ver más abajo)
                // Pasamos agregadas mediante cierre a la construcción de resp
                // usando una referencia efectiva final
                List<FacturaGlobalFacturaDTO> facturasAgregadas = agregadas;
                // Construir respuesta parcial acá no; lo haremos al final
                // Guardamos en un objeto holder si fuera necesario
                // pero por simplicidad reutilizaremos variable facturasAgregadas más abajo
                // Para usar fuera del bloque, redefinimos variable en ámbito exterior
                // (ver edición más abajo)
                //
                // NOTA: El compilador de Java requiere que se declare afuera; ajustamos abajo
            } catch (Exception e) {
                logger.warn("Fallo al consultar FACTURAS: {}", e.getMessage());
            }
        } else {
            logger.warn("ConsultaFacturaDAO no disponible (perfil Oracle no activo)");
        }

        // 2) TICKETS por cada día del rango (DAO soporta igualdad en fecha)
        if (ticketService != null) {
            LocalDate cursor = inicio;
            while (!cursor.isAfter(fin)) {
                try {
                    TicketSearchRequest filtros = new TicketSearchRequest();
                    filtros.setCodigoTienda(req.getCodigoTienda());
                    filtros.setTerminalId(req.getTerminalId());
                    filtros.setFecha(cursor.toString());
                    List<TicketDto> tickets = ticketService.buscarTickets(filtros);
                    for (TicketDto t : tickets) {
                        FacturaGlobalRegistro r = new FacturaGlobalRegistro();
                        r.setTipo("TICKET");
                        r.setTienda(t.getCodigoTienda());
                        r.setTerminal(t.getTerminalId() != null ? String.valueOf(t.getTerminalId()) : null);
                        r.setFecha(t.getFecha());
                        r.setFolio(t.getFolio() != null ? String.valueOf(t.getFolio()) : null);
                        r.setTotal(t.getTotal());
                        String estado = (t.getStatus() != null && t.getStatus() == 0) ? "CANCELADO" : "ACTIVO";
                        r.setEstado(estado);
                        registros.add(r);
                    }
                } catch (Exception e) {
                    logger.warn("Fallo al consultar TICKETS para {}: {}", cursor, e.getMessage());
                }
                cursor = cursor.plusDays(1);
            }
            cntTick = (int) registros.stream().filter(r -> "TICKET".equals(r.getTipo())).count();
        } else {
            logger.warn("TicketService no disponible (perfil Oracle no activo)");
        }

        // 3) CARTA_PORTE por rango de fechas (usa FECHA_SALIDA)
        if (Boolean.TRUE.equals(req.getIncluirCartaPorte()) && cartaPorteConsultaDAO != null) {
            try {
                List<com.cibercom.facturacion_back.dao.CartaPorteConsultaDAO.CartaPorteRow> cps =
                        cartaPorteConsultaDAO.buscarPorRangoFechas(inicio, fin);
                for (var cp : cps) {
                    FacturaGlobalRegistro r = new FacturaGlobalRegistro();
                    r.setTipo("CARTA_PORTE");
                    r.setFecha(cp.fechaSalida != null ? cp.fechaSalida : cp.fechaLlegada);
                    r.setFolio(cp.numeroSerie);
                    r.setTotal(cp.precio);
                    r.setEstado("VIGENTE");
                    registros.add(r);
                }
                cntCp = cps != null ? cps.size() : 0;
            } catch (Exception e) {
                logger.warn("Fallo al consultar CARTA_PORTE: {}", e.getMessage());
            }
        }

        // Ordenar por fecha ascendente
        registros.sort(Comparator.comparing(FacturaGlobalRegistro::getFecha, Comparator.nullsLast(Comparator.naturalOrder())));

        FacturaGlobalConsultaResponse resp = new FacturaGlobalConsultaResponse();
        resp.setRegistros(registros);
        resp.setTotalFacturas(cntFact);
        resp.setTotalTickets(cntTick);
        resp.setTotalCartaPorte(cntCp);
        // Construcción de estructura agregada si hay datos de facturas
        try {
            if (consultaFacturaDAO != null) {
                ConsultaFacturaRequest cfReq = new ConsultaFacturaRequest();
                cfReq.setFechaInicio(inicio);
                cfReq.setFechaFin(fin);
                cfReq.setTienda(req.getCodigoTienda());
                List<FacturaConsultaDTO> facturas = consultaFacturaDAO.buscarFacturas(cfReq);
                List<FacturaGlobalFacturaDTO> agregadas = new ArrayList<>();
                for (FacturaConsultaDTO f : facturas) {
                    FacturaGlobalFacturaDTO agg = new FacturaGlobalFacturaDTO();
                    agg.setUuid(f.getUuid());
                    agg.setSerie(f.getSerie());
                    agg.setFolio(f.getFolio());
                    agg.setFechaEmision(f.getFechaEmision());
                    agg.setImporte(f.getImporte());
                    agg.setEstatusFacturacion(f.getEstatusFacturacion());
                    agg.setEstatusSat(f.getEstatusSat());
                    agg.setTienda(f.getTienda());

                    List<FacturaGlobalTicketDTO> ticketsAgg = new ArrayList<>();
                    try {
                        if (conceptoOracleDAO != null && ticketService != null && ticketDetalleService != null) {
                            java.util.Optional<Long> idOpt = conceptoOracleDAO.obtenerIdFacturaPorUuid(f.getUuid());
                            if (idOpt.isPresent() && idOpt.get() != null) {
                                Long idFactura = idOpt.get();
                                List<TicketDto> vinculados = ticketService.buscarTicketsPorIdFactura(idFactura);
                                for (TicketDto t : vinculados) {
                                    FacturaGlobalTicketDTO tAgg = new FacturaGlobalTicketDTO();
                                    tAgg.setIdTicket(t.getIdTicket());
                                    tAgg.setFecha(t.getFecha());
                                    tAgg.setFolio(t.getFolio());
                                    tAgg.setTotal(t.getTotal());
                                    tAgg.setFormaPago(t.getFormaPago());
                                    try {
                                        List<TicketDetalleDto> dets = ticketDetalleService.buscarDetallesPorIdTicket(t.getIdTicket());
                                        tAgg.setDetalles(dets != null ? dets : java.util.Collections.emptyList());
                                    } catch (Exception eDet) {
                                        logger.warn("Fallo al consultar detalles de ticket {}: {}", t.getIdTicket(), eDet.getMessage());
                                        tAgg.setDetalles(java.util.Collections.emptyList());
                                    }
                                    ticketsAgg.add(tAgg);
                                }
                            }
                        }
                    } catch (Exception eAgg) {
                        logger.warn("Fallo al construir agregados para factura {}: {}", f.getUuid(), eAgg.getMessage());
                    }
                    agg.setTickets(ticketsAgg);
                    agregadas.add(agg);
                }
                resp.setFacturasAgregadas(agregadas);
            }
        } catch (Exception e) {
            logger.warn("Fallo al construir facturas agregadas en respuesta: {}", e.getMessage());
        }
        return resp;
    }

    private BigDecimal normalizarTasa(BigDecimal tasaCol) {
        if (tasaCol == null) return BigDecimal.ZERO;
        // Si viene en porcentaje (16), convertir a fracción (0.16)
        if (tasaCol.compareTo(BigDecimal.ONE) > 0) {
            return tasaCol.divide(new BigDecimal("100"));
        }
        return tasaCol;
    }

    private BigDecimal safeBD(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    /**
     * Guarda una factura global en la base de datos y relaciona las facturas hijas.
     */
    @Transactional
    public FacturaGlobalGuardarResponse guardarFacturaGlobal(FacturaGlobalGuardarRequest request) {
        if (jdbcTemplate == null || conceptoOracleDAO == null) {
            throw new IllegalStateException("Servicios de base de datos no disponibles (perfil Oracle no activo)");
        }

        FacturaGlobalGuardarResponse response = new FacturaGlobalGuardarResponse();

        try {
            // Generar UUID para la factura global
            String uuidGlobal = UUID.randomUUID().toString().toUpperCase().replace("-", "");

            // Insertar factura global en FACTURAS
            // Usar SYSDATE para FECHA como en otros inserts de la aplicación
            String insertSql = "INSERT INTO FACTURAS (" +
                    "RFC_E, RFC_R, ID_RECEPTOR, SERIE, FOLIO, FECHA, UUID, " +
                    "IMPORTE, TIPO_FACTURA, ESTATUS_FACTURA, SUBTOTAL, " +
                    "TIENDA_ORIGEN, USUARIO, COMENTARIOS, RAZON_SOCIAL, " +
                    "SISTEMA_ORIGEN, USO_CFDI, METODO_PAGO, FORMA_PAGO) " +
                    "VALUES (?, ?, ?, ?, ?, SYSDATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // Valores por defecto para factura global
            String rfcEmisor = "XAXX010101000"; // RFC genérico para resúmenes
            String rfcReceptor = "XAXX010101000";
            Long idReceptor = 1L; // ID por defecto, ajustar según tu esquema
            Integer tipoFactura = 3; // TIPO_FACTURA = 3 para factura global (ajustar según tu código)
            Integer estatusFactura = 1; // 1 = Pendiente/Activa (ajustar según tu código)
            Integer usuario = 1; // Usuario sistema o obtener del contexto
            Integer tiendaOrigen = request.getCodigoTienda() != null ? 
                    Integer.parseInt(request.getCodigoTienda()) : null;
            String sistemaOrigen = "GLOBAL"; // Sistema origen para facturas globales
            String usoCFDI = "G03"; // Uso CFDI genérico para resúmenes
            String metodoPago = "PUE"; // Pago en una sola exhibición
            String formaPago = "99"; // Por definir
            
            // Convertir folio a número (FOLIO es NUMBER en la tabla)
            // Usar hash del folio string como número o timestamp
            Long folioNumber;
            String folioString = request.getFolio() != null ? request.getFolio() : 
                    (request.getPeriodo() + "_" + request.getFecha().toString());
            try {
                // Intentar parsear como número si es posible
                folioNumber = Long.parseLong(folioString);
            } catch (NumberFormatException e) {
                // Si no es número, usar hash del string como identificador único
                // Convertir explícitamente de int a Long
                folioNumber = Long.valueOf(Math.abs(folioString.hashCode()));
            }

            // Convertir importe y subtotal a BigDecimal para compatibilidad con Oracle
            BigDecimal importe = request.getImporte() != null ? 
                    BigDecimal.valueOf(request.getImporte()) : BigDecimal.ZERO;
            BigDecimal subtotal = request.getSubtotal() != null ? 
                    BigDecimal.valueOf(request.getSubtotal()) : BigDecimal.ZERO;

            // Insertar factura global (Oracle generará el ID automáticamente si ID_FACTURA es GENERATED ALWAYS AS IDENTITY)
            logger.info("Insertando factura global con UUID: {}, Serie: {}, Folio: {}, Importe: {}", 
                    uuidGlobal, request.getSerie() != null ? request.getSerie() : "RG", folioNumber, importe);
            
            try {
                int rowsInserted = jdbcTemplate.update(insertSql,
                        rfcEmisor,
                        rfcReceptor,
                        idReceptor,
                        request.getSerie() != null ? request.getSerie() : "RG",
                        folioNumber,
                        // FECHA se maneja con SYSDATE en el SQL, no se pasa como parámetro
                        uuidGlobal,
                        importe,
                        tipoFactura,
                        estatusFactura,
                        subtotal,
                        tiendaOrigen,
                        usuario,
                        request.getDescripcion(),
                        request.getDescripcion() != null ? request.getDescripcion() : "Facturación Global",
                        sistemaOrigen,
                        usoCFDI,
                        metodoPago,
                        formaPago
                );
                
                if (rowsInserted == 0) {
                    throw new RuntimeException("No se insertó ninguna fila en FACTURAS");
                }
                
                logger.info("Factura global insertada correctamente. Filas afectadas: {}", rowsInserted);
            } catch (Exception e) {
                logger.error("Error al insertar factura global en BD: {}", e.getMessage(), e);
                throw new RuntimeException("Error al insertar factura global en la base de datos: " + e.getMessage(), e);
            }

            // Obtener el ID generado después de la inserción usando el UUID
            String selectIdSql = "SELECT ID_FACTURA FROM FACTURAS WHERE UUID = ?";
            Long idFacturaGlobal;
            try {
                idFacturaGlobal = jdbcTemplate.queryForObject(selectIdSql, Long.class, uuidGlobal);
            } catch (Exception e) {
                logger.error("Error al obtener ID de factura global después de inserción: {}", e.getMessage());
                throw new RuntimeException("No se pudo obtener el ID de la factura global recién insertada", e);
            }

            // Relacionar facturas hijas: actualizar ID_FACTURA_GLOBAL en las facturas hijas
            Integer facturasRelacionadas = 0;
            if (request.getFacturasHijasUuid() != null && !request.getFacturasHijasUuid().isEmpty()) {
                for (String uuidHija : request.getFacturasHijasUuid()) {
                    try {
                        // Obtener ID de la factura hija por UUID
                        Optional<Long> idHijaOpt = conceptoOracleDAO.obtenerIdFacturaPorUuid(uuidHija);
                        
                        if (idHijaOpt.isPresent() && idHijaOpt.get() != null) {
                            Long idHija = idHijaOpt.get();
                            
                            // Actualizar factura hija para relacionarla con la global
                            String updateSql = "UPDATE FACTURAS SET ID_FACTURA_GLOBAL = ? WHERE ID_FACTURA = ?";
                            int rowsUpdated = jdbcTemplate.update(updateSql, idFacturaGlobal, idHija);
                            
                            if (rowsUpdated > 0) {
                                facturasRelacionadas = facturasRelacionadas + 1;
                            }
                        } else {
                            logger.warn("No se encontró factura con UUID: {}", uuidHija);
                        }
                    } catch (Exception e) {
                        logger.error("Error al relacionar factura hija con UUID {}: {}", uuidHija, e.getMessage());
                    }
                }
            }

            response.setSuccess(true);
            response.setMessage("Factura global guardada exitosamente");
            response.setIdFacturaGlobal(idFacturaGlobal);
            response.setUuid(uuidGlobal);
            response.setSerie(request.getSerie() != null ? request.getSerie() : "RG");
            // Devolver el folio como string para mantener consistencia con el frontend
            response.setFolio(folioString);
            response.setFacturasRelacionadas(facturasRelacionadas);

            logger.info("Factura global guardada exitosamente. ID: {}, UUID: {}, Facturas relacionadas: {}", 
                    idFacturaGlobal, uuidGlobal, facturasRelacionadas);

        } catch (Exception e) {
            logger.error("Error al guardar factura global: {}", e.getMessage(), e);
            logger.error("Stack trace completo:", e);
            response.setSuccess(false);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Error desconocido al guardar factura global";
            response.setMessage("Error al guardar factura global: " + errorMessage);
            // No lanzar RuntimeException aquí, devolver la respuesta con el error
            // para que el controlador pueda manejarlo apropiadamente
            return response;
        }

        return response;
    }

    static class MontoAcumulado {
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
    }
}