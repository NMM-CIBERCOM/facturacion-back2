package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.TicketDAO;
import com.cibercom.facturacion_back.dto.TicketDto;
import com.cibercom.facturacion_back.dto.TicketSearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    @Autowired(required = false)
    private TicketDAO ticketDAO;

    public List<TicketDto> buscarTickets(TicketSearchRequest request) {
        if (ticketDAO == null) {
            throw new IllegalStateException("No hay implementación de TicketDAO disponible (perfil Oracle no activo)");
        }
        return ticketDAO.buscar(request);
    }

    /**
     * Enlaza un ticket con una factura actualizando TICKETS.ID_FACTURA según filtros.
     */
    public boolean enlazarTicketConFactura(TicketSearchRequest filtros, Long idFactura) {
        if (ticketDAO == null) {
            throw new IllegalStateException("No hay implementación de TicketDAO disponible (perfil Oracle no activo)");
        }
        return ticketDAO.actualizarIdFacturaPorFiltros(filtros, idFactura);
    }

    /**
     * Busca tickets vinculados a una factura por su ID_FACTURA.
     */
    public java.util.List<TicketDto> buscarTicketsPorIdFactura(Long idFactura) {
        if (ticketDAO == null) {
            throw new IllegalStateException("No hay implementación de TicketDAO disponible (perfil Oracle no activo)");
        }
        return ticketDAO.buscarPorIdFactura(idFactura);
    }
}