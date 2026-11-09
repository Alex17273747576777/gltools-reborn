# Qué está probado y qué no

Máquina de compilación: JDK 21.0.13 · Gradle 8.11.1 · Android SDK platform 35 ·
build-tools 35.0.0.

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
