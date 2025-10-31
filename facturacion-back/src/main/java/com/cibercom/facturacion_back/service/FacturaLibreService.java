package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.FacturaLibreDAO;
import com.cibercom.facturacion_back.dto.FacturaLibreSaveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("oracle")
public class FacturaLibreService {

    private static final Logger logger = LoggerFactory.getLogger(FacturaLibreService.class);

    @Autowired
    private FacturaLibreDAO facturaLibreDAO;

    public String guardar(FacturaLibreSaveRequest request) {
        logger.info("Guardando factura libre para RFC: {}", request.getRfc());
        
        // Validaciones básicas
        if (request.getRfc() == null || request.getRfc().trim().isEmpty()) {
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
        
        // Validaciones adicionales específicas para factura libre
        if (request.getRegimenFiscal() == null || request.getRegimenFiscal().trim().isEmpty()) {
            throw new IllegalArgumentException("Régimen fiscal es requerido");
        }
        
        if (request.getUsoCfdi() == null || request.getUsoCfdi().trim().isEmpty()) {
            throw new IllegalArgumentException("Uso CFDI es requerido");
        }
        
        if (request.getMedioPago() == null || request.getMedioPago().trim().isEmpty()) {
            throw new IllegalArgumentException("Medio de pago es requerido");
        }
        
        if (request.getFormaPago() == null || request.getFormaPago().trim().isEmpty()) {
            throw new IllegalArgumentException("Forma de pago es requerida");
        }
        
        // Validar que al menos haya un concepto
        if (request.getConcepto() == null) {
            throw new IllegalArgumentException("Concepto es requerido");
        }
        
        // Validar totales
        if (request.getSubtotal() == null) {
            throw new IllegalArgumentException("Subtotal es requerido");
        }
        
        if (request.getTotal() == null) {
            throw new IllegalArgumentException("Total es requerido");
        }
        
        try {
            String uuid = facturaLibreDAO.insertar(request);
            logger.info("Factura libre guardada exitosamente con UUID: {}", uuid);
            return uuid;
        } catch (Exception e) {
            logger.error("Error al guardar factura libre: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar factura libre: " + e.getMessage(), e);
        }
    }
}