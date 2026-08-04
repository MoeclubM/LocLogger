# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Compose
-dontwarn androidx.compose.**

# MapLibre
-dontwarn org.maplibre.**
-keep class org.maplibre.android.** { *; }
-keep class org.maplibre.geojson.** { *; }
-keep class org.maplibre.location.** { *; }
