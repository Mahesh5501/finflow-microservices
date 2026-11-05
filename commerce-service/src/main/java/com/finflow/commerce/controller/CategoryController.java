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

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CatalogueService catalogueService;

    public CategoryController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request, Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return catalogueService.createCategory(user.orgIds(), request);
    }

    @GetMapping
    public List<CategoryResponse> list(@RequestParam @NotNull Long organisationId, Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return catalogueService.listCategories(organisationId, user.orgIds());
    }
}
