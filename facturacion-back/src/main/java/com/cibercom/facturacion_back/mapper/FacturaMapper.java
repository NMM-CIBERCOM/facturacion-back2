/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cibercom.facturacion_back.mapper;

import com.cibercom.facturacion_back.dto.FacturaFrontendRequest;
import com.cibercom.facturacion_back.model.FacturaMongo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class FacturaMapper {

    /**
     * Mapea FacturaFrontendRequest a FacturaMongo
     */
    public FacturaMongo toMongo(FacturaFrontendRequest request) {
        // Crear mapas para emisor y receptor
        Map<String, Object> emisor = new HashMap<>();
        emisor.put("rfc", request.getRfc());
        emisor.put("razonSocial", request.getRazonSocial());
        emisor.put("nombre", request.getNombre());
        emisor.put("paterno", request.getPaterno());
        emisor.put("materno", request.getMaterno());
        emisor.put("correo", request.getCorreoElectronico());
        emisor.put("pais", request.getPais());
        emisor.put("domicilioFiscal", request.getDomicilioFiscal());
        emisor.put("regimenFiscal", request.getRegimenFiscal());
        
        Map<String, Object> receptor = new HashMap<>();
        receptor.put("rfc", request.getRfc());
        receptor.put("razonSocial", request.getRazonSocial());
        receptor.put("nombre", request.getNombre());
        receptor.put("paterno", request.getPaterno());
        receptor.put("materno", request.getMaterno());
        receptor.put("correo", request.getCorreoElectronico());
        receptor.put("pais", request.getPais());
        receptor.put("domicilioFiscal", request.getDomicilioFiscal());
        receptor.put("regimenFiscal", request.getRegimenFiscal());
        receptor.put("usoCfdi", request.getUsoCfdi());
        
        // Crear FacturaMongo usando el builder
        return FacturaMongo.builder()
                .uuid(java.util.UUID.randomUUID().toString().toUpperCase())
                .xmlContent("") // Se llenará después
                .fechaGeneracion(LocalDateTime.now())
                .fechaTimbrado(LocalDateTime.now())
                .estado("GENERADA")
                .serie("A")
                .folio("1")
                .cadenaOriginal("")
                .selloDigital("")
                .certificado("")
                .emisor(emisor)
                .receptor(receptor)
                .codigoFacturacion(request.getCodigoFacturacion())
                .tienda(request.getTienda())
                .fechaFactura(LocalDateTime.now())
                .terminal(request.getTerminal())
                .boleta(request.getBoleta())
                .medioPago(request.getMedioPago())
                .formaPago(request.getFormaPago())
                .iepsDesglosado(request.getIepsDesglosado())
                .subtotal(BigDecimal.ZERO) // Se calculará después
                .iva(BigDecimal.ZERO) // Se calculará después
                .ieps(BigDecimal.ZERO)
                .total(BigDecimal.ZERO) // Se calculará después
                .build();
    }
    
    /**
     * Mapea datos básicos del frontend a FacturaMongo
     * Versión simplificada para casos básicos
     */
    public FacturaMongo toMongoSimple(FacturaFrontendRequest request) {
        Map<String, Object> emisor = new HashMap<>();
        emisor.put("rfc", request.getRfc());
        emisor.put("razonSocial", request.getRazonSocial());
        emisor.put("domicilioFiscal", request.getDomicilioFiscal());
        emisor.put("regimenFiscal", request.getRegimenFiscal());
        
        Map<String, Object> receptor = new HashMap<>();
        receptor.put("rfc", request.getRfc());
        receptor.put("razonSocial", request.getRazonSocial());
        receptor.put("domicilioFiscal", request.getDomicilioFiscal());
        receptor.put("regimenFiscal", request.getRegimenFiscal());
        receptor.put("usoCfdi", request.getUsoCfdi());
        
        return FacturaMongo.builder()
                .uuid(java.util.UUID.randomUUID().toString().toUpperCase())
                .estado("GENERADA")
                .emisor(emisor)
                .receptor(receptor)
                .codigoFacturacion(request.getCodigoFacturacion())
                .tienda(request.getTienda())
                .fechaFactura(LocalDateTime.now())
                .build();
    }
}
