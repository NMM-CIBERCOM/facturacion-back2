package com.cibercom.facturacion_back.controller;

import com.cibercom.facturacion_back.dto.CatalogoProductoServicioRequest;
import com.cibercom.facturacion_back.model.CatalogoProductoServicio;
import com.cibercom.facturacion_back.service.CatalogoProdServService;
import com.cibercom.facturacion_back.service.CatalogoProductoServicioService;
import com.cibercom.facturacion_back.service.CodigoPostalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CatalogosController {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CatalogoProdServService catalogoProdServService;

    @Autowired(required = false)
    private CatalogoProductoServicioService catalogoProductoServicioService;

    @Autowired
    private CodigoPostalService codigoPostalService;

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

    @GetMapping("/codigos-postales/{codigoPostal}")
    public ResponseEntity<Map<String, Object>> obtenerDatosCodigoPostal(@PathVariable("codigoPostal") String codigoPostal) {
        Map<String, Object> response = new HashMap<>();
        
        if (codigoPostal == null || codigoPostal.length() != 5 || !codigoPostal.matches("\\d{5}")) {
            response.put("error", "Código postal inválido. Debe tener 5 dígitos numéricos");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Optional<CodigoPostalService.CodigoPostalResult> resultado = codigoPostalService.buscarPorCodigo(codigoPostal);
            
            if (resultado.isPresent()) {
                CodigoPostalService.CodigoPostalResult data = resultado.get();
                response.put("codigoPostal", data.getCodigoPostal());
                response.put("estado", data.getEstado() != null ? data.getEstado() : "");
                response.put("municipio", data.getMunicipio() != null ? data.getMunicipio() : "");
                response.put("colonias", data.getColonias() != null ? data.getColonias() : List.of());
                return ResponseEntity.ok(response);
            } else {
                // No se encontró el código postal
                response.put("codigoPostal", codigoPostal);
                response.put("estado", "");
                response.put("municipio", "");
                response.put("colonias", List.of());
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CatalogosController.class);
            logger.error("Error al buscar código postal: {}", e.getMessage(), e);
            response.put("codigoPostal", codigoPostal);
            response.put("estado", "");
            response.put("municipio", "");
            response.put("colonias", List.of());
            response.put("error", "Error al consultar código postal: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/catalogos/sat/productos-servicios")
    public ResponseEntity<Map<String, Object>> buscarProductosServicios(
            @RequestParam(value = "busqueda", required = false, defaultValue = "") String busqueda,
            @RequestParam(value = "limite", required = false, defaultValue = "20") int limite) {
        
        Map<String, Object> response = new HashMap<>();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CatalogosController.class);
        
        try {
            logger.info("Búsqueda de productos/servicios. Búsqueda: '{}', Límite: {}", busqueda, limite);
            
            List<CatalogoProdServService.CatalogoItem> resultados;
            
            if (busqueda == null || busqueda.trim().isEmpty()) {
                logger.debug("Búsqueda vacía, retornando lista vacía");
                resultados = new java.util.ArrayList<>();
            } else {
                logger.debug("Ejecutando búsqueda en catálogo...");
                resultados = catalogoProdServService.buscar(busqueda, limite);
                logger.debug("Búsqueda completada. Resultados encontrados: {}", resultados != null ? resultados.size() : 0);
            }

            if (resultados == null) {
                logger.warn("El servicio retornó null");
                resultados = new java.util.ArrayList<>();
            }

            List<Map<String, Object>> resultadosDTO = resultados.stream()
                    .filter(item -> item != null) // Filtrar items nulos
                    .map(item -> {
                        try {
                            Map<String, Object> itemMap = new HashMap<>();
                            itemMap.put("clave", item.getClave() != null ? item.getClave() : "");
                            itemMap.put("descripcion", item.getDescripcion() != null ? item.getDescripcion() : "");
                            itemMap.put("unidad", item.getUnidad() != null ? item.getUnidad() : "");
                            return itemMap;
                        } catch (Exception e) {
                            logger.warn("Error al mapear item: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(map -> map != null) // Filtrar maps nulos
                    .collect(Collectors.toList());

            response.put("exitoso", true);
            response.put("resultados", resultadosDTO);
            response.put("total", resultadosDTO.size());
            
            logger.info("Respuesta exitosa. Total de resultados: {}", resultadosDTO.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al buscar en el catálogo: {}", e.getMessage(), e);
            response.put("exitoso", false);
            response.put("error", "Error al buscar en el catálogo: " + e.getMessage());
            response.put("detalle", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/catalogos/sat/productos-servicios/{clave}")
    public ResponseEntity<Map<String, Object>> obtenerProductoServicioPorClave(@PathVariable String clave) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CatalogoProdServService.CatalogoItem item = catalogoProdServService.buscarPorClave(clave);
            
            if (item == null) {
                response.put("exitoso", false);
                response.put("mensaje", "No se encontró el producto/servicio con la clave: " + clave);
                return ResponseEntity.status(404).body(response);
            }

            response.put("exitoso", true);
            response.put("clave", item.getClave());
            response.put("descripcion", item.getDescripcion());
            response.put("unidad", item.getUnidad());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("exitoso", false);
            response.put("error", "Error al buscar la clave: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================================================
    // ENDPOINTS PARA CATÁLOGO PERSONALIZADO DE PRODUCTOS/SERVICIOS POR USUARIO
    // ============================================================================

    /**
     * Obtiene todos los productos/servicios del catálogo personalizado del usuario
     */
    @GetMapping("/catalogos/productos-servicios")
    public ResponseEntity<Map<String, Object>> obtenerCatalogoUsuario(
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        Map<String, Object> response = new HashMap<>();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CatalogosController.class);

        try {
            Long usuarioId = parseUsuario(usuarioStr);
            if (usuarioId == null || usuarioId == 0) {
                response.put("exitoso", false);
                response.put("error", "ID de usuario no válido");
                return ResponseEntity.badRequest().body(response);
            }

            if (catalogoProductoServicioService == null) {
                response.put("exitoso", true);
                response.put("resultados", List.of());
                response.put("total", 0);
                response.put("mensaje", "Servicio no disponible (perfil oracle no activo)");
                return ResponseEntity.ok(response);
            }

            List<CatalogoProductoServicio> catalogos = catalogoProductoServicioService.obtenerPorUsuario(usuarioId);
            List<Map<String, Object>> resultadosDTO = catalogos.stream()
                    .map(this::mapearCatalogoADTO)
                    .collect(Collectors.toList());

            response.put("exitoso", true);
            response.put("resultados", resultadosDTO);
            response.put("total", resultadosDTO.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al obtener catálogo del usuario: {}", e.getMessage(), e);
            response.put("exitoso", false);
            response.put("error", "Error al obtener catálogo: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Busca en el catálogo personalizado del usuario
     */
    @GetMapping("/catalogos/productos-servicios/buscar")
    public ResponseEntity<Map<String, Object>> buscarEnCatalogoUsuario(
            @RequestParam(value = "busqueda", required = false, defaultValue = "") String busqueda,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        Map<String, Object> response = new HashMap<>();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CatalogosController.class);

        try {
            Long usuarioId = parseUsuario(usuarioStr);
            if (usuarioId == null || usuarioId == 0) {
                response.put("exitoso", false);
                response.put("error", "ID de usuario no válido");
                return ResponseEntity.badRequest().body(response);
            }

            if (catalogoProductoServicioService == null) {
                response.put("exitoso", true);
                response.put("resultados", List.of());
                response.put("total", 0);
                return ResponseEntity.ok(response);
            }

            List<CatalogoProductoServicio> catalogos = catalogoProductoServicioService.buscarPorUsuarioYTexto(
                    usuarioId, busqueda);
            List<Map<String, Object>> resultadosDTO = catalogos.stream()
                    .map(this::mapearCatalogoADTO)
                    .collect(Collectors.toList());

            response.put("exitoso", true);
            response.put("resultados", resultadosDTO);
            response.put("total", resultadosDTO.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al buscar en catálogo: {}", e.getMessage(), e);
            response.put("exitoso", false);
            response.put("error", "Error al buscar: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Guarda un producto/servicio en el catálogo personalizado del usuario
     */
    @PostMapping("/catalogos/productos-servicios")
    public ResponseEntity<Map<String, Object>> guardarCatalogo(
            @RequestBody CatalogoProductoServicioRequest request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        Map<String, Object> response = new HashMap<>();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CatalogosController.class);

        try {
            Long usuarioId = parseUsuario(usuarioStr);
            if (usuarioId == null || usuarioId == 0) {
                response.put("exitoso", false);
                response.put("error", "ID de usuario no válido");
                return ResponseEntity.badRequest().body(response);
            }

            if (catalogoProductoServicioService == null) {
                logger.warn("CatalogoProductoServicioService no disponible - perfil oracle no activo");
                response.put("exitoso", false);
                response.put("error", "Servicio no disponible. El perfil 'oracle' debe estar activo. Activa el perfil con: --spring.profiles.active=dev,oracle");
                return ResponseEntity.status(503).body(response);
            }

            // Validar campos requeridos
            if (request.getClaveProdServ() == null || request.getClaveProdServ().trim().isEmpty()) {
                response.put("exitoso", false);
                response.put("error", "La clave de producto/servicio es obligatoria");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getDescripcion() == null || request.getDescripcion().trim().isEmpty()) {
                response.put("exitoso", false);
                response.put("error", "La descripción es obligatoria");
                return ResponseEntity.badRequest().body(response);
            }

            // Convertir DTO a entidad
            CatalogoProductoServicio catalogo = convertirRequestAEntidad(request);

            // Guardar
            logger.info("Intentando guardar producto/servicio para usuario {}", usuarioId);
            CatalogoProductoServicio guardado = catalogoProductoServicioService.guardar(usuarioId, catalogo);

            response.put("exitoso", true);
            response.put("mensaje", "Producto/servicio guardado exitosamente");
            response.put("id", guardado.getId());
            response.put("catalogo", mapearCatalogoADTO(guardado));
            logger.info("Producto/servicio guardado exitosamente con ID: {}", guardado.getId());
            return ResponseEntity.ok(response);
        } catch (org.springframework.dao.DataAccessException e) {
            logger.error("Error de base de datos al guardar catálogo: {}", e.getMessage(), e);
            String mensajeError = e.getMessage();
            String mensajeUsuario = mensajeError;
            
            // Manejo específico de errores Oracle
            if (mensajeError != null) {
                if (mensajeError.contains("ORA-01017") || mensajeError.contains("nombre de usuario/contraseña")) {
                    mensajeUsuario = "Error de conexión a la base de datos: Credenciales incorrectas o base de datos no accesible. " +
                                   "Verifica que Oracle esté corriendo y que las credenciales en application.yml sean correctas.";
                } else if (mensajeError.contains("ORA-00942") || (mensajeError.contains("table") && mensajeError.contains("does not exist"))) {
                    mensajeUsuario = "La tabla CATALOGOS_PRODUCTOS_SERVICIOS no existe. Ejecuta el script: create_catalogos_productos_servicios.sql";
                } else if (mensajeError.contains("Unable to acquire JDBC Connection") || mensajeError.contains("Connection refused")) {
                    mensajeUsuario = "No se puede conectar a la base de datos Oracle. Verifica que: " +
                                   "1) Oracle esté corriendo en localhost:1521, " +
                                   "2) El servicio Oracle esté activo, " +
                                   "3) Las credenciales sean correctas.";
                }
            }
            
            response.put("exitoso", false);
            response.put("error", mensajeUsuario);
            response.put("detalle", "Error técnico: " + mensajeError);
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            logger.error("Error al guardar catálogo: {}", e.getMessage(), e);
            response.put("exitoso", false);
            response.put("error", "Error al guardar: " + e.getMessage());
            response.put("detalle", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                response.put("causa", e.getCause().getMessage());
            }
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Actualiza un producto/servicio en el catálogo
     */
    @PutMapping("/catalogos/productos-servicios/{id}")
    public ResponseEntity<Map<String, Object>> actualizarCatalogo(
            @PathVariable Long id,
            @RequestBody CatalogoProductoServicioRequest request,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        Map<String, Object> response = new HashMap<>();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CatalogosController.class);

        try {
            Long usuarioId = parseUsuario(usuarioStr);
            if (usuarioId == null || usuarioId == 0) {
                response.put("exitoso", false);
                response.put("error", "ID de usuario no válido");
                return ResponseEntity.badRequest().body(response);
            }

            if (catalogoProductoServicioService == null) {
                response.put("exitoso", false);
                response.put("error", "Servicio no disponible");
                return ResponseEntity.status(503).body(response);
            }

            CatalogoProductoServicio catalogo = convertirRequestAEntidad(request);
            CatalogoProductoServicio actualizado = catalogoProductoServicioService.actualizar(usuarioId, id, catalogo);

            response.put("exitoso", true);
            response.put("mensaje", "Producto/servicio actualizado exitosamente");
            response.put("catalogo", mapearCatalogoADTO(actualizado));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("exitoso", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error al actualizar catálogo: {}", e.getMessage(), e);
            response.put("exitoso", false);
            response.put("error", "Error al actualizar: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Elimina un producto/servicio del catálogo
     */
    @DeleteMapping("/catalogos/productos-servicios/{id}")
    public ResponseEntity<Map<String, Object>> eliminarCatalogo(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario", required = false, defaultValue = "0") String usuarioStr) {
        Map<String, Object> response = new HashMap<>();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CatalogosController.class);

        try {
            Long usuarioId = parseUsuario(usuarioStr);
            if (usuarioId == null || usuarioId == 0) {
                response.put("exitoso", false);
                response.put("error", "ID de usuario no válido");
                return ResponseEntity.badRequest().body(response);
            }

            if (catalogoProductoServicioService == null) {
                response.put("exitoso", false);
                response.put("error", "Servicio no disponible");
                return ResponseEntity.status(503).body(response);
            }

            catalogoProductoServicioService.eliminar(usuarioId, id);

            response.put("exitoso", true);
            response.put("mensaje", "Producto/servicio eliminado exitosamente");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("exitoso", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error al eliminar catálogo: {}", e.getMessage(), e);
            response.put("exitoso", false);
            response.put("error", "Error al eliminar: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Métodos auxiliares
    private Long parseUsuario(String usuarioStr) {
        if (usuarioStr == null || usuarioStr.trim().isEmpty() || "0".equals(usuarioStr.trim())) {
            return null;
        }
        try {
            return Long.parseLong(usuarioStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private CatalogoProductoServicio convertirRequestAEntidad(CatalogoProductoServicioRequest request) {
        CatalogoProductoServicio catalogo = new CatalogoProductoServicio();
        catalogo.setClaveProdServ(request.getClaveProdServ());
        catalogo.setCantidad(request.getCantidad() != null ? request.getCantidad() : BigDecimal.ONE);
        catalogo.setUnidad(request.getUnidad() != null ? request.getUnidad() : "Servicio");
        catalogo.setClaveUnidad(request.getClaveUnidad() != null && !request.getClaveUnidad().trim().isEmpty() 
            ? request.getClaveUnidad() : "E48");
        catalogo.setDescripcion(request.getDescripcion());
        catalogo.setObjetoImpuesto(request.getObjetoImpuesto() != null ? request.getObjetoImpuesto() : "02");
        catalogo.setValorUnitario(request.getValorUnitario() != null ? request.getValorUnitario() : BigDecimal.ZERO);
        catalogo.setImporte(request.getImporte() != null ? request.getImporte() : BigDecimal.ZERO);
        catalogo.setTasaIva(request.getTasaIva());
        return catalogo;
    }

    private Map<String, Object> mapearCatalogoADTO(CatalogoProductoServicio catalogo) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", catalogo.getId());
        dto.put("claveProdServ", catalogo.getClaveProdServ());
        dto.put("cantidad", catalogo.getCantidad());
        dto.put("unidad", catalogo.getUnidad());
        dto.put("claveUnidad", catalogo.getClaveUnidad());
        dto.put("descripcion", catalogo.getDescripcion());
        dto.put("objetoImpuesto", catalogo.getObjetoImpuesto());
        dto.put("valorUnitario", catalogo.getValorUnitario());
        dto.put("importe", catalogo.getImporte());
        dto.put("tasaIVA", catalogo.getTasaIva());
        dto.put("fechaCreacion", catalogo.getFechaCreacion());
        dto.put("fechaActualizacion", catalogo.getFechaActualizacion());
        return dto;
    }
}