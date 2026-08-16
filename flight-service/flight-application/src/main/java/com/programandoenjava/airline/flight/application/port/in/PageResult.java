package com.programandoenjava.airline.flight.application.port.in;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        content = List.copyOf(content);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mapped = content.stream().map(mapper).toList();
        return new PageResult<>(mapped, page, size, totalElements);
    }
}
