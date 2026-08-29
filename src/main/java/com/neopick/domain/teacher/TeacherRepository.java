package com.neopick.domain.teacher;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository {

    Teacher save(Teacher teacher);

    Optional<Teacher> findById(TeacherId id);

    List<Teacher> search(TeacherSearchCriteria criteria);

    long count(TeacherSearchCriteria criteria);

    List<Teacher> findFeatured(String cityCode, int limit);

    List<Teacher> findPopular(String cityCode, int limit);
}
