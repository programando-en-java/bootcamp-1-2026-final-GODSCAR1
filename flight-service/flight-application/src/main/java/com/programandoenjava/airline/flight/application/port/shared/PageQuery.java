package com.programandoenjava.airline.flight.application.port.shared;

import java.util.List;

public record PageQuery(int page, int size, List<SortOrder> sort) {

    public static final int MAX_SIZE = 20;
    public static final int DEFAULT_SIZE = 20;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be negative, was: " + page);
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be positive, was: " + size);
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must not exceed " + MAX_SIZE + ", was: " + size);
        }
        sort = List.copyOf(sort);
    }

    public static PageQuery of(final int page, final int size) {
        return new PageQuery(page, size, List.of());
    }

    public boolean isUnsorted() {
        return sort.isEmpty();
    }

    public record SortOrder(SortableField field, Direction direction) {
    }

    public enum Direction {
        ASC, DESC
    }
}
