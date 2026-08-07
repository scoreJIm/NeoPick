package com.neopick.application.homepage;

import com.neopick.adapter.persistence.entity.BannerJpaEntity;
import com.neopick.adapter.persistence.entity.CategoryJpaEntity;
import com.neopick.adapter.persistence.repository.BannerJpaRepository;
import com.neopick.adapter.persistence.repository.CategoryJpaRepository;
import com.neopick.domain.teacher.Teacher;
import com.neopick.domain.teacher.TeacherRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetHomePageUseCase {

    private final BannerJpaRepository bannerRepo;
    private final CategoryJpaRepository categoryRepo;
    private final TeacherRepository teacherRepository;

    public GetHomePageUseCase(BannerJpaRepository bannerRepo, CategoryJpaRepository categoryRepo,
                               TeacherRepository teacherRepository) {
        this.bannerRepo = bannerRepo;
        this.categoryRepo = categoryRepo;
        this.teacherRepository = teacherRepository;
    }

    public HomePageResult execute(String cityCode) {
        List<BannerJpaEntity> banners = bannerRepo.findActiveByCity(cityCode);
        List<CategoryJpaEntity> categories = categoryRepo.findByActiveTrueOrderBySortOrderAsc();
        List<Teacher> popular = teacherRepository.findPopular(cityCode, 6);
        List<Teacher> featured = teacherRepository.findFeatured(cityCode, 6);
        return new HomePageResult(banners, categories, popular, featured);
    }

    public record HomePageResult(
            List<BannerJpaEntity> banners,
            List<CategoryJpaEntity> categories,
            List<Teacher> popularTeachers,
            List<Teacher> featuredTeachers) {}
}
