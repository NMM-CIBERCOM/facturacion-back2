/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.FacturaDAO;
import com.cibercom.facturacion_back.dto.FacturaFrontendRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FacturaCoordinadorService {

    private final FacturaDAO facturaDAO;

    @Autowired
    public FacturaCoordinadorService(FacturaDAO facturaDAO) {
        this.facturaDAO = facturaDAO;
    }

    public void guardarFactura(FacturaFrontendRequest dto) {
        facturaDAO.guardarFactura(dto);
    }
}


