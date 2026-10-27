package com.stockflow.repository;

import com.stockflow.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    List<SaleItem> findBySaleIdOrderByIdAsc(Long saleId);
}