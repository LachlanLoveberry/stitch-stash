# Keep Room entities and DAOs
-keep class com.lachlan.stitchstash.data.db.entities.** { *; }
-keep class com.lachlan.stitchstash.data.db.dao.** { *; }

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
