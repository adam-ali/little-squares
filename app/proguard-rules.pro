# Glance instantiates action callbacks by class name.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
# Stored data model (kotlinx.serialization).
-keep class com.littlesquares.app.data.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontwarn org.jetbrains.annotations.**
