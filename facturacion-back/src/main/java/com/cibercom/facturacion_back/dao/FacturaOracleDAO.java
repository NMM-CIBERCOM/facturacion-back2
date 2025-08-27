package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.FacturaFrontendRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("oracle")
public class FacturaOracleDAO implements FacturaDAO {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    @Override
    public void guardarFactura(FacturaFrontendRequest factura) {
        em.createStoredProcedureQuery("SP_INSERTA_FACTURA")
            .registerStoredProcedureParameter("p_rfc", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_correo", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_razon", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_nombre", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_paterno", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_materno", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_pais", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_no_id", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_domicilio", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_regimen", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_uso_cfdi", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_codigo", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_tienda", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_fecha", java.sql.Date.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_terminal", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_boleta", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_medio_pago", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_forma_pago", String.class, jakarta.persistence.ParameterMode.IN)
            .registerStoredProcedureParameter("p_ieps", Integer.class, jakarta.persistence.ParameterMode.IN)

            .setParameter("p_rfc", factura.getRfc())
            .setParameter("p_correo", factura.getCorreoElectronico())
            .setParameter("p_razon", factura.getRazonSocial())
            .setParameter("p_nombre", factura.getNombre())
            .setParameter("p_paterno", factura.getPaterno())
            .setParameter("p_materno", factura.getMaterno())
            .setParameter("p_pais", factura.getPais())
            .setParameter("p_no_id", factura.getNoRegistroIdentidadTributaria())
            .setParameter("p_domicilio", factura.getDomicilioFiscal())
            .setParameter("p_regimen", factura.getRegimenFiscal())
            .setParameter("p_uso_cfdi", factura.getUsoCfdi())
            .setParameter("p_codigo", factura.getCodigoFacturacion())
            .setParameter("p_tienda", factura.getTienda())
            .setParameter("p_fecha", factura.getFecha() != null ? java.sql.Date.valueOf(factura.getFecha()) : null)
            .setParameter("p_terminal", factura.getTerminal())
            .setParameter("p_boleta", factura.getBoleta())
            .setParameter("p_medio_pago", factura.getMedioPago())
            .setParameter("p_forma_pago", factura.getFormaPago())
            .setParameter("p_ieps", factura.getIepsDesglosado() != null && factura.getIepsDesglosado() ? 1 : 0)
            .execute();
    }
}
