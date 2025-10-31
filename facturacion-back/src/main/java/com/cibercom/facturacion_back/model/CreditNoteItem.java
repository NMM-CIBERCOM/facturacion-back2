package com.cibercom.facturacion_back.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "CREDIT_NOTE_ITEMS", indexes = {
        @Index(name = "idx_cni_uuid_nc", columnList = "UUID_NC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditNoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UUID_NC", referencedColumnName = "UUID_NC")
    private CreditNote creditNote;

    @Column(name = "CLAVE_PROD_SERV", length = 20)
    private String claveProdServ;

    @Column(name = "DESCRIPCION", length = 300)
    private String descripcion;

    @Column(name = "CANTIDAD", precision = 15, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "VALOR_UNITARIO", precision = 15, scale = 6)
    private BigDecimal valorUnitario;

    @Column(name = "IMPORTE", precision = 15, scale = 2)
    private BigDecimal importe;

    @Column(name = "IVA_TASA", precision = 5, scale = 4)
    private BigDecimal ivaTasa;

    @Column(name = "IVA_IMPORTE", precision = 15, scale = 2)
    private BigDecimal ivaImporte;

    // Setter explícito usado por el servicio
    public void setCreditNote(CreditNote creditNote) { this.creditNote = creditNote; }

    // Getters explícitos para compatibilidad si Lombok falla
    public String getClaveProdServ() { return claveProdServ; }
    public String getDescripcion() { return descripcion; }
    public java.math.BigDecimal getCantidad() { return cantidad; }
    public java.math.BigDecimal getValorUnitario() { return valorUnitario; }
    public java.math.BigDecimal getImporte() { return importe; }
    public java.math.BigDecimal getIvaTasa() { return ivaTasa; }
    public java.math.BigDecimal getIvaImporte() { return ivaImporte; }

    // Builder manual para compatibilidad cuando Lombok no esté activo
    public static CreditNoteItemBuilder builder() { return new CreditNoteItemBuilder(); }
    public static class CreditNoteItemBuilder {
        private final CreditNoteItem i = new CreditNoteItem();
        public CreditNoteItemBuilder claveProdServ(String claveProdServ) { i.claveProdServ = claveProdServ; return this; }
        public CreditNoteItemBuilder descripcion(String descripcion) { i.descripcion = descripcion; return this; }
        public CreditNoteItemBuilder cantidad(java.math.BigDecimal cantidad) { i.cantidad = cantidad; return this; }
        public CreditNoteItemBuilder valorUnitario(java.math.BigDecimal valorUnitario) { i.valorUnitario = valorUnitario; return this; }
        public CreditNoteItemBuilder importe(java.math.BigDecimal importe) { i.importe = importe; return this; }
        public CreditNoteItemBuilder ivaTasa(java.math.BigDecimal ivaTasa) { i.ivaTasa = ivaTasa; return this; }
        public CreditNoteItemBuilder ivaImporte(java.math.BigDecimal ivaImporte) { i.ivaImporte = ivaImporte; return this; }
        public CreditNoteItem build() { return i; }
    }
}