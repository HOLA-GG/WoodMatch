# Reglas de Ofuscación y Protección contra Ingeniería Inversa (R8 / Proguard)

# 1. Repaquetado y aplanamiento de nombres de clases para dificultar descompilación
-repackageclasses ''
-allowaccessmodification

# 2. Eliminar logs de depuración en compilaciones de producción
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 3. Modelos de datos para Firestore y JSON (conservar nombres de campos serializables)
-keep class com.example.kotlinapp.data.model.** { *; }
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses

# 4. Ofuscar repositorios y servicios de red protegiendo APIs
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 5. OpenStreetMap (osmdroid)
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# 6. Firebase y Google Play Services
-keepattributes *Annotation*,Signature
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
