package com.programandoenjava.airline.flight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * This class lives in com.programandoenjava.airline.flight, the package shared
 * by all three layers, and NOT in ...flight.infrastructure.
 *
 * Component scanning starts at the annotated class's own package and walks
 * downwards; it does not care about module boundaries, only packages. Moving
 * this class into a deeper package would leave adapters undiscovered, and the
 * application would start with no beans and no error message.
 */
@SpringBootApplication
public class FlightServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlightServiceApplication.class, args);
    }
}
