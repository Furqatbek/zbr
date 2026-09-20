package com.fooddelivery.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Encrypts a column on the way to the database and back.
 *
 * <p>A converter rather than a service call at each use site, so the three
 * places that read a partner's outbound key did not have to change and a fourth
 * cannot forget. The field stays a plain String to everything above JPA.
 *
 * <p><strong>Why the cipher is static.</strong> Hibernate instantiates
 * converters itself, so a constructor-injected one only works where Spring has
 * the dependency — which the full application has and a {@code @DataJpaTest}
 * slice does not. Making it a Spring bean therefore broke every JPA slice test
 * in the codebase, including several with nothing to do with partners.
 * {@link SecretCipherRegistrar} installs the real cipher at startup.
 *
 * <p>Left uninstalled, the fallback is an unconfigured cipher: it reads legacy
 * plaintext and refuses to encrypt. That is deliberately the same safe state as
 * a missing key — a converter that quietly wrote secrets in the clear because a
 * wiring detail was missed is exactly the bug this class exists to close.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static volatile SecretCipher cipher = new SecretCipher(null);

    static void install(SecretCipher configured) {
        cipher = configured;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return cipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return cipher.decrypt(dbData);
    }
}
