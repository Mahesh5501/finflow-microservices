package com.finflow.commerce.service;

import com.finflow.commerce.entity.Cart;
import com.finflow.commerce.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Deletes carts that have not been touched since {@code cutoff} (abandoned browse sessions). */
@Service
public class AbandonedCartCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AbandonedCartCleanupService.class);

    private final CartRepository cartRepository;

    public AbandonedCartCleanupService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    /** @return number of carts removed */
    @Transactional
    public int purgeAbandonedCarts(Instant cutoff) {
        List<Cart> abandoned = cartRepository.findByUpdatedAtBefore(cutoff);
        if (abandoned.isEmpty()) {
            return 0;
        }
        cartRepository.deleteAll(abandoned);
        log.info("Purged {} abandoned cart(s) older than {}", abandoned.size(), cutoff);
        return abandoned.size();
    }
}
