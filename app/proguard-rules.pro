# Add project specific ProGuard rules here.

# Keep WebView JavaScript interface methods
-keepclassmembers class com.nightpos.app.webview.* {
   public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void openFileChooser(...);
}

# Keep Compose runtime
-dontwarn org.jetbrains.annotations.**

# SnakeYAML references java.beans.* which is not available on Android.
# GeckoView bundles SnakeYAML for its config parser; the code path that
# uses java.beans.Introspector is never reached on Android, so suppressing
# the missing-class error is safe.
-dontwarn java.beans.**
