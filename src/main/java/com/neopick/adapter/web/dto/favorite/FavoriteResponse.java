package com.neopick.adapter.web.dto.favorite;

import com.neopick.domain.favorite.Favorite;

public record FavoriteResponse(
        String studentId,
        Long teacherId,
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
