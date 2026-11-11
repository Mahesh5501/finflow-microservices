package com.finflow.commerce.catalogue.application;

import com.finflow.commerce.catalogue.dto.CategoryRequest;
import com.finflow.commerce.catalogue.dto.CategoryResponse;
import com.finflow.commerce.catalogue.dto.ProductRequest;
import com.finflow.commerce.catalogue.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.catalogue.dto.ProductUpdateRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class CatalogueServiceTest {

    private static final List<Long> ORGS = List.of(10L);

    @Autowired
    private CatalogueService catalogueService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void cachedCategoriesStillRequireMembershipAndCreationEvicts() {
        long seller = 610L;
        List<Long> allowed = List.of(seller);
        catalogueService.createCategory(allowed, new CategoryRequest(seller, "Cached A", "cached-a"));
        var first = catalogueService.listCategories(seller, allowed);
        var cache = cacheManager.getCache("categories");
        assertNotNull(cache);
        assertNotNull(cache.get(seller));
        assertThrows(ApiException.class, () -> catalogueService.listCategories(seller, List.of(611L)));
        assertThrows(ApiException.class, () -> catalogueService.listCategories(seller, null));
        var serializer = new JdkSerializationRedisSerializer();
        assertEquals(first, serializer.deserialize(serializer.serialize(first)));
        catalogueService.createCategory(allowed, new CategoryRequest(seller, "Cached B", "cached-b"));
        assertNull(cache.get(seller));
        assertEquals(2, catalogueService.listCategories(seller, allowed).size());
    }

    @Test
    void productUpdatesPreserveMerchantAndCategoryIsolation() {
        var category = catalogueService.createCategory(List.of(620L),
                new CategoryRequest(620L, "Own", "own"));
        var foreign = catalogueService.createCategory(List.of(621L),
                new CategoryRequest(621L, "Foreign", "foreign"));
        var product = catalogueService.createProduct(List.of(620L),
                new ProductRequest(620L, "Scoped", new BigDecimal("12.00"), 4, category.categoryId()));
        assertThrows(ApiException.class, () -> catalogueService.updateProduct(product.productId(), List.of(621L),
                new ProductUpdateRequest(new BigDecimal("13.00"), null, null)));
        assertEquals("CATEGORY_ORG_MISMATCH", assertThrows(ApiException.class,
                () -> catalogueService.updateProduct(product.productId(), List.of(620L),
                        new ProductUpdateRequest(null, null, foreign.categoryId()))).getCode());
        var updated = catalogueService.updateProduct(product.productId(), List.of(620L),
                new ProductUpdateRequest(new BigDecimal("13.00"), 7, null));
        assertEquals(category.categoryId(), updated.categoryId());
        assertEquals("Own", updated.categoryName());
        assertEquals(7, updated.availableQuantity());
    }

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
