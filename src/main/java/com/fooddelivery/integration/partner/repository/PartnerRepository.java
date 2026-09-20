package com.fooddelivery.integration.partner.repository;

import com.fooddelivery.integration.partner.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartnerRepository extends JpaRepository<Partner, Long> {
    Optional<Partner> findByCode(String code);
    boolean existsByCode(String code);

    /**
     * Write an already-encrypted credential straight to the column.
     *
     * <p>Native on purpose. Re-saving the entity would not work: the converter
     * runs on the attribute, Hibernate compares attributes to decide what is
     * dirty, and a rewrap changes only the ciphertext — the plaintext is
     * identical, so JPA would see no change and write nothing.
     */
    @Modifying
    @Query(value = "UPDATE partners SET outbound_api_key = :ciphertext WHERE id = :id",
           nativeQuery = true)
    int writeOutboundApiKey(@Param("id") Long id, @Param("ciphertext") String ciphertext);

    @Query("SELECT p FROM Partner p WHERE p.outboundApiKey IS NOT NULL")
    List<Partner> findAllWithOutboundCredential();

    /**
     * The credential exactly as stored, ciphertext and all.
     *
     * <p>Native because the entity attribute is decrypted on the way out, and
     * deciding whether a value needs rewrapping is a question about the stored
     * form rather than the secret.
     */
    @Query(value = "SELECT outbound_api_key FROM partners WHERE id = :id", nativeQuery = true)
    String readStoredOutboundApiKey(@Param("id") Long id);
}
