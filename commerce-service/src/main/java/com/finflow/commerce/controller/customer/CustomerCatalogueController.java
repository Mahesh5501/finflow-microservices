package com.finflow.commerce.controller.customer;

import com.finflow.commerce.dto.catalogue.CategoryResponse;
import com.finflow.commerce.dto.catalogue.ProductResponse;
import com.finflow.commerce.service.catalogue.CatalogueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerCatalogueController {

    private final CatalogueService catalogueService;

    public CustomerCatalogueController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return catalogueService.listMarketplaceCategories();
    }

    @GetMapping("/products/search")
    public Page<ProductResponse> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return catalogueService.searchMarketplaceProducts(q, categoryId, pageable);
    }

    @GetMapping("/products/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId) {
        return catalogueService.getMarketplaceProduct(productId);
    }
}
