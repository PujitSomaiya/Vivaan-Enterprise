# Baseline ProGuard & R8 Optimization Rules for Vivaan Enterprise

# Preserve metadata required by reflection-based serializers (Cloud Firestore)
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod

# Preserve remote DTO models used by Cloud Firestore reflective deserialization (toObject / toObjects)
-keep class com.vivaanenterprise.app.data.remote.model.** {
    <fields>;
    public <init>();
    <methods>;
}

# WorkManager Hilt Worker reflection initialization
-keep class * extends androidx.work.ListenableWorker {
    @dagger.assisted.AssistedInject <init>(...);
}
