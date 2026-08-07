package com.neopick.domain.favorite;

import java.util.List;

public interface FavoriteRepository {

    Favorite save(Favorite favorite);

    void delete(String studentId, Long teacherId);

    List<Favorite> findByStudentId(String studentId, int page, int size);

    boolean exists(String studentId, Long teacherId);
}
