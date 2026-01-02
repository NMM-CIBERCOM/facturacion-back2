package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.model.CatalogoProductoServicio;
import com.cibercom.facturacion_back.repository.CatalogoProductoServicioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Profile("oracle")
public class CatalogoProductoServicioService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogoProductoServicioService.class);

    @Autowired
    private CatalogoProductoServicioRepository repository;

    /**
     * Obtiene todos los productos/servicios activos de un usuario
     */
    public List<CatalogoProductoServicio> obtenerPorUsuario(Long idUsuario) {
        logger.debug("Obteniendo catálogo de productos/servicios para usuario: {}", idUsuario);
        return repository.findByIdUsuarioAndActivoOrderByDescripcion(idUsuario, "1");
    }

    /**
     * Busca productos/servicios por texto (clave o descripción)
     */
    public List<CatalogoProductoServicio> buscarPorUsuarioYTexto(Long idUsuario, String busqueda) {
        logger.debug("Buscando en catálogo de usuario {} con texto: {}", idUsuario, busqueda);
        if (busqueda == null || busqueda.trim().isEmpty()) {
            return obtenerPorUsuario(idUsuario);
        }
        return repository.buscarPorUsuarioYTexto(idUsuario, busqueda.trim());
    }

    /**
     * Guarda un nuevo producto/servicio o actualiza si ya existe
     */
    @Transactional(rollbackFor = Exception.class)
    public CatalogoProductoServicio guardar(Long idUsuario, CatalogoProductoServicio catalogo) {
        logger.info("Guardando producto/servicio {} para usuario {}", catalogo.getClaveProdServ(), idUsuario);

        try {
            // Verificar si ya existe (buscar también inactivos para reactivarlos)
            Optional<CatalogoProductoServicio> existenteActivo = repository.findByIdUsuarioAndClaveProdServAndActivo(
                    idUsuario, catalogo.getClaveProdServ(), "1");
            
            // Si no está activo, buscar cualquier registro con esa clave y usuario
            Optional<CatalogoProductoServicio> existenteCualquiera = repository.findByIdUsuarioAndClaveProdServ(
                    idUsuario, catalogo.getClaveProdServ());

            CatalogoProductoServicio aGuardar;
            if (existenteActivo.isPresent()) {
                // Actualizar existente activo
                aGuardar = existenteActivo.get();
                logger.debug("Actualizando producto/servicio existente activo ID: {}", aGuardar.getId());
            } else if (existenteCualquiera.isPresent()) {
                // Reactivar y actualizar existente inactivo
                aGuardar = existenteCualquiera.get();
                logger.debug("Reactivando y actualizando producto/servicio existente ID: {}", aGuardar.getId());
                aGuardar.setActivo("1");
            } else {
                // Crear nuevo
                catalogo.setIdUsuario(idUsuario);
                catalogo.setActivo("1");
                aGuardar = catalogo;
            }

            // Actualizar campos
            aGuardar.setCantidad(catalogo.getCantidad() != null ? catalogo.getCantidad() : BigDecimal.ONE);
            aGuardar.setUnidad(catalogo.getUnidad() != null ? catalogo.getUnidad() : "Servicio");
            aGuardar.setClaveUnidad(catalogo.getClaveUnidad() != null && !catalogo.getClaveUnidad().trim().isEmpty() 
                ? catalogo.getClaveUnidad() : "E48");
            aGuardar.setDescripcion(catalogo.getDescripcion());
            aGuardar.setObjetoImpuesto(catalogo.getObjetoImpuesto() != null ? catalogo.getObjetoImpuesto() : "02");
            aGuardar.setValorUnitario(catalogo.getValorUnitario() != null ? catalogo.getValorUnitario() : BigDecimal.ZERO);
            if (catalogo.getTasaIva() != null) {
                aGuardar.setTasaIva(catalogo.getTasaIva());
            }

            // Calcular importe
            BigDecimal cantidad = aGuardar.getCantidad() != null ? aGuardar.getCantidad() : BigDecimal.ONE;
            BigDecimal valorUnitario = aGuardar.getValorUnitario() != null ? aGuardar.getValorUnitario() : BigDecimal.ZERO;
            aGuardar.setImporte(cantidad.multiply(valorUnitario));

            CatalogoProductoServicio guardado = repository.save(aGuardar);
            logger.info("Producto/servicio guardado exitosamente con ID: {}", guardado.getId());
            return guardado;
        } catch (Exception e) {
            logger.error("Error al guardar producto/servicio: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar producto/servicio: " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza un producto/servicio existente
     */
    @Transactional
    public CatalogoProductoServicio actualizar(Long idUsuario, Long id, CatalogoProductoServicio catalogo) {
        logger.info("Actualizando producto/servicio ID {} para usuario {}", id, idUsuario);

        Optional<CatalogoProductoServicio> existente = repository.findById(id);
        if (existente.isEmpty()) {
            throw new IllegalArgumentException("No se encontró el producto/servicio con ID: " + id);
        }

        CatalogoProductoServicio aActualizar = existente.get();
        if (!aActualizar.getIdUsuario().equals(idUsuario)) {
            throw new IllegalArgumentException("El producto/servicio no pertenece al usuario");
        }

        aActualizar.setCantidad(catalogo.getCantidad());
        aActualizar.setUnidad(catalogo.getUnidad());
        aActualizar.setClaveUnidad(catalogo.getClaveUnidad() != null && !catalogo.getClaveUnidad().trim().isEmpty() 
            ? catalogo.getClaveUnidad() : "E48");
        aActualizar.setDescripcion(catalogo.getDescripcion());
        aActualizar.setObjetoImpuesto(catalogo.getObjetoImpuesto());
        aActualizar.setValorUnitario(catalogo.getValorUnitario());
        aActualizar.setImporte(catalogo.getImporte());
        if (catalogo.getTasaIva() != null) {
            aActualizar.setTasaIva(catalogo.getTasaIva());
        }

        // Recalcular importe
        BigDecimal cantidad = aActualizar.getCantidad() != null ? aActualizar.getCantidad() : BigDecimal.ONE;
        BigDecimal valorUnitario = aActualizar.getValorUnitario() != null ? aActualizar.getValorUnitario() : BigDecimal.ZERO;
        aActualizar.setImporte(cantidad.multiply(valorUnitario));

        return repository.save(aActualizar);
    }

    /**
     * Elimina (marca como inactivo) un producto/servicio
     */
    @Transactional
    public void eliminar(Long idUsuario, Long id) {
        logger.info("Eliminando producto/servicio ID {} para usuario {}", id, idUsuario);

        Optional<CatalogoProductoServicio> existente = repository.findById(id);
        if (existente.isEmpty()) {
            throw new IllegalArgumentException("No se encontró el producto/servicio con ID: " + id);
        }

        CatalogoProductoServicio aEliminar = existente.get();
        if (!aEliminar.getIdUsuario().equals(idUsuario)) {
            throw new IllegalArgumentException("El producto/servicio no pertenece al usuario");
        }

        aEliminar.setActivo("0");
        repository.save(aEliminar);
        logger.info("Producto/servicio eliminado exitosamente");
    }

    /**
     * Obtiene un producto/servicio por ID
     */
    public Optional<CatalogoProductoServicio> obtenerPorId(Long idUsuario, Long id) {
        Optional<CatalogoProductoServicio> catalogo = repository.findById(id);
        if (catalogo.isPresent() && catalogo.get().getIdUsuario().equals(idUsuario) && catalogo.get().isActivo()) {
            return catalogo;
        }
        return Optional.empty();
    }
}

