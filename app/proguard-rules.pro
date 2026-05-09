# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keepclassmembers @androidx.compose.runtime.Composable class * {
    <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-dontwarn androidx.compose.**

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin serialization / reflection
-keepattributes *Annotation*
-keepattributes InnerClasses
-keep class kotlin.Metadata { *; }

# Material Icons (prevent stripping of icon resources)
-keep class androidx.compose.material.icons.** { *; }
