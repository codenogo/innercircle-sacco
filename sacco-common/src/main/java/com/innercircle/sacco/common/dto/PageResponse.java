package com.innercircle.sacco.common.dto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * Stable API representation for Spring Data page results.
 */
public record PageResponse<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty,
        List<SortOrder> sort
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        List<SortOrder> sortOrders = page.getSort().stream()
                .map(order -> new SortOrder(
                        order.getProperty(),
                        order.getDirection().name(),
                        order.isIgnoreCase(),
                        order.getNullHandling().name()
                ))
                .toList();

        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty(),
                sortOrders
        );
    }

    public record SortOrder(
            String property,
            String direction,
            boolean ignoreCase,
            String nullHandling
    ) {
    }
}
