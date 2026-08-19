package com.programandoenjava.airline.payment.infrastructure.adapter.out.booking;

import com.programandoenjava.airline.payment.infrastructure.adapter.out.booking.dto.BookingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "bookingService",
        url = "${airline.booking-service.url}",
        configuration = BookingClientConfiguration.class)
public interface BookingClient {

    @GetMapping("/api/v1/bookings/{bookingId}")
    BookingResponse byId(@PathVariable UUID bookingId);
}
