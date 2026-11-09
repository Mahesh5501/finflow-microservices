package com.finflow.commerce.service;

import com.finflow.commerce.entity.Cart;
import com.finflow.commerce.repository.CartRepository;
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