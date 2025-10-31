package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.TicketDetalleDAO;
import com.cibercom.facturacion_back.dto.TicketDetalleDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketDetalleService {

    @Autowired(required = false)
    private TicketDetalleDAO ticketDetalleDAO;

    public List<TicketDetalleDto> buscarDetallesPorIdTicket(Long idTicket) {
        if (ticketDetalleDAO == null) {
            throw new IllegalStateException("No hay implementación de TicketDetalleDAO disponible (perfil Oracle no activo)");
        }
        return ticketDetalleDAO.buscarPorIdTicket(idTicket);
    }

    public List<TicketDetalleDto> buscarDetallesPorIdFactura(Long idFactura) {
        if (ticketDetalleDAO == null) {
            throw new IllegalStateException("No hay implementación de TicketDetalleDAO disponible (perfil Oracle no activo)");
        }
        return ticketDetalleDAO.buscarPorIdFactura(idFactura);
    }
}