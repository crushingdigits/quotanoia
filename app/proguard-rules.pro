# ── Quotanoia app ProGuard / R8 rules ────────────────────────────────

# Repackage obfuscated classes into a single flat package for smaller DEX
-repackageclasses

# ── OkHttp ───────────────────────────────────────────────────────────
# OkHttp uses reflection for platform detection
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# ── Compose ──────────────────────────────────────────────────────────
# AGP / Compose compiler plugin already emits the necessary keep rules.
# If you encounter issues, uncomment:
# -keep class androidx.compose.** { *; }
