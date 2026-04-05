# Poopilot ProGuard Rules

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Pleos Connect SDK
-keep class ai.pleos.playground.** { *; }
-dontwarn ai.pleos.playground.**

# Retrofit + Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.panicdev.poopilot.data.model.** { *; }
-keep class com.panicdev.poopilot.data.api.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# GleoAiReceiver (accessed via manifest)
-keep class com.panicdev.poopilot.data.receiver.GleoAiReceiver { *; }
