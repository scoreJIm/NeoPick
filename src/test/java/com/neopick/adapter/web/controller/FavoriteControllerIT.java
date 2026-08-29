package com.neopick.adapter.web.controller;

import com.neopick.application.favorite.FavoriteUseCase;
import com.neopick.domain.favorite.Favorite;
import com.neopick.domain.favorite.FavoriteRepository;
import com.neopick.port.security.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FavoriteController.class)
@Import(FavoriteUseCase.class)
@DisplayName("Favorite API Integration Tests")
class FavoriteControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private FavoriteRepository favoriteRepository;
    @MockBean private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        when(securityContext.requireCurrentUserId()).thenReturn("student-001");
        when(securityContext.getCurrentUserId()).thenReturn(Optional.of("student-001"));
    }

    @Nested
    @DisplayName("POST /api/v1/favorites — Add favorite")
    class AddFavorite {

        @Test
        @DisplayName("should add a favorite teacher")
        void shouldAddFavorite() throws Exception {
            when(favoriteRepository.exists("student-001", 100L)).thenReturn(false);
            when(favoriteRepository.save(any(Favorite.class)))
                    .thenReturn(new Favorite("student-001", 100L));

            mockMvc.perform(post("/api/v1/favorites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"teacherId\": 100}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.teacher_id").value(100L));
        }

        @Test
        @DisplayName("should reject duplicate favorite")
        void shouldRejectDuplicate() throws Exception {
            when(favoriteRepository.exists("student-001", 100L)).thenReturn(true);

            mockMvc.perform(post("/api/v1/favorites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"teacherId\": 100}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/favorites/{teacherId} — Remove favorite")
    class RemoveFavorite {

        @Test
        @DisplayName("should remove a favorite")
        void shouldRemoveFavorite() throws Exception {
            mockMvc.perform(delete("/api/v1/favorites/{teacherId}", 100L))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/favorites — List favorites")
    class ListFavorites {

        @Test
        @DisplayName("should return favorite list")
        void shouldReturnFavorites() throws Exception {
            when(favoriteRepository.findByStudentId("student-001", 0, 20))
                    .thenReturn(List.of(new Favorite("student-001", 100L)));

            mockMvc.perform(get("/api/v1/favorites"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].teacher_id").value(100L));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/favorites/check/{teacherId} — Check favorite status")
    class CheckFavorite {

        @Test
        @DisplayName("should return true when favorited")
        void shouldReturnFavorited() throws Exception {
            when(favoriteRepository.exists("student-001", 100L)).thenReturn(true);

            mockMvc.perform(get("/api/v1/favorites/check/{teacherId}", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isFavorited").value(true));
        }

        @Test
        @DisplayName("should return false when not favorited")
        void shouldReturnNotFavorited() throws Exception {
            when(favoriteRepository.exists("student-001", 999L)).thenReturn(false);

            mockMvc.perform(get("/api/v1/favorites/check/{teacherId}", 999L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isFavorited").value(false));
        }
    }
}
