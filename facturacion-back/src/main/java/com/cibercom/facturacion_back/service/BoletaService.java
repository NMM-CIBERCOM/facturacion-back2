package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.BoletaDto;
import com.cibercom.facturacion_back.model.Boleta;
import com.cibercom.facturacion_back.repository.BoletaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BoletaService {
    private static final Logger logger = LoggerFactory.getLogger(BoletaService.class);
    private final BoletaRepository boletaRepository;

    public BoletaService(BoletaRepository boletaRepository) {
        this.boletaRepository = boletaRepository;
    }

    @Transactional
    public BoletaDto crearBoleta(BoletaDto dto) {
        logger.info("Creando boleta: tienda={}, terminal={}, numero={}", dto.getTienda(), dto.getTerminal(), dto.getNumeroBoleta());

        if (dto.getTienda() == null || dto.getTienda().trim().isEmpty()) {
            throw new RuntimeException("La tienda es obligatoria");
        }
        if (dto.getNumeroBoleta() == null || dto.getNumeroBoleta().trim().isEmpty()) {
            throw new RuntimeException("El número de boleta es obligatorio");
        }

        if (boletaRepository.existsByTiendaAndTerminalAndNumeroBoleta(
                dto.getTienda(), dto.getTerminal(), dto.getNumeroBoleta())) {
            throw new RuntimeException("Ya existe una boleta con esos datos");
        }

        Boleta b = new Boleta();
        b.setTienda(dto.getTienda());
        b.setTerminal(dto.getTerminal());
        b.setNumeroBoleta(dto.getNumeroBoleta());
        b.setMontoTotal(dto.getMontoTotal());
        b.setEstatus(dto.getEstatus() != null ? dto.getEstatus() : "Pendiente");
        b.setFechaEmision(dto.getFechaEmision() != null ? dto.getFechaEmision() : LocalDateTime.now());

        Boleta saved = boletaRepository.save(b);
        return toDto(saved);
    }

    private BoletaDto toDto(Boleta b) {
        BoletaDto dto = new BoletaDto();
        dto.setIdBoleta(b.getIdBoleta());
        dto.setTienda(b.getTienda());
        dto.setTerminal(b.getTerminal());
        dto.setNumeroBoleta(b.getNumeroBoleta());
        dto.setFechaEmision(b.getFechaEmision());
        dto.setMontoTotal(b.getMontoTotal());
        dto.setEstatus(b.getEstatus());
        return dto;
    }
}