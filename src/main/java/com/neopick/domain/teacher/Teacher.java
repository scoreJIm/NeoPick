package com.neopick.domain.teacher;

import com.neopick.domain.common.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Teacher implements AggregateRoot {

    private TeacherId id;
    private String userId;
    private String realName;
    private String bio;
    private TeacherLevel level;
    private Integer teachingYears;
    private BigDecimal basePrice;
    private City city;
    private String district;
    private String coverImageUrl;
    private BigDecimal rating;
    private Integer reviewCount;
    private Integer bookingCount;
    private boolean featured;
    private TeacherStatus status;
    private List<String> tags;
    private List<Long> categoryIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Teacher() {
    }

    public Teacher(TeacherId id, String userId, String realName, TeacherLevel level,
                   BigDecimal basePrice, City city, String district) {
        this.id = id;
        this.userId = userId;
        this.realName = realName;
        this.level = level;
        this.basePrice = basePrice;
        this.city = city;
        this.district = district;
        this.rating = BigDecimal.ZERO;
        this.reviewCount = 0;
        this.bookingCount = 0;
        this.featured = false;
        this.status = TeacherStatus.APPROVED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRating(int newRating) {
        int total = this.reviewCount;
        BigDecimal totalScore = this.rating.multiply(BigDecimal.valueOf(total));
        totalScore = totalScore.add(BigDecimal.valueOf(newRating));
        this.reviewCount = total + 1;
        this.rating = totalScore.divide(BigDecimal.valueOf(this.reviewCount),
                java.math.RoundingMode.HALF_UP);
    }

    public void incrementBookingCount() {
        this.bookingCount++;
    }

    public TeacherId getId() { return id; }
    public String getUserId() { return userId; }
    public String getRealName() { return realName; }
    public String getBio() { return bio; }
    public TeacherLevel getLevel() { return level; }
    public Integer getTeachingYears() { return teachingYears; }
    public BigDecimal getBasePrice() { return basePrice; }
    public City getCity() { return city; }
    public String getDistrict() { return district; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public BigDecimal getRating() { return rating; }
    public Integer getReviewCount() { return reviewCount; }
    public Integer getBookingCount() { return bookingCount; }
    public boolean isFeatured() { return featured; }
    public TeacherStatus getStatus() { return status; }
    public List<String> getTags() { return tags; }
    public List<Long> getCategoryIds() { return categoryIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
