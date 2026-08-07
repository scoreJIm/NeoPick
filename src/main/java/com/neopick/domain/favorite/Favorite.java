package com.neopick.domain.favorite;

import java.time.LocalDateTime;

public class Favorite {

    private Long id;
    private String studentId;
    private Long teacherId;
    private LocalDateTime createdAt;

    private Favorite() {
    }

    public Favorite(String studentId, Long teacherId) {
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getStudentId() { return studentId; }
    public Long getTeacherId() { return teacherId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
