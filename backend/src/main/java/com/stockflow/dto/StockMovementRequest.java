package com.stockflow.dto;

import com.stockflow.model.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockMovementRequest(
        @NotNull Long productId,
        @NotNull MovementType movementType,
        @NotNull @Min(1) Integer quantity
) {}
