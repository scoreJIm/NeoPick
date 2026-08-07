package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.application.city.GetCitiesUseCase;
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
    public ApiResponse<List<Map<String, Object>>> getAll() {
        return ApiResponse.success(getCitiesUseCase.allCities());
    }

    @GetMapping("/hot")
    public ApiResponse<List<Map<String, Object>>> getHot() {
        return ApiResponse.success(getCitiesUseCase.hotCities());
    }
}
