package com.programandoenjava.airline.flight;

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
@DisplayName("Flight service wiring")
class FlightServiceApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("when the context starts")
    class ContextStartup {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        @DisplayName("should register the application entry point")
        void shouldRegisterTheApplicationEntryPoint() {
            Assertions.assertThat(applicationContext.getBean(FlightServiceApplication.class))
                    .isNotNull();
        }

        @Test
        @DisplayName("should connect to a real PostgreSQL instance")
        void shouldConnectToARealPostgreSqlInstance() {
            String product = jdbcTemplate.queryForObject("SELECT version()", String.class);

            Assertions.assertThat(product).startsWith("PostgreSQL");
        }
    }

    @Nested
    @DisplayName("when Flyway has run")
    class FlywayMigrations {

        @Test
        @DisplayName("should have created its schema history table")
        void shouldHaveCreatedItsSchemaHistoryTable() {
            Integer tables = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_name = 'flyway_schema_history'
                    """, Integer.class);

            Assertions.assertThat(tables).isEqualTo(1);
        }
    }
}
