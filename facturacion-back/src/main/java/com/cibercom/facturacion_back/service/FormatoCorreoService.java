package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.FormatoCorreoDto;
import com.cibercom.facturacion_back.entity.FormatoCorreo;
import com.cibercom.facturacion_back.repository.FormatoCorreoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servicio para la gestión de configuración de formato de correo electrónico
 */
@Service
@Transactional
public class FormatoCorreoService {
    
    private static final Logger logger = LoggerFactory.getLogger(FormatoCorreoService.class);
    
    @Autowired
    private FormatoCorreoRepository formatoCorreoRepository;
    
    // Cache en memoria y persistencia en archivo
    private volatile FormatoCorreoDto configuracionActivaCache;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Path configuracionArchivoPath = Paths.get("config", "formato-correo-activo.json");
    
    /**
     * Obtiene la configuración de formato activa
     * 
     * @return Configuración activa o configuración por defecto
     */
    public FormatoCorreoDto obtenerConfiguracionActiva() {
        try {
            // 1) Intentar obtener desde cache en memoria
            if (configuracionActivaCache != null) {
                return configuracionActivaCache;
            }
            
            // 2) Intentar leer desde archivo JSON de persistencia
            try {
                if (Files.exists(configuracionArchivoPath)) {
                    String json = Files.readString(configuracionArchivoPath);
                    FormatoCorreoDto dto = jsonMapper.readValue(json, FormatoCorreoDto.class);
                    configuracionActivaCache = dto;
                    return dto;
                }
            } catch (Exception fileEx) {
                logger.warn("No se pudo leer configuración activa desde archivo: {}", fileEx.getMessage());
            }
            
            // 3) Consultar BD
            Optional<FormatoCorreo> configuracionOpt = formatoCorreoRepository.findByActivo(true);
            if (configuracionOpt.isPresent()) {
                FormatoCorreoDto dto = convertirADto(configuracionOpt.get());
                configuracionActivaCache = dto;
                // Persistir en archivo para futuras lecturas rápidas
                try {
                    // Crear directorio si no existe
                    Files.createDirectories(configuracionArchivoPath.getParent());
                    Files.writeString(configuracionArchivoPath, jsonMapper.writeValueAsString(dto));
                } catch (Exception writeEx) {
                    logger.warn("No se pudo persistir configuración activa en archivo: {}", writeEx.getMessage());
                }
                return dto;
            } else {
                // 4) Si no hay configuración activa, crear una por defecto
                FormatoCorreoDto porDefecto = crearConfiguracionPorDefecto();
                configuracionActivaCache = porDefecto;
                try {
                    Files.createDirectories(configuracionArchivoPath.getParent());
                    Files.writeString(configuracionArchivoPath, jsonMapper.writeValueAsString(porDefecto));
                } catch (Exception writeEx) {
                    logger.warn("No se pudo persistir configuración por defecto en archivo: {}", writeEx.getMessage());
                }
                return porDefecto;
            }
        } catch (Exception e) {
            logger.error("Error al obtener configuración activa: {}", e.getMessage(), e);
            FormatoCorreoDto fallback = crearConfiguracionPorDefecto();
            configuracionActivaCache = fallback;
            return fallback;
        }
    }
    
    /**
     * Obtiene todas las configuraciones de formato
     * 
     * @return Lista de todas las configuraciones
     */
    public List<FormatoCorreoDto> obtenerTodasConfiguraciones() {
        try {
            List<FormatoCorreo> configuraciones = formatoCorreoRepository.findAllByOrderByFechaCreacionDesc();
            return configuraciones.stream()
                    .map(this::convertirADto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error al obtener todas las configuraciones: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener configuraciones", e);
        }
    }
    
    /**
     * Guarda una nueva configuración de formato
     * 
     * @param configuracionDto Configuración a guardar
     * @return Configuración guardada
     * @throws RuntimeException si hay un error al guardar la configuración
     */
    public FormatoCorreoDto guardarConfiguracion(FormatoCorreoDto configuracionDto) {
        try {
            // Normalizar y suavizar validaciones (asignar valores por defecto en lugar de lanzar excepciones)
            if (configuracionDto == null) {
                logger.warn("Configuración nula recibida, se usará configuración por defecto");
                configuracionDto = crearConfiguracionPorDefecto();
            }
            if (configuracionDto.getTipoFuente() == null || configuracionDto.getTipoFuente().trim().isEmpty()) {
                logger.warn("tipoFuente inválido ({}), se usará valor por defecto 'Arial'", configuracionDto.getTipoFuente());
                configuracionDto.setTipoFuente("Arial");
            }
            if (configuracionDto.getTamanoFuente() == null || configuracionDto.getTamanoFuente() <= 0) {
                logger.warn("tamanoFuente inválido ({}), se usará valor por defecto 14", configuracionDto.getTamanoFuente());
                configuracionDto.setTamanoFuente(14);
            }
            if (configuracionDto.getColorTexto() == null || !configuracionDto.getColorTexto().matches("^#[0-9A-Fa-f]{6}$")) {
                logger.warn("colorTexto inválido ({}), se usará valor por defecto '#000000'", configuracionDto.getColorTexto());
                configuracionDto.setColorTexto("#000000");
            }
            if (configuracionDto.getEsCursiva() == null) {
                configuracionDto.setEsCursiva(false);
            }
            if (configuracionDto.getEsSubrayado() == null) {
                configuracionDto.setEsSubrayado(false);
            }
            if (configuracionDto.getActivo() == null) {
                configuracionDto.setActivo(true);
            }

            // Verificar si ya existe una configuración similar
            boolean existe = false;
            try {
                existe = formatoCorreoRepository.existsByTipoFuenteAndTamanoFuenteAndEsCursivaAndEsSubrayadoAndColorTextoAndActivo(
                        configuracionDto.getTipoFuente(),
                        configuracionDto.getTamanoFuente(),
                        configuracionDto.getEsCursiva(),
                        configuracionDto.getEsSubrayado(),
                        configuracionDto.getColorTexto(),
                        true
                );
            } catch (Exception dbEx) {
                logger.warn("No se pudo verificar existencia en BD, se continuará con guardar (fallback): {}", dbEx.getMessage());
            }
            
            // Log para depuración
            logger.info("Verificando existencia de configuración: tipoFuente={}, tamanoFuente={}, esCursiva={}, colorTexto={}", 
                    configuracionDto.getTipoFuente(),
                    configuracionDto.getTamanoFuente(),
                    configuracionDto.getEsCursiva(),
                    configuracionDto.getColorTexto());
            
            if (existe) {
                logger.info("Ya existe una configuración similar activa");
                return obtenerConfiguracionActiva();
            }
            
            try {
                // Desactivar todas las configuraciones existentes usando entidades para asegurar convertidor
                List<FormatoCorreo> activas = formatoCorreoRepository.findAllByActivo(true);
                if (activas != null && !activas.isEmpty()) {
                    for (FormatoCorreo cfg : activas) {
                        cfg.setActivo(false);
                    }
                    formatoCorreoRepository.saveAll(activas);
                }
                
                // Crear nueva configuración
                FormatoCorreo nuevaConfiguracion = convertirAEntidad(configuracionDto);
                nuevaConfiguracion.setActivo(true);
                nuevaConfiguracion.setFechaCreacion(LocalDateTime.now());
                
                FormatoCorreo configuracionGuardada = formatoCorreoRepository.save(nuevaConfiguracion);
                
                // Actualizar cache y persistir en archivo
                FormatoCorreoDto dtoGuardado = convertirADto(configuracionGuardada);
                configuracionActivaCache = dtoGuardado;
                try {
                    Files.createDirectories(configuracionArchivoPath.getParent());
                    Files.writeString(configuracionArchivoPath, jsonMapper.writeValueAsString(dtoGuardado));
                } catch (Exception writeEx) {
                    logger.warn("No se pudo persistir configuración activa en archivo: {}", writeEx.getMessage());
                }
                return dtoGuardado;
            } catch (Exception dbSaveEx) {
                logger.error("Error al guardar en BD, devolviendo configuración en memoria para evitar 500", dbSaveEx);
                // Fallback: devolver la configuración recibida en memoria
                return configuracionDto;
            }
        } catch (Exception e) {
            logger.error("Error inesperado al guardar configuración, devolviendo por defecto", e);
            return crearConfiguracionPorDefecto();
        }
    }
    
    /**
     * Actualiza una configuración existente
     * 
     * @param configuracionDto Configuración a actualizar
     * @return Configuración actualizada
     */
    public FormatoCorreoDto actualizarConfiguracion(FormatoCorreoDto configuracionDto) {
        try {
            Optional<FormatoCorreo> configuracionOpt = formatoCorreoRepository.findById(configuracionDto.getId());
            
            if (configuracionOpt.isPresent()) {
                FormatoCorreo configuracion = configuracionOpt.get();
                
                // Actualizar campos
                configuracion.setTipoFuente(configuracionDto.getTipoFuente());
                configuracion.setTamanoFuente(configuracionDto.getTamanoFuente());
                configuracion.setEsCursiva(configuracionDto.getEsCursiva());
                configuracion.setEsSubrayado(configuracionDto.getEsSubrayado());
                configuracion.setColorTexto(configuracionDto.getColorTexto());
                configuracion.setFechaModificacion(LocalDateTime.now());
                
                // Si se está activando esta configuración, desactivar las demás usando entidades
                if (configuracionDto.getActivo() != null && configuracionDto.getActivo()) {
                    List<FormatoCorreo> activas = formatoCorreoRepository.findAllByActivo(true);
                    if (activas != null && !activas.isEmpty()) {
                        for (FormatoCorreo cfg : activas) {
                            cfg.setActivo(false);
                        }
                        formatoCorreoRepository.saveAll(activas);
                    }
                    configuracion.setActivo(true);
                }
                
                FormatoCorreo configuracionActualizada = formatoCorreoRepository.save(configuracion);
                
                // Si queda activa, actualizar cache y persistir archivo
                FormatoCorreoDto dtoActualizado = convertirADto(configuracionActualizada);
                if (Boolean.TRUE.equals(dtoActualizado.getActivo())) {
                    configuracionActivaCache = dtoActualizado;
                    try {
                        Files.createDirectories(configuracionArchivoPath.getParent());
                        Files.writeString(configuracionArchivoPath, jsonMapper.writeValueAsString(dtoActualizado));
                    } catch (Exception writeEx) {
                        logger.warn("No se pudo persistir configuración actualizada en archivo: {}", writeEx.getMessage());
                    }
                }
                
                logger.info("Configuración actualizada con ID: {}", configuracionActualizada.getId());
                return dtoActualizado;
                
            } else {
                throw new RuntimeException("Configuración no encontrada con ID: " + configuracionDto.getId());
            }
            
        } catch (Exception e) {
            logger.error("Error al actualizar configuración: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar configuración", e);
        }
    }
    
    /**
     * Elimina una configuración
     * 
     * @param id ID de la configuración a eliminar
     */
    public void eliminarConfiguracion(Long id) {
        try {
            Optional<FormatoCorreo> configuracionOpt = formatoCorreoRepository.findById(id);
            
            if (configuracionOpt.isPresent()) {
                formatoCorreoRepository.deleteById(id);
                logger.info("Configuración eliminada con ID: {}", id);
                
                // Si era la configuración activa, activar otra o crear una por defecto
                if (configuracionOpt.get().getActivo()) {
                    List<FormatoCorreo> configuraciones = formatoCorreoRepository.findAllByOrderByFechaCreacionDesc();
                    if (!configuraciones.isEmpty()) {
                        FormatoCorreo primeraConfiguracion = configuraciones.get(0);
                        primeraConfiguracion.setActivo(true);
                        FormatoCorreo guardada = formatoCorreoRepository.save(primeraConfiguracion);
                        FormatoCorreoDto dto = convertirADto(guardada);
                        configuracionActivaCache = dto;
                        try {
                            Files.createDirectories(configuracionArchivoPath.getParent());
                            Files.writeString(configuracionArchivoPath, jsonMapper.writeValueAsString(dto));
                        } catch (Exception writeEx) {
                            logger.warn("No se pudo persistir configuración reactivada en archivo: {}", writeEx.getMessage());
                        }
                    } else {
                        FormatoCorreoDto porDefecto = crearConfiguracionPorDefecto();
                        configuracionActivaCache = porDefecto;
                        try {
                            Files.createDirectories(configuracionArchivoPath.getParent());
                            Files.writeString(configuracionArchivoPath, jsonMapper.writeValueAsString(porDefecto));
                        } catch (Exception writeEx) {
                            logger.warn("No se pudo persistir configuración por defecto en archivo tras eliminación: {}", writeEx.getMessage());
                        }
                    }
                }
            } else {
                throw new RuntimeException("Configuración no encontrada con ID: " + id);
            }
            
        } catch (Exception e) {
            logger.error("Error al eliminar configuración: {}", e.getMessage(), e);
            throw new RuntimeException("Error al eliminar configuración", e);
        }
    }
    
    /**
     * Genera estilos CSS para una configuración específica
     * 
     * @param id ID de la configuración
     * @return Estilos CSS como string
     */
    public String generarEstilosCSS(Long id) {
        try {
            Optional<FormatoCorreo> configuracionOpt = formatoCorreoRepository.findById(id);
            
            if (configuracionOpt.isPresent()) {
                return generarEstilosCSS(convertirADto(configuracionOpt.get()));
            } else {
                return generarEstilosCSS(crearConfiguracionPorDefecto());
            }
            
        } catch (Exception e) {
            logger.error("Error al generar estilos CSS: {}", e.getMessage(), e);
            return generarEstilosCSS(crearConfiguracionPorDefecto());
        }
    }
    
    /**
     * Genera estilos CSS para una configuración
     * 
     * @param configuracion Configuración de formato
     * @return Estilos CSS como string
     */
    public String generarEstilosCSS(FormatoCorreoDto configuracion) {
        StringBuilder estilos = new StringBuilder();
    
        estilos.append("font-family: ").append(configuracion.getTipoFuente()).append(", sans-serif; ");
        estilos.append("font-size: ").append(configuracion.getTamanoFuente()).append("px; ");
        estilos.append("color: ").append(configuracion.getColorTexto()).append("; ");
    
        if (configuracion.getEsCursiva() != null && configuracion.getEsCursiva()) {
            estilos.append("font-style: italic; ");
        }
        if (configuracion.getEsNegrita() != null && configuracion.getEsNegrita()) {
            estilos.append("font-weight: bold; ");
        }
        if (configuracion.getEsSubrayado() != null && configuracion.getEsSubrayado()) {
            estilos.append("text-decoration: underline; ");
        }
    
        return estilos.toString();
    }
    
    /**
     * Aplica formato HTML a un contenido
     * 
     * @param contenido Contenido a formatear
     * @param configuracion Configuración de formato
     * @return Contenido con formato HTML aplicado
     */
    public String aplicarFormatoHTML(String contenido, FormatoCorreoDto configuracion) {
        if (contenido == null || contenido.trim().isEmpty()) {
            return contenido;
        }
        
        // Generar estilos CSS personalizados según la configuración
        String estilos = generarEstilosCSS(configuracion);
        
        // Determinar color para el encabezado (fallback al azul por defecto)
        String headerColor = (configuracion != null && configuracion.getColorTexto() != null && !configuracion.getColorTexto().trim().isEmpty()) 
            ? configuracion.getColorTexto().trim() 
            : "#0056b3";
        
        // Inyectar un bloque de estilo para sobrescribir el color del header de la plantilla
        contenido = contenido.replaceFirst(
            "</head>",
            "<style>.header { background-color: " + headerColor + "; }</style></head>"
        );
        
        // Insertar estilos en el body principal
        contenido = contenido.replaceFirst(
            "<body([^>]*)>",
            "<body$1 style=\"" + estilos + "\">"
        );
        
        logger.info("Plantilla HTML cargada y procesada exitosamente con estilos personalizados");
        return contenido;
    }
    
    /**
     * Convierte una entidad a DTO
     * 
     * @param entidad Entidad a convertir
     * @return DTO convertido
     */
    private FormatoCorreoDto convertirADto(FormatoCorreo entidad) {
        FormatoCorreoDto dto = new FormatoCorreoDto();
        dto.setId(entidad.getId());
        dto.setTipoFuente(entidad.getTipoFuente());
        dto.setTamanoFuente(entidad.getTamanoFuente());
        dto.setEsCursiva(entidad.getEsCursiva());
        dto.setEsNegrita(entidad.getEsNegrita());
        dto.setEsSubrayado(entidad.getEsSubrayado());
        dto.setColorTexto(entidad.getColorTexto());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        dto.setFechaModificacion(entidad.getFechaModificacion());
        dto.setActivo(entidad.getActivo());
        return dto;
    }
    
    /**
     * Convierte un DTO a entidad
     * 
     * @param dto DTO a convertir
     * @return Entidad convertida
     */
    private FormatoCorreo convertirAEntidad(FormatoCorreoDto dto) {
        FormatoCorreo entidad = new FormatoCorreo();
        entidad.setId(dto.getId());
        entidad.setTipoFuente(dto.getTipoFuente());
        entidad.setTamanoFuente(dto.getTamanoFuente());
        entidad.setEsCursiva(dto.getEsCursiva());
        entidad.setEsNegrita(dto.getEsNegrita());
        entidad.setEsSubrayado(dto.getEsSubrayado());
        entidad.setColorTexto(dto.getColorTexto());
        entidad.setFechaCreacion(dto.getFechaCreacion());
        entidad.setFechaModificacion(dto.getFechaModificacion());
        entidad.setActivo(dto.getActivo());
        return entidad;
    }
    
    /**
     * Crea una configuración por defecto
     * 
     * @return Configuración por defecto
     */
    private FormatoCorreoDto crearConfiguracionPorDefecto() {
        FormatoCorreoDto configuracionDefault = new FormatoCorreoDto();
        configuracionDefault.setTipoFuente("Arial");
        configuracionDefault.setTamanoFuente(14);
        configuracionDefault.setEsCursiva(false);
        configuracionDefault.setEsSubrayado(false);
        configuracionDefault.setColorTexto("#000000");
        configuracionDefault.setActivo(true);
        return configuracionDefault;
    }

    /**
     * Carga la plantilla HTML desde los recursos y reemplaza las variables, aplicando estilos personalizados en el body.
     * 
     * @param variables Mapa de variables a reemplazar en la plantilla
     * @param configuracion Configuración de formato para aplicar estilos
     * @return Plantilla HTML procesada con estilos personalizados
     */
    public String cargarYProcesarPlantillaHTML(Map<String, String> variables, FormatoCorreoDto configuracion) {
        try {
            // Cargar plantilla desde recursos
            Resource resource = new ClassPathResource("templates/email-template.html");
            String template;
            try (InputStream is = resource.getInputStream()) {
                template = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
            }

            // Valores por defecto para variables no proporcionadas
            Map<String, String> defaultValues = new HashMap<>();
            defaultValues.put("saludo", "Estimado(a) cliente,");
            defaultValues.put("mensajePrincipal", "Se ha generado su factura electrónica.");
            defaultValues.put("agradecimiento", "Gracias por su preferencia.");
            defaultValues.put("mensajePersonalizado", "");
            defaultValues.put("despedida", "Atentamente,");
            defaultValues.put("firma", "Equipo de Facturación Cibercom");
            
            // Combinar variables proporcionadas con valores por defecto
            Map<String, String> allVariables = new HashMap<>(defaultValues);
            if (variables != null) {
                allVariables.putAll(variables);
            }

            // Reemplazar todas las variables en la plantilla
            String contenido = template;
            for (Map.Entry<String, String> entry : allVariables.entrySet()) {
                String variable = "{" + entry.getKey() + "}";
                String valor = entry.getValue() != null ? entry.getValue() : "";
                contenido = contenido.replace(variable, valor);
            }

            // Generar estilos CSS personalizados según la configuración
            String estilos = generarEstilosCSS(configuracion);
            
            // Determinar color para el encabezado (fallback al azul por defecto)
            String headerColor = (configuracion != null && configuracion.getColorTexto() != null && !configuracion.getColorTexto().trim().isEmpty()) 
                ? configuracion.getColorTexto().trim() 
                : "#0056b3";
            
            // Inyectar un bloque de estilo para sobrescribir el color del header de la plantilla
            contenido = contenido.replaceFirst(
                "</head>",
                "<style>.header { background-color: " + headerColor + "; }</style></head>"
            );
            
            // Insertar estilos en el body principal
            contenido = contenido.replaceFirst(
                "<body([^>]*)>",
                "<body$1 style=\"" + estilos + "\">"
            );

            logger.info("Plantilla HTML cargada y procesada exitosamente con estilos personalizados");
            return contenido;

        } catch (Exception e) {
            logger.error("Error al cargar o procesar la plantilla HTML: {}", e.getMessage(), e);
            throw new RuntimeException("Error al procesar la plantilla de correo", e);
        }
    }
}