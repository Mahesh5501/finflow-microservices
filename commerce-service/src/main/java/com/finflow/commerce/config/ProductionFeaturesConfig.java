
package com.finflow.commerce.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configures production infrastructure features.
 *
 * This class enables:
 * 1. Spring caching.
 * 2. ShedLock for distributed scheduled-job locking.
 * 3. A JDBC-based ShedLock LockProvider.
 */
@Configuration

// Enables Spring's cache annotation infrastructure.
// Methods/classes can then use annotations such as @Cacheable,
// @CachePut and @CacheEvict.
@EnableCaching

// Enables ShedLock's @SchedulerLock support.
// The defaults apply when a scheduled job does not specify
// its own lockAtMostFor and lockAtLeastFor values.
@EnableSchedulerLock(defaultLockAtMostFor = "10m", defaultLockAtLeastFor = "10s")
public class ProductionFeaturesConfig {

    /**
     * Creates the ShedLock LockProvider.
     * ShedLock stores lock information in a database table. This implementation uses the application's DataSource.
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {

        // JdbcTemplate provides convenient JDBC operations. It uses the same DataSource configured for the application.
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Create ShedLock's table if it does not already exist.
        // Hibernate creates our JPA entity tables, but it does not automatically create this ShedLock infrastructure table.
        // IF NOT EXISTS prevents an existing table and its lock rows from being replaced during application restarts.
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS shedlock (
                    name VARCHAR(64) NOT NULL PRIMARY KEY,
                    lock_until TIMESTAMP(3) NOT NULL,
                    locked_at TIMESTAMP(3) NOT NULL,
                    locked_by VARCHAR(255) NOT NULL
                )
                """);

        // Create the JDBC-backed ShedLock provider.
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(jdbcTemplate) // Use the JdbcTemplate connected to our database.
                        .usingDbTime() // Use database time to coordinate locks. This avoids relying on potentially different application-server clocks.
                        .build()
        );
    }
}
