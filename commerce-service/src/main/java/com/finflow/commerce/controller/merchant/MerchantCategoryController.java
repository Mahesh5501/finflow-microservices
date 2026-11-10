package com.finflow.commerce.controller.merchant;

import com.finflow.commerce.dto.catalogue.CategoryRequest;
import com.finflow.commerce.dto.catalogue.CategoryResponse;
import com.finflow.commerce.service.catalogue.CatalogueService;
import com.finflow.commerce.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/categories")
public class MerchantCategoryController {

    private final CatalogueService catalogueService;

    public MerchantCategoryController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalogueService.createCategory(user.orgIds(), request);
    }
}
