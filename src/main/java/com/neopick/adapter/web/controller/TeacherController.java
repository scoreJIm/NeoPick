package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.common.PageResponse;
import com.neopick.adapter.web.dto.teacher.TeacherCardResponse;
import com.neopick.application.teacher.*;
import com.neopick.shared.Constants;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
public class TeacherController {

    private final SearchTeachersUseCase searchTeachersUseCase;
    private final GetTeacherDetailUseCase getTeacherDetailUseCase;
    private final GetFeaturedTeachersUseCase getFeaturedTeachersUseCase;
    private final GetPopularTeachersUseCase getPopularTeachersUseCase;
    private final GetWeeklyRecommendationsUseCase getWeeklyRecommendationsUseCase;

    public TeacherController(SearchTeachersUseCase searchTeachersUseCase,
                             GetTeacherDetailUseCase getTeacherDetailUseCase,
                             GetFeaturedTeachersUseCase getFeaturedTeachersUseCase,
                             GetPopularTeachersUseCase getPopularTeachersUseCase,
                             GetWeeklyRecommendationsUseCase getWeeklyRecommendationsUseCase) {
        this.searchTeachersUseCase = searchTeachersUseCase;
        this.getTeacherDetailUseCase = getTeacherDetailUseCase;
        this.getFeaturedTeachersUseCase = getFeaturedTeachersUseCase;
        this.getPopularTeachersUseCase = getPopularTeachersUseCase;
        this.getWeeklyRecommendationsUseCase = getWeeklyRecommendationsUseCase;
    }

    @GetMapping
    public ApiResponse<PageResponse<TeacherCardResponse>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Double priceMin,
            @RequestParam(required = false) Double priceMax,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(size, Constants.MAX_PAGE_SIZE);
        SearchTeachersUseCase.SearchCommand command = new SearchTeachersUseCase.SearchCommand(
                keyword, city, category, gender, level, priceMin, priceMax, sort, page, safeSize);
        var result = searchTeachersUseCase.execute(command);
        List<TeacherCardResponse> cards = result.teachers().stream()
                .map(TeacherCardResponse::from).toList();
        return ApiResponse.success(PageResponse.of(cards, page, safeSize, result.total()));
    }

    @GetMapping("/{id}")
    public ApiResponse<TeacherCardResponse> getById(@PathVariable Long id) {
        var teacher = getTeacherDetailUseCase.execute(id);
        return ApiResponse.success(TeacherCardResponse.from(teacher));
    }

    @GetMapping("/featured")
    public ApiResponse<List<TeacherCardResponse>> featured(
            @RequestParam String city,
            @RequestParam(defaultValue = "6") int limit) {
        var teachers = getFeaturedTeachersUseCase.execute(city, limit);
        return ApiResponse.success(teachers.stream().map(TeacherCardResponse::from).toList());
    }

    @GetMapping("/popular")
    public ApiResponse<List<TeacherCardResponse>> popular(
            @RequestParam String city,
            @RequestParam(defaultValue = "6") int limit) {
        var teachers = getPopularTeachersUseCase.execute(city, limit);
        return ApiResponse.success(teachers.stream().map(TeacherCardResponse::from).toList());
    }

    @GetMapping("/weekly-recommendations")
    public ApiResponse<List<TeacherCardResponse>> weeklyRecommendations(
            @RequestParam String city,
            @RequestParam(defaultValue = "10") int limit) {
        var teachers = getWeeklyRecommendationsUseCase.execute(city, limit);
        return ApiResponse.success(teachers.stream().map(TeacherCardResponse::from).toList());
    }
}
