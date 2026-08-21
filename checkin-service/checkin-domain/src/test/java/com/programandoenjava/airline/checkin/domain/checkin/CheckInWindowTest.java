package com.programandoenjava.airline.checkin.domain.checkin;

import com.programandoenjava.airline.checkin.domain.checkin.exception.CheckInClosedException;
import com.programandoenjava.airline.checkin.domain.checkin.exception.CheckInNotOpenYetException;
import com.programandoenjava.airline.checkin.domain.checkin.exception.FlightDepartedException;
import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

@DisplayName("Check-in window")
class CheckInWindowTest {

    private static final Instant DEPARTURE = Instant.parse("2026-03-11T08:00:00Z");

    private static final Duration OPENS_BEFORE = Duration.ofHours(24);
    private static final Duration CLOSES_BEFORE = Duration.ofHours(1);

    private static final Instant OPENING = Instant.parse("2026-03-10T08:00:00Z");
    private static final Instant CLOSING = Instant.parse("2026-03-11T07:00:00Z");

    private static final Duration A_SECOND = Duration.ofSeconds(1);

    @Nested
    @DisplayName("while it is open")
    class Open {

        @Test
        @DisplayName("should let a passenger check in the day before")
        void shouldLetAPassengerCheckInTheDayBefore() {
            Instant theDayBefore = DEPARTURE.minus(Duration.ofHours(20));

            Assertions.assertThatCode(() -> window().requireOpenAt(DEPARTURE, theDayBefore))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should let a passenger check in at the very moment it opens")
        void shouldLetAPassengerCheckInAtTheVeryMomentItOpens() {
            Assertions.assertThatCode(() -> window().requireOpenAt(DEPARTURE, OPENING))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should let a passenger check in a second before it closes")
        void shouldLetAPassengerCheckInASecondBeforeItCloses() {
            Instant justInTime = CLOSING.minus(A_SECOND);

            Assertions.assertThatCode(() -> window().requireOpenAt(DEPARTURE, justInTime))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("before it opens")
    class TooEarly {

        @Test
        @DisplayName("should refuse a passenger who is a week early")
        void shouldRefuseAPassengerWhoIsAWeekEarly() {
            Instant aWeekEarly = DEPARTURE.minus(Duration.ofDays(7));

            Assertions.assertThatThrownBy(() -> window().requireOpenAt(DEPARTURE, aWeekEarly))
                    .isInstanceOf(CheckInNotOpenYetException.class);
        }

        @Test
        @DisplayName("should refuse a second before it opens")
        void shouldRefuseASecondBeforeItOpens() {
            Instant justTooEarly = OPENING.minus(A_SECOND);

            Assertions.assertThatThrownBy(() -> window().requireOpenAt(DEPARTURE, justTooEarly))
                    .isInstanceOf(CheckInNotOpenYetException.class);
        }

        @Test
        @DisplayName("should say when it will open")
        void shouldSayWhenItWillOpen() {
            Instant aWeekEarly = DEPARTURE.minus(Duration.ofDays(7));

            Assertions.assertThatThrownBy(() -> window().requireOpenAt(DEPARTURE, aWeekEarly))
                    .hasMessageContaining(OPENING.toString());
        }
    }

    @Nested
    @DisplayName("after it closes")
    class TooLate {

        @Test
        @DisplayName("should refuse at the very moment it closes")
        void shouldRefuseAtTheVeryMomentItCloses() {
            Assertions.assertThatThrownBy(() -> window().requireOpenAt(DEPARTURE, CLOSING))
                    .isInstanceOf(CheckInClosedException.class);
        }

        @Test
        @DisplayName("should say when it closed")
        void shouldSayWhenItClosed() {
            Assertions.assertThatThrownBy(() -> window().requireOpenAt(DEPARTURE, CLOSING))
                    .hasMessageContaining(CLOSING.toString());
        }

        @Test
        @DisplayName("should not call a flight departed while it is still on the ground")
        void shouldNotCallAFlightDepartedWhileItIsStillOnTheGround() {
            Instant halfAnHourBefore = DEPARTURE.minus(Duration.ofMinutes(30));

            Assertions.assertThatThrownBy(() -> window().requireOpenAt(DEPARTURE, halfAnHourBefore))
                    .isNotInstanceOf(FlightDepartedException.class);
        }
    }

    @Nested
    @DisplayName("once the flight has gone")
    class Departed {

        @Test
        @DisplayName("should refuse at the moment of departure")
        void shouldRefuseAtTheMomentOfDeparture() {
            Assertions.assertThatThrownBy(() -> window().requireOpenAt(DEPARTURE, DEPARTURE))
                    .isInstanceOf(FlightDepartedException.class);
        }

        @Test
        @DisplayName("should refuse an hour after it left")
        void shouldRefuseAnHourAfterItLeft() {
            Instant afterwards = DEPARTURE.plus(Duration.ofHours(1));

            Assertions.assertThatThrownBy(() -> window().requireOpenAt(DEPARTURE, afterwards))
                    .isInstanceOf(FlightDepartedException.class);
        }
    }

    @Nested
    @DisplayName("as a window")
    class Construction {

        @Test
        @DisplayName("should reject one that closes before it opens")
        void shouldRejectOneThatClosesBeforeItOpens() {
            Duration opens = Duration.ofHours(1);
            Duration closes = Duration.ofHours(24);

            Assertions.assertThatThrownBy(() -> new CheckInWindow(opens, closes))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("must open before it closes");
        }

        @Test
        @DisplayName("should reject one that closes after departure")
        void shouldRejectOneThatClosesAfterDeparture() {
            Duration closes = Duration.ofHours(-1);

            Assertions.assertThatThrownBy(() -> new CheckInWindow(OPENS_BEFORE, closes))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("cannot close after departure");
        }

        @Test
        @DisplayName("should allow one that stays open until departure")
        void shouldAllowOneThatStaysOpenUntilDeparture() {
            CheckInWindow untilDeparture = new CheckInWindow(OPENS_BEFORE, Duration.ZERO);

            Instant aMinuteBefore = DEPARTURE.minus(Duration.ofMinutes(1));

            Assertions.assertThatCode(() -> untilDeparture.requireOpenAt(DEPARTURE, aMinuteBefore))
                    .doesNotThrowAnyException();
        }
    }

    private static CheckInWindow window() {
        return new CheckInWindow(OPENS_BEFORE, CLOSES_BEFORE);
    }
}
