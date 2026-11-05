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

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final CatalogueService catalogueService;

    public ProductController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request, Authentication authentication) {
        AuthenticatedUser user = caller(authentication);
        return catalogueService.createProduct(user.orgIds(), request);
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam Long organisationId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication);
        return catalogueService.listProducts(organisationId, user.orgIds());
    }

    @GetMapping("/search")
    public Page<ProductResponse> search(
            @RequestParam Long organisationId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        AuthenticatedUser user = caller(authentication);
        return catalogueService.searchProducts(organisationId, user.orgIds(), q, categoryId, pageable);
    }

    @PatchMapping("/{productId}")
    public ProductResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser user = caller(authentication);
        return catalogueService.updateProduct(productId, user.orgIds(), request);
    }

    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
