package com.innercircle.sacco.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CursorPage<T> {

    private final List<T> items;
    private final String nextCursor;
    private final boolean hasMore;
    private final int size;

    public static <T> CursorPage<T> of(List<T> items, String nextCursor, boolean hasMore) {
        return CursorPage.<T>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .size(items.size())
                .build();
    }

    public static <T> CursorPage<T> empty() {
        return CursorPage.<T>builder()
                .items(List.of())
                .hasMore(false)
                .size(0)
                .build();
    }
}
