package com.example.productService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductWriteRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @Positive Double price,
        @NotNull @PositiveOrZero Integer quantity,
        @Size(max = 255) String imageUrl
) {}
