package com.cibercom.facturacion_back.dto;

// Lombok annotations retained, but manual code added for reliability
// import lombok.Builder; // removed to avoid conflict with manual Builder
import lombok.Data;

import java.util.List;

@Data
// @Builder // removed to avoid conflict with manual Builder
public class BuscarReceptorResponse {
    private boolean encontrado;
    private boolean completoCFDI40;
    private String idReceptor;
    private Receptor receptor;
    private List<String> faltantes;

    // Explicit getters/setters
    public boolean isEncontrado() { return encontrado; }
    public void setEncontrado(boolean encontrado) { this.encontrado = encontrado; }

    public boolean isCompletoCFDI40() { return completoCFDI40; }
    public void setCompletoCFDI40(boolean completoCFDI40) { this.completoCFDI40 = completoCFDI40; }

    public String getIdReceptor() { return idReceptor; }
    public void setIdReceptor(String idReceptor) { this.idReceptor = idReceptor; }

    public Receptor getReceptor() { return receptor; }
    public void setReceptor(Receptor receptor) { this.receptor = receptor; }

    public List<String> getFaltantes() { return faltantes; }
    public void setFaltantes(List<String> faltantes) { this.faltantes = faltantes; }

    // Manual builder for outer class
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private boolean encontrado;
        private boolean completoCFDI40;
        private String idReceptor;
        private Receptor receptor;
        private List<String> faltantes;

        public Builder encontrado(boolean encontrado) { this.encontrado = encontrado; return this; }
        public Builder completoCFDI40(boolean completoCFDI40) { this.completoCFDI40 = completoCFDI40; return this; }
        public Builder idReceptor(String idReceptor) { this.idReceptor = idReceptor; return this; }
        public Builder receptor(Receptor receptor) { this.receptor = receptor; return this; }
        public Builder faltantes(List<String> faltantes) { this.faltantes = faltantes; return this; }

        public BuscarReceptorResponse build() {
            BuscarReceptorResponse r = new BuscarReceptorResponse();
            r.setEncontrado(this.encontrado);
            r.setCompletoCFDI40(this.completoCFDI40);
            r.setIdReceptor(this.idReceptor);
            r.setReceptor(this.receptor);
            r.setFaltantes(this.faltantes);
            return r;
        }
    }

    @Data
    // @Builder // removed to avoid conflict with manual Builder
    public static class Receptor {
        private String rfc;
        private String razonSocial;
        private String nombre;
        private String paterno;
        private String materno;
        private String pais;
        private String domicilioFiscal;
        private String regimenFiscal;
        private String usoCfdi;

        // Explicit getters/setters
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

        // Manual builder for nested class
        public static Receptor.Builder builder() { return new Receptor.Builder(); }
        public static class Builder {
            private String rfc;
            private String razonSocial;
            private String nombre;
            private String paterno;
            private String materno;
            private String pais;
            private String domicilioFiscal;
            private String regimenFiscal;
            private String usoCfdi;

            public Builder rfc(String rfc) { this.rfc = rfc; return this; }
            public Builder razonSocial(String razonSocial) { this.razonSocial = razonSocial; return this; }
            public Builder nombre(String nombre) { this.nombre = nombre; return this; }
            public Builder paterno(String paterno) { this.paterno = paterno; return this; }
            public Builder materno(String materno) { this.materno = materno; return this; }
            public Builder pais(String pais) { this.pais = pais; return this; }
            public Builder domicilioFiscal(String domicilioFiscal) { this.domicilioFiscal = domicilioFiscal; return this; }
            public Builder regimenFiscal(String regimenFiscal) { this.regimenFiscal = regimenFiscal; return this; }
            public Builder usoCfdi(String usoCfdi) { this.usoCfdi = usoCfdi; return this; }

            public Receptor build() {
                Receptor r = new Receptor();
                r.setRfc(this.rfc);
                r.setRazonSocial(this.razonSocial);
                r.setNombre(this.nombre);
                r.setPaterno(this.paterno);
                r.setMaterno(this.materno);
                r.setPais(this.pais);
                r.setDomicilioFiscal(this.domicilioFiscal);
                r.setRegimenFiscal(this.regimenFiscal);
                r.setUsoCfdi(this.usoCfdi);
                return r;
            }
        }
    }
}