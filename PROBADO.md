# Qué está probado y qué no

Máquina de compilación: JDK 21.0.13 · Gradle 8.11.1 · Android SDK platform 35 ·
build-tools 35.0.0.

## 1.7.0 · estructura nueva

| Qué | Cómo | Resultado |
|---|---|---|
| Navegación de cinco destinos | `gradle :app:pruebasNucleo` | **12 comprobaciones**: ninguna sección de la 1.6.0 se perdió |
| Banner que no miente | idem | verde con root/Shizuku, ámbar sin permiso, **rojo sin vía**; nunca dice «mejor modo posible» |
| Avisos de Home | idem | sin problemas **no hay avisos** (lista vacía comprobada) |
| Pantalla Configurar | idem | 4 grupos, ids únicos, nada marcado de fábrica, toda fila no disponible explicada |
| Sin `LazyColumn` anidado | idem, sobre el fuente | vigila el fallo que cerraría la app al abrir Configurar |
| Iconos vectoriales | idem, sobre el fuente | se dibujan con `Path`/`Stroke`; no quedan glifos |
| Suite completa | `gradle :app:pruebasNucleo` | **267 comprobaciones, 0 fallos** |
| Lint | `gradle :app:lintDebug` | 0 errores |
| APK normal | `assembleRelease` | **3.176.147 B (3,03 MB)** |
| APK sin comprimir | `-PrebornDatos=crudo` | **16.226.163 B (15,48 MB)** |
| El código nuevo va dentro | `strings` sobre los `.dex` del APK | aparece el texto de Configurar |
| Firma | `apksigner verify --print-certs` | SHA-256 `1ff4c6ee…c6c9e803` (la misma) |

**Lo que no está probado en 1.7.0, y se dice:** la app **no se ha instalado en un teléfono
físico** desde esta máquina (no hay `/dev/kvm` ni dispositivo conectado). Las pantallas
nuevas compilan, pasan las pruebas de estructura y están dentro del APK, pero **el aspecto
final en el TECNO BG7 no lo he visto yo**: eso lo tiene que decir la primera instalación.

## Probado de verdad

| Qué | Cómo | Resultado |
|---|---|---|
| Compila | `gradle :app:assembleDebug` | OK |
| Compila release y firma | `./gradlew :app:assembleRelease` con clave propia | OK, APK 1.2.0 de **927.095 bytes** |
| Firma verificada | `apksigner verify --print-certs` | SHA-256 `baedd474…4647ffcca` |
| La clave de firma se perdió | comprobar huella de la clave actual | `1ff4c6ee…c6c9e803` — **distinta**. Ver `CLAVE.md` |
| La firma sale del entorno, no del archivo | compilar sin las variables `REBORN_*` | produce `app-release-unsigned.apk`: **no se firma sola** |
| **Cero permiso de INTERNET** | `aapt2 dump permissions` sobre el APK | 0 coincidencias |
| Núcleo completo (con utilidades) | `gradle :app:pruebasNucleo` | **70 comprobaciones, todas pasan** |
| Núcleo: catálogo | `gradle :app:pruebasNucleo` | **51 comprobaciones, todas pasan** (contadas: `grep -c 'check("' ` → 51) |
| Núcleo: escritura | idem, con ejecutor falso | escribe, omite vacíos, confirma |
| Núcleo: bloqueo del fabricante | idem | **no declara éxito si no cambió** |
| Núcleo: sin vía | idem | no aplica nada y lo dice |
| Núcleo: restaurar | idem | vuelve al valor original |
| La suite no da verde regalado | sabotear `ok = true` a propósito | la prueba falla; restaurado, verde |
| La regla del GPU es cierta | comprobar que todo `openglEs` es numérico | todos numéricos |
| **El filtro bloquea identidad en EXPERTO** | 8 claves (imei, serial, mac, meid…) | 8 de 8 bloqueadas |
| **El filtro bloquea arranque en EXPERTO** | 5 claves (persist.*, ro.boot.*, ro.secure…) | 5 de 5 bloqueadas |
| Rechaza inyección de shell | `;`, comillas, backticks, salto de línea | 4 de 4 rechazados |
| Límite real de 92 caracteres | valor de 92 vs 93 | 92 pasa, 93 no |
| Los dos modos difieren | `ro.build.version.sdk` | seguro NO, experto SÍ |
| Sabotaje: abrir el filtro de identidad | comentar la comprobación | **6 pruebas en rojo** |
| Sabotaje: abrir el filtro de arranque | idem | **5 pruebas en rojo** |
| Fichas TOP500 sin inventar | núcleos, Rmax, Rpeak, potencia | El Capitan 11.340.000 núcleos; Eagle sin potencia (no publicada) |
| El censo cuadra | verificados + parciales = total | cuadra |
| Compila con el wrapper | `./gradlew assembleRelease` | OK, misma huella |

## Compatibilidad (1.3.0)

| Qué | Cómo | Resultado |
|---|---|---|
| Mi código no llama APIs > Android 8 | Lint `NewApi` | **0** |
| Errores de Lint | `lintDebug` | **0 errores**, 7 avisos |
| Dependencias de Google Play | `gradle dependencies` | **ninguna** |
| Arquitecturas | `aapt2 dump badging` | arm64-v8a, armeabi-v7a, x86, x86_64 |
| Detección de Huawei/Honor | 6 pruebas de lógica pura | pasan |
| Detección de HarmonyOS | idem | pasan |
| La app se autoprueba en el equipo | pestaña **Compat.** | lanza `wm`, `pm`, `settings`, `cmd` de verdad |

**Fallo real encontrado por Lint:** `getInstalledApplications` no devuelve todas las apps
desde Android 11. Corregido con `QUERY_ALL_PACKAGES` + `<queries>`, y la app avisa si la
lista viene cortada. Detalle completo en [COMPATIBILIDAD-ANDROID.md](COMPATIBILIDAD-ANDROID.md).

**Sigue sin probarse:** instalar el APK en un teléfono real, un Huawei físico, y HarmonyOS
NEXT (donde ninguna APK de Android se instala, ni esta).

## Comprobado sobre el APK final (no sobre el código)

| Qué | Cómo | Resultado |
|---|---|---|
| Es un APK válido | `aapt2 dump badging` | `io.github.labandita.glreborn` v1.1.0 (code 2), minSdk 26, targetSdk 35 |
| Tiene activity lanzable | idem | `MainActivity` con etiqueta «GLTools Reborn» |
| El `ShizukuProvider` está en el manifest final | `aapt2 dump xmltree` | presente |
| La lógica de seguridad sobrevive al ofuscado | `strings` sobre `classes.dex` extraído | «identidad del», «propiedad de arranque», «ro.serialno», «persist.» → presentes |
| La etiqueta SIMULACIÓN llega al binario | idem | presente |
| Los perfiles viajan dentro | idem | «Google Pixel 8 Pro», «LineShine», `196610` → presentes |
| Compilación reproducible | borrar `build/` y recompilar | **927.095 bytes exactos**, idéntico al subido a Drive |

### Un error de método que cometí y corrijo aquí

Conté las cadenas con `grep -c` **sobre el APK comprimido** y salió 0 en todo. Concluí
«la lógica no sobrevivió al ofuscado», y era falso: `grep` sobre un binario comprimido no
ve dentro del `classes.dex`. Al extraer el DEX y usar `strings -a`, las cadenas estaban
todas. Queda escrito porque es exactamente el tipo de error que produce un «no funciona»
inventado.

## NO probado, y por qué

- **No se ha instalado ni arrancado en un teléfono.** Esta máquina no tiene `/dev/kvm`,
  así que no hay emulador posible. Decir «probado en el dispositivo» sería falso.
- **No se ha probado con Shizuku real.** No hay teléfono. La llamada usa la API pública
  `ShizukuSystemProperties.set/get`, cuya firma se comprobó leyendo el AAR 13.1.5 con
  `javap` —no de memoria—, pero el comportamiento en un teléfono concreto no se ha visto.
- **No se sabe si `setprop` de `ro.*` funciona en tu teléfono.** En Android moderno
  muchas `ro.*` son de sólo lectura. La app está escrita para **decirte que no** cuando
  no funcione (relee y compara), pero no puede garantizar que funcione.

Esa última línea es la más importante del documento: ninguna app puede prometer que
cambiar `ro.product.model` va a funcionar en todos los teléfonos, y la que lo prometa
está vendiendo algo que no controla.

---

# 1.4.0 — paneles por app, Liquid Glass y permisos

Fecha: 8 de noviembre de 2026. Lo que sigue son **mediciones sobre el APK de 1.4.0**,
no impresiones.

## Lo que sí está probado

| Comprobación | Resultado |
|---|---|
| `./gradlew :app:pruebasNucleo` | **87 comprobaciones**, todas pasan (8 nuevas del cuaderno) |
| `:app:lintDebug` | **0 errores**, 8 avisos (4 `GradleDependency`, `OldTargetApi`, `ModifierParameter`, `DataExtractionRules`, `ObsoleteSdkInt`) |
| `:app:assembleRelease` | APK firmado, **1.031.921 B** |
| `apksigner verify --print-certs` | OK · SHA-256 `1ff4c6ee…c9e803` (**la misma clave que 1.2.0 y 1.3.0**: actualiza encima) |
| `aapt2 dump badging` | `versionCode 5`, `1.4.0`, minSdk 26, target 35 |
| `aapt2 dump permissions` | **3 permisos**, los mismos que 1.3.0. Sigue **sin INTERNET** |
| `assets/panel/index.html` dentro del APK | sí, **8.893 B** |
| Panel funcionando en el APK | `WebViewAssetLoader` sustituido por R8 por `e1/a` y `e1/b`, y **`AssetManager.open` presente** en el DEX |
| Historial de lo aplicado | cadena `aplicado.json` presente en el DEX |

## Lo que NO está probado, y por qué

- **No se ha instalado ni arrancado en un teléfono.** Esta máquina no tiene `/dev/kvm`:
  no hay emulador y no puedo decir «probado en el dispositivo» sin mentir.
- El panel se ha verificado **compilado y empaquetado**, no pintado en pantalla. Lo que sí
  se comprueba en el teléfono del usuario, en cuanto abra una app: si el `WebView` no
  cargara, se vería el texto de respaldo de `PanelAssets` (fondo oscuro con un mensaje).

## Cómo comprobarlo tú en dos minutos

```bash
aapt2 dump permissions GLToolsReborn-1.4.0-release.apk   # 3 líneas, sin INTERNET
unzip -p GLToolsReborn-1.4.0-release.apk assets/panel/index.html | head -5
sha256sum GLToolsReborn-1.4.0-release.apk
```

Y en la app: **Útil → cualquier app → Panel**. Si es la primera vez, dirá «Todavía no se le
aplicó nada a esta app»; congela algo y vuelve a abrir el panel.

---

# 1.5.0 — la app ya abre, y aprende perfiles

Fecha: 8 de noviembre de 2026.

## El fallo que reportó el usuario: «no abre»

En un TECNO BG7 (Android 13, 4 GB) con Shizuku concedido, la 1.3.0 no llegaba a pintar.
**Causa, en el código, no una suposición:** `MainActivity.releer()` corría en el hilo de
interfaz y, por cada app instalada, abría un proceso de shell para preguntar si estaba
congelada (`GestorApps.estaCongelada` → `pm list packages -d`). Con ~100 apps y un tope
de 8 s por consulta, el arranque podía tardar minutos; Android mata por ANR antes. Además
se ejecutaban las 9 pruebas de compatibilidad seguidas y cualquier excepción cerraba la app.

| Antes (1.3.0) | Ahora (1.5.0) |
|---|---|
| 1 consulta de shell **por app**, en el hilo de UI | **1 consulta** en total (`pm list packages -d`) |
| Nada se pintaba hasta terminar | La pantalla se pinta **antes**, con esqueleto |
| Una excepción = app cerrada | Se convierte en resultado rojo con el mensaje real |
| Arranque en negro si algo falla | Pantalla de error con el mensaje, y la app sigue |

## Lo nuevo, y cómo se ha medido

| Comprobación | Resultado |
|---|---|
| `:app:pruebasNucleo` | **147 comprobaciones**, 0 fallos (60 nuevas: sonda, generador, buscador, extractor, distribuida, arranque) |
| `:app:lintDebug` | **0 errores**, 10 avisos |
| `:app:assembleRelease` | APK firmado, **1.198.617 B** |
| `apksigner verify` | OK · misma clave `1ff4c6ee…` (**actualiza encima de 1.3.0**) |
| `aapt2 dump permissions` | INTERNET, POST_NOTIFICATIONS, shizuku API_V23, DYNAMIC_RECEIVER… |
| `QUERY_ALL_PACKAGES` | **eliminado**, verificado con `aapt2` |
| `networkSecurityConfig` dentro del APK | `cleartextTrafficPermitted=false` y `<trust-anchors/>` **vacío**, comprobado con `aapt2 dump xmltree` |
| `assets/panel/index.html` | dentro, 8.893 B |

## Lo que NO está probado

- **No se ha instalado en el teléfono del usuario.** Sigue sin haber `/dev/kvm` aquí. Lo que
  sí está: la causa del ANR identificada en el código, y las tres cosas que la provocaban
  quitadas del hilo de interfaz.
- **La lectura real de `getprop` en un TECNO no se ha ejecutado aquí.** El intérprete de la
  salida y las reglas de generación sí están probados con una salida realista de ese mismo
  modelo (marca, modelo, device, OpenGL ES 196610, Android 13).
- La notificación de reversión no se ha visto en pantalla; sí está verificado que el canal
  se crea y que el permiso está declarado.

## Cómo comprobarlo en dos minutos

```bash
aapt2 dump permissions GLToolsReborn-1.5.0-release.apk     # 4 líneas, sin QUERY_ALL_PACKAGES
aapt2 dump xmltree --file res/e5.xml GLToolsReborn-1.5.0-release.apk   # red cerrada
```

En la app: si abre y se ve «Leyendo el teléfono…» antes que nada, el ANR está resuelto.
Después: **Aprender → Generar todos**, y debería aparecer tu TECNO con su GPU y su
OpenGL ES leídos del aparato.

---

# 1.6.0 — el NPE del arranque, datos reales y las funciones de los módulos

Fecha: 9 de noviembre de 2026.

## El fallo que reportó el usuario: `NullPointerException` al arrancar

Pantalla de error con el mensaje real (la red de seguridad de la 1.5.0 funcionó, eso sí).

**Causa, en el código:** en `Director`, `var modo: Modo = cargarModo()` se inicializaba
antes de que existiera `private val prefs`, que estaba declarado **dos líneas más abajo**.
Kotlin inicializa las propiedades en orden de declaración → `prefs` era `null` al llamar a
`getString` → el constructor reventaba y la app moría antes de pintar.

| Antes | Ahora |
|---|---|
| `modo` usaba `prefs` antes de existir | `prefs` se declara antes y con `by lazy` |
| una excepción en el constructor = app muerta | `cargarModo()` tiene su `try/catch` → arranca en SEGURO |
| nada lo detectaba | **prueba que lee `Director.kt`** y comprueba que `prefs` va antes que `modo` |

## Datos reales (no relleno)

| Bloque | Medido |
|---|---|
| TOP500 | **7.000 posiciones** (14 ediciones 2019-2025) + los **14 XML originales** |
| Google Play | **54.434 dispositivos** + el CSV original |
| mobolist.net | **1.386 fichas** con chipset y GPU (1.051 con GPU, 847 con versión de Android) |
| Wikipedia | 63 Snapdragon + 38 MediaTek + 78 GPUs Adreno |
| Peso de los datos | **14,33 MB** sin comprimir |

## Peso del APK, con las dos versiones medidas

| APK | Peso | Contenido |
|---|---|---|
| normal | **3.157.571 B (3,01 MB)** | el ZIP comprime los datos al 21% |
| `-sin-comprimir` | **16.256.459 B (15,50 MB)** | datos crudos: idénticos, se abren sin descomprimir |

No hay un byte de relleno en ninguno de los dos: `./gradlew :app:assembleRelease
-PrebornDatos=crudo` produce el segundo desde el mismo código.

## Lo que se ha comprobado

| Comprobación | Resultado |
|---|---|
| `:app:pruebasNucleo` | **228 comprobaciones**, 0 fallos |
| `:app:lintDebug` | 0 errores |
| `apksigner verify` | OK · misma clave `1ff4c6ee…` (actualiza encima de 1.5.0) |
| `versionCode` | 7 · `1.6.0` |
| Documentos dentro del APK | los 14 XML del TOP500 y el CSV de Google, verificados con `unzip -p` |
| Tu TECNO BG7 | hallado en el catálogo oficial de Google dentro del APK |

## Lo que NO está probado

- **Sigo sin poder instalarlo aquí**: no hay `/dev/kvm`. El NPE está identificado y
  corregido en el código, y ahora hay una prueba que lo vigila; pero el arranque en tu
  teléfono lo confirmas tú.
- Las acciones de *Rendi.* están probadas en cuanto a **qué comando generan, cómo
  comprueban y cómo revierten** (42 comprobaciones). No se han ejecutado en un teléfono
  real desde aquí. En tu TECNO, la que tiene más sentido probar primero es **AOT sobre una
  app concreta**: es la de riesgo bajo y su comprobación es un `grep -c` que dice la verdad.
