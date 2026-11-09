
package com.finflow.commerce.dto;

import com.finflow.commerce.entity.Product;
import com.finflow.commerce.entity.ProductStatus;

import java.math.BigDecimal;

/**
 * Response DTO representing a product in the catalogue.
 *
 * Includes product details and an optional category name.
 */
public record ProductResponse(

        // Database-generated product identifier.
        Long productId,

        // Organisation that owns this product.
        Long organisationId,

        // Category identifier, if the product has a category.
        Long categoryId,

        // Category display name.
        // This can be null when the caller does not supply it.
        String categoryName,

        // Product display name.
        String name,

        // Current unit selling price.
        BigDecimal unitPrice,

        // Current available stock.
        int availableQuantity,

        // Current product lifecycle status.
        ProductStatus status

) {

    /**
     * Converts a Product entity without category-name information.
     *
     * Delegates to the overloaded method with null categoryName.
     */
    public static ProductResponse from(Product p) {
        return from(p, null);
    }

    /**
     * Converts a Product entity and includes its category name.
     *
     * The category name is supplied separately because Product
     * stores the category ID rather than requiring a category object.
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
