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

final class AirlineStack {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";
    private static final String KAFKA_IMAGE = "apache/kafka:4.1.0";

    private static final String FLIGHT_DB_ALIAS = "flight-db";
    private static final String BOOKING_DB_ALIAS = "booking-db";
    private static final String PAYMENT_DB_ALIAS = "payment-db";
    private static final String CHECKIN_DB_ALIAS = "checkin-db";
    private static final String NOTIFICATION_DB_ALIAS = "notification-db";
    private static final String KAFKA_ALIAS = "kafka";
    private static final String FLIGHT_ALIAS = "flight-service";
    private static final String BOOKING_ALIAS = "booking-service";

    private static final int FLIGHT_PORT = 8081;
    private static final int BOOKING_PORT = 8082;
    private static final int PAYMENT_PORT = 8083;
    private static final int CHECKIN_PORT = 8084;
    private static final int NOTIFICATION_PORT = 8085;
    private static final int POSTGRES_PORT = 5432;

    private static final String DATABASE_USER = "airline";
    private static final String DATABASE_PASSWORD = "airline";
    private static final String FLIGHT_DATABASE = "airline_flight";
    private static final String BOOKING_DATABASE = "airline_booking";
    private static final String PAYMENT_DATABASE = "airline_payment";
    private static final String CHECKIN_DATABASE = "airline_checkin";
    private static final String NOTIFICATION_DATABASE = "airline_notification";

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
    private static final PostgreSQLContainer NOTIFICATION_DB =
            database(NOTIFICATION_DB_ALIAS, NOTIFICATION_DATABASE);

    private static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases(KAFKA_ALIAS);

    private static final GenericContainer<?> FLIGHT_SERVICE;
    private static final GenericContainer<?> BOOKING_SERVICE;
    private static final GenericContainer<?> PAYMENT_SERVICE;
    private static final GenericContainer<?> CHECKIN_SERVICE;
    private static final GenericContainer<?> NOTIFICATION_SERVICE;

    static {
        FLIGHT_DB.start();
        BOOKING_DB.start();
        PAYMENT_DB.start();
        CHECKIN_DB.start();
        NOTIFICATION_DB.start();
        KAFKA.start();

        FLIGHT_SERVICE = flightService();
        FLIGHT_SERVICE.start();

        BOOKING_SERVICE = bookingService();
        BOOKING_SERVICE.start();

        PAYMENT_SERVICE = paymentService();
        PAYMENT_SERVICE.start();

        CHECKIN_SERVICE = checkinService();
        CHECKIN_SERVICE.start();

        NOTIFICATION_SERVICE = notificationService();
        NOTIFICATION_SERVICE.start();
    }

    private AirlineStack() {
    }

    static void start() {
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

    static String notificationDatabaseUrl() {
        return jdbcUrlOf(NOTIFICATION_DB, NOTIFICATION_DATABASE);
    }

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

    private static GenericContainer<?> notificationService() {
        return service("notification-service/notification-infrastructure", NOTIFICATION_PORT,
                NOTIFICATION_DB_ALIAS, NOTIFICATION_DATABASE)
                .withEnv("SPRING_KAFKA_BOOTSTRAP-SERVERS", brokerInsideTheNetwork())
                .dependsOn(NOTIFICATION_DB, KAFKA);
    }

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
