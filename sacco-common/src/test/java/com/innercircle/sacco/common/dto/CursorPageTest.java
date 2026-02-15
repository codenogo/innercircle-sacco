package com.innercircle.sacco.common.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CursorPageTest {

    @Test
    void of_withItemsAndCursor_shouldCreatePageWithCorrectValues() {
        List<String> items = List.of("item1", "item2", "item3");
        String nextCursor = "cursor-abc-123";

        CursorPage<String> page = CursorPage.of(items, nextCursor, true);

        assertThat(page.getItems()).containsExactly("item1", "item2", "item3");
        assertThat(page.getNextCursor()).isEqualTo("cursor-abc-123");
        assertThat(page.isHasMore()).isTrue();
        assertThat(page.getSize()).isEqualTo(3);
    }

    @Test
    void of_withNoMoreItems_shouldSetHasMoreFalse() {
        List<String> items = List.of("item1");

        CursorPage<String> page = CursorPage.of(items, null, false);

        assertThat(page.getItems()).containsExactly("item1");
        assertThat(page.getNextCursor()).isNull();
        assertThat(page.isHasMore()).isFalse();
        assertThat(page.getSize()).isEqualTo(1);
    }

    @Test
    void of_withEmptyList_shouldCreatePageWithZeroSize() {
        List<String> items = List.of();

        CursorPage<String> page = CursorPage.of(items, null, false);

        assertThat(page.getItems()).isEmpty();
        assertThat(page.getNextCursor()).isNull();
        assertThat(page.isHasMore()).isFalse();
        assertThat(page.getSize()).isEqualTo(0);
    }

    @Test
    void of_sizeShouldMatchItemCount() {
        List<Integer> items = List.of(10, 20, 30, 40, 50);

        CursorPage<Integer> page = CursorPage.of(items, "next", true);

        assertThat(page.getSize()).isEqualTo(5);
        assertThat(page.getItems()).hasSize(5);
    }

    @Test
    void empty_shouldCreateEmptyPage() {
        CursorPage<String> page = CursorPage.empty();

        assertThat(page.getItems()).isEmpty();
        assertThat(page.getNextCursor()).isNull();
        assertThat(page.isHasMore()).isFalse();
        assertThat(page.getSize()).isEqualTo(0);
    }

    @Test
    void empty_withDifferentTypes_shouldWork() {
        CursorPage<Integer> intPage = CursorPage.empty();
        CursorPage<Double> doublePage = CursorPage.empty();

        assertThat(intPage.getItems()).isEmpty();
        assertThat(intPage.getSize()).isEqualTo(0);
        assertThat(doublePage.getItems()).isEmpty();
        assertThat(doublePage.getSize()).isEqualTo(0);
    }

    @Test
    void builder_shouldCreatePageWithAllFields() {
        CursorPage<String> page = CursorPage.<String>builder()
                .items(List.of("a", "b"))
                .nextCursor("cursor-next")
                .hasMore(true)
                .size(2)
                .build();

        assertThat(page.getItems()).containsExactly("a", "b");
        assertThat(page.getNextCursor()).isEqualTo("cursor-next");
        assertThat(page.isHasMore()).isTrue();
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void constructor_shouldSetAllFields() {
        List<String> items = List.of("x", "y");

        CursorPage<String> page = new CursorPage<>(items, "cur", true, 2);

        assertThat(page.getItems()).containsExactly("x", "y");
        assertThat(page.getNextCursor()).isEqualTo("cur");
        assertThat(page.isHasMore()).isTrue();
        assertThat(page.getSize()).isEqualTo(2);
    }
}
