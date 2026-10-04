# MealWise ProGuard / R8 rules — keep what reflection/serialization needs, shrink the rest.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-dontwarn kotlinx.**
# Moshi codegen: keep generated adapters + annotated models
-keep class `in`.mealwise.** { *; }
-keepclasseswithmembernames class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.squareup.moshi.** { *; }
# Retrofit / OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
