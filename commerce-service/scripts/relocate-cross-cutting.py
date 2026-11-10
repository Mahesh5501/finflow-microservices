#!/usr/bin/env python3
"""Move shared/* and infrastructure/* to root-layer packages and fix imports."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "java" / "com" / "finflow" / "commerce"

REPLACEMENTS = [
    ("com.finflow.commerce.shared.exception", "com.finflow.commerce.exception"),
    ("com.finflow.commerce.shared.api", "com.finflow.commerce.api"),
    ("com.finflow.commerce.shared.security", "com.finflow.commerce.security"),
    ("com.finflow.commerce.shared.web.SecuritySupport", "com.finflow.commerce.security.SecuritySupport"),
    ("com.finflow.commerce.shared.web.TraceIdFilter", "com.finflow.commerce.config.TraceIdFilter"),
    ("com.finflow.commerce.infrastructure.config", "com.finflow.commerce.config"),
    ("com.finflow.commerce.infrastructure.health", "com.finflow.commerce.health"),
    ("com.finflow.commerce.infrastructure.scheduling", "com.finflow.commerce.scheduler"),
]

MOVES: list[tuple[str, str, str]] = [
    ("shared/api/ErrorResponse.java", "api/ErrorResponse.java", "com.finflow.commerce.api"),
    ("shared/exception/ApiException.java", "exception/ApiException.java", "com.finflow.commerce.exception"),
    ("shared/exception/GlobalExceptionHandler.java", "exception/GlobalExceptionHandler.java", "com.finflow.commerce.exception"),
    ("shared/security/AuthenticatedUser.java", "security/AuthenticatedUser.java", "com.finflow.commerce.security"),
    ("shared/security/JwtAuthFilter.java", "security/JwtAuthFilter.java", "com.finflow.commerce.security"),
    ("shared/security/JwksKeyProvider.java", "security/JwksKeyProvider.java", "com.finflow.commerce.security"),
    ("shared/security/SecurityConfig.java", "security/SecurityConfig.java", "com.finflow.commerce.security"),
    ("shared/web/SecuritySupport.java", "security/SecuritySupport.java", "com.finflow.commerce.security"),
    ("shared/web/TraceIdFilter.java", "config/TraceIdFilter.java", "com.finflow.commerce.config"),
    ("infrastructure/config/TransactionConfig.java", "config/TransactionConfig.java", "com.finflow.commerce.config"),
    ("infrastructure/config/ProductionFeaturesConfig.java", "config/ProductionFeaturesConfig.java", "com.finflow.commerce.config"),
    ("infrastructure/health/DependencyHealth.java", "health/DependencyHealth.java", "com.finflow.commerce.health"),
    ("infrastructure/health/CommerceDependencyHealthService.java", "health/CommerceDependencyHealthService.java", "com.finflow.commerce.health"),
    ("infrastructure/health/CommerceDependencyHealthController.java", "health/CommerceDependencyHealthController.java", "com.finflow.commerce.health"),
    (
        "infrastructure/scheduling/CommerceMaintenanceJobs.java",
        "scheduler/CommerceMaintenanceJobs.java",
        "com.finflow.commerce.scheduler",
    ),
]

LEGACY_DELETE = [
    "security/SecurityConfig.java",
    "security/JwksKeyProvider.java",
    "security/AuthenticatedUser.java",
    "security/JwtAuthFilter.java",
    "config/TransactionConfig.java",
    "config/ProductionFeaturesConfig.java",
]


def rewrite_package(content: str, pkg: str) -> str:
    stripped = content.lstrip("\ufeff")
    if re.match(r"package\s+[\w.]+\s*;", stripped):
        return re.sub(r"package\s+[\w.]+\s*;", f"package {pkg};", stripped, count=1)
    return f"package {pkg};\n\n{stripped}"


def main() -> None:
    for rel_src, rel_dst, pkg in MOVES:
        src = ROOT / rel_src.replace("/", "\\") if False else ROOT / Path(rel_src)
        dst = ROOT / Path(rel_dst)
        if not src.is_file():
            print(f"SKIP missing: {rel_src}")
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        text = src.read_text(encoding="utf-8")
        dst.write_text(rewrite_package(text, pkg), encoding="utf-8")
        print(f"Wrote {rel_dst}")

    src_roots = [
        ROOT.parents[3] / "src" if False else Path(__file__).resolve().parents[1] / "src",
    ]
    src_root = Path(__file__).resolve().parents[1] / "src"
    for path in src_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        original = text
        for old, new in REPLACEMENTS:
            text = text.replace(old, new)
        if text != original:
            path.write_text(text, encoding="utf-8")

    for rel in MOVES:
        p = ROOT / Path(rel[0])
        if p.is_file():
            p.unlink()
    for rel in LEGACY_DELETE:
        p = ROOT / Path(rel)
        if p.is_file():
            p.unlink()
            print(f"Deleted legacy {rel}")

    for empty_dir in sorted(ROOT.rglob("*"), reverse=True):
        if empty_dir.is_dir() and not any(empty_dir.iterdir()):
            empty_dir.rmdir()
            print(f"Removed empty {empty_dir.relative_to(ROOT)}")

    print("Done.")


if __name__ == "__main__":
    main()
