# Keep all app classes (data classes used with Gson need field reflection)
-keep class com.lipton.vpn.** { *; }

# xray/libv2ray native bindings
-keep class libv2ray.** { *; }
-keep class go.** { *; }

# Gson — keep fields used for JSON parsing (vmess config)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-dontnote com.google.gson.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# DataStore
-keep class androidx.datastore.** { *; }

# Compose — keep lambda/function types used in lambdas
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep debug info for crash reports
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
