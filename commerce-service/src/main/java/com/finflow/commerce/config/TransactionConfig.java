
package com.finflow.commerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Configures programmatic transaction management.
 * Exposes TransactionTemplate as a Spring bean so that services can execute code inside explicitly controlled transactions.
 */
@Configuration
public class TransactionConfig {

    /**
     * Creates a TransactionTemplate using Spring's transaction manager.
     * PlatformTransactionManager controls transaction begin, commit and rollback operations.
     */
    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager); // TransactionTemplate delegates transaction handling to the supplied transaction manager.
    }
}
