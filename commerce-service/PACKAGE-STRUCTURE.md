# FinFlow Commerce package structure

Updated 2026-11-11. Implemented feature-first modular monolith, rooted at `com.finflow.commerce`.

```text
com.finflow.commerce
|-- CommerceServiceApplication
|-- catalogue
|   |-- api/customer: CustomerCatalogueController
|   |-- api/merchant: MerchantCategoryController, MerchantProductController
|   |-- application: CatalogueService, ProductMapper
|   |-- domain: Product, Category, ProductStatus
|   |-- persistence: ProductRepository, CategoryRepository
|   `-- dto: CategoryRequest, CategoryResponse, ProductRequest, ProductResponse, ProductUpdateRequest
|-- cart
|   |-- api/customer: CartController
|   |-- application: CartService, AbandonedCartCleanupService
|   |-- domain: Cart, CartItem
|   |-- persistence: CartRepository
|   `-- dto: CartItemRequest, CartItemResponse, CartResponse, UpdateCartItemRequest
|-- order
|   |-- api/customer: CustomerOrderController
|   |-- api/merchant: MerchantOrderController
|   |-- application: CommerceService, OrderPlacementService, PaymentSettlementService,
|   |                OrderCancelService, ReservationExpiryService, CommerceEvents
|   |-- domain: CommerceOrder, OrderItem, OrderStatus, ShippingAddress,
|   |           IdempotencyRecord, PaymentOutbox, OutboxStatus
|   |-- persistence: CommerceOrderRepository, IdempotencyRecordRepository, PaymentOutboxRepository
|   `-- dto: CheckoutRequest, CreateOrderRequest, OrderItemRequest, OrderItemResponse,
|            OrderResponse, ShippingAddressRequest, ShippingAddressResponse
|-- payment
|   `-- PaymentGateway, WalletPaymentGateway, MockPaymentGateway, PaymentConfig,
|       PaymentChargeRequest, PaymentRefundRequest, PaymentResult, PaymentOutcome
|-- returns
|   |-- api/customer: CustomerReturnController
|   |-- api/merchant: MerchantReturnController
|   |-- application: ReturnService
|   |-- domain: ReturnRequest, ReturnStatus
|   |-- persistence: ReturnRequestRepository
|   `-- dto: ReturnRequestBody, ReturnResponse
|-- coupon
|   |-- api/merchant: MerchantCouponController
|   |-- application: CouponService
|   |-- domain: Coupon
|   |-- persistence: CouponRepository
|   `-- dto: CouponRequest, CouponResponse
|-- integration/event
|   `-- CommerceDomainEvent, CommerceEventType, CommerceEventPublisher,
|       KafkaEventConfig, KafkaCommerceEventPublisher, LoggingCommerceEventPublisher
|-- config: CacheConfig
|-- security: SecurityConfig, JwtAuthFilter, JwksKeyProvider, AuthenticatedUser, CommerceSecurityProperties
|-- exception: ApiException, ErrorResponse, GlobalExceptionHandler
`-- health: CommerceDependencyHealthController, CommerceDependencyHealthService
```

Tests mirror their features (`catalogue.application`, `order.application`, `payment`). Root context and workflow integration tests exercise discovery across features.

| Package role | Responsibility |
|---|---|
| api/customer or api/merchant | HTTP binding, Jakarta validation, trusted principal extraction, response status |
| application | Use-case orchestration, authorization, transactions, mapping |
| domain | Business state/invariants; pragmatic JPA entities, not duplicate persistence models |
| persistence | Spring Data queries and database access |
| dto | Stable JSON contracts, separate from entities |
| payment | Provider abstraction and conditional implementations; no wallet database access |

Dependencies point from cart to order/catalogue, returns to order/catalogue, and order to catalogue/coupon/payment. Catalogue and coupon do not call order. `integration.event` holds transport-neutral event contracts/publishers; the order-to-event mapper is in `order.application`, preventing a dependency back to JPA entities. Shared security/exception classes remain below the application scan root. This is a package boundary convention, not enforced module isolation.

Catalogue remains one cohesive service: small category management and product category validation share the same repositories; a split would add coordination without independent change needs. ProductMapper stays in application. CouponService was necessary to remove coupon management from the order facade. No CQRS, new microservice, service interface per class or dependency was introduced.

All production endpoints, JSON components, JPA mappings and provider properties are preserved. Cache annotations are now enabled; the cached category read checks membership before lookup and CategoryResponse supports the configured Redis serialization.

See [architecture and flows](COMMERCE_SERVICE_ARCHITECTURE.md) and the [exact move/change inventory](docs/FEATURE-FIRST-REFACTOR.md). The first three Catalogue migration batches already existed and were reused. The subsequent cleanup removed old migration scripts/backups, scaffold markers, unused PaymentSimulator and all empty legacy source folders. See [local testing instructions](README.md) for the JWT validation switch.
