package com.fooddelivery.integration.partner.repository;

import com.fooddelivery.integration.partner.entity.PartnerKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartnerKeyRepository extends JpaRepository<PartnerKey, Long> {

    Optional<PartnerKey> findByKeyId(String keyId);

    List<PartnerKey> findByPartnerIdOrderByCreatedAtDesc(Long partnerId);

    /**
     * Bulk UPDATE rather than save(): this runs on partner traffic, and loading
     * the entity to set one timestamp would put an optimistic-lock bump and a
     * full row write in the path of every request.
     */
    @Modifying
    @Query("UPDATE PartnerKey k SET k.lastUsedAt = :now " +
            "WHERE k.id = :id AND (k.lastUsedAt IS NULL OR k.lastUsedAt < :threshold)")
    int touchLastUsed(@Param("id") Long id,
                      @Param("now") LocalDateTime now,
                      @Param("threshold") LocalDateTime threshold);
}
