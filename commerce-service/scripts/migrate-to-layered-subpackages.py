#!/usr/bin/env python3
"""Layered layout: controller/service/entity/repository/dto + feature subpackage."""
from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "src" / "main" / "java" / "com" / "finflow" / "commerce"
TEST = ROOT / "src" / "test" / "java" / "com" / "finflow" / "commerce"

FEATURE = ("catalogue", "cart", "coupon", "order", "returns")

def move_feature(feat: str) -> int:
  n = 0
  n += move_tree(f"{feat}/api/dto", f"dto/{feat}")
  api = BASE / feat / "api"
  if api.is_dir():
      for f in api.glob("*.java"):
          n += write_moved(f, BASE / f"controller/{feat}" / f.name)
  n += move_tree(f"{feat}/application", f"service/{feat}")
  n += move_tree(f"{feat}/domain", f"entity/{feat}")
  n += move_tree(f"{feat}/persistence", f"repository/{feat}")
  return n


def write_moved(src: Path, dest: Path) -> int:
    dest.parent.mkdir(parents=True, exist_ok=True)
    text = src.read_text(encoding="utf-8")
    pkg = pkg_for(dest.relative_to(BASE).as_posix())
    text = re.sub(r"^package\s+[\w.]+;", f"package {pkg};", text, count=1, flags=re.M)
    dest.write_text(rewrite(text), encoding="utf-8")
    return 1

# Flatten: move all java files from src glob to dest preserving filename
IMPORT_REPLACEMENTS: list[tuple[str, str]] = [
    ("com.finflow.commerce.catalogue.api.dto.", "com.finflow.commerce.dto.catalogue."),
    ("com.finflow.commerce.cart.api.dto.", "com.finflow.commerce.dto.cart."),
    ("com.finflow.commerce.coupon.api.dto.", "com.finflow.commerce.dto.coupon."),
    ("com.finflow.commerce.order.api.dto.", "com.finflow.commerce.dto.order."),
    ("com.finflow.commerce.returns.api.dto.", "com.finflow.commerce.dto.returns."),
    ("com.finflow.commerce.catalogue.api.", "com.finflow.commerce.controller.catalogue."),
    ("com.finflow.commerce.cart.api.", "com.finflow.commerce.controller.cart."),
    ("com.finflow.commerce.coupon.api.", "com.finflow.commerce.controller.coupon."),
    ("com.finflow.commerce.order.api.", "com.finflow.commerce.controller.order."),
    ("com.finflow.commerce.returns.api.", "com.finflow.commerce.controller.returns."),
    ("com.finflow.commerce.catalogue.application.", "com.finflow.commerce.service.catalogue."),
    ("com.finflow.commerce.cart.application.", "com.finflow.commerce.service.cart."),
    ("com.finflow.commerce.coupon.application.", "com.finflow.commerce.service.coupon."),
    ("com.finflow.commerce.order.application.", "com.finflow.commerce.service.order."),
    ("com.finflow.commerce.returns.application.", "com.finflow.commerce.service.returns."),
    ("com.finflow.commerce.catalogue.domain.", "com.finflow.commerce.entity.catalogue."),
    ("com.finflow.commerce.cart.domain.", "com.finflow.commerce.entity.cart."),
    ("com.finflow.commerce.coupon.domain.", "com.finflow.commerce.entity.coupon."),
    ("com.finflow.commerce.order.domain.", "com.finflow.commerce.entity.order."),
    ("com.finflow.commerce.returns.domain.", "com.finflow.commerce.entity.returns."),
    ("com.finflow.commerce.catalogue.persistence.", "com.finflow.commerce.repository.catalogue."),
    ("com.finflow.commerce.cart.persistence.", "com.finflow.commerce.repository.cart."),
    ("com.finflow.commerce.coupon.persistence.", "com.finflow.commerce.repository.coupon."),
    ("com.finflow.commerce.order.persistence.", "com.finflow.commerce.repository.order."),
    ("com.finflow.commerce.returns.persistence.", "com.finflow.commerce.repository.returns."),
    # legacy flat packages (duplicate tree)
    ("com.finflow.commerce.controller.", "com.finflow.commerce.controller."),
    ("com.finflow.commerce.service.", "com.finflow.commerce.service."),
    ("com.finflow.commerce.entity.", "com.finflow.commerce.entity."),
    ("com.finflow.commerce.repository.", "com.finflow.commerce.repository."),
    ("com.finflow.commerce.dto.", "com.finflow.commerce.dto."),
]

# order-specific cross-feature (after prefix passes)
EXTRA: list[tuple[str, str]] = [
    ("com.finflow.commerce.service.order.CommerceService", "com.finflow.commerce.service.order.CommerceService"),
]


def rewrite(content: str) -> str:
    rules = sorted(IMPORT_REPLACEMENTS[:-5], key=lambda x: len(x[0]), reverse=True)
    for old, new in rules:
        content = content.replace(old, new)
    return content


def pkg_for(rel: str) -> str:
    return "com.finflow.commerce." + Path(rel).parent.as_posix().replace("/", ".")


def move_tree(src_rel: str, dest_rel: str) -> int:
    src = BASE / src_rel.replace("/", "\\")
    if not src.exists():
        src = BASE / src_rel
    if not src.exists():
        return 0
    n = 0
    for f in src.rglob("*.java"):
        rel = f.relative_to(src)
        dest = BASE / dest_rel / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        text = f.read_text(encoding="utf-8")
        pkg = pkg_for(f"{dest_rel}/{rel.as_posix()}")
        text = re.sub(r"^package\s+[\w.]+;", f"package {pkg};", text, count=1, flags=re.M)
        text = rewrite(text)
        dest.write_text(text, encoding="utf-8")
        n += 1
    return n


def remove_feature_roots() -> None:
    for feat in FEATURE:
        p = BASE / feat
        if p.is_dir():
            shutil.rmtree(p, ignore_errors=True)


def purge_legacy_flat() -> None:
    """Remove duplicate top-level layer files (no subpackage)."""
    for layer in ("controller", "service", "entity", "dto", "repository"):
        d = BASE / layer
        if not d.is_dir():
            continue
        for f in d.glob("*.java"):
            f.unlink()


def rewrite_all_java(root: Path) -> None:
    if not root.exists():
        return
    for f in root.rglob("*.java"):
        f.write_text(rewrite(f.read_text(encoding="utf-8")), encoding="utf-8")


def fix_test_packages() -> None:
    mappings = [
        ("catalogue/application/CatalogueServiceTest.java", "service/catalogue/CatalogueServiceTest.java"),
        ("order/application/ReservationExpiryServiceTest.java", "service/order/ReservationExpiryServiceTest.java"),
        ("integration/payment/WalletPaymentAdapterTest.java", "integration/payment/WalletPaymentAdapterTest.java"),
    ]
    for old, new in mappings:
        o = TEST / old
        if not o.exists():
            continue
        dest = TEST / new
        dest.parent.mkdir(parents=True, exist_ok=True)
        text = o.read_text(encoding="utf-8")
        text = re.sub(
            r"^package\s+[\w.]+;",
            f"package {pkg_for(new)};",
            text,
            count=1,
            flags=re.M,
        )
        text = rewrite(text)
        dest.write_text(text, encoding="utf-8")
        o.unlink()


def main() -> None:
    total = 0
    for feat in FEATURE:
        c = move_feature(feat)
        if c:
            print(f"feature {feat}: {c} files")
            total += c
    remove_feature_roots()
    purge_legacy_flat()
    rewrite_all_java(BASE)
    rewrite_all_java(TEST)
    fix_test_packages()
    print("done, moved", total)


if __name__ == "__main__":
    main()
