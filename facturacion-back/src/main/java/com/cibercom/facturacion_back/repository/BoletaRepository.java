package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoletaRepository extends JpaRepository<Boleta, Long> {
    boolean existsByTiendaAndTerminalAndNumeroBoleta(String tienda, String terminal, String numeroBoleta);
}