package com.finflow.commerce.service;

import com.finflow.commerce.dto.CategoryRequest;
import com.finflow.commerce.dto.CategoryResponse;
import com.finflow.commerce.dto.ProductRequest;
import com.finflow.commerce.dto.ProductResponse;
import com.finflow.commerce.dto.ProductUpdateRequest;
import com.finflow.commerce.entity.Category;
import com.finflow.commerce.entity.Product;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.mapper.ProductMapper;
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

/**
 * Handles product catalogue and category use cases.
 *
 * Organisation access is checked before catalogue data is accessed.
 */
@Service
public class CatalogueService {

    // Reads and writes product data.
    private final ProductRepository productRepository;

    // Reads and writes category data.
    private final CategoryRepository categoryRepository;

    // Converts Product entities into API response DTOs.
    private final ProductMapper productMapper;

    // Constructor injection keeps dependencies explicit.
    public CatalogueService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductMapper productMapper
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    /**
     * Creates a category for an organisation.
     *
     * Evicts the cached category list for this organisation after the
     * method completes successfully.
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(
            value = "categories",
            key = "#request.organisationId()"
    )
    public CategoryResponse createCategory(
            List<Long> callerOrgIds,
            CategoryRequest request
    ) {
        // Ensure the caller belongs to the target organisation.
        requireOrgAccess(callerOrgIds, request.organisationId());

        // Normalize the slug so that casing and surrounding spaces
        // do not create logically duplicate category identifiers.
        String slug = request.slug()
                .trim()
                .toLowerCase();

        // Check whether the organisation already has this slug.
        if (categoryRepository.existsByOrganisationIdAndSlugIgnoreCase(
                request.organisationId(),
                slug
        )) {
            throw ApiException.conflict(
                    "CATEGORY_EXISTS",
                    "Category slug already exists"
            );
        }

        // Create the category entity.
        Category category = new Category(
                request.organisationId(),
                request.name(),
                slug
        );

        // Save the category and convert it to the API response.
        return CategoryResponse.from(
                categoryRepository.save(category)
        );
    }

    /**
     * Returns categories for an organisation.
     *
     * The result is cached using the organisation ID as the cache key.
     */
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(
            value = "categories",
            key = "#organisationId"
    )
    public List<CategoryResponse> listCategories(
            Long organisationId,
            List<Long> callerOrgIds
    ) {
        requireOrgAccess(callerOrgIds, organisationId);

        // Fetch categories in name order and convert them to DTOs.
        return categoryRepository
                .findByOrganisationIdOrderByNameAsc(organisationId)
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * Creates a product and optionally associates it with a category.
     */
    @Transactional
    public ProductResponse createProduct(
            List<Long> callerOrgIds,
            ProductRequest request
    ) {
        requireOrgAccess(callerOrgIds, request.organisationId());

        // Verify that the category exists and belongs to this organisation.
        // A null category ID is allowed by validateCategory().
        Long categoryId = validateCategory(
                request.organisationId(),
                request.categoryId()
        );

        // Create the product entity using validated request data.
        Product product = new Product(
                request.organisationId(),
                request.name().trim(),
                request.unitPrice(),
                request.availableQuantity(),
                categoryId
        );

        return toResponse(productRepository.save(product));
    }

    /**
     * Lists all products for an organisation.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(
            Long organisationId,
            List<Long> callerOrgIds
    ) {
        requireOrgAccess(callerOrgIds, organisationId);

        // Fetch category names once rather than looking up a category
        // separately for every product.
        Map<Long, String> names = categoryNames(organisationId);

        // Fetch products and map each one using its category name.
        return productRepository
                .findByOrganisationIdOrderByProductIdAsc(organisationId)
                .stream()
                .map(p -> productMapper.toResponse(
                        p,
                        names.get(p.getCategoryId())
                ))
                .toList();
    }

    /**
     * Updates the supplied product fields.
     *
     * Null request fields mean that the corresponding field is unchanged.
     */
    @Transactional
    public ProductResponse updateProduct(
            Long productId,
            List<Long> callerOrgIds,
            ProductUpdateRequest request
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound(
                        "PRODUCT_NOT_FOUND",
                        "Product not found: " + productId
                ));

        // Check access against the organisation that owns the product.
        requireOrgAccess(callerOrgIds, product.getOrganisationId());

        // Update price only when a new price was supplied.
        if (request.unitPrice() != null) {
            product.updateUnitPrice(request.unitPrice());
        }

        // Update available quantity only when a value was supplied.
        if (request.availableQuantity() != null) {
            product.updateAvailableQuantity(
                    request.availableQuantity()
            );
        }

        // Validate and assign the new category only when supplied.
        if (request.categoryId() != null) {
            product.assignCategory(
                    validateCategory(
                            product.getOrganisationId(),
                            request.categoryId()
                    )
            );
        }

        return toResponse(productRepository.save(product));
    }

    /**
     * Searches products using optional name and category filters.
     *
     * Pageable controls pagination and sorting.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(
            Long organisationId,
            List<Long> callerOrgIds,
            String q,
            Long categoryId,
            Pageable pageable
    ) {
        requireOrgAccess(callerOrgIds, organisationId);

        // If a category filter is supplied, make sure that category
        // belongs to the requested organisation.
        if (categoryId != null) {
            validateCategory(organisationId, categoryId);
        }

        // Specification allows us to construct database conditions
        // dynamically based on the supplied search filters.
        Specification<Product> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Every search must be restricted to this organisation.
            predicates.add(
                    cb.equal(
                            root.get("organisationId"),
                            organisationId
                    )
            );

            // Add the category condition only when a category was supplied.
            if (categoryId != null) {
                predicates.add(
                        cb.equal(
                                root.get("categoryId"),
                                categoryId
                        )
                );
            }

            // Add a case-insensitive partial name search when q is present.
            if (q != null && !q.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + q.trim().toLowerCase() + "%"
                        )
                );
            }

            // Combine all conditions using AND.
            return cb.and(
                    predicates.toArray(Predicate[]::new)
            );
        };

        // Load category names for response mapping.
        Map<Long, String> names = categoryNames(organisationId);

        // findAll(spec, pageable) applies both the dynamic filters
        // and the requested pagination/sorting.
        return productRepository.findAll(spec, pageable)
                .map(p -> productMapper.toResponse(
                        p,
                        names.get(p.getCategoryId())
                ));
    }

    /**
     * Converts one Product entity into a ProductResponse.
     *
     * This helper loads the category name when the product has a category.
     */
    private ProductResponse toResponse(Product product) {

        String categoryName = null;

        if (product.getCategoryId() != null) {
            categoryName = categoryRepository
                    .findById(product.getCategoryId())
                    .map(Category::getName)
                    .orElse(null);
        }

        return productMapper.toResponse(product, categoryName);
    }

    /**
     * Loads category names for an organisation into a map.
     *
     * Map key   = category ID
     * Map value = category name
     */
    private Map<Long, String> categoryNames(Long organisationId) {

        Map<Long, String> names = new HashMap<>();

        for (Category category :
                categoryRepository.findByOrganisationIdOrderByNameAsc(
                        organisationId
                )) {

            names.put(
                    category.getCategoryId(),
                    category.getName()
            );
        }

        return names;
    }

    /**
     * Validates that a category exists and belongs to the organisation.
     *
     * A null category ID means the product has no category.
     */
    private Long validateCategory(
            Long organisationId,
            Long categoryId
    ) {
        if (categoryId == null) {
            return null;
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound(
                        "CATEGORY_NOT_FOUND",
                        "Category not found: " + categoryId
                ));

        // A category belonging to another organisation cannot be assigned.
        if (!Objects.equals(
                category.getOrganisationId(),
                organisationId
        )) {
            throw ApiException.unprocessable(
                    "CATEGORY_ORG_MISMATCH",
                    "Category belongs to another organisation"
            );
        }

        return categoryId;
    }

    /**
     * Shared organisation access validation.
     */
    private static void requireOrgAccess(
            List<Long> callerOrgIds,
            Long organisationId
    ) {
        if (callerOrgIds == null
                || !callerOrgIds.contains(organisationId)) {
            throw ApiException.forbidden(
                    "No access to organisation " + organisationId
            );
        }
    }
}