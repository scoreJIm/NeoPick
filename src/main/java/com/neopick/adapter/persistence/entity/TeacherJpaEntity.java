package com.neopick.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "teachers")
public class TeacherJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(name = "bio")
    private String bio;

    @Column(name = "level", nullable = false, length = 20)
    private String level;

    @Column(name = "teaching_years")
    private Integer teachingYears;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "city_code", nullable = false, length = 10)
    private String cityCode;

    @Column(name = "city_name", nullable = false, length = 50)
    private String cityName;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "booking_count", nullable = false)
    private Integer bookingCount;

    @Column(name = "is_featured", nullable = false)
    private boolean featured;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TeacherJpaEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Integer getTeachingYears() { return teachingYears; }
    public void setTeachingYears(Integer teachingYears) { this.teachingYears = teachingYears; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getCityCode() { return cityCode; }
    public void setCityCode(String cityCode) { this.cityCode = cityCode; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public Integer getBookingCount() { return bookingCount; }
    public void setBookingCount(Integer bookingCount) { this.bookingCount = bookingCount; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
