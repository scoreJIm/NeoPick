package com.neopick.adapter.web.dto.teacher;

import com.neopick.domain.teacher.Teacher;

import java.util.List;
import java.util.Map;

public record TeacherCardResponse(
        Long id,
        String realName,
        String avatarUrl,
        String coverImageUrl,
        Map<String, String> city,
        String district,
        String level,
        String bio,
        double basePrice,
        double rating,
        int reviewCount,
        int bookingCount,
        boolean featured,
        List<String> tags,
        List<String> categories
) {
    public static TeacherCardResponse from(Teacher teacher) {
        return new TeacherCardResponse(
                teacher.getId().value(),
                teacher.getRealName(),
                "/avatars/" + teacher.getUserId(),
                teacher.getCoverImageUrl(),
                Map.of("code", teacher.getCity().code(), "name", teacher.getCity().name()),
                teacher.getDistrict(),
                teacher.getLevel().name(),
                teacher.getBio(),
                teacher.getBasePrice().doubleValue(),
                teacher.getRating().doubleValue(),
                teacher.getReviewCount(),
                teacher.getBookingCount(),
                teacher.isFeatured(),
                teacher.getTags() != null ? teacher.getTags() : List.of(),
                List.of()
        );
    }
}
