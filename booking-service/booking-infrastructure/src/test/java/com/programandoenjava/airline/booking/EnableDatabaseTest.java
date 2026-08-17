package com.programandoenjava.airline.booking;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.AutoConfigureTestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Turns a hand-built slice into one with a real database behind it.
 *
 * <p>A slice that lists its own beans has auto-configuration switched off, so
 * everything JPA needs has to be asked for by name. Replace.NONE is what stops
 * Boot swapping the Testcontainers datasource for an embedded one.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableJpaRepositories(basePackages = "com.programandoenjava.airline.booking")
@EntityScan(basePackages = "com.programandoenjava.airline.booking")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureTestEntityManager
@AutoConfigureDataJpa
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
public @interface EnableDatabaseTest {
}
