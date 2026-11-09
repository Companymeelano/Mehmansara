# ============================================================================
# MEHRA — R8 / ProGuard rules
# ============================================================================

# --- Gson / DTOs -------------------------------------------------------------
# DTOs are deserialized reflectively by field name; R8 must not rename or strip
# any field of the wire models, otherwise the API contract silently breaks.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class ir.ahvaz.mehra.data.remote.dto.** { *; }
-keep class ir.ahvaz.mehra.data.local.entity.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep generic signature of Call/Response (retrofit)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- OkHttp ------------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**

# --- Room --------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# --- Coroutines / Hilt -------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keep,allowobfuscation @interface dagger.hilt.**

# --- App specific ------------------------------------------------------------
# SignalR hub payloads are parsed with Gson as well.
-keep class ir.ahvaz.mehra.core.network.realtime.** { *; }
-keepnames class ir.ahvaz.mehra.**ViewModel { *; }

# --- errorprone annotations (transitive deps, annotations only) --------------
-dontwarn com.google.errorprone.annotations.**
