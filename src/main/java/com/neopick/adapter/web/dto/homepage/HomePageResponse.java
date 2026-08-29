package com.neopick.adapter.web.dto.homepage;

import com.neopick.adapter.web.dto.teacher.TeacherCardResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Aggregated homepage content: banners, categories, and teacher highlights")
public record HomePageResponse(
        @Schema(description = "Carousel banners displayed at the top of the homepage")
        List<Banner> banners,

        @Schema(description = "Instrument/style category shortcuts")
        List<Category> categories,

        @Schema(description = "Most popular teachers in the selected city")
        List<TeacherCardResponse> popularTeachers,

        @Schema(description = "Curated featured teachers for the selected city")
        List<TeacherCardResponse> featuredTeachers
) {
    @Schema(description = "Homepage banner/carousel item")
    public record Banner(
            @Schema(description = "Banner ID", example = "1") Long id,
            @Schema(description = "Banner title text", example = "Summer Guitar Camp") String title,
            @Schema(description = "Banner image URL", example = "https://cdn.neopick.com/banners/summer2024.jpg") String imageUrl,
            @Schema(description = "Link type when banner is clicked (TEACHER, URL, CATEGORY)", example = "URL") String linkType,
            @Schema(description = "Link target value", example = "https://neopick.com/summer2024") String linkValue
    ) {}

    @Schema(description = "Category shortcut for instrument/style filtering")
    public record Category(
            @Schema(description = "Category ID", example = "1") Long id,
            @Schema(description = "Category display name", example = "Acoustic Guitar") String name,
            @Schema(description = "Category icon URL", example = "https://cdn.neopick.com/icons/acoustic.png") String iconUrl
    ) {}
}
