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
