package com.neopick.adapter.web.dto.teacher;

import com.neopick.domain.teacher.Teacher;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Teacher summary card displayed in search results and listings")
public record TeacherCardResponse(
        @Schema(description = "Teacher ID", example = "42") Long id,
        @Schema(description = "Teacher's real name", example = "Zhang Wei") String realName,
        @Schema(description = "Avatar image URL", example = "/avatars/user123") String avatarUrl,
        @Schema(description = "Cover/background image URL", example = "https://cdn.neopick.com/covers/teacher42.jpg") String coverImageUrl,
        @Schema(description = "City information with code and display name") Map<String, String> city,
        @Schema(description = "District within the city", example = "Jing'an") String district,
        @Schema(description = "Teaching level (BEGINNER, INTERMEDIATE, ADVANCED)", example = "ADVANCED") String level,
        @Schema(description = "Short bio/introduction", example = "10 years of guitar teaching experience...") String bio,
        @Schema(description = "Hourly base price in CNY", example = "200.00") double basePrice,
        @Schema(description = "Average rating (0.0 - 5.0)", example = "4.8") double rating,
        @Schema(description = "Number of reviews received", example = "156") int reviewCount,
        @Schema(description = "Number of completed bookings", example = "320") int bookingCount,
        @Schema(description = "Whether the teacher is featured on the homepage", example = "true") boolean featured,
        @Schema(description = "Style/genre tags", example = "[\"Rock\", \"Blues\", \"Fingerstyle\"]") List<String> tags,
        @Schema(description = "Teaching category names", example = "[\"Acoustic Guitar\", \"Electric Guitar\"]") List<String> categories
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
