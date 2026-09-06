package com.eventplatform.event.service;

import com.eventplatform.event.domain.EventCategory;
import com.eventplatform.event.dto.CreateCategoryRequest;
import com.eventplatform.event.dto.EventCategoryResponse;
import com.eventplatform.event.exception.ApiException;
import com.eventplatform.event.repository.EventCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final EventCategoryRepository categoryRepository;

    public CategoryService(EventCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<EventCategoryResponse> list() {
        return categoryRepository.findAll().stream().map(EventCategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EventCategoryResponse get(UUID id) {
        return EventCategoryResponse.from(require(id));
    }

    @Transactional
    public EventCategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim()) || categoryRepository.existsBySlug(request.slug())) {
            throw ApiException.conflict("Category name or slug already exists");
        }
        EventCategory category = new EventCategory();
        category.setName(request.name().trim());
        category.setSlug(request.slug().trim());
        category.setDescription(request.description() == null ? null : request.description().trim());
        categoryRepository.save(category);
        return EventCategoryResponse.from(category);
    }

    public EventCategory require(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
    }
}
