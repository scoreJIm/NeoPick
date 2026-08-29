package com.neopick.domain.teacher;

public record TeacherSearchCriteria(
        String keyword,
        String cityCode,
        Long categoryId,
        String gender,
        String level,
        Double priceMin,
        Double priceMax,
        String sort,
        int page,
        int size
) {
    public int offset() {
        return page * size;
    }
}
