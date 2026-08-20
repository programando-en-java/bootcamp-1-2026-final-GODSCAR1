package com.programandoenjava.airline.payment.infrastructure.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.programandoenjava.airline.payment.EnableDatabaseTest;
import com.programandoenjava.airline.payment.TestcontainersConfiguration;
import com.programandoenjava.airline.payment.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.payment.application.port.out.readbooking.BookingToPay;
import com.programandoenjava.airline.payment.application.port.out.readbooking.ReadBookingPort;
import com.programandoenjava.airline.payment.application.port.out.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.payment.domain.payment.BookingId;
import com.programandoenjava.airline.payment.domain.payment.PassengerId;
import com.programandoenjava.airline.payment.domain.shared.Money;
import com.programandoenjava.airline.payment.infrastructure.adapter.out.gateway.GatewayConfiguration;
import com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.payment.PaymentPersistenceConfiguration;
import com.programandoenjava.airline.payment.infrastructure.config.ApplicationConfiguration;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * booking-service is mocked; the gateway is not. The stand-in gateway decides by
 * card number rather than by chance (ADR-013), so it is deterministic enough to
 * exercise both outcomes for real — mocking it would only prove the mock.
 */
@SpringBootTest(classes = {
        PaymentController.class,
        GlobalExceptionHandler.class,
        ApplicationConfiguration.class,
        GatewayConfiguration.class,
        PaymentPersistenceConfiguration.class
})
@EnableDatabaseTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        ValidationAutoConfiguration.class,
        JacksonAutoConfiguration.class
})
@AutoConfigureMockMvc
@Sql(scripts = "/db/testdata/R__reset_payments.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Pay booking slice (hand-built, no full context)")
class PayBookingSliceTest {

    private static final String PAYMENTS = "/api/v1/payments";

    private static final String GOOD_CARD = "4242424242424242";
    private static final String DECLINING_CARD = "4000000000000002";

    private static final String TOTAL = "500000.00";
    private static final double TOTAL_AS_NUMBER = 500_000.00;
    private static final double A_CENT = 0.001;
    private static final String COP = "COP";

    private static final String PENDING = "PENDING";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String SUCCEEDED = "SUCCEEDED";
    private static final String FAILED = "FAILED";

    private static final String PROCESSED_AT = "2026-03-10T12:00:00Z";

    private static final String COUNT_PAYMENTS = "SELECT count(*) FROM payments";
    private static final String FIND_STATUS =
            "SELECT status FROM payments WHERE booking_id = ?";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ReadBookingPort readBookingPort;

    /*
     * The outbox is not this slice's business. Mocking the publisher stops the
     * listener running, and with it the Kafka machinery a web slice has no
     * reason to start.
     */
    @MockitoBean
    private DomainEventPublisher domainEventPublisher;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse(PROCESSED_AT), ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("when the card is good")
    class Charged {

        @Test
        @DisplayName("should answer with a payment that succeeded")
        void shouldAnswerWithAPaymentThatSucceeded() throws Exception {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            mockMvc.perform(pay(booking, GOOD_CARD))
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.paymentId").isNotEmpty())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.bookingId")
                            .value(booking.toString()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(SUCCEEDED));
        }

        /*
         * The amount comes from the booking, not from the request. There is
         * nowhere in the request to put one, which is the point: a caller cannot
         * decide what it owes.
         */
        @Test
        @DisplayName("should charge what the booking says is owed")
        void shouldChargeWhatTheBookingSaysIsOwed() throws Exception {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            mockMvc.perform(pay(booking, GOOD_CARD))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.amount")
                            .value(Matchers.closeTo(TOTAL_AS_NUMBER, A_CENT)))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.currency").value(COP));
        }

        @Test
        @DisplayName("should record when it was taken, by the application's clock")
        void shouldRecordWhenItWasTaken() throws Exception {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            mockMvc.perform(pay(booking, GOOD_CARD))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.processedAt")
                            .value(PROCESSED_AT));
        }

        @Test
        @DisplayName("should keep the payment")
        void shouldKeepThePayment() throws Exception {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            mockMvc.perform(pay(booking, GOOD_CARD));

            Assertions.assertThat(statusOf(booking)).isEqualTo(SUCCEEDED);
        }
    }

    @Nested
    @DisplayName("when the card is declined")
    class Declined {

        /*
         * 201, not 402. The charge was attempted and the answer was no, which is
         * a payment that happened rather than a request that failed. Its status
         * is what says which, and it is what the saga will announce.
         */
        @Test
        @DisplayName("should answer with a payment that failed")
        void shouldAnswerWithAPaymentThatFailed() throws Exception {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            mockMvc.perform(pay(booking, DECLINING_CARD))
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(FAILED));
        }

        @Test
        @DisplayName("should keep the refusal")
        void shouldKeepTheRefusal() throws Exception {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            mockMvc.perform(pay(booking, DECLINING_CARD));

            Assertions.assertThat(statusOf(booking)).isEqualTo(FAILED);
        }

        @Test
        @DisplayName("should record the refusal as fully as a charge")
        void shouldRecordTheRefusalAsFullyAsACharge() throws Exception {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            mockMvc.perform(pay(booking, DECLINING_CARD))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.amount")
                            .value(Matchers.closeTo(TOTAL_AS_NUMBER, A_CENT)))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.cardLastFourDigits")
                            .value("0002"));
        }
    }

    @Nested
    @DisplayName("keeping the card to itself")
    class Hiding {

        /*
         * Worth more than it looks. Add the number to the response DTO and this
         * is the only test that would say so.
         */
        @Test
        @DisplayName("should answer with the last four digits and nothing more")
        void shouldAnswerWithTheLastFourDigitsAndNothingMore() throws Exception {
            UUID booking = aBooking();
            givenBookingIsPayable(booking);

            String body = mockMvc.perform(pay(booking, GOOD_CARD))
                    .andReturn().getResponse().getContentAsString();

            Assertions.assertThat(body).doesNotContain(GOOD_CARD);

            String lastFour = JsonPath.read(body, "$.cardLastFourDigits");
            Assertions.assertThat(lastFour).isEqualTo("4242");
        }
    }

    @Nested
    @DisplayName("when the booking cannot be paid")
    class NotPayable {

        @Test
        @DisplayName("should refuse a booking that is already confirmed")
        void shouldRefuseABookingThatIsAlreadyConfirmed() throws Exception {
            UUID booking = aBooking();
            givenBookingIs(booking, CONFIRMED);

            mockMvc.perform(pay(booking, GOOD_CARD))
                    .andExpect(MockMvcResultMatchers.status().isConflict())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title")
                            .value("Booking cannot be paid"));
        }

        @Test
        @DisplayName("should record nothing when it refuses")
        void shouldRecordNothingWhenItRefuses() throws Exception {
            UUID booking = aBooking();
            givenBookingIs(booking, CONFIRMED);

            mockMvc.perform(pay(booking, GOOD_CARD));

            Assertions.assertThat(paymentCount()).isZero();
        }

        @Test
        @DisplayName("should answer nothing for a booking that does not exist")
        void shouldAnswerNothingForABookingThatDoesNotExist() throws Exception {
            UUID unknown = aBooking();
            BookingId id = new BookingId(unknown);
            BDDMockito.given(readBookingPort.byId(BDDMockito.any()))
                    .willThrow(new BookingNotFoundException(id));

            mockMvc.perform(pay(unknown, GOOD_CARD))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }
    }

    @Nested
    @DisplayName("rejecting bad input")
    class RejectingBadInput {

        @Test
        @DisplayName("should reject a card number that fails its check digit")
        void shouldRejectACardNumberThatFailsItsCheckDigit() throws Exception {
            mockMvc.perform(pay(aBooking(), "4242424242424243"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                            .value(Matchers.containsString("not valid")));
        }

        @Test
        @DisplayName("should reject something too short to be a card")
        void shouldRejectSomethingTooShortToBeACard() throws Exception {
            mockMvc.perform(pay(aBooking(), "4242"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should reject a request with no card")
        void shouldRejectARequestWithNoCard() throws Exception {
            String body = """
                    {"bookingId": "%s"}
                    """.formatted(aBooking());

            mockMvc.perform(MockMvcRequestBuilders.post(PAYMENTS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should reject a request with no booking")
        void shouldRejectARequestWithNoBooking() throws Exception {
            String body = """
                    {"cardNumber": "%s"}
                    """.formatted(GOOD_CARD);

            mockMvc.perform(MockMvcRequestBuilders.post(PAYMENTS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should not ask booking-service for a request it refuses")
        void shouldNotAskBookingServiceForARequestItRefuses() throws Exception {
            mockMvc.perform(pay(aBooking(), "4242"));

            BDDMockito.then(readBookingPort).shouldHaveNoInteractions();
        }
    }

    private void givenBookingIsPayable(final UUID bookingId) {
        givenBookingIs(bookingId, PENDING);
    }

    private void givenBookingIs(final UUID bookingId, final String status) {
        BookingId id = new BookingId(bookingId);
        PassengerId passenger = new PassengerId(UUID.randomUUID());
        Money total = Money.of(TOTAL, COP);
        BookingToPay booking = new BookingToPay(id, passenger, total, status);

        BDDMockito.given(readBookingPort.byId(BDDMockito.any())).willReturn(booking);
    }

    private MockHttpServletRequestBuilder pay(final UUID bookingId, final String cardNumber) {
        String body = """
                {"bookingId": "%s", "cardNumber": "%s"}
                """.formatted(bookingId, cardNumber);

        return MockMvcRequestBuilders.post(PAYMENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private long paymentCount() {
        return jdbcTemplate.queryForObject(COUNT_PAYMENTS, Long.class);
    }

    private String statusOf(final UUID bookingId) {
        return jdbcTemplate.queryForObject(FIND_STATUS, String.class, bookingId);
    }

    private static UUID aBooking() {
        return UUID.randomUUID();
    }
}
