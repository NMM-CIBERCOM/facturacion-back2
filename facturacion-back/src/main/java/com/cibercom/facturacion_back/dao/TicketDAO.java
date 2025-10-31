package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.TicketDto;
import com.cibercom.facturacion_back.dto.TicketSearchRequest;
import java.util.List;

/**
 * Contrato DAO para consultar la tabla de TICKETS en Oracle.
 */
public interface TicketDAO {
    List<TicketDto> buscar(TicketSearchRequest request);
    /**
     * Actualiza el campo ID_FACTURA del ticket identificado por los filtros dados.
     * Devuelve true si se afectó al menos una fila.
     */
    boolean actualizarIdFacturaPorFiltros(TicketSearchRequest request, Long idFactura);

    /**
     * Busca tickets vinculados a una factura específica por su ID_FACTURA.
     */
    List<TicketDto> buscarPorIdFactura(Long idFactura);
}