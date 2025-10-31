package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.Justificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JustificacionRepository extends JpaRepository<Justificacion, Long> {
}