package com.fooddelivery.auth.repository;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The admin panel's owner picker sits directly on this query, and the bug it
 * had could only be seen against a real database: a bare {@code LIKE} is
 * case-sensitive, so "asad" did not find "Asad", and firstName/lastName were
 * compared separately, so "Asad Karimov" found nobody at all. Mocking the
 * repository would have asserted nothing about either.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:usersearch;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("UserRepository.searchUsers")
class UserSearchQueryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void seed() {
        userRepository.deleteAll();
        userRepository.saveAll(List.of(
                user("asad@example.com", "Asad", "Karimov", "998901234567"),
                user("dilnoza@example.com", "Dilnoza", "Rahimova", "998907654321"),
                user("user1@example.com", "Bobur", "Aliyev", "998911112233")));
    }

    private User user(String email, String first, String last, String phone) {
        return User.builder()
                .email(email).firstName(first).lastName(last).phone(phone)
                .role(Role.CONSUMER).status(UserStatus.ACTIVE)
                .build();
    }

    /** Mirrors what UserService passes for a query that is not a phone number. */
    private List<String> emailsFor(String query) {
        return emailsFor(query, query);
    }

    private List<String> emailsFor(String search, String phoneTerm) {
        return userRepository.searchUsers(search, phoneTerm, PageRequest.of(0, 20))
                .getContent().stream().map(User::getEmail).toList();
    }

    @Test
    @DisplayName("a lowercase query finds a capitalised name")
    void isCaseInsensitiveOnName() {
        assertThat(emailsFor("asad")).containsExactly("asad@example.com");
        assertThat(emailsFor("KARIMOV")).containsExactly("asad@example.com");
    }

    @Test
    @DisplayName("a lowercase query finds a mixed-case email")
    void isCaseInsensitiveOnEmail() {
        assertThat(emailsFor("DILNOZA@EXAMPLE.COM")).containsExactly("dilnoza@example.com");
    }

    @Test
    @DisplayName("the full name matches, not just its halves")
    void matchesFullName() {
        assertThat(emailsFor("Asad Karimov")).containsExactly("asad@example.com");
        assertThat(emailsFor("dilnoza rahimova")).containsExactly("dilnoza@example.com");
    }

    @Test
    @DisplayName("a phone number matches however it was punctuated")
    void matchesPhoneRegardlessOfFormatting() {
        // What UserService derives from "+998 90 123 45 67".
        assertThat(emailsFor("+998 90 123 45 67", "998901234567"))
                .containsExactly("asad@example.com");
        // A partial number is a prefix search, as an operator would expect.
        assertThat(emailsFor("907654", "907654")).containsExactly("dilnoza@example.com");
    }

    @Test
    @DisplayName("an email containing a digit does not drag in every phone number")
    void emailQueryDoesNotMatchPhones() {
        // UserService passes the raw query as phoneTerm here precisely because
        // reducing "user1@example.com" to "1" would match all three phones.
        assertThat(emailsFor("user1@example.com")).containsExactly("user1@example.com");
    }

    @Test
    @DisplayName("a query matching nothing returns nothing")
    void noMatches() {
        assertThat(emailsFor("nobody")).isEmpty();
    }
}
