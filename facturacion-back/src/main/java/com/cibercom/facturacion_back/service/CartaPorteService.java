package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.CartaPorteDAO;
import com.cibercom.facturacion_back.dto.CartaPorteSaveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("oracle")
public class CartaPorteService {

    private static final Logger logger = LoggerFactory.getLogger(CartaPorteService.class);

    @Autowired
    private CartaPorteDAO cartaPorteDAO;

    public Long guardar(CartaPorteSaveRequest request) {
        logger.info("Guardando carta porte para RFC: {}", request.getRfcCompleto());
        
        // Validaciones básicas
        if (request.getRfcCompleto() == null || request.getRfcCompleto().trim().isEmpty()) {
            throw new IllegalArgumentException("RFC es requerido");
        }
        
        if (request.getCorreoElectronico() == null || request.getCorreoElectronico().trim().isEmpty()) {
            throw new IllegalArgumentException("Correo electrónico es requerido");
        }
        
        if (request.getRazonSocial() == null || request.getRazonSocial().trim().isEmpty()) {
            throw new IllegalArgumentException("Razón social es requerida");
        }
        
        if (request.getDomicilioFiscal() == null || request.getDomicilioFiscal().trim().isEmpty()) {
            throw new IllegalArgumentException("Domicilio fiscal es requerido");
        }
        
        if (request.getRegimenFiscal() == null || request.getRegimenFiscal().trim().isEmpty()) {
            throw new IllegalArgumentException("Régimen fiscal es requerido");
        }
        
        if (request.getUsoCfdi() == null || request.getUsoCfdi().trim().isEmpty()) {
            throw new IllegalArgumentException("Uso CFDI es requerido");
        }
        
        if (request.getDescripcion() == null || request.getDescripcion().trim().isEmpty()) {
            throw new IllegalArgumentException("Descripción es requerida");
        }
        
        if (request.getNumeroSerie() == null || request.getNumeroSerie().trim().isEmpty()) {
            throw new IllegalArgumentException("Número de serie es requerido");
        }
        
        if (request.getPrecio() == null || request.getPrecio().trim().isEmpty()) {
            throw new IllegalArgumentException("Precio es requerido");
        }
        
        if (request.getPersonaAutoriza() == null || request.getPersonaAutoriza().trim().isEmpty()) {
            throw new IllegalArgumentException("Persona que autoriza es requerida");
        }
        
        if (request.getPuesto() == null || request.getPuesto().trim().isEmpty()) {
            throw new IllegalArgumentException("Puesto es requerido");
        }
        
        try {
            Long idGenerado = cartaPorteDAO.insertar(request);
            logger.info("Carta porte guardada exitosamente con ID: {}", idGenerado);
            return idGenerado;
        } catch (Exception e) {
            logger.error("Error al guardar carta porte: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar carta porte: " + e.getMessage(), e);
        }
    }
}