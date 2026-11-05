package com.finflow.commerce.job;

import com.finflow.commerce.service.AbandonedCartCleanupService;
import com.finflow.commerce.service.ReservationExpiryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Periodic stock / cart hygiene.
 * Disable with {@code commerce.maintenance.enabled=false} (tests often leave it on with short TTL overrides).
 */
@Component
@ConditionalOnProperty(name = "commerce.maintenance.enabled", havingValue = "true", matchIfMissing = true)
public class CommerceMaintenanceJobs {

    private final ReservationExpiryService reservationExpiryService;
    private final AbandonedCartCleanupService abandonedCartCleanupService;
    private final long reservationTtlMinutes;
    private final long cartAbandonHours;

    public CommerceMaintenanceJobs(
            ReservationExpiryService reservationExpiryService,
            AbandonedCartCleanupService abandonedCartCleanupService,
            @Value("${commerce.reservation-ttl-minutes:30}") long reservationTtlMinutes,
            @Value("${commerce.cart-abandon-hours:72}") long cartAbandonHours
    ) {
        this.reservationExpiryService = reservationExpiryService;
        this.abandonedCartCleanupService = abandonedCartCleanupService;
        this.reservationTtlMinutes = reservationTtlMinutes;
        this.cartAbandonHours = cartAbandonHours;
    }

    @Scheduled(fixedDelayString = "${commerce.maintenance.reservation-fixed-delay-ms:60000}")
    public void expireReservations() {
        Instant cutoff = Instant.now().minus(reservationTtlMinutes, ChronoUnit.MINUTES);
        reservationExpiryService.expireStaleReservations(cutoff);
    }

    @Scheduled(fixedDelayString = "${commerce.maintenance.cart-fixed-delay-ms:300000}")
    public void purgeAbandonedCarts() {
        Instant cutoff = Instant.now().minus(cartAbandonHours, ChronoUnit.HOURS);
        abandonedCartCleanupService.purgeAbandonedCarts(cutoff);
    }
}
