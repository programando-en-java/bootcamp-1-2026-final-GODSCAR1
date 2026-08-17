package com.programandoenjava.airline.flight.domain;

import com.programandoenjava.airline.flight.domain.shared.AirportCode;
import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Airport code")
class AirportCodeTest {

    @Nested
    @DisplayName("when the value is a valid IATA code")
    class ValidCode {

        @Test
        @DisplayName("should normalise it to upper case")
        void shouldNormaliseItToUpperCase() {
            AirportCode code = new AirportCode("bog");

            Assertions.assertThat(code.value()).isEqualTo("BOG");
        }

        @Test
        @DisplayName("should ignore surrounding whitespace")
        void shouldIgnoreSurroundingWhitespace() {
            AirportCode code = new AirportCode("  mde  ");

            Assertions.assertThat(code.value()).isEqualTo("MDE");
        }

        @Test
        @DisplayName("should treat two spellings of the same code as equal")
        void shouldTreatTwoSpellingsOfTheSameCodeAsEqual() {
            AirportCode lowercase = new AirportCode("bog");
            AirportCode uppercase = new AirportCode("BOG");

            Assertions.assertThat(lowercase).isEqualTo(uppercase);
        }
    }

    @Nested
    @DisplayName("when the value is not a valid IATA code")
    class InvalidCode {

        @Test
        @DisplayName("should reject a code longer than three letters")
        void shouldRejectACodeLongerThanThreeLetters() {
            Assertions.assertThatThrownBy(() -> new AirportCode("BOGOTA"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("three letters");
        }

        @Test
        @DisplayName("should reject a code containing digits")
        void shouldRejectACodeContainingDigits() {
            Assertions.assertThatThrownBy(() -> new AirportCode("B0G"))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("should keep the original spelling in the error message")
        void shouldKeepTheOriginalSpellingInTheErrorMessage() {
            Assertions.assertThatThrownBy(() -> new AirportCode("  bogota  "))
                    .hasMessageContaining("  bogota  ");
        }

        @Test
        @DisplayName("should reject a blank value")
        void shouldRejectABlankValue() {
            Assertions.assertThatThrownBy(() -> new AirportCode("   "))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("should reject null")
        void shouldRejectNull() {
            Assertions.assertThatThrownBy(() -> new AirportCode(null))
                    .isInstanceOf(DomainValidationException.class);
        }
    }
}
