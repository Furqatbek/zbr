package com.fooddelivery.auth.security;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Neutralizes the demo/seed accounts installed by Flyway migration V2 when the
 * application runs under the "prod" profile.
 *
 * V2 seeds well-known accounts (admin@fooddelivery.com etc.) all sharing the
 * bcrypt hash of the password "password" — a full-compromise vector if it ever
 * reaches production. Rather than trust ops to delete them, this guard suspends
 * any seed account whose password hash is still the committed default, so it
 * cannot be used to log in. Accounts whose password has been changed are left
 * untouched.
 */
@Component
// Deliberately NOT @Profile("prod"). This ran only under "prod" while
// docker-compose activated "docker", so on the one deployment path anybody
// actually uses, the guard never ran and admin@fooddelivery.com / "password"
// stayed live. Gating on the ABSENCE of the local profiles means a server can
// only lose this protection by explicitly asking for a development profile.
@Profile("!test & !dev")
@RequiredArgsConstructor
@Slf4j
public class SeedAccountGuard implements ApplicationRunner {

    /** bcrypt hash of "password" as committed in V2__seed_data.sql. */
    private static final String DEFAULT_SEED_HASH =
            "$2a$12$VOI3I3JDdZyxdFeKh55p.upfoSP.FJVVok.qcXr1zFBGRiFfYr1He";

    private static final List<String> SEED_EMAILS = List.of(
            "admin@fooddelivery.com",
            "platform@fooddelivery.com",
            "owner@pizzapalace.com",
            "john.doe@example.com",
            "courier@fooddelivery.com"
    );

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int suspended = 0;
        for (String email : SEED_EMAILS) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                continue;
            }
            if (DEFAULT_SEED_HASH.equals(user.getPasswordHash()) && user.getStatus() != UserStatus.SUSPENDED) {
                user.setStatus(UserStatus.SUSPENDED);
                userRepository.save(user);
                suspended++;
                log.error("SECURITY: prod seed account '{}' still uses the default committed password. " +
                        "Suspended it. Delete or reset+reactivate it before use.", email);
            }
        }
        if (suspended > 0) {
            log.error("SECURITY: suspended {} default-password seed account(s) in prod. " +
                    "Rotate credentials and remove V2 seed data from production databases.", suspended);
        } else {
            log.info("Seed account guard: no default-password seed accounts active in prod.");
        }
    }
}
