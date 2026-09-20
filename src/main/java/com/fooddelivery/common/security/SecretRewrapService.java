package com.fooddelivery.common.security;

import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.repository.PartnerRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Re-encrypts stored secrets under the current primary key.
 *
 * <p>The second half of a key rotation. Adding a new primary and keeping the
 * old one readable is enough to keep working; this is what lets the old key
 * then be deleted, which is the point of rotating in the first place. A
 * rotation that never finishes is a key you have to keep forever.
 *
 * <p>Safe to run repeatedly: a value already under the primary key is skipped,
 * so the useful signal is {@code remaining == 0} rather than a count of work
 * done.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecretRewrapService {

    private final PartnerRepository partnerRepository;
    private final SecretCipher cipher;

    @Data
    @Builder
    public static class RewrapResult {
        private String primaryKeyId;
        private int examined;
        private int rewrapped;
        /** Values still not under the primary key. Zero means the old key can go. */
        private int remaining;
        @Builder.Default
        private List<String> failures = new ArrayList<>();
    }

    @Transactional
    public RewrapResult rewrapPartnerCredentials() {
        if (!cipher.isAvailable()) {
            throw new IllegalStateException("Cannot rewrap: app.security.secret-key is not configured.");
        }

        List<Partner> partners = partnerRepository.findAllWithOutboundCredential();
        RewrapResult result = RewrapResult.builder()
                .primaryKeyId(cipher.primaryKeyId())
                .examined(partners.size())
                .failures(new ArrayList<>())
                .build();

        for (Partner partner : partners) {
            try {
                // Reading the entity already decrypted it, with whichever key
                // wrote it. Encrypting again writes it under the primary.
                String plaintext = partner.getOutboundApiKey();
                if (plaintext == null || plaintext.isBlank()) {
                    continue;
                }
                partnerRepository.writeOutboundApiKey(partner.getId(), cipher.encrypt(plaintext));
                result.setRewrapped(result.getRewrapped() + 1);
                log.info("Rewrapped {}'s outbound credential under key {}",
                        partner.getCode(), cipher.primaryKeyId());
            } catch (Exception e) {
                // One unreadable partner must not stop the rest: the whole
                // purpose is to get the remaining count to zero, and a partner
                // whose original key is genuinely lost needs a person, not a
                // retry. Never the secret, and never the message.
                result.getFailures().add(partner.getCode() + ": " + e.getClass().getSimpleName());
                result.setRemaining(result.getRemaining() + 1);
                log.error("Could not rewrap {}'s outbound credential — its key may not be in "
                        + "app.security.previous-keys. The credential must be reissued.",
                        partner.getCode());
            }
        }

        if (result.getRemaining() == 0) {
            log.info("Rewrap complete: {} credential(s) under key {}. Previous keys can now be "
                    + "removed from configuration.", result.getRewrapped(), cipher.primaryKeyId());
        }
        return result;
    }

    /** How many stored secrets are not yet under the primary key. */
    @Transactional(readOnly = true)
    public RewrapResult status() {
        List<Partner> partners = partnerRepository.findAllWithOutboundCredential();
        int stale = 0;
        for (Partner partner : partners) {
            // Compares the STORED form, so this has to read the column rather
            // than the decrypted attribute.
            String stored = partnerRepository.readStoredOutboundApiKey(partner.getId());
            if (cipher.needsRewrap(stored)) {
                stale++;
            }
        }
        return RewrapResult.builder()
                .primaryKeyId(cipher.primaryKeyId())
                .examined(partners.size())
                .rewrapped(0)
                .remaining(stale)
                .failures(new ArrayList<>())
                .build();
    }
}
