package com.ecobook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PagedResponseDTO<T> {
    private List<T> results;
    private int page;
    private int size;
    private long total;
    private int totalPages;
    private boolean hasNext;

    public static <T> PagedResponseDTO<T> of(List<T> results, int page, int size, long total) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / (double) size);
        boolean hasNext = page + 1 < totalPages;
        return PagedResponseDTO.<T>builder()
                .results(results)
                .page(page)
                .size(size)
                .total(total)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .build();
    }
}
