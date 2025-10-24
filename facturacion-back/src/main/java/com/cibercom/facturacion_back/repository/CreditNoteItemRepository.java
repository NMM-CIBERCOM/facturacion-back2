package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.CreditNoteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditNoteItemRepository extends JpaRepository<CreditNoteItem, Long> {
}