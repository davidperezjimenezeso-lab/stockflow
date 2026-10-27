package com.stockflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SaleRequest(
        @NotEmpty List<@Valid SaleItemRequest> items
) {}