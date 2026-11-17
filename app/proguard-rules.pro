# Baseline ProGuard & R8 Optimization Rules for Vivaan Enterprise

# 1. Preserve essential metadata and annotations for reflection
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod,*Annotation*

# 2. Preserve remote DTO models used by Cloud Firestore reflective serialization/deserialization (toObject / toObjects)
-keep class com.vivaanenterprise.app.data.remote.model.** {
    *;
}
-keepclassmembers class * {
    @com.google.firebase.firestore.* <fields>;
    @com.google.firebase.firestore.* <methods>;
}

# 3. WorkManager & Hilt Worker reflection initialization
# WorkManager persists worker class names in internal SQLite and instantiates them reflectively by name
-keepnames class * extends androidx.work.ListenableWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
    @dagger.assisted.AssistedInject <init>(...);
}
-keep @androidx.hilt.work.HiltWorker class * {
    *;
}
-keep class com.vivaanenterprise.app.core.sync.** {
    *;
}

# 4. Room Database Entities and TypeConverters
-keep class * extends androidx.room.RoomDatabase
-keep class com.vivaanenterprise.app.core.database.entity.** {
    *;
}
-keep class com.vivaanenterprise.app.core.database.converter.** {
    *;
}

# 5. Preserve Enums used by Room TypeConverters and Firestore
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep enum com.vivaanenterprise.app.core.common.** {
    *;
}

# 6. Core DataStore Models
-keep class com.vivaanenterprise.app.core.datastore.** {
    *;
}
