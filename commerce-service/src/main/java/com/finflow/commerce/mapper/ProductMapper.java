package com.finflow.commerce.mapper;

import com.finflow.commerce.dto.ProductResponse;
import com.finflow.commerce.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

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
