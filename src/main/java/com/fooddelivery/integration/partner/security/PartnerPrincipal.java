package com.fooddelivery.integration.partner.security;

import com.fooddelivery.integration.partner.entity.Partner;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * The authenticated caller on a partner request.
 *
 * <p>Deliberately not a {@code UserPrincipal}: a partner is not a person, has no
 * account to suspend and no roles in our role model. Reusing the user principal
 * would put a synthetic user into every ownership check in the codebase, where
 * it would eventually be treated as one.
 */
@Getter
public class PartnerPrincipal {

    public static final String ROLE = "ROLE_PARTNER";

    private final Long partnerId;
    private final String partnerCode;
    private final Long keyId;

    public PartnerPrincipal(Partner partner, Long keyId) {
        this.partnerId = partner.getId();
        this.partnerCode = partner.getCode();
        this.keyId = keyId;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(ROLE));
    }

    @Override
    public String toString() {
        return "partner:" + partnerCode;
    }
}
