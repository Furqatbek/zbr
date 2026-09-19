package com.fooddelivery.integration.partner.service;

import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.entity.PartnerKey;
import com.fooddelivery.integration.partner.repository.PartnerKeyRepository;
import com.fooddelivery.integration.partner.repository.PartnerRepository;
import com.fooddelivery.integration.partner.security.PartnerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * What a presented key is, and is not, allowed to authenticate.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Partner key authentication")
class PartnerKeyServiceTest {

    @Mock private PartnerRepository partnerRepository;
    @Mock private PartnerKeyRepository keyRepository;

    private PartnerKeyService service;
    private Partner partner;
    private PartnerCredentials.Generated credential;

    @BeforeEach
    void setUp() {
        service = new PartnerKeyService(partnerRepository, keyRepository);
        ReflectionTestUtils.setField(service, "environment", "production");
        ReflectionTestUtils.setField(service, "lastUsedWriteIntervalSeconds", 300L);

        partner = Partner.builder().id(7L).code("RESTOS").name("Restos").active(true).build();
        credential = PartnerCredentials.generate();
    }

    private PartnerKey storedKey() {
        return PartnerKey.builder()
                .id(1L).partner(partner)
                .keyId(credential.keyId()).secretHash(credential.secretHash())
                .environment("production").active(true)
                .build();
    }

    private void stored(PartnerKey key) {
        when(keyRepository.findByKeyId(anyString())).thenReturn(Optional.of(key));
    }

    @Test
    @DisplayName("a valid key authenticates as its partner")
    void validKeyAuthenticates() {
        stored(storedKey());

        Optional<PartnerPrincipal> principal = service.authenticate(credential.presentedKey());

        assertThat(principal).isPresent();
        assertThat(principal.get().getPartnerCode()).isEqualTo("RESTOS");
        assertThat(principal.get().getPartnerId()).isEqualTo(7L);
        assertThat(principal.get().getAuthorities())
                .extracting(a -> a.getAuthority()).containsExactly("ROLE_PARTNER");
    }

    @Test
    @DisplayName("a revoked key stops working")
    void revokedKeyRejected() {
        PartnerKey key = storedKey();
        key.setActive(false);
        key.setRevokedAt(LocalDateTime.now());
        stored(key);

        assertThat(service.authenticate(credential.presentedKey())).isEmpty();
    }

    @Test
    @DisplayName("every key of a deactivated partner stops working at once")
    void inactivePartnerRejected() {
        // Turning a partner off must not mean hunting down their keys one by
        // one — that is the lever you reach for when something is going wrong.
        partner.setActive(false);
        stored(storedKey());

        assertThat(service.authenticate(credential.presentedKey())).isEmpty();
    }

    @Test
    @DisplayName("a staging key is refused in production")
    void wrongEnvironmentRejected() {
        // The mistake this prevents is a test order printing in a real kitchen,
        // which is the one integration error whose cost lands on someone
        // outside the company.
        PartnerKey key = storedKey();
        key.setEnvironment("staging");
        stored(key);

        assertThat(service.authenticate(credential.presentedKey())).isEmpty();
    }

    @Test
    @DisplayName("the right key id with the wrong secret is refused")
    void wrongSecretRejected() {
        stored(storedKey());

        String forged = PartnerCredentials.PREFIX + "_" + credential.keyId() + "_wrongsecret";

        assertThat(service.authenticate(forged)).isEmpty();
    }

    @Test
    @DisplayName("an unknown key id is refused without a database write")
    void unknownKeyRejected() {
        when(keyRepository.findByKeyId(anyString())).thenReturn(Optional.empty());

        assertThat(service.authenticate(PartnerCredentials.generate().presentedKey())).isEmpty();
    }

    @Test
    @DisplayName("junk in the header is refused without hitting the database")
    void junkNeverReachesTheDatabase() {
        // Anyone can send anything to this header. Parsing must fail before a
        // query, or an unauthenticated caller can make us do database work.
        assertThat(service.authenticate("not-a-key")).isEmpty();
        assertThat(service.authenticate(null)).isEmpty();

        org.mockito.Mockito.verify(keyRepository, org.mockito.Mockito.never()).findByKeyId(any());
    }

    @Test
    @DisplayName("usage is written once, then throttled")
    void usageWriteIsThrottled() {
        service.recordUsage(1L);
        service.recordUsage(1L);
        service.recordUsage(1L);

        // Without the throttle this is one database write per partner request.
        org.mockito.Mockito.verify(keyRepository, org.mockito.Mockito.times(1))
                .touchLastUsed(org.mockito.ArgumentMatchers.eq(1L), any(), any());
    }

    @Test
    @DisplayName("a failed usage write never fails the request")
    void usageWriteFailureIsSwallowed() {
        when(keyRepository.touchLastUsed(any(), any(), any()))
                .thenThrow(new RuntimeException("database went away"));

        // Bookkeeping. A partner's order does not fail because we could not
        // write down that their key was used.
        service.recordUsage(42L);
    }
}
