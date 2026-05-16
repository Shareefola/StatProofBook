# StatProof ProGuard Rules
# ─────────────────────────────────────────────────────────────────────────────

# Keep the application class
-keep class com.statproof.app.StatProofApplication { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.statproof.**$$serializer { *; }
-keepclassmembers class com.statproof.** {
    *** Companion;
}
-keepclasseswithmembers class com.statproof.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all serializable data classes
-keep @kotlinx.serialization.Serializable class * { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Fts4 class * { *; }
-dontwarn androidx.room.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Proof engine — keep AST sealed classes (reflection-free but used via when expressions)
-keep class com.statproof.proofengine.ast.** { *; }
-keep class com.statproof.proofengine.models.** { *; }
-keep class com.statproof.proofengine.rules.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# DataStore
-keep class androidx.datastore.** { *; }

# General Android
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# Remove all logging in release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
