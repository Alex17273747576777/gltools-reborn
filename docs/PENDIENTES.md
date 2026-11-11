# Pendientes abiertos de GLTools Reborn

Un pendiente que vive en el chat se pierde. Éste es el sitio donde viven, con su criterio de
cierre: no se tacha nada hasta que se cumpla lo que dice la columna «cierra cuando».

Estado a 1.13.2 (versionCode 29, firmada con la clave original `1ff4c6ee…e803`).

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

## Abiertos, por orden de trabajo

| # | Pendiente | Cierra cuando |
|---|---|---|
| 1 | Auto-diagnóstico profundo: temperatura, RAM medida, almacenamiento, batería, y que el informe diga qué NO se pudo medir sin root. | Hay motor en `core/`, pruebas que lo cubren y una pantalla que enseña crudo + interpretación. Sin estimaciones presentadas como medidas. |
| 2 | Informe `.md` al cerrar la app, guardado en Documentos **y** botón de compartir. | Se genera sin que el usuario lo pida, se puede sacar por el selector de Android, y las dos cosas tienen prueba. |
| 3 | Volcado de errores (timestamps, códigos, descripción) junto al informe. | Existe el fichero, incluye lo de `RegistroErrores` y lo de las pruebas de compatibilidad, sin datos personales. |
| 4 | Catálogo de dispositivos ampliado (mobolist Top AnTuTu y más) con importación reproducible. | Cada ficha tiene fuente, fecha de captura y `sha256` del origen; nada metido a mano de memoria. |
| 5 | Perfiles personalizados de diagnóstico en JSON (detalle, muestreo, pruebas, umbrales). | Se crean, se guardan, se cargan al iniciar y una prueba demuestra ida y vuelta. |
| 6 | Aplicación **profunda y verificada**, sin simulaciones parciales: se comprueba el efecto leyendo después de escribir, y lo que no se puede leer se declara. | `Ejecutor`, `Sistema` y `Apps` ya leían de vuelta antes de decir «aplicado»; el agujero que quedaba era el clasificador de penetración, cerrado en 1.13.2. Falta la comprobación en teléfono → pendiente 9. |
| 7 | Modo de penetración de sistema (qué se puede y qué no sin root real). | **Cerrado en código en 1.13.2**: el clasificador exige código de salida y las sondas que escriben restauran lo que había. Sigue sin probarse en un teléfono real → eso es el pendiente 9. Nada de `persist.sys.*` inventado, `rc.local` ni Device Owner como si dieran root. |
| 8 | Importación del catálogo sorteando el bloqueo del sitio (sin bots). | Se obtiene por la vía legítima y reproducible, con la fecha y el hash guardados. |
| 9 | **Verificar en un teléfono real** que compartir abre el selector y entrega el `.md` y el `.txt`, y que el informe se escribe de verdad en Documentos. | Hay una captura o un registro del teléfono, no sólo pruebas de JVM. Todo lo cerrado en 1.13.1 está probado en código, no en dispositivo: hasta que no haya teléfono, eso es lo que se puede afirmar. |

## Límites que no se van a cruzar

- Nada de scripts de arranque en `/etc/init.d`: Android 13 no los lee. Ponerlos y decir que
  «quedan activos al encender» sería mentir.
- `dpm set-device-owner` no da permisos de sistema ni ejecuta `setprop` privilegiado.
- `setprop persist.sys.perfmode` no existe en Android 13; inventarlo es el mismo error que
  `cmd power get-fixed-performance-mode-supported`, que ya costó una versión.
