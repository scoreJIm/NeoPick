package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.homepage.HomePageResponse;
import com.neopick.adapter.web.dto.teacher.TeacherCardResponse;
import com.neopick.application.homepage.GetHomePageUseCase;
import io.micrometer.core.annotation.Timed;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/home")
public class HomeController {

    private final GetHomePageUseCase getHomePageUseCase;

    public HomeController(GetHomePageUseCase getHomePageUseCase) {
        this.getHomePageUseCase = getHomePageUseCase;
    }

    @GetMapping
    @Timed(value = "neopick.home.get", description = "Get home page")
    public ApiResponse<HomePageResponse> home(@RequestParam(defaultValue = "SH") String city) {
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
