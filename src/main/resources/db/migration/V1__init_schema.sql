CREATE TABLE cities (
    code        VARCHAR(10) PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    is_hot      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(20) NOT NULL UNIQUE,
    nickname        VARCHAR(50) NOT NULL,
    avatar_url      VARCHAR(500),
    gender          VARCHAR(10),
    role            VARCHAR(10) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    registered_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMP
);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);

CREATE TABLE sms_codes (
    id          BIGSERIAL PRIMARY KEY,
    phone       VARCHAR(20) NOT NULL,
    code        VARCHAR(6) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_sms_phone ON sms_codes(phone, created_at DESC);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(128) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_rt_user ON refresh_tokens(user_id);
CREATE INDEX idx_rt_token ON refresh_tokens(token_hash);

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    icon_url    VARCHAR(500),
    sort_order  INTEGER NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE teachers (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id),
    real_name       VARCHAR(50) NOT NULL,
    bio             TEXT,
    level           VARCHAR(20) NOT NULL,
    teaching_years  INTEGER,
    base_price      DECIMAL(10,2) NOT NULL,
    city_code       VARCHAR(10) NOT NULL,
    city_name       VARCHAR(50) NOT NULL,
    district        VARCHAR(100),
    latitude        DECIMAL(10,7),
    longitude       DECIMAL(10,7),
    cover_image_url VARCHAR(500),
    rating          DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    review_count    INTEGER NOT NULL DEFAULT 0,
    booking_count   INTEGER NOT NULL DEFAULT 0,
    is_featured     BOOLEAN NOT NULL DEFAULT FALSE,
    status          VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_teachers_city_level ON teachers(city_code, level);
CREATE INDEX idx_teachers_rating ON teachers(rating DESC);
CREATE INDEX idx_teachers_base_price ON teachers(base_price);
CREATE INDEX idx_teachers_status ON teachers(status);
CREATE INDEX idx_teachers_featured ON teachers(is_featured, city_code);

CREATE TABLE teacher_tags (
    id          BIGSERIAL PRIMARY KEY,
    teacher_id  BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    tag         VARCHAR(30) NOT NULL,
    UNIQUE(teacher_id, tag)
);
CREATE INDEX idx_teacher_tags_tag ON teacher_tags(tag);

CREATE TABLE teacher_categories (
    id          BIGSERIAL PRIMARY KEY,
    teacher_id  BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    UNIQUE(teacher_id, category_id)
);

CREATE TABLE teacher_availability (
    id          BIGSERIAL PRIMARY KEY,
    teacher_id  BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_avail_teacher ON teacher_availability(teacher_id, day_of_week);

CREATE TABLE teacher_blocked_slots (
    id          BIGSERIAL PRIMARY KEY,
    teacher_id  BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    block_date  DATE NOT NULL,
    start_time  TIME,
    end_time    TIME,
    reason      VARCHAR(100)
);
CREATE INDEX idx_blocked_teacher_date ON teacher_blocked_slots(teacher_id, block_date);

CREATE TABLE bookings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id          UUID NOT NULL REFERENCES users(id),
    teacher_id          BIGINT NOT NULL REFERENCES teachers(id),
    status              VARCHAR(20) NOT NULL,
    scheduled_start     TIMESTAMP NOT NULL,
    scheduled_end       TIMESTAMP NOT NULL,
    duration_minutes    INTEGER NOT NULL,
    price               DECIMAL(10,2) NOT NULL,
    address_label       VARCHAR(200),
    address_detail      VARCHAR(300),
    address_lat         DECIMAL(10,7),
    address_lng         DECIMAL(10,7),
    student_note        TEXT,
    cancel_reason       TEXT,
    cancelled_by        VARCHAR(10),
    confirmed_at        TIMESTAMP,
    paid_at             TIMESTAMP,
    completed_at        TIMESTAMP,
    cancelled_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_bookings_student ON bookings(student_id, status);
CREATE INDEX idx_bookings_teacher ON bookings(teacher_id, status);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_scheduled ON bookings(scheduled_start);

CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id      UUID NOT NULL UNIQUE REFERENCES bookings(id),
    amount          DECIMAL(10,2) NOT NULL,
    method          VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id  VARCHAR(100),
    paid_at         TIMESTAMP,
    refunded_at     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_transaction ON payments(transaction_id);

CREATE TABLE reviews (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID NOT NULL UNIQUE REFERENCES bookings(id),
    student_id  UUID NOT NULL REFERENCES users(id),
    teacher_id  BIGINT NOT NULL REFERENCES teachers(id),
    rating      SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    content     TEXT,
    tags        TEXT[],
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reviews_teacher ON reviews(teacher_id, created_at DESC);
CREATE INDEX idx_reviews_student ON reviews(student_id, created_at DESC);

CREATE TABLE conversations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id          UUID NOT NULL REFERENCES users(id),
    teacher_id          BIGINT NOT NULL REFERENCES teachers(id),
    last_message_content TEXT,
    last_message_at      TIMESTAMP,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(student_id, teacher_id)
);
CREATE INDEX idx_conv_student ON conversations(student_id, last_message_at DESC);
CREATE INDEX idx_conv_teacher ON conversations(teacher_id, last_message_at DESC);

CREATE TABLE chat_messages (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id  UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id        UUID NOT NULL REFERENCES users(id),
    receiver_id      UUID NOT NULL REFERENCES users(id),
    content          TEXT NOT NULL,
    message_type     VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    is_read          BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_msg_conv ON chat_messages(conversation_id, sent_at);

CREATE TABLE notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id),
    title        VARCHAR(200) NOT NULL,
    content      TEXT NOT NULL,
    type         VARCHAR(30) NOT NULL,
    reference_id VARCHAR(100),
    is_read      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notif_user ON notifications(user_id, is_read, created_at DESC);

CREATE TABLE favorites (
    id          BIGSERIAL PRIMARY KEY,
    student_id  UUID NOT NULL REFERENCES users(id),
    teacher_id  BIGINT NOT NULL REFERENCES teachers(id),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(student_id, teacher_id)
);
CREATE INDEX idx_fav_student ON favorites(student_id, created_at DESC);

CREATE TABLE banners (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    link_type   VARCHAR(20) NOT NULL DEFAULT 'NONE',
    link_value  VARCHAR(500),
    sort_order  INTEGER NOT NULL DEFAULT 0,
    city_code   VARCHAR(10),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_banners_city ON banners(city_code, is_active, sort_order);
