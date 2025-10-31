package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, String> {

    Optional<CreditNote> findByUuidNc(String uuidNc);

    Optional<CreditNote> findByUniqueKey(String uniqueKey);

    List<CreditNote> findByFechaEmisionBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT cn FROM CreditNote cn JOIN cn.links l WHERE l.uuidFacturaOrigen = :uuidFactura")
    List<CreditNote> findByUuidFacturaOrigen(@Param("uuidFactura") String uuidFactura);
}