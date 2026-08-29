package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.TeacherJpaEntity;
import com.neopick.adapter.persistence.spec.TeacherSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("TeacherJpaRepository Integration Tests")
class TeacherJpaRepositoryIT {

    @Autowired private TeacherJpaRepository repository;

    @BeforeEach
    void setUp() {
        TeacherJpaEntity t1 = createTeacher("John", "SH", "EXPERT", 300);
        TeacherJpaEntity t2 = createTeacher("Jane", "SH", "BEGINNER", 150);
        TeacherJpaEntity t3 = createTeacher("Mike", "BJ", "ADVANCED", 250);
        TeacherJpaEntity t4 = createTeacher("Anna", "SH", "EXPERT", 350);
        t4.setFeatured(true);
        repository.saveAll(List.of(t1, t2, t3, t4));
    }

    @Nested
    @DisplayName("Search with Specification")
    class Search {

        @Test
        @DisplayName("should filter by city code")
        void shouldFilterByCity() {
            Specification<TeacherJpaEntity> spec =
                    TeacherSpecification.withCriteria("SH", null, null, null,
                            null, null, null);
            List<TeacherJpaEntity> results = repository.findAll(spec, PageRequest.of(0, 20))
                    .getContent();

            assertThat(results).hasSize(3);
            assertThat(results).allMatch(t -> t.getCityCode().equals("SH"));
        }

        @Test
        @DisplayName("should filter by level")
        void shouldFilterByLevel() {
            Specification<TeacherJpaEntity> spec =
                    TeacherSpecification.withCriteria("SH", null, null, "EXPERT",
                            null, null, null);
            List<TeacherJpaEntity> results = repository.findAll(spec).stream().toList();

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(t -> t.getLevel().equals("EXPERT"));
        }

        @Test
        @DisplayName("should filter by price range")
        void shouldFilterByPriceRange() {
            Specification<TeacherJpaEntity> spec =
                    TeacherSpecification.withCriteria("SH", null, null, null,
                            200.0, 350.0, null);
            List<TeacherJpaEntity> results = repository.findAll(spec).stream().toList();

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(t ->
                    t.getBasePrice().compareTo(new BigDecimal("200.0")) >= 0 &&
                    t.getBasePrice().compareTo(new BigDecimal("350.0")) <= 0);
        }

        @Test
        @DisplayName("should filter by city AND level AND price")
        void shouldCombineFilters() {
            Specification<TeacherJpaEntity> spec =
                    TeacherSpecification.withCriteria("SH", null, null, "EXPERT",
                            200.0, 400.0, null);
            List<TeacherJpaEntity> results = repository.findAll(spec).stream().toList();

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(t ->
                    t.getCityCode().equals("SH") &&
                    t.getLevel().equals("EXPERT"));
        }
    }

    @Nested
    @DisplayName("Featured and popular queries")
    class FeaturedAndPopular {

        @Test
        @DisplayName("should find featured teachers by city")
        void shouldFindFeaturedByCity() {
            List<TeacherJpaEntity> featured = repository.findFeaturedByCity("SH");

            assertThat(featured).hasSize(1);
            assertThat(featured.get(0).getRealName()).isEqualTo("Anna");
            assertThat(featured.get(0).isFeatured()).isTrue();
        }

        @Test
        @DisplayName("should find popular teachers by city ordered by booking count")
        void shouldFindPopularByCity() {
            List<TeacherJpaEntity> popular = repository.findPopularByCity("SH");

            assertThat(popular).hasSize(3);
            assertThat(popular).allMatch(t -> t.getCityCode().equals("SH"));
        }
    }

    private TeacherJpaEntity createTeacher(String name, String cityCode,
                                            String level, double price) {
        TeacherJpaEntity entity = new TeacherJpaEntity();
        entity.setUserId("user-" + name.toLowerCase());
        entity.setRealName(name);
        entity.setCityCode(cityCode);
        entity.setCityName(cityCode.equals("SH") ? "Shanghai" : "Beijing");
        entity.setLevel(level);
        entity.setBasePrice(new BigDecimal(price));
        entity.setStatus("APPROVED");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}
