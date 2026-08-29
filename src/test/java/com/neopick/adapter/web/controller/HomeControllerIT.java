package com.neopick.adapter.web.controller;

import com.neopick.adapter.persistence.entity.BannerJpaEntity;
import com.neopick.adapter.persistence.entity.CategoryJpaEntity;
import com.neopick.adapter.persistence.repository.BannerJpaRepository;
import com.neopick.adapter.persistence.repository.CategoryJpaRepository;
import com.neopick.application.homepage.GetHomePageUseCase;
import com.neopick.domain.teacher.*;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import(GetHomePageUseCase.class)
@DisplayName("Homepage API Integration Tests")
class HomeControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private BannerJpaRepository bannerJpaRepository;
    @MockBean private CategoryJpaRepository categoryJpaRepository;
    @MockBean private TeacherRepository teacherRepository;

    @Nested
    @DisplayName("GET /api/v1/home — Homepage aggregation")
    class HomePage {

        @Test
        @DisplayName("should return homepage with banners, categories, teachers")
        void shouldReturnHomePage() throws Exception {
            when(bannerJpaRepository.findActiveByCity("SH"))
                    .thenReturn(List.of(createBanner(1L)));
            when(categoryJpaRepository.findByActiveTrueOrderBySortOrderAsc())
                    .thenReturn(List.of(createCategory(1L, "Classical Guitar")));
            when(teacherRepository.findPopular(anyString(), anyInt()))
                    .thenReturn(List.of(createTeacher(1L, "John")));
            when(teacherRepository.findFeatured(anyString(), anyInt()))
                    .thenReturn(List.of(createTeacher(2L, "Jane")));

            mockMvc.perform(get("/api/v1/home")
                            .param("city", "SH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.banners", hasSize(1)))
                    .andExpect(jsonPath("$.data.categories", hasSize(1)))
                    .andExpect(jsonPath("$.data.popular_teachers", hasSize(1)))
                    .andExpect(jsonPath("$.data.featured_teachers", hasSize(1)))
                    .andExpect(jsonPath("$.data.categories[0].name").value("Classical Guitar"));
        }

        @Test
        @DisplayName("should default city to SH when not provided")
        void shouldDefaultCity() throws Exception {
            when(bannerJpaRepository.findActiveByCity("SH"))
                    .thenReturn(List.of());
            when(categoryJpaRepository.findByActiveTrueOrderBySortOrderAsc())
                    .thenReturn(List.of());
            when(teacherRepository.findPopular(anyString(), anyInt()))
                    .thenReturn(List.of());
            when(teacherRepository.findFeatured(anyString(), anyInt()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/home"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.popular_teachers", hasSize(0)));
        }
    }

    private BannerJpaEntity createBanner(Long id) {
        BannerJpaEntity b = new BannerJpaEntity();
        b.setId(id);
        b.setTitle("Summer Sale");
        b.setImageUrl("https://cdn.example.com/banner1.jpg");
        b.setLinkType("TEACHER");
        b.setLinkValue("1");
        return b;
    }

    private CategoryJpaEntity createCategory(Long id, String name) {
        CategoryJpaEntity c = new CategoryJpaEntity();
        c.setId(id);
        c.setName(name);
        c.setIconUrl("https://cdn.example.com/icon.png");
        return c;
    }

    private Teacher createTeacher(Long id, String name) {
        return new Teacher(new TeacherId(id), "user-" + id, name,
                TeacherLevel.ADVANCED, new BigDecimal("250.00"),
                new City("SH", "Shanghai"), "Jing'an");
    }
}
