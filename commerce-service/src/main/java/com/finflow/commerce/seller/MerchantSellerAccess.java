package com.finflow.commerce.seller;

import com.finflow.commerce.exception.ApiException;

import java.util.List;

/**
 * MERCHANT SELLER RESOLUTION (trusted source, not the HTTP body).
 *
 * Why this exists:
 * Customer APIs must not accept a seller or organisation id — that would let a caller act on another shop's catalogue or orders.
 * Merchant APIs used to take {@code organisationId} in JSON or query params; that duplicated what identity already asserts in the JWT {@code orgIds} claim and was easy to get wrong.
 * This helper is the single place that turns {@code AuthenticatedUser.orgIds()} into the internal seller key used on products, orders and carts (still stored as organisation id in persistence until a dedicated Seller entity is introduced).
 *
 * Call from application services after HTTP authentication — not from controllers alone — so non-HTTP callers cannot skip the same rule.
 */
public final class MerchantSellerAccess {

    private MerchantSellerAccess() {
    }

    /**
     * Returns the seller id the merchant user may manage on this request.
     *
     * <p>Today we require exactly one membership in {@code orgIds}. Multiple seller accounts per
     * user will need an explicit selector (header, path, or profile default); until then we fail
     * with {@code MULTIPLE_SELLER_ACCOUNTS} instead of guessing.</p>
     *
     * @param merchantOrgIds organisation ids from the verified access token (seller memberships)
     * @return internal seller id for catalogue, coupon, and order merchant operations
     */
    public static Long requireSellerId(List<Long> merchantOrgIds) {
        if (merchantOrgIds == null || merchantOrgIds.isEmpty()) {
            throw ApiException.forbidden("No seller account linked to this user");
        }
        if (merchantOrgIds.size() > 1) {
            throw ApiException.unprocessable(
                    "MULTIPLE_SELLER_ACCOUNTS",
                    "User belongs to multiple seller accounts; use one seller login or add seller selection later"
            );
        }
        return merchantOrgIds.getFirst();
    }
}
