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

# Keep Jackson's internal feature enums intact
-keepclassmembers enum com.fasterxml.jackson.databind.** { *; }
-keepclassmembers enum com.fasterxml.jackson.core.** { *; }

# Keep Jackson annotations and general class structure
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.fasterxml.jackson.** *;
}
-dontwarn com.fasterxml.jackson.**

# Keep Google GenAI SDK types, builders, and methods
-keep class com.google.genai.** { *; }
-keepclassmembers class com.google.genai.** { *; }
-keepclassmembers class com.google.genai.types.**$Builder { *; }

# Keep all builder methods and inner classes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod