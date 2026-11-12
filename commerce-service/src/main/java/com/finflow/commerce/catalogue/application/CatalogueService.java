package com.finflow.commerce.catalogue.application;

import com.finflow.commerce.catalogue.dto.CategoryRequest;
import com.finflow.commerce.catalogue.dto.CategoryResponse;
import com.finflow.commerce.catalogue.dto.ProductRequest;
import com.finflow.commerce.catalogue.dto.ProductResponse;
import com.finflow.commerce.catalogue.dto.ProductUpdateRequest;
import com.finflow.commerce.catalogue.domain.Category;
import com.finflow.commerce.catalogue.domain.Product;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.seller.MerchantSellerAccess;
import com.finflow.commerce.catalogue.persistence.CategoryRepository;
import com.finflow.commerce.catalogue.persistence.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import com.finflow.commerce.catalogue.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles product catalogue and category use cases.
 *
 * Organisation access is checked before catalogue data is accessed.
 *
 * APPLICATION SERVICE - ORCHESTRATES CATALOGUE USE CASES
 *
 * This class is the application-layer entry point for catalogue operations.
 * Controllers should handle HTTP concerns (routes, request binding and status codes); repositories should handle database access; this service coordinates the business workflow between them.
 *
 * CURRENT RESPONSIBILITIES:
 * 1. Merchant category creation and listing.
 * 2. Merchant product creation, listing, updating and searching.
 * 3. Public marketplace browsing of active products and categories.
 * 4. Organisation ownership checks before merchant data is accessed.
 *
 * END-TO-END EXAMPLE - CREATE PRODUCT:
 * Controller receives JSON -> request DTO validation -> this service checks organisation access -> validates the optional category -> creates
 * Product -> repository persists it -> mapper builds the response DTO -> controller serialises the response as JSON.
 *
 * DESIGN NOTES:
 * - @Service makes this class a Spring-managed application component. Constructor injection makes required dependencies explicit and testable.
 * - @Transactional defines the database unit of work around write operations.
 * - Read methods use readOnly=true as a hint that no entity changes are expected.
 * - This service currently groups product and category use cases for a small catalogue module. We can split ProductService and CategoryService later
 *   if their responsibilities or change rates justify the extra classes.
 *
 * SECURITY BOUNDARY:
 * Merchant methods receive the caller's organisation IDs and verify ownership.
 * Marketplace methods are intentionally public and therefore only return ACTIVE products. Do not remove these checks when adding new endpoints.
 *
 * Catalogue controllers call this application service; ProductRepository, CategoryRepository and ProductMapper are its constructor-injected dependencies.
 * Product and category operations remain together because product creation/search already validate categories, and category management has only two small use cases.
 * Splitting them now would add coordination without an independent responsibility.
 *
 * @Transactional uses REQUIRED propagation: it joins a caller's transaction or opens one. readOnly is an optimization hint, not authorization or a write ban.
 * Without the annotation a multi-repository operation loses its shared unit of work;
 * TransactionTemplate is an alternative where transaction boundaries must be explicit.
 * @Cacheable avoids repeated category queries;
 * @CacheEvict invalidates after creation.
 * Removing them affects freshness/performance, not database constraints. A cache hit skips the body, so the cache condition also checks the authenticated membership.
 * JWT memberships can become stale; this is not a live membership lookup.
 *
 * Marketplace reads cross seller boundaries intentionally but return ACTIVE products only.
 * These are customer-facing, authenticated HTTP APIs under SecurityConfig;
 * "public marketplace" here means shared catalogue visibility, not anonymous access.
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
    public CatalogueService(ProductRepository productRepository, CategoryRepository categoryRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    /**
     * Creates a category for an organisation.
     * Evicts the cached category list for this organisation after the method completes successfully.
     */

    @Transactional
    @CacheEvict(value = "categories", key = "#result.sellerId()")
    public CategoryResponse createCategory(List<Long> callerOrgIds, CategoryRequest request) {

        // Merchant seller is taken from JWT orgIds, not from CategoryRequest (marketplace API rule).
        Long sellerId = MerchantSellerAccess.requireSellerId(callerOrgIds);
        String slug = request.slug().trim().toLowerCase();

        if (categoryRepository.existsByOrganisationIdAndSlugIgnoreCase(sellerId, slug)) {
            throw ApiException.conflict(
                    "CATEGORY_EXISTS",
                    "Category slug already exists"
            );
        }

        Category category = new Category(sellerId, request.name(), slug);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    /**
     * Returns categories for an organisation.
     * The result is cached using the organisation ID as the cache key.
     */
    // readOnly=true documents that this use case only reads data and may let the persistence provider/database apply read optimisations.
    @Transactional(readOnly = true)
    @Cacheable(  // Return the cached category list when the organisation key is present; the method executes and populates the cache on a cache miss.
            value = "categories",
            key = "#organisationId",
            // Cache advice can skip the method body on hits. Check membership before any cache lookup; unauthorized calls execute the body and fail below.
            condition = "#callerOrgIds != null && #callerOrgIds.contains(#organisationId)"
    )
    public List<CategoryResponse> listCategories(Long organisationId, List<Long> callerOrgIds) {
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
    public ProductResponse createProduct(List<Long> callerOrgIds, ProductRequest request) {
        // Same seller resolution as categories — body cannot name a different shop.
        Long sellerId = MerchantSellerAccess.requireSellerId(callerOrgIds);
        Long categoryId = validateCategory(sellerId, request.categoryId());

        Product product = new Product(
                sellerId,
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
    // readOnly=true documents that this use case only reads data and may let the persistence provider/database apply read optimisations.
    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(Long organisationId, List<Long> callerOrgIds) {
        requireOrgAccess(callerOrgIds, organisationId);

        // Fetch category names once rather than looking up a category separately for every product.
        Map<Long, String> names = categoryNames(organisationId);

        // Fetch products and map each one using its category name.
        return productRepository
                .findByOrganisationIdOrderByProductIdAsc(organisationId)
                .stream()
                .map(p -> productMapper.toResponse(p, names.get(p.getCategoryId())))
                .toList();
    }

    /**
     * Updates the supplied product fields.
     * Null request fields mean that the corresponding field is unchanged.
     */
    // A write transaction groups the database changes in this use case; if an unchecked exception escapes, Spring rolls the transaction back.
    @Transactional
    public ProductResponse updateProduct(Long productId, List<Long> callerOrgIds, ProductUpdateRequest request) {
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
            product.updateAvailableQuantity(request.availableQuantity());
        }

        // Validate and assign the new category only when supplied.
        if (request.categoryId() != null) {
            product.assignCategory(validateCategory(product.getOrganisationId(), request.categoryId()));
        }

        return toResponse(productRepository.save(product));
    }

    /**
     * Marketplace browse: all active products across sellers (no organisation on the request).
     */
    // readOnly=true documents that this use case only reads data and may let the persistence provider/database apply read optimisations.
    @Transactional(readOnly = true)
    public List<CategoryResponse> listMarketplaceCategories() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    // readOnly=true documents that this use case only reads data and may let the persistence provider/database apply read optimisations.
    @Transactional(readOnly = true)
    public ProductResponse getMarketplaceProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound(
                        "PRODUCT_NOT_FOUND",
                        "Product not found: " + productId
                ));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw ApiException.notFound(
                    "PRODUCT_NOT_FOUND",
                    "Product not found: " + productId
            );
        }
        return toResponse(product);
    }

    // readOnly=true documents that this use case only reads data and may let the persistence provider/database apply read optimisations.
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchMarketplaceProducts(String q, Long categoryId, Pageable pageable) {
        return productRepository.findAll(marketplaceProductSpec(q, categoryId), pageable).map(this::toResponse);
    }

    /**
     * Builds dynamic marketplace filters. Unlike merchant search, this deliberately has no organisation predicate because the marketplace spans sellers;
     * it must always restrict results to ACTIVE products.
     */
    private Specification<Product> marketplaceProductSpec(String q, Long categoryId) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw ApiException.notFound("CATEGORY_NOT_FOUND", "Category not found: " + categoryId);
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (q != null && !q.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + q.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Searches products using optional name and category filters.
     * Pageable controls pagination and sorting.
     */
    // readOnly=true documents that this use case only reads data and may let the persistence provider/database apply read optimisations.
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(Long organisationId, List<Long> callerOrgIds, String q, Long categoryId, Pageable pageable) {
        requireOrgAccess(callerOrgIds, organisationId);

        // If a category filter is supplied, make sure that category belongs to the requested organisation.
        if (categoryId != null) {
            validateCategory(organisationId, categoryId);
        }

        // Specification is Spring Data JPA's composable Criteria API wrapper.
        // It is useful when filters are optional; a fixed derived query would require many method combinations as the number of filters grows.
        // Specification allows us to construct database conditions dynamically based on the supplied search filters.
        Specification<Product> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Every search must be restricted to this organisation.
            predicates.add(cb.equal(root.get("organisationId"), organisationId));

            // Add the category condition only when a category was supplied.
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }

            // Add a case-insensitive partial name search when q is present.
            if (q != null && !q.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + q.trim().toLowerCase() + "%"));
            }

            // Combine all conditions using AND.
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        // Load category names for response mapping.
        Map<Long, String> names = categoryNames(organisationId);

        // findAll(spec, pageable) applies both the dynamic filters and the requested pagination/sorting.
        return productRepository.findAll(spec, pageable)
                .map(p -> productMapper.toResponse(p, names.get(p.getCategoryId())));
    }

    /**
     * Converts one Product entity into a ProductResponse.
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
     * Map key   = category ID
     * Map value = category name
     */
    private Map<Long, String> categoryNames(Long organisationId) {

        Map<Long, String> names = new HashMap<>();

        for (Category category : categoryRepository.findByOrganisationIdOrderByNameAsc(organisationId)) {
            names.put(category.getCategoryId(), category.getName());
        }

        return names;
    }

    /**
     * Validates that a category exists and belongs to the organisation.
     *
     * A null category ID means the product has no category.
     */
    private Long validateCategory(Long organisationId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound(
                        "CATEGORY_NOT_FOUND",
                        "Category not found: " + categoryId
                ));

        // A category belonging to another organisation cannot be assigned.
        if (!Objects.equals(category.getOrganisationId(), organisationId)) {
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
    /**
     * Enforces tenant/organisation isolation for merchant operations.
     * A caller may belong to multiple organisations, so membership is checked against the authenticated principal's allowed ID list.
     * Keep this check in the application layer even if the controller already receives an authenticated principal.
     */
    private static void requireOrgAccess(List<Long> callerOrgIds, Long organisationId) {
        if (callerOrgIds == null || !callerOrgIds.contains(organisationId)) {
            throw ApiException.forbidden("No access to organisation " + organisationId);
        }
    }
}
