package com.programandoenjava.airline.booking.infrastructure.adapter.in.events;

import com.programandoenjava.airline.booking.application.port.in.settlebooking.ConfirmBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.FailBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.SettleBookingCommand;
import com.programandoenjava.airline.booking.domain.booking.BookingId;
import com.programandoenjava.airline.booking.infrastructure.adapter.in.events.dto.PaymentFailedEvent;
import com.programandoenjava.airline.booking.infrastructure.adapter.in.events.dto.PaymentSucceededEvent;
import org.springframework.kafka.annotation.KafkaListener;
import tools.jackson.databind.ObjectMapper;

class PaymentEventsListener {

    private final ConfirmBookingUseCase confirmBooking;
    private final FailBookingUseCase failBooking;
    private final ObjectMapper objectMapper;

    PaymentEventsListener(final ConfirmBookingUseCase confirmBooking,
                          final FailBookingUseCase failBooking,
                          final ObjectMapper objectMapper) {
        this.confirmBooking = confirmBooking;
        this.failBooking = failBooking;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = PaymentTopics.SUCCEEDED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentSucceeded(final String payload) {
        PaymentSucceededEvent event =
                objectMapper.readValue(payload, PaymentSucceededEvent.class);

        SettleBookingCommand command = toCommand(event.eventId(), event.bookingId());

        confirmBooking.confirm(command);
    }

    @KafkaListener(topics = PaymentTopics.FAILED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentFailed(final String payload) {
        PaymentFailedEvent event =
                objectMapper.readValue(payload, PaymentFailedEvent.class);

        SettleBookingCommand command = toCommand(event.eventId(), event.bookingId());

        failBooking.fail(command);
    }

    private static SettleBookingCommand toCommand(final java.util.UUID eventId,
                                                  final java.util.UUID bookingId) {
        BookingId booking = new BookingId(bookingId);

        return new SettleBookingCommand(eventId, booking);
    }
}
