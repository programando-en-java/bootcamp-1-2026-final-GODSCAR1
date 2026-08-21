package com.programandoenjava.airline.notification.domain.notification;

import com.programandoenjava.airline.notification.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@DisplayName("Notification")
class NotificationTest {

    private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-03-10T12:00:05Z");

    private static final BigDecimal TOTAL = new BigDecimal("500000.00");
    private static final String COP = "COP";
    private static final int SEATS = 2;

    private static final String FLIGHT_NUMBER = "AV8001";
    private static final String ORIGIN = "BOG";
    private static final String DESTINATION = "MDE";
    private static final Instant DEPARTURE = Instant.parse("2026-03-11T08:00:00Z");
    private static final int FIRST_TO_BOARD = 1;

    @Nested
    @DisplayName("when it is raised")
    class Raising {

        @Test
        @DisplayName("should be addressed to the passenger it is about")
        void shouldBeAddressedToThePassengerItIsAbout() {
            PassengerId passenger = aPassenger();

            Notification notification = Notification.raise(
                    passenger, aBooking(), NotificationType.PAYMENT_SUCCEEDED,
                    aMessage(), NOW);

            Assertions.assertThat(notification.passengerId()).isEqualTo(passenger);
        }

        @Test
        @DisplayName("should not claim to have been sent yet")
        void shouldNotClaimToHaveBeenSentYet() {
            Notification notification = aNotification();

            Assertions.assertThat(notification.hasBeenSent()).isFalse();
            Assertions.assertThat(notification.sentAt()).isNull();
        }

        @Test
        @DisplayName("should refuse to be raised without a passenger")
        void shouldRefuseToBeRaisedWithoutAPassenger() {
            Assertions.assertThatThrownBy(() -> new Notification(
                    NotificationId.newId(), null, aBooking(),
                    NotificationType.BOOKING_CREATED, aMessage(), NOW, null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("addressed to a passenger");
        }

        @Test
        @DisplayName("should refuse to be raised without a message")
        void shouldRefuseToBeRaisedWithoutAMessage() {
            Assertions.assertThatThrownBy(() -> new Notification(
                    NotificationId.newId(), aPassenger(), aBooking(),
                    NotificationType.BOOKING_CREATED, null, NOW, null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("carry a message");
        }
    }

    @Nested
    @DisplayName("when the channel has taken it")
    class Sent {

        @Test
        @DisplayName("should record when that was")
        void shouldRecordWhenThatWas() {
            Notification sent = aNotification().sentAt(LATER);

            Assertions.assertThat(sent.hasBeenSent()).isTrue();
            Assertions.assertThat(sent.sentAt()).isEqualTo(LATER);
        }

        @Test
        @DisplayName("should leave the original untouched")
        void shouldLeaveTheOriginalUntouched() {
            Notification notification = aNotification();

            notification.sentAt(LATER);

            Assertions.assertThat(notification.hasBeenSent()).isFalse();
        }

        @Test
        @DisplayName("should keep everything else the same")
        void shouldKeepEverythingElseTheSame() {
            Notification notification = aNotification();

            Notification sent = notification.sentAt(LATER);

            Assertions.assertThat(sent.id()).isEqualTo(notification.id());
            Assertions.assertThat(sent.message()).isEqualTo(notification.message());
            Assertions.assertThat(sent.createdAt()).isEqualTo(notification.createdAt());
        }
    }

    @Nested
    @DisplayName("what a booking message says")
    class BookingCreatedMessage {

        @Test
        @DisplayName("should say what is being held and what it costs")
        void shouldSayWhatIsBeingHeldAndWhatItCosts() {
            NotificationMessage message =
                    NotificationMessage.bookingCreated(SEATS, TOTAL, COP);

            Assertions.assertThat(message.body())
                    .contains("2 seat")
                    .contains("500000.00")
                    .contains(COP);
        }

        @Test
        @DisplayName("should not call an unpaid booking confirmed")
        void shouldNotCallAnUnpaidBookingConfirmed() {
            NotificationMessage message =
                    NotificationMessage.bookingCreated(SEATS, TOTAL, COP);

            Assertions.assertThat(message.subject()).doesNotContainIgnoringCase("confirmed");
        }
    }

    @Nested
    @DisplayName("what a payment message says")
    class PaymentSucceededMessage {

        @Test
        @DisplayName("should say what was taken")
        void shouldSayWhatWasTaken() {
            NotificationMessage message =
                    NotificationMessage.paymentSucceeded(TOTAL, COP);

            Assertions.assertThat(message.body())
                    .contains("500000.00")
                    .contains(COP);
        }

        @Test
        @DisplayName("should tell the passenger what they can do next")
        void shouldTellThePassengerWhatTheyCanDoNext() {
            NotificationMessage message =
                    NotificationMessage.paymentSucceeded(TOTAL, COP);

            Assertions.assertThat(message.body()).containsIgnoringCase("check in");
        }
    }

    @Nested
    @DisplayName("what a check-in message says")
    class CheckInCompletedMessage {

        @Test
        @DisplayName("should name the flight and the route")
        void shouldNameTheFlightAndTheRoute() {
            NotificationMessage message = NotificationMessage.checkInCompleted(
                    FLIGHT_NUMBER, ORIGIN, DESTINATION, DEPARTURE, FIRST_TO_BOARD);

            Assertions.assertThat(message.subject()).contains(FLIGHT_NUMBER);
            Assertions.assertThat(message.body())
                    .contains(ORIGIN)
                    .contains(DESTINATION);
        }

        @Test
        @DisplayName("should say where in the queue they are")
        void shouldSayWhereInTheQueueTheyAre() {
            NotificationMessage message = NotificationMessage.checkInCompleted(
                    FLIGHT_NUMBER, ORIGIN, DESTINATION, DEPARTURE, FIRST_TO_BOARD);

            Assertions.assertThat(message.body()).contains("number 1");
        }
    }

    @Nested
    @DisplayName("as a message")
    class Wording {

        @Test
        @DisplayName("should refuse an empty subject")
        void shouldRefuseAnEmptySubject() {
            Assertions.assertThatThrownBy(() -> new NotificationMessage("  ", "something"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("subject");
        }

        @Test
        @DisplayName("should refuse an empty body")
        void shouldRefuseAnEmptyBody() {
            Assertions.assertThatThrownBy(() -> new NotificationMessage("something", ""))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("body");
        }
    }

    private static Notification aNotification() {
        return Notification.raise(aPassenger(), aBooking(),
                NotificationType.PAYMENT_SUCCEEDED, aMessage(), NOW);
    }

    private static NotificationMessage aMessage() {
        return NotificationMessage.paymentSucceeded(TOTAL, COP);
    }

    private static PassengerId aPassenger() {
        return new PassengerId(UUID.randomUUID());
    }

    private static BookingId aBooking() {
        return new BookingId(UUID.randomUUID());
    }
}
