package io.github.labandita.glreborn.ui

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import org.json.JSONArray
import org.json.JSONObject
import io.github.labandita.glreborn.core.Aplicado
import io.github.labandita.glreborn.core.Ficha
import io.github.labandita.glreborn.core.Hecho
import io.github.labandita.glreborn.core.Inventario

/**
 * El panel de cada app, en WebView.
 *
 * Se sirve por `WebViewAssetLoader` bajo https://appassets.androidplatform.net, que
 * lee de los assets del APK. Eso permite que el HTML use rutas normales y a la vez
 * **no exista ninguna conexión de red**: por eso la app sigue sin permiso de INTERNET,
 * y el panel funciona en modo avión.
 *
 * Javascript está activado porque el panel lo necesita para pintar. Lo que NO está:
 * acceso a archivos locales (`allowFileAccess`), contenido mixto, ni geolocalización.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PanelApp(
    context: Context,
    ficha: Ficha?,
    app: Inventario.App?,
    telefono: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    val html = recuerdaHtml(context, ficha, app, telefono)

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val cargador = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()

            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                settings.setSupportZoom(false)
                setBackgroundColor(0xFF12081F.toInt())
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? = cargador.shouldInterceptRequest(request.url)
                }
                loadDataWithBaseURL(
                    "https://appassets.androidplatform.net/assets/panel/",
                    html,
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
        update = { vista ->
            vista.loadDataWithBaseURL(
                "https://appassets.androidplatform.net/assets/panel/",
                html,
                "text/html",
                "UTF-8",
                null,
            )
        },
    )
}

/** Construye el HTML con los datos inyectados. Nada se inventa: si falta, va vacío. */
private fun recuerdaHtml(
    context: Context,
    ficha: Ficha?,
    app: Inventario.App?,
    telefono: Map<String, String>,
): String {
    val plantilla = PanelAssets.html(context)
    val paquete = ficha?.paquete ?: app?.paquete ?: ""
    val nombre = app?.nombre ?: ficha?.nombre ?: paquete

    val hechos = JSONArray()
    ficha?.hechos?.forEach { h ->
        hechos.put(JSONObject().apply {
            put("cuando", h.cuando)
            put("accion", h.accion)
            put("detalle", h.detalle)
            put("reversible", h.reversible)
        })
    }
    val tel = JSONObject()
    telefono.forEach { (k, v) -> tel.put(k.replace("ro.", ""), v) }

    val permisos = JSONArray()
    @Suppress("DEPRECATION")
    val info = try {
        context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS,
        )
    } catch (t: Throwable) {
        null
    }
    info?.requestedPermissions?.filterNotNull()?.forEach { permiso ->
        permisos.put(JSONObject().apply {
            put("nombre", permiso.substringAfterLast('.'))
            put("para", explicarPermiso(permiso))
        })
    }

    val datos = JSONObject().apply {
        put("paquete", paquete)
        put("nombre", nombre)
        put("hechos", hechos)
        put("telefono", tel)
        put("permisos", permisos)
    }
    return plantilla.replace("window.DATOS || {}", datos.toString())
}

/**
 * Lee el HTML de los assets del APK. Se cachea: son 8 KB.
 * Se usa `AssetManager` y no el classloader porque el classloader puede variar entre
 * versiones de Android; los assets, no.
 */
object PanelAssets {
    @Volatile private var cache: String? = null

    fun html(context: Context): String {
        cache?.let { return it }
        return try {
            val texto = context.assets.open("panel/index.html")
                .bufferedReader().use { it.readText() }
            cache = texto
            texto
        } catch (t: Throwable) {
            "<html><body style='background:#12081F;color:#F4F1FF;font-family:sans-serif;padding:24px'>" +
                "No se pudo cargar el panel: ${t.message}</body></html>"
        }
    }
}

/**
 * Traduce un permiso a una frase que se entienda.
 *
 * Son los cuatro del APK; si apareciera otro, se dice que es de Android o de Shizuku
 * en vez de inventar para qué sirve.
 */
fun explicarPermiso(permiso: String): String = when {
    permiso.contains("shizuku") ->
        "Deja que GLTools pida permiso a Shizuku para ejecutar órdenes de shell. " +
            "Solo Shizuku puede concederlo; sin él la app no puede hacer nada."
    permiso.contains("QUERY_ALL_PACKAGES") ->
        "Ver la lista completa de apps instaladas, incluidas las que no tienen icono. " +
            "Sin esto, algunas apps no se podrían congelar."
    permiso.contains("DYNAMIC_RECEIVER_NOT_EXPORTED") ->
        "Lo añade AndroidX sola para que sus propios receptores internos no los pueda " +
            "llamar nadie de fuera. No da acceso a nada."
    permiso.contains("INTERNET") ->
        "Acceso a red. La app NO lo pide."
    else -> "Permiso de Android o de otra librería; la app no lo usa directamente."
}
