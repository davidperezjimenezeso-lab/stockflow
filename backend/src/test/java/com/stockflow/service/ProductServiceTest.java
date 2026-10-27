
package com.stockflow.service;
import com.stockflow.repository.StockMovementRepository;
import com.stockflow.dto.ProductRequest;
import com.stockflow.dto.ProductResponse;
import com.stockflow.model.Product;
import com.stockflow.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldReturnAllProducts() {

        Product product = new Product(
                "Monitor Samsung",
                new BigDecimal("199.99"),
                8
        );

        when(productRepository.findAll())
                .thenReturn(List.of(product));

        List<ProductResponse> result = productService.getProducts();

        assertEquals(1, result.size());
        assertEquals("Monitor Samsung", result.get(0).name());
        assertEquals(new BigDecimal("199.99"), result.get(0).price());

        verify(productRepository).findAll();
    }

    @Test
    void shouldCreateProduct() {

        ProductRequest request = new ProductRequest(
                "Teclado Logitech",
                new BigDecimal("49.99"),
                20,
                null
        );

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = productService.createProduct(request);

        assertEquals("Teclado Logitech", result.name());
        assertEquals(new BigDecimal("49.99"), result.price());
        assertEquals(20, result.stock());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldUpdateExistingProduct() {

        Product existing = new Product(
                "Monitor antiguo",
                new BigDecimal("150.00"),
                5
        );

        ProductRequest request = new ProductRequest(
                "Monitor actualizado",
                new BigDecimal("220.00"),
                5,
                null
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = productService.updateProduct(1L, request);

        assertEquals("Monitor actualizado", result.name());
        assertEquals(new BigDecimal("220.00"), result.price());

        // El stock no se modifica desde la edición.
        assertEquals(5, result.stock());

        verify(productRepository).save(existing);
    }

    @Test
    void shouldThrow404WhenUpdatingMissingProduct() {

        ProductRequest request = new ProductRequest(
                "Producto inexistente",
                new BigDecimal("50.00"),
                3,
                null
        );

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.updateProduct(999L, request)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    void shouldDeleteExistingProduct() {

        when(productRepository.existsById(1L))
                .thenReturn(true);

        when(movementRepository.existsByProductId(1L))
                .thenReturn(false);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void shouldThrow404WhenDeletingMissingProduct() {

        when(productRepository.existsById(999L))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.deleteProduct(999L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(productRepository, never())
                .deleteById(anyLong());
    }
}
