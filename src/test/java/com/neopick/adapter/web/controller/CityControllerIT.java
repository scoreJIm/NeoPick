package com.neopick.adapter.web.controller;

import com.neopick.adapter.persistence.entity.CityJpaEntity;
import com.neopick.adapter.persistence.repository.CityJpaRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CityController.class)
@Import(GetCitiesUseCase.class)
@DisplayName("City API Integration Tests")
class CityControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean
    private CityJpaRepository cityJpaRepository;

    @Nested
    @DisplayName("GET /api/v1/cities - All cities")
    class AllCities {

        @Test
        @DisplayName("should return all cities")
        void shouldReturnAllCities() throws Exception {
            CityJpaEntity shanghai = buildCity("SH", "Shanghai", 1, true);
            CityJpaEntity beijing = buildCity("BJ", "Beijing", 2, true);

            when(cityJpaRepository.findAll()).thenReturn(List.of(shanghai, beijing));

            mockMvc.perform(get("/api/v1/cities"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].code").value("SH"))
                    .andExpect(jsonPath("$.data[1].code").value("BJ"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cities/hot - Hot cities")
    class HotCities {

        @Test
        @DisplayName("should return hot cities")
        void shouldReturnHotCities() throws Exception {
            CityJpaEntity shanghai = buildCity("SH", "Shanghai", 1, true);

            when(cityJpaRepository.findByHotTrueOrderBySortOrderAsc())
                    .thenReturn(List.of(shanghai));

            mockMvc.perform(get("/api/v1/cities/hot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }

    private CityJpaEntity buildCity(String code, String name, int sortOrder, boolean hot) {
        CityJpaEntity entity = new CityJpaEntity();
        entity.setCode(code);
        entity.setName(name);
        entity.setSortOrder(sortOrder);
        entity.setHot(hot);
        return entity;
    }
}
