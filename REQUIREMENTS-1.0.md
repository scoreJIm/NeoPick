# NEOPick 1.0 MVP Requirements

> Product Manager analysis — 2026-08-12

## Current State Summary

Neopick is a guitar lesson booking marketplace with Java 21, Spring Boot 3.3, PostgreSQL 16, Redis 7, and AWS (ECS Fargate, RDS, ElastiCache, S3). Hexagonal architecture across 204 Java files and 32 tests.

**Completed (Phases 0-7):**
- **Auth**: SMS code login (auto-register), JWT stateless auth with refresh tokens. Rate limiting on SMS (5/min) and login (10/min) via sliding window interceptor.
- **User**: Profile CRUD, phone/nickname/avatar/gender/role/status.
- **Teacher & Category**: Dynamic search (city, category, gender, level, price range, keyword, sort), featured/popular/weekly recommendations, 8 instrument categories, 10 cities.
- **Booking**: Full state machine (PENDING_CONFIRM → PENDING_PAY → PENDING_CLASS → COMPLETED, cancellable at any point). 7 REST endpoints + BookingExpiryScheduler.
- **Payment**: WeChat/Alipay initiate endpoints. Callback endpoints are stubs.
- **Review**: 1-5 star + tags, duplicate check, instructor-rated.
- **Notification**: Type-classified (SYSTEM/BOOKING/PAYMENT/REVIEW/CHAT), mark read, unread count.
- **Messages**: Conversations with last-message tracking, 3 message types (TEXT/IMAGE/BOOKING_CARD). REST endpoints.
- **WebSocket**: Configured in WebSocketConfig with /ws/chat path, but ChatWebSocketHandler is an empty placeholder.
- **Homepage**: Aggregates banners (city-targeted), categories, popular teachers, featured teachers.
- **Favorites**: Add/remove/list/check per student-teacher pair.
- **Media**: POST /api/v1/media/upload returns a mock S3 URL — no actual upload.
- **CI/CD**: GitHub Actions (ci.yml: checkstyle + unit tests + package on PR; deploy.yml: build → Docker → ECR → ECS on main push). Multi-stage Dockerfile.
- **k8s**: Deployment (2 replicas, rolling update, 500m-1000m CPU, 512Mi-1Gi memory), Service (ClusterIP:8080), Ingress (TLS, nginx, CORS), Kustomization. Probes point to /api/v1/health.
- **Observability**: Prometheus + OTel in pom.xml. @Timed on 3 BookingController endpoints. BusinessMetrics with 8 counters + 1 timer. MetricsConfig with TimedAspect bean. AccessLogFilter with MDC request-id + latency. No structured JSON logging.
- **Tests**: 32 files — BaseIntegrationTest (Testcontainers PostgreSQL), BookingE2ETest (full lifecycle), BookingH2E2ETest (H2 variant), domain unit tests, security tests, exception handler tests.

## Gap Analysis

1. **WebSocket is non-functional**: ChatWebSocketHandler is an empty class. No real-time messaging. No STOMP, no message routing, no WebSocket auth.
2. **Media upload is fake**: Returns a mock URL, no S3 upload. The AWS S3 SDK dependency exists but is unused.
3. **Payment integrations are stubs**: WeChat/Alipay callbacks are empty. No payment gateway connection.
4. **No Swagger/OpenAPI docs**: springdoc is in pom.xml but zero controllers or DTOs have annotations.
5. **Rate limiting covers only 2 endpoints**: SMS and login only. Teacher search, booking submission, message send, payment initiation are all unprotected.
6. **Redis severely underused**: Only rate limiter uses it. No @Cacheable for homepage data, teacher search results, or hot queries.
7. **Observability coverage is thin**: Only 3 of ~30 controller endpoints have @Timed.
8. **No OTel agent**: micrometer-tracing-bridge-otel is present but no `-javaagent:opentelemetry-javaagent.jar` in Dockerfile.
9. **No circuit breaker**: No Resilience4j for SMS provider, payment gateway, or S3 calls.
10. **H2 used alongside Testcontainers**: BookingH2E2ETest skips Flyway (create-drop), so schema compatibility with PostgreSQL is not verified.
11. **No HPA or PDB in k8s**: No autoscaling or disruption budget.
12. **No log aggregation**: Plain text logging, no structured JSON.
13. **JWT secret exposed in k8s Secret stringData**: No sealed secrets or external secret manager.

## Prioritized 1.0 Requirements

### MUST Have (P0) — Launch Blocker

#### REQ-1: Functional WebSocket Messaging
**Effort**: M (3-5 days)

Real-time chat is a core marketplace feature. Replace the empty ChatWebSocketHandler with STOMP over WebSocket using Spring's built-in support.

**What to build:**
- STOMP message broker (in-memory SimpleBroker for v1)
- WebSocket auth: validate JWT on CONNECT frame via ChannelInterceptor
- Message routing: `/user/queue/messages` for push delivery, `/app/chat/{conversationId}` for sending
- Typing indicators, read receipts delivered in real-time
- Fallback to REST polling as graceful degradation

**Acceptance Criteria:**
- Student and teacher can exchange messages in real-time (< 500ms delivery)
- JWT validated on WebSocket handshake; invalid tokens rejected with 401
- Messages persist to DB and appear in conversation history via REST
- Concurrent connections from multiple devices for same user do not collide
- Connection/disconnection metrics exposed to Prometheus

---

#### REQ-2: Working Media Upload with S3
**Effort**: M (2-3 days)

Replace the mock upload with actual S3 integration.

**What to build:**
- Wire AWS S3 SDK to actual S3 upload with presigned URLs
- Client-side upload flow: GET `/api/v1/media/presign?type=avatar|chat|banner` returns presigned PUT URL + final CDN URL
- File type whitelist: image/jpeg, image/png, image/webp, max 5MB
- File type validation via magic bytes, not extension
- Avatar upload triggers resize to 3 sizes (s/m/l)

**Acceptance Criteria:**
- Upload avatar returns CDN URL; avatar appears at 3 resolutions
- Chat image upload returns CDN URL; image renders in chat
- Non-image files rejected with 400 "UNSUPPORTED_FILE_TYPE"
- Files > 5MB rejected with 413 "FILE_TOO_LARGE"
- Media upload metrics exposed to Prometheus

---

#### REQ-3: Production Observability
**Effort**: M (3-4 days)

Add comprehensive metrics, structured logging, and real distributed tracing.

**What to build:**
- @Timed to ALL controller endpoints (all 13 controllers)
- Custom BusinessMetrics additions: SMS sent, login success/fail, teacher search duration, media upload size, WebSocket connections, payment failures
- Distributed Tracing: Add OTel Java agent to Dockerfile. Configure exporter in application.yml.
- Structured Logging: Add logback-spring.xml with JSON encoder (logstash-logback-encoder). Include requestId, userId, spanId, traceId.
- Health Checks: DB, Redis, S3 bucket accessibility, SMS provider reachability.

**Acceptance Criteria:**
- All controller endpoints appear in Prometheus with http_server_requests_seconds metrics
- Grafana dashboard: request rate per endpoint, P50/P95/P99 latency, error rate, booking funnel conversion
- OTel spans visible for full booking lifecycle
- Logs output as JSON with traceId + spanId for correlation
- `/actuator/health` returns 503 if DB, Redis, or S3 is unreachable

---

#### REQ-4: OpenAPI/Swagger Documentation
**Effort**: S (1-2 days)

springdoc-openapi is already in pom.xml. Annotate every controller and DTO.

**What to build:**
- @Operation + @ApiResponses on every controller method
- @Schema annotations on all DTOs
- Grouped API docs: Auth, Teachers, Bookings, Payments, Reviews, Messages/WebSocket, Misc
- Swagger UI accessible at /swagger-ui.html

**Acceptance Criteria:**
- Every endpoint documented in Swagger UI with request/response examples
- All DTO fields show types, descriptions, and required/optional status
- Authentication endpoints show expected headers
- Error response schemas documented

---

#### REQ-5: Production-Grade Rate Limiting
**Effort**: M (2-3 days)

Extend RateLimitInterceptor from 2 endpoints to all public-facing endpoints, with Redis backing.

**What to build:**
- Replace ConcurrentHashMap-based RateLimitInterceptor with Redis-backed rate limiter
- Rate limits: SMS 5/min/IP, Login 10/min/IP, Teacher search 30/min/IP, Booking submit 10/min/user, Message send 30/min/user, Payment initiate 5/min/user, File upload 10/min/user
- Rate limit response headers: X-RateLimit-Remaining, X-RateLimit-Reset, X-RateLimit-Limit

**Acceptance Criteria:**
- Exceeding rate limit returns 429 with Retry-After header
- Limits survive pod restart (backed by Redis)
- Rate limit metrics tagged by endpoint

---

#### REQ-6: Payment Gateway Integration (Alipay Sandbox)
**Effort**: L (5-7 days)

Wire up at least one real payment provider (Alipay sandbox) end-to-end.

**What to build:**
- Implement PaymentGateway port with Alipay adapter
- POST `/api/v1/payments` returns real Alipay pay URL (sandbox)
- POST `/api/v1/payments/callback/alipay` validates signature, updates Payment status
- Payment status polling endpoint: GET `/api/v1/payments/{id}/status`
- Payment expiry: auto-cancel unpaid payments after 2 hours

**Acceptance Criteria:**
- User can initiate payment and receive a valid Alipay QR/pay URL
- Alipay sandbox callback updates booking status to PENDING_CLASS
- Invalid callbacks (bad signature) rejected with 400
- Payment expiry cancels booking and notifies both parties

---

### SHOULD Have (P1)

#### REQ-7: Circuit Breaker for External Dependencies
**Effort**: S (1-2 days)
Add Resilience4j circuit breaker and retry for: SMS provider, payment gateway, S3 operations.

#### REQ-8: PostgreSQL-based E2E Tests (No H2)
**Effort**: S (1 day)
Remove H2 dependency. Convert BookingH2E2ETest to extend BaseIntegrationTest with Testcontainers PostgreSQL.

#### REQ-9: HPA + PDB for Kubernetes
**Effort**: S (1 day)
HPA: min 2, max 6 replicas, target 70% CPU. PDB: maxUnavailable: 1.

#### REQ-10: Redis Caching Layer
**Effort**: M (2-3 days)
Add @Cacheable on: homepage (TTL 5min), teacher search results (TTL 2min), city/category lists (TTL 30min).

### NICE to Have (P2)

#### REQ-11: Structured JSON Logging
#### REQ-12: Refresh Token Rotation
#### REQ-13: Prometheus Alert Rules
#### REQ-14: GitLab CI Mirror
