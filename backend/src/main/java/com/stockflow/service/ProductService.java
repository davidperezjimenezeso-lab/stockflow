package com.stockflow.service;

import com.stockflow.dto.ProductRequest;
import com.stockflow.dto.ProductResponse;
import com.stockflow.model.Category;
import com.stockflow.model.Product;
import com.stockflow.repository.CategoryRepository;
import com.stockflow.repository.ProductRepository;

import com.stockflow.repository.StockMovementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository movementRepository;
    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            StockMovementRepository movementRepository) {

        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.movementRepository = movementRepository;
    }

    // GET - Listar productos
    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // POST - Crear producto
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        Product product = new Product(
                request.name(),
                request.price(),
                request.stock()
        );

        product.setCategory(
                resolveCategory(request.categoryId())
        );

        Product saved = productRepository.save(product);

        return toResponse(saved);
    }

    // PUT - Actualizar producto
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto no encontrado"
                ));

        product.setName(request.name());
        product.setPrice(request.price());


        product.setCategory(
                resolveCategory(request.categoryId())
        );

        return toResponse(productRepository.save(product));
    }

    // DELETE - Eliminar producto
    @Transactional
    public void deleteProduct(Long id) {
        if (movementRepository.existsByProductId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar un producto con movimientos registrados"
            );
        }
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Producto no encontrado"
            );
        }

        productRepository.deleteById(id);
    }

    // Buscar la categoría asociada al producto
    private Category resolveCategory(Long categoryId) {

        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Categoría no encontrada"
                ));
    }

    // Convertir entidad JPA a DTO
    private ProductResponse toResponse(Product product) {

        Category category = product.getCategory();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null
        );
    }
}