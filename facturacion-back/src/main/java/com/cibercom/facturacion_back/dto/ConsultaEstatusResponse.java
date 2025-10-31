package com.cibercom.facturacion_back.dto;

public class ConsultaEstatusResponse {
    private boolean exitoso;
    private String mensaje;
    private String codigoEstatus;
    private String estado;
    private String esCancelable;
    private String estatusCancelacion;

    // Constructor por defecto
    public ConsultaEstatusResponse() {
    }

    // Constructor con todos los parámetros
    public ConsultaEstatusResponse(boolean exitoso, String mensaje, String codigoEstatus, 
                                 String estado, String esCancelable, String estatusCancelacion) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
        this.codigoEstatus = codigoEstatus;
        this.estado = estado;
        this.esCancelable = esCancelable;
        this.estatusCancelacion = estatusCancelacion;
    }

    // Getters y Setters
    public boolean isExitoso() {
        return exitoso;
    }

    public void setExitoso(boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getCodigoEstatus() {
        return codigoEstatus;
    }

    public void setCodigoEstatus(String codigoEstatus) {
        this.codigoEstatus = codigoEstatus;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEsCancelable() {
        return esCancelable;
    }

    public void setEsCancelable(String esCancelable) {
        this.esCancelable = esCancelable;
    }

    public String getEstatusCancelacion() {
        return estatusCancelacion;
    }

    public void setEstatusCancelacion(String estatusCancelacion) {
        this.estatusCancelacion = estatusCancelacion;
    }
    
    public static ConsultaEstatusResponse exito(String codigoEstatus, String estado, 
                                              String esCancelable, String estatusCancelacion) {
        return new ConsultaEstatusResponse(true, "Consulta de estatus exitosa", 
                                         codigoEstatus, estado, esCancelable, estatusCancelacion);
    }
    
    public static ConsultaEstatusResponse error(String mensaje) {
        return new ConsultaEstatusResponse(false, mensaje, null, null, null, null);
    }
    
    // Métodos explícitos para compatibilidad
    public static ConsultaEstatusResponseBuilder builder() {
        return new ConsultaEstatusResponseBuilder();
    }
    
    public static class ConsultaEstatusResponseBuilder {
        private boolean exitoso;
        private String mensaje;
        private String codigoEstatus;
        private String estado;
        private String esCancelable;
        private String estatusCancelacion;
        
        public ConsultaEstatusResponseBuilder exitoso(boolean exitoso) {
            this.exitoso = exitoso;
            return this;
        }
        
        public ConsultaEstatusResponseBuilder mensaje(String mensaje) {
            this.mensaje = mensaje;
            return this;
        }
        
        public ConsultaEstatusResponseBuilder codigoEstatus(String codigoEstatus) {
            this.codigoEstatus = codigoEstatus;
            return this;
        }
        
        public ConsultaEstatusResponseBuilder estado(String estado) {
            this.estado = estado;
            return this;
        }
        
        public ConsultaEstatusResponseBuilder esCancelable(String esCancelable) {
            this.esCancelable = esCancelable;
            return this;
        }
        
        public ConsultaEstatusResponseBuilder estatusCancelacion(String estatusCancelacion) {
            this.estatusCancelacion = estatusCancelacion;
            return this;
        }
        
        public ConsultaEstatusResponse build() {
            ConsultaEstatusResponse response = new ConsultaEstatusResponse();
            response.setExitoso(exitoso);
            response.setMensaje(mensaje);
            response.setCodigoEstatus(codigoEstatus);
            response.setEstado(estado);
            response.setEsCancelable(esCancelable);
            response.setEstatusCancelacion(estatusCancelacion);
            return response;
        }
    }
}