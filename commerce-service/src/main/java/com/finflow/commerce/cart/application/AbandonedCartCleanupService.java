package com.finflow.commerce.cart.application;

import com.finflow.commerce.cart.domain.Cart;
import com.finflow.commerce.cart.persistence.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Deletes carts that have not been updated since the supplied cutoff.
 *
 * These carts represent abandoned browsing sessions.
 *
 * Cart application maintenance use case depending only on CartRepository.
 * It is callable by a future scheduler/operator; no scheduler is wired in the current
 * application. @Service makes it injectable and @Transactional groups entity deletes
 * and child cascade removal. Without that application boundary separate repository
 * calls may commit independently. A bulk delete would need explicit child cleanup
 * and is not equivalent to the existing entity-lifecycle operation.
 */
@Service
public class AbandonedCartCleanupService {

    // Used to record cleanup activity.
    private static final Logger log =
            LoggerFactory.getLogger(AbandonedCartCleanupService.class);

    // Repository used to find and delete carts.
    private final CartRepository cartRepository;

    public AbandonedCartCleanupService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    /**
     * Deletes carts whose updatedAt timestamp is older than cutoff.
     *
     * @return number of carts removed
     */
    @Transactional
    public int purgeAbandonedCarts(Instant cutoff) {

        // Fetch all carts that have not been updated since cutoff.
        List<Cart> abandoned =
                cartRepository.findByUpdatedAtBefore(cutoff);

        // Avoid executing a delete operation when there is nothing to delete.
        if (abandoned.isEmpty()) {
            return 0;
        }

        // Delete the selected carts.
        // This executes inside the current database transaction.
        cartRepository.deleteAll(abandoned);

        // Record how many carts were removed.
        log.info(
                "Purged {} abandoned cart(s) older than {}",
                abandoned.size(),
                cutoff
        );

        return abandoned.size();
    }
}