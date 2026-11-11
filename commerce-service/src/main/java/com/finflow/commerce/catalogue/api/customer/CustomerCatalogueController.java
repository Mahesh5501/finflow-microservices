package com.finflow.commerce.catalogue.api.customer;

import com.finflow.commerce.catalogue.dto.CategoryResponse;
import com.finflow.commerce.catalogue.dto.ProductResponse;
import com.finflow.commerce.catalogue.application.CatalogueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CUSTOMER-FACING CATALOGUE HTTP API.
 *
 * <p>Purpose: expose read-only marketplace catalogue operations to customers.
 * This class is the HTTP boundary: it accepts URL/query parameters, delegates business decisions to CatalogueService, and returns response DTOs. It should
 * not contain repository calls, pricing rules, tenant filtering, or entity manipulation.</p>
 *
 * <p>Why this package: the controller belongs to the Catalogue feature and to the customer API audience.
 * Keeping it here makes feature ownership obvious and prevents a global controller package from becoming a dumping ground.</p>
 *
 * <p>Spring annotations:
 * @RestController registers this class as a REST controller and serializes returned objects (normally as JSON).
 * @RequestMapping establishes the common URL prefix.
 * @GetMapping maps safe read operations.
 * @RequestParam binds optional search filters,
 * @PathVariable binds a resource identifier from the URL, and
 * @PageableDefault supplies paging defaults.
 * Without these mappings Spring would not expose these handler methods.</p>
 *
 * <p>Design note: returning DTOs rather than JPA entities protects the API from persistence details and lazy-loading problems.
 * Pagination prevents an unbounded product search from loading the entire catalogue into memory.</p>
 *
 * Constructor injection supplies CatalogueService as the only business dependency.
 * The global SecurityFilterChain still protects these APIs; "customer-facing" does not mean anonymous.
 * DTO validation and domain errors retain their existing HTTP mapping.
 * Transaction boundaries belong in the application service, not the handler.
 */
@RestController
@RequestMapping("/api/v1/customer")

public class CustomerCatalogueController {

    private final CatalogueService catalogueService;

    public CustomerCatalogueController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    /**
     * Customer GET lists the shared category directory through CatalogueService.
     * The service reads categories in name order and maps DTOs inside its read transaction.
     */
    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return catalogueService.listMarketplaceCategories();
    }

    /**
     * Customer GET passes optional text/category filters and pagination to the service.
     * The service enforces ACTIVE visibility; an unknown category returns CATEGORY_NOT_FOUND.
     */
    @GetMapping("/products/search")
    public Page<ProductResponse> searchProducts(@RequestParam(required = false) String q, @RequestParam(required = false) Long categoryId, @PageableDefault(size = 20) Pageable pageable) {
        return catalogueService.searchMarketplaceProducts(q, categoryId, pageable);
    }

    /**
     * Customer GET loads one marketplace product through the service. Missing or
     * inactive products both return PRODUCT_NOT_FOUND, keeping inactive rows off this API.
     */
    @GetMapping("/products/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId) {
        return catalogueService.getMarketplaceProduct(productId);
    }
}
