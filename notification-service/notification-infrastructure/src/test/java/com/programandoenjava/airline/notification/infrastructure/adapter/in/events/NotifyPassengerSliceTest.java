package com.programandoenjava.airline.notification.infrastructure.adapter.in.events;

import com.programandoenjava.airline.notification.EnableDatabaseTest;
import com.programandoenjava.airline.notification.TestcontainersConfiguration;
import com.programandoenjava.airline.notification.application.port.in.notify.BookingCreatedCommand;
import com.programandoenjava.airline.notification.application.port.in.notify.CheckInCompletedCommand;
import com.programandoenjava.airline.notification.application.port.in.notify.NotifyPassengerUseCase;
import com.programandoenjava.airline.notification.application.port.in.notify.PaymentSucceededCommand;
import com.programandoenjava.airline.notification.application.port.out.channel.NotificationChannel;
import com.programandoenjava.airline.notification.domain.notification.BookingId;
import com.programandoenjava.airline.notification.domain.notification.Notification;
import com.programandoenjava.airline.notification.domain.notification.PassengerId;
import com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.notifications.NotificationPersistenceConfiguration;
import com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.processedevents.ProcessedEventsConfiguration;
import com.programandoenjava.airline.notification.infrastructure.config.ApplicationConfiguration;
import com.programandoenjava.airline.notification.infrastructure.transaction.TransactionSupportConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * All three stories of EPIC-05, against a real database.
 *
 * <p>The channel is mocked because what it does today is write a log line, and
 * asserting on logs is the kind of test that breaks when someone rewords a
 * message. What is asserted instead is that the channel was handed the right
 * notification, and that the row says it went out.
 *
 * <p>Driven through the use case rather than through Kafka. Whether a listener
 * can deserialise what another service really sends is the end-to-end test's
 * question; this one is about what happens once it has.
 */
@SpringBootTest(classes = {
        ApplicationConfiguration.class,
        NotificationPersistenceConfiguration.class,
        ProcessedEventsConfiguration.class,
        TransactionSupportConfiguration.class
})
@EnableDatabaseTest
@Import(TestcontainersConfiguration.class)
@Sql(scripts = "/db/testdata/R__reset_notifications.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Notifying a passenger (hand-built, no full context)")
class NotifyPassengerSliceTest {

    private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");
    private static final Instant DEPARTURE = Instant.parse("2026-03-11T08:00:00Z");

    private static final BigDecimal TOTAL = new BigDecimal("500000.00");
    private static final String COP = "COP";
    private static final int SEATS = 2;

    private static final String FLIGHT_NUMBER = "AV8001";
    private static final String ORIGIN = "BOG";
    private static final String DESTINATION = "MDE";
    private static final int FIRST_TO_BOARD = 1;

    private static final String COUNT_NOTIFICATIONS = "SELECT count(*) FROM notifications";
    private static final String FIND_NOTIFICATIONS = """
            SELECT passenger_id, booking_id, type, subject, body, sent_at
            FROM notifications ORDER BY created_at
            """;
    private static final String COUNT_CLAIMED = "SELECT count(*) FROM processed_events";

    @Autowired
    private NotifyPassengerUseCase notifyPassengerUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private NotificationChannel notificationChannel;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("when a booking is made")
    class BookingCreated {

        @Test
        @DisplayName("should tell the passenger it has their booking")
        void shouldTellThePassengerItHasTheirBooking() {
            UUID passenger = UUID.randomUUID();

            notifyOfBooking(UUID.randomUUID(), passenger, UUID.randomUUID());

            Map<String, Object> notification = theOnlyNotification();

            Assertions.assertThat(notification.get("passenger_id")).hasToString(passenger.toString());
            Assertions.assertThat(notification.get("type")).isEqualTo("BOOKING_CREATED");
        }

        @Test
        @DisplayName("should say what is held and what is owed")
        void shouldSayWhatIsHeldAndWhatIsOwed() {
            notifyOfBooking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            String body = String.valueOf(theOnlyNotification().get("body"));

            Assertions.assertThat(body)
                    .contains("2 seat")
                    .contains("500000.00");
        }

        @Test
        @DisplayName("should hand it to the channel")
        void shouldHandItToTheChannel() {
            UUID booking = UUID.randomUUID();

            notifyOfBooking(UUID.randomUUID(), UUID.randomUUID(), booking);

            ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
            Mockito.verify(notificationChannel).send(sent.capture());

            Assertions.assertThat(sent.getValue().bookingId())
                    .isEqualTo(new BookingId(booking));
        }
    }

    @Nested
    @DisplayName("when a payment goes through")
    class PaymentSucceeded {

        @Test
        @DisplayName("should tell the passenger their money arrived")
        void shouldTellThePassengerTheirMoneyArrived() {
            notifyOfPayment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            Map<String, Object> notification = theOnlyNotification();

            Assertions.assertThat(notification.get("type")).isEqualTo("PAYMENT_SUCCEEDED");
            Assertions.assertThat(String.valueOf(notification.get("body")))
                    .contains("500000.00");
        }

        /*
         * The passenger arrives in this message because payment.succeeded.v1
         * was given a passengerId for this epic. Without it there would be
         * nobody to address.
         */
        @Test
        @DisplayName("should address it to the passenger the event named")
        void shouldAddressItToThePassengerTheEventNamed() {
            UUID passenger = UUID.randomUUID();

            notifyOfPayment(UUID.randomUUID(), passenger, UUID.randomUUID());

            ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
            Mockito.verify(notificationChannel).send(sent.capture());

            Assertions.assertThat(sent.getValue().passengerId())
                    .isEqualTo(new PassengerId(passenger));
        }
    }

    @Nested
    @DisplayName("when a passenger checks in")
    class CheckInCompleted {

        @Test
        @DisplayName("should name the flight they are on")
        void shouldNameTheFlightTheyAreOn() {
            notifyOfCheckIn(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            Map<String, Object> notification = theOnlyNotification();

            Assertions.assertThat(notification.get("type")).isEqualTo("CHECK_IN_COMPLETED");
            Assertions.assertThat(String.valueOf(notification.get("subject")))
                    .contains(FLIGHT_NUMBER);
        }

        @Test
        @DisplayName("should say where in the boarding order they are")
        void shouldSayWhereInTheBoardingOrderTheyAre() {
            notifyOfCheckIn(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            Assertions.assertThat(String.valueOf(theOnlyNotification().get("body")))
                    .contains(ORIGIN)
                    .contains(DESTINATION)
                    .contains("number 1");
        }
    }

    @Nested
    @DisplayName("once the channel has taken it")
    class Sending {

        @Test
        @DisplayName("should record that it went out")
        void shouldRecordThatItWentOut() {
            notifyOfBooking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            Assertions.assertThat(theOnlyNotification().get("sent_at")).isNotNull();
        }

        /*
         * Written first, sent second. If the channel throws, the row is still
         * there with sent_at empty, and the partial index over those rows is
         * what a sweep would use. Nothing sweeps yet, and that is the gap.
         */
        @Test
        @DisplayName("should keep the notification when the channel fails")
        void shouldKeepTheNotificationWhenTheChannelFails() {
            BDDMockito.willThrow(new IllegalStateException("the provider said no"))
                    .given(notificationChannel).send(BDDMockito.any());

            Assertions.assertThatThrownBy(() ->
                            notifyOfBooking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                    .isInstanceOf(RuntimeException.class);

            Assertions.assertThat(notificationCount()).isEqualTo(1);
            Assertions.assertThat(theOnlyNotification().get("sent_at")).isNull();
        }
    }

    @Nested
    @DisplayName("when the same message arrives again")
    class Redelivered {

        /*
         * The reason this service claims an event before doing anything. A
         * booking confirmed twice is the same booking; a passenger notified
         * twice is a passenger who noticed.
         */
        @Test
        @DisplayName("should not notify the passenger a second time")
        void shouldNotNotifyThePassengerASecondTime() {
            UUID eventId = UUID.randomUUID();
            UUID passenger = UUID.randomUUID();
            UUID booking = UUID.randomUUID();

            notifyOfBooking(eventId, passenger, booking);
            notifyOfBooking(eventId, passenger, booking);

            Assertions.assertThat(notificationCount()).isEqualTo(1);
            Mockito.verify(notificationChannel, Mockito.times(1)).send(BDDMockito.any());
        }

        @Test
        @DisplayName("should still notify about a different event")
        void shouldStillNotifyAboutADifferentEvent() {
            UUID passenger = UUID.randomUUID();
            UUID booking = UUID.randomUUID();

            notifyOfBooking(UUID.randomUUID(), passenger, booking);
            notifyOfPayment(UUID.randomUUID(), passenger, booking);

            Assertions.assertThat(notificationCount()).isEqualTo(2);
            Assertions.assertThat(claimedCount()).isEqualTo(2);
        }
    }

    private void notifyOfBooking(final UUID eventId, final UUID passengerId, final UUID bookingId) {
        BookingCreatedCommand command = new BookingCreatedCommand(
                eventId,
                new PassengerId(passengerId),
                new BookingId(bookingId),
                SEATS,
                TOTAL,
                COP);

        notifyPassengerUseCase.onBookingCreated(command);
    }

    private void notifyOfPayment(final UUID eventId, final UUID passengerId, final UUID bookingId) {
        PaymentSucceededCommand command = new PaymentSucceededCommand(
                eventId,
                new PassengerId(passengerId),
                new BookingId(bookingId),
                TOTAL,
                COP);

        notifyPassengerUseCase.onPaymentSucceeded(command);
    }

    private void notifyOfCheckIn(final UUID eventId, final UUID passengerId, final UUID bookingId) {
        CheckInCompletedCommand command = new CheckInCompletedCommand(
                eventId,
                new PassengerId(passengerId),
                new BookingId(bookingId),
                FLIGHT_NUMBER,
                ORIGIN,
                DESTINATION,
                DEPARTURE,
                FIRST_TO_BOARD);

        notifyPassengerUseCase.onCheckInCompleted(command);
    }

    private Map<String, Object> theOnlyNotification() {
        List<Map<String, Object>> notifications = jdbcTemplate.queryForList(FIND_NOTIFICATIONS);

        Assertions.assertThat(notifications).hasSize(1);

        return notifications.getFirst();
    }

    private long notificationCount() {
        return jdbcTemplate.queryForObject(COUNT_NOTIFICATIONS, Long.class);
    }

    private long claimedCount() {
        return jdbcTemplate.queryForObject(COUNT_CLAIMED, Long.class);
    }
}
