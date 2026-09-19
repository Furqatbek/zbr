package com.fooddelivery.integration.partner.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.entity.PartnerKey;
import com.fooddelivery.integration.partner.repository.PartnerKeyRepository;
import com.fooddelivery.integration.partner.repository.PartnerRepository;
import com.fooddelivery.integration.partner.security.PartnerPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Issuing, revoking and verifying partner credentials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerKeyService {

    private final PartnerRepository partnerRepository;
    private final PartnerKeyRepository keyRepository;

    /**
     * Which deployment this is. A key is stamped with the environment it was
     * issued for and refused anywhere else, so a staging credential cannot
     * print a test order in a real kitchen — the one integration mistake whose
     * cost lands on someone outside the company.
     */
    @Value("${app.integration.partner.environment:production}")
    private String environment;

    /** How stale a key's last-used timestamp may get before we write it again. */
    @Value("${app.integration.partner.last-used-write-interval-seconds:300}")
    private long lastUsedWriteIntervalSeconds;

    @Transactional
    public Partner registerPartner(String code, String name) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        if (normalized.isEmpty()) {
            throw new BusinessException("Partner code is required");
        }
        if (partnerRepository.existsByCode(normalized)) {
            throw new BusinessException("Partner already exists: " + normalized);
        }
        Partner partner = partnerRepository.save(Partner.builder()
                .code(normalized)
                .name(name != null && !name.isBlank() ? name : normalized)
                .active(true)
                .build());
        log.info("Registered partner {} ({})", partner.getCode(), partner.getId());
        return partner;
    }

    /**
     * Issue a key. The returned secret is the ONLY time the full credential
     * exists outside the caller's hands — it is not recoverable afterwards, by
     * us or by anyone who reaches the database.
     */
    @Transactional
    public IssuedKey issueKey(Long partnerId, String label) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", "id", partnerId));

        PartnerCredentials.Generated generated = PartnerCredentials.generate();
        PartnerKey key = keyRepository.save(PartnerKey.builder()
                .partner(partner)
                .keyId(generated.keyId())
                .secretHash(generated.secretHash())
                .environment(environment)
                .label(label)
                .active(true)
                .build());

        log.info("Issued partner key {} for {} in {}", key.getKeyId(), partner.getCode(), environment);
        return new IssuedKey(key.getId(), key.getKeyId(), generated.presentedKey(), environment);
    }

    public record IssuedKey(Long id, String keyId, String secret, String environment) {
    }

    @Transactional
    public void revokeKey(Long keyId) {
        PartnerKey key = keyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("PartnerKey", "id", keyId));
        key.setActive(false);
        key.setRevokedAt(LocalDateTime.now());
        keyRepository.save(key);
        log.warn("Revoked partner key {} ({})", key.getKeyId(), key.getPartner().getCode());
    }

    @Transactional(readOnly = true)
    public List<PartnerKey> listKeys(Long partnerId) {
        return keyRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
    }

    /**
     * Turn a presented credential into a principal, or nothing.
     *
     * <p>Every rejection returns the same empty result and logs at debug. The
     * caller learns only that the key did not work — not whether the key id
     * exists, whether the secret was wrong, or whether the key was revoked. Each
     * of those distinctions is a probe an attacker can make.
     */
    @Transactional(readOnly = true)
    public Optional<PartnerPrincipal> authenticate(String presentedKey) {
        PartnerCredentials.Presented presented = PartnerCredentials.parse(presentedKey);
        if (presented == null) {
            return Optional.empty();
        }

        Optional<PartnerKey> found = keyRepository.findByKeyId(presented.keyId());
        if (found.isEmpty()) {
            return Optional.empty();
        }

        PartnerKey key = found.get();
        if (!PartnerCredentials.matches(presented.secret(), key.getSecretHash())) {
            log.debug("Partner key {} presented with a wrong secret", key.getKeyId());
            return Optional.empty();
        }
        if (!key.isUsable()) {
            log.debug("Partner key {} is revoked or its partner is inactive", key.getKeyId());
            return Optional.empty();
        }
        if (!environment.equals(key.getEnvironment())) {
            // Loud, because it is nearly always a deployment mistake and the
            // silent version of it is a test order in a real kitchen.
            log.warn("Partner key {} was issued for {} but presented to {}",
                    key.getKeyId(), key.getEnvironment(), environment);
            return Optional.empty();
        }

        return Optional.of(new PartnerPrincipal(key.getPartner(), key.getId()));
    }

    /**
     * Record that a key was used — at most once every few minutes per key, and
     * never at the cost of the request describing it.
     *
     * <p>Two throttles, doing different jobs. The in-memory one keeps the
     * database out of the hot path entirely, which matters because this runs on
     * every partner call. The SQL predicate is the backstop that makes it
     * correct across instances, where each process has its own map and would
     * otherwise each write once per interval.
     *
     * <p>Its own transaction, because this runs inside a filter before any
     * request transaction exists, and a failure here must not fail a partner's
     * order.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUsage(Long id) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusSeconds(lastUsedWriteIntervalSeconds);

        LocalDateTime lastWritten = lastUsedWrites.get(id);
        if (lastWritten != null && lastWritten.isAfter(threshold)) {
            return;
        }
        lastUsedWrites.put(id, now);

        try {
            keyRepository.touchLastUsed(id, now, threshold);
        } catch (Exception e) {
            // Bookkeeping. A partner's order does not fail because we could not
            // write down that their key was used.
            log.debug("Could not record usage of partner key {}: {}", id, e.toString());
        }
    }

    private final java.util.concurrent.ConcurrentHashMap<Long, LocalDateTime> lastUsedWrites =
            new java.util.concurrent.ConcurrentHashMap<>();
}
