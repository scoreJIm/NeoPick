package com.neopick.adapter.web.controller;

import com.neopick.application.city.GetCitiesUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CityController.class)
@Import(GetCitiesUseCase.class)
@DisplayName("City API Integration Tests")
class CityControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean(name = "cityRepository")  // GetCitiesUseCase uses CityRepository port
    private com.neopick.domain.teacher.CityRepository cityRepository;

    @Nested
    @DisplayName("GET /api/v1/cities — All cities")
    class AllCities {

        @Test
        @DisplayName("should return all cities")
        void shouldReturnAllCities() throws Exception {
            when(cityRepository.findAll()).thenReturn(List.of(
                    Map.of("code", "SH", "name", "Shanghai"),
                    Map.of("code", "BJ", "name", "Beijing")));

            mockMvc.perform(get("/api/v1/cities"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].code").value("SH"))
                    .andExpect(jsonPath("$.data[1].code").value("BJ"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cities/hot — Hot cities")
    class HotCities {

        @Test
        @DisplayName("should return hot cities")
        void shouldReturnHotCities() throws Exception {
            when(cityRepository.findHot()).thenReturn(List.of(
                    Map.of("code", "SH", "name", "Shanghai")));

            mockMvc.perform(get("/api/v1/cities/hot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }
}
