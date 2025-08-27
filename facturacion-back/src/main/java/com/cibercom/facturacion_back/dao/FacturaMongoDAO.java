/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.FacturaFrontendRequest;
import com.cibercom.facturacion_back.mapper.FacturaMapper;
import com.cibercom.facturacion_back.model.FacturaMongo;
import com.cibercom.facturacion_back.repository.FacturaMongoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("mongo")
public class FacturaMongoDAO implements FacturaDAO {

    @Autowired
    private FacturaMongoRepository mongoRepo;

    @Autowired
    private FacturaMapper mapper;

    @Override
    public void guardarFactura(FacturaFrontendRequest factura) {
        FacturaMongo entity = mapper.toMongo(factura);
        mongoRepo.save(entity);
    }
}

