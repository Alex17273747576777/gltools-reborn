# Permisos de GLTools Reborn

Revisión hecha sobre el APK publicado con `aapt2 dump permissions`
(**vuelve a hacerlo tú**: cambia entre versiones y esta tabla puede quedarse vieja).

## Los permisos de la 1.5.0

| Permiso | Quién lo declara | Para qué | ¿Se puede negar? |
|---|---|---|---|
| `moe.shizuku.manager.permission.API_V23` | **Shizuku / OmniShizuku** | Ejecutar órdenes de shell con los permisos de `adb shell`. Sin él, la app no puede cambiar nada. | Sí, y entonces la app dice **SIN VÍA** en rojo |
| `android.permission.POST_NOTIFICATIONS` | **Android 13+** | Un solo aviso: cuando un cambio de resolución se va a deshacer solo en 15 s, para poder confirmarlo sin abrir la app. | Sí: la app funciona igual, sólo pierdes ese aviso |
| `android.permission.INTERNET` | el sistema | **Sólo** para la búsqueda opcional de la sección *Aprender*. Ver abajo por qué no cambia nada. | — (no es concesión del usuario) |
| `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | **AndroidX**, sola | Que sus receptores internos no los pueda invocar otra app. Interna. | — |

### Lo que se ha quitado: `QUERY_ALL_PACKAGES`

Hasta la 1.4.0 estaba, para ver todas las apps instaladas. Ese permiso es de los que
Google revisa a mano y da acceso a la lista **completa** de aplicaciones del teléfono.
A partir de la 1.5.0 la app se conforma con `<queries>`: ve las apps que tienen icono de
lanzador y las que responden a `VIEW`. **Consecuencia honesta:** las apps que corren en
segundo plano sin icono pueden no aparecer en la lista de congelar. Está escrito en la
pantalla *Diagnóstico*, no escondido en un pie.

### Por qué INTERNET no rompe la promesa de «funciona sin red»

Tres cierres, y se pueden comprobar:

1. **`res/xml/red_segura.xml`**: `networkSecurityConfig` sin un solo dominio de confianza
   y con `cleartextTrafficPermitted="false"`. El sistema no puede abrir una conexión
   TLS a nada, porque no hay nada en quien confiar.
2. **Nada depende de la red**: perfiles, panel por app, lectura del teléfono y generación
   de perfiles funcionan con el teléfono en modo avión.
3. **El permiso se puede quitar**: borrando una línea del manifiesto, la sección
   *Aprender* baja al catálogo local y todo lo demás sigue igual.

## Lo que **no** pide

`ACCESS_NETWORK_STATE` · ubicación · cámara · micrófono · contactos ·
calendario · SMS · teléfono · almacenamiento (`READ/WRITE_*`) · `READ_PHONE_STATE` ·
`GET_ACCOUNTS` · accesibilidad · administrador del dispositivo · `SYSTEM_ALERT_WINDOW`.

Se puede comprobar sin creerme:

```bash
aapt2 dump permissions GLToolsReborn-1.4.0-release.apk
# o, en el teléfono:
adb shell dumpsys package io.github.labandita.glreborn | grep -A20 "requested permissions"
```

## Lo que viaja por la red: nada, hoy

La sección *Aprender* **no** hace peticiones por ahora: buscador, extractor y generador
son locales. El permiso existe para poder añadir más adelante una búsqueda de datos
opcional sin volver a tocar los permisos. Si algún día se añade, dirá de dónde saca cada
dato; lo que no va a pasar es que la app mande nada tuyo a ningún sitio.

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
