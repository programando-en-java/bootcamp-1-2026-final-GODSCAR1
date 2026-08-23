package com.programandoenjava.airline.auth;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("Auth service")
class AuthServiceApplicationTests {

    @Nested
    @DisplayName("when the context starts")
    class ContextStartup {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("should register the application entry point")
        void shouldRegisterTheApplicationEntryPoint() {
            Assertions.assertThat(context.getBean(AuthServiceApplication.class)).isNotNull();
        }
    }

    @Nested
    @DisplayName("when Flyway has run")
    class FlywayMigrations {

        @Test
        @DisplayName("should have seeded one account per role")
        void shouldHaveSeededOneAccountPerRole(@Autowired final JdbcTemplate jdbc) {
            Integer users = jdbc.queryForObject("SELECT count(*) FROM users", Integer.class);

            Assertions.assertThat(users).isEqualTo(3);
        }

        @Test
        @DisplayName("should have stored no password in the clear")
        void shouldHaveStoredNoPasswordInTheClear(@Autowired final JdbcTemplate jdbc) {
            Integer hashed = jdbc.queryForObject("""
                    SELECT count(*) FROM users WHERE password_hash LIKE '$2%'
                    """, Integer.class);

            Assertions.assertThat(hashed).isEqualTo(3);
        }
    }
}
