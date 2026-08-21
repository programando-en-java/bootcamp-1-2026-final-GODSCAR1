package com.programandoenjava.airline.flight.domain;

import com.programandoenjava.airline.flight.domain.flight.FlightNumber;
import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Flight flightNumber")
class FlightNumberTest {

    @Test
    @DisplayName("should accept a two letter airline with four digits")
    void shouldAcceptATwoLetterAirlineWithFourDigits() {
        FlightNumber number = new FlightNumber("AV8001");

        Assertions.assertThat(number.value()).isEqualTo("AV8001");
    }

    @Test
    @DisplayName("should accept the single digit an airline may use")
    void shouldAcceptTheSingleDigitAnAirlineMayUse() {
        FlightNumber number = new FlightNumber("LA1");

        Assertions.assertThat(number.value()).isEqualTo("LA1");
    }

    @Test
    @DisplayName("should accept a numeric airline code")
    void shouldAcceptANumericAirlineCode() {
        FlightNumber number = new FlightNumber("9B123");

        Assertions.assertThat(number.value()).isEqualTo("9B123");
    }

    @Test
    @DisplayName("should reject a designator with no digits")
    void shouldRejectADesignatorWithNoDigits() {
        Assertions.assertThatThrownBy(() -> new FlightNumber("AV"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("should reject more digits than a designator carries")
    void shouldRejectMoreDigitsThanADesignatorCarries() {
        Assertions.assertThatThrownBy(() -> new FlightNumber("AV80015"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("should reject an airline name in place of a code")
    void shouldRejectAnAirlineNameInPlaceOfACode() {
        Assertions.assertThatThrownBy(() -> new FlightNumber("AVIANCA123"))
                .isInstanceOf(DomainValidationException.class);
    }
}