package com.cibercom.facturacion_back.dto;

import lombok.Data;

/**
 * Respuesta simplificada de cliente para consulta por RFC
 */
@Data
public class ClienteResponse {
    private boolean encontrado;
    private Cliente cliente;

    public boolean isEncontrado() { return encontrado; }
    public void setEncontrado(boolean encontrado) { this.encontrado = encontrado; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    @Data
    public static class Cliente {
      private String rfc;
      private String razonSocial;
      private String nombre;
      private String paterno;
      private String materno;
      private String pais;
      private String domicilioFiscal;
      private String regimenFiscal;
      private String usoCfdi;

      public String getRfc() { return rfc; }
      public void setRfc(String rfc) { this.rfc = rfc; }
      public String getRazonSocial() { return razonSocial; }
      public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
      public String getNombre() { return nombre; }
      public void setNombre(String nombre) { this.nombre = nombre; }
      public String getPaterno() { return paterno; }
      public void setPaterno(String paterno) { this.paterno = paterno; }
      public String getMaterno() { return materno; }
      public void setMaterno(String materno) { this.materno = materno; }
      public String getPais() { return pais; }
      public void setPais(String pais) { this.pais = pais; }
      public String getDomicilioFiscal() { return domicilioFiscal; }
      public void setDomicilioFiscal(String domicilioFiscal) { this.domicilioFiscal = domicilioFiscal; }
      public String getRegimenFiscal() { return regimenFiscal; }
      public void setRegimenFiscal(String regimenFiscal) { this.regimenFiscal = regimenFiscal; }
      public String getUsoCfdi() { return usoCfdi; }
      public void setUsoCfdi(String usoCfdi) { this.usoCfdi = usoCfdi; }
    }
}