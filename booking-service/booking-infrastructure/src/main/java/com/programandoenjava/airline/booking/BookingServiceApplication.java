package com.programandoenjava.airline.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Sits in the package the three layers share, so component scanning reaches
 * adapters that live in other module jars.
 */
@SpringBootApplication
@EnableFeignClients
public class BookingServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
