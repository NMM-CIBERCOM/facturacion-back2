package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.ConceptoMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptoMongoRepository extends MongoRepository<ConceptoMongo, String> {
    List<ConceptoMongo> findByUuidFactura(String uuidFactura);
    List<ConceptoMongo> findByIdFactura(Long idFactura);
}