package com.finflow.commerce.catalogue.application;

import com.finflow.commerce.catalogue.dto.ProductResponse;
import com.finflow.commerce.catalogue.domain.Product;
import org.springframework.stereotype.Component;

/**
 * MAPPING COMPONENT - ENTITY TO API DTO
 *
 * Converts the persistence/domain Product entity into the API response DTO.
 * Keeping this conversion outside the controller avoids mixing HTTP handling
 * with object mapping, and outside the repository avoids leaking API shapes
 * into the persistence layer.
 *
 * WHY A SMALL HAND-WRITTEN MAPPER?
 * The mapping is currently straightforward, so explicit constructor mapping is
 * easy to read and compile-time checked. A library such as MapStruct becomes
 * useful when the number of DTOs/mappings grows; reflection-based mapping is
 * not necessary for this small module.
 */
// @Component registers this mapper as a Spring bean. The application service
// receives it through constructor injection rather than constructing it itself.
@Component
public class ProductMapper {

    /**
     * Maps one Product into the response contract returned by the API.
     * categoryName is passed in by the service so list operations can
     * load category names in bulk and avoid an N+1 query per product.
     * This application component is called only by CatalogueService; it depends
     * on the domain type and response contract, and does no database/HTTP work.
     * Removing @Component requires explicit construction or @Bean registration.
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
