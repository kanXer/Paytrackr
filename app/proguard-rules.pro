# ProGuard / R8 rules for PayTrackr

# Keep Kotlin Coroutines & Flow
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Data Models and Domain classes
-keep class com.example.paytrackr.data.** { *; }
-keepclassmembers class com.example.paytrackr.data.** { *; }
-keep class com.example.paytrackr.auth.BusinessProfile { *; }
-keepclassmembers class com.example.paytrackr.auth.BusinessProfile { *; }
-keep class com.example.paytrackr.util.ReminderItemBreakdown { *; }

# Keep Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Firebase & Google Play Services
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.** { *; }

# Compose & AndroidX
-dontwarn androidx.compose.**
