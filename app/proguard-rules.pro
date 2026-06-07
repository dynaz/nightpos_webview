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
