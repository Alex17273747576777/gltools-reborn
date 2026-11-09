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

Son **tres**, y ninguno es mío de verdad. Tabla completa con motivo y origen en
[PERMISOS.md](PERMISOS.md). Resumen:

| Permiso | Quién lo declara | Para qué |
|---|---|---|
| `moe.shizuku.manager.permission.API_V23` | Shizuku / OmniShizuku | ejecutar la shell |
| `android.permission.QUERY_ALL_PACKAGES` | el sistema | ver todas las apps instaladas |
| `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | la añade AndroidX sola | proteger sus receptores internos |

**No hay INTERNET, ni ubicación, ni cámara, ni contactos, ni almacenamiento, ni teléfono,
ni SMS.** La ausencia de INTERNET no es un descuido: es la razón de que la app pueda
funcionar en modo avión y de que no haya nada que enviar a ningún sitio.

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

## Las ocho pantallas

- **Perfiles** — el estado real del teléfono arriba, el catálogo con su nivel de confianza,
  y *Aplicar* / *Restaurar*. Cada card dice si está VERIFICADO o PARCIAL y **qué falta**.
- **Apps** — todo lo instalado, contado y sin filtro. Está aquí para que compruebes tú
  mismo que no hay lista blanca: si tu app no aparece, es que no está instalada.
- **GPU** — lo que declara el teléfono y la regla del formato: `196610` es OpenGL ES 3.2.
  Poner el texto «OpenGL ES 3.2» en esa propiedad no la cambia, la rompe.
- **Super** — diez fichas del TOP500, con los datos de la lista vigente. **Son simulación**:
  el teléfono *declara* llamarse LineShine; no tiene sus 13.789.440 núcleos. Va aquí y no
  en Perfiles para que nunca se confunda con un perfil de dispositivo.
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

## Compilar

```bash
./gradlew :app:assembleDebug          # APK de depuración
./gradlew :app:pruebasNucleo          # 87 comprobaciones del núcleo, sin emulador
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
