package com.cibercom.facturacion_back.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CfdiConsultaResponse {
    private boolean exitoso;
    private String mensaje;
    private String uuid;
    private String estado; // VIGENTE / CANCELADO / DESCONOCIDO
    private String rfcReceptor;

    private Basicos basicos;
    private Relacionados relacionados;
    private Pago pago;

    // Constructor sin argumentos para compatibilidad con el builder manual
    public CfdiConsultaResponse() {}

    // Getters/Setters explícitos para confiabilidad si Lombok no está activo
    public boolean isExitoso() { return exitoso; }
    public void setExitoso(boolean exitoso) { this.exitoso = exitoso; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getRfcReceptor() { return rfcReceptor; }
    public void setRfcReceptor(String rfcReceptor) { this.rfcReceptor = rfcReceptor; }
    public Basicos getBasicos() { return basicos; }
    public void setBasicos(Basicos basicos) { this.basicos = basicos; }
    public Relacionados getRelacionados() { return relacionados; }
    public void setRelacionados(Relacionados relacionados) { this.relacionados = relacionados; }
    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }

    // Builder manual para compatibilidad cuando Lombok falle
    public static CfdiConsultaResponseBuilder builder() { return new CfdiConsultaResponseBuilder(); }
    public static class CfdiConsultaResponseBuilder {
        private final CfdiConsultaResponse r = new CfdiConsultaResponse();
        public CfdiConsultaResponseBuilder exitoso(boolean exitoso) { r.exitoso = exitoso; return this; }
        public CfdiConsultaResponseBuilder mensaje(String mensaje) { r.mensaje = mensaje; return this; }
        public CfdiConsultaResponseBuilder uuid(String uuid) { r.uuid = uuid; return this; }
        public CfdiConsultaResponseBuilder estado(String estado) { r.estado = estado; return this; }
        public CfdiConsultaResponseBuilder rfcReceptor(String rfcReceptor) { r.rfcReceptor = rfcReceptor; return this; }
        public CfdiConsultaResponseBuilder basicos(Basicos basicos) { r.basicos = basicos; return this; }
        public CfdiConsultaResponseBuilder relacionados(Relacionados relacionados) { r.relacionados = relacionados; return this; }
        public CfdiConsultaResponseBuilder pago(Pago pago) { r.pago = pago; return this; }
        public CfdiConsultaResponse build() { return r; }
    }
    @Data
    @Builder
    public static class Basicos {
        private String serie;
        private String folio;
        private BigDecimal subtotal;
        private BigDecimal descuento;
        private BigDecimal iva;
        private BigDecimal ieps;
        private BigDecimal total;
        private String metodoPago;
        private String formaPago;
        private String usoCfdi;
        // Nuevos: desglose de IVA e IEPS
        private BigDecimal iva16;
        private BigDecimal iva8;
        private BigDecimal iva0;
        private BigDecimal ivaExento;
        private BigDecimal ieps26;
        private BigDecimal ieps160;
        private BigDecimal ieps8;
        private BigDecimal ieps30;
        private BigDecimal ieps304;
        private BigDecimal ieps7;
        private BigDecimal ieps53;
        private BigDecimal ieps25;
        private BigDecimal ieps6;
        private BigDecimal ieps50;
        private BigDecimal ieps9;
        private BigDecimal ieps3;
        private BigDecimal ieps43;

        // Constructor sin argumentos para compatibilidad
        public Basicos() {}
        // Getters/Setters explícitos para confiabilidad si Lombok no está activo
        public String getSerie() { return serie; }
        public void setSerie(String serie) { this.serie = serie; }
        public String getFolio() { return folio; }
        public void setFolio(String folio) { this.folio = folio; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
        public BigDecimal getDescuento() { return descuento; }
        public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }
        public BigDecimal getIva() { return iva; }
        public void setIva(BigDecimal iva) { this.iva = iva; }
        public BigDecimal getIeps() { return ieps; }
        public void setIeps(BigDecimal ieps) { this.ieps = ieps; }
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
        public String getMetodoPago() { return metodoPago; }
        public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
        public String getFormaPago() { return formaPago; }
        public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
        public String getUsoCfdi() { return usoCfdi; }
        public void setUsoCfdi(String usoCfdi) { this.usoCfdi = usoCfdi; }
        public BigDecimal getIva16() { return iva16; }
        public void setIva16(BigDecimal iva16) { this.iva16 = iva16; }
        public BigDecimal getIva8() { return iva8; }
        public void setIva8(BigDecimal iva8) { this.iva8 = iva8; }
        public BigDecimal getIva0() { return iva0; }
        public void setIva0(BigDecimal iva0) { this.iva0 = iva0; }
        public BigDecimal getIvaExento() { return ivaExento; }
        public void setIvaExento(BigDecimal ivaExento) { this.ivaExento = ivaExento; }
        public BigDecimal getIeps26() { return ieps26; }
        public void setIeps26(BigDecimal ieps26) { this.ieps26 = ieps26; }
        public BigDecimal getIeps160() { return ieps160; }
        public void setIeps160(BigDecimal ieps160) { this.ieps160 = ieps160; }
        public BigDecimal getIeps8() { return ieps8; }
        public void setIeps8(BigDecimal ieps8) { this.ieps8 = ieps8; }
        public BigDecimal getIeps30() { return ieps30; }
        public void setIeps30(BigDecimal ieps30) { this.ieps30 = ieps30; }
        public BigDecimal getIeps304() { return ieps304; }
        public void setIeps304(BigDecimal ieps304) { this.ieps304 = ieps304; }
        public BigDecimal getIeps7() { return ieps7; }
        public void setIeps7(BigDecimal ieps7) { this.ieps7 = ieps7; }
        public BigDecimal getIeps53() { return ieps53; }
        public void setIeps53(BigDecimal ieps53) { this.ieps53 = ieps53; }
        public BigDecimal getIeps25() { return ieps25; }
        public void setIeps25(BigDecimal ieps25) { this.ieps25 = ieps25; }
        public BigDecimal getIeps6() { return ieps6; }
        public void setIeps6(BigDecimal ieps6) { this.ieps6 = ieps6; }
        public BigDecimal getIeps50() { return ieps50; }
        public void setIeps50(BigDecimal ieps50) { this.ieps50 = ieps50; }
        public BigDecimal getIeps9() { return ieps9; }
        public void setIeps9(BigDecimal ieps9) { this.ieps9 = ieps9; }
        public BigDecimal getIeps3() { return ieps3; }
        public void setIeps3(BigDecimal ieps3) { this.ieps3 = ieps3; }
        public BigDecimal getIeps43() { return ieps43; }

        // Builder manual para compatibilidad
        public static BasicosBuilder builder() { return new BasicosBuilder(); }
        public static class BasicosBuilder {
            private final Basicos b = new Basicos();
            public BasicosBuilder serie(String serie) { b.serie = serie; return this; }
            public BasicosBuilder folio(String folio) { b.folio = folio; return this; }
            public BasicosBuilder subtotal(BigDecimal subtotal) { b.subtotal = subtotal; return this; }
            public BasicosBuilder descuento(BigDecimal descuento) { b.descuento = descuento; return this; }
            public BasicosBuilder iva(BigDecimal iva) { b.iva = iva; return this; }
            public BasicosBuilder ieps(BigDecimal ieps) { b.ieps = ieps; return this; }
            public BasicosBuilder total(BigDecimal total) { b.total = total; return this; }
            public BasicosBuilder metodoPago(String metodoPago) { b.metodoPago = metodoPago; return this; }
            public BasicosBuilder formaPago(String formaPago) { b.formaPago = formaPago; return this; }
            public BasicosBuilder usoCfdi(String usoCfdi) { b.usoCfdi = usoCfdi; return this; }
            public BasicosBuilder iva16(BigDecimal iva16) { b.iva16 = iva16; return this; }
            public BasicosBuilder iva8(BigDecimal iva8) { b.iva8 = iva8; return this; }
            public BasicosBuilder iva0(BigDecimal iva0) { b.iva0 = iva0; return this; }
            public BasicosBuilder ivaExento(BigDecimal ivaExento) { b.ivaExento = ivaExento; return this; }
            public BasicosBuilder ieps26(BigDecimal ieps26) { b.ieps26 = ieps26; return this; }
            public BasicosBuilder ieps160(BigDecimal ieps160) { b.ieps160 = ieps160; return this; }
            public BasicosBuilder ieps8(BigDecimal ieps8) { b.ieps8 = ieps8; return this; }
            public BasicosBuilder ieps30(BigDecimal ieps30) { b.ieps30 = ieps30; return this; }
            public BasicosBuilder ieps304(BigDecimal ieps304) { b.ieps304 = ieps304; return this; }
            public BasicosBuilder ieps7(BigDecimal ieps7) { b.ieps7 = ieps7; return this; }
            public BasicosBuilder ieps53(BigDecimal ieps53) { b.ieps53 = ieps53; return this; }
            public BasicosBuilder ieps25(BigDecimal ieps25) { b.ieps25 = ieps25; return this; }
            public BasicosBuilder ieps6(BigDecimal ieps6) { b.ieps6 = ieps6; return this; }
            public BasicosBuilder ieps50(BigDecimal ieps50) { b.ieps50 = ieps50; return this; }
            public BasicosBuilder ieps9(BigDecimal ieps9) { b.ieps9 = ieps9; return this; }
            public BasicosBuilder ieps3(BigDecimal ieps3) { b.ieps3 = ieps3; return this; }
            public BasicosBuilder ieps43(BigDecimal ieps43) { b.ieps43 = ieps43; return this; }
            public Basicos build() { return b; }
        }
    }

    @Data
    @Builder
    public static class Relacionados {
        private String tipoRelacion; // SAT: 01, 02, 03, 04, etc.
        private List<String> uuids;  // CfdiRelacionado UUIDs
        private String uuidOriginal; // Para sustitución (TipoRelacion=04)

        // Constructor sin argumentos para compatibilidad
        public Relacionados() {}
        // Builder manual para compatibilidad
        public static RelacionadosBuilder builder() { return new RelacionadosBuilder(); }
        public static class RelacionadosBuilder {
            private final Relacionados r = new Relacionados();
            public RelacionadosBuilder tipoRelacion(String tipoRelacion) { r.tipoRelacion = tipoRelacion; return this; }
            public RelacionadosBuilder uuids(List<String> uuids) { r.uuids = uuids; return this; }
            public RelacionadosBuilder uuidOriginal(String uuidOriginal) { r.uuidOriginal = uuidOriginal; return this; }
            public Relacionados build() { return r; }
        }
    }

    @Data
    @Builder
    public static class Pago {
        // Pago (P)
        private String formaDePagoP;
        private String fechaPago;
        private String monedaP;
        private BigDecimal monto;
        // Docto Relacionado (DR)
        private String idDocumento;
        private String serieDR;
        private String folioDR;
        private String monedaDR;
        private String metodoDePagoDR;
        private String numParcialidad;
        private BigDecimal impSaldoAnt;
        private BigDecimal impPagado;
        private BigDecimal impSaldoInsoluto;

        // Constructor sin argumentos para compatibilidad
        public Pago() {}
        // Builder manual para compatibilidad
        public static PagoBuilder builder() { return new PagoBuilder(); }
        public static class PagoBuilder {
            private final Pago p = new Pago();
            public PagoBuilder formaDePagoP(String formaDePagoP) { p.formaDePagoP = formaDePagoP; return this; }
            public PagoBuilder fechaPago(String fechaPago) { p.fechaPago = fechaPago; return this; }
            public PagoBuilder monedaP(String monedaP) { p.monedaP = monedaP; return this; }
            public PagoBuilder monto(BigDecimal monto) { p.monto = monto; return this; }
            public PagoBuilder idDocumento(String idDocumento) { p.idDocumento = idDocumento; return this; }
            public PagoBuilder serieDR(String serieDR) { p.serieDR = serieDR; return this; }
            public PagoBuilder folioDR(String folioDR) { p.folioDR = folioDR; return this; }
            public PagoBuilder monedaDR(String monedaDR) { p.monedaDR = monedaDR; return this; }
            public PagoBuilder metodoDePagoDR(String metodoDePagoDR) { p.metodoDePagoDR = metodoDePagoDR; return this; }
            public PagoBuilder numParcialidad(String numParcialidad) { p.numParcialidad = numParcialidad; return this; }
            public PagoBuilder impSaldoAnt(BigDecimal impSaldoAnt) { p.impSaldoAnt = impSaldoAnt; return this; }
            public PagoBuilder impPagado(BigDecimal impPagado) { p.impPagado = impPagado; return this; }
            public PagoBuilder impSaldoInsoluto(BigDecimal impSaldoInsoluto) { p.impSaldoInsoluto = impSaldoInsoluto; return this; }
            public Pago build() { return p; }
        }
    }
}