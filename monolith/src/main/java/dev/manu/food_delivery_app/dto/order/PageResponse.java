package dev.manu.food_delivery_app.dto.order;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T> (
        List<T> content,
       int page,
       int size,
       long totalElements,
       int totalPages
){
    public static <S, T> PageResponse<T> from(Page<S> p, List<T> mapped) {
        return new PageResponse<>(mapped, p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }
}
