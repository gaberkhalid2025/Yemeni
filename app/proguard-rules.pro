# Proguard rules for com.wam

# Keep Firebase models and serializable fields
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
