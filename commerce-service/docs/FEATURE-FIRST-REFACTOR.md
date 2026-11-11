# Feature-first refactor change inventory

Date: 2026-11-11. Paths are relative to commerce-service.

Baseline: the user's already completed Catalogue batches and other working-tree changes were present before this task. The moves below are additions to those batches; no earlier migration was repeated.

## Moved files

| Previous path | Current path |
|---|---|
| `src/main/java/com/finflow/commerce/dto/catalogue/CategoryRequest.java` | `src/main/java/com/finflow/commerce/catalogue/dto/CategoryRequest.java` |
| `src/main/java/com/finflow/commerce/dto/catalogue/CategoryResponse.java` | `src/main/java/com/finflow/commerce/catalogue/dto/CategoryResponse.java` |
| `src/main/java/com/finflow/commerce/dto/catalogue/ProductRequest.java` | `src/main/java/com/finflow/commerce/catalogue/dto/ProductRequest.java` |
| `src/main/java/com/finflow/commerce/dto/catalogue/ProductResponse.java` | `src/main/java/com/finflow/commerce/catalogue/dto/ProductResponse.java` |
| `src/main/java/com/finflow/commerce/dto/catalogue/ProductUpdateRequest.java` | `src/main/java/com/finflow/commerce/catalogue/dto/ProductUpdateRequest.java` |
| `src/main/java/com/finflow/commerce/dto/cart/CartItemRequest.java` | `src/main/java/com/finflow/commerce/cart/dto/CartItemRequest.java` |
| `src/main/java/com/finflow/commerce/dto/cart/CartItemResponse.java` | `src/main/java/com/finflow/commerce/cart/dto/CartItemResponse.java` |
| `src/main/java/com/finflow/commerce/dto/cart/CartResponse.java` | `src/main/java/com/finflow/commerce/cart/dto/CartResponse.java` |
| `src/main/java/com/finflow/commerce/dto/cart/UpdateCartItemRequest.java` | `src/main/java/com/finflow/commerce/cart/dto/UpdateCartItemRequest.java` |
| `src/main/java/com/finflow/commerce/entity/cart/Cart.java` | `src/main/java/com/finflow/commerce/cart/domain/Cart.java` |
| `src/main/java/com/finflow/commerce/entity/cart/CartItem.java` | `src/main/java/com/finflow/commerce/cart/domain/CartItem.java` |
| `src/main/java/com/finflow/commerce/repository/cart/CartRepository.java` | `src/main/java/com/finflow/commerce/cart/persistence/CartRepository.java` |
| `src/main/java/com/finflow/commerce/service/cart/AbandonedCartCleanupService.java` | `src/main/java/com/finflow/commerce/cart/application/AbandonedCartCleanupService.java` |
| `src/main/java/com/finflow/commerce/service/cart/CartService.java` | `src/main/java/com/finflow/commerce/cart/application/CartService.java` |
| `src/main/java/com/finflow/commerce/dto/order/CheckoutRequest.java` | `src/main/java/com/finflow/commerce/order/dto/CheckoutRequest.java` |
| `src/main/java/com/finflow/commerce/dto/order/CreateOrderRequest.java` | `src/main/java/com/finflow/commerce/order/dto/CreateOrderRequest.java` |
| `src/main/java/com/finflow/commerce/dto/order/OrderItemRequest.java` | `src/main/java/com/finflow/commerce/order/dto/OrderItemRequest.java` |
| `src/main/java/com/finflow/commerce/dto/order/OrderItemResponse.java` | `src/main/java/com/finflow/commerce/order/dto/OrderItemResponse.java` |
| `src/main/java/com/finflow/commerce/dto/order/OrderResponse.java` | `src/main/java/com/finflow/commerce/order/dto/OrderResponse.java` |
| `src/main/java/com/finflow/commerce/dto/order/ShippingAddressRequest.java` | `src/main/java/com/finflow/commerce/order/dto/ShippingAddressRequest.java` |
| `src/main/java/com/finflow/commerce/dto/order/ShippingAddressResponse.java` | `src/main/java/com/finflow/commerce/order/dto/ShippingAddressResponse.java` |
| `src/main/java/com/finflow/commerce/entity/order/CommerceOrder.java` | `src/main/java/com/finflow/commerce/order/domain/CommerceOrder.java` |
| `src/main/java/com/finflow/commerce/entity/order/IdempotencyRecord.java` | `src/main/java/com/finflow/commerce/order/domain/IdempotencyRecord.java` |
| `src/main/java/com/finflow/commerce/entity/order/OrderItem.java` | `src/main/java/com/finflow/commerce/order/domain/OrderItem.java` |
| `src/main/java/com/finflow/commerce/entity/order/OrderStatus.java` | `src/main/java/com/finflow/commerce/order/domain/OrderStatus.java` |
| `src/main/java/com/finflow/commerce/entity/order/OutboxStatus.java` | `src/main/java/com/finflow/commerce/order/domain/OutboxStatus.java` |
| `src/main/java/com/finflow/commerce/entity/order/PaymentOutbox.java` | `src/main/java/com/finflow/commerce/order/domain/PaymentOutbox.java` |
| `src/main/java/com/finflow/commerce/entity/order/ShippingAddress.java` | `src/main/java/com/finflow/commerce/order/domain/ShippingAddress.java` |
| `src/main/java/com/finflow/commerce/repository/order/CommerceOrderRepository.java` | `src/main/java/com/finflow/commerce/order/persistence/CommerceOrderRepository.java` |
| `src/main/java/com/finflow/commerce/repository/order/IdempotencyRecordRepository.java` | `src/main/java/com/finflow/commerce/order/persistence/IdempotencyRecordRepository.java` |
| `src/main/java/com/finflow/commerce/repository/order/PaymentOutboxRepository.java` | `src/main/java/com/finflow/commerce/order/persistence/PaymentOutboxRepository.java` |
| `src/main/java/com/finflow/commerce/service/order/CommerceService.java` | `src/main/java/com/finflow/commerce/order/application/CommerceService.java` |
| `src/main/java/com/finflow/commerce/service/order/OrderCancelService.java` | `src/main/java/com/finflow/commerce/order/application/OrderCancelService.java` |
| `src/main/java/com/finflow/commerce/service/order/OrderPlacementService.java` | `src/main/java/com/finflow/commerce/order/application/OrderPlacementService.java` |
| `src/main/java/com/finflow/commerce/service/order/PaymentSettlementService.java` | `src/main/java/com/finflow/commerce/order/application/PaymentSettlementService.java` |
| `src/main/java/com/finflow/commerce/service/order/ReservationExpiryService.java` | `src/main/java/com/finflow/commerce/order/application/ReservationExpiryService.java` |
| `src/test/java/com/finflow/commerce/service/order/ReservationExpiryServiceTest.java` | `src/test/java/com/finflow/commerce/order/application/ReservationExpiryServiceTest.java` |
| `src/main/java/com/finflow/commerce/dto/returns/ReturnRequestBody.java` | `src/main/java/com/finflow/commerce/returns/dto/ReturnRequestBody.java` |
| `src/main/java/com/finflow/commerce/dto/returns/ReturnResponse.java` | `src/main/java/com/finflow/commerce/returns/dto/ReturnResponse.java` |
| `src/main/java/com/finflow/commerce/entity/returns/ReturnRequest.java` | `src/main/java/com/finflow/commerce/returns/domain/ReturnRequest.java` |
| `src/main/java/com/finflow/commerce/entity/returns/ReturnStatus.java` | `src/main/java/com/finflow/commerce/returns/domain/ReturnStatus.java` |
| `src/main/java/com/finflow/commerce/repository/returns/ReturnRequestRepository.java` | `src/main/java/com/finflow/commerce/returns/persistence/ReturnRequestRepository.java` |
| `src/main/java/com/finflow/commerce/service/returns/ReturnService.java` | `src/main/java/com/finflow/commerce/returns/application/ReturnService.java` |
| `src/main/java/com/finflow/commerce/dto/coupon/CouponRequest.java` | `src/main/java/com/finflow/commerce/coupon/dto/CouponRequest.java` |
| `src/main/java/com/finflow/commerce/dto/coupon/CouponResponse.java` | `src/main/java/com/finflow/commerce/coupon/dto/CouponResponse.java` |
| `src/main/java/com/finflow/commerce/entity/coupon/Coupon.java` | `src/main/java/com/finflow/commerce/coupon/domain/Coupon.java` |
| `src/main/java/com/finflow/commerce/repository/coupon/CouponRepository.java` | `src/main/java/com/finflow/commerce/coupon/persistence/CouponRepository.java` |
| `src/main/java/com/finflow/commerce/controller/customer/CartController.java` | `src/main/java/com/finflow/commerce/cart/api/customer/CartController.java` |
| `src/main/java/com/finflow/commerce/controller/customer/CustomerOrderController.java` | `src/main/java/com/finflow/commerce/order/api/customer/CustomerOrderController.java` |
| `src/main/java/com/finflow/commerce/controller/merchant/MerchantOrderController.java` | `src/main/java/com/finflow/commerce/order/api/merchant/MerchantOrderController.java` |
| `src/main/java/com/finflow/commerce/controller/customer/CustomerReturnController.java` | `src/main/java/com/finflow/commerce/returns/api/customer/CustomerReturnController.java` |
| `src/main/java/com/finflow/commerce/controller/merchant/MerchantReturnController.java` | `src/main/java/com/finflow/commerce/returns/api/merchant/MerchantReturnController.java` |
| `src/main/java/com/finflow/commerce/controller/merchant/MerchantCouponController.java` | `src/main/java/com/finflow/commerce/coupon/api/merchant/MerchantCouponController.java` |
| `src/main/java/com/finflow/commerce/integration/payment/PaymentChargeRequest.java` | `src/main/java/com/finflow/commerce/payment/PaymentChargeRequest.java` |
| `src/main/java/com/finflow/commerce/integration/payment/PaymentConfig.java` | `src/main/java/com/finflow/commerce/payment/PaymentConfig.java` |
| `src/main/java/com/finflow/commerce/integration/payment/PaymentOutcome.java` | `src/main/java/com/finflow/commerce/payment/PaymentOutcome.java` |
| `src/main/java/com/finflow/commerce/integration/payment/PaymentPort.java` | `src/main/java/com/finflow/commerce/payment/PaymentGateway.java` |
| `src/main/java/com/finflow/commerce/integration/payment/PaymentRefundRequest.java` | `src/main/java/com/finflow/commerce/payment/PaymentRefundRequest.java` |
| `src/main/java/com/finflow/commerce/integration/payment/PaymentResult.java` | `src/main/java/com/finflow/commerce/payment/PaymentResult.java` |
| `src/main/java/com/finflow/commerce/integration/payment/PaymentSimulator.java` | `src/main/java/com/finflow/commerce/payment/PaymentSimulator.java` |
| `src/main/java/com/finflow/commerce/integration/payment/SimulatedPaymentAdapter.java` | `src/main/java/com/finflow/commerce/payment/MockPaymentGateway.java` |
| `src/main/java/com/finflow/commerce/integration/payment/WalletPaymentAdapter.java` | `src/main/java/com/finflow/commerce/payment/WalletPaymentGateway.java` |
| `src/main/java/com/finflow/commerce/integration/event/CommerceEvents.java` | `src/main/java/com/finflow/commerce/order/application/CommerceEvents.java` |

## Created classes

- `coupon.application.CouponService`: merchant creation and seller-scoped checkout lookup.
- `config.CacheConfig`: activates the existing catalogue caching annotations.
- Tests: `FeatureWorkflowIntegrationTest`, `payment.PaymentGatewayTest`.

## Modified existing classes

- Moved classes: package/import updates; important API/application/domain/repository/payment classes also receive source explanations.
- `catalogue.application.CatalogueService`: DTO imports, cache-hit membership guard, consolidated class documentation; no split.
- `catalogue.application.ProductMapper`: new DTO import and consolidated mapping explanations.
- `catalogue.api.customer.CustomerCatalogueController`, `catalogue.api.merchant.MerchantCategoryController`, `catalogue.api.merchant.MerchantProductController`: new DTO imports and consolidated API-layer documentation above the annotations.
- `catalogue.persistence.CategoryRepository`, `catalogue.persistence.ProductRepository`: repository discovery, transaction and authorization explanations.
- `catalogue.dto.CategoryResponse`: Serializable for the configured Redis serializer, same JSON fields.
- `order.application.CommerceService`: removed catalogue delegates/coupon management and their dependencies.
- `order.application.OrderPlacementService`: coupon lookup now calls CouponService within the existing placement transaction.
- `coupon.api.merchant.MerchantCouponController`: calls CouponService directly.
- `integration.event.CommerceEvents` moved to `order.application` to keep the event integration package independent of entities.
- `CommerceServiceApplication`: scanning/ownership documentation.
- `order.application.PaymentSettlementService`: repeated UNCERTAIN retains ACTION_REQUIRED while updating payment work; no general domain-transition change.
- Existing `CatalogueServiceTest` and `ReservationExpiryServiceTest`: new imports/boundaries, missing cart imports fixed, cache/isolation regressions.
- Event factory imports updated in event-producing services. Test cache selects simple instead of external Redis; test datasource settings are unchanged.

## Deletions and preserved files

- The move table records the earlier refactor. Subsequent cleanup also removed the unused legacy PaymentSimulator, described below.
- No unrelated Java class was deleted. PaymentSimulator was later removed after confirming no application/test callers.
- The refactor initially preserved scaffolding, scripts and backups; the explicitly requested cleanup below subsequently removed them.
- Existing POM, production properties, SQL mapping names, security policy and event JSON are preserved.

## Static review evidence

- Compared all 15 domain types against the task-start source after stripping package/import/comments: identical domain code and JPA mappings.
- Compared all route/status annotation arguments across 9 existing controllers: identical.
- Checked every main/test Java declaration against its source path and checked for obsolete layer-first imports and payment types.
- Maven verification and runtime limitations are recorded in COMMERCE_SERVICE_ARCHITECTURE.md and PROJECT_STATUS.md.

## Empty directory cleanup limitation

Historical limitation during the refactor: these empty directories resisted Python deletion. The subsequent native PowerShell cleanup removed them all without changing permissions:

- `src/main/java/com/finflow/commerce/controller`
- `src/main/java/com/finflow/commerce/dto`
- `src/main/java/com/finflow/commerce/entity`
- `src/main/java/com/finflow/commerce/repository`
- `src/main/java/com/finflow/commerce/service`
- `src/test/java/com/finflow/commerce/service`
## Updated documentation

- PACKAGE-STRUCTURE.md: complete current class/package map.
- COMMERCE_SERVICE_ARCHITECTURE.md: six source-traced feature flows, decisions, contracts, test evidence and production limits.
- ../docs/ARCHITECTURE.md and ../docs/DECISIONS.md: current Commerce workflow/decision record.
- ../PROJECT_STATUS.md: current task checkpoint, service progress and verification.

## Subsequent cleanup and standalone testing - 2026-11-11

Removed 84 obsolete files (and their empty directories):

- `.migration-backups/`: 33 duplicate historical source files.
- `scripts/cleanup-legacy-packages.py`, `flatten-controllers.py`, `migrate-catalogue-batch-1.py`, `migrate-catalogue-batch-2.py`, `migrate-catalogue-batch-3.py`, `migrate-packages.py`, `migrate-to-layered-subpackages.py`, `relocate-cross-cutting.py`: eight obsolete migration scripts. The legacy cleanup script listed active payment/security packages and must not be used.
- `create-commerce-structure.ps1`: completed scaffolding generator.
- 41 zero-byte `.gitkeep` markers; removed empty test/legacy folders after checking contents.
- `payment/PaymentSimulator.java`: unused deprecated no-op class; no current main/test callers.

Added README.md, CommerceSecurityProperties, application-local.properties and three security test classes. Modified JwtAuthFilter, SecurityConfig, dependency health, PaymentResult/MockPaymentGateway/settlement, the cart seller-inference transaction and payment contract tests. JWT stays on normally; explicit local mode binds loopback, uses a configured principal and simulated payments. The internal simulated-result flag fixes standalone approval without fake wallet references. No public endpoint/JSON or database mapping changed. See README and architecture for operation and final verification.
