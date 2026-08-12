package com.neopick.application.teacher;

import com.neopick.domain.teacher.Teacher;
import com.neopick.domain.teacher.TeacherRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetPopularTeachersUseCase {

    private final TeacherRepository teacherRepository;

    public GetPopularTeachersUseCase(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @Cacheable(value = "popularTeachers", key = "#cityCode + ':' + #limit", unless = "#result.isEmpty()")
    public List<Teacher> execute(String cityCode, int limit) {
        return teacherRepository.findPopular(cityCode, limit);
    }
}
