package com.finflow.commerce.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure configuration enabling CatalogueService's cache annotations.
 * Boot selects Redis from the existing spring.cache.type property; tests explicitly
 * select a local cache. @Configuration registers this setup and @EnableCaching
 * installs Spring's interception advice. Without the latter @Cacheable/@CacheEvict
 * are inert. An explicit CacheManager bean is an alternative when per-cache TTLs
 * or a different serializer are required; the existing Boot configuration suffices.
 *
 * <p>Cached reads still require authorization before lookup (see listCategories).
 * Cache eviction avoids stale reads after creation; it is not a database transaction
 * or a substitute for table constraints. Calls must go through the Spring proxy:
 * a direct self-invocation does not activate caching or transactional advice.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
