# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

Hotel self-check-in system (酒店自助入住系统) — a **Spring Cloud microservices** application. Source code is in active development; most P1 fixes from the architecture design docs have been implemented.

Key design documents (in parent directory `..`):
- `酒店自助入住系统设计文档.md` — Full architecture: domain models, DB schema, Kafka, Redis, API design
- `架构缺陷修复方案_P1级.md` — P1 defect fix specifications (idempotency, inventory, cache protection)

## Commands

- **Build**: `mvn clean package -DskipTests`
- **Test all**: `mvn test`
- **Test single class**: `mvn test -pl hotel-common -Dtest=IdempotentInterceptorTest`
- **Run infra**: `docker compose -f hotel-infrastructure/docker/docker-compose.yml up -d`
- **Run a service**: `mvn spring-boot:run -pl hotel-services/hotel-order`
- **Java 17** required; the root `pom.xml` declares `maven.compiler.source=17`.

## Module Structure

```
hotel-selfcheckin/
├── hotel-common/          # Shared library: idempotency, Saga, JWT, encryption, cache, Feign, Kafka, core types
├── hotel-gateway/         # Spring Cloud Gateway (port 8080): JWT auth + request signing + route forwarding
├── hotel-services/
│   ├── hotel-order/       # Order lifecycle (PENDING → PAID → ASSIGNED → CHECKED_IN → CHECKED_OUT)
│   ├── hotel-room/        # Room inventory (Redis Lua), assignment (Redisson lock), pricing
│   ├── hotel-payment/     # Payments, refunds, payment callbacks (idempotent)
│   ├── hotel-checkin/     # Check-in, identity verification, guest management, extensions, checkout
│   ├── hotel-card/        # Room card creation, QR codes, card lifecycle
│   └── hotel-member/      # Membership, points, levels
└── hotel-infrastructure/
    ├── docker/             # docker-compose.yml: MySQL 8.0, Redis 7.0, Kafka (cp-kafka 7.5), Nacos 2.3.1
    └── sql/                # schema.sql (all DDL), seed.sql
```

## Key Architecture Patterns

### Idempotency (`hotel-common/.../idempotent/`)
- `@Idempotent` annotation on controller methods — uses AOP (`IdempotentInterceptor`)
- Redis-backed: `SETNX` with configurable TTL, supports `WAIT`/`THROW`/`SKIP` concurrency strategies
- Result caching: completed requests return cached result on duplicate calls
- **Rule**: all payment/order/callback endpoints must use `@Idempotent`

### Distributed Transactions — Saga (`hotel-common/.../saga/`)
- `SagaExecutor`: orchestrates multi-step transactions (e.g., Payment → Room Assign → Card Create)
- Each step implements `SagaStepInterface` with `execute()` and `compensate()`
- Steps ordered via `@SagaStep(order=N)` annotation
- Persistent to `saga_log` table; `SagaRetryScheduler` handles failed compensations
- `SagaExecutorWithTimeout` for time-bounded sagas

### Inventory Management (`RoomInventoryService`)
- Redis Lua scripts for atomic decrement — **never oversells** (returns -1 for insufficient stock, -2 for missing key)
- `@PostConstruct warmup()` loads MySQL counts into Redis on startup
- `decrement()` / `increment()` / `decrementBatch()` / `refreshFromDb()`

### Room Assignment (`RoomAssignmentService`)
- Redisson distributed lock (`tryLock`) with configurable TTL (default 30s) — **not** raw `setIfAbsent`
- Two-phase: decrement inventory → select room (strategy pattern) → lock and update DB
- Rolls back inventory on any failure
- Strategies: `SequentialStrategy` (default), `HighFloorStrategy`

### Authentication & Security
- **Gateway**: `AuthFilter` (global filter, order=-100) validates JWT Bearer tokens, checks whitelist paths
- **Gateway**: `SignFilter` validates request signatures (timestamp-based, 5-min window)
- **JWT**: `JwtUtil` in hotel-common — HS256, configurable secret/expiration
- **ID Card**: `IdCardEncryptService` — AES-256-GCM encryption with PBKDF2 key derivation, SHA-256 hashing with per-record salt, versioned keys, timing-safe comparison, output masking (`3*******4`). Stores encrypted + hashed + masked in `chk_guest`.

### Inter-Service Communication
- **Sync**: OpenFeign (`@FeignClient`) with fallback factories (e.g., `RoomFeignFallback`, `CardFeignFallback`)
- **Async**: Kafka — room status changes, payment events, check-in events. Each service has `KafkaConsumerConfig` + event consumers
- **Tracing**: `FeignTracingInterceptor` and `KafkaTracingInterceptor` for distributed trace propagation

### Cache Protection (`CacheService` in hotel-common)
- Cache breakdown (热点key过期): mutex lock pattern
- Cache avalanche (批量过期): randomized TTL (±20%)
- Cache penetration (不存在的数据): null-value caching with short TTL

## Database Conventions

- MySQL 8.0, `hotel_db`, charset `utf8mb4`
- Table prefixes by domain: `ord_` (order), `chk_` (checkin), `crd_` (card), `pay_` (payment), `mem_` (member), `sys_` (system), `rom_` (room)
- All primary keys are `VARCHAR(32)` (not auto-increment)
- All tables have `created_time`/`updated_time` with `DEFAULT CURRENT_TIMESTAMP`/`ON UPDATE CURRENT_TIMESTAMP`
- Status fields use `TINYINT` with comments documenting each value

## Configuration

- **Registry/Config**: Nacos (`localhost:8848`, env override `NACOS_ADDR`)
- **Gateway routes** defined in `hotel-gateway/src/main/resources/application.yml`
- **Sensitive values** use env vars with defaults: `JWT_SECRET`, `SIGN_SECRET`, `NACOS_ADDR`
- **Redis password**: `redis-pass-2024` (docker-compose default)
- **MySQL**: root password `hotel2024` (docker-compose default)

## Working Guidelines

- When implementing new services, use existing patterns: `@Idempotent` on mutating endpoints, Saga for cross-service operations, Redisson for distributed locks
- Follow the DDL conventions from `hotel-infrastructure/sql/schema.sql`
- Encrypt/mask all PII (ID card numbers) via `IdCardEncryptService` — never store plaintext
- Use `RoomInventoryService.decrement()` (Lua atomic) for inventory, never raw Redis operations
- Gateway auth: add new public paths to `gateway.white-list.paths` in gateway config
