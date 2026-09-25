# ali - release hardening
# Keep Android entry point and custom view constructors; R8 can still obfuscate internals.
-keep public class com.example.ali.MainActivity { public <init>(); }
-keep public class com.example.ali.LogoCanvas { public <init>(...); }

# Remove verbose logs/assert-like debug code where possible.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Don't keep parameter/local-variable metadata unnecessarily.
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,EnclosingMethod

# Obfuscate and optimize app code.
-dontwarn javax.annotation.**
