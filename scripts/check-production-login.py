"""Ensure the shipped production bundle excludes the removable demo provider."""
from pathlib import Path
assets = Path(__file__).resolve().parent.parent / "frontend/dist"
files = list(assets.rglob("*.js"))
assert files, "Production build is missing"
for p in files:
    content = p.read_text()
    for marker in ("Preview-User-1", "Preview-User-2", "Preview-Etf-3", "Preview-Global-4"):
        assert marker not in content, "Demo login credential shipped in production bundle"
print("PASS: production bundle contains no demo login credentials")
