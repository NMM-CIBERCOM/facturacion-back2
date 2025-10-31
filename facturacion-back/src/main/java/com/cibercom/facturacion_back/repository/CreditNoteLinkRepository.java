package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.CreditNoteLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditNoteLinkRepository extends JpaRepository<CreditNoteLink, Long> {
}