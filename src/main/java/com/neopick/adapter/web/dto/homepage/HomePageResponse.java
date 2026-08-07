package com.neopick.adapter.web.dto.homepage;

import com.neopick.adapter.web.dto.teacher.TeacherCardResponse;

import java.util.List;

public record HomePageResponse(
        List<Banner> banners,
        List<Category> categories,
        List<TeacherCardResponse> popularTeachers,
        List<TeacherCardResponse> featuredTeachers
) {
    public record Banner(Long id, String title, String imageUrl, String linkType, String linkValue) {}
    public record Category(Long id, String name, String iconUrl) {}
}
