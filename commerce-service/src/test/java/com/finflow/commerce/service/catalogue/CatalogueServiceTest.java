package com.finflow.commerce.service.catalogue;

import com.finflow.commerce.dto.catalogue.CategoryRequest;
import com.finflow.commerce.dto.catalogue.CategoryResponse;
import com.finflow.commerce.dto.catalogue.ProductRequest;
import com.finflow.commerce.dto.catalogue.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CatalogueServiceTest {

    private static final List<Long> ORGS = List.of(10L);

    @Autowired
    private CatalogueService catalogueService;

    @Test
    void category_and_search_by_name_and_category() {
        CategoryResponse category = catalogueService.createCategory(
                ORGS, new CategoryRequest(10L, "Apparel", "apparel")
        );

        catalogueService.createProduct(
                ORGS, new ProductRequest(10L, "Blue Tee", new BigDecimal("20.00"), 10, category.categoryId())
        );
        catalogueService.createProduct(
                ORGS, new ProductRequest(10L, "Red Mug", new BigDecimal("12.00"), 10, null)
        );

        Page<ProductResponse> byName = catalogueService.searchProducts(
                10L, ORGS, "tee", null, PageRequest.of(0, 10)
        );
        assertEquals(1, byName.getTotalElements());
        assertEquals("Blue Tee", byName.getContent().getFirst().name());
        assertEquals("Apparel", byName.getContent().getFirst().categoryName());

        Page<ProductResponse> byCategory = catalogueService.searchProducts(
                10L, ORGS, null, category.categoryId(), PageRequest.of(0, 10)
        );
        assertEquals(1, byCategory.getTotalElements());
        assertTrue(byCategory.getContent().getFirst().name().contains("Tee"));
    }
}
