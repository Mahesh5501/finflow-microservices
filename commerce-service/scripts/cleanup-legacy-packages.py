#!/usr/bin/env python3
"""Remove pre-migration layer packages after feature-package move."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LEGACY_DIRS = [
    ROOT / "src/main/java/com/finflow/commerce/controller",
    ROOT / "src/main/java/com/finflow/commerce/service",
    ROOT / "src/main/java/com/finflow/commerce/entity",
    ROOT / "src/main/java/com/finflow/commerce/dto",
    ROOT / "src/main/java/com/finflow/commerce/repository",
    ROOT / "src/main/java/com/finflow/commerce/payment",
    ROOT / "src/main/java/com/finflow/commerce/event",
    ROOT / "src/main/java/com/finflow/commerce/config",
    ROOT / "src/main/java/com/finflow/commerce/security",
    ROOT / "src/main/java/com/finflow/commerce/exception",
    ROOT / "src/main/java/com/finflow/commerce/job",
    ROOT / "src/main/java/com/finflow/commerce/mapper",
    ROOT / "src/test/java/com/finflow/commerce/service",
    ROOT / "src/test/java/com/finflow/commerce/payment",
]

def main() -> None:
    removed = 0
    for base in LEGACY_DIRS:
        if not base.is_dir():
            continue
        for f in list(base.rglob("*.java")):
            f.unlink()
            removed += 1
            print("deleted", f.relative_to(ROOT))
        for d in sorted(base.rglob("*"), reverse=True):
            if d.is_dir() and not any(d.iterdir()):
                try:
                    d.rmdir()
                except OSError:
                    pass
        if base.is_dir() and not any(base.iterdir()):
            try:
                base.rmdir()
                print("removed dir", base.relative_to(ROOT))
            except OSError:
                pass
    print("total files removed:", removed)

if __name__ == "__main__":
    main()
