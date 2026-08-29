# order-service

Order orchestration for the [ar-ecommerce-platform](https://github.com/ar-ecommerce-platform).
This is the service that ties the others together.

- **Port:** 8082
- **Persistence:** in-memory H2 (`orders`, `order_lines`) — resets on restart
- **Registers with:** Eureka (discovery-server :8761)
- Calls the other services with a `@LoadBalanced RestClient` (targets `lb://<service>`), so
  every hop goes through Eureka + Spring Cloud LoadBalancer.

## The placement flow

`POST /orders` runs, in order:

1. **product-service** `GET /products/{id}` for each line — validate + capture unit price
   (`400 UNKNOWN_PRODUCT` if missing)
2. **inventory-service** `POST /inventory/{id}/reserve` for each line — on failure the order is
   saved `REJECTED_STOCK` and `409` is returned
3. **payment-service** `POST /payments` for the total — on decline the order is saved
   `PAYMENT_FAILED` and `402` is returned
4. order saved `CONFIRMED`, then **notification-service** `POST /notifications` (fire-and-forget)

The flow is synchronous and deliberately simple: no compensation of a partial reservation, no
retries, no circuit breaker.

## Endpoints

Reached through the gateway as `/api/orders/**`.

| Method | Path | Body / query | Result |
|---|---|---|---|
| `POST` | `/orders` | `{ userId, items: [{ productId, quantity }] }` | `201` with the resolved order + lines + `status` |
| `GET` | `/orders/{id}` | — | one order, `404` if missing |
| `GET` | `/orders` | `?userId=` | that user's orders, newest first |

**API docs:** Swagger UI at `http://localhost:8082/swagger-ui.html` (OpenAPI JSON at `/v3/api-docs`).

## Run

Whole platform (recommended):

```bash
docker compose -f ../infra/compose/docker-compose.yml up -d --build
```

This service alone (needs discovery-server + product/inventory/payment/notification running):

```bash
./gradlew bootRun
```

## Build & quality

```bash
./gradlew build          # compile + test + spotless + checkstyle (cyclomatic complexity <= 10) + jacoco report
./gradlew spotlessApply
```

Quality config is vendored: `gradle/quality.gradle`, `config/checkstyle/`.

## Config

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8082` | HTTP port |
| `PRODUCT_SERVICE_URL` etc. | `lb://product-service` … | downstream base URLs (override to point at fixed hosts) |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` | registry URL |

## Tech

Java 21 · Spring Boot 3.5.7 · Spring Data JPA + H2 · `RestClient` + Spring Cloud LoadBalancer ·
Spring Cloud 2025.0.0 (`netflix-eureka-client`, `loadbalancer`) · Gradle
