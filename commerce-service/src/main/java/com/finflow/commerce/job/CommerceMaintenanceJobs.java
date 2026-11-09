
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
 * Coordinates recurring maintenance tasks for the Commerce Service.
 *
 * This class decides WHEN maintenance tasks should run.
 *
 * The actual business operations are delegated to:
 * - ReservationExpiryService
 * - AbandonedCartCleanupService
 *
 * Keeping scheduling separate from business logic makes the
 * services easier to test and reuse.
 */
@Component

/**
 * Registers this component only when the maintenance property is enabled.
 *
 * If commerce.maintenance.enabled is missing, maintenance is enabled
 * because matchIfMissing = true.
 *
 * Set commerce.maintenance.enabled=false to disable these jobs.
 */
@ConditionalOnProperty(
        name = "commerce.maintenance.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CommerceMaintenanceJobs {

    // Handles expiration of unpaid order reservations.
    private final ReservationExpiryService reservationExpiryService;

    // Handles deletion of abandoned shopping carts.
    private final AbandonedCartCleanupService abandonedCartCleanupService;

    // How long an order can remain unpaid before its reservation expires.
    private final long reservationTtlMinutes;

    // How long a cart can remain untouched before it is considered abandoned.
    private final long cartAbandonHours;

    /**
     * Constructor injection.
     *
     * @Value reads configurable values from application properties.
     *
     * Defaults:
     * - Reservation TTL: 30 minutes.
     * - Cart abandonment threshold: 72 hours.
     */
    public CommerceMaintenanceJobs(
            ReservationExpiryService reservationExpiryService,
            AbandonedCartCleanupService abandonedCartCleanupService,
            @Value("${commerce.reservation-ttl-minutes:30}")
            long reservationTtlMinutes,
            @Value("${commerce.cart-abandon-hours:72}")
            long cartAbandonHours
    ) {
        this.reservationExpiryService = reservationExpiryService;
        this.abandonedCartCleanupService = abandonedCartCleanupService;
        this.reservationTtlMinutes = reservationTtlMinutes;
        this.cartAbandonHours = cartAbandonHours;
    }

    /**
     * Scheduled job for expiring unpaid order reservations.
     *
     * fixedDelayString:
     * Waits the configured delay after the previous execution finishes
     * before starting the next execution.
     *
     * Default delay: 60,000 milliseconds (60 seconds).
     *
     * SchedulerLock:
     * Prevents multiple application instances from executing this
     * scheduled task simultaneously, using ShedLock.
     */
    @Scheduled(
            fixedDelayString =
                    "${commerce.maintenance.reservation-fixed-delay-ms:60000}"
    )
    @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(
            name = "expireReservationsLock",
            lockAtMostFor = "50s",
            lockAtLeastFor = "10s"
    )
    public void expireReservations() {

        // Calculate the time before which an unpaid order is considered stale.
        Instant cutoff = Instant.now().minus(
                reservationTtlMinutes,
                ChronoUnit.MINUTES
        );

        // Delegate the actual expiration and stock-release logic.
        reservationExpiryService.expireStaleReservations(cutoff);
    }

    /**
     * Scheduled job for removing abandoned carts.
     *
     * Default delay: 300,000 milliseconds (5 minutes).
     *
     * The cart abandonment threshold defaults to 72 hours.
     */
    @Scheduled(
            fixedDelayString =
                    "${commerce.maintenance.cart-fixed-delay-ms:300000}"
    )
    @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(
            name = "purgeAbandonedCartsLock",
            lockAtMostFor = "4m",
            lockAtLeastFor = "30s"
    )
    public void purgeAbandonedCarts() {

        // A cart last updated before this cutoff is considered abandoned.
        Instant cutoff = Instant.now().minus(
                cartAbandonHours,
                ChronoUnit.HOURS
        );

        // Delegate the actual cart deletion.
        abandonedCartCleanupService.purgeAbandonedCarts(cutoff);
    }
}
