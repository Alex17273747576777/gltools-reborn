# Permisos de GLTools Reborn

Revisión hecha sobre el APK de la versión 1.3.0 publicado, con
`aapt2 dump permissions`. Son **tres líneas**, y ninguna concede acceso a datos
personales. Aquí está cada una con su origen y su motivo.

## Los tres

| Permiso | Quién lo declara | Para qué |
|---|---|---|
| `moe.shizuku.manager.permission.API_V23` | **Shizuku / OmniShizuku** | Ejecutar órdenes de shell con los permisos de `adb shell`. Sin Shizuku concediéndolo, la app no puede cambiar nada. |
| `android.permission.QUERY_ALL_PACKAGES` | **el sistema** (Android 11+) | Ver la lista completa de apps instaladas, incluidas las que no tienen icono. Sin esto, congelar algunas apps falla en silencio. |
| `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | **AndroidX**, la añade ella sola | Que sus receptores internos (`androidx.startup`) no los pueda invocar otra app. Es interna: no da acceso a nada. |

## Lo que **no** pide

`INTERNET` · `ACCESS_NETWORK_STATE` · ubicación · cámara · micrófono · contactos ·
calendario · SMS · teléfono · almacenamiento (`READ/WRITE_*`) · `READ_PHONE_STATE` ·
`GET_ACCOUNTS` · accesibilidad · administrador del dispositivo · `SYSTEM_ALERT_WINDOW`.

Se puede comprobar sin creerme:

```bash
aapt2 dump permissions GLToolsReborn-1.4.0-release.apk
# o, en el teléfono:
adb shell dumpsys package io.github.labandita.glreborn | grep -A20 "requested permissions"
```

## Por qué no hay INTERNET

Porque no hace falta, y no tenerlo es una garantía comprobable:

- Los perfiles viajan **dentro del APK**.
- El panel de cada app se sirve desde los assets con `WebViewAssetLoader`, en
  `https://appassets.androidplatform.net`, que no es una dirección de red: el `WebView`
  lo intercepta antes de intentar conectarse.
- Las fichas del TOP500 están escritas en el código, tomadas de la lista de junio de 2026.

Resultado: **funciona en modo avión**, y no existe un sitio al que enviar nada aunque
quisiera. Un `WebView` sin `INTERNET` no puede cargar ni un píxel de fuera.

## Permisos que sí existen pero no aparecen en esa lista

El acceso a otras apps para congelarlas **no** es un permiso de Android: es la shell de
Shizuku. Por eso la app no pide `WRITE_SECURE_SETTINGS` ni nada parecido; ejecuta
`pm disable-user` y compañía como lo haría `adb`.

Lo que esta app **no** puede hacer nunca, ni con todo concedido, está listado a la vista
en la pestaña *Diagnóstico* y explicado en [SEGURIDAD.md](SEGURIDAD.md): IMEI, número de
serie, MAC, Android ID, `persist.*`, `ro.secure`.
