# Add project specific ProGuard rules here.

# Keep WebView JavaScript interface methods
-keepclassmembers class com.nightpos.geckoview.webview.* {
   public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void openFileChooser(...);
}

# Keep Compose runtime
-dontwarn org.jetbrains.annotations.**

# GeckoView pulls in SnakeYAML (geckoview-config.yaml parsing), whose
# reflection-based PropertyUtils references java.beans.* — not present on
# Android but only reached via code paths GeckoView doesn't exercise.
-dontwarn java.beans.**
-dontwarn org.yaml.snakeyaml.**
