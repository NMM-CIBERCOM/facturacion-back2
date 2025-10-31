package com.cibercom.facturacion_back.repository;

import com.cibercom.facturacion_back.model.ClienteCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteCatalogoRepository extends JpaRepository<ClienteCatalogo, Long> {

    Optional<ClienteCatalogo> findByRfc(String rfc);

    @Query("SELECT c FROM ClienteCatalogo c WHERE UPPER(c.rfc) = UPPER(:rfc)")
    Optional<ClienteCatalogo> findByRfcIgnoreCase(@Param("rfc") String rfc);

    @Query("SELECT c FROM ClienteCatalogo c WHERE UPPER(TRIM(c.rfc)) = UPPER(TRIM(:rfc))")
    Optional<ClienteCatalogo> findByRfcNormalized(@Param("rfc") String rfc);

    // Búsqueda tolerante a prefijo de 1 carácter (numérico o cualquier), usando LIKE con '_'
    @Query("SELECT c FROM ClienteCatalogo c WHERE UPPER(TRIM(c.rfc)) = UPPER(TRIM(:rfc)) OR UPPER(TRIM(c.rfc)) LIKE CONCAT('_', UPPER(TRIM(:rfc)))")
    Optional<ClienteCatalogo> findByRfcOrPrefixed(@Param("rfc") String rfc);
}