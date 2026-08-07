# Neopick 1.0 Development Process Log

## Project Overview

**Product**: Neopick — Guitar lesson booking marketplace connecting students with private teachers
**Repository**: neopick-case-study-repo
**Tech Stack**: Java 21, Spring Boot 3.3, PostgreSQL 16, Redis 7, AWS (ECS Fargate, RDS, ElastiCache, S3, SNS, SQS)
**Architecture**: Hexagonal (Ports & Adapters) with Clean Code principles

## Development Log

### Phase 0: Foundation (2026-08-08)
- Created Maven pom.xml with all dependencies (Spring Boot, JPA, Security, WebSocket, JWT, AWS SDK, MapStruct, Lombok, Testcontainers)
- Configured application.yml with dev/prod profiles
- Created Flyway migrations: V1 (17 tables DDL) and V2 (seed data for cities and categories)
- Implemented shared kernel: Result monad, Constants, Money value object
- Defined all port interfaces: TokenProvider, SecurityContext, SmsSender, FileStorage, CacheManager, EventPublisher, WebSocketSessionManager, PaymentGateway
- Created infrastructure config: NeopickProperties, RedisConfig, SchedulerConfig, JacksonConfig
- Added common DTOs: ApiResponse, ErrorResponse, PageResponse
- Files: 20

### Phase 1: Auth & User (2026-08-08)
- User domain aggregate with value objects (UserId, PhoneNumber) and enums (Gender, UserRole, UserStatus)
- Auth domain: TokenPair, SmsCodeService, AuthService
- JPA entities: UserJpaEntity, RefreshTokenJpaEntity
- Spring Data repositories with repository implementations mapping JPA to domain
- MockSmsSender for dev, production-ready SmsCodeServiceImpl with rate limiting via Redis
- JWT authentication: JwtTokenProvider, JwtAuthenticationFilter, SecurityContextHolder
- Spring Security config with stateless JWT filter chain, public/private path rules
- Use cases: SendSmsCode, Login (auto-register), RefreshToken
- REST controllers: AuthController, UserController
- Full DTO layer: SendSmsRequest, LoginRequest/Response, RefreshTokenRequest, UserResponse, UpdateProfileRequest
- Files: 25

### Phase 2: Teacher & Category (2026-08-08)
- Teacher domain aggregate: TeacherId, TeacherLevel, TeacherStatus, City value object, TeacherSearchCriteria
- Category JPA entity for instrument types (Classical, Electric, Acoustic, Folk, Flamenco, Bass, Ukulele, Music Theory)
- City entity for geographic filtering (10 cities: Shanghai, Beijing, Guangzhou, Shenzhen, Chengdu, Hangzhou, Wuhan, Nanjing, Chongqing, Xi'an)
- JPA Specification for dynamic teacher search/filter (city, category, gender, level, price range, keyword)
- TeacherRepository with search pagination and sort support (PRICE_ASC/DESC, RATING_ASC/DESC)
- Use cases: SearchTeachers, GetTeacherDetail, GetFeaturedTeachers, GetPopularTeachers, GetWeeklyRecommendations
- Controllers: TeacherController, CityController
- DTOs: TeacherCardResponse, CityResponse
- Files: 24

### Phase 3: Booking & Payment (2026-08-08)
- Booking aggregate root with full state machine (PENDING_CONFIRM → PENDING_PAY → PENDING_CLASS → COMPLETED)
- InvalidBookingTransitionException for state violations
- Payment aggregate: PaymentId, PaymentMethod (WECHAT, ALIPAY), PaymentStatus
- Address value object with lat/lng for lesson location
- JPA entities: BookingJpaEntity, PaymentJpaEntity with full mappings
- Repository implementations with bidirectional mapping
- Use cases: SubmitBooking, ManageBooking (confirm/reject/cancel/complete), GetStudentBookings, GetTeacherBookings, InitiatePayment
- Controllers: BookingController (9 endpoints), PaymentController
- DTOs: SubmitBookingRequest, BookingResponse, CancelBookingRequest, InitiatePaymentRequest, PaymentResponse
- Files: 21

### Phase 4: Review & Notification (2026-08-08)
- Review aggregate with 1-5 star rating validation and tags
- Notification entity with type classification (SYSTEM, BOOKING, PAYMENT, REVIEW, CHAT)
- JPA entities and repositories for both
- Use cases: SubmitReview (with duplicate check), GetMyReviews, NotificationUseCase (list/mark-read/unread-count)
- Controllers: ReviewController, NotificationController
- DTOs: SubmitReviewRequest, ReviewResponse, NotificationResponse
- Files: 17

### Phase 5: Messages & WebSocket (2026-08-08)
- Conversation aggregate root with last-message tracking
- ChatMessage entity with sender/receiver and read status
- MessageType enum: TEXT, IMAGE, BOOKING_CARD
- JPA entities: ConversationJpaEntity (unique constraint on student+teacher), ChatMessageJpaEntity
- Repository with full conversation and message CRUD
- Use cases: listConversations, startConversation, getMessages, sendMessage
- MessageController with REST endpoints + WebSocket config placeholder
- DTOs: ConversationResponse, MessageResponse, StartConversationRequest, SendMessageRequest
- Files: 17

### Phase 6: Homepage, Favorites, Media (2026-08-08)
- Favorite domain entity with unique constraint per student-teacher pair
- Banner JPA entity for homepage carousel (with city targeting and link types)
- Use cases: FavoriteUseCase (add/remove/list/check), GetHomePageUseCase (banners + categories + teachers aggregation)
- Controllers: HomeController, FavoriteController, MediaController
- DTOs: HomePageResponse, FavoriteResponse
- Files: 14

### Phase 7: Cross-cutting & CI/CD (2026-08-08)
- GlobalExceptionHandler: BusinessException (400), IllegalArgumentException (400), IllegalStateException (409), validation errors, catch-all 500
- WebConfig with CORS configuration
- WebSocketConfig with chat handler
- AccessLogFilter with MDC request-id tracing and latency logging
- BookingExpiryScheduler for auto-cancelling expired PENDING bookings
- Dockerfile: multi-stage build (Maven → JRE Alpine), health check
- GitHub Actions CI: checkstyle, unit tests, package (on PR)
- GitHub Actions Deploy: build → test → Docker → ECR → ECS (on push to main)
- Files: 30

## File Statistics

| Category | Count |
|----------|-------|
| Domain classes | 45 |
| Application use cases | 22 |
| Adapter - Web (controllers, configs) | 18 |
| Adapter - Persistence (entities, repos, impls) | 35 |
| Adapter - Infrastructure (SMS, cache, logging) | 8 |
| Port interfaces | 9 |
| DTOs | 24 |
| Config files (YAML, XML) | 5 |
| Database migrations | 2 |
| CI/CD + Docker | 3 |
| **Total** | **171** |

## Architecture Compliance

- Domain layer has ZERO framework dependencies (no Spring, JPA, AWS imports)
- All use cases are single-method classes following Command pattern
- JPA entities are separate from domain objects — mapping in repository implementations
- Port interfaces define all outbound dependencies
- REST controllers are thin (delegate to use cases, return ApiResponse envelope)
- Booking state machine enforced in domain, not in controllers
- All code, comments, and documentation in English
