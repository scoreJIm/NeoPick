package com.neopick.application.teacher;

import com.neopick.domain.teacher.Teacher;
import com.neopick.domain.teacher.TeacherRepository;
import com.neopick.domain.teacher.TeacherSearchCriteria;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchTeachersUseCase {

    private final TeacherRepository teacherRepository;

    public SearchTeachersUseCase(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    public SearchResult execute(SearchCommand command) {
        TeacherSearchCriteria criteria = new TeacherSearchCriteria(
                command.keyword(), command.cityCode(), command.categoryId(),
                command.gender(), command.level(), command.priceMin(), command.priceMax(),
                command.sort(), command.page(), command.size());
        List<Teacher> teachers = teacherRepository.search(criteria);
        long total = teacherRepository.count(criteria);
        return new SearchResult(teachers, total);
    }

    public record SearchCommand(
            String keyword, String cityCode, Long categoryId, String gender,
            String level, Double priceMin, Double priceMax, String sort, int page, int size) {}

    public record SearchResult(List<Teacher> teachers, long total) {}
}
