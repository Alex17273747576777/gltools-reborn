# GLTools Reborn

Perfiles de dispositivo para Android **sin root obligatorio, sin servidores y sin nada de pago**.

Código abierto. Los perfiles viajan dentro del APK: en modo avión funciona igual.

---

## Qué es esto y qué no es

**Es** una app nueva, escrita desde cero, que cambia lo que el teléfono declara ser
(`ro.product.model`, `ro.product.brand`, `ro.product.device`, `ro.hardware`,
`ro.opengles.version`) para que los juegos y las apps vean el dispositivo que tú elijas.
Cambiar el modelo es útil para que un juego aplique un preset gráfico en vez de otro.

**No es** GLTools. No lleva el módulo Xposed de nadie, no desbloquea la app de pago de
terceros, no trae binarios ajenos. Ver [PROCEDENCIA.md](PROCEDENCIA.md) para el detalle
de qué se tomó como referencia del proyecto original y qué se descartó, con nombre y motivo.

## Con qué gestor funciona

**Shizuku (Rikka) y OmniShizuku, sin cambiar nada.** Comprobado: los dos declaran el
mismo permiso `moe.shizuku.manager.permission.API_V23`. Detalle y por qué Dhizuku no
sirve para esto: [COMPATIBILIDAD.md](COMPATIBILIDAD.md).

## Las tres vías, y la cuarta que es decir la verdad

| Vía | ¿Root? | Escribe propiedades |
|---|---|---|
| Shizuku activo + permiso | No | Sí |
| `su` disponible | Sí | Sí |
| Ninguna de las dos | — | **No, y lo dice** |

La cuarta fila es la importante. Sin Shizuku ni root, Android no deja escribir `ro.*` a una
app normal: no hay truco, no hay API oculta, no hay «modo compatibilidad» que lo evite. En
ese estado la app se abre, lee lo que puede, y muestra **SIN VÍA** en rojo arriba. Los
botones de aplicar existen pero responden «No se aplicó nada». Una app que dijera
«aplicado» ahí estaría mintiendo.

## Modo seguro y modo experto

Hay un interruptor arriba, siempre visible.

- **Seguro** (por defecto): sólo modelo, marca, placa, hardware y OpenGL ES. Si algo no
  cuadra al releer, **se deshace solo** y te dice que el teléfono está como estaba.
- **Experto**: añade API, versión de Android, huella de compilación y kernel. Cada
  escritura se anota antes de hacerse, y si la app muere a mitad se recupera sola al
  abrirla.

Lo que **ningún** modo permite: tocar IMEI, número de serie, MAC o Android ID, ni
`persist.*`, `ro.boot.*`, `ro.secure`. Esas listas están en `core/Seguridad.kt` y a la
vista en Diagnóstico. El detalle completo está en [SEGURIDAD.md](SEGURIDAD.md).

**No hay «acceso total»**, ni con el modo experto. Sin root no existe, y con root esta app
no lo necesita: no hay un botón que finja dártelo.

## Compatibilidad

Android **8.0 a 16** · cuatro arquitecturas · **cero dependencias de Google Play** (por
eso puede funcionar en Huawei sin GMS). Verificado con Lint `NewApi` = 0, y con la
pestaña **Compat.**, que prueba en tu propio teléfono cada función y te dice cuáles
responden de verdad. Detalle: [COMPATIBILIDAD-ANDROID.md](COMPATIBILIDAD-ANDROID.md).

## Los datos que viajan dentro (1.6.0)

La app **no descarga nada**: los datos van en el APK. Son de terceros, con su fuente a la
vista en la pantalla *Diagnóstico* y en `app/src/main/assets/datos/manifiesto.json`:

| Bloque | Qué hay | Fuente |
|---|---|---|
| **TOP500** | 14 ediciones oficiales (2019-2025): **7.000 posiciones** con núcleos, Rmax, Rpeak, potencia y país — **más los 14 XML originales**, sin tocar | top500.org |
| **Dispositivos** | **54.434** modelos del catálogo oficial de Google, con marca, `device` y modelo — **más el CSV original** | Google Play |
| **Fichas** | **1.386** modelos con su SoC, su GPU y la versión de Android que declaran | mobolist.net |
| **SoC / GPU** | 63 Snapdragon y 38 MediaTek con GPU, MHz y GFLOPS; **78 GPUs Adreno** con ALUs | Wikipedia |
| **Android** | 28 versiones con su nivel de API | documentación de Android |

De aquí salen perfiles que **existen de verdad**: tu propio TECNO BG7 está en el catálogo
de Google (`Tecno · TECNO SPARK 20C · device TECNO-BG7 · model TECNO BG7`), y buscando
`samsung s24` o `helio g36` sale su ficha con la GPU real.

**Cuánto pesa, sin adornos.** Son 14,3 MB de datos; el ZIP del APK los comprime. Se
publican **dos APK con exactamente el mismo contenido**:

| APK | Pesa | Por qué |
|---|---|---|
| normal | **3,01 MB** | el ZIP comprime los datos a un 21% |
| `-sin-comprimir` | **15,50 MB** | datos crudos dentro del ZIP: se abren sin descomprimir en el teléfono y se verifican con `unzip` |

Para llegar a 15 MB **no se ha añadido ni un byte de relleno**: el contenido es idéntico.
Se puede reproducir: `./gradlew :app:assembleRelease -PrebornDatos=crudo`.

## Rendimiento: lo que hacen los módulos, sin root (1.6.0)

Sección **Rendi.** Se leyeron las fichas de los **280 módulos** de la categoría
*AxManager Plugins* de magiskmodule.com para ver **qué hace cada uno** (no se copia código
ni diseño: ese sitio publica fichas, no código) y se implementó lo que se puede hacer con
los permisos de `adb shell`, que son los que tiene esta app:

- **Compilar una app por adelantado (AOT)** — `cmd package compile -m speed -f`
- **Suspender una app** sin desinstalarla — `am suspend`
- **Forzar Doze** ahora mismo — `dumpsys deviceidle force-idle`
- **Modo juego** por app (Android 12+) — `cmd game set --mode performance`
- **Modo de rendimiento fijo** — `cmd power set-fixed-performance-mode-enabled`
- **Quitar el limitador térmico** (riesgo alto, revierte al reiniciar) — `cmd thermalservice`
- **Que el WiFi no escanee sin parar** — `settings put global wifi_scan_throttle_enabled`
- **Ahorro de batería forzado** — `settings put global low_power`

Cada tarjeta enseña **el comando exacto antes de ejecutarlo**, **cómo se comprueba** y
**cómo se deshace**. Y hay una lista al final, con nombre y motivo, de lo que esos módulos
hacen y aquí **no** se puede: gobernadores de CPU, Vulkan forzado, SurfaceFlinger, sondeo
táctil. Todo eso pide root; una app sin root que lo ofreciera estaría mintiendo.

## Aprender: buscar, extraer y generar perfiles (1.5.0)

Sección **Aprender**. Tres cosas, y ninguna se inventa datos:

- **Buscar** en el catálogo y en tus perfiles. Sin acentos y sin mayúsculas: `tecno`
  encuentra `TECNO`. Si no hay, dice «sin resultados»; no rellena con parecidos.
- **Generar desde tu teléfono.** Lee lo que el aparato **declara de verdad** (`getprop`
  en una sola llamada) y deriva perfiles con cuatro criterios: mismo aparato, misma
  familia, misma GPU, o el conservador que sólo toca marca y modelo. Cada candidato
  trae un **encaje** —cuánto se parece a tu teléfono— con sus motivos: misma GPU, misma
  placa, mismo OpenGL ES, y cuántos campos se pudieron leer.
- **Extraer de un texto pegado.** Pega una ficha de especificaciones y reconoce marca,
  modelo, device, SoC, GPU y OpenGL ES. Lo que no aparezca **queda vacío**.

Dos reglas que se prueban en las 143 comprobaciones del núcleo:

1. **Nada inventado.** Si el teléfono no declara `ro.opengles.version`, el perfil deja el
   campo vacío en vez de escribir «OpenGL ES 3.2», que es texto y rompe la propiedad.
2. **Nada que identifique el aparato.** El perfil derivado no se lleva número de serie,
   IMEI ni MAC. Se comprueba con una lista de claves prohibidas, no con buena voluntad.

Sobre **«generación distribuida»**: reparte el trabajo entre varios generadores dentro
del propio teléfono y consolida por encaje. No es una red de máquinas, y en la pantalla
se llama por su nombre.

Tus perfiles se guardan en `filesDir/perfiles.json`, en texto plano: se pueden abrir,
leer y borrar a mano. Un perfil que no puedes inspeccionar no es de fiar.

## Paneles por app (1.4.0)

Cada app tiene **su propio panel**, en pestaña nueva **Aplicado** o con el botón *Panel* de
cada tarjeta. Dice qué se le aplicó a **esa** app, cuándo, y qué significa:

- Congelada → deja de gastar batería y desaparece del cajón de apps.
- Activada · Detenida · Caché borrada · Desinstalada.
- Lo que **no** tiene vuelta atrás se marca en rojo, dentro del panel y en la lista.

El panel se sirve en un `WebView` desde los assets del APK con `WebViewAssetLoader`
(`https://appassets.androidplatform.net`). **No hay servidor, ni petición de red**: por eso
la app sigue sin permiso de INTERNET y el panel funciona en modo avión. El historial vive
en `filesDir/aplicado.json`, se queda en el teléfono y guarda como mucho **40 hechos por
app** (es un historial legible, no un registro de auditoría).

Javascript está encendido porque el panel lo necesita para pintar. Lo que está apagado:
`allowFileAccess`, `allowContentAccess` y `domStorage`.

## Permisos, uno por uno

Tabla completa con motivo y origen en [PERMISOS.md](PERMISOS.md). Resumen de la 1.5.0:

| Permiso | Quién lo declara | Para qué |
|---|---|---|
| `moe.shizuku.manager.permission.API_V23` | Shizuku / OmniShizuku | ejecutar la shell |
| `android.permission.POST_NOTIFICATIONS` | Android 13+ | un aviso: la reversión de resolución en 15 s |
| `android.permission.INTERNET` | el sistema | **sólo** para la búsqueda opcional de *Aprender* |
| `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | la añade AndroidX sola | proteger sus receptores internos |

**Se quitó `QUERY_ALL_PACKAGES`** (existía hasta la 1.4.0). Consecuencia honesta: las
apps sin icono de lanzador pueden no aparecer en la lista de congelar. Está escrito en
*Diagnóstico* y en `PERMISOS.md`.

**INTERNET no cambia lo de «funciona sin red»**: `res/xml/red_segura.xml` no declara ni
un dominio de confianza y prohíbe el tráfico en claro, así que el sistema no puede abrir
conexiones. Y nada depende de la red: en modo avión se puede aplicar un perfil, ver el
panel de una app y generar perfiles del aparato.

**No hay ubicación, ni cámara, ni contactos, ni almacenamiento, ni teléfono, ni SMS.**

## Utilidades (1.2.0)

Sección **Útil**, todo a través de la shell de Shizuku — los mismos permisos que un
`adb shell`, sin root:

- **Resolución** con densidad recalculada y **reversión automática en 15 s** si no
  confirmas. El comando de vuelta corre aparte: funciona aunque la app se cierre.
- **Frecuencia de refresco** a 60 / 90 / 120 Hz.
- **Congelar apps** para que no gasten batería, sin desinstalarlas. Las del sistema están
  bloqueadas en el código.
- **Borrar caché** por app, midiendo antes y después.
- **DNS privado** y **escala de animación**.

De dónde salió cada idea y qué se descartó a propósito: [FUNCIONES.md](FUNCIONES.md).

## Las diez pantallas

- **Perfiles** — el estado real del teléfono arriba, el catálogo con su nivel de confianza,
  y *Aplicar* / *Restaurar*. Cada card dice si está VERIFICADO o PARCIAL y **qué falta**.
- **Apps** — todo lo instalado, contado y sin filtro. Está aquí para que compruebes tú
  mismo que no hay lista blanca: si tu app no aparece, es que no está instalada.
- **GPU** — lo que declara el teléfono y la regla del formato: `196610` es OpenGL ES 3.2.
  Poner el texto «OpenGL ES 3.2» en esa propiedad no la cambia, la rompe.
- **Super** — diez fichas del TOP500, con los datos de la lista vigente. **Son simulación**:
  el teléfono *declara* llamarse LineShine; no tiene sus 13.789.440 núcleos. Va aquí y no
  en Perfiles para que nunca se confunda con un perfil de dispositivo.
- **Rendi.** — las acciones que se pueden hacer con shell, con comando, comprobación y
  reversión a la vista, y la lista de lo que sólo se puede con root.
- **Aprender** — buscar en el catálogo, generar perfiles del aparato real (con su encaje
  y sus avisos) y extraerlos de un texto pegado.
- **Útil** — resolución con reversión automática, refresco, congelar apps, borrar caché,
  DNS privado y escala de animación. Cada tarjeta tiene su botón **Panel**.
- **Aplicado** — una ficha por app con lo que se le hizo, y su panel en cristal.
- **Compat.** — nueve pruebas reales en tu teléfono (`wm size`, `pm list packages`,
  DNS privado, `getprop`, `setprop`…) y el resumen de qué responde y qué no.
- **Diagnóstico** — cada afirmación con su estado, las listas del filtro completas, y una
  lista explícita de lo que esta app **no** puede hacer. A la vista, no en un pie.

## El aspecto

Interfaz **Liquid Glass**: superficies translúcidas con degradados radiales, bordes de luz
arriba y abajo, y la navegación en una **píldora flotante** sobre el contenido en vez de
una fila de botones pegada al borde. La referencia es el iOS de las apps de anime/manga
(ReDantotsu, Kiroku) — blur, píldora abajo, animaciones de muelle. No es una copia de
ninguna de ellas ni de la ficha de un módulo: la paleta (violeta, cian, rosa) y todo el
código son de aquí.

El blur es de Compose (`Modifier.vidrio()`), no captura de pantalla: no cuesta batería
extra y no depende de la versión de Android.

## Si la app «no abre»: los dos fallos reales, arreglados

**1)** En la 1.3.0 se preguntaba *una vez por app* si estaba congelada, desde el hilo de
interfaz: con ~100 apps, Android mataba la app por ANR antes de pintar. **2)** En la 1.5.0
el constructor de `Director` lanzaba `NullPointerException`: `modo` se inicializaba
llamando a `cargarModo()`, que leía de `prefs`… declarado dos líneas **después**. Kotlin
inicializa en orden de declaración, así que `prefs` era null. Ahora `prefs` va antes y
además tiene su propio `try/catch`, y hay una **prueba que lee `Director.kt`** para que no
vuelva a pasar.

## Notas antiguas

Pasó en un TECNO de 4 GB con la 1.3.0, y la causa era mía: al arrancar se preguntaba
**una vez por app** si estaba congelada, en el hilo de interfaz, con 8 s de tope por
consulta. Con ~100 apps eso no termina nunca y Android mata la app por ANR. En la 1.5.0:

- Se pregunta **una sola vez** (`pm list packages -d` devuelve todas).
- Todo lo que habla con la shell va **fuera del hilo de interfaz**.
- La pantalla se pinta **antes** de leer nada, con un esqueleto de cristal.
- Si algo falla al arrancar, sale el mensaje real en pantalla en vez de quedarse en negro.
- Cada acción está envuelta: si revienta, se convierte en un resultado rojo, no en un cierre.

Se puede comprobar leyendo `MainActivity`: `releer()` no contiene ni una llamada a la shell.

## Compilar

```bash
./gradlew :app:assembleDebug          # APK de depuración
./gradlew :app:pruebasNucleo          # 143 comprobaciones del núcleo, sin emulador
```

Para firmar el release hacen falta variables de entorno; **no hay ninguna clave en el
repositorio**:

```bash
export REBORN_KEYSTORE=/ruta/reborn-release.jks
export REBORN_STORE_PASS=...  REBORN_KEY_PASS=...  REBORN_KEY_ALIAS=reborn
./gradlew :app:assembleRelease
```

Sin esas variables el resultado es `app-release-unsigned.apk`. Eso es a propósito: una
llave que se firma sola es una llave que cualquiera puede usar.

## Sin límite de apps

No hay lista blanca ni cuota. Un perfil se aplica al sistema entero: afecta a todas las
apps a la vez, que es lo que hace útil a esta clase de herramienta. No hay «3 perfiles
gratis» ni «desbloquea más dispositivos»: el catálogo entero está abierto.
