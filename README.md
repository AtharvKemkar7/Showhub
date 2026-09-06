# EventHub Platform

Java 21 / Spring Boot 3.3 microservices. Each service owns its PostgreSQL database. JWT is issued by Identity Service and verified independently. There are no cross-service foreign keys.

## Services

| Service | Port | Database |
| --- | --- | --- |
| identity-service | 8081 | `identity_db` |
| event-service | 8082 | `event_db` |
| venue-service | 8083 | `venue_db` |
| show-service | 8084 | `show_db` |
| inventory-service | 8085 | `inventory_db` |
| booking-service | 8086 | `booking_db` |
| payment-service | 8087 | `payment_db` |
| notification-service | 8088 | `notification_db` |
| search-service | 8089 | `search_db` |

## Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL 15
- Redis 7 (inventory locks)

```bash
export JAVA_HOME=/path/to/jdk-21
export JWT_SECRET=change-me-to-a-long-random-secret-value
export ADMIN_EMAIL=admin@eventplatform.local
export ADMIN_PASSWORD=AdminPass123!
export INTERNAL_TOKEN=dev-internal-token
```

Create databases with `scripts/create-databases.sql`. Never hardcode secrets.

## Web UI

Vite app in `frontend/` (port `5173`). Proxies `/api/v1/*` to the services below. If backends are down, the UI uses a styled demo catalog.

```bash
cd frontend && npm install && npm run dev
```

## Run

```bash
mvn -pl identity-service spring-boot:run
mvn -pl event-service spring-boot:run
mvn -pl venue-service spring-boot:run
mvn -pl show-service spring-boot:run
mvn -pl inventory-service spring-boot:run
mvn -pl booking-service spring-boot:run
mvn -pl payment-service spring-boot:run
mvn -pl notification-service spring-boot:run
mvn -pl search-service spring-boot:run
```

Infra: `docker compose up -d` starts Postgres, Redis, Kafka, Kafka UI, Prometheus, Grafana.

## Booking flow

1. Customer locks seats via Inventory (`POST /api/v1/inventory/locks`) or Booking creates a pending booking which locks seats internally.
2. Customer initiates payment (`POST /api/v1/payments`).
3. Mock webhook or `POST /api/v1/payments/{id}/mock-checkout` marks payment succeeded.
4. Payment Service confirms the booking over the internal API (`X-Internal-Token`).
5. Inventory seats become `BOOKED`. Kafka-style domain events are published (logging fallback if Kafka is absent).

Identity is taken from the JWT SecurityContext. Request bodies never supply `userId`, `organizerId`, or `role`.

## Tests

```bash
mvn test
```
