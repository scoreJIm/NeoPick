package com.neopick.domain.review;

import com.neopick.domain.common.AggregateRoot;

import java.time.LocalDateTime;
import java.util.List;

public class Review implements AggregateRoot {

    private ReviewId id;
    private String bookingId;
    private String studentId;
    private Long teacherId;
    private int rating;
    private String content;
    private List<String> tags;
    private LocalDateTime createdAt;

    private Review() {
    }

    public Review(ReviewId id, String bookingId, String studentId, Long teacherId,
                  int rating, String content, List<String> tags) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.id = id;
        this.bookingId = bookingId;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.rating = rating;
        this.content = content;
        this.tags = tags != null ? tags : List.of();
        this.createdAt = LocalDateTime.now();
    }

    public ReviewId getId() { return id; }
    public String getBookingId() { return bookingId; }
    public String getStudentId() { return studentId; }
    public Long getTeacherId() { return teacherId; }
    public int getRating() { return rating; }
    public String getContent() { return content; }
    public List<String> getTags() { return tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
