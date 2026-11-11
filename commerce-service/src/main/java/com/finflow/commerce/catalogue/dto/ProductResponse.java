package com.finflow.commerce.catalogue.dto;

import com.finflow.commerce.catalogue.domain.Product;
import com.finflow.commerce.catalogue.domain.ProductStatus;

import java.math.BigDecimal;

/**
 * Response DTO representing a product in the catalogue.
 * A record is suitable because this DTO carries response data and does not need mutable fields.
 * Includes product details and an optional category name.
 */
public record ProductResponse(
        Long productId, // Database-generated product identifier.
        Long organisationId, // Organisation that owns this product.
        Long categoryId, // Category identifier, if the product has a category.
        String categoryName, // Category display name. This can be null when the caller does not supply it.
        String name, // Product display name.
        BigDecimal unitPrice, // Current unit selling price.
        int availableQuantity, // Current available stock.
        ProductStatus status // Current product lifecycle status.
) {

    /**
     * Converts a Product entity without category-name information.
     * Keeping this mapping method here avoids repeating the same entity-to-response conversion in service methods.
     * Delegates to the overloaded method with null categoryName.
     */
    public static ProductResponse from(Product p) {
        return from(p, null);
    }

    /**
     * Converts a Product entity and includes its category name.
     * Keeping this mapping method here avoids repeating the same entity-to-response conversion in service methods.
     * The category name is supplied separately because Product stores the category ID rather than requiring a category object.
     */
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
