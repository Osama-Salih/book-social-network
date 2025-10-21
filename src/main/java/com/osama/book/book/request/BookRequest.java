package com.osama.book.book.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record BookRequest(
        Integer id,

        @NotNull(message = "Title is required")
        @NotEmpty(message = "Title can't be empty")
        String title,

        @NotNull(message = "Author name is required")
        @NotEmpty(message = "Author name can't be empty")
        String authorName,

        @NotNull(message = "isbn is required")
        @NotEmpty(message = "isbn can't be empty")
        String isbn,

        @NotNull(message = "synopsis is required")
        @NotEmpty(message = "synopsis can't be empty")
        String synopsis,

        @NotNull(message = "shareable is required")
        @NotEmpty(message = "shareable can't be empty")
        boolean shareable
) {}
