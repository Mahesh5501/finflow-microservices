package com.finflow.commerce.service;

import com.finflow.commerce.dto.CategoryRequest;
import com.finflow.commerce.dto.CategoryResponse;
import com.finflow.commerce.dto.ProductRequest;
import com.finflow.commerce.dto.ProductResponse;
import com.finflow.commerce.dto.ProductUpdateRequest;
import com.finflow.commerce.entity.Category;
import com.finflow.commerce.entity.Product;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.repository.CategoryRepository;
import com.finflow.commerce.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CatalogueService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public CatalogueService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(List<Long> callerOrgIds, CategoryRequest request) {
        requireOrgAccess(callerOrgIds, request.organisationId());
        String slug = request.slug().trim().toLowerCase();
        if (categoryRepository.existsByOrganisationIdAndSlugIgnoreCase(request.organisationId(), slug)) {
            throw ApiException.conflict("CATEGORY_EXISTS", "Category slug already exists");
        }
        Category category = new Category(request.organisationId(), request.name(), slug);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(Long organisationId, List<Long> callerOrgIds) {
        requireOrgAccess(callerOrgIds, organisationId);
        return categoryRepository.findByOrganisationIdOrderByNameAsc(organisationId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(List<Long> callerOrgIds, ProductRequest request) {
        requireOrgAccess(callerOrgIds, request.organisationId());
        Long categoryId = validateCategory(request.organisationId(), request.categoryId());
        Product product = new Product(
                request.organisationId(),
                request.name().trim(),
                request.unitPrice(),
                request.availableQuantity(),
                categoryId
        );
        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(Long organisationId, List<Long> callerOrgIds) {
        requireOrgAccess(callerOrgIds, organisationId);
        Map<Long, String> names = categoryNames(organisationId);
        return productRepository.findByOrganisationIdOrderByProductIdAsc(organisationId).stream()
                .map(p -> ProductResponse.from(p, names.get(p.getCategoryId())))
                .toList();
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, List<Long> callerOrgIds, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Product not found: " + productId));
        requireOrgAccess(callerOrgIds, product.getOrganisationId());
        if (request.unitPrice() != null) {
            product.updateUnitPrice(request.unitPrice());
        }
        if (request.availableQuantity() != null) {
            product.updateAvailableQuantity(request.availableQuantity());
        }
        if (request.categoryId() != null) {
            product.assignCategory(validateCategory(product.getOrganisationId(), request.categoryId()));
        }
        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(
            Long organisationId,
            List<Long> callerOrgIds,
            String q,
            Long categoryId,
            Pageable pageable
    ) {
        requireOrgAccess(callerOrgIds, organisationId);
        if (categoryId != null) {
            validateCategory(organisationId, categoryId);
        }
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organisationId"), organisationId));
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (q != null && !q.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + q.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Map<Long, String> names = categoryNames(organisationId);
        return productRepository.findAll(spec, pageable)
                .map(p -> ProductResponse.from(p, names.get(p.getCategoryId())));
    }

    private ProductResponse toResponse(Product product) {
        String categoryName = null;
        if (product.getCategoryId() != null) {
            categoryName = categoryRepository.findById(product.getCategoryId())
                    .map(Category::getName)
                    .orElse(null);
        }
        return ProductResponse.from(product, categoryName);
    }

    private Map<Long, String> categoryNames(Long organisationId) {
        Map<Long, String> names = new HashMap<>();
        for (Category category : categoryRepository.findByOrganisationIdOrderByNameAsc(organisationId)) {
            names.put(category.getCategoryId(), category.getName());
        }
        return names;
    }

    private Long validateCategory(Long organisationId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND", "Category not found: " + categoryId));
        if (!Objects.equals(category.getOrganisationId(), organisationId)) {
            throw ApiException.unprocessable("CATEGORY_ORG_MISMATCH", "Category belongs to another organisation");
        }
        return categoryId;
    }

    private static void requireOrgAccess(List<Long> callerOrgIds, Long organisationId) {
        if (callerOrgIds == null || !callerOrgIds.contains(organisationId)) {
            throw ApiException.forbidden("No access to organisation " + organisationId);
        }
    }
}
