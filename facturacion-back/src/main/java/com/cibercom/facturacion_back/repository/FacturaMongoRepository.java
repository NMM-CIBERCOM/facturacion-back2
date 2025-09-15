/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.FacturaMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FacturaMongoRepository extends MongoRepository<FacturaMongo, String> {

    FacturaMongo findByUuid(String uuid);

    List<FacturaMongo> findByEmisorRfc(String emisorRfc);

    List<FacturaMongo> findByTienda(String tienda);

    List<FacturaMongo> findByEstado(String estado);

    List<FacturaMongo> findByFechaGeneracionBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    List<FacturaMongo> findByEmisorRfcAndTienda(String emisorRfc, String tienda);

    List<FacturaMongo> findByEmisorRfcAndTiendaAndFechaGeneracionBetween(
            String emisorRfc, String tienda, LocalDateTime fechaInicio, LocalDateTime fechaFin);

    List<FacturaMongo> findByEmisorRfcAndFechaGeneracionBetween(
            String emisorRfc, LocalDateTime fechaInicio, LocalDateTime fechaFin);

    @Query("{'emisor.rfc': ?0, 'tienda': {$regex: ?1, $options: 'i'}, 'fechaGeneracion': {$gte: ?2, $lte: ?3}}")
    List<FacturaMongo> findFacturasByCriterios(
            String emisorRfc, String tienda, LocalDateTime fechaInicio, LocalDateTime fechaFin);
}
