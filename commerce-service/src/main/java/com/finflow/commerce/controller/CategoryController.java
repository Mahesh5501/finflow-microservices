
package com.finflow.commerce.controller;

import com.finflow.commerce.dto.CategoryRequest;
import com.finflow.commerce.dto.CategoryResponse;
import com.finflow.commerce.security.AuthenticatedUser;
import com.finflow.commerce.service.CatalogueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for product-category operations.
 * Category creation and retrieval are delegated to CatalogueService.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CatalogueService catalogueService; // CatalogueService contains category and product business operations.

    /**
     * Constructor injection.
     * Spring injects the CatalogueService bean when creating this controller.
     */
    public CategoryController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService; // Inject the CatalogueService bean when creating this controller.
    }

    /**
     * POST /api/v1/categories
     * Creates a category for an organisation.
     * @Valid checks CategoryRequest constraints before service execution.
     * CREATED returns HTTP 201 when creation succeeds.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return catalogueService.createCategory(user.orgIds(), request); // Delegate category creation to the service.
    }

    /**
     * GET /api/v1/categories?organisationId=10
     * Retrieves categories belonging to the requested organisation.
     */
    @GetMapping
    public List<CategoryResponse> list(@RequestParam @NotNull Long organisationId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return catalogueService.listCategories(organisationId, user.orgIds()); // Delegate category listing to the service.
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
