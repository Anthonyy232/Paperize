# Library consumer rules preserve their runtime entry points. Keep release crash locations.
-keepattributes SourceFile,LineNumberTable

# Strip debug and verbose logging from release builds.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
