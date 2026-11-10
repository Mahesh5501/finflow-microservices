package com.finflow.commerce.service.cart;

import com.finflow.commerce.service.order.CommerceService;
import com.finflow.commerce.dto.cart.CartItemRequest;
import com.finflow.commerce.dto.cart.CartItemResponse;
import com.finflow.commerce.dto.cart.CartResponse;
import com.finflow.commerce.dto.order.CheckoutRequest;
import com.finflow.commerce.dto.order.CreateOrderRequest;
import com.finflow.commerce.dto.order.OrderItemRequest;
import com.finflow.commerce.dto.order.OrderResponse;
import com.finflow.commerce.dto.cart.UpdateCartItemRequest;
import com.finflow.commerce.entity.cart.Cart;
import com.finflow.commerce.entity.cart.CartItem;
import com.finflow.commerce.entity.catalogue.Product;
import com.finflow.commerce.entity.catalogue.ProductStatus;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.repository.cart.CartRepository;
import com.finflow.commerce.repository.catalogue.ProductRepository;
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
 */
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CommerceService commerceService;
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

    @Transactional(readOnly = true)
    public CartResponse getCart(Long customerUserId) {
        return toUnifiedResponse(customerUserId);
    }

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

    @Transactional
    public CartResponse clear(Long customerUserId, List<Long> callerOrgIds) {
        for (Cart cart : cartRepository.findByCustomerUserId(customerUserId)) {
            cart.clearItems();
            cartRepository.save(cart);
        }
        return toUnifiedResponse(customerUserId);
    }

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
            seller = inferCheckoutSeller(customerUserId);
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
