package com.programandoenjava.airline.checkin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CheckinServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(CheckinServiceApplication.class, args);
    }
}
