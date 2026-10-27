package com.stockflow.controller;

import com.stockflow.dto.StockMovementRequest;
import com.stockflow.dto.StockMovementResponse;
import com.stockflow.service.StockMovementService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
@CrossOrigin(origins = "http://localhost:4200")
public class StockMovementController {

    private final StockMovementService movementService;

    public StockMovementController(StockMovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    public List<StockMovementResponse> getMovements() {
        return movementService.getMovements();
    }

    @GetMapping("/product/{productId}")
    public List<StockMovementResponse> getMovementsByProduct(
            @PathVariable Long productId) {
        return movementService.getMovementsByProduct(productId);
    }

    @PostMapping
    public ResponseEntity<StockMovementResponse> createMovement(
            @Valid @RequestBody StockMovementRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(movementService.createMovement(request));
    }
}
