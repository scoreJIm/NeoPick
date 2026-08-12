package com.neopick.adapter.web.controller;

import com.neopick.application.teacher.*;
import com.neopick.domain.teacher.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeacherController.class)
@Import({SearchTeachersUseCase.class, GetTeacherDetailUseCase.class,
         GetFeaturedTeachersUseCase.class, GetPopularTeachersUseCase.class,
         GetWeeklyRecommendationsUseCase.class})
@DisplayName("Teacher API Integration Tests")
class TeacherControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private TeacherRepository teacherRepository;

    private Teacher teacher;

    @BeforeEach
    void setUp() {
        teacher = new Teacher(new TeacherId(1L), "user-1", "John Smith",
                TeacherLevel.EXPERT, new BigDecimal("300.00"),
                new City("SH", "Shanghai"), "Jing'an");
    }

    @Nested
    @DisplayName("GET /api/v1/teachers — Search teachers")
    class SearchTeachers {

        @Test
        @DisplayName("should search with city and level filters")
        void shouldSearchWithFilters() throws Exception {
            when(teacherRepository.search(any(TeacherSearchCriteria.class)))
                    .thenReturn(List.of(teacher));
            when(teacherRepository.count(any(TeacherSearchCriteria.class))).thenReturn(1L);

            mockMvc.perform(get("/api/v1/teachers")
                            .param("city", "SH")
                            .param("level", "EXPERT")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(1)))
                    .andExpect(jsonPath("$.data.items[0].real_name").value("John Smith"))
                    .andExpect(jsonPath("$.data.total").value(1));
        }

        @Test
        @DisplayName("should search with price range filter")
        void shouldSearchByPriceRange() throws Exception {
            when(teacherRepository.search(any(TeacherSearchCriteria.class)))
                    .thenReturn(List.of(teacher));
            when(teacherRepository.count(any(TeacherSearchCriteria.class))).thenReturn(1L);

            mockMvc.perform(get("/api/v1/teachers")
                            .param("price_min", "100")
                            .param("price_max", "500"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(1)));
        }

        @Test
        @DisplayName("should sort by rating descending")
        void shouldSortByRating() throws Exception {
            when(teacherRepository.search(any(TeacherSearchCriteria.class)))
                    .thenReturn(List.of(teacher));
            when(teacherRepository.count(any(TeacherSearchCriteria.class))).thenReturn(1L);

            mockMvc.perform(get("/api/v1/teachers")
                            .param("sort", "rating_desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(1)));
        }

        @Test
        @DisplayName("should return empty results for unmatched search")
        void shouldReturnEmptyResults() throws Exception {
            when(teacherRepository.search(any(TeacherSearchCriteria.class)))
                    .thenReturn(List.of());
            when(teacherRepository.count(any(TeacherSearchCriteria.class))).thenReturn(0L);

            mockMvc.perform(get("/api/v1/teachers")
                            .param("city", "BJ")
                            .param("keyword", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(0)))
                    .andExpect(jsonPath("$.data.total").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teachers/{id} — Teacher detail")
    class TeacherDetail {

        @Test
        @DisplayName("should return teacher by id")
        void shouldReturnTeacherDetail() throws Exception {
            when(teacherRepository.findById(any(TeacherId.class)))
                    .thenReturn(Optional.of(teacher));

            mockMvc.perform(get("/api/v1/teachers/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.real_name").value("John Smith"))
                    .andExpect(jsonPath("$.data.level").value("EXPERT"));
        }

        @Test
        @DisplayName("should return 400 when teacher not found")
        void shouldReturnNotFound() throws Exception {
            when(teacherRepository.findById(any(TeacherId.class)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/teachers/{id}", 999L))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teachers/featured — Featured teachers")
    class FeaturedTeachers {

        @Test
        @DisplayName("should return featured teachers for city")
        void shouldReturnFeaturedTeachers() throws Exception {
            when(teacherRepository.findFeatured(anyString(), anyInt()))
                    .thenReturn(List.of(teacher));

            mockMvc.perform(get("/api/v1/teachers/featured")
                            .param("city", "SH")
                            .param("limit", "6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].real_name").value("John Smith"));
        }

        @Test
        @DisplayName("should require city parameter")
        void shouldRequireCityParam() throws Exception {
            mockMvc.perform(get("/api/v1/teachers/featured"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teachers/popular — Popular teachers")
    class PopularTeachers {

        @Test
        @DisplayName("should return popular teachers for city")
        void shouldReturnPopularTeachers() throws Exception {
            when(teacherRepository.findPopular(anyString(), anyInt()))
                    .thenReturn(List.of(teacher));

            mockMvc.perform(get("/api/v1/teachers/popular")
                            .param("city", "SH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teachers/weekly-recommendations — Weekly recommendations")
    class WeeklyRecommendations {

        @Test
        @DisplayName("should return weekly recommendations")
        void shouldReturnWeeklyRecommendations() throws Exception {
            when(teacherRepository.findPopular(anyString(), anyInt()))
                    .thenReturn(List.of(teacher));

            mockMvc.perform(get("/api/v1/teachers/weekly-recommendations")
                            .param("city", "SH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }
}
