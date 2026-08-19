package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.outbox;

import com.programandoenjava.airline.payment.EnableDatabaseTest;
import com.programandoenjava.airline.payment.TestcontainersConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.kafka.KafkaContainer;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * The one place a message actually leaves this service.
 *
 * <p>A real broker, because what is being checked is that the broker received
 * something: a mocked template would only prove a method was called. It is also
 * the only place {@code claimPending} runs, and with it the SKIP LOCKED that
 * keeps two replicas from sending everything twice.
 *
 * <p>The broker outlives each test, so the topic holds what earlier cases left
 * on it. Every assertion here therefore names the aggregate it is about and
 * ignores the rest, rather than taking whichever message arrives first.
 */
@SpringBootTest(classes = OutboxConfiguration.class)
@EnableDatabaseTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@Sql(scripts = "/db/testdata/R__reset_payments.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Outbox relay")
class OutboxRelayTest {

    private static final String TOPIC = "payment.succeeded.v1";
    private static final String AGGREGATE_TYPE = "payment";

    private static final String INSERT_MESSAGE = """
            INSERT INTO outbox (id, aggregate_type, aggregate_id, topic, payload, created_at)
            VALUES (?::uuid, ?, ?, ?, ?::jsonb, ?)
            """;
    private static final String COUNT_UNSENT =
            "SELECT count(*) FROM outbox WHERE published_at IS NULL";
    private static final String FIND_SENT_AT =
            "SELECT published_at FROM outbox WHERE id = ?::uuid";

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration SINGLE_POLL = Duration.ofMillis(500);

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /*
     * The container rather than KafkaProperties: @ServiceConnection sets the
     * property for beans the context builds, and this consumer is built by hand.
     */
    @Autowired
    private KafkaContainer kafkaContainer;

    /* The relay stamps published_at with this. */
    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.systemUTC();
    }

    private KafkaConsumer<String, String> consumer;

    @BeforeEach
    void subscribe() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaContainer.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "relay-test-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());

        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(List.of(TOPIC));
    }

    @AfterEach
    void unsubscribe() {
        consumer.close();
    }

    @Nested
    @DisplayName("when messages are waiting")
    class Sending {

        @Test
        @DisplayName("should put the payload on the topic it names")
        void shouldPutThePayloadOnTheTopicItNames() {
            String booking = aBooking();
            String payload = """
                    {"eventId": "%s", "bookingId": "%s"}
                    """.formatted(UUID.randomUUID(), booking);
            aMessage(booking, payload);

            outboxRelay.publishPending();

            ConsumerRecord<String, String> received = messageFor(booking);

            Assertions.assertThat(received.topic()).isEqualTo(TOPIC);
            Assertions.assertThat(received.value()).contains("eventId");
        }

        /*
         * Keying by aggregate is what puts every message about one booking on
         * the same partition, and with it in the order they were written.
         */
        @Test
        @DisplayName("should key the message by the aggregate it is about")
        void shouldKeyTheMessageByTheAggregateItIsAbout() {
            String booking = aBooking();
            aMessage(booking, "{}");

            outboxRelay.publishPending();

            Assertions.assertThat(messageFor(booking).key()).isEqualTo(booking);
        }

        @Test
        @DisplayName("should mark what it sent, so it does not send it again")
        void shouldMarkWhatItSent() {
            String booking = aBooking();
            String id = aMessage(booking, "{}");

            outboxRelay.publishPending();

            Assertions.assertThat(sentAt(id)).isNotNull();
            Assertions.assertThat(unsentCount()).isZero();
        }

        @Test
        @DisplayName("should send everything that is waiting")
        void shouldSendEverythingThatIsWaiting() {
            aMessage(aBooking(), "{}");
            aMessage(aBooking(), "{}");
            aMessage(aBooking(), "{}");

            outboxRelay.publishPending();

            Assertions.assertThat(unsentCount()).isZero();
        }

        /*
         * A second sweep must not resend what has gone. Asserted against the
         * table rather than the topic, because the topic also holds what other
         * tests put there — and what "sent twice" would mean is two rows going
         * out, not two messages arriving.
         */
        @Test
        @DisplayName("should leave nothing to send the second time round")
        void shouldLeaveNothingToSendTheSecondTimeRound() {
            String booking = aBooking();
            String id = aMessage(booking, "{}");

            outboxRelay.publishPending();
            Instant firstSweep = sentAt(id);

            outboxRelay.publishPending();

            Assertions.assertThat(sentAt(id)).isEqualTo(firstSweep);
            Assertions.assertThat(unsentCount()).isZero();
        }
    }

    @Nested
    @DisplayName("when the outbox is empty")
    class Idle {

        @Test
        @DisplayName("should do nothing at all")
        void shouldDoNothingAtAll() {
            outboxRelay.publishPending();

            Assertions.assertThat(unsentCount()).isZero();
        }
    }

    private String aMessage(final String aggregateId, final String payload) {
        String id = UUID.randomUUID().toString();
        Timestamp now = Timestamp.from(Instant.now());

        jdbcTemplate.update(INSERT_MESSAGE,
                id, AGGREGATE_TYPE, aggregateId, TOPIC, payload, now);

        return id;
    }

    /*
     * Polls in a loop and keeps only what this test is about. A consumer's first
     * poll usually returns nothing while it is still being assigned partitions,
     * and the ones after that return whatever else is on the topic.
     */
    private ConsumerRecord<String, String> messageFor(final String aggregateId) {
        Instant deadline = Instant.now().plus(POLL_TIMEOUT);

        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, String> records = consumer.poll(SINGLE_POLL);
            Optional<ConsumerRecord<String, String>> mine = findKey(records, aggregateId);
            if (mine.isPresent()) {
                return mine.get();
            }
        }

        throw new AssertionError("No message for " + aggregateId + " on " + TOPIC);
    }

    private static Optional<ConsumerRecord<String, String>> findKey(
            final ConsumerRecords<String, String> records, final String key) {

        for (ConsumerRecord<String, String> record : records) {
            if (key.equals(record.key())) {
                return Optional.of(record);
            }
        }

        return Optional.empty();
    }

    private Instant sentAt(final String id) {
        Map<String, Object> row = jdbcTemplate.queryForMap(FIND_SENT_AT, id);
        Object value = row.get("published_at");

        return value == null ? null : ((Timestamp) value).toInstant();
    }

    private long unsentCount() {
        return jdbcTemplate.queryForObject(COUNT_UNSENT, Long.class);
    }

    private static String aBooking() {
        return UUID.randomUUID().toString();
    }
}
