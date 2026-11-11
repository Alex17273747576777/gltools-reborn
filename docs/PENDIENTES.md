# Pendientes abiertos de GLTools Reborn

Un pendiente que vive en el chat se pierde. Éste es el sitio donde viven, con su criterio de
cierre: no se tacha nada hasta que se cumpla lo que dice la columna «cierra cuando».

Estado a 1.16.0 (versionCode 33, firmada con la clave original `1ff4c6ee…e803`).

## Cerrado en 1.12.1

| Pendiente | Cómo quedó |
|---|---|
| Publicación bloqueada por «no hay clave» | El keystore y su contraseña sí existían. El secreto `GLTOOLS` del repositorio estaba mal formado (`FORMATO_JSON=INVALIDO`) y el verificador no entendía el formato de huella de `apksigner` 35 (`V2 Signer:`). Arreglado en `f5ed2d1` y `dc4f349`. La release `v1.12.1` sale firmada y con el certificado comprobado. |

## Cerrado en 1.13.1

| Pendiente | Cómo quedó |
|---|---|
| Compartir el informe no funcionaba en Android 10+ | `Informes.guardar` devuelve una etiqueta de MediaStore, no una ruta, y `Compartir.para` filtraba con `File(ruta).isFile`: siempre falso, y el botón decía «no hay ningún informe». Ahora se saca el nombre de la ruta o etiqueta, se resuelve en la carpeta de la app y, si no está, se comparte la tanda más reciente que haya ahí. Prueba JVM que reproduce el fallo con ficheros reales. |
| Rama `Uri.fromFile` muerta pero peligrosa | Eliminada. Con `targetSdk 35`, un `file://` en `ACTION_SEND` es `FileUriExposedException`. CI comprueba que la palabra no vuelve a aparecer en el proveedor. |
| Bytecode versionado y versión escrita en dos sitios | Borrados los `.pyc`, `.gitignore` los cubre, el paso que los generaba va con `-B`, CI falla si entra bytecode o un keystore en git, y CI compara `VERSION_REBORN` con `build.gradle`. |

## Cerrado en 1.13.2

| Pendiente | Cómo quedó |
|---|---|
| El clasificador de penetración ignoraba el código de salida | `Shell.Salida.codigo` existía y nadie se lo pasaba a `ReglasPenetracion.clasificar`: un comando que terminaba mal con salida inocente salía COMPLETO. Ahora `codigo` es obligatorio y distinto de cero es NO (`-1` también). Doce comprobaciones JVM con códigos explícitos, incluida la que reproduce el fallo. |
| Las sondas que escribían no restauraban el estado previo | `settings put global reborn_prueba 1` se quedaba en el sistema para siempre; `setprop debug.…` se ponía a vacío en vez de volver a lo que había. Las dos leen el valor previo, escriben, leen de vuelta y restauran o borran en el mismo comando, con `exit $rc` para que el código sea el de la escritura y no el del eco. Semántica comprobada con `sh` real. |
| Nombres que prometían más que el comando | «Congelar apps (pm disable-user)» sólo listaba; «Modo rendimiento fijo» sólo miraba la ayuda. Renombrados a lo que de verdad prueban, con prueba que impide que vuelvan. |

## Cerrado en 1.14.0

| Pendiente | Cómo quedó |
|---|---|
| La prueba de CPU se llamaba «Carga de CPU» | Medía operaciones de punto flotante por segundo en un solo hilo, que no es la carga. Renombrada a «Velocidad de CPU (un hilo)» y el detalle dice que no es la carga ni una puntuación comparable con AnTuTu. Con prueba que impide que el nombre vuelva. |
| El perfil prometía cosas que no hacía | `PerfilDiagnostico` decía «las pruebas que no estén marcadas no se ejecutan» y `ejecutar` no recibía la lista: se ejecutaban todas. El selector de duración de CPU (150/400/1000 ms) tampoco llegaba a ningún sitio. Ambos se aplican ahora con la misma regla pura (`ReglasDiagnostico.seleccion`) en el teléfono y en las pruebas, y los perfiles antiguos que decían «todas» se migran a las nueve claves con aviso. |
| El calor del teléfono no se leía aunque Android lo publica | Nuevo: `getCurrentThermalStatus` (API 29) y `getThermalHeadroom` (API 30), enseñados como niveles declarados por el sistema con las descripciones literales de la documentación oficial, nunca como grados. Los valores numéricos de `THERMAL_STATUS_*` no están publicados, así que se compara contra las constantes simbólicas. |
| La escritura se medía sin `fsync` | Medía sobre todo la caché de páginas. Ahora hace `fsync` y el detalle dice si lo hizo, porque cambia lo que significa la cifra. |
| Datos de terceros sin procedencia verificable | `manifiesto.json` lleva el sha256 y el tamaño de cada fichero de datos, el hash y la fecha del origen oficial descargado, y la deriva declarada (el origen de Google tiene hoy 18 fichas más y va en UTF-16LE). El CI los recalcula en cada push y falla si algún dato viaja sin hash. La fecha de captura original NO se registró nunca y va en `null` con el motivo. |

## Cerrado en 1.15.0

| Pendiente | Cómo quedó |
|---|---|
| 10 · Casillas en la interfaz para elegir qué pruebas se ejecutan | Sección «Qué pruebas se ejecutan» con las nueve, en filas con su hilo. Las etiquetas viven en `ReglasDiagnostico.ETIQUETAS` (pura, probada) y no dicen más de lo que cada prueba hace: red es una estimación del sistema, la CPU no mide carga, lo térmico no son grados, y RAM/IO sólo corren en completo. La pantalla distingue «marcada» de «se ejecuta» y no deja quitar la última prueba, explicando por qué. Lo marcado y lo que corre salen de la misma `seleccion` que usa el teléfono. |
| El hueco que dejó 1.14.0 | «Guardar informe ahora» llamaba a `Informes.alCerrar` sin el perfil: ejecutaba las nueve pruebas a 400 ms ignorando lo elegido. Arreglado, y con una prueba que recorre `MainActivity.kt` y falla si vuelve a quedar un `alCerrar` sin `pruebas`. |

## Cerrado en 1.15.1

| Pendiente | Cómo quedó |
|---|---|
| El estado térmico se enseñaba como un número | Un usuario con un teléfono en la mano dijo que «salía raro»: el nivel 0-6 se pintaba con barra vacía y «0.00», que se lee como si no hubiera dato. `Medida` tiene ahora un campo `texto` para categorías, y la regla es que las categorías van con palabras y las cantidades con números. El margen térmico sigue con número porque es una proporción. |
| «El sistema no devolvió nada» era una suposición | La lectura iba envuelta en un `getOrNull()` que convertía cualquier excepción del fabricante en «no contestó». Ahora la excepción se anota en los errores del diagnóstico y se dice en el motivo, y el NaN de `getThermalHeadroom` se declara como lo que es: el sistema no sabe. |
| Un informe corto no explicaba por qué era corto | `DiagnosticoCompleto.sinEjecutar` y una lista al final del informe con los nombres que ve el usuario. Desde 1.14.0 el perfil se respeta, y no aparecer no es lo mismo que decir por qué no está. |
| Leer el estado térmico y sólo pintarlo | Si el sistema declara nivel 3 de 6 o peor, `avisos` lo dice, igual que con la memoria y la batería. El umbral vive en un solo sitio (`NIVEL_PREOCUPA`). |

## Cerrado en 1.16.0

| Pendiente | Cómo quedó |
|---|---|
| La interfaz prometía cosas que el código no hace | Se extrajeron las 78 frases visibles que afirmaban un comportamiento y se contrastaron con el código. Cinco mentían: el permiso de superposición «único» (falso en Android 13+), Shizuku como única vía y «sin él la app no puede hacer nada», el informe «se genera igual al cerrar» con `alCerrar` desactivado, «se guarda en Documentos» en Android 8 y 9, y la auto-reparación «al abrirla» sin vía. |
| Las promesas no se podían probar | Nace `core/ReglasPromesas.kt`, puro: cada frase sale de los hechos de los que depende (nivel de API, ajuste del perfil, si hay vía). Una frase escrita a mano no se prueba; una derivada de un hecho, sí. |
| Nada impedía que una frase falsa volviera | Hay una comprobación que escanea el fuente de `ui/` y falla si reaparece cualquiera de las cinco. |
| No estaba dicho qué promesas sí son verdad | Cinco se comprobaron y resultaron ciertas (comando de vuelta desacoplado, anotación antes de escribir, borrado del parte, posición persistente, modo que sobrevive al arranque). Quedan anotadas en el KDoc para que nadie las «arregle» sin mirar. |

## Abiertos, por orden de trabajo

| # | Pendiente | Cierra cuando |
|---|---|---|
| 1 | Auto-diagnóstico profundo: temperatura, RAM medida, almacenamiento, batería, y que el informe diga qué NO se pudo medir sin root. | **Cerrado en código en 1.14.0**: el estado térmico y el margen del sistema ya se leen, la temperatura de la CPU se sigue declarando «no medido» con su motivo, la escritura hace `fsync`, y toda la interpretación vive en `ReglasDiagnostico` con pruebas. Sin estimaciones presentadas como medidas. Falta la comprobación en teléfono → pendiente 9. |
| 2 | Informe `.md` al cerrar la app, guardado en Documentos **y** botón de compartir. | Se genera sin que el usuario lo pida, se puede sacar por el selector de Android, y las dos cosas tienen prueba. |
| 3 | Volcado de errores (timestamps, códigos, descripción) junto al informe. | Existe el fichero, incluye lo de `RegistroErrores` y lo de las pruebas de compatibilidad, sin datos personales. |
| 4 | Catálogo de dispositivos ampliado (mobolist Top AnTuTu y más) con importación reproducible. | **Procedencia cerrada en 1.14.0** (hash por fichero, origen verificado y deriva declarada en el propio APK). Falta la parte de «ampliado»: reimportar las 18 fichas nuevas del origen de Google y añadir más fuentes, cosa que no se hace a ciegas sin poder probar el resultado en un teléfono. |
| 5 | Perfiles personalizados de diagnóstico en JSON (detalle, muestreo, pruebas, umbrales). | Se crean, se guardan, se cargan al iniciar y una prueba demuestra ida y vuelta. |
| 6 | Aplicación **profunda y verificada**, sin simulaciones parciales: se comprueba el efecto leyendo después de escribir, y lo que no se puede leer se declara. | `Ejecutor`, `Sistema` y `Apps` ya leían de vuelta antes de decir «aplicado»; el agujero que quedaba era el clasificador de penetración, cerrado en 1.13.2. Falta la comprobación en teléfono → pendiente 9. |
| 7 | Modo de penetración de sistema (qué se puede y qué no sin root real). | **Cerrado en código en 1.13.2**: el clasificador exige código de salida y las sondas que escriben restauran lo que había. Sigue sin probarse en un teléfono real → eso es el pendiente 9. Nada de `persist.sys.*` inventado, `rc.local` ni Device Owner como si dieran root. |
| 8 | Importación del catálogo sorteando el bloqueo del sitio (sin bots). | **Premisa corregida en 1.14.0**: el catálogo que viaja en el APK NO viene de un sitio bloqueado, viene del CSV oficial de Google Play (`storage.googleapis.com/play_public/supported_devices.csv`), que se descarga sin problema y cuyo hash quedó guardado en el manifiesto. Lo que sí estaba bloqueado (versus.com, desafío anti-bot en su `robots.txt`) sigue sin rastrearse y está declarado en `fuentes_no_usadas`. No se sortea ningún bloqueo. |
| 11 | **Contrastar el resto de frases de la interfaz.** En 1.16.0 se auditaron sólo las que **afirman** un comportamiento (78). Las que describen —nombres de pruebas, unidades, títulos, ayudas— no se revisaron, y el mismo género de defecto puede estar ahí. | Hay una pasada equivalente sobre las frases descriptivas, o se dice explícitamente que no la va a haber. |
| 9 | **Verificar en un teléfono real** que compartir abre el selector y entrega el `.md` y el `.txt`, y que el informe se escribe de verdad en Documentos. | Hay una captura o un registro del teléfono, no sólo pruebas de JVM. Todo lo cerrado en 1.13.1 está probado en código, no en dispositivo: hasta que no haya teléfono, eso es lo que se puede afirmar. **Primera evidencia de dispositivo en 1.15.1**: un usuario instaló la 1.14.0 y reportó que el estado térmico salía raro; eso destapó tres defectos reales. Sigue sin ser una verificación completa —no hay modelo, nivel de API ni texto exacto— y falta confirmar en su pantalla que el arreglo se ve bien. |
| 10 | Casillas en la interfaz para elegir qué pruebas se ejecutan. | **Cerrado en código en 1.15.0**: la pantalla permite marcar cada una de las nueve, una prueba JVM demuestra que lo marcado es exactamente lo que `seleccion` devuelve, y `MainActivity.kt` queda vigilado para que ningún `alCerrar` vuelva a llamarse sin el perfil. Falta la comprobación en teléfono → pendiente 9. |

## Límites que no se van a cruzar

- Nada de scripts de arranque en `/etc/init.d`: Android 13 no los lee. Ponerlos y decir que
  «quedan activos al encender» sería mentir.
- `dpm set-device-owner` no da permisos de sistema ni ejecuta `setprop` privilegiado.
- `setprop persist.sys.perfmode` no existe en Android 13; inventarlo es el mismo error que
  `cmd power get-fixed-performance-mode-supported`, que ya costó una versión.
