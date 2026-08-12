package com.neopick.application.teacher;

import com.neopick.domain.teacher.Teacher;
import com.neopick.domain.teacher.TeacherRepository;
import com.neopick.shared.Constants;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetFeaturedTeachersUseCase {

    private final TeacherRepository teacherRepository;

    public GetFeaturedTeachersUseCase(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @Cacheable(value = "featuredTeachers", key = "#cityCode + ':' + #limit", unless = "#result.isEmpty()")
    public List<Teacher> execute(String cityCode, int limit) {
        int actualLimit = Math.min(limit, Constants.MAX_PAGE_SIZE);
        return teacherRepository.findFeatured(cityCode, actualLimit);
    }
}
