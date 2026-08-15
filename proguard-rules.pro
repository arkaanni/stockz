# --- Android SDK Suppressions (Never present on Desktop JVM) ---
-dontwarn android.security.**
-dontwarn android.net.http.**

# --- OkHttp Optional Platform Suppressions ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Google Auth & Logging Suppressions ---
-dontwarn com.google.auth.oauth2.Slf4jLoggingHelpers
-dontwarn org.slf4j.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.http.**

# --- Guava MethodHandle & Reflection Suppressions ---
-dontwarn com.google.common.hash.ChecksumHashFunction$ChecksumMethodHandles
-dontwarn com.google.common.**