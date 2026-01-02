package com.cibercom.facturacion_back.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogoProdServService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogoProdServService.class);
    private static final String EXCEL_PATH = "librerias/complements/catalogo-productos-servicios-sat.xls";
    private List<CatalogoItem> catalogoCache = null;

    @PostConstruct
    public void inicializar() {
        try {
            logger.info("Inicializando servicio de catálogo de productos/servicios SAT...");
            logger.info("Buscando archivo Excel en: {}", EXCEL_PATH);
            cargarCatalogo(); // Precargar el catálogo al iniciar
            logger.info("Servicio de catálogo inicializado. Items cargados: {}", 
                       catalogoCache != null ? catalogoCache.size() : 0);
        } catch (Exception e) {
            logger.error("Error al inicializar el servicio de catálogo: {}", e.getMessage(), e);
            // No lanzar excepción para que el servicio pueda seguir funcionando
            // El catálogo se intentará cargar la primera vez que se use
        }
    }

    public static class CatalogoItem {
        private String clave;
        private String descripcion;
        private String unidad;

        public CatalogoItem(String clave, String descripcion, String unidad) {
            this.clave = clave;
            this.descripcion = descripcion != null ? descripcion : "";
            this.unidad = unidad != null ? unidad : "";
        }

        public String getClave() { return clave; }
        public String getDescripcion() { return descripcion; }
        public String getUnidad() { return unidad; }
    }

    /**
     * Carga el catálogo desde el Excel en memoria (cache)
     */
    private synchronized List<CatalogoItem> cargarCatalogo() {
        if (catalogoCache != null) {
            logger.debug("Usando catálogo en cache con {} items", catalogoCache.size());
            return catalogoCache;
        }

        List<CatalogoItem> items = new ArrayList<>();
        InputStream inputStream = null;
        Workbook workbook = null;
        
        try {
            logger.info("Iniciando carga del catálogo desde: {}", EXCEL_PATH);
            ClassPathResource resource = new ClassPathResource(EXCEL_PATH);
            
            if (!resource.exists()) {
                logger.error("No se encontró el archivo Excel en la ruta: {}", EXCEL_PATH);
                logger.error("Intentando buscar el archivo en el classpath...");
                // Intentar con diferentes rutas posibles
                String[] posiblesRutas = {
                    "librerias/complements/catalogo-productos-servicios-sat.xls",
                    "complements/catalogo-productos-servicios-sat.xls",
                    "catalogo-productos-servicios-sat.xls",
                    "classpath:librerias/complements/catalogo-productos-servicios-sat.xls"
                };
                
                boolean encontrado = false;
                for (String ruta : posiblesRutas) {
                    try {
                        ClassPathResource altResource = new ClassPathResource(ruta);
                        if (altResource.exists()) {
                            logger.info("Archivo encontrado en ruta alternativa: {}", ruta);
                            resource = altResource;
                            encontrado = true;
                            break;
                        }
                    } catch (Exception e) {
                        // Continuar con la siguiente ruta
                    }
                }
                
                if (!encontrado) {
                    logger.error("No se encontró el archivo Excel en ninguna de las rutas posibles");
                    return items;
                }
            }

            boolean workbookCreado = false;
            
            try {
                // Intentar abrir como XLS primero (formato .xls)
                logger.debug("Intentando abrir como XLS (HSSF)...");
                inputStream = resource.getInputStream();
                workbook = new HSSFWorkbook(inputStream);
                workbookCreado = true;
                logger.info("Catálogo Excel abierto correctamente como XLS (HSSF)");
            } catch (Exception e) {
                logger.warn("No se pudo abrir como XLS, intentando como XLSX: {}", e.getMessage());
                // Si falla, intentar como XLSX
                if (inputStream != null) {
                    try { inputStream.close(); } catch (Exception ignored) {}
                    inputStream = null;
                }
                try {
                    logger.debug("Intentando abrir como XLSX...");
                    inputStream = resource.getInputStream();
                    workbook = new XSSFWorkbook(inputStream);
                    workbookCreado = true;
                    logger.info("Catálogo Excel abierto correctamente como XLSX");
                } catch (Exception e2) {
                    logger.error("Error al abrir el archivo Excel en ambos formatos (XLS y XLSX): {}", e2.getMessage(), e2);
                    if (inputStream != null) {
                        try { inputStream.close(); } catch (Exception ignored) {}
                    }
                    return items;
                }
            }
            
            if (!workbookCreado || workbook == null) {
                logger.error("No se pudo crear el workbook del archivo Excel");
                if (inputStream != null) {
                    try { inputStream.close(); } catch (Exception ignored) {}
                }
                return items;
            }

            Sheet sheet = workbook.getSheetAt(0); // Primera hoja
            
            // Leer encabezados (primera fila)
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                logger.warn("La primera fila del Excel está vacía");
                workbook.close();
                if (inputStream != null) {
                    try { inputStream.close(); } catch (Exception ignored) {}
                }
                return items;
            }

            // Buscar índices de columnas (buscando en las primeras filas por si hay encabezados)
            int claveIndex = -1;
            int descripcionIndex = -1;
            int unidadIndex = -1;
            int headerRowIndex = -1; // Fila donde encontramos los encabezados

            // Buscar en las primeras 3 filas los encabezados
            for (int rowIdx = 0; rowIdx < Math.min(3, sheet.getLastRowNum() + 1); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                // Resetear índices para esta fila
                int tempClaveIndex = -1;
                int tempDescripcionIndex = -1;
                int tempUnidadIndex = -1;

                for (int colIdx = 0; colIdx < row.getLastCellNum(); colIdx++) {
                    Cell cell = row.getCell(colIdx);
                    if (cell == null) continue;

                    String cellValue = obtenerValorCelda(cell).toLowerCase().trim();
                    
                    if (tempClaveIndex == -1 && (cellValue.contains("clave") || cellValue.contains("código"))) {
                        tempClaveIndex = colIdx;
                    }
                    if (tempDescripcionIndex == -1 && (cellValue.contains("descripción") || cellValue.contains("descripcion") || cellValue.contains("nombre"))) {
                        tempDescripcionIndex = colIdx;
                    }
                    if (tempUnidadIndex == -1 && cellValue.contains("unidad")) {
                        tempUnidadIndex = colIdx;
                    }
                }

                // Si encontramos al menos clave y descripción, esta es la fila de encabezados
                if (tempClaveIndex >= 0 && tempDescripcionIndex >= 0) {
                    claveIndex = tempClaveIndex;
                    descripcionIndex = tempDescripcionIndex;
                    unidadIndex = tempUnidadIndex >= 0 ? tempUnidadIndex : -1;
                    headerRowIndex = rowIdx;
                    break;
                }
            }

            // Determinar la fila de inicio de datos
            int startRow;
            if (claveIndex >= 0 && descripcionIndex >= 0 && headerRowIndex >= 0) {
                // Encontramos encabezados, empezar desde la siguiente fila
                startRow = headerRowIndex + 1;
            } else {
                // Si no encontramos encabezados, verificar si la primera fila es encabezado o dato
                Row firstRow = sheet.getRow(0);
                if (firstRow != null && firstRow.getCell(0) != null) {
                    String firstCellValue = obtenerValorCelda(firstRow.getCell(0)).trim();
                    // Si la primera celda parece ser una clave numérica (como "01010101"), es dato, no encabezado
                    if (firstCellValue.matches("^\\d+$")) {
                        startRow = 0;
                        claveIndex = 0;
                        descripcionIndex = 1;
                        unidadIndex = 2;
                    } else {
                        // Es encabezado
                        startRow = 1;
                        claveIndex = 0;
                        descripcionIndex = 1;
                        unidadIndex = 2;
                    }
                } else {
                    startRow = 1;
                    claveIndex = 0;
                    descripcionIndex = 1;
                    unidadIndex = 2;
                }
            }

            logger.info("Leyendo catálogo desde fila {} con índices: clave={}, descripcion={}, unidad={}", 
                       startRow, claveIndex, descripcionIndex, unidadIndex);

            // Leer datos desde la fila determinada
            for (int rowIdx = startRow; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                String clave = obtenerValorCelda(row.getCell(claveIndex)).trim();
                String descripcion = obtenerValorCelda(row.getCell(descripcionIndex)).trim();
                String unidad = unidadIndex >= 0 ? obtenerValorCelda(row.getCell(unidadIndex)).trim() : "";

                // Solo agregar si tiene clave y descripción
                if (!clave.isEmpty() && !descripcion.isEmpty()) {
                    items.add(new CatalogoItem(clave, descripcion, unidad));
                }
            }

            logger.info("Catálogo cargado exitosamente: {} items", items.size());
            catalogoCache = items;
        } catch (Exception e) {
            logger.error("Error al cargar el catálogo desde Excel: {}", e.getMessage(), e);
            logger.error("Stack trace completo:", e);
            items = new ArrayList<>(); // Asegurar que siempre retornamos una lista vacía en caso de error
        } finally {
            // Asegurar que siempre cerramos los recursos
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    logger.warn("Error al cerrar workbook: {}", e.getMessage());
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    logger.warn("Error al cerrar input stream: {}", e.getMessage());
                }
            }
        }

        return items;
    }

    /**
     * Obtiene el valor de una celda como String
     */
    private String obtenerValorCelda(Cell cell) {
        if (cell == null) return "";
        
        try {
            CellType cellType = cell.getCellType();
            
            // Si es fórmula, obtener el tipo de valor calculado
            if (cellType == CellType.FORMULA) {
                cellType = cell.getCachedFormulaResultType();
            }
            
            switch (cellType) {
                case STRING:
                    String strValue = cell.getStringCellValue();
                    return strValue != null ? strValue : "";
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        // Si es numérico pero queremos string, convertir sin decimales si es entero
                        double numValue = cell.getNumericCellValue();
                        if (numValue == (long) numValue) {
                            return String.valueOf((long) numValue);
                        } else {
                            return String.valueOf(numValue);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    // Este caso no debería llegar aquí ya que lo manejamos arriba, pero por si acaso
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e) {
                        try {
                            double numValue = cell.getNumericCellValue();
                            if (numValue == (long) numValue) {
                                return String.valueOf((long) numValue);
                            } else {
                                return String.valueOf(numValue);
                            }
                        } catch (Exception e2) {
                            return "";
                        }
                    }
                case BLANK:
                case _NONE:
                    return "";
                default:
                    // Intentar obtener como string por defecto
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e) {
                        return "";
                    }
            }
        } catch (Exception e) {
            logger.warn("Error al obtener valor de celda: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Busca en el catálogo por clave o descripción
     */
    public List<CatalogoItem> buscar(String busqueda, int limite) {
        try {
            if (busqueda == null || busqueda.trim().isEmpty()) {
                return new ArrayList<>();
            }

            List<CatalogoItem> catalogo = cargarCatalogo();
            if (catalogo == null || catalogo.isEmpty()) {
                logger.warn("El catálogo está vacío o no se pudo cargar");
                return new ArrayList<>();
            }

            String busquedaLower = busqueda.toLowerCase().trim();

            return catalogo.stream()
                    .filter(item -> {
                        try {
                            if (item == null || item.getClave() == null || item.getDescripcion() == null) {
                                return false;
                            }
                            return item.getClave().toLowerCase().contains(busquedaLower) ||
                                   item.getDescripcion().toLowerCase().contains(busquedaLower);
                        } catch (Exception e) {
                            logger.warn("Error al filtrar item: {}", e.getMessage());
                            return false;
                        }
                    })
                    .limit(limite > 0 ? limite : 50)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error al buscar en el catálogo: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Busca una clave exacta
     */
    public CatalogoItem buscarPorClave(String clave) {
        try {
            if (clave == null || clave.trim().isEmpty()) {
                return null;
            }

            List<CatalogoItem> catalogo = cargarCatalogo();
            if (catalogo == null || catalogo.isEmpty()) {
                logger.warn("El catálogo está vacío al buscar clave: {}", clave);
                return null;
            }

            String claveBuscar = clave.trim();

            return catalogo.stream()
                    .filter(item -> {
                        try {
                            return item != null && 
                                   item.getClave() != null && 
                                   item.getClave().equals(claveBuscar);
                        } catch (Exception e) {
                            logger.warn("Error al filtrar por clave: {}", e.getMessage());
                            return false;
                        }
                    })
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Error al buscar clave en el catálogo: {}", e.getMessage(), e);
            return null;
        }
    }
}

