
package com.stockflow.service;

import com.stockflow.dto.StockMovementRequest;
import com.stockflow.dto.StockMovementResponse;
import com.stockflow.model.MovementType;
import com.stockflow.model.Product;
import com.stockflow.model.StockMovement;
import com.stockflow.repository.ProductRepository;
import com.stockflow.repository.StockMovementRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StockMovementService {

    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;

    public StockMovementService(
            StockMovementRepository movementRepository,
            ProductRepository productRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public StockMovementResponse createMovement(StockMovementRequest request) {

        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto no encontrado"
                ));

        int quantity = request.quantity();

        if (request.movementType() == MovementType.IN) {
            if (product.getStock() > Integer.MAX_VALUE - quantity) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La cantidad supera el stock máximo permitido"
                );
            }

            product.setStock(product.getStock() + quantity);

        } else {
            if (product.getStock() < quantity) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Stock insuficiente"
                );
            }

            product.setStock(product.getStock() - quantity);
        }

        productRepository.save(product);

        StockMovement movement = new StockMovement(
                product,
                request.movementType(),
                quantity,
                product.getStock()
        );

        StockMovement saved = movementRepository.save(movement);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovements() {
        return movementRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovementsByProduct(Long productId) {

        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Producto no encontrado"
            );
        }

        return movementRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private StockMovementResponse toResponse(StockMovement movement) {
        Product product = movement.getProduct();

        return new StockMovementResponse(
                movement.getId(),
                product.getId(),
                product.getName(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getResultingStock(),
                movement.getCreatedAt()
        );
    }
}
