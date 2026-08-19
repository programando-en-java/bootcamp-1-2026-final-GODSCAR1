package com.programandoenjava.airline.payment.domain.payment;

import com.programandoenjava.airline.payment.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Card number")
class CardNumberTest {

    private static final String VALID = "4242424242424242";
    private static final String DECLINING = "4000000000000002";

    @Nested
    @DisplayName("when reading what the passenger typed")
    class Accepting {

        @Test
        @DisplayName("should ignore the spaces a passenger types between groups")
        void shouldIgnoreTheSpacesAPassengerTypesBetweenGroups() {
            CardNumber spaced = new CardNumber("4242 4242 4242 4242");

            Assertions.assertThat(spaced).isEqualTo(new CardNumber(VALID));
        }

        @Test
        @DisplayName("should ignore hyphens as well")
        void shouldIgnoreHyphensAsWell() {
            CardNumber hyphenated = new CardNumber("4242-4242-4242-4242");

            Assertions.assertThat(hyphenated).isEqualTo(new CardNumber(VALID));
        }

        /*
         * The card the gateway declines has to be a real number, or the refusal
         * would come from validation rather than from the charge, and US-006
         * would be testing the wrong thing.
         */
        @Test
        @DisplayName("should accept the number the gateway is set up to decline")
        void shouldAcceptTheNumberTheGatewayIsSetUpToDecline() {
            CardNumber declining = new CardNumber(DECLINING);

            Assertions.assertThat(declining.lastFourDigits()).isEqualTo("0002");
        }
    }

    @Nested
    @DisplayName("when the number is not one")
    class Rejecting {

        /*
         * The check digit every card carries. Without it a mistyped number
         * reaches the gateway and comes back as a refusal, which tells the
         * passenger the wrong thing.
         */
        @Test
        @DisplayName("should reject a number with a digit mistyped")
        void shouldRejectANumberWithADigitMistyped() {
            Assertions.assertThatThrownBy(() -> new CardNumber("4242424242424243"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("not valid");
        }

        @Test
        @DisplayName("should reject something too short to be a card")
        void shouldRejectSomethingTooShortToBeACard() {
            Assertions.assertThatThrownBy(() -> new CardNumber("42424242"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("13 to 19 digits");
        }

        @Test
        @DisplayName("should reject letters")
        void shouldRejectLetters() {
            Assertions.assertThatThrownBy(() -> new CardNumber("4242abcd42424242"))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("should reject nothing at all")
        void shouldRejectNothingAtAll() {
            Assertions.assertThatThrownBy(() -> new CardNumber(""))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("required");
        }
    }

    @Nested
    @DisplayName("when handing the number out")
    class Hiding {

        @Test
        @DisplayName("should give back only the last four digits")
        void shouldGiveBackOnlyTheLastFourDigits() {
            CardNumber card = new CardNumber(VALID);

            Assertions.assertThat(card.lastFourDigits()).isEqualTo("4242");
        }

        /*
         * The one test worth more than it looks. Remove toString and the full
         * number starts appearing wherever the object is concatenated, and
         * nothing else in the suite would say so.
         */
        @Test
        @DisplayName("should not print the number it holds")
        void shouldNotPrintTheNumberItHolds() {
            CardNumber card = new CardNumber(VALID);

            String printed = String.valueOf(card);

            Assertions.assertThat(printed).doesNotContain(VALID).isEqualTo("****4242");
        }
    }
}
