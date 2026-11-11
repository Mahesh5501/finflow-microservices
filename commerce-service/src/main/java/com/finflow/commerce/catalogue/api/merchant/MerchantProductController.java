package com.finflow.commerce.catalogue.api.merchant;

import com.finflow.commerce.catalogue.dto.ProductRequest;
import com.finflow.commerce.catalogue.dto.ProductResponse;
import com.finflow.commerce.catalogue.dto.ProductUpdateRequest;
import com.finflow.commerce.catalogue.application.CatalogueService;
import com.finflow.commerce.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Merchant catalogue management (writes).
 *
 * MERCHANT-FACING PRODUCT MANAGEMENT API.
 *
 * <p>Purpose: expose product creation and update endpoints for authenticated
 * merchants. This is an inbound HTTP adapter: it translates HTTP input into
 * application-service calls and returns ProductResponse DTOs.</p>
 *
 * <p>@RestController registers the class and enables JSON request/response
 * handling. @RequestMapping supplies the common resource path. @PostMapping
 * creates a product; @PatchMapping applies a partial update to an existing
 * product. @PathVariable obtains the product ID from the URL, @RequestBody
 * deserializes JSON, and @Valid runs bean validation on the DTO. @ResponseStatus
 * makes successful creation return HTTP 201 rather than the default 200.
 * @AuthenticationPrincipal supplies the authenticated merchant context.</p>
 *
 * <p>Tenant isolation: the authenticated organisation IDs are passed to the
 * service for every write. The service must verify that the target category or
 * product belongs to an organisation the caller can manage. Hiding this check
 * only in the controller would leave other service callers unprotected.</p>
 *
 * <p>Why DTOs: ProductRequest and ProductUpdateRequest define the public input
 * contract; ProductResponse defines the output contract. Do not bind request
 * JSON directly to a JPA Product entity because clients could then modify
 * persistence-only fields such as IDs, version, or ownership.</p>
 *
 * Constructor injection supplies CatalogueService as the only business dependency.
 * The global SecurityFilterChain still protects these APIs; "customer-facing" does
 * not mean anonymous. DTO validation and domain errors retain their existing HTTP
 * mapping. Transaction boundaries belong in the application service, not the handler.
 */
@RestController
@RequestMapping("/api/v1/merchant/products")

public class MerchantProductController {

    private final CatalogueService catalogueService;

    public MerchantProductController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    /**
     * Merchant POST delegates validated input and trusted memberships. CatalogueService
     * checks the owning organisation and optional category before persisting/mapping;
     * request-body organisationId alone never grants permission to create for that seller.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogueService.createProduct(user.orgIds(), request);
    }

    /**
     * Merchant PATCH delegates only supplied fields. The service checks stored ownership
     * and category seller consistency; nulls keep existing values. Missing product, foreign
     * seller/category and optimistic stock conflicts retain the existing error handling.
     */
    @PatchMapping("/{productId}")
    public ProductResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return catalogueService.updateProduct(productId, user.orgIds(), request);
    }
}
