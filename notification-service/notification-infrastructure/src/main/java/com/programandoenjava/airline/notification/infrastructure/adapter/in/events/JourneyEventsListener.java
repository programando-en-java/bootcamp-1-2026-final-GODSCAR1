package com.programandoenjava.airline.notification.infrastructure.adapter.in.events;

import com.programandoenjava.airline.notification.application.port.in.notify.BookingCreatedCommand;
import com.programandoenjava.airline.notification.application.port.in.notify.CheckInCompletedCommand;
import com.programandoenjava.airline.notification.application.port.in.notify.NotifyPassengerUseCase;
import com.programandoenjava.airline.notification.application.port.in.notify.PaymentSucceededCommand;
import com.programandoenjava.airline.notification.domain.notification.BookingId;
import com.programandoenjava.airline.notification.domain.notification.PassengerId;
import com.programandoenjava.airline.notification.infrastructure.adapter.in.events.dto.BookingCreatedEvent;
import com.programandoenjava.airline.notification.infrastructure.adapter.in.events.dto.CheckInCompletedEvent;
import com.programandoenjava.airline.notification.infrastructure.adapter.in.events.dto.PaymentSucceededEvent;
import org.springframework.kafka.annotation.KafkaListener;
import tools.jackson.databind.ObjectMapper;

/**
 * The three topics this service listens to. One method each rather than one
 * with a branch: they arrive on different topics, carry different fields and
 * say different things.
 *
 * <p>Nothing here decides whether a message has been seen before. The event id
 * travels into the command and the use case claims it, because what must not
 * happen twice is the notification rather than the delivery (ADR-014).
 *
 * <p>Nothing is listened to on payment.failed.v1. No story asks for it, and a
 * passenger whose card was declined already learned that from the response to
 * their own request.
 */
class JourneyEventsListener {

    private final NotifyPassengerUseCase notifyPassenger;
    private final ObjectMapper objectMapper;

    JourneyEventsListener(final NotifyPassengerUseCase notifyPassenger,
                          final ObjectMapper objectMapper) {
        this.notifyPassenger = notifyPassenger;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = JourneyTopics.BOOKING_CREATED,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onBookingCreated(final String payload) {
        BookingCreatedEvent event = objectMapper.readValue(payload, BookingCreatedEvent.class);

        BookingCreatedCommand command = new BookingCreatedCommand(
                event.eventId(),
                new PassengerId(event.passengerId()),
                new BookingId(event.bookingId()),
                event.seats(),
                event.total(),
                event.currency());

        notifyPassenger.onBookingCreated(command);
    }

    @KafkaListener(topics = JourneyTopics.PAYMENT_SUCCEEDED,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentSucceeded(final String payload) {
        PaymentSucceededEvent event = objectMapper.readValue(payload, PaymentSucceededEvent.class);

        PaymentSucceededCommand command = new PaymentSucceededCommand(
                event.eventId(),
                new PassengerId(event.passengerId()),
                new BookingId(event.bookingId()),
                event.amount(),
                event.currency());

        notifyPassenger.onPaymentSucceeded(command);
    }

    @KafkaListener(topics = JourneyTopics.CHECK_IN_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onCheckInCompleted(final String payload) {
        CheckInCompletedEvent event = objectMapper.readValue(payload, CheckInCompletedEvent.class);

        CheckInCompletedCommand command = new CheckInCompletedCommand(
                event.eventId(),
                new PassengerId(event.passengerId()),
                new BookingId(event.bookingId()),
                event.flightNumber(),
                event.origin(),
                event.destination(),
                event.departureTime(),
                event.boardingSequence());

        notifyPassenger.onCheckInCompleted(command);
    }
}
