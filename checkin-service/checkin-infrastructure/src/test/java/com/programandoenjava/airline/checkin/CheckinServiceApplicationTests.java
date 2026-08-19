package com.programandoenjava.airline.checkin;

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
@DisplayName("Check-in service")
class CheckinServiceApplicationTests {

    @Nested
    @DisplayName("when the context starts")
    class ContextStartup {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("should register the application entry point")
        void shouldRegisterTheApplicationEntryPoint() {
            Assertions.assertThat(context.getBean(CheckinServiceApplication.class)).isNotNull();
        }

        @Test
        @DisplayName("should connect to a real PostgreSQL instance")
        void shouldConnectToARealPostgreSqlInstance(@Autowired final JdbcTemplate jdbc) {
            String version = jdbc.queryForObject("SELECT version()", String.class);

            Assertions.assertThat(version).contains("PostgreSQL");
        }
    }

    @Nested
    @DisplayName("when Flyway has run")
    class FlywayMigrations {

        @Test
        @DisplayName("should have created the boarding pass tables")
        void shouldHaveCreatedTheBoardingPassTables(@Autowired final JdbcTemplate jdbc) {
            Integer tables = jdbc.queryForObject("""
                    SELECT count(*) FROM information_schema.tables
                    WHERE table_name IN ('boarding_passes', 'boarding_sequences')
                    """, Integer.class);

            Assertions.assertThat(tables).isEqualTo(2);
        }
    }
}
