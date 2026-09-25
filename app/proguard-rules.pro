# Keep Kotlin Multiplatform & Serialization metadata
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep WorkDay and data models
-keep class com.emeric.timecraft.model.** { *; }
-keepclassmembers class com.emeric.timecraft.model.** { *; }

# Ignore warnings for desktop/java.awt classes in Kotlin Multiplatform
-dontwarn java.awt.**
-dontwarn javax.swing.**
