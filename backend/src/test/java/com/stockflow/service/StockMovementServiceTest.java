
package com.stockflow.service;

import com.stockflow.dto.StockMovementRequest;
import com.stockflow.dto.StockMovementResponse;
import com.stockflow.model.MovementType;
import com.stockflow.model.Product;
import com.stockflow.model.StockMovement;
import com.stockflow.repository.ProductRepository;
import com.stockflow.repository.StockMovementRepository;

import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockMovementService movementService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product(
                "Monitor Samsung",
                new BigDecimal("199.99"),
                10
        );
    }

    // TEST 1: Registrar una entrada incrementa el stock.
    @Test
    void shouldIncreaseStockWhenMovementIsIn() {

        StockMovementRequest request =
                new StockMovementRequest(1L, MovementType.IN, 5);

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(movementRepository.save(any(StockMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementResponse response =
                movementService.createMovement(request);

        assertEquals(15, product.getStock());
        assertEquals(15, response.resultingStock());
        assertEquals(MovementType.IN, response.movementType());
        assertEquals(5, response.quantity());

        verify(productRepository).save(product);
        verify(movementRepository).save(any(StockMovement.class));
    }

    // TEST 2: Registrar una salida reduce el stock.
    @Test
    void shouldDecreaseStockWhenMovementIsOut() {

        StockMovementRequest request =
                new StockMovementRequest(1L, MovementType.OUT, 4);

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(movementRepository.save(any(StockMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementResponse response =
                movementService.createMovement(request);

        assertEquals(6, product.getStock());
        assertEquals(6, response.resultingStock());
        assertEquals(MovementType.OUT, response.movementType());

        verify(productRepository).save(product);
        verify(movementRepository).save(any(StockMovement.class));
    }

    // TEST 3: No se puede retirar más stock del disponible.
    @Test
    void shouldRejectMovementWhenStockIsInsufficient() {

        StockMovementRequest request =
                new StockMovementRequest(1L, MovementType.OUT, 20);

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> movementService.createMovement(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(10, product.getStock());

        verify(productRepository, never()).save(any(Product.class));
        verify(movementRepository, never()).save(any(StockMovement.class));
    }

    // TEST 4: Se puede retirar exactamente el stock disponible.
    @Test
    void shouldAllowMovementThatLeavesZeroStock() {

        StockMovementRequest request =
                new StockMovementRequest(1L, MovementType.OUT, 10);

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(movementRepository.save(any(StockMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementResponse response =
                movementService.createMovement(request);

        assertEquals(0, product.getStock());
        assertEquals(0, response.resultingStock());

        verify(productRepository).save(product);
        verify(movementRepository).save(any(StockMovement.class));
    }

    // TEST 5: Un producto inexistente devuelve HTTP 404.
    @Test
    void shouldRejectMovementWhenProductDoesNotExist() {

        StockMovementRequest request =
                new StockMovementRequest(999L, MovementType.IN, 5);

        when(productRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> movementService.createMovement(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productRepository, never()).save(any(Product.class));
        verify(movementRepository, never()).save(any(StockMovement.class));
    }

    // TEST 6: Evitar desbordamiento del valor máximo de stock.
    @Test
    void shouldRejectMovementWhenStockExceedsIntegerLimit() {

        product.setStock(Integer.MAX_VALUE - 2);

        StockMovementRequest request =
                new StockMovementRequest(1L, MovementType.IN, 5);

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> movementService.createMovement(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(Integer.MAX_VALUE - 2, product.getStock());

        verify(productRepository, never()).save(any(Product.class));
        verify(movementRepository, never()).save(any(StockMovement.class));
    }

    // TEST 7: Consultar el historial general.
    @Test
    void shouldReturnMovementHistory() {

        when(movementRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of());

        List<StockMovementResponse> result =
                movementService.getMovements();

        assertTrue(result.isEmpty());

        verify(movementRepository).findAllByOrderByCreatedAtDesc();
    }

    // TEST 8: Consultar historial de un producto existente.
    @Test
    void shouldReturnMovementsForExistingProduct() {

        when(productRepository.existsById(1L))
                .thenReturn(true);

        when(movementRepository.findByProductIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        List<StockMovementResponse> result =
                movementService.getMovementsByProduct(1L);

        assertTrue(result.isEmpty());

        verify(movementRepository)
                .findByProductIdOrderByCreatedAtDesc(1L);
    }

    // TEST 9: Consultar historial de un producto inexistente.
    @Test
    void shouldRejectHistoryRequestForMissingProduct() {

        when(productRepository.existsById(999L))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> movementService.getMovementsByProduct(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(movementRepository, never())
                .findByProductIdOrderByCreatedAtDesc(any());
    }
}
