package com.stockflow.controller;

import com.stockflow.dto.SaleRequest;
import com.stockflow.dto.SaleResponse;
import com.stockflow.service.SaleService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "http://localhost:4200")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public List<SaleResponse> getSales() {
        return saleService.getSales();
    }

    @GetMapping("/{id}")
    public SaleResponse getSale(@PathVariable Long id) {
        return saleService.getSale(id);
    }

    @PostMapping
    public ResponseEntity<SaleResponse> createSale(
            @Valid @RequestBody SaleRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(saleService.createSale(request));
    }
}