# Proguard rules for SendChat Android release

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all project classes to prevent any runtime issues
-keep class com.bytegif.sendchat.** { *; }

# SQLDelight
-keep class app.cash.sqldelight.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }

# Compose rules are automatically provided by Compose AAR consumer rules.
# Allow R8 to strip unused material icons from material-icons-extended.
-dontwarn androidx.compose.**
