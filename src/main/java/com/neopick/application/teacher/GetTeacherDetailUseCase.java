package com.neopick.application.teacher;

import com.neopick.domain.teacher.Teacher;
import com.neopick.domain.teacher.TeacherId;
import com.neopick.domain.teacher.TeacherRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GetTeacherDetailUseCase {

    private final TeacherRepository teacherRepository;

    public GetTeacherDetailUseCase(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @Cacheable(value = "teacherDetail", key = "#teacherId", unless = "#result == null")
    public Teacher execute(Long teacherId) {
        return teacherRepository.findById(new TeacherId(teacherId))
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));
    }
}
