package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.application.city.GetCitiesUseCase;
import com.neopick.infrastructure.ratelimit.RateLimit;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cities")
@Tag(name = "Cities", description = "Available cities for teacher search and filtering")
public class CityController {

    private final GetCitiesUseCase getCitiesUseCase;

    public CityController(GetCitiesUseCase getCitiesUseCase) {
        this.getCitiesUseCase = getCitiesUseCase;
    }

    @GetMapping
    @RateLimit(limit = 120, windowSeconds = 60)
    @Timed(value = "neopick.cities.get_all", description = "Get all cities")
    @Operation(summary = "Get all cities", description = "Returns a list of all available cities with their codes and names for filtering purposes.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All cities returned successfully")
    })
    public ApiResponse<List<Map<String, Object>>> getAll() {
        return ApiResponse.success(getCitiesUseCase.allCities());
    }

    @GetMapping("/hot")
    @RateLimit(limit = 120, windowSeconds = 60)
    @Timed(value = "neopick.cities.get_hot", description = "Get hot cities")
    @Operation(summary = "Get hot cities", description = "Returns a curated list of popular/hot cities with active teacher communities.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Hot cities returned successfully")
    })
    public ApiResponse<List<Map<String, Object>>> getHot() {
        return ApiResponse.success(getCitiesUseCase.hotCities());
    }
}
