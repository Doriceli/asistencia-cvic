package mx.cvic.asistencia

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.io.OutputStream

/**
 * La app es un WebView a pantalla completa con la página local assets/index.html
 * (tabla de asistencia, integrantes y generador de la plantilla para Samsung Notes).
 * Los datos se guardan dentro de la app (localStorage del WebView).
 * La página llama a [Puente] como window.AndroidApp para guardar PDF, PNG y respaldos en Descargas.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private var selectorArchivo: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // La pantalla no se apaga mientras se toma asistencia.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        web = WebView(this)
        setContentView(web)

        // Android 15+ dibuja detrás de las barras del sistema: se deja espacio para ellas y para el teclado.
        web.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val i = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.ime() or WindowInsets.Type.displayCutout()
                )
                v.setPadding(i.left, i.top, i.right, i.bottom)
            }
            insets
        }

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true // localStorage: aquí se guardan sesiones e integrantes
            allowFileAccess = true
            allowContentAccess = true
            textZoom = 100
            setSupportZoom(false)
        }

        web.addJavascriptInterface(Puente(), "AndroidApp")

        web.webViewClient = object : WebViewClient() {
            // Los enlaces externos se abren en el navegador; la página local se queda en la app.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.scheme == "file") return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                } catch (_: ActivityNotFoundException) {
                }
                return true
            }
        }

        // Selector de archivos para "Restaurar respaldo".
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                selectorArchivo?.onReceiveValue(null)
                selectorArchivo = filePathCallback
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
                return try {
                    @Suppress("DEPRECATION")
                    startActivityForResult(intent, PEDIR_ARCHIVO)
                    true
                } catch (_: ActivityNotFoundException) {
                    selectorArchivo = null
                    false
                }
            }
        }

        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("file:///android_asset/index.html")
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PEDIR_ARCHIVO) {
            val uri = data?.data
            selectorArchivo?.onReceiveValue(if (resultCode == RESULT_OK && uri != null) arrayOf(uri) else null)
            selectorArchivo = null
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    /** Botón Atrás: regresa entre pestañas antes de cerrar la app. */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (web.canGoBack()) {
            web.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /** Métodos que la página llama como window.AndroidApp. Responden "OK:<texto>" o "ERROR:<texto>". */
    inner class Puente {

        /** Recibe la plantilla como PNG (base64) y la guarda como PDF de una página. */
        @JavascriptInterface
        fun guardarPdf(nombre: String, pngBase64: String): String = try {
            val bytes = Base64.decode(pngBase64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return "ERROR:No se pudo leer la plantilla."
            // 2 píxeles por punto: la imagen conserva toda su resolución dentro del PDF.
            val ancho = bmp.width / 2
            val alto = bmp.height / 2
            val pdf = PdfDocument()
            val pagina = pdf.startPage(PdfDocument.PageInfo.Builder(ancho, alto, 1).create())
            pagina.canvas.drawBitmap(bmp, null, Rect(0, 0, ancho, alto), Paint(Paint.FILTER_BITMAP_FLAG))
            pdf.finishPage(pagina)
            val ok = escribirEnDescargas(nombre, "application/pdf") { pdf.writeTo(it) }
            pdf.close()
            bmp.recycle()
            if (ok) avisar("Guardado en Descargas › $CARPETA › $nombre") else "ERROR:No se pudo guardar el PDF."
        } catch (e: Exception) {
            "ERROR:No se pudo crear el PDF (${e.message})."
        }

        /** Guarda un archivo tal cual (PNG de la plantilla o respaldo JSON). */
        @JavascriptInterface
        fun guardarArchivo(nombre: String, mime: String, base64: String): String = try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            if (escribirEnDescargas(nombre, mime) { it.write(bytes) })
                avisar("Guardado en Descargas › $CARPETA › $nombre")
            else "ERROR:No se pudo guardar el archivo."
        } catch (e: Exception) {
            "ERROR:No se pudo guardar el archivo (${e.message})."
        }

        private fun avisar(texto: String): String {
            runOnUiThread { Toast.makeText(this@MainActivity, texto, Toast.LENGTH_LONG).show() }
            return "OK:$texto"
        }
    }

    /** Escribe en Download/Plantillas CVIC con MediaStore (Android 10+, sin permisos). */
    private fun escribirEnDescargas(nombre: String, mime: String, escribir: (OutputStream) -> Unit): Boolean {
        val valores = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + CARPETA)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores) ?: return false
        return try {
            contentResolver.openOutputStream(uri)?.use(escribir) ?: throw IllegalStateException("sin flujo")
            valores.clear()
            valores.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(uri, valores, null, null)
            true
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            false
        }
    }

    companion object {
        private const val PEDIR_ARCHIVO = 41
        const val CARPETA = "Plantillas CVIC"
    }
}
