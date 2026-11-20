# Conserva los métodos que llama la página (window.AndroidApp).
-keepclassmembers class mx.cvic.asistencia.MainActivity$Puente {
    @android.webkit.JavascriptInterface <methods>;
}
