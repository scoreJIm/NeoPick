package com.neopick.adapter.web.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response wrapper")
public record PageResponse<T>(
        @Schema(description = "List of items for the current page") List<T> content,
        @Schema(description = "Current page index (zero-based)", example = "0") int page,
        @Schema(description = "Page size", example = "20") int size,
        @Schema(description = "Total number of elements across all pages", example = "150") long totalElements,
        @Schema(description = "Total number of pages", example = "8") int totalPages,
        @Schema(description = "Whether this is the first page", example = "true") boolean first,
        @Schema(description = "Whether this is the last page", example = "false") boolean last
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PageResponse<>(
                content, page, size, totalElements, totalPages,
                page == 0, page >= totalPages - 1
        );
    }
}
