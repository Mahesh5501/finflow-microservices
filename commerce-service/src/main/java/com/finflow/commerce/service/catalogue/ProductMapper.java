
package com.finflow.commerce.service.catalogue;

import com.finflow.commerce.dto.catalogue.ProductResponse;
import com.finflow.commerce.entity.catalogue.Product;
import org.springframework.stereotype.Component;

/**
 * Converts Product entities into ProductResponse DTOs.
 * The mapper keeps entity-to-response conversion out of the service and controller layers.
 */
@Component
public class ProductMapper {

    /**
     * Converts a Product entity into a ProductResponse. Returns the ProductResponse containing the API-facing product data.
     * @param p the Product entity loaded from the database.
     * @param categoryName the name of the product's category.
     * @return ProductResponse containing the API-facing product data.
     */
    public ProductResponse toResponse(Product p, String categoryName) {
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
