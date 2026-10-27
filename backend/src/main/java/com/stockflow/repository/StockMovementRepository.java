package com.stockflow.repository;

import com.stockflow.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findAllByOrderByCreatedAtDesc();

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    boolean existsByProductId(Long productId);
}