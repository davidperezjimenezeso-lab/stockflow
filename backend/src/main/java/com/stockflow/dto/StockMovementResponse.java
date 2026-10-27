package com.stockflow.dto;

import com.stockflow.model.MovementType;
import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long productId,
        String productName,
        MovementType movementType,
        int quantity,
        int resultingStock,
        Instant createdAt
) {}