package com.programandoenjava.airline.auth.domain.user;

import com.programandoenjava.airline.auth.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@DisplayName("User")
class UserTest {

    private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");
    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuvwxyz012345678901234567890123456";

    @Nested
    @DisplayName("when one is registered")
    class Registering {

        @Test
        @DisplayName("should be reachable at the email it gave")
        void shouldBeReachableAtTheEmailItGave() {
            User user = User.register(new Email("oscar@example.com"), aHash(),
                    Set.of(Role.PASSENGER), NOW);

            Assertions.assertThat(user.email().value()).isEqualTo("oscar@example.com");
        }

        @Test
        @DisplayName("should refuse a user with no role at all")
        void shouldRefuseAUserWithNoRoleAtAll() {
            Assertions.assertThatThrownBy(() -> User.register(
                    anEmail(), aHash(), Set.of(), NOW))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("at least one role");
        }

        @Test
        @DisplayName("should keep its roles to itself")
        void shouldKeepItsRolesToItself() {
            Set<Role> given = new HashSet<>(Set.of(Role.PASSENGER));

            User user = User.register(anEmail(), aHash(), given, NOW);
            given.add(Role.ADMIN);

            Assertions.assertThat(user.hasRole(Role.ADMIN)).isFalse();
        }
    }

    @Nested
    @DisplayName("the email")
    class Emails {

        @Test
        @DisplayName("should be stored in lower case, however it was typed")
        void shouldBeStoredInLowerCase() {
            Email email = new Email("  Oscar@Example.COM ");

            Assertions.assertThat(email.value()).isEqualTo("oscar@example.com");
        }

        @Test
        @DisplayName("should refuse something that is not an address")
        void shouldRefuseSomethingThatIsNotAnAddress() {
            Assertions.assertThatThrownBy(() -> new Email("oscar at example"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Not an email address");
        }

        @Test
        @DisplayName("should refuse an empty one")
        void shouldRefuseAnEmptyOne() {
            Assertions.assertThatThrownBy(() -> new Email("   "))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("An email is required");
        }
    }

    @Nested
    @DisplayName("the password")
    class Passwords {

        @Test
        @DisplayName("should refuse to hold one that was never hashed")
        void shouldRefuseToHoldOneThatWasNeverHashed() {
            Assertions.assertThatThrownBy(() -> new PasswordHash("hunter2"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("must be hashed");
        }

        @Test
        @DisplayName("should not print itself")
        void shouldNotPrintItself() {
            PasswordHash hash = aHash();

            Assertions.assertThat(hash.toString()).doesNotContain(HASH);
        }

        @Test
        @DisplayName("should stay out of a user's own printout")
        void shouldStayOutOfAUsersOwnPrintout() {
            User user = User.register(anEmail(), aHash(), Set.of(Role.PASSENGER), NOW);

            Assertions.assertThat(user.toString()).doesNotContain(HASH);
        }
    }

    private static Email anEmail() {
        return new Email("oscar@example.com");
    }

    private static PasswordHash aHash() {
        return new PasswordHash(HASH);
    }
}
