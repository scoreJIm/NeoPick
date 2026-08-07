# NEOPick — Guitar Lesson Marketplace

A two-sided marketplace connecting guitar students with private teachers. Search,
book, pay, review, chat — the full loop. Built from scratch, product design
through deployment.

## Architecture

Hexagonal (Ports & Adapters) with domain-driven design. The domain layer has zero
framework dependencies — no Spring, no JPA, no AWS imports. All external concerns
live in the adapter layer.

```
adapter/          ← Spring Boot, JPA, Redis, AWS, WebSocket
  ├── web/        ← REST controllers, DTOs, security filters
  ├── persistence/← JPA entities, repositories, specs
  ├── sms/        ← SMS code delivery (mock + production)
  ├── cache/      ← Redis cache adapter
  └── scheduled/  ← Background jobs

application/      ← Use cases (one class per action)
domain/           ← Aggregates, value objects, domain services, ports
port/             ← Interfaces for all outbound dependencies
infrastructure/   ← Cross-cutting config (Jackson, Redis, scheduling)
shared/           ← Result monad, constants, base classes
```

## Tech Stack

| Layer | Choices |
|-------|---------|
| Runtime | Java 21, Spring Boot 3.3 |
| Database | PostgreSQL 16, Flyway migrations |
| Cache | Redis 7 |
| Auth | JWT (access + refresh tokens), SMS code login |
| API | REST (Spring MVC), WebSocket (chat) |
| Cloud | AWS ECS Fargate, RDS, S3, SNS/SQS, Secrets Manager |
| Testing | JUnit 5, Testcontainers, Rest Assured, JaCoCo (80% min) |
| CI/CD | GitHub Actions (PR checks), Docker multi-stage build |
| Docs | SpringDoc OpenAPI / Swagger UI |

## Quick Start

### Prerequisites

- JDK 21+
- PostgreSQL 16 (or Docker)
- Redis 7 (or Docker)
- Maven 3.9+

### Local dev (H2, no Docker needed)

```bash
git clone git@github.com:scoreJIm/neopick-case-study-repo.git
cd neopick-case-study-repo
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

H2 console at `http://localhost:8080/h2-console`. All schemas auto-create.
No PostgreSQL or Redis required.

### Dev environment (with Docker)

```bash
docker run -d --name neopick-pg -p 5432:5432 \
  -e POSTGRES_DB=neopick -e POSTGRES_USER=neopick -e POSTGRES_PASSWORD=neopick_dev \
  postgres:16

docker run -d --name neopick-redis -p 6379:6379 redis:7

mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Environment Profiles

| Profile | DB | SMS | Redis | Purpose |
|---------|----|-----|-------|---------|
| `local` | H2 in-memory | Mock | Optional | Zero-dependency dev |
| `dev` | PostgreSQL localhost | Mock | Required | Full stack local |
| `prod` | AWS RDS | Real | ElastiCache | Production |

## API Overview

```
POST   /api/v1/auth/send-sms              Send login code
POST   /api/v1/auth/login                 Login / auto-register
POST   /api/v1/auth/refresh               Refresh access token

GET    /api/v1/teachers                   Search & filter teachers
GET    /api/v1/teachers/{id}              Teacher detail
GET    /api/v1/teachers/featured          Featured teachers
GET    /api/v1/cities                     City list

POST   /api/v1/bookings                   Submit booking
GET    /api/v1/bookings/student           My bookings (student)
GET    /api/v1/bookings/teacher           My bookings (teacher)
PUT    /api/v1/bookings/{id}/confirm      Confirm booking
PUT    /api/v1/bookings/{id}/reject       Reject booking

POST   /api/v1/reviews                    Submit review
GET    /api/v1/reviews/my                 My reviews

GET    /api/v1/messages/conversations     Conversation list
POST   /api/v1/messages/conversations     Start conversation
GET    /api/v1/messages/{id}              Message history
POST   /api/v1/messages                   Send message (also via WebSocket)

GET    /api/v1/home                       Homepage aggregation
POST   /api/v1/favorites                  Add favorite
DELETE /api/v1/favorites/{teacherId}      Remove favorite

GET    /api/v1/notifications              Notification list
```

## Domain Model

- **User** — Student or teacher, phone-based auth
- **Teacher** — Profile, level, city, instrument categories
- **Booking** — State machine: PENDING_CONFIRM → PENDING_PAY → PENDING_CLASS → COMPLETED
- **Payment** — WeChat / Alipay, linked to booking
- **Review** — 1-5 star rating, tags, one review per booking
- **Conversation** — Student + teacher chat, real-time via WebSocket
- **Notification** — System, booking, payment, review events

## Project Structure

```
src/main/java/com/neopick/
├── NeopickApplication.java
├── domain/        ← 45 domain classes (zero framework deps)
├── application/   ← 22 use cases (single-method command pattern)
├── adapter/       ← 53 adapter classes (web, persistence, sms, cache, scheduled)
├── port/          ← 9 outbound interfaces
├── infrastructure/← 6 config/logging classes
└── shared/        ← 2 shared utilities (Result monad, Constants)

src/main/resources/
├── application.yml          ← Common config
├── application-local.yml    ← Local dev (H2, mock services)
├── application-dev.yml      ← Dev environment
├── application-prod.yml     ← Production
└── db/migration/            ← Flyway migrations (17 tables)

frontend/          ← Vue 3 + Vite (separate project, see frontend/README.md)
```

## CI/CD

- **PR checks** (`.github/workflows/ci.yml`): Checkstyle → Unit tests → Package
- **Deploy** (`.github/workflows/deploy.yml`): Build → Test → Docker → ECR → ECS
- **Quality gates**: JaCoCo 80% line, 75% branch coverage minimum

## Status

Feature-complete backend. Currently iterating on the frontend and preparing
for production deployment. See PROCESS.md for the full development log.
