package com.stockflow.service;

import com.stockflow.dto.CategoryRequest;
import com.stockflow.dto.CategoryResponse;
import com.stockflow.model.Category;
import com.stockflow.repository.CategoryRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(c -> new CategoryResponse(
                        c.getId(),
                        c.getName()
                ))
                .toList();
    }

    public CategoryResponse createCategory(CategoryRequest request) {

        Category category = new Category(request.name());

        Category saved = categoryRepository.save(category);

        return new CategoryResponse(
                saved.getId(),
                saved.getName()
        );
    }
}
