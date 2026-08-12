package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.application.city.GetCitiesUseCase;
import io.micrometer.core.annotation.Timed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cities")
public class CityController {

    private final GetCitiesUseCase getCitiesUseCase;

    public CityController(GetCitiesUseCase getCitiesUseCase) {
        this.getCitiesUseCase = getCitiesUseCase;
    }

    @GetMapping
    @Timed(value = "neopick.cities.get_all", description = "Get all cities")
    public ApiResponse<List<Map<String, Object>>> getAll() {
        return ApiResponse.success(getCitiesUseCase.allCities());
    }

    @GetMapping("/hot")
    @Timed(value = "neopick.cities.get_hot", description = "Get hot cities")
    public ApiResponse<List<Map<String, Object>>> getHot() {
        return ApiResponse.success(getCitiesUseCase.hotCities());
    }
}
