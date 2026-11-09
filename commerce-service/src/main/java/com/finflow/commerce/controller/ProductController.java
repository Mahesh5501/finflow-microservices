
package com.finflow.commerce.controller;

import com.finflow.commerce.dto.ProductRequest;
import com.finflow.commerce.dto.ProductResponse;
import com.finflow.commerce.dto.ProductUpdateRequest;
import com.finflow.commerce.security.AuthenticatedUser;
import com.finflow.commerce.service.CatalogueService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import jakarta.validation.constraints.NotNull;

/**
 * REST controller responsible for product catalogue APIs.
 * All product-related business operations are delegated to CatalogueService.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final CatalogueService catalogueService; // CatalogueService contains product creation and management logic.

    /**
     * Constructor injection.
     * Spring injects the CatalogueService bean when creating this controller.
     */
    public ProductController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService; // Inject the CatalogueService bean when creating this controller.
    }

    /**
     * POST /api/v1/products
     * Creates a new product.
     * Request validation happens before the service is called.
     * HTTP 201 indicates successful creation.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return catalogueService.createProduct(user.orgIds(), request); // Delegate product creation to the service.
    }

    /**
     * GET /api/v1/products?organisationId=10
     * Retrieves products belonging to an organisation.
     * Returns a List because this endpoint returns the full product list rather than a paginated search result.
     */
    @GetMapping
    public List<ProductResponse> list(@RequestParam @NotNull Long organisationId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return catalogueService.listProducts(organisationId, user.orgIds()); // Delegate product listing to the service.
    }

    /**
     * GET /api/v1/products/search
     * Searches products with optional filters and pagination.
     * q: Optional text search.
     * categoryId: Optional category filter.
     * Pageable: Contains page number, page size and sorting information.
     * @PageableDefault(size = 20): Uses 20 records per page when the client does not specify a page size.
     * Page<ProductResponse> includes the current page content and pagination metadata such as total elements and pages.
     */
    @GetMapping("/search")
    public Page<ProductResponse> search(@RequestParam @NotNull Long organisationId, @RequestParam(required = false) String q, @RequestParam(required = false) Long categoryId, @PageableDefault(size = 20) Pageable pageable, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return catalogueService.searchProducts(organisationId, user.orgIds(), q, categoryId, pageable); // Delegate product search to the service.
    }

    /**
     * PATCH /api/v1/products/{productId}
     * Partially updates an existing product.
     * PATCH is appropriate because ProductUpdateRequest allows individual fields to be omitted.
     */
    @PatchMapping("/{productId}")
    public ProductResponse update(@PathVariable Long productId, @Valid @RequestBody ProductUpdateRequest request, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return catalogueService.updateProduct(productId, user.orgIds(), request); // Delegate product update to the service.
    }

    /**
     * Extracts our custom AuthenticatedUser from Spring Security.
     * The JWT authentication flow places AuthenticatedUser inside Authentication.getPrincipal().
     * Keeping this extraction in one helper avoids repeating the same cast in every endpoint.
     */
    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
