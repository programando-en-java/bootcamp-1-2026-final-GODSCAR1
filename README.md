# Sistema de reservas de aerolínea

Proyecto final del bootcamp **De Senior a Arquitecto - Programando En Java**. Un sistema de reservas construido como siete servicios independientes: buscar un vuelo, reservarlo, pagarlo, facturar y enterarse de todo ello.

No es una maqueta. Los servicios se comunican por Kafka con outbox transaccional, la reserva y el pago se coordinan con una saga que compensa cuando la tarjeta se rechaza, cada petición viaja con un JWT firmado con RS256, y hay 448 pruebas automáticas más 35 de extremo a extremo contra la pila real levantada con Testcontainers.

**Las decisiones están en [`ARCHITECTURE.md`](ARCHITECTURE.md): 24 ADR con su índice al principio.** Este README dice qué hay y cómo ejecutarlo; el porqué de cada cosa está allí.

---

## Servicios

| Servicio               | Responsabilidad                                 | Puerto | Base                   |
| ---------------------- | ----------------------------------------------- | ------ | ---------------------- |
| `api-gateway`          | Única puerta de entrada, enruta y filtra         | 8080   | ninguna                |
| `flight-service`       | Vuelos, inventario y bloqueo de asientos         | 8081   | `airline_flight`       |
| `booking-service`      | Reservas y su ciclo de vida                      | 8082   | `airline_booking`      |
| `payment-service`      | Cobros y pasarela simulada                       | 8083   | `airline_payment`      |
| `checkin-service`      | Facturación y tarjeta de embarque                | 8084   | `airline_checkin`      |
| `notification-service` | Avisa al pasajero; solo consume, no expone HTTP  | 8085   | `airline_notification` |
| `auth-service`         | Usuarios, roles y emisión de tokens              | 8086   | `airline_auth`         |

Cada servicio tiene su propia base de datos y nadie lee la del vecino. Los puertos 8081 a 8086 **no están publicados**: desde fuera solo existe el 8080.

---

## Ejecutar el sistema

```bash
docker compose up -d --build --wait
```

Levanta catorce contenedores: seis bases PostgreSQL, Kafka y los siete servicios. Las imágenes se construyen desde el código en dos etapas, así que no hace falta compilar antes.

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"passenger@airline.test","password":"passenger123"}'
```

Las tres cuentas de demostración, una por rol, están en [`scripts/README.md`](scripts/README.md) junto con los guiones que recorren el sistema entero y muestran lo que ocurre en cada paso.

```bash
docker compose down -v
```

---

## Ejecutar las pruebas

```bash
./mvnw -B clean verify                                # 448 pruebas
./mvnw -B verify -pl e2e-tests -Dairline.e2e=true     # 35 más, con la pila real
```

Las de extremo a extremo van detrás de un flag porque levantan catorce contenedores y tardan varios minutos; el resto se ejecuta en cada cambio.

**No hay pruebas unitarias con mocks de la capa de persistencia.** El estilo está en [ADR-010](ARCHITECTURE.md#adr-010-testing-style): contextos de Spring construidos a mano contra PostgreSQL real en Testcontainers, `@Nested` con nombres que se leen como frases, y DAMP antes que DRY. Las clases de dominio puras son la única excepción.

---

## Por dónde entrar a los ADR

Los 24 están indexados al principio de [`ARCHITECTURE.md`](ARCHITECTURE.md). Si vas a leer solo unos pocos:

**Cómo está montado.** [ADR-006](ARCHITECTURE.md#adr-006-hexagonal-architecture-with-one-maven-module-per-layer) explica por qué cada servicio son tres módulos Maven cuando el enunciado decía expresamente que no hacía falta, y [ADR-003](ARCHITECTURE.md#adr-003-no-shared-module) por qué no hay un módulo compartido y los contratos se duplican a propósito.

**Cómo se hablan los servicios.** [ADR-001](ARCHITECTURE.md#adr-001-transactional-outbox-for-integration-events) es el outbox transaccional: el evento y la fila que lo provoca se confirman juntos o no se confirma ninguno. [ADR-004](ARCHITECTURE.md#adr-004-synchronous-commands-for-contended-resources-kafka-for-facts) explica por qué bloquear asientos es una llamada síncrona y todo lo demás es un hecho publicado.

**La saga.** [ADR-013](ARCHITECTURE.md#adr-013-the-payment-saga) es la orquestada que confirma la reserva cuando el pago sale y devuelve los asientos cuando no. [ADR-007](ARCHITECTURE.md#adr-007-pessimistic-locking-for-seat-inventory) es el bloqueo pesimista que impide vender el mismo asiento dos veces.

**Concurrencia.** [ADR-019](ARCHITECTURE.md#adr-019-deciding-a-write-by-reading-first) es el más útil si vienes a aprender algo: por qué las escrituras idempotentes son `SERIALIZABLE` con reintento, y por qué `flight-service` deliberadamente **no** lo es. La diferencia se midió con un test que pasó de responder `[201, 409]` a devolver cinco errores 500.

**Seguridad.** Del [ADR-020](ARCHITECTURE.md#adr-020-who-issues-tokens-and-what-signs-them) al [ADR-024](ARCHITECTURE.md#adr-024-one-door-in): quién firma, por qué el pasajero sale del token y no del cuerpo de la petición, por qué una reserva ajena se responde como inexistente en vez de con un 403, y qué aporta el gateway de verdad, que es menos de lo que suele acreditársele.

Varios registran **errores, no aciertos**. [ADR-001](ARCHITECTURE.md#adr-001-transactional-outbox-for-integration-events) está enmendado porque afirmaba que `booking-service` nunca necesitaría un outbox, y resultó falso. [ADR-022](ARCHITECTURE.md#adr-022-a-booking-that-is-not-yours-does-not-exist) existe porque al añadir la comprobación de pertenencia apareció una forma de leer la tarjeta de embarque de otro pasajero. Está hecho a propósito: un registro que solo guarda las decisiones que salieron bien no sirve de mucho.

---

## API

Todo pasa por `http://localhost:8080`.

| Método   | Ruta                       | Quién                |
| -------- | -------------------------- | -------------------- |
| `POST`   | `/api/v1/auth/login`       | público              |
| `GET`    | `/api/v1/flights`          | público              |
| `GET`    | `/api/v1/flights/{id}`     | público              |
| `POST`   | `/api/v1/bookings`         | autenticado          |
| `GET`    | `/api/v1/bookings/{id}`    | su dueño, o personal |
| `POST`   | `/api/v1/payments`         | autenticado          |
| `POST`   | `/api/v1/boarding-passes`  | su dueño, o personal |

Buscar vuelos es público porque nadie inicia sesión para mirar qué hay a la venta. Los bloqueos de asiento **no se enrutan**: los usa `booking-service` desde dentro de la red y no son alcanzables desde fuera ni con rol de administrador.

### Roles

`PASSENGER` reserva y factura lo suyo, `AGENT` gestiona reservas ajenas, `ADMIN` accede a todo. La pertenencia no la decide el gateway sino el servicio dueño de los datos, que es el único que puede.

---

## Eventos

| Topic                  | Lo publica      | Lo consume                            |
| ---------------------- | --------------- | ------------------------------------- |
| `booking.created.v1`   | booking-service | notification-service                  |
| `payment.succeeded.v1` | payment-service | booking-service, notification-service |
| `payment.failed.v1`    | payment-service | booking-service                       |
| `checkin.completed.v1` | checkin-service | notification-service                  |

Todos salen de una tabla `outbox` con un relay que usa `FOR UPDATE SKIP LOCKED`, y todos los consumidores reclaman el identificador del evento antes de actuar, porque la entrega es al menos una vez.

---

## Stack

Java 21, Spring Boot 4.1, Spring Cloud Gateway (variante webmvc), Spring Security 7, PostgreSQL 17, Kafka 4.1 en KRaft, Hibernate 7.2, Flyway, Maven multimódulo, Docker Compose, Testcontainers y GitHub Actions.

Sin H2 en ninguna parte, ni siquiera en pruebas. El motivo está en [ADR-002](ARCHITECTURE.md#adr-002-postgresql-in-every-environment).

---

## Estructura

```text
airline-system/
├── api-gateway/               un solo módulo: no tiene dominio
├── flight-service/            domain / application / infrastructure
├── booking-service/           idem
├── payment-service/           idem
├── checkin-service/           idem
├── notification-service/      idem
├── auth-service/              idem
├── e2e-tests/                 recorridos completos con Testcontainers
├── scripts/                   guiones de humo contra la pila levantada
├── ARCHITECTURE.md            los 24 ADR
└── docker-compose.yml
```
