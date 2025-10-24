package com.cibercom.facturacion_back.dto;

import lombok.Data;

@Data
public class ConsultaEstatusRequest {
    private String username;
    private String password;
    private String uuid;
    private String rfcEmisor;
    private String rfcReceptor;
    private String total;

    // Getters/Setters explícitos para confiabilidad si Lombok no está activo
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getRfcEmisor() { return rfcEmisor; }
    public void setRfcEmisor(String rfcEmisor) { this.rfcEmisor = rfcEmisor; }

    public String getRfcReceptor() { return rfcReceptor; }
    public void setRfcReceptor(String rfcReceptor) { this.rfcReceptor = rfcReceptor; }

    public String getTotal() { return total; }
    public void setTotal(String total) { this.total = total; }
}