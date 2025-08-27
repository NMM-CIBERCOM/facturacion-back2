package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.ConsultaFacturaRequest;
import com.cibercom.facturacion_back.dto.ConsultaFacturaResponse.FacturaConsultaDTO;
import com.cibercom.facturacion_back.dto.CancelFacturaRequest;
import java.util.List;

public interface ConsultaFacturaDAO {
    
    /**
     * Busca facturas en la base de datos según los criterios especificados
     * Utiliza el stored procedure FEE_UTIL_PCK.buscaFacturas
     */
    List<FacturaConsultaDTO> buscarFacturas(ConsultaFacturaRequest request);

    /**
     * Cancela una factura por UUID si cumple las reglas (estado y periodo permitido)
     * Devuelve true si se actualizó a CANCELADA, false en caso contrario
     */
    boolean cancelarFactura(CancelFacturaRequest request);

    /**
     * Obtiene información mínima de la factura por UUID para integraciones (PAC).
     */
    FacturaInfo obtenerFacturaPorUuid(String uuid);

    /** Marca la factura como EN_PROCESO en BD. */
    boolean marcarEnProceso(String uuid);

    /** Actualiza ESTADO de la factura a un valor dado (CANCELADA/RECHAZADA). */
    boolean actualizarEstado(String uuid, String estado);

    class FacturaInfo {
        public String uuid;
        public String rfcEmisor;
        public String rfcReceptor;
        public java.time.OffsetDateTime fechaFactura;
        public java.math.BigDecimal total;
        public String serie;
        public String folio;
        public String tienda;
        public String estatus;
    }
}
