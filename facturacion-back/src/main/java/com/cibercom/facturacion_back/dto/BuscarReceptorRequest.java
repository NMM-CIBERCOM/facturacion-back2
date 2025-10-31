package com.cibercom.facturacion_back.dto;

import lombok.Data;

@Data
public class BuscarReceptorRequest {
    private String rfc;
    private String idTienda;
    private String correoElectronico;

    public String getRfc() { return rfc; }
    public String getIdTienda() { return idTienda; }
    public String getCorreoElectronico() { return correoElectronico; }
    public void setRfc(String rfc) { this.rfc = rfc; }
    public void setIdTienda(String idTienda) { this.idTienda = idTienda; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }
}