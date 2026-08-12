package com.neopick.adapter.web.dto.favorite;

import com.neopick.domain.favorite.Favorite;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher favorite record for a student")
public record FavoriteResponse(
        @Schema(description = "Student user ID who favorited the teacher", example = "user_student_001")
        String studentId,

        @Schema(description = "Favorited teacher ID", example = "42")
        Long teacherId,

        @Schema(description = "When the teacher was favorited", example = "2024-06-14T10:00:00")
        String createdAt
) {
    public static FavoriteResponse from(Favorite f) {
        return new FavoriteResponse(
                f.getStudentId(),
                f.getTeacherId(),
                f.getCreatedAt() != null ? f.getCreatedAt().toString() : null
        );
    }
}
