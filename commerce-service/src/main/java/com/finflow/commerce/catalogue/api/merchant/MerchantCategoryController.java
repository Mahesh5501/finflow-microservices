package com.finflow.commerce.catalogue.api.merchant;

import com.finflow.commerce.catalogue.dto.CategoryRequest;
import com.finflow.commerce.catalogue.dto.CategoryResponse;
import com.finflow.commerce.catalogue.application.CatalogueService;
import com.finflow.commerce.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * MERCHANT-FACING CATEGORY MANAGEMENT API.
 *
 * <p>
 *     Purpose: accept merchant category creation requests and delegate the operation to the Catalogue application service.
 *     The controller performs HTTP-level concerns only;
 *     validation rules and persistence remain outside.
 * </p>
 *
 * <p>
     * @RestController makes this a Spring MVC REST endpoint.
     * @RequestMapping defines the merchant category URL prefix.
     * @PostMapping handles resource creation.
     * @ResponseStatus(CREATED) returns HTTP 201 on success.
     * @Valid triggers Jakarta Bean Validation on the request DTO before the method executes;
     * @RequestBody deserializes the JSON body.
     * @AuthenticationPrincipal injects the authenticated caller supplied by Spring Security.
 * </p>
 *
 * Security boundary: user.orgIds() is passed to the application service so category ownership is scoped to the authenticated merchant organisation.
 * Never trust an organisation ID supplied by the request body for this purpose. The service must still enforce the ownership/business rules.
 *
 * Constructor injection supplies CatalogueService as the only business dependency.
 * The global SecurityFilterChain still protects these APIs;
 * "customer-facing" does not mean anonymous. DTO validation and domain errors retain their existing HTTP mapping.
 * Transaction boundaries belong in the application service, not the handler.
 */
@RestController
@RequestMapping("/api/v1/merchant/categories")

public class MerchantCategoryController {

    private final CatalogueService catalogueService;

    public MerchantCategoryController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    /**
     * Merchant POST validates JSON, supplies trusted orgIds and returns the created DTO.
     * CatalogueService checks membership and duplicate slug, then saves/evicts its cache in a write use case.
     * Forbidden membership and CATEGORY_EXISTS retain their errors.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogueService.createCategory(user.orgIds(), request);
    }
}
