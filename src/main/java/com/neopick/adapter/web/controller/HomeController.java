package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.homepage.HomePageResponse;
import com.neopick.adapter.web.dto.teacher.TeacherCardResponse;
import com.neopick.application.homepage.GetHomePageUseCase;
import com.neopick.infrastructure.ratelimit.RateLimit;
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
@RequestMapping("/api/v1/home")
@Tag(name = "Home", description = "Homepage data aggregation: banners, categories, and teacher highlights")
public class HomeController {

    private final GetHomePageUseCase getHomePageUseCase;

    public HomeController(GetHomePageUseCase getHomePageUseCase) {
        this.getHomePageUseCase = getHomePageUseCase;
    }

    @GetMapping
    @RateLimit(limit = 30, windowSeconds = 60)
    @Timed(value = "neopick.home.get", description = "Get home page")
    @Operation(summary = "Get homepage data", description = "Returns aggregated homepage content including banners, categories, popular teachers, and featured teachers for the specified city.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Homepage data returned successfully")
    })
    public ApiResponse<HomePageResponse> home(
            @Parameter(description = "City code (e.g., SH for Shanghai)", example = "SH") @RequestParam(defaultValue = "SH") String city) {
        var result = getHomePageUseCase.execute(city);
        return ApiResponse.success(new HomePageResponse(
                result.banners().stream().map(b -> new HomePageResponse.Banner(
                        b.getId(), b.getTitle(), b.getImageUrl(),
                        b.getLinkType(), b.getLinkValue())).toList(),
                result.categories().stream().map(c -> new HomePageResponse.Category(
                        c.getId(), c.getName(), c.getIconUrl())).toList(),
                result.popularTeachers().stream().map(TeacherCardResponse::from).toList(),
                result.featuredTeachers().stream().map(TeacherCardResponse::from).toList()
        ));
    }
}
