# Firestore maps these DTOs by their property names.
-keep class com.example.kotlinapp.data.model.** { *; }
-keepattributes Signature,*Annotation*
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
