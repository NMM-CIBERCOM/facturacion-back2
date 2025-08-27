package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.ConsultaFacturaRequest;
import com.cibercom.facturacion_back.dto.ConsultaFacturaResponse;
import com.cibercom.facturacion_back.dto.ConsultaFacturaResponse.FacturaConsultaDTO;
import com.cibercom.facturacion_back.dto.CancelFacturaRequest;
import com.cibercom.facturacion_back.dao.ConsultaFacturaDAO;
import com.cibercom.facturacion_back.integration.PacClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Service
public class ConsultaFacturaService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsultaFacturaService.class);
    
    @Autowired
    private ConsultaFacturaDAO consultaFacturaDAO;
    @Autowired
    private PacClient pacClient;
    
    /**
     * Realiza la consulta de facturas con todas las validaciones necesarias
     */
    @Transactional(readOnly = true)
    public ConsultaFacturaResponse consultarFacturas(ConsultaFacturaRequest request) {
        logger.info("Iniciando validación de consulta de facturas");
        
        try {
            // 1. Validar que al menos un campo de búsqueda esté lleno
            logger.info("Validando campos obligatorios...");
            if (!request.tieneAlMenosUnCampoLleno()) {
                logger.warn("Validación fallida: no hay campos de búsqueda llenos");
                return ConsultaFacturaResponse.error(
                    "Es necesario seleccionar RFC receptor o Nombre y Apellido Paterno o Razón Social o Almacén o Usuario o Serie"
                );
            }
            logger.info("Validación de campos obligatorios exitosa");
            
            // 2. Validar rango de fechas
            logger.info("Validando rango de fechas...");
            if (!request.rangoFechasValido()) {
                long diasMaximos = 365; // Según las especificaciones
                logger.warn("Validación fallida: rango de fechas excede {} días", diasMaximos);
                return ConsultaFacturaResponse.error(
                    "El rango máximo permitido es de " + diasMaximos + " días. Reintente"
                );
            }
            logger.info("Validación de rango de fechas exitosa");
            
            // 3. Validar formato de fechas (dd/MM/yy)
            logger.info("Validando formato de fechas...");
            if (request.getFechaInicio() != null && request.getFechaFin() != null) {
                if (!validarFormatoFechas(request.getFechaInicio(), request.getFechaFin())) {
                    logger.warn("Validación fallida: formato de fechas inválido");
                    return ConsultaFacturaResponse.error("Formato de fechas inválido. Use formato dd/MM/yy");
                }
            }
            logger.info("Validación de formato de fechas exitosa");
            
            // 4. Realizar búsqueda en base de datos
            logger.info("Iniciando búsqueda en base de datos...");
            List<FacturaConsultaDTO> facturas = consultaFacturaDAO.buscarFacturas(request);
            logger.info("Búsqueda completada. Facturas encontradas: {}", facturas.size());
            
            // 5. Determinar si cada factura permite cancelación
            logger.info("Procesando permisos de cancelación...");
            for (FacturaConsultaDTO factura : facturas) {
                factura.setPermiteCancelacion(determinarPermiteCancelacion(factura, request.getPerfilUsuario()));
                if (!factura.isPermiteCancelacion()) {
                    factura.setMotivoNoCancelacion(obtenerMotivoNoCancelacion(factura, request.getPerfilUsuario()));
                }
            }
            
            // 6. Retornar respuesta exitosa
            logger.info("Consulta completada exitosamente");
            return ConsultaFacturaResponse.exito(facturas);
            
        } catch (Exception e) {
            logger.error("Error al consultar facturas", e);
            return ConsultaFacturaResponse.error("Error al consultar facturas: " + e.getMessage());
        }
    }

    @Transactional
    public ConsultaFacturaResponse cancelarFactura(CancelFacturaRequest request) {
        logger.info("Solicitando cancelación para UUID {} por usuario {}", request.getUuid(), request.getUsuario());

        // Validaciones básicas
        if (request.getUuid() == null || request.getUuid().trim().isEmpty()) {
            return ConsultaFacturaResponse.error("UUID requerido para cancelar");
        }
        if (request.getPerfilUsuario() == null || "CONSULTA".equalsIgnoreCase(request.getPerfilUsuario())) {
            return ConsultaFacturaResponse.error("Usuario sin permisos para cancelar");
        }
        if (request.getMotivo() == null || !("01".equals(request.getMotivo()) || "02".equals(request.getMotivo()) || "03".equals(request.getMotivo()) || "04".equals(request.getMotivo()))) {
            return ConsultaFacturaResponse.error("Motivo de cancelación inválido");
        }

        // Integración PAC: solicitar cancelación tipo PAC
        // Consultar datos reales de la factura
        var info = consultaFacturaDAO.obtenerFacturaPorUuid(request.getUuid());
        if (info == null) {
            return ConsultaFacturaResponse.error("Factura no encontrada por UUID");
        }

        PacClient.PacRequest pacReq = new PacClient.PacRequest();
        pacReq.uuid = info.uuid;
        pacReq.motivo = request.getMotivo();
        pacReq.rfcEmisor = info.rfcEmisor;
        pacReq.rfcReceptor = info.rfcReceptor;
        pacReq.total = info.total != null ? info.total.doubleValue() : 0.0;
        pacReq.tipo = "INGRESO"; // si hay tipo en BD, mapearlo; placeholder
        pacReq.fechaFactura = info.fechaFactura != null ? info.fechaFactura.toString() : java.time.OffsetDateTime.now().toString();
        pacReq.publicoGeneral = Boolean.FALSE; // derivable por tienda/serie si aplica
        pacReq.tieneRelaciones = Boolean.FALSE; // TODO: consultar relaciones si existen

        var pacResp = pacClient.solicitarCancelacion(pacReq);
        if (pacResp != null && Boolean.TRUE.equals(pacResp.getOk())) {
            if ("CANCELADA".equalsIgnoreCase(pacResp.getStatus())) {
                // actualizar BD a CANCELADA
                boolean ok = consultaFacturaDAO.cancelarFactura(request);
                if (ok) return ConsultaFacturaResponse.exito(new java.util.ArrayList<>());
                return ConsultaFacturaResponse.error("PAC aprobó pero BD no actualizó");
            }
            if ("EN_PROCESO".equalsIgnoreCase(pacResp.getStatus())) {
                // marcar estado en proceso en BD
                boolean okProc = consultaFacturaDAO.marcarEnProceso(request.getUuid());
                if (!okProc) {
                    return ConsultaFacturaResponse.error("No se pudo marcar EN_PROCESO en BD");
                }
                return ConsultaFacturaResponse.exito(new java.util.ArrayList<>());
            }
            return ConsultaFacturaResponse.error(pacResp.getMessage() != null ? pacResp.getMessage() : "PAC rechazó");
        }
        return ConsultaFacturaResponse.error(pacResp != null ? pacResp.getMessage() : "Error llamando PAC");
    }
    
    /**
     * Valida el formato de fechas (dd/MM/yy)
     */
    private boolean validarFormatoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        try {
            // Las fechas ya vienen como LocalDate, solo validamos que sean válidas
            return fechaInicio != null && fechaFin != null && 
                   !fechaInicio.isAfter(fechaFin);
        } catch (Exception e) {
            logger.error("Error al validar formato de fechas", e);
            return false;
        }
    }
    
    /**
     * Determina si una factura permite cancelación según las reglas de negocio
     */
    private boolean determinarPermiteCancelacion(FacturaConsultaDTO factura, String perfilUsuario) {
        logger.debug("Evaluando permisos de cancelación para factura {} con perfil {}", factura.getUuid(), perfilUsuario);
        
        // ❌ NO se puede cancelar si:
        
        // 1. Usuario es CONSULTA
        if ("CONSULTA".equalsIgnoreCase(perfilUsuario)) {
            logger.debug("Usuario CONSULTA no puede cancelar");
            return false;
        }
        
        // 2. Fecha excede el período permitido (se valida en el DAO)
        // Esta validación se hace a nivel de base de datos
        
        // 3. No tiene reglas de periodo configuradas
        // Esta validación se hace a nivel de base de datos
        
        // 4. Es un perfil restringido
        if (esPerfilRestringido(perfilUsuario)) {
            logger.debug("Perfil restringido no puede cancelar");
            return false;
        }
        
        // 5. Estatus de la factura no permite cancelación
        if (!estatusPermiteCancelacion(factura.getEstatusFacturacion())) {
            logger.debug("Estatus de facturación no permite cancelación: {}", factura.getEstatusFacturacion());
            return false;
        }
        
        // 6. Estatus SAT no permite cancelación
        if (!estatusSatPermiteCancelacion(factura.getEstatusSat())) {
            logger.debug("Estatus SAT no permite cancelación: {}", factura.getEstatusSat());
            return false;
        }
        
        logger.debug("Factura permite cancelación");
        return true;
    }
    
    /**
     * Verifica si el perfil del usuario está restringido
     */
    private boolean esPerfilRestringido(String perfilUsuario) {
        if (perfilUsuario == null) return true;
        
        String perfil = perfilUsuario.toUpperCase();
        return "RESTRINGIDO".equals(perfil) || 
               "SIN_PERMISOS".equals(perfil) || 
               "BLOQUEADO".equals(perfil);
    }
    
    /**
     * Verifica si el estatus de facturación permite cancelación
     */
    private boolean estatusPermiteCancelacion(String estatusFacturacion) {
        if (estatusFacturacion == null) return false;
        
        String estatus = estatusFacturacion.toUpperCase();
        return "VIGENTE".equals(estatus) || 
               "ACTIVA".equals(estatus) || 
               "EMITIDA".equals(estatus);
    }
    
    /**
     * Verifica si el estatus SAT permite cancelación
     */
    private boolean estatusSatPermiteCancelacion(String estatusSat) {
        if (estatusSat == null) return false;
        
        String estatus = estatusSat.toUpperCase();
        return "VIGENTE".equals(estatus) || 
               "ACTIVA".equals(estatus) || 
               "EMITIDA".equals(estatus);
    }
    
    /**
     * Obtiene el motivo por el cual no se puede cancelar una factura
     */
    private String obtenerMotivoNoCancelacion(FacturaConsultaDTO factura, String perfilUsuario) {
        if ("CONSULTA".equalsIgnoreCase(perfilUsuario)) {
            return "Usuario con perfil CONSULTA";
        }
        
        if (esPerfilRestringido(perfilUsuario)) {
            return "Perfil de usuario restringido";
        }
        
        if (!estatusPermiteCancelacion(factura.getEstatusFacturacion())) {
            return "Estatus de facturación no permite cancelación: " + factura.getEstatusFacturacion();
        }
        
        if (!estatusSatPermiteCancelacion(factura.getEstatusSat())) {
            return "Sin reglas de periodo configuradas";
        }
        
        return "Sin reglas de periodo configuradas";
    }
}
