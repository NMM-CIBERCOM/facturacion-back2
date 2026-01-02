package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.DashboardEstadisticasResponse;
import com.cibercom.facturacion_back.dto.FacturaSustituidaResponse;
import com.cibercom.facturacion_back.dto.FacturasPorUsuarioResponse;
import com.cibercom.facturacion_back.repository.FacturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Profile("oracle")
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FacturaRepository facturaRepository;

    public DashboardEstadisticasResponse obtenerEstadisticas() {
        try {
            DashboardEstadisticasResponse.EstadisticasRapidas estadisticasRapidas = obtenerEstadisticasRapidas();
            List<DashboardEstadisticasResponse.DatoGrafico> datosGrafico = obtenerDatosGrafico();
            List<DashboardEstadisticasResponse.FacturaResumen> ultimasFacturas = obtenerUltimasFacturas();

            return DashboardEstadisticasResponse.builder()
                    .exitoso(true)
                    .mensaje("Estadísticas obtenidas correctamente")
                    .estadisticasRapidas(estadisticasRapidas)
                    .datosGrafico(datosGrafico)
                    .ultimasFacturas(ultimasFacturas)
                    .build();
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas del dashboard: {}", e.getMessage(), e);
            return DashboardEstadisticasResponse.builder()
                    .exitoso(false)
                    .mensaje("Error al obtener estadísticas: " + e.getMessage())
                    .build();
        }
    }

    private DashboardEstadisticasResponse.EstadisticasRapidas obtenerEstadisticasRapidas() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = hoy.atTime(23, 59, 59);

        LocalDate primerDiaMes = hoy.withDayOfMonth(1);
        LocalDateTime inicioMes = primerDiaMes.atStartOfDay();
        LocalDateTime finMes = hoy.atTime(23, 59, 59);

        LocalDate primerDiaAnio = hoy.withDayOfYear(1);
        LocalDateTime inicioAnio = primerDiaAnio.atStartOfDay();
        LocalDateTime finAnio = hoy.atTime(23, 59, 59);

        // Contar facturas
        Long facturasHoy = (long) facturaRepository.findByFechaFacturaBetween(inicioHoy, finHoy).size();
        Long facturasMes = (long) facturaRepository.findByFechaFacturaBetween(inicioMes, finMes).size();
        Long facturasAnio = (long) facturaRepository.findByFechaFacturaBetween(inicioAnio, finAnio).size();

        // Calcular ingresos
        BigDecimal ingresosHoy = calcularIngresos(inicioHoy, finHoy);
        BigDecimal ingresosMes = calcularIngresos(inicioMes, finMes);
        BigDecimal ingresosAnio = calcularIngresos(inicioAnio, finAnio);

        return DashboardEstadisticasResponse.EstadisticasRapidas.builder()
                .facturasHoy(facturasHoy)
                .facturasMes(facturasMes)
                .facturasAnio(facturasAnio)
                .ingresosHoy(ingresosHoy != null ? ingresosHoy : BigDecimal.ZERO)
                .ingresosMes(ingresosMes != null ? ingresosMes : BigDecimal.ZERO)
                .ingresosAnio(ingresosAnio != null ? ingresosAnio : BigDecimal.ZERO)
                .build();
    }

    private BigDecimal calcularIngresos(LocalDateTime inicio, LocalDateTime fin) {
        try {
            String sql = "SELECT NVL(SUM(IMPORTE), 0) FROM FACTURAS WHERE FECHA BETWEEN ? AND ?";
            BigDecimal resultado = jdbcTemplate.queryForObject(sql, BigDecimal.class, inicio, fin);
            return resultado != null ? resultado : BigDecimal.ZERO;
        } catch (Exception e) {
            logger.warn("Error calculando ingresos: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private List<DashboardEstadisticasResponse.DatoGrafico> obtenerDatosGrafico() {
        List<DashboardEstadisticasResponse.DatoGrafico> datos = new ArrayList<>();
        
        // Obtener últimos 7 meses
        LocalDate hoy = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate fechaMes = hoy.minusMonths(i);
            String mesAbrev = fechaMes.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            mesAbrev = mesAbrev.substring(0, 1).toUpperCase() + mesAbrev.substring(1).toLowerCase();
            
            LocalDate primerDia = fechaMes.withDayOfMonth(1);
            LocalDate ultimoDia = fechaMes.withDayOfMonth(fechaMes.lengthOfMonth());
            LocalDateTime inicio = primerDia.atStartOfDay();
            LocalDateTime fin = ultimoDia.atTime(23, 59, 59);

            // Contar facturas (todas las que NO son notas de crédito, es decir, que no tienen UUID_ORIG)
            Long facturas = contarFacturas(inicio, fin);
            
            // Boletas ya no se usan, dejar en 0
            Long boletas = 0L;
            
            // Contar notas de crédito (facturas con UUID_ORIG o tipo específico)
            Long notas = contarNotas(inicio, fin);
            
            // Contar tickets
            Long tickets = contarTickets(inicio, fin);

            datos.add(DashboardEstadisticasResponse.DatoGrafico.builder()
                    .mes(mesAbrev)
                    .facturas(facturas)
                    .boletas(boletas)
                    .notas(notas)
                    .tickets(tickets)
                    .build());
        }

        return datos;
    }

    private Long contarFacturas(LocalDateTime inicio, LocalDateTime fin) {
        try {
            // Contar facturas que NO son notas de crédito (no tienen UUID_ORIG)
            String sql = "SELECT COUNT(*) FROM FACTURAS WHERE FECHA BETWEEN ? AND ? AND (UUID_ORIG IS NULL OR UUID_ORIG = '')";
            Long resultado = jdbcTemplate.queryForObject(sql, Long.class, inicio, fin);
            return resultado != null ? resultado : 0L;
        } catch (Exception e) {
            logger.warn("Error contando facturas: {}", e.getMessage());
            // Fallback: contar todas las facturas si la consulta falla
            try {
                String sqlFallback = "SELECT COUNT(*) FROM FACTURAS WHERE FECHA BETWEEN ? AND ?";
                Long resultado = jdbcTemplate.queryForObject(sqlFallback, Long.class, inicio, fin);
                return resultado != null ? resultado : 0L;
            } catch (Exception e2) {
                logger.warn("Error en fallback de conteo de facturas: {}", e2.getMessage());
                return 0L;
            }
        }
    }

    private Long contarFacturasPorTipo(LocalDateTime inicio, LocalDateTime fin, Integer tipoFactura) {
        try {
            String sql = "SELECT COUNT(*) FROM FACTURAS WHERE FECHA BETWEEN ? AND ? AND TIPO_FACTURA = ?";
            Long resultado = jdbcTemplate.queryForObject(sql, Long.class, inicio, fin, tipoFactura);
            return resultado != null ? resultado : 0L;
        } catch (Exception e) {
            logger.warn("Error contando facturas por tipo {}: {}", tipoFactura, e.getMessage());
            return 0L;
        }
    }

    private Long contarNotas(LocalDateTime inicio, LocalDateTime fin) {
        try {
            // Asumiendo que hay una tabla NOTAS_CREDITO o similar
            // Si no existe, puedes usar una consulta a FACTURAS con un tipo específico
            String sql = "SELECT COUNT(*) FROM NOTAS_CREDITO WHERE FECHA BETWEEN ? AND ?";
            Long resultado = jdbcTemplate.queryForObject(sql, Long.class, inicio, fin);
            return resultado != null ? resultado : 0L;
        } catch (Exception e) {
            // Si la tabla no existe, intentar con FACTURAS que tengan tipo específico
            try {
                String sql = "SELECT COUNT(*) FROM FACTURAS WHERE FECHA BETWEEN ? AND ? AND (TIPO_FACTURA = 3 OR UUID_ORIG IS NOT NULL)";
                Long resultado = jdbcTemplate.queryForObject(sql, Long.class, inicio, fin);
                return resultado != null ? resultado : 0L;
            } catch (Exception e2) {
                logger.warn("Error contando notas: {}", e2.getMessage());
                return 0L;
            }
        }
    }

    private Long contarTickets(LocalDateTime inicio, LocalDateTime fin) {
        try {
            String sql = "SELECT COUNT(*) FROM TICKETS WHERE TRUNC(FECHA) BETWEEN TRUNC(?) AND TRUNC(?)";
            Long resultado = jdbcTemplate.queryForObject(sql, Long.class, inicio.toLocalDate(), fin.toLocalDate());
            return resultado != null ? resultado : 0L;
        } catch (Exception e) {
            logger.warn("Error contando tickets: {}", e.getMessage());
            return 0L;
        }
    }

    private List<DashboardEstadisticasResponse.FacturaResumen> obtenerUltimasFacturas() {
        try {
            String sql = "SELECT * FROM (" +
                    "SELECT UUID, SERIE, FOLIO, RAZON_SOCIAL, IMPORTE, FECHA " +
                    "FROM FACTURAS " +
                    "ORDER BY FECHA DESC" +
                    ") WHERE ROWNUM <= 10";
            
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                DashboardEstadisticasResponse.FacturaResumen resumen = 
                    DashboardEstadisticasResponse.FacturaResumen.builder()
                        .uuid(rs.getString("UUID"))
                        .serie(rs.getString("SERIE"))
                        .folio(rs.getString("FOLIO"))
                        .receptorRazonSocial(rs.getString("RAZON_SOCIAL"))
                        .total(rs.getBigDecimal("IMPORTE"))
                        .fechaFactura(rs.getTimestamp("FECHA") != null ? 
                            rs.getTimestamp("FECHA").toLocalDateTime() : null)
                        .build();
                return resumen;
            });
        } catch (Exception e) {
            logger.error("Error obteniendo últimas facturas: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public FacturaSustituidaResponse consultarFacturasSustituidas(LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
            LocalDateTime fin = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

            String sql = "SELECT f.UUID, f.UUID_ORIG, f.SERIE, f.FOLIO, f.RAZON_SOCIAL, f.RFC_R, f.IMPORTE, f.FECHA, " +
                    "f_orig.SERIE AS SERIE_ORIG, f_orig.FOLIO AS FOLIO_ORIG " +
                    "FROM FACTURAS f " +
                    "LEFT JOIN FACTURAS f_orig ON f.UUID_ORIG = f_orig.UUID " +
                    "WHERE f.UUID_ORIG IS NOT NULL";

            List<Object> params = new ArrayList<>();
            if (inicio != null) {
                sql += " AND f.FECHA >= ?";
                params.add(inicio);
            }
            if (fin != null) {
                sql += " AND f.FECHA <= ?";
                params.add(fin);
            }
            sql += " ORDER BY f.FECHA DESC";

            List<FacturaSustituidaResponse.FacturaSustituida> facturas = jdbcTemplate.query(
                    sql,
                    params.toArray(),
                    new org.springframework.jdbc.core.RowMapper<FacturaSustituidaResponse.FacturaSustituida>() {
                        @Override
                        public FacturaSustituidaResponse.FacturaSustituida mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
                            return FacturaSustituidaResponse.FacturaSustituida.builder()
                                    .uuid(rs.getString("UUID"))
                                    .uuidOrig(rs.getString("UUID_ORIG"))
                                    .serie(rs.getString("SERIE"))
                                    .folio(rs.getString("FOLIO"))
                                    .receptorRazonSocial(rs.getString("RAZON_SOCIAL"))
                                    .receptorRfc(rs.getString("RFC_R"))
                                    .total(rs.getBigDecimal("IMPORTE"))
                                    .fechaFactura(rs.getTimestamp("FECHA") != null ? 
                                        rs.getTimestamp("FECHA").toLocalDateTime() : null)
                                    .serieOrig(rs.getString("SERIE_ORIG"))
                                    .folioOrig(rs.getString("FOLIO_ORIG"))
                                    .build();
                        }
                    }
            );

            return FacturaSustituidaResponse.builder()
                    .exitoso(true)
                    .mensaje("Facturas sustituidas obtenidas correctamente")
                    .facturas(facturas)
                    .build();
        } catch (Exception e) {
            logger.error("Error consultando facturas sustituidas: {}", e.getMessage(), e);
            return FacturaSustituidaResponse.builder()
                    .exitoso(false)
                    .mensaje("Error al consultar facturas sustituidas: " + e.getMessage())
                    .facturas(new ArrayList<>())
                    .build();
        }
    }

    public FacturasPorUsuarioResponse consultarFacturasPorUsuario(String usuarioFiltro, LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
            LocalDateTime fin = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

            // Si se proporciona un filtro de usuario, buscar ID_DFI en la tabla USUARIOS por NO_USUARIO
            Integer idDfiFiltro = null;
            String nombreUsuarioFiltro = null;
            java.util.Map<Integer, String> idDfiToNombreMap = new java.util.HashMap<>();
            
            if (usuarioFiltro != null && !usuarioFiltro.trim().isEmpty()) {
                try {
                    // Buscar en USUARIOS por NO_USUARIO para obtener ID_DFI y NOMBRE_EMPLEADO
                    String sqlBuscarUsuario = "SELECT ID_DFI, NOMBRE_EMPLEADO, NO_USUARIO FROM USUARIOS WHERE UPPER(NO_USUARIO) LIKE UPPER(?)";
                    List<java.util.Map<String, Object>> usuariosEncontrados = jdbcTemplate.queryForList(
                        sqlBuscarUsuario, "%" + usuarioFiltro.trim() + "%");
                    
                    if (!usuariosEncontrados.isEmpty()) {
                        // Si se encontró un usuario, usar su ID_DFI (o ID_PERFIL si ID_DFI es null)
                        for (java.util.Map<String, Object> usuarioRow : usuariosEncontrados) {
                            Integer idDfi = usuarioRow.get("ID_DFI") != null ? 
                                ((java.math.BigDecimal) usuarioRow.get("ID_DFI")).intValue() : null;
                            String nombreEmpleado = (String) usuarioRow.get("NOMBRE_EMPLEADO");
                            String noUsuario = (String) usuarioRow.get("NO_USUARIO");
                            
                            // Si ID_DFI es null, buscar ID_PERFIL para este usuario
                            if (idDfi == null) {
                                String sqlPerfil = "SELECT ID_PERFIL FROM USUARIOS WHERE NO_USUARIO = ?";
                                try {
                                    Integer idPerfil = jdbcTemplate.queryForObject(sqlPerfil, Integer.class, noUsuario);
                                    if (idPerfil != null) {
                                        idDfi = idPerfil; // Usar ID_PERFIL como fallback
                                    }
                                } catch (Exception e) {
                                    logger.warn("No se pudo obtener ID_PERFIL para usuario {}: {}", noUsuario, e.getMessage());
                                }
                            }
                            
                            if (idDfi != null) {
                                idDfiFiltro = idDfi;
                                nombreUsuarioFiltro = nombreEmpleado != null ? nombreEmpleado : noUsuario;
                                idDfiToNombreMap.put(idDfi, nombreUsuarioFiltro);
                                logger.info("Usuario encontrado: NO_USUARIO={}, ID_DFI={}, NOMBRE={}", 
                                           noUsuario, idDfi, nombreUsuarioFiltro);
                            }
                        }
                    } else {
                        logger.warn("No se encontró usuario con NO_USUARIO que contenga: {}", usuarioFiltro);
                    }
                } catch (Exception e) {
                    logger.error("Error buscando usuario por NO_USUARIO: {}", e.getMessage(), e);
                }
            }

            // Consulta para obtener facturas agrupadas por usuario
            // Buscar por ID_DFI en el campo USUARIO de FACTURAS
            String sqlFacturas = "SELECT f.UUID, f.SERIE, f.FOLIO, f.RAZON_SOCIAL, f.RFC_R, f.IMPORTE, f.FECHA, " +
                    "f.ESTATUS_FACTURA, f.STATUS_SAT, f.TIPO_FACTURA, " +
                    "COALESCE(TO_CHAR(f.USUARIO), 'SIN_USUARIO') AS USUARIO " +
                    "FROM FACTURAS f " +
                    "WHERE 1=1";

            List<Object> params = new ArrayList<>();
            if (idDfiFiltro != null) {
                // Filtrar por ID_DFI específico
                sqlFacturas += " AND f.USUARIO = ?";
                params.add(idDfiFiltro);
                logger.info("Filtrando facturas por ID_DFI: {}", idDfiFiltro);
            } else if (usuarioFiltro != null && !usuarioFiltro.trim().isEmpty()) {
                // Si no se encontró ID_DFI pero hay filtro, buscar por coincidencia en USUARIO (puede ser numérico)
                try {
                    // Intentar convertir el filtro a número para buscar directamente
                    Integer usuarioNum = Integer.parseInt(usuarioFiltro.trim());
                    sqlFacturas += " AND f.USUARIO = ?";
                    params.add(usuarioNum);
                    logger.info("Filtrando facturas por USUARIO numérico: {}", usuarioNum);
                } catch (NumberFormatException e) {
                    // Si no es numérico, buscar por LIKE en el campo USUARIO convertido a string
                    sqlFacturas += " AND TO_CHAR(f.USUARIO) LIKE ?";
                    params.add("%" + usuarioFiltro.trim() + "%");
                    logger.info("Filtrando facturas por USUARIO (texto): {}", usuarioFiltro);
                }
            }
            if (inicio != null) {
                sqlFacturas += " AND f.FECHA >= ?";
                params.add(inicio);
            }
            if (fin != null) {
                sqlFacturas += " AND f.FECHA <= ?";
                params.add(fin);
            }
            sqlFacturas += " ORDER BY f.FECHA DESC";

            // Consulta para notas de crédito (asumiendo que tienen UUID_ORIG o tipo específico)
            String sqlNotas = "SELECT f.UUID, f.SERIE, f.FOLIO, f.RAZON_SOCIAL, f.RFC_R, f.IMPORTE, f.FECHA, " +
                    "f.ESTATUS_FACTURA, f.STATUS_SAT, f.TIPO_FACTURA, " +
                    "COALESCE(TO_CHAR(f.USUARIO), 'SIN_USUARIO') AS USUARIO " +
                    "FROM FACTURAS f " +
                    "WHERE (f.UUID_ORIG IS NOT NULL OR f.TIPO_FACTURA = 3)";

            List<Object> paramsNotas = new ArrayList<>();
            if (idDfiFiltro != null) {
                // Filtrar por ID_DFI específico
                sqlNotas += " AND f.USUARIO = ?";
                paramsNotas.add(idDfiFiltro);
            } else if (usuarioFiltro != null && !usuarioFiltro.trim().isEmpty()) {
                // Si no se encontró ID_DFI pero hay filtro, buscar por coincidencia en USUARIO
                try {
                    Integer usuarioNum = Integer.parseInt(usuarioFiltro.trim());
                    sqlNotas += " AND f.USUARIO = ?";
                    paramsNotas.add(usuarioNum);
                } catch (NumberFormatException e) {
                    sqlNotas += " AND TO_CHAR(f.USUARIO) LIKE ?";
                    paramsNotas.add("%" + usuarioFiltro.trim() + "%");
                }
            }
            if (inicio != null) {
                sqlNotas += " AND f.FECHA >= ?";
                paramsNotas.add(inicio);
            }
            if (fin != null) {
                sqlNotas += " AND f.FECHA <= ?";
                paramsNotas.add(fin);
            }
            sqlNotas += " ORDER BY f.FECHA DESC";

            // Estructura para almacenar documentos con su usuario
            class DocumentoConUsuario {
                FacturasPorUsuarioResponse.DocumentoFacturacion documento;
                String usuario;
                
                DocumentoConUsuario(FacturasPorUsuarioResponse.DocumentoFacturacion doc, String usr) {
                    this.documento = doc;
                    this.usuario = usr;
                }
            }

            List<DocumentoConUsuario> todosDocumentosConUsuario = new ArrayList<>();

            // Obtener todas las facturas con usuario
            try {
                List<DocumentoConUsuario> facturasConUsuario = jdbcTemplate.query(sqlFacturas, params.toArray(),
                    new org.springframework.jdbc.core.RowMapper<DocumentoConUsuario>() {
                        @Override
                        public DocumentoConUsuario mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
                            String usuario = rs.getString("USUARIO");
                            if (usuario == null || usuario.trim().isEmpty()) {
                                usuario = "SIN_USUARIO";
                            }
                            
                            FacturasPorUsuarioResponse.DocumentoFacturacion doc = 
                                FacturasPorUsuarioResponse.DocumentoFacturacion.builder()
                                    .uuid(rs.getString("UUID"))
                                    .tipo("FACTURA")
                                    .serie(rs.getString("SERIE"))
                                    .folio(rs.getString("FOLIO"))
                                    .receptorRazonSocial(rs.getString("RAZON_SOCIAL"))
                                    .receptorRfc(rs.getString("RFC_R"))
                                    .total(rs.getBigDecimal("IMPORTE"))
                                    .fechaFactura(rs.getTimestamp("FECHA") != null ? 
                                        rs.getTimestamp("FECHA").toLocalDateTime() : null)
                                    .estatusFacturacion(rs.getString("ESTATUS_FACTURA"))
                                    .estatusSat(rs.getString("STATUS_SAT"))
                                    .build();
                            
                            return new DocumentoConUsuario(doc, usuario);
                        }
                    });
                todosDocumentosConUsuario.addAll(facturasConUsuario);
            } catch (Exception e) {
                logger.warn("Error consultando facturas (puede que no exista columna USUARIO_CREACION): {}", e.getMessage());
                // Intentar sin columna de usuario
                try {
                    String sqlSinUsuario = sqlFacturas.replace("COALESCE(f.USUARIO_CREACION, 'SIN_USUARIO') AS USUARIO, ", "");
                    List<DocumentoConUsuario> facturasSinUsuario = jdbcTemplate.query(sqlSinUsuario, params.toArray(),
                        (rs, rowNum) -> {
                            FacturasPorUsuarioResponse.DocumentoFacturacion doc = 
                                FacturasPorUsuarioResponse.DocumentoFacturacion.builder()
                                    .uuid(rs.getString("UUID"))
                                    .tipo("FACTURA")
                                    .serie(rs.getString("SERIE"))
                                    .folio(rs.getString("FOLIO"))
                                    .receptorRazonSocial(rs.getString("RAZON_SOCIAL"))
                                    .receptorRfc(rs.getString("RFC_R"))
                                    .total(rs.getBigDecimal("IMPORTE"))
                                    .fechaFactura(rs.getTimestamp("FECHA") != null ? 
                                        rs.getTimestamp("FECHA").toLocalDateTime() : null)
                                    .estatusFacturacion(rs.getString("ESTATUS_FACTURA"))
                                    .estatusSat(rs.getString("STATUS_SAT"))
                                    .build();
                            return new DocumentoConUsuario(doc, "SIN_USUARIO");
                        });
                    todosDocumentosConUsuario.addAll(facturasSinUsuario);
                } catch (Exception e2) {
                    logger.error("Error consultando facturas sin columna usuario: {}", e2.getMessage());
                }
            }

            // Obtener notas de crédito con usuario
            try {
                List<DocumentoConUsuario> notasConUsuario = jdbcTemplate.query(sqlNotas, paramsNotas.toArray(),
                    new org.springframework.jdbc.core.RowMapper<DocumentoConUsuario>() {
                        @Override
                        public DocumentoConUsuario mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
                            String usuario = rs.getString("USUARIO");
                            if (usuario == null || usuario.trim().isEmpty()) {
                                usuario = "SIN_USUARIO";
                            }
                            
                            FacturasPorUsuarioResponse.DocumentoFacturacion doc = 
                                FacturasPorUsuarioResponse.DocumentoFacturacion.builder()
                                    .uuid(rs.getString("UUID"))
                                    .tipo("NOTA_CREDITO")
                                    .serie(rs.getString("SERIE"))
                                    .folio(rs.getString("FOLIO"))
                                    .receptorRazonSocial(rs.getString("RAZON_SOCIAL"))
                                    .receptorRfc(rs.getString("RFC_R"))
                                    .total(rs.getBigDecimal("IMPORTE"))
                                    .fechaFactura(rs.getTimestamp("FECHA") != null ? 
                                        rs.getTimestamp("FECHA").toLocalDateTime() : null)
                                    .estatusFacturacion(rs.getString("ESTATUS_FACTURA"))
                                    .estatusSat(rs.getString("STATUS_SAT"))
                                    .build();
                            
                            return new DocumentoConUsuario(doc, usuario);
                        }
                    });
                todosDocumentosConUsuario.addAll(notasConUsuario);
            } catch (Exception e) {
                logger.warn("Error consultando notas de crédito: {}", e.getMessage());
            }

            // Obtener nombres de usuarios desde la tabla USUARIOS para todos los IDs encontrados
            java.util.Map<String, String> usuarioIdToNombreMap = new java.util.HashMap<>();
            if (!todosDocumentosConUsuario.isEmpty()) {
                try {
                    // Recopilar todos los IDs de usuario únicos
                    java.util.Set<String> usuarioIds = new java.util.HashSet<>();
                    for (DocumentoConUsuario doc : todosDocumentosConUsuario) {
                        String usuarioId = doc.usuario;
                        if (usuarioId != null && !usuarioId.equals("SIN_USUARIO") && !usuarioId.trim().isEmpty()) {
                            try {
                                // Intentar convertir a número para buscar por ID_DFI
                                Integer idDfi = Integer.parseInt(usuarioId.trim());
                                usuarioIds.add(usuarioId.trim());
                            } catch (NumberFormatException e) {
                                // Si no es numérico, ignorar
                            }
                        }
                    }
                    
                    // Buscar nombres de usuarios por ID_DFI o ID_PERFIL
                    if (!usuarioIds.isEmpty()) {
                        String sqlNombres = "SELECT ID_DFI, ID_PERFIL, NOMBRE_EMPLEADO, NO_USUARIO FROM USUARIOS WHERE ID_DFI IN (" +
                                usuarioIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(",")) + 
                                ") OR ID_PERFIL IN (" +
                                usuarioIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(",")) + ")";
                        
                        List<Object> paramsNombres = new ArrayList<>();
                        paramsNombres.addAll(usuarioIds);
                        paramsNombres.addAll(usuarioIds);
                        
                        List<java.util.Map<String, Object>> usuariosNombres = jdbcTemplate.queryForList(sqlNombres, paramsNombres.toArray());
                        for (java.util.Map<String, Object> usuarioRow : usuariosNombres) {
                            Integer idDfi = usuarioRow.get("ID_DFI") != null ? 
                                ((java.math.BigDecimal) usuarioRow.get("ID_DFI")).intValue() : null;
                            Integer idPerfil = usuarioRow.get("ID_PERFIL") != null ? 
                                ((java.math.BigDecimal) usuarioRow.get("ID_PERFIL")).intValue() : null;
                            String nombreEmpleado = (String) usuarioRow.get("NOMBRE_EMPLEADO");
                            String noUsuario = (String) usuarioRow.get("NO_USUARIO");
                            
                            if (idDfi != null) {
                                usuarioIdToNombreMap.put(String.valueOf(idDfi), nombreEmpleado != null ? nombreEmpleado : noUsuario);
                            }
                            if (idPerfil != null) {
                                usuarioIdToNombreMap.put(String.valueOf(idPerfil), nombreEmpleado != null ? nombreEmpleado : noUsuario);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error obteniendo nombres de usuarios: {}", e.getMessage());
                }
            }
            
            // Agrupar por usuario
            java.util.Map<String, FacturasPorUsuarioResponse.UsuarioFacturas> usuariosMap = new java.util.HashMap<>();
            
            for (DocumentoConUsuario docConUsuario : todosDocumentosConUsuario) {
                String usuario = docConUsuario.usuario;
                FacturasPorUsuarioResponse.DocumentoFacturacion doc = docConUsuario.documento;
                
                // Obtener nombre del usuario desde el mapa o usar el ID como fallback
                String nombreUsuario = usuarioIdToNombreMap.getOrDefault(usuario, 
                    idDfiToNombreMap.values().stream().findFirst().orElse(usuario));
                
                usuariosMap.putIfAbsent(usuario, FacturasPorUsuarioResponse.UsuarioFacturas.builder()
                        .usuario(usuario)
                        .nombreUsuario(nombreUsuario)
                        .totalFacturas(0L)
                        .totalNotasCredito(0L)
                        .totalImporte(BigDecimal.ZERO)
                        .documentos(new ArrayList<>())
                        .build());
                
                FacturasPorUsuarioResponse.UsuarioFacturas usuarioFacturas = usuariosMap.get(usuario);
                usuarioFacturas.getDocumentos().add(doc);
                
                if ("FACTURA".equals(doc.getTipo())) {
                    usuarioFacturas.setTotalFacturas(usuarioFacturas.getTotalFacturas() + 1);
                } else if ("NOTA_CREDITO".equals(doc.getTipo())) {
                    usuarioFacturas.setTotalNotasCredito(usuarioFacturas.getTotalNotasCredito() + 1);
                }
                
                if (doc.getTotal() != null) {
                    usuarioFacturas.setTotalImporte(usuarioFacturas.getTotalImporte().add(doc.getTotal()));
                }
            }

            return FacturasPorUsuarioResponse.builder()
                    .exitoso(true)
                    .mensaje("Facturas por usuario obtenidas correctamente")
                    .usuarios(new ArrayList<>(usuariosMap.values()))
                    .build();
        } catch (Exception e) {
            logger.error("Error consultando facturas por usuario: {}", e.getMessage(), e);
            return FacturasPorUsuarioResponse.builder()
                    .exitoso(false)
                    .mensaje("Error al consultar facturas por usuario: " + e.getMessage())
                    .usuarios(new ArrayList<>())
                    .build();
        }
    }
}

