package com.programandoenjava.airline.e2e;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * The whole system, on a network of its own: a database per service, a broker,
 * and the four services, arranged the way the compose file arranges them.
 *
 * <p>One Postgres container per service rather than one with a schema each,
 * because a service that cannot reach another's tables is the property being
 * relied on.
 *
 * <p>Started once and left running for every test in the module. Tearing it down
 * between classes would cost minutes, and the tests make their own flights
 * rather than sharing one catalogue.
 *
 * <p>Images are built from the same Dockerfiles the compose file uses, so what
 * runs here is what would be deployed. They copy a jar Maven has already built:
 * without a package first, these tests exercise the previous build.
 */
final class AirlineStack {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";
    private static final String KAFKA_IMAGE = "apache/kafka:4.1.0";

    private static final String FLIGHT_DB_ALIAS = "flight-db";
    private static final String BOOKING_DB_ALIAS = "booking-db";
    private static final String PAYMENT_DB_ALIAS = "payment-db";
    private static final String CHECKIN_DB_ALIAS = "checkin-db";
    private static final String KAFKA_ALIAS = "kafka";
    private static final String FLIGHT_ALIAS = "flight-service";
    private static final String BOOKING_ALIAS = "booking-service";

    private static final int FLIGHT_PORT = 8081;
    private static final int BOOKING_PORT = 8082;
    private static final int PAYMENT_PORT = 8083;
    private static final int CHECKIN_PORT = 8084;
    private static final int POSTGRES_PORT = 5432;

    private static final String DATABASE_USER = "airline";
    private static final String DATABASE_PASSWORD = "airline";
    private static final String FLIGHT_DATABASE = "airline_flight";
    private static final String BOOKING_DATABASE = "airline_booking";
    private static final String PAYMENT_DATABASE = "airline_payment";
    private static final String CHECKIN_DATABASE = "airline_checkin";

    private static final Path REPOSITORY_ROOT = Path.of("..");
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(2);

    private static final Network NETWORK = Network.newNetwork();

    private static final PostgreSQLContainer FLIGHT_DB =
            database(FLIGHT_DB_ALIAS, FLIGHT_DATABASE);
    private static final PostgreSQLContainer BOOKING_DB =
            database(BOOKING_DB_ALIAS, BOOKING_DATABASE);
    private static final PostgreSQLContainer PAYMENT_DB =
            database(PAYMENT_DB_ALIAS, PAYMENT_DATABASE);
    private static final PostgreSQLContainer CHECKIN_DB =
            database(CHECKIN_DB_ALIAS, CHECKIN_DATABASE);

    /*
     * Reached only from inside the network, so no listener is advertised to the
     * host: nothing outside the services needs to speak to it.
     */
    private static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases(KAFKA_ALIAS);

    private static final GenericContainer<?> FLIGHT_SERVICE;
    private static final GenericContainer<?> BOOKING_SERVICE;
    private static final GenericContainer<?> PAYMENT_SERVICE;
    private static final GenericContainer<?> CHECKIN_SERVICE;

    static {
        FLIGHT_DB.start();
        BOOKING_DB.start();
        PAYMENT_DB.start();
        CHECKIN_DB.start();
        KAFKA.start();

        FLIGHT_SERVICE = flightService();
        FLIGHT_SERVICE.start();

        BOOKING_SERVICE = bookingService();
        BOOKING_SERVICE.start();

        PAYMENT_SERVICE = paymentService();
        PAYMENT_SERVICE.start();

        CHECKIN_SERVICE = checkinService();
        CHECKIN_SERVICE.start();
    }

    private AirlineStack() {
    }

    /**
     * Touching any member runs the static initialiser, which is what starts the
     * stack. Called once from a test's setup rather than relied upon by
     * accident.
     */
    static void start() {
        // The class loading did the work.
    }

    static String bookingServiceUrl() {
        return urlOf(BOOKING_SERVICE, BOOKING_PORT);
    }

    static String paymentServiceUrl() {
        return urlOf(PAYMENT_SERVICE, PAYMENT_PORT);
    }

    static String checkinServiceUrl() {
        return urlOf(CHECKIN_SERVICE, CHECKIN_PORT);
    }

    /**
     * The databases as seen from the test, which is outside the network the
     * services share. Postgres answers on a port Docker picked, so these cannot
     * be constants.
     */
    static String flightDatabaseUrl() {
        return jdbcUrlOf(FLIGHT_DB, FLIGHT_DATABASE);
    }

    static String bookingDatabaseUrl() {
        return jdbcUrlOf(BOOKING_DB, BOOKING_DATABASE);
    }

    static String checkinDatabaseUrl() {
        return jdbcUrlOf(CHECKIN_DB, CHECKIN_DATABASE);
    }

    static String databaseUser() {
        return DATABASE_USER;
    }

    static String databasePassword() {
        return DATABASE_PASSWORD;
    }

    private static String urlOf(final GenericContainer<?> container, final int port) {
        String host = container.getHost();
        Integer mapped = container.getMappedPort(port);

        return "http://" + host + ":" + mapped;
    }

    private static String jdbcUrlOf(final PostgreSQLContainer container, final String database) {
        String host = container.getHost();
        Integer port = container.getMappedPort(POSTGRES_PORT);

        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    private static PostgreSQLContainer database(final String alias, final String name) {
        return new PostgreSQLContainer(POSTGRES_IMAGE)
                .withNetwork(NETWORK)
                .withNetworkAliases(alias)
                .withUsername(DATABASE_USER)
                .withPassword(DATABASE_PASSWORD)
                .withDatabaseName(name);
    }

    private static GenericContainer<?> flightService() {
        return service("flight-service/flight-infrastructure", FLIGHT_PORT,
                FLIGHT_DB_ALIAS, FLIGHT_DATABASE)
                .withNetworkAliases(FLIGHT_ALIAS)
                .dependsOn(FLIGHT_DB);
    }

    private static GenericContainer<?> bookingService() {
        Map<String, String> environment = Map.of(
                "AIRLINE_FLIGHT-SERVICE_URL", "http://" + FLIGHT_ALIAS + ":" + FLIGHT_PORT,
                "SPRING_KAFKA_BOOTSTRAP-SERVERS", brokerInsideTheNetwork());

        return service("booking-service/booking-infrastructure", BOOKING_PORT,
                BOOKING_DB_ALIAS, BOOKING_DATABASE)
                .withNetworkAliases(BOOKING_ALIAS)
                .withEnv(environment)
                .dependsOn(BOOKING_DB, KAFKA, FLIGHT_SERVICE);
    }

    private static GenericContainer<?> paymentService() {
        Map<String, String> environment = Map.of(
                "AIRLINE_BOOKING-SERVICE_URL", "http://" + BOOKING_ALIAS + ":" + BOOKING_PORT,
                "SPRING_KAFKA_BOOTSTRAP-SERVERS", brokerInsideTheNetwork());

        return service("payment-service/payment-infrastructure", PAYMENT_PORT,
                PAYMENT_DB_ALIAS, PAYMENT_DATABASE)
                .withEnv(environment)
                .dependsOn(PAYMENT_DB, KAFKA, BOOKING_SERVICE);
    }

    /*
     * Reads both of the services before it and writes nothing they read, so it
     * is last to start and nothing waits on it.
     */
    private static GenericContainer<?> checkinService() {
        Map<String, String> environment = Map.of(
                "AIRLINE_BOOKING-SERVICE_URL", "http://" + BOOKING_ALIAS + ":" + BOOKING_PORT,
                "AIRLINE_FLIGHT-SERVICE_URL", "http://" + FLIGHT_ALIAS + ":" + FLIGHT_PORT,
                "SPRING_KAFKA_BOOTSTRAP-SERVERS", brokerInsideTheNetwork());

        return service("checkin-service/checkin-infrastructure", CHECKIN_PORT,
                CHECKIN_DB_ALIAS, CHECKIN_DATABASE)
                .withEnv(environment)
                .dependsOn(CHECKIN_DB, KAFKA, BOOKING_SERVICE, FLIGHT_SERVICE);
    }

    /*
     * The alias and the port the broker listens on inside the network, not the
     * one Testcontainers maps for the host: the services are on the network and
     * the mapped port would not reach it from there.
     */
    private static String brokerInsideTheNetwork() {
        return KAFKA_ALIAS + ":9093";
    }

    private static GenericContainer<?> service(final String module,
                                               final int port,
                                               final String databaseAlias,
                                               final String database) {
        Path context = REPOSITORY_ROOT.resolve(module);
        ImageFromDockerfile image = new ImageFromDockerfile()
                .withDockerfile(context.resolve("Dockerfile"));

        String url = "jdbc:postgresql://" + databaseAlias + ":" + POSTGRES_PORT + "/" + database;

        return new GenericContainer<>(image)
                .withNetwork(NETWORK)
                .withExposedPorts(port)
                .withEnv("SPRING_DATASOURCE_URL", url)
                .withEnv("SPRING_DATASOURCE_USERNAME", DATABASE_USER)
                .withEnv("SPRING_DATASOURCE_PASSWORD", DATABASE_PASSWORD)
                .waitingFor(Wait.forHttp("/actuator/health").forPort(port).forStatusCode(200))
                .withStartupTimeout(STARTUP_TIMEOUT);
    }
}
