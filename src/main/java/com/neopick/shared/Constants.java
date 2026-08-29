package com.neopick.shared;

public final class Constants {

    private Constants() {
    }

    public static final String API_V1 = "/api/v1";

    public static final int SMS_CODE_LENGTH = 6;
    public static final int SMS_RATE_LIMIT_SECONDS = 60;
    public static final int SMS_DAILY_LIMIT = 5;

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String CHINA_PHONE_REGEX = "^\\+86\\d{11}$";

    public static final long BOOKING_PENDING_CONFIRM_TIMEOUT_HOURS = 24;
    public static final long BOOKING_PENDING_PAY_TIMEOUT_HOURS = 2;

    public static final int HOME_CACHE_TTL_SECONDS = 600;
    public static final int TEACHER_DETAIL_CACHE_TTL_SECONDS = 3600;
    public static final int CITY_LIST_CACHE_TTL_SECONDS = 86400;
}
