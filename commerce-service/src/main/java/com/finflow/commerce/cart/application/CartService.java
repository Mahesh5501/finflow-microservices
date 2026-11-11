package com.finflow.commerce.cart.application;

import com.finflow.commerce.order.application.CommerceService;
import com.finflow.commerce.cart.dto.CartItemRequest;
import com.finflow.commerce.cart.dto.CartItemResponse;
import com.finflow.commerce.cart.dto.CartResponse;
import com.finflow.commerce.order.dto.CheckoutRequest;
import com.finflow.commerce.order.dto.CreateOrderRequest;
import com.finflow.commerce.order.dto.OrderItemRequest;
import com.finflow.commerce.order.dto.OrderResponse;
import com.finflow.commerce.cart.dto.UpdateCartItemRequest;
import com.finflow.commerce.cart.domain.Cart;
import com.finflow.commerce.cart.domain.CartItem;
import com.finflow.commerce.catalogue.domain.Product;
import com.finflow.commerce.catalogue.domain.ProductStatus;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.cart.persistence.CartRepository;
import com.finflow.commerce.catalogue.persistence.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Marketplace cart: shoppers add by {@code productId} only; seller is derived from the product row.
 *
 * Cart application layer, called by CartController. CartRepository loads only
 * the authenticated customer's seller buckets; ProductRepository supplies current
 * price, activity and stock; CommerceService coordinates order creation. Shoppers
 * need no membership in a seller organisation. Seller IDs come from stored products.
 * Constructor injection makes these required collaborators visible and easy to fake.
 *
 * @Service registers the use case with Spring; without it (or an explicit @Bean)
 * HTTP wiring fails. @Transactional on mutations groups cart changes and response
 * mapping; readOnly on getCart is a hint and keeps lazy items available while mapping.
 * Removing these boundaries can detach item collections or split persistence work.
 *
 * Checkout deliberately has no encompassing transaction: TransactionTemplate snapshots
 * one seller, the order service reserves stock and calls payment, then another short
 * transaction clears that seller. A single @Transactional method would keep a database
 * transaction open during wallet HTTP. The existing clear happens even on a returned
 * DECLINED/UNCERTAIN result; concurrent cart edits and checkout replay need further work.
 * Cart quantities are shopping intent, not stock reservations; placement revalidates.
 */
@Service
public class CartService {

    // Owner-scoped bucket persistence; never use a client-supplied owner ID.
    private final CartRepository cartRepository;
    // Authoritative seller, activity, current price and stock lookup.
    private final ProductRepository productRepository;
    // Reuses the single order placement/payment workflow during checkout.
    private final CommerceService commerceService;
    // Separates snapshot and clear commits from the external payment operation.
    private final TransactionTemplate transactionTemplate;

    public CartService(
            CartRepository cartRepository,
            ProductRepository productRepository,
            CommerceService commerceService,
            TransactionTemplate transactionTemplate
    ) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.commerceService = commerceService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Customer reads merge seller buckets, using live product prices rather than reserving stock.
     * Called by CartController; read-only transaction covers lazy item mapping.
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long customerUserId) {
        return toUnifiedResponse(customerUserId);
    }

    /**
     * Customer add validates active product and requested stock, derives the seller, then
     * increments the Cart domain quantity and returns the merged view in one transaction.
     * Missing/inactive products or insufficient requested stock fail before persistence.
     * This checks the incoming amount; accumulated quantity is revalidated at placement.
     */
    @Transactional
    public CartResponse addItem(
            Long customerUserId,
            List<Long> callerOrgIds,
            CartItemRequest request
    ) {
        Product product = requireActiveProduct(request.productId());
        long sellerOrg = product.getOrganisationId();

        if (product.getAvailableQuantity() < request.quantity()) {
            throw ApiException.conflict(
                    "INSUFFICIENT_STOCK",
                    "Not enough stock for product " + request.productId()
            );
        }

        Cart cart = getOrCreate(customerUserId, sellerOrg);
        cart.addQuantity(request.productId(), request.quantity());
        cartRepository.save(cart);
        return toUnifiedResponse(customerUserId);
    }

    /**
     * Customer PUT replaces a line quantity; zero removes it. Product state/stock is
     * checked before saving the seller bucket. A failure rolls back this cart mutation.
     */
    @Transactional
    public CartResponse upsertItem(
            Long customerUserId,
            List<Long> callerOrgIds,
            Long productId,
            UpdateCartItemRequest request
    ) {
        Product product = requireActiveProduct(productId);
        long sellerOrg = product.getOrganisationId();
        Cart cart = getOrCreate(customerUserId, sellerOrg);

        if (request.quantity() > 0 && product.getAvailableQuantity() < request.quantity()) {
            throw ApiException.conflict(
                    "INSUFFICIENT_STOCK",
                    "Not enough stock for product " + productId
            );
        }

        cart.upsertItem(productId, request.quantity());
        cartRepository.save(cart);
        return toUnifiedResponse(customerUserId);
    }

    /**
     * Customer DELETE derives seller from the stored product and removes only this
     * owner's line. A missing cart is harmless; a missing product retains the 404 contract.
     */
    @Transactional
    public CartResponse removeItem(
            Long customerUserId,
            List<Long> callerOrgIds,
            Long productId
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound(
                        "PRODUCT_NOT_FOUND",
                        "Product not found: " + productId
                ));
        cartRepository.findByCustomerUserIdAndOrganisationId(
                customerUserId,
                product.getOrganisationId()
        ).ifPresent(cart -> {
            cart.removeItem(productId);
            cartRepository.save(cart);
        });
        return toUnifiedResponse(customerUserId);
    }

    /**
     * Customer clear empties every seller bucket for this authenticated user.
     * The repository query never accepts a customer ID from the HTTP body.
     */
    @Transactional
    public CartResponse clear(Long customerUserId, List<Long> callerOrgIds) {
        for (Cart cart : cartRepository.findByCustomerUserId(customerUserId)) {
            cart.clearItems();
            cartRepository.save(cart);
        }
        return toUnifiedResponse(customerUserId);
    }

    /**
     * CartController checkout selects one seller (explicitly or by inference), snapshots
     * its lines, calls order placement/payment, then clears that seller separately.
     * Empty or ambiguous multi-seller carts fail before ordering. Do not add an outer
     * transaction: wallet calls must happen after placement commits.
     */
    public OrderResponse checkout(
            Long customerUserId,
            List<Long> callerOrgIds,
            String idempotencyKey,
            String accessToken,
            CheckoutRequest request,
            Long sellerOrganisationId
    ) {
        Long seller = sellerOrganisationId != null
                ? sellerOrganisationId
                : request.organisationId();
        if (seller == null) {
            // Seller inference reads lazy cart items. Keep those reads attached
            // without spanning the subsequent order placement/payment call.
            seller = transactionTemplate.execute(status -> inferCheckoutSeller(customerUserId));
        }

        final Long checkoutSeller = seller;
        List<OrderItemRequest> lines = transactionTemplate.execute(
                status -> snapshotLines(customerUserId, checkoutSeller)
        );

        OrderResponse order = commerceService.createOrder(
                customerUserId,
                callerOrgIds,
                idempotencyKey,
                accessToken,
                new CreateOrderRequest(
                        checkoutSeller,
                        lines,
                        request.shippingAddress(),
                        request.couponCode(),
                        request.paymentOutcome()
                )
        );

        final Long clearSeller = checkoutSeller;
        transactionTemplate.executeWithoutResult(status -> {
            cartRepository.findByCustomerUserIdAndOrganisationId(customerUserId, clearSeller)
                    .ifPresent(cart -> {
                        cart.clearItems();
                        cartRepository.save(cart);
                    });
        });

        return order;
    }

    /**
     * Finds the sole nonempty seller bucket; multi-seller carts require the existing
     * sellerOrganisationId checkout parameter. Does not change any cart.
     */
    private Long inferCheckoutSeller(Long customerUserId) {
        Set<Long> sellers = new HashSet<>();
        for (Cart cart : cartRepository.findByCustomerUserId(customerUserId)) {
            if (!cart.getItems().isEmpty()) {
                sellers.add(cart.getOrganisationId());
            }
        }
        if (sellers.isEmpty()) {
            throw ApiException.unprocessable("CART_EMPTY", "Cart is empty");
        }
        if (sellers.size() > 1) {
            throw ApiException.unprocessable(
                    "MULTI_SELLER_CHECKOUT",
                    "Cart has items from multiple sellers. Checkout one seller at a time using sellerOrganisationId."
            );
        }
        return sellers.iterator().next();
    }

    /**
     * Copies product IDs/quantities inside the short checkout snapshot transaction;
     * placement reloads current prices/stock. Missing or empty seller cart is CART_EMPTY.
     */
    private List<OrderItemRequest> snapshotLines(Long customerUserId, Long organisationId) {
        Cart cart = cartRepository.findByCustomerUserIdAndOrganisationId(
                        customerUserId,
                        organisationId
                )
                .orElseThrow(() -> ApiException.unprocessable("CART_EMPTY", "Cart is empty for this seller"));

        if (cart.getItems().isEmpty()) {
            throw ApiException.unprocessable("CART_EMPTY", "Cart is empty for this seller");
        }

        return cart.getItems().stream()
                .map(i -> new OrderItemRequest(i.getProductId(), i.getQuantity()))
                .toList();
    }

    private Cart getOrCreate(Long customerUserId, Long organisationId) {
        return cartRepository.findByCustomerUserIdAndOrganisationId(customerUserId, organisationId)
                .orElseGet(() -> new Cart(customerUserId, organisationId));
    }

    private Product requireActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound(
                        "PRODUCT_NOT_FOUND",
                        "Product not found: " + productId
                ));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw ApiException.unprocessable(
                    "PRODUCT_INACTIVE",
                    "Product is not active: " + productId
            );
        }
        return product;
    }

    /**
     * Maps all owner buckets to DTOs while items are attached, summing current prices.
     * Missing products retain the existing Unknown/zero-price fallback. This per-line
     * lookup is an N+1 optimization opportunity, not an immutable checkout price.
     */
    private CartResponse toUnifiedResponse(Long customerUserId) {
        List<Cart> carts = cartRepository.findByCustomerUserId(customerUserId);
        List<CartItemResponse> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        Instant latestUpdate = null;

        for (Cart cart : carts) {
            for (CartItem item : cart.getItems()) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                String name = product == null ? "Unknown" : product.getName();
                BigDecimal price = product == null ? BigDecimal.ZERO : product.getUnitPrice();
                int available = product == null ? 0 : product.getAvailableQuantity();
                Long seller = product == null ? cart.getOrganisationId() : product.getOrganisationId();
                BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
                subtotal = subtotal.add(lineTotal);
                lines.add(new CartItemResponse(
                        item.getProductId(),
                        name,
                        price,
                        item.getQuantity(),
                        lineTotal,
                        available,
                        seller
                ));
            }
            if (cart.getUpdatedAt() != null
                    && (latestUpdate == null || cart.getUpdatedAt().isAfter(latestUpdate))) {
                latestUpdate = cart.getUpdatedAt();
            }
        }

        lines.sort(Comparator.comparing(CartItemResponse::productId));

        return new CartResponse(
                null,
                null,
                customerUserId,
                subtotal,
                latestUpdate,
                lines
        );
    }
}
