# Proguard rules for Flippy Walkie-Talkie
-keep class com.flippy.walkietalkie.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
