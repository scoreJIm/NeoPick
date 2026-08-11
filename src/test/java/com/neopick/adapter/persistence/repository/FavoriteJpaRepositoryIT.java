package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.FavoriteJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("FavoriteJpaRepository Integration Tests")
class FavoriteJpaRepositoryIT {

    @Autowired private FavoriteJpaRepository repository;

    @BeforeEach
    void setUp() {
        repository.save(createFavorite("student-1", 100L));
        repository.save(createFavorite("student-1", 200L));
        repository.save(createFavorite("student-2", 100L));
    }

    @Nested
    @DisplayName("Find by student")
    class FindByStudent {

        @Test
        @DisplayName("should return student's favorites")
        void shouldReturnFavorites() {
            List<FavoriteJpaEntity> favs = repository.findByStudentIdOrderByCreatedAtDesc(
                    "student-1", PageRequest.of(0, 20));

            assertThat(favs).hasSize(2);
            assertThat(favs).allMatch(f -> f.getStudentId().equals("student-1"));
        }
    }

    @Nested
    @DisplayName("Check existence")
    class Exists {

        @Test
        @DisplayName("should detect existing favorite")
        void shouldDetectExisting() {
            assertThat(repository.existsByStudentIdAndTeacherId("student-1", 100L)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-favorited")
        void shouldReturnFalseForNonexistent() {
            assertThat(repository.existsByStudentIdAndTeacherId("student-1", 999L)).isFalse();
        }
    }

    @Nested
    @DisplayName("Delete favorite")
    class Delete {

        @Test
        @DisplayName("should remove favorite and verify deleted")
        void shouldRemoveFavorite() {
            assertThat(repository.existsByStudentIdAndTeacherId("student-2", 100L)).isTrue();

            repository.deleteByStudentIdAndTeacherId("student-2", 100L);

            assertThat(repository.existsByStudentIdAndTeacherId("student-2", 100L)).isFalse();
        }
    }

    private FavoriteJpaEntity createFavorite(String studentId, Long teacherId) {
        FavoriteJpaEntity entity = new FavoriteJpaEntity();
        entity.setStudentId(studentId);
        entity.setTeacherId(teacherId);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
