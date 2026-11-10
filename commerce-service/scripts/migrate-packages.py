#!/usr/bin/env python3
"""One-shot package restructure for commerce-service (feature + hexagonal layout)."""
from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA_SRC = ROOT / "src" / "main" / "java" / "com" / "finflow" / "commerce"
TEST_SRC = ROOT / "src" / "test" / "java" / "com" / "finflow" / "commerce"

# relative path from com/finflow/commerce -> new relative path
FILE_MOVES: dict[str, str] = {
    # catalogue
    "controller/ProductController.java": "catalogue/api/ProductController.java",
    "controller/CategoryController.java": "catalogue/api/CategoryController.java",
    "service/CatalogueService.java": "catalogue/application/CatalogueService.java",
    "mapper/ProductMapper.java": "catalogue/application/ProductMapper.java",
    "entity/Product.java": "catalogue/domain/Product.java",
    "entity/Category.java": "catalogue/domain/Category.java",
    "repository/ProductRepository.java": "catalogue/persistence/ProductRepository.java",
    "repository/CategoryRepository.java": "catalogue/persistence/CategoryRepository.java",
    "dto/ProductRequest.java": "catalogue/api/dto/ProductRequest.java",
    "dto/ProductResponse.java": "catalogue/api/dto/ProductResponse.java",
    "dto/ProductUpdateRequest.java": "catalogue/api/dto/ProductUpdateRequest.java",
    "dto/CategoryRequest.java": "catalogue/api/dto/CategoryRequest.java",
    "dto/CategoryResponse.java": "catalogue/api/dto/CategoryResponse.java",
    # cart
    "controller/CartController.java": "cart/api/CartController.java",
    "service/CartService.java": "cart/application/CartService.java",
    "service/AbandonedCartCleanupService.java": "cart/application/AbandonedCartCleanupService.java",
    "entity/Cart.java": "cart/domain/Cart.java",
    "entity/CartItem.java": "cart/domain/CartItem.java",
    "repository/CartRepository.java": "cart/persistence/CartRepository.java",
    "dto/CartResponse.java": "cart/api/dto/CartResponse.java",
    "dto/CartItemResponse.java": "cart/api/dto/CartItemResponse.java",
    "dto/CartItemRequest.java": "cart/api/dto/CartItemRequest.java",
    "dto/UpdateCartItemRequest.java": "cart/api/dto/UpdateCartItemRequest.java",
    # coupon
    "controller/CouponController.java": "coupon/api/CouponController.java",
    "entity/Coupon.java": "coupon/domain/Coupon.java",
    "repository/CouponRepository.java": "coupon/persistence/CouponRepository.java",
    "dto/CouponRequest.java": "coupon/api/dto/CouponRequest.java",
    "dto/CouponResponse.java": "coupon/api/dto/CouponResponse.java",
    # order
    "controller/OrderController.java": "order/api/OrderController.java",
    "service/CommerceService.java": "order/application/CommerceService.java",
    "service/OrderPlacementService.java": "order/application/OrderPlacementService.java",
    "service/PaymentSettlementService.java": "order/application/PaymentSettlementService.java",
    "service/OrderCancelService.java": "order/application/OrderCancelService.java",
    "service/ReservationExpiryService.java": "order/application/ReservationExpiryService.java",
    "entity/CommerceOrder.java": "order/domain/CommerceOrder.java",
    "entity/OrderItem.java": "order/domain/OrderItem.java",
    "entity/OrderStatus.java": "order/domain/OrderStatus.java",
    "entity/ShippingAddress.java": "order/domain/ShippingAddress.java",
    "entity/IdempotencyRecord.java": "order/domain/IdempotencyRecord.java",
    "entity/PaymentOutbox.java": "order/domain/PaymentOutbox.java",
    "repository/CommerceOrderRepository.java": "order/persistence/CommerceOrderRepository.java",
    "repository/IdempotencyRecordRepository.java": "order/persistence/IdempotencyRecordRepository.java",
    "repository/PaymentOutboxRepository.java": "order/persistence/PaymentOutboxRepository.java",
    "dto/CreateOrderRequest.java": "order/api/dto/CreateOrderRequest.java",
    "dto/CheckoutRequest.java": "order/api/dto/CheckoutRequest.java",
    "dto/OrderResponse.java": "order/api/dto/OrderResponse.java",
    "dto/OrderItemRequest.java": "order/api/dto/OrderItemRequest.java",
    "dto/OrderItemResponse.java": "order/api/dto/OrderItemResponse.java",
    "dto/ShippingAddressRequest.java": "order/api/dto/ShippingAddressRequest.java",
    "dto/ShippingAddressResponse.java": "order/api/dto/ShippingAddressResponse.java",
    # returns
    "controller/ReturnController.java": "returns/api/ReturnController.java",
    "service/ReturnService.java": "returns/application/ReturnService.java",
    "entity/ReturnRequest.java": "returns/domain/ReturnRequest.java",
    "repository/ReturnRequestRepository.java": "returns/persistence/ReturnRequestRepository.java",
    "dto/ReturnRequestBody.java": "returns/api/dto/ReturnRequestBody.java",
    "dto/ReturnResponse.java": "returns/api/dto/ReturnResponse.java",
    # integration
    "payment/PaymentPort.java": "integration/payment/PaymentPort.java",
    "payment/PaymentChargeRequest.java": "integration/payment/PaymentChargeRequest.java",
    "payment/PaymentRefundRequest.java": "integration/payment/PaymentRefundRequest.java",
    "payment/PaymentResult.java": "integration/payment/PaymentResult.java",
    "payment/PaymentOutcome.java": "integration/payment/PaymentOutcome.java",
    "payment/PaymentConfig.java": "integration/payment/PaymentConfig.java",
    "payment/WalletPaymentAdapter.java": "integration/payment/WalletPaymentAdapter.java",
    "payment/SimulatedPaymentAdapter.java": "integration/payment/SimulatedPaymentAdapter.java",
    "payment/PaymentSimulator.java": "integration/payment/PaymentSimulator.java",
    "event/CommerceEventPublisher.java": "integration/event/CommerceEventPublisher.java",
    "event/CommerceDomainEvent.java": "integration/event/CommerceDomainEvent.java",
    "event/CommerceEventType.java": "integration/event/CommerceEventType.java",
    "event/CommerceEvents.java": "integration/event/CommerceEvents.java",
    "event/KafkaCommerceEventPublisher.java": "integration/event/KafkaCommerceEventPublisher.java",
    "event/LoggingCommerceEventPublisher.java": "integration/event/LoggingCommerceEventPublisher.java",
    "event/KafkaEventConfig.java": "integration/event/KafkaEventConfig.java",
    # infrastructure + shared
    "config/ProductionFeaturesConfig.java": "infrastructure/config/ProductionFeaturesConfig.java",
    "config/TransactionConfig.java": "infrastructure/config/TransactionConfig.java",
    "job/CommerceMaintenanceJobs.java": "infrastructure/scheduling/CommerceMaintenanceJobs.java",
    "exception/ApiException.java": "shared/exception/ApiException.java",
    "exception/GlobalExceptionHandler.java": "shared/exception/GlobalExceptionHandler.java",
    "security/AuthenticatedUser.java": "shared/security/AuthenticatedUser.java",
    "security/JwtAuthFilter.java": "shared/security/JwtAuthFilter.java",
    "security/JwksKeyProvider.java": "shared/security/JwksKeyProvider.java",
    "security/SecurityConfig.java": "shared/security/SecurityConfig.java",
}

# Map old import prefixes to new (longest match first)
IMPORT_MAP: list[tuple[str, str]] = [
    ("com.finflow.commerce.catalogue.api.CartController", "com.finflow.commerce.cart.api.CartController"),
    ("com.finflow.commerce.catalogue.api.OrderController", "com.finflow.commerce.order.api.OrderController"),
    ("com.finflow.commerce.catalogue.api.CouponController", "com.finflow.commerce.coupon.api.CouponController"),
    ("com.finflow.commerce.catalogue.api.ReturnController", "com.finflow.commerce.returns.api.ReturnController"),
    ("com.finflow.commerce.dto.Product", "com.finflow.commerce.catalogue.api.dto.Product"),
    ("com.finflow.commerce.dto.Category", "com.finflow.commerce.catalogue.api.dto.Category"),
    ("com.finflow.commerce.dto.Cart", "com.finflow.commerce.cart.api.dto.Cart"),
    ("com.finflow.commerce.dto.Coupon", "com.finflow.commerce.coupon.api.dto.Coupon"),
    ("com.finflow.commerce.dto.Order", "com.finflow.commerce.order.api.dto.Order"),
    ("com.finflow.commerce.dto.Checkout", "com.finflow.commerce.order.api.dto.Checkout"),
    ("com.finflow.commerce.dto.CreateOrder", "com.finflow.commerce.order.api.dto.CreateOrder"),
    ("com.finflow.commerce.dto.ShippingAddress", "com.finflow.commerce.order.api.dto.ShippingAddress"),
    ("com.finflow.commerce.dto.UpdateCartItem", "com.finflow.commerce.cart.api.dto.UpdateCartItem"),
    ("com.finflow.commerce.dto.Return", "com.finflow.commerce.returns.api.dto.Return"),
    ("com.finflow.commerce.entity.Product", "com.finflow.commerce.catalogue.domain.Product"),
    ("com.finflow.commerce.entity.Category", "com.finflow.commerce.catalogue.domain.Category"),
    ("com.finflow.commerce.entity.CartItem", "com.finflow.commerce.cart.domain.CartItem"),
    ("com.finflow.commerce.entity.Cart", "com.finflow.commerce.cart.domain.Cart"),
    ("com.finflow.commerce.entity.Coupon", "com.finflow.commerce.coupon.domain.Coupon"),
    ("com.finflow.commerce.entity.CommerceOrder", "com.finflow.commerce.order.domain.CommerceOrder"),
    ("com.finflow.commerce.entity.OrderItem", "com.finflow.commerce.order.domain.OrderItem"),
    ("com.finflow.commerce.entity.OrderStatus", "com.finflow.commerce.order.domain.OrderStatus"),
    ("com.finflow.commerce.entity.ShippingAddress", "com.finflow.commerce.order.domain.ShippingAddress"),
    ("com.finflow.commerce.entity.IdempotencyRecord", "com.finflow.commerce.order.domain.IdempotencyRecord"),
    ("com.finflow.commerce.entity.PaymentOutbox", "com.finflow.commerce.order.domain.PaymentOutbox"),
    ("com.finflow.commerce.entity.ReturnRequest", "com.finflow.commerce.returns.domain.ReturnRequest"),
    ("com.finflow.commerce.repository.Product", "com.finflow.commerce.catalogue.persistence.Product"),
    ("com.finflow.commerce.repository.Category", "com.finflow.commerce.catalogue.persistence.Category"),
    ("com.finflow.commerce.repository.Cart", "com.finflow.commerce.cart.persistence.Cart"),
    ("com.finflow.commerce.repository.Coupon", "com.finflow.commerce.coupon.persistence.Coupon"),
    ("com.finflow.commerce.repository.CommerceOrder", "com.finflow.commerce.order.persistence.CommerceOrder"),
    ("com.finflow.commerce.repository.Idempotency", "com.finflow.commerce.order.persistence.Idempotency"),
    ("com.finflow.commerce.repository.PaymentOutbox", "com.finflow.commerce.order.persistence.PaymentOutbox"),
    ("com.finflow.commerce.repository.ReturnRequest", "com.finflow.commerce.returns.persistence.ReturnRequest"),
    ("com.finflow.commerce.service.CatalogueService", "com.finflow.commerce.catalogue.application.CatalogueService"),
    ("com.finflow.commerce.service.CartService", "com.finflow.commerce.cart.application.CartService"),
    ("com.finflow.commerce.service.AbandonedCartCleanupService", "com.finflow.commerce.cart.application.AbandonedCartCleanupService"),
    ("com.finflow.commerce.service.CommerceService", "com.finflow.commerce.order.application.CommerceService"),
    ("com.finflow.commerce.service.OrderPlacementService", "com.finflow.commerce.order.application.OrderPlacementService"),
    ("com.finflow.commerce.service.PaymentSettlementService", "com.finflow.commerce.order.application.PaymentSettlementService"),
    ("com.finflow.commerce.service.OrderCancelService", "com.finflow.commerce.order.application.OrderCancelService"),
    ("com.finflow.commerce.service.ReservationExpiryService", "com.finflow.commerce.order.application.ReservationExpiryService"),
    ("com.finflow.commerce.service.ReturnService", "com.finflow.commerce.returns.application.ReturnService"),
    ("com.finflow.commerce.controller.ProductController", "com.finflow.commerce.catalogue.api.ProductController"),
    ("com.finflow.commerce.controller.CategoryController", "com.finflow.commerce.catalogue.api.CategoryController"),
    ("com.finflow.commerce.controller.CartController", "com.finflow.commerce.cart.api.CartController"),
    ("com.finflow.commerce.controller.CouponController", "com.finflow.commerce.coupon.api.CouponController"),
    ("com.finflow.commerce.controller.OrderController", "com.finflow.commerce.order.api.OrderController"),
    ("com.finflow.commerce.controller.ReturnController", "com.finflow.commerce.returns.api.ReturnController"),
    ("com.finflow.commerce.mapper.ProductMapper", "com.finflow.commerce.catalogue.application.ProductMapper"),
    ("com.finflow.commerce.payment", "com.finflow.commerce.integration.payment"),
    ("com.finflow.commerce.event", "com.finflow.commerce.integration.event"),
    ("com.finflow.commerce.config", "com.finflow.commerce.infrastructure.config"),
    ("com.finflow.commerce.job", "com.finflow.commerce.infrastructure.scheduling"),
    ("com.finflow.commerce.exception", "com.finflow.commerce.shared.exception"),
    ("com.finflow.commerce.security", "com.finflow.commerce.shared.security"),
    ("com.finflow.commerce.dto.ProductRequest", "com.finflow.commerce.catalogue.api.dto.ProductRequest"),
    ("com.finflow.commerce.dto.ProductResponse", "com.finflow.commerce.catalogue.api.dto.ProductResponse"),
    ("com.finflow.commerce.dto.ProductUpdateRequest", "com.finflow.commerce.catalogue.api.dto.ProductUpdateRequest"),
    ("com.finflow.commerce.dto.CategoryRequest", "com.finflow.commerce.catalogue.api.dto.CategoryRequest"),
    ("com.finflow.commerce.dto.CategoryResponse", "com.finflow.commerce.catalogue.api.dto.CategoryResponse"),
    ("com.finflow.commerce.dto.CartResponse", "com.finflow.commerce.cart.api.dto.CartResponse"),
    ("com.finflow.commerce.dto.CartItemResponse", "com.finflow.commerce.cart.api.dto.CartItemResponse"),
    ("com.finflow.commerce.dto.CartItemRequest", "com.finflow.commerce.cart.api.dto.CartItemRequest"),
    ("com.finflow.commerce.dto.UpdateCartItemRequest", "com.finflow.commerce.cart.api.dto.UpdateCartItemRequest"),
    ("com.finflow.commerce.dto.CouponRequest", "com.finflow.commerce.coupon.api.dto.CouponRequest"),
    ("com.finflow.commerce.dto.CouponResponse", "com.finflow.commerce.coupon.api.dto.CouponResponse"),
    ("com.finflow.commerce.dto.CreateOrderRequest", "com.finflow.commerce.order.api.dto.CreateOrderRequest"),
    ("com.finflow.commerce.dto.CheckoutRequest", "com.finflow.commerce.order.api.dto.CheckoutRequest"),
    ("com.finflow.commerce.dto.OrderResponse", "com.finflow.commerce.order.api.dto.OrderResponse"),
    ("com.finflow.commerce.dto.OrderItemRequest", "com.finflow.commerce.order.api.dto.OrderItemRequest"),
    ("com.finflow.commerce.dto.OrderItemResponse", "com.finflow.commerce.order.api.dto.OrderItemResponse"),
    ("com.finflow.commerce.dto.ShippingAddressRequest", "com.finflow.commerce.order.api.dto.ShippingAddressRequest"),
    ("com.finflow.commerce.dto.ShippingAddressResponse", "com.finflow.commerce.order.api.dto.ShippingAddressResponse"),
    ("com.finflow.commerce.dto.ReturnRequestBody", "com.finflow.commerce.returns.api.dto.ReturnRequestBody"),
    ("com.finflow.commerce.dto.ReturnResponse", "com.finflow.commerce.returns.api.dto.ReturnResponse"),
]


def target_package(rel_java_path: str) -> str:
    parts = Path(rel_java_path).parent.as_posix().split("/")
    return "com.finflow.commerce." + ".".join(parts)


def rewrite_imports(content: str) -> str:
    for old, new in IMPORT_MAP:
        content = content.replace(old, new)
    return content


def process_file(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    text = rewrite_imports(text)
    path.write_text(text, encoding="utf-8")


def move_main_files() -> None:
    for old_rel, new_rel in FILE_MOVES.items():
        old = JAVA_SRC / old_rel.replace("/", "\\")
        if not old.exists():
            old = JAVA_SRC / old_rel
        new = JAVA_SRC / new_rel
        new.parent.mkdir(parents=True, exist_ok=True)
        content = old.read_text(encoding="utf-8")
        pkg = target_package(new_rel)
        content = re.sub(
            r"^package\s+[\w.]+;",
            f"package {pkg};",
            content,
            count=1,
            flags=re.MULTILINE,
        )
        content = rewrite_imports(content)
        new.write_text(content, encoding="utf-8")
        old.unlink()
        print(f"moved {old_rel} -> {new_rel}")


def cleanup_empty_dirs(base: Path) -> None:
    for dirpath in sorted(base.rglob("*"), reverse=True):
        if dirpath.is_dir() and not any(dirpath.iterdir()):
            dirpath.rmdir()


def rewrite_tree(base: Path) -> None:
    if not base.exists():
        return
    for path in base.rglob("*.java"):
        process_file(path)


TEST_MOVES: dict[str, str] = {
    "service/CatalogueServiceTest.java": "catalogue/application/CatalogueServiceTest.java",
    "service/ReservationExpiryServiceTest.java": "order/application/ReservationExpiryServiceTest.java",
    "payment/WalletPaymentAdapterTest.java": "integration/payment/WalletPaymentAdapterTest.java",
}


def move_test_files() -> None:
    for old_rel, new_rel in TEST_MOVES.items():
        old = TEST_SRC / old_rel
        if not old.exists():
            continue
        new = TEST_SRC / new_rel
        new.parent.mkdir(parents=True, exist_ok=True)
        content = old.read_text(encoding="utf-8")
        pkg = target_package(new_rel)
        content = re.sub(
            r"^package\s+[\w.]+;",
            f"package {pkg};",
            content,
            count=1,
            flags=re.MULTILINE,
        )
        content = rewrite_imports(content)
        new.write_text(content, encoding="utf-8")
        old.unlink()
        print(f"moved test {old_rel} -> {new_rel}")


def main() -> None:
    move_main_files()
    move_test_files()
    cleanup_empty_dirs(JAVA_SRC)
    cleanup_empty_dirs(TEST_SRC)
    rewrite_tree(JAVA_SRC)
    rewrite_tree(TEST_SRC)
    print("done")


if __name__ == "__main__":
    main()
