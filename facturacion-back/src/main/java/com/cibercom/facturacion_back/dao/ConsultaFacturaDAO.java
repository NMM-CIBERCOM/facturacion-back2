package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.ConsultaFacturaRequest;
import com.cibercom.facturacion_back.dto.ConsultaFacturaResponse.FacturaConsultaDTO;
import com.cibercom.facturacion_back.dto.CancelFacturaRequest;
import java.util.List;

public interface ConsultaFacturaDAO {

    List<FacturaConsultaDTO> buscarFacturas(ConsultaFacturaRequest request);

    boolean cancelarFactura(CancelFacturaRequest request);

    FacturaInfo obtenerFacturaPorUuid(String uuid);

    boolean marcarEnProceso(String uuid);

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
