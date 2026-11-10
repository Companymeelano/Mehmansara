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

# ============================================================================
# Hilt / Dagger — the generated graph is rooted ONLY in generated classes that
# are not referenced from the manifest, so R8 (full mode) strips it and the app
# crashes instantly in Application.onCreate (un-injected lateinit fields).
# Observed in prod release 1.1.0: Hilt_MehraApplication / DaggerMehraApplication
# missing from classes.dex. These keeps are mandatory for minified builds.
# ============================================================================
-keep class dagger.hilt.** { *; }
-keep class androidx.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class **.Hilt_* { *; }
-keep class **.*_HiltModules* { *; }
-keep class **.*_HiltComponents* { *; }
-keep class **.Dagger* { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}
-keepclasseswithmembers class * {
    @dagger.hilt.DefineComponent <methods>;
}

# --- WorkManager -------------------------------------------------------------
# Workers are re-instantiated from their class name stored in the WorkManager
# database (reflection), and the app Application implements Configuration.Provider.
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.Worker { *; }

# --- Retrofit / OkHttp --------------------------------------------------------
# Service interfaces are invoked through java.lang.reflect.Proxy.
-keep interface ir.ahvaz.mehra.data.remote.api.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# --- Diagnostics --------------------------------------------------------------
# Keep line numbers so any future crash report is actionable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
