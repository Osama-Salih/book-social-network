package com.osama.book.feedback.request;

import jakarta.validation.constraints.*;

public record FeedbackRequest(
        @Positive
        @Min(value = 0, message = "200")
        @Max(value = 5, message = "200")
        Double note,

        @NotNull(message = "300")
        @NotEmpty(message = "301")
        @NotBlank(message = "302")
        String comment,

        @NotNull(message = "300")
        Integer bookId
) {}
