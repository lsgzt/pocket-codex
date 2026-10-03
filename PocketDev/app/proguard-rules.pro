# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Retrofit models
-keep class com.pocketdev.app.api.models.** { *; }
-keep class com.pocketdev.app.data.models.** { *; }

# Keep Gson serializable classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Retrofit
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Retrofit needs parameter annotations + generic signatures to build calls
# (e.g. @Header/@Body/@HeaderMap params and Response<ChatResponse> types).
-keepattributes Exceptions, InnerClasses, EnclosingMethod, Signature,
    RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep interface com.pocketdev.app.api.service.ChatApiService { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Rhino (JavaScript Engine)
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**

# Keep Chaquopy (Python Engine)
-keep class com.chaquo.python.** { *; }
-dontwarn com.chaquo.python.**

# --- v1.1.1 fix: Chaquopy Python->Kotlin bridge callbacks -------------------
# Python invokes these Kotlin interfaces' call() method from native code via
# JNI reflection BY NAME. R8 obfuscated them in v1.1.0, which broke Python
# execution ("object is not callable" / NoSuchMethod). Keep them intact.
-keep interface com.pocketdev.app.execution.PythonEngine$InputCallback { *; }
-keep interface com.pocketdev.app.execution.PythonEngine$OutputCallback { *; }
-keep class * implements com.pocketdev.app.execution.PythonEngine$InputCallback { *; }
-keep class * implements com.pocketdev.app.execution.PythonEngine$OutputCallback { *; }

# Keep security crypto
-keep class androidx.security.crypto.** { *; }

# Remove debug logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Keep Sora editor + TextMate (accessed via reflection in EditorCoreView)
-keep class io.github.rosemoe.sora.** { *; }
-keep class org.eclipse.tm4e.** { *; }
-dontwarn io.github.rosemoe.sora.**
-dontwarn org.eclipse.tm4e.**

# OkHttp / OkIO
-dontwarn okhttp3.**
-dontwarn okio.**

# Java Diff Utils (used by DiffViewer)
-dontwarn com.github.difflib.**
