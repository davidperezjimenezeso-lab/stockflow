package com.stockflow.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        Instant createdAt,
        BigDecimal total,
        List<SaleItemResponse> items
) {}