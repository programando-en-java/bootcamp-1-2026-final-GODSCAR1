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

/**
 * What the payment slice cannot say, because it mocks the publisher: that a
 * settled payment leaves a message behind, and that the message shares the
 * payment's transaction.
 *
 * <p>Kafka is absent on purpose. Sending is the relay's job and has its own
 * test; this one is about the row, which is what a crash after the charge would
 * otherwise lose.
 */
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

    /*
     * A spy rather than a mock: the save has to work for most tests, and be made
     * to fail for the one that checks the two writes share a transaction.
     */
    @MockitoSpyBean
    private SavePaymentPort savePaymentPort;

    /*
     * The relay is a bean of OutboxConfiguration and cannot be built without a
     * template. Nothing here calls it: sending is its own concern, with its own
     * test, and what this one is about is the row the listener leaves behind.
     */
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

        /*
         * A refusal is announced too. Without it the saga would never learn that
         * the seats it is holding are not going to be paid for.
         */
        @Test
        @DisplayName("should announce a refusal on the failed topic")
        void shouldAnnounceARefusalOnTheFailedTopic() {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            pay(booking, DECLINING_CARD);

            Assertions.assertThat(topicOfTheOnlyMessage()).isEqualTo(FAILED_TOPIC);
        }

        /*
         * Keying by booking is what puts every message about one booking on the
         * same partition, and with it in order.
         */
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

        /*
         * The integration event is flat and made of primitives, so nothing about
         * the card can reach it even by accident. This is the test that says so.
         */
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

        /*
         * The reason the listener runs BEFORE_COMMIT. If the two writes were in
         * separate transactions, this would announce a charge that never
         * happened — and nothing downstream could tell the difference.
         */
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
        BookingId id = new BookingId(bookingId);
        Money total = Money.of(TOTAL, COP);
        BookingToPay booking = new BookingToPay(id, total, PENDING);

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
