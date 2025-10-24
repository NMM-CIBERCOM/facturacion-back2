package com.cibercom.facturacion_back.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CREDIT_NOTE_LINKS", indexes = {
        @Index(name = "idx_cnl_uuid_nc", columnList = "UUID_NC"),
        @Index(name = "idx_cnl_uuid_origen", columnList = "UUID_FACTURA_ORIGEN")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditNoteLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UUID_NC", referencedColumnName = "UUID_NC")
    private CreditNote creditNote;

    @Column(name = "UUID_FACTURA_ORIGEN", length = 36, nullable = false)
    private String uuidFacturaOrigen;

    // Setter explícito usado por el servicio
    public void setCreditNote(CreditNote creditNote) { this.creditNote = creditNote; }

    // Getter explícito para compatibilidad cuando Lombok no esté activo
    public String getUuidFacturaOrigen() { return uuidFacturaOrigen; }

    // Builder manual para compatibilidad cuando Lombok no esté activo
    public static CreditNoteLinkBuilder builder() { return new CreditNoteLinkBuilder(); }
    public static class CreditNoteLinkBuilder {
        private final CreditNoteLink l = new CreditNoteLink();
        public CreditNoteLinkBuilder uuidFacturaOrigen(String uuidFacturaOrigen) { l.uuidFacturaOrigen = uuidFacturaOrigen; return this; }
        public CreditNoteLink build() { return l; }
    }
}