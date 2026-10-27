
package com.stockflow.service;

import com.stockflow.dto.*;
import com.stockflow.model.*;
import com.stockflow.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @InjectMocks
    private SaleService saleService;

    private Product monitor;
    private Product mouse;

    @BeforeEach
    void setUp() {

        monitor = createProduct(
                1L, "Monitor Samsung", "200.00", 10
        );

        mouse = createProduct(
                2L, "Ratón Logitech", "25.50", 20
        );
    }

    private Product createProduct(
            Long id,
            String name,
            String price,
            int stock
    ) {
        Product product = new Product(
                name,
                new BigDecimal(price),
                stock
        );

        ReflectionTestUtils.setField(product, "id", id);

        return product;
    }

    private void mockSuccessfulSalePersistence() {

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> {

                    Sale sale = invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            sale, "id", 1L
                    );

                    return sale;
                });

        when(saleItemRepository.save(any(SaleItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // TEST 1: Una venta descuenta correctamente el stock.
    @Test
    void shouldDecreaseStockAfterSale() {

        SaleRequest request = new SaleRequest(
                List.of(new SaleItemRequest(1L, 3))
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(monitor));

        mockSuccessfulSalePersistence();

        SaleResponse response = saleService.createSale(request);

        assertEquals(7, monitor.getStock());

        assertEquals(
                0,
                new BigDecimal("600.00")
                        .compareTo(response.total())
        );

        assertEquals(1, response.items().size());
        assertEquals(3, response.items().get(0).quantity());

        verify(saleRepository).save(any(Sale.class));
        verify(saleItemRepository).save(any(SaleItem.class));
        verify(movementRepository).save(any(StockMovement.class));
    }

    // TEST 2: Venta con varios productos y total correcto.
    @Test
    void shouldCalculateTotalForMultipleProducts() {

        SaleRequest request = new SaleRequest(
                List.of(
                        new SaleItemRequest(1L, 2),
                        new SaleItemRequest(2L, 4)
                )
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(monitor));

        when(productRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(mouse));

        mockSuccessfulSalePersistence();

        SaleResponse response = saleService.createSale(request);

        // 2 * 200.00 + 4 * 25.50 = 502.00
        assertEquals(
                0,
                new BigDecimal("502.00")
                        .compareTo(response.total())
        );

        assertEquals(8, monitor.getStock());
        assertEquals(16, mouse.getStock());

        assertEquals(2, response.items().size());

        verify(saleItemRepository, times(2))
                .save(any(SaleItem.class));

        verify(movementRepository, times(2))
                .save(any(StockMovement.class));
    }

    // TEST 3: No vender más unidades de las disponibles.
    @Test
    void shouldRejectSaleWhenStockIsInsufficient() {

        SaleRequest request = new SaleRequest(
                List.of(new SaleItemRequest(1L, 15))
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(monitor));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.createSale(request)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(10, monitor.getStock());

        verify(saleRepository, never())
                .save(any(Sale.class));

        verify(saleItemRepository, never())
                .save(any(SaleItem.class));

        verify(movementRepository, never())
                .save(any(StockMovement.class));
    }

    // TEST 4: Rechazar productos inexistentes.
    @Test
    void shouldRejectSaleWhenProductDoesNotExist() {

        SaleRequest request = new SaleRequest(
                List.of(new SaleItemRequest(999L, 1))
        );

        when(productRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.createSale(request)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(saleRepository, never())
                .save(any(Sale.class));
    }

    // TEST 5: No repetir un producto en la misma venta.
    @Test
    void shouldRejectDuplicatedProducts() {

        SaleRequest request = new SaleRequest(
                List.of(
                        new SaleItemRequest(1L, 2),
                        new SaleItemRequest(1L, 3)
                )
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.createSale(request)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(
                productRepository,
                saleRepository,
                saleItemRepository,
                movementRepository
        );
    }

    // TEST 6: Poder vender exactamente todo el stock.
    @Test
    void shouldAllowSaleThatLeavesZeroStock() {

        SaleRequest request = new SaleRequest(
                List.of(new SaleItemRequest(1L, 10))
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(monitor));

        mockSuccessfulSalePersistence();

        SaleResponse response = saleService.createSale(request);

        assertEquals(0, monitor.getStock());

        assertEquals(
                0,
                new BigDecimal("2000.00")
                        .compareTo(response.total())
        );

        verify(movementRepository)
                .save(any(StockMovement.class));
    }

    // TEST 7: Bloquear los productos en orden ascendente.
    @Test
    void shouldLockProductsInSortedOrder() {

        // Enviamos primero el producto 2 y luego el 1.
        SaleRequest request = new SaleRequest(
                List.of(
                        new SaleItemRequest(2L, 1),
                        new SaleItemRequest(1L, 1)
                )
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(monitor));

        when(productRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(mouse));

        mockSuccessfulSalePersistence();

        saleService.createSale(request);

        var order = inOrder(productRepository);

        order.verify(productRepository).findByIdForUpdate(1L);
        order.verify(productRepository).findByIdForUpdate(2L);
    }

    // TEST 8: Historial vacío si todavía no hay ventas.
    @Test
    void shouldReturnEmptySalesHistory() {

        when(saleRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of());

        List<SaleResponse> result = saleService.getSales();

        assertTrue(result.isEmpty());

        verify(saleRepository).findAllByOrderByCreatedAtDesc();
    }

    // TEST 9: Devolver 404 si una venta no existe.
    @Test
    void shouldRejectRequestForMissingSale() {

        when(saleRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.getSale(999L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(saleItemRepository, never())
                .findBySaleIdOrderByIdAsc(any());
    }

    // TEST 10: Obtener el detalle de una venta existente.
    @Test
    void shouldReturnExistingSaleDetails() {

        Sale sale = new Sale(new BigDecimal("400.00"));

        ReflectionTestUtils.setField(sale, "id", 5L);

        SaleItem item = new SaleItem(sale, monitor, 2);

        when(saleRepository.findById(5L))
                .thenReturn(Optional.of(sale));

        when(saleItemRepository.findBySaleIdOrderByIdAsc(5L))
                .thenReturn(List.of(item));

        SaleResponse response = saleService.getSale(5L);

        assertEquals(5L, response.id());

        assertEquals(
                0,
                new BigDecimal("400.00")
                        .compareTo(response.total())
        );

        assertEquals(1, response.items().size());
        assertEquals("Monitor Samsung",
                response.items().get(0).productName());

        assertEquals(2, response.items().get(0).quantity());
    }
}
