package com.programandoenjava.airline.payment.infrastructure.adapter.out.events;

import com.jayway.jsonpath.JsonPath;
import com.programandoenjava.airline.payment.EnableDatabaseTest;
import com.programandoenjava.airline.payment.TestcontainersConfiguration;
import com.programandoenjava.airline.payment.application.port.in.paybooking.PayBookingCommand;
import com.programandoenjava.airline.payment.application.port.in.paybooking.PayBookingUseCase;
import com.programandoenjava.airline.payment.application.port.out.savepayment.SavePaymentPort;
import com.programandoenjava.airline.payment.application.port.out.readbooking.BookingToPay;
import com.programandoenjava.airline.payment.application.port.out.readbooking.ReadBookingPort;
import com.programandoenjava.airline.payment.domain.payment.BookingId;
import com.programandoenjava.airline.payment.domain.payment.PassengerId;
import com.programandoenjava.airline.payment.domain.payment.CardNumber;
import com.programandoenjava.airline.payment.domain.shared.Money;
import com.programandoenjava.airline.payment.infrastructure.adapter.out.gateway.GatewayConfiguration;
import com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.outbox.OutboxConfiguration;
import com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.payment.PaymentPersistenceConfiguration;
import com.programandoenjava.airline.payment.infrastructure.config.ApplicationConfiguration;
import com.programandoenjava.airline.payment.infrastructure.transaction.TransactionSupportConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.mockito.BDDMockito;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(classes = {
        ApplicationConfiguration.class,
        GatewayConfiguration.class,
        PaymentPersistenceConfiguration.class,
        OutboxConfiguration.class,
        EventsConfiguration.class,
        TransactionSupportConfiguration.class
})
@EnableDatabaseTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Sql(scripts = "/db/testdata/R__reset_payments.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Payment outbox (hand-built, no full context)")
class PaymentOutboxSliceTest {

    private static final String GOOD_CARD = "4242424242424242";
    private static final String DECLINING_CARD = "4000000000000002";

    private static final String TOTAL = "500000.00";
    private static final String COP = "COP";
    private static final String PENDING = "PENDING";

    private static final String SUCCEEDED_TOPIC = "payment.succeeded.v1";
    private static final String FAILED_TOPIC = "payment.failed.v1";

    private static final String FIND_MESSAGES = """
            SELECT topic, aggregate_type, aggregate_id, payload, published_at
            FROM outbox ORDER BY created_at
            """;
    private static final String COUNT_MESSAGES = "SELECT count(*) FROM outbox";
    private static final String COUNT_PAYMENTS = "SELECT count(*) FROM payments";

    @Autowired
    private PayBookingUseCase payBookingUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ReadBookingPort readBookingPort;

    @MockitoSpyBean
    private SavePaymentPort savePaymentPort;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Nested
    @DisplayName("when a payment settles")
    class Settled {

        @Test
        @DisplayName("should leave one message behind")
        void shouldLeaveOneMessageBehind() {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            pay(booking, GOOD_CARD);

            Assertions.assertThat(messageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should announce a charge on the succeeded topic")
        void shouldAnnounceAChargeOnTheSucceededTopic() {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            pay(booking, GOOD_CARD);

            Assertions.assertThat(topicOfTheOnlyMessage()).isEqualTo(SUCCEEDED_TOPIC);
        }

        @Test
        @DisplayName("should announce a refusal on the failed topic")
        void shouldAnnounceARefusalOnTheFailedTopic() {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            pay(booking, DECLINING_CARD);

            Assertions.assertThat(topicOfTheOnlyMessage()).isEqualTo(FAILED_TOPIC);
        }

        @Test
        @DisplayName("should key the message by the booking it is about")
        void shouldKeyTheMessageByTheBookingItIsAbout() {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            pay(booking, GOOD_CARD);

            Map<String, Object> message = theOnlyMessage();

            Assertions.assertThat(message.get("aggregate_id")).isEqualTo(booking.toString());
            Assertions.assertThat(message.get("aggregate_type")).isEqualTo("payment");
        }

        @Test
        @DisplayName("should leave it unsent for the relay to pick up")
        void shouldLeaveItUnsentForTheRelayToPickUp() {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            pay(booking, GOOD_CARD);

            Assertions.assertThat(theOnlyMessage().get("published_at")).isNull();
        }
    }

    @Nested
    @DisplayName("what the message carries")
    class Payload {

        @Test
        @DisplayName("should carry an event id, so a consumer can recognise a repeat")
        void shouldCarryAnEventId() {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            pay(booking, GOOD_CARD);

            String eventId = JsonPath.read(payloadOfTheOnlyMessage(), "$.eventId");

            Assertions.assertThat(eventId).isNotBlank();
        }

        @Test
        @DisplayName("should carry the booking and what was charged")
        void shouldCarryTheBookingAndWhatWasCharged() {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            pay(booking, GOOD_CARD);

            String payload = payloadOfTheOnlyMessage();

            String bookingId = JsonPath.read(payload, "$.bookingId");
            String currency = JsonPath.read(payload, "$.currency");

            Assertions.assertThat(bookingId).isEqualTo(booking.toString());
            Assertions.assertThat(currency).isEqualTo(COP);
        }

        @Test
        @DisplayName("should name the passenger whose booking was charged")
        void shouldNameThePassengerWhoseBookingWasCharged() {
            UUID booking = aBooking();
            UUID passenger = UUID.randomUUID();
            givenBookingBelongsTo(booking, passenger);

            pay(booking, GOOD_CARD);

            String named = JsonPath.read(payloadOfTheOnlyMessage(), "$.passengerId");

            Assertions.assertThat(named).isEqualTo(passenger.toString());
        }

        @Test
        @DisplayName("should name the passenger on a refusal too")
        void shouldNameThePassengerOnARefusalToo() {
            UUID booking = aBooking();
            UUID passenger = UUID.randomUUID();
            givenBookingBelongsTo(booking, passenger);

            pay(booking, DECLINING_CARD);

            String named = JsonPath.read(payloadOfTheOnlyMessage(), "$.passengerId");

            Assertions.assertThat(named).isEqualTo(passenger.toString());
        }

        @Test
        @DisplayName("should carry nothing about the card")
        void shouldCarryNothingAboutTheCard() {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            pay(booking, GOOD_CARD);

            String payload = payloadOfTheOnlyMessage();

            Assertions.assertThat(payload)
                    .doesNotContain(GOOD_CARD)
                    .doesNotContain("4242")
                    .doesNotContain("card");
        }
    }

    @Nested
    @DisplayName("when the payment cannot be written")
    class Rollback {

        @Test
        @DisplayName("should announce nothing")
        void shouldAnnounceNothing() {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);
            givenTheSaveFails();

            Assertions.assertThatThrownBy(() -> pay(booking, GOOD_CARD))
                    .isInstanceOf(RuntimeException.class);

            Assertions.assertThat(messageCount()).isZero();
            Assertions.assertThat(paymentCount()).isZero();
        }
    }

    private void pay(final UUID bookingId, final String cardNumber) {
        BookingId id = new BookingId(bookingId);
        CardNumber card = new CardNumber(cardNumber);
        PayBookingCommand command = new PayBookingCommand(id, card);

        payBookingUseCase.pay(command);
    }

    private void givenBookingIsPayable(final UUID bookingId) {
        givenBookingBelongsTo(bookingId, UUID.randomUUID());
    }

    private void givenBookingBelongsTo(final UUID bookingId, final UUID passengerId) {
        BookingId id = new BookingId(bookingId);
        PassengerId passenger = new PassengerId(passengerId);
        Money total = Money.of(TOTAL, COP);
        BookingToPay booking = new BookingToPay(id, passenger, total, PENDING);

        BDDMockito.given(readBookingPort.byId(BDDMockito.any())).willReturn(booking);
    }

    private void givenTheSaveFails() {
        BDDMockito.willThrow(new IllegalStateException("the database said no"))
                .given(savePaymentPort).save(BDDMockito.any());
    }

    private Map<String, Object> theOnlyMessage() {
        List<Map<String, Object>> messages = jdbcTemplate.queryForList(FIND_MESSAGES);

        Assertions.assertThat(messages).hasSize(1);

        return messages.getFirst();
    }

    private String topicOfTheOnlyMessage() {
        return String.valueOf(theOnlyMessage().get("topic"));
    }

    private String payloadOfTheOnlyMessage() {
        return String.valueOf(theOnlyMessage().get("payload"));
    }

    private long messageCount() {
        return jdbcTemplate.queryForObject(COUNT_MESSAGES, Long.class);
    }

    private long paymentCount() {
        return jdbcTemplate.queryForObject(COUNT_PAYMENTS, Long.class);
    }

    private static UUID aBooking() {
        return UUID.randomUUID();
    }
}
