#!/usr/bin/env python3
"""Put all REST controllers in com.finflow.commerce.controller (flat)."""
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CTRL = ROOT / "src/main/java/com/finflow/commerce/controller"


def main() -> None:
    moved = []
    for f in list(CTRL.rglob("*.java")):
        if f.parent == CTRL:
            # already flat — fix package if needed
            text = f.read_text(encoding="utf-8")
            text = re.sub(
                r"^package\s+com\.finflow\.commerce\.controller\.\w+;",
                "package com.finflow.commerce.controller;",
                text,
                count=1,
                flags=re.M,
            )
            f.write_text(text, encoding="utf-8")
            continue
        name = f.name
        dest = CTRL / name
        text = f.read_text(encoding="utf-8")
        text = re.sub(
            r"^package\s+[\w.]+;",
            "package com.finflow.commerce.controller;",
            text,
            count=1,
            flags=re.M,
        )
        dest.write_text(text, encoding="utf-8")
        f.unlink()
        moved.append(name)
    for sub in sorted(CTRL.iterdir(), reverse=True):
        if sub.is_dir() and not any(sub.rglob("*")):
            shutil.rmtree(sub, ignore_errors=True)
        elif sub.is_dir():
            # remove leftover empty nested dirs
            for d in sorted(sub.rglob("*"), reverse=True):
                if d.is_dir() and not any(d.iterdir()):
                    d.rmdir()
            if sub.is_dir() and not any(sub.iterdir()):
                sub.rmdir()
    print("flattened:", ", ".join(sorted(set(moved))) or "(none)")


if __name__ == "__main__":
    main()
