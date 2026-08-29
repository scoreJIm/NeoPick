package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.common.PageResponse;
import com.neopick.adapter.web.dto.teacher.TeacherCardResponse;
import com.neopick.application.teacher.*;
import com.neopick.infrastructure.ratelimit.RateLimit;
import com.neopick.shared.Constants;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
@Tag(name = "Teachers", description = "Teacher search, detail, and discovery endpoints")
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
    @RateLimit(limit = 30, windowSeconds = 60, scope = "IP")
    @Timed(value = "neopick.teachers.search", description = "Search teachers")
    @Operation(summary = "Search teachers", description = "Full-text search and filter teachers by city, category, gender, level, price range, and sort order. Results are paginated.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filter parameters", content = @Content)
    })
    public ApiResponse<PageResponse<TeacherCardResponse>> search(
            @Parameter(description = "City code (e.g., SH for Shanghai, BJ for Beijing)") @RequestParam(required = false) String city,
            @Parameter(description = "Category ID for filtering by instrument/style") @RequestParam(required = false) Long category,
            @Parameter(description = "Teacher gender filter (MALE, FEMALE)") @RequestParam(required = false) String gender,
            @Parameter(description = "Teaching level filter (BEGINNER, INTERMEDIATE, ADVANCED)") @RequestParam(required = false) String level,
            @Parameter(description = "Minimum hourly price") @RequestParam(required = false) Double priceMin,
            @Parameter(description = "Maximum hourly price") @RequestParam(required = false) Double priceMax,
            @Parameter(description = "Sort order (rating, price_asc, price_desc, reviews)") @RequestParam(required = false) String sort,
            @Parameter(description = "Free-text keyword search") @RequestParam(required = false) String keyword,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(size, Constants.MAX_PAGE_SIZE);
        SearchTeachersUseCase.SearchCommand command = new SearchTeachersUseCase.SearchCommand(
                keyword, city, category, gender, level, priceMin, priceMax, sort, page, safeSize);
        var result = searchTeachersUseCase.execute(command);
        List<TeacherCardResponse> cards = result.teachers().stream()
                .map(TeacherCardResponse::from).toList();
        return ApiResponse.success(PageResponse.of(cards, page, safeSize, result.total()));
    }

    @GetMapping("/{id}")
    @RateLimit(limit = 60, windowSeconds = 60, scope = "IP")
    @Timed(value = "neopick.teachers.get_by_id", description = "Get teacher detail")
    @Operation(summary = "Get teacher detail", description = "Returns complete teacher profile information by teacher ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Teacher detail retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Teacher not found", content = @Content)
    })
    public ApiResponse<TeacherCardResponse> getById(
            @Parameter(description = "Teacher ID") @PathVariable Long id) {
        var teacher = getTeacherDetailUseCase.execute(id);
        return ApiResponse.success(TeacherCardResponse.from(teacher));
    }

    @GetMapping("/featured")
    @RateLimit(limit = 60, windowSeconds = 60, scope = "IP")
    @Timed(value = "neopick.teachers.featured", description = "Get featured teachers")
    @Operation(summary = "Get featured teachers", description = "Returns a curated list of featured teachers for a specific city.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Featured teachers returned successfully")
    })
    public ApiResponse<List<TeacherCardResponse>> featured(
            @Parameter(description = "City code (e.g., SH, BJ)") @RequestParam String city,
            @Parameter(description = "Maximum number of results") @RequestParam(defaultValue = "6") int limit) {
        var teachers = getFeaturedTeachersUseCase.execute(city, limit);
        return ApiResponse.success(teachers.stream().map(TeacherCardResponse::from).toList());
    }

    @GetMapping("/popular")
    @RateLimit(limit = 60, windowSeconds = 60, scope = "IP")
    @Timed(value = "neopick.teachers.popular", description = "Get popular teachers")
    @Operation(summary = "Get popular teachers", description = "Returns a list of the most popular teachers based on booking volume.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Popular teachers returned successfully")
    })
    public ApiResponse<List<TeacherCardResponse>> popular(
            @Parameter(description = "City code (e.g., SH, BJ)") @RequestParam String city,
            @Parameter(description = "Maximum number of results") @RequestParam(defaultValue = "6") int limit) {
        var teachers = getPopularTeachersUseCase.execute(city, limit);
        return ApiResponse.success(teachers.stream().map(TeacherCardResponse::from).toList());
    }

    @GetMapping("/weekly-recommendations")
    @RateLimit(limit = 60, windowSeconds = 60, scope = "IP")
    @Timed(value = "neopick.teachers.weekly_recommendations", description = "Get weekly recommendations")
    @Operation(summary = "Get weekly recommendations", description = "Returns personalized weekly teacher recommendations for the specified city.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Weekly recommendations returned successfully")
    })
    public ApiResponse<List<TeacherCardResponse>> weeklyRecommendations(
            @Parameter(description = "City code (e.g., SH, BJ)") @RequestParam String city,
            @Parameter(description = "Maximum number of results") @RequestParam(defaultValue = "10") int limit) {
        var teachers = getWeeklyRecommendationsUseCase.execute(city, limit);
        return ApiResponse.success(teachers.stream().map(TeacherCardResponse::from).toList());
    }
}
