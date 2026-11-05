package com.finflow.commerce.repository;

import com.finflow.commerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByOrganisationIdOrderByNameAsc(Long organisationId);

    Optional<Category> findByOrganisationIdAndSlugIgnoreCase(Long organisationId, String slug);

    boolean existsByOrganisationIdAndSlugIgnoreCase(Long organisationId, String slug);
}
