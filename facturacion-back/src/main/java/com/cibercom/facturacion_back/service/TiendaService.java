package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.TiendaDto;
import com.cibercom.facturacion_back.model.Tienda;
import com.cibercom.facturacion_back.repository.TiendaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de tiendas
 */
@Service
@Transactional
public class TiendaService {

    private static final Logger logger = LoggerFactory.getLogger(TiendaService.class);

    @Autowired
    private TiendaRepository tiendaRepository;

    /**
     * Crear una nueva tienda
     */
    public TiendaDto crearTienda(TiendaDto tiendaDto) {
        logger.info("Creando nueva tienda con código: {}", tiendaDto.getCodigoTienda());

        // Verificar si el código ya existe
        if (tiendaRepository.existsByCodigoTienda(tiendaDto.getCodigoTienda())) {
            throw new RuntimeException("Ya existe una tienda con el código: " + tiendaDto.getCodigoTienda());
        }

        Tienda tienda = convertirDtoAEntidad(tiendaDto);
        tienda.setUsuarioCreacion(tiendaDto.getUsuarioCreacion() != null ? 
                                 tiendaDto.getUsuarioCreacion() : "sistema");
        
        Tienda tiendaGuardada = tiendaRepository.save(tienda);
        logger.info("Tienda creada exitosamente con ID: {}", tiendaGuardada.getIdTienda());
        
        return convertirEntidadADto(tiendaGuardada);
    }

    /**
     * Actualizar una tienda existente
     */
    public TiendaDto actualizarTienda(Long idTienda, TiendaDto tiendaDto) {
        logger.info("Actualizando tienda con ID: {}", idTienda);

        Optional<Tienda> tiendaExistente = tiendaRepository.findById(idTienda);
        if (tiendaExistente.isEmpty()) {
            throw new RuntimeException("No se encontró la tienda con ID: " + idTienda);
        }

        // Verificar si el código ya existe en otra tienda
        if (tiendaRepository.existsByCodigoTiendaAndIdTiendaNot(tiendaDto.getCodigoTienda(), idTienda)) {
            throw new RuntimeException("Ya existe otra tienda con el código: " + tiendaDto.getCodigoTienda());
        }

        Tienda tienda = tiendaExistente.get();
        actualizarCamposTienda(tienda, tiendaDto);
        tienda.setUsuarioModificacion(tiendaDto.getUsuarioModificacion() != null ? 
                                     tiendaDto.getUsuarioModificacion() : "sistema");
        tienda.setFechaModificacion(LocalDateTime.now());

        Tienda tiendaActualizada = tiendaRepository.save(tienda);
        logger.info("Tienda actualizada exitosamente con ID: {}", tiendaActualizada.getIdTienda());
        
        return convertirEntidadADto(tiendaActualizada);
    }

    /**
     * Obtener todas las tiendas con filtros opcionales
     */
    @Transactional(readOnly = true)
    public List<TiendaDto> listarTiendas(String estadoTienda, String region, String zona, 
                                        String tipoTienda, String busqueda) {
        logger.info("Listando tiendas con filtros - Estado: {}, Región: {}, Zona: {}, Tipo: {}, Búsqueda: {}", 
                   estadoTienda, region, zona, tipoTienda, busqueda);

        List<Tienda> tiendas = tiendaRepository.findTiendasConFiltros(
                estadoTienda, region, zona, tipoTienda, busqueda);
        
        return tiendas.stream()
                     .map(this::convertirEntidadADto)
                     .collect(Collectors.toList());
    }

    /**
     * Obtener una tienda por ID
     */
    @Transactional(readOnly = true)
    public TiendaDto obtenerTiendaPorId(Long idTienda) {
        logger.info("Obteniendo tienda con ID: {}", idTienda);

        Optional<Tienda> tienda = tiendaRepository.findById(idTienda);
        if (tienda.isEmpty()) {
            throw new RuntimeException("No se encontró la tienda con ID: " + idTienda);
        }

        return convertirEntidadADto(tienda.get());
    }

    /**
     * Obtener una tienda por código
     */
    @Transactional(readOnly = true)
    public TiendaDto obtenerTiendaPorCodigo(String codigoTienda) {
        logger.info("Obteniendo tienda con código: {}", codigoTienda);

        Optional<Tienda> tienda = tiendaRepository.findByCodigoTienda(codigoTienda);
        if (tienda.isEmpty()) {
            throw new RuntimeException("No se encontró la tienda con código: " + codigoTienda);
        }

        return convertirEntidadADto(tienda.get());
    }

    /**
     * Eliminar una tienda (soft delete)
     */
    public void eliminarTienda(Long idTienda, String usuarioModificacion) {
        logger.info("Eliminando tienda con ID: {}", idTienda);

        Optional<Tienda> tiendaExistente = tiendaRepository.findById(idTienda);
        if (tiendaExistente.isEmpty()) {
            throw new RuntimeException("No se encontró la tienda con ID: " + idTienda);
        }

        Tienda tienda = tiendaExistente.get();
        tienda.setEstadoTienda("INACTIVO");
        tienda.setUsuarioModificacion(usuarioModificacion != null ? usuarioModificacion : "sistema");
        tienda.setFechaModificacion(LocalDateTime.now());

        tiendaRepository.save(tienda);
        logger.info("Tienda eliminada (soft delete) exitosamente con ID: {}", idTienda);
    }

    /**
     * Eliminar una tienda permanentemente
     */
    public void eliminarTiendaPermanente(Long idTienda) {
        logger.info("Eliminando permanentemente tienda con ID: {}", idTienda);

        if (!tiendaRepository.existsById(idTienda)) {
            throw new RuntimeException("No se encontró la tienda con ID: " + idTienda);
        }

        tiendaRepository.deleteById(idTienda);
        logger.info("Tienda eliminada permanentemente con ID: {}", idTienda);
    }

    /**
     * Obtener estadísticas de tiendas
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas() {
        logger.info("Obteniendo estadísticas de tiendas");

        Object[] estadisticas = tiendaRepository.getEstadisticasTiendas();
        
        Map<String, Object> resultado = new HashMap<>();
        if (estadisticas != null && estadisticas.length > 0) {
            resultado.put("totalTiendas", estadisticas[0]);
            resultado.put("tiendasActivas", estadisticas[1]);
            resultado.put("tiendasInactivas", estadisticas[2]);
            resultado.put("tiendasSuspendidas", estadisticas[3]);
        }

        // Obtener listas de valores únicos
        resultado.put("regiones", tiendaRepository.findDistinctRegiones());
        resultado.put("zonas", tiendaRepository.findDistinctZonas());
        resultado.put("tiposTienda", tiendaRepository.findDistinctTiposTienda());

        return resultado;
    }

    /**
     * Obtener tiendas activas
     */
    @Transactional(readOnly = true)
    public List<TiendaDto> obtenerTiendasActivas() {
        logger.info("Obteniendo tiendas activas");

        List<Tienda> tiendas = tiendaRepository.findTiendasActivas();
        return tiendas.stream()
                     .map(this::convertirEntidadADto)
                     .collect(Collectors.toList());
    }

    /**
     * Convertir entidad a DTO
     */
    private TiendaDto convertirEntidadADto(Tienda tienda) {
        TiendaDto dto = new TiendaDto();
        dto.setIdTienda(tienda.getIdTienda());
        dto.setCodigoTienda(tienda.getCodigoTienda());
        dto.setNombreTienda(tienda.getNombreTienda());
        dto.setDireccion(tienda.getDireccion());
        dto.setCiudad(tienda.getCiudad());
        dto.setEstado(tienda.getEstado());
        dto.setCodigoPostal(tienda.getCodigoPostal());
        dto.setTelefono(tienda.getTelefono());
        dto.setEmail(tienda.getEmail());
        dto.setGerente(tienda.getGerente());
        dto.setRegion(tienda.getRegion());
        dto.setZona(tienda.getZona());
        dto.setTipoTienda(tienda.getTipoTienda());
        dto.setEstadoTienda(tienda.getEstadoTienda());
        dto.setFechaApertura(tienda.getFechaApertura());
        dto.setFechaCreacion(tienda.getFechaCreacion());
        dto.setFechaModificacion(tienda.getFechaModificacion());
        dto.setUsuarioCreacion(tienda.getUsuarioCreacion());
        dto.setUsuarioModificacion(tienda.getUsuarioModificacion());
        dto.setObservaciones(tienda.getObservaciones());
        return dto;
    }

    /**
     * Convertir DTO a entidad
     */
    private Tienda convertirDtoAEntidad(TiendaDto dto) {
        Tienda tienda = new Tienda();
        tienda.setCodigoTienda(dto.getCodigoTienda());
        tienda.setNombreTienda(dto.getNombreTienda());
        tienda.setDireccion(dto.getDireccion());
        tienda.setCiudad(dto.getCiudad());
        tienda.setEstado(dto.getEstado());
        tienda.setCodigoPostal(dto.getCodigoPostal());
        tienda.setTelefono(dto.getTelefono());
        tienda.setEmail(dto.getEmail());
        tienda.setGerente(dto.getGerente());
        tienda.setRegion(dto.getRegion());
        tienda.setZona(dto.getZona());
        tienda.setTipoTienda(dto.getTipoTienda() != null ? dto.getTipoTienda() : "Sucursal");
        tienda.setEstadoTienda(dto.getEstadoTienda() != null ? dto.getEstadoTienda() : "ACTIVO");
        tienda.setFechaApertura(dto.getFechaApertura());
        tienda.setObservaciones(dto.getObservaciones());
        return tienda;
    }

    /**
     * Actualizar campos de la tienda
     */
    private void actualizarCamposTienda(Tienda tienda, TiendaDto dto) {
        tienda.setCodigoTienda(dto.getCodigoTienda());
        tienda.setNombreTienda(dto.getNombreTienda());
        tienda.setDireccion(dto.getDireccion());
        tienda.setCiudad(dto.getCiudad());
        tienda.setEstado(dto.getEstado());
        tienda.setCodigoPostal(dto.getCodigoPostal());
        tienda.setTelefono(dto.getTelefono());
        tienda.setEmail(dto.getEmail());
        tienda.setGerente(dto.getGerente());
        tienda.setRegion(dto.getRegion());
        tienda.setZona(dto.getZona());
        tienda.setTipoTienda(dto.getTipoTienda());
        tienda.setEstadoTienda(dto.getEstadoTienda());
        tienda.setFechaApertura(dto.getFechaApertura());
        tienda.setObservaciones(dto.getObservaciones());
    }
}