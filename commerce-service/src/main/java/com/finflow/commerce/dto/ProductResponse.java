package com.finflow.commerce.dto;

import com.finflow.commerce.entity.Product;
import com.finflow.commerce.entity.ProductStatus;

import java.math.BigDecimal;

public record ProductResponse(
        Long productId,
        Long organisationId,
        Long categoryId,
        String categoryName,
        String name,
        BigDecimal unitPrice,
        int availableQuantity,
        ProductStatus status
) {
    public static ProductResponse from(Product p) {
        return from(p, null);
    }

    public static ProductResponse from(Product p, String categoryName) {
        return new ProductResponse(
                p.getProductId(),
                p.getOrganisationId(),
                p.getCategoryId(),
                categoryName,
                p.getName(),
                p.getUnitPrice(),
                p.getAvailableQuantity(),
                p.getStatus()
        );
    }
}
