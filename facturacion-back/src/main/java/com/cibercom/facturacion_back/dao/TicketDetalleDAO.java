package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.TicketDetalleDto;

import java.util.List;

/**
 * DAO para consultar detalles de tickets.
 */
public interface TicketDetalleDAO {
    /**
     * Busca los detalles por ID_TICKET.
     */
    List<TicketDetalleDto> buscarPorIdTicket(Long idTicket);

    /**
     * Busca los detalles por ID_FACTURA realizando JOIN con TICKETS.
     */
    List<TicketDetalleDto> buscarPorIdFactura(Long idFactura);
}