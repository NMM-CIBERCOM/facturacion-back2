package com.cibercom.facturacion_back.dto;

public class CancelFacturaRequest {
    private String uuid;
    private String motivo;
    private String usuario;
    private String perfilUsuario;
    private String uuidSustituto;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPerfilUsuario() {
        return perfilUsuario;
    }

    public void setPerfilUsuario(String perfilUsuario) {
        this.perfilUsuario = perfilUsuario;
    }

    public String getUuidSustituto() {
        return uuidSustituto;
    }

    public void setUuidSustituto(String uuidSustituto) {
        this.uuidSustituto = uuidSustituto;
    }
}



