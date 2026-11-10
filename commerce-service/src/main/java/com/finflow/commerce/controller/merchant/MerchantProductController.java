package com.finflow.commerce.controller.merchant;

import com.finflow.commerce.dto.catalogue.ProductRequest;
import com.finflow.commerce.dto.catalogue.ProductResponse;
import com.finflow.commerce.dto.catalogue.ProductUpdateRequest;
import com.finflow.commerce.service.catalogue.CatalogueService;
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

/** Merchant catalogue management (writes). */
@RestController
@RequestMapping("/api/v1/merchant/products")
public class MerchantProductController {

    private final CatalogueService catalogueService;

    public MerchantProductController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogueService.createProduct(user.orgIds(), request);
    }

    @PatchMapping("/{productId}")
    public ProductResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return catalogueService.updateProduct(productId, user.orgIds(), request);
    }
}
