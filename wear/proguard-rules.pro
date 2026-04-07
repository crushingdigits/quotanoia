# ── Quotanoia wear ProGuard / R8 rules ───────────────────────────────

# Repackage obfuscated classes into a single flat package for smaller DEX
-repackageclasses

# ── Compose ──────────────────────────────────────────────────────────
# AGP / Compose compiler plugin already emits the necessary keep rules.
# If you encounter issues, uncomment:
# -keep class androidx.compose.** { *; }

# ── Wear Tiles / ProtoLayout ─────────────────────────────────────────
# Keep tile service entry points used by the system
-keep class * extends androidx.wear.tiles.TileService { *; }
