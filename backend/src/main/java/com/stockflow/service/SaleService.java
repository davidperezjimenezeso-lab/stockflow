
package com.stockflow.service;

import com.stockflow.dto.*;
import com.stockflow.model.*;
import com.stockflow.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    public SaleService(
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            ProductRepository productRepository,
            StockMovementRepository movementRepository) {

        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public SaleResponse createSale(SaleRequest request) {

        // Evitar productos repetidos en una misma venta
        Set<Long> productIds = new HashSet<>();

        for (SaleItemRequest item : request.items()) {
            if (!productIds.add(item.productId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No puedes repetir el mismo producto en una venta"
                );
            }
        }

        // Bloquear productos en orden para reducir deadlocks
        Map<Long, Product> products = new HashMap<>();

        List<Long> sortedIds = new ArrayList<>(productIds);
        Collections.sort(sortedIds);

        for (Long productId : sortedIds) {
            Product product = productRepository
                    .findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Producto no encontrado: " + productId
                    ));

            products.put(productId, product);
        }

        // Comprobar stock y calcular total antes de modificar datos
        BigDecimal total = BigDecimal.ZERO;

        for (SaleItemRequest item : request.items()) {

            Product product = products.get(item.productId());

            if (product.getStock() < item.quantity()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para " + product.getName()
                );
            }

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(item.quantity()));

            total = total.add(subtotal);
        }

        // Crear cabecera de venta
        Sale sale = saleRepository.save(new Sale(total));

        List<SaleItemResponse> responseItems = new ArrayList<>();

        for (SaleItemRequest requestItem : request.items()) {

            Product product = products.get(requestItem.productId());
            int quantity = requestItem.quantity();

            // Guardar línea de venta
            SaleItem saleItem = saleItemRepository.save(
                    new SaleItem(sale, product, quantity)
            );

            // Descontar stock
            product.setStock(product.getStock() - quantity);

            // Registrar salida en el historial
            StockMovement movement = new StockMovement(
                    product,
                    MovementType.OUT,
                    quantity,
                    product.getStock()
            );

            movement.setSale(sale);
            movementRepository.save(movement);

            responseItems.add(new SaleItemResponse(
                    product.getId(),
                    product.getName(),
                    quantity,
                    saleItem.getUnitPrice(),
                    saleItem.getSubtotal()
            ));
        }

        return new SaleResponse(
                sale.getId(),
                sale.getCreatedAt(),
                sale.getTotal(),
                responseItems
        );
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getSales() {
        return saleRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SaleResponse getSale(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Venta no encontrada"
                ));

        return toResponse(sale);
    }

    private SaleResponse toResponse(Sale sale) {

        List<SaleItemResponse> items = saleItemRepository
                .findBySaleIdOrderByIdAsc(sale.getId())
                .stream()
                .map(item -> new SaleItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new SaleResponse(
                sale.getId(),
                sale.getCreatedAt(),
                sale.getTotal(),
                items
        );
    }
}
