package com.programandoenjava.airline.e2e;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;
import java.time.Duration;

/**
 * The whole system, on a network of its own: a database per service, and the two
 * services, arranged the way the compose file arranges them.
 *
 * <p>Two Postgres containers rather than one with two schemas, because a service
 * that cannot reach another's tables is the property being relied on. One
 * instance would let a stray query cross the boundary and nothing would notice
 * until the two were deployed apart.
 *
 * <p>Started once and left running for every test in the module. Tearing it down
 * between classes would cost a minute each time, and the tests make their own
 * flights rather than sharing one catalogue.
 *
 * <p>Images are built from the same Dockerfiles the compose file uses, so what
 * runs here is what would be deployed. They copy a jar Maven has already built:
 * without a package first, these tests exercise the previous build.
 */
final class AirlineStack {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final String FLIGHT_DB_ALIAS = "flight-db";
    private static final String BOOKING_DB_ALIAS = "booking-db";
    private static final String FLIGHT_ALIAS = "flight-service";

    private static final int FLIGHT_PORT = 8081;
    private static final int BOOKING_PORT = 8082;
    private static final int POSTGRES_PORT = 5432;

    private static final String DATABASE_USER = "airline";
    private static final String DATABASE_PASSWORD = "airline";
    private static final String FLIGHT_DATABASE = "airline_flight";
    private static final String BOOKING_DATABASE = "airline_booking";

    private static final Path REPOSITORY_ROOT = Path.of("..");
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(2);

    private static final Network NETWORK = Network.newNetwork();

    private static final PostgreSQLContainer FLIGHT_DB =
            database(FLIGHT_DB_ALIAS, FLIGHT_DATABASE);
    private static final PostgreSQLContainer BOOKING_DB =
            database(BOOKING_DB_ALIAS, BOOKING_DATABASE);

    private static final GenericContainer<?> FLIGHT_SERVICE;
    private static final GenericContainer<?> BOOKING_SERVICE;

    static {
        FLIGHT_DB.start();
        BOOKING_DB.start();

        FLIGHT_SERVICE = flightService();
        FLIGHT_SERVICE.start();

        BOOKING_SERVICE = bookingService();
        BOOKING_SERVICE.start();
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
        String host = BOOKING_SERVICE.getHost();
        Integer port = BOOKING_SERVICE.getMappedPort(BOOKING_PORT);

        return "http://" + host + ":" + port;
    }

    /**
     * The flight database as seen from the test, which is outside the network
     * the services share. Postgres answers on a port Docker picked, so the url
     * cannot be a constant.
     */
    static String flightDatabaseUrl() {
        String host = FLIGHT_DB.getHost();
        Integer port = FLIGHT_DB.getMappedPort(POSTGRES_PORT);

        return "jdbc:postgresql://" + host + ":" + port + "/" + FLIGHT_DATABASE;
    }

    static String databaseUser() {
        return DATABASE_USER;
    }

    static String databasePassword() {
        return DATABASE_PASSWORD;
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
        String flightUrl = "http://" + FLIGHT_ALIAS + ":" + FLIGHT_PORT;

        return service("booking-service/booking-infrastructure", BOOKING_PORT,
                BOOKING_DB_ALIAS, BOOKING_DATABASE)
                .withEnv("AIRLINE_FLIGHT-SERVICE_URL", flightUrl)
                .dependsOn(BOOKING_DB, FLIGHT_SERVICE);
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
