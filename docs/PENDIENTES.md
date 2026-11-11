# Pendientes abiertos de GLTools Reborn

Un pendiente que vive en el chat se pierde. Éste es el sitio donde viven, con su criterio de
cierre: no se tacha nada hasta que se cumpla lo que dice la columna «cierra cuando».

Estado a 1.13.1 (versionCode 28, firmada con la clave original `1ff4c6ee…e803`).

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

## Abiertos, por orden de trabajo

| # | Pendiente | Cierra cuando |
|---|---|---|
| 1 | Auto-diagnóstico profundo: temperatura, RAM medida, almacenamiento, batería, y que el informe diga qué NO se pudo medir sin root. | Hay motor en `core/`, pruebas que lo cubren y una pantalla que enseña crudo + interpretación. Sin estimaciones presentadas como medidas. |
| 2 | Informe `.md` al cerrar la app, guardado en Documentos **y** botón de compartir. | Se genera sin que el usuario lo pida, se puede sacar por el selector de Android, y las dos cosas tienen prueba. |
| 3 | Volcado de errores (timestamps, códigos, descripción) junto al informe. | Existe el fichero, incluye lo de `RegistroErrores` y lo de las pruebas de compatibilidad, sin datos personales. |
| 4 | Catálogo de dispositivos ampliado (mobolist Top AnTuTu y más) con importación reproducible. | Cada ficha tiene fuente, fecha de captura y `sha256` del origen; nada metido a mano de memoria. |
| 5 | Perfiles personalizados de diagnóstico en JSON (detalle, muestreo, pruebas, umbrales). | Se crean, se guardan, se cargan al iniciar y una prueba demuestra ida y vuelta. |
| 6 | Aplicación **profunda y verificada**, sin simulaciones parciales: se comprueba el efecto leyendo después de escribir, y lo que no se puede leer se declara. | Ninguna acción dice «aplicado» sin lectura posterior; el modo que el fabricante ignore se dice, no se finge. |
| 7 | Modo de penetración de sistema (qué se puede y qué no sin root real). | Documentado y probado en lo posible; **nada de `persist.sys.*` inventado, `rc.local` ni Device Owner como si dieran root**: en Android 13 eso no funciona y no se va a fingir que sí. |
| 8 | Importación del catálogo sorteando el bloqueo del sitio (sin bots). | Se obtiene por la vía legítima y reproducible, con la fecha y el hash guardados. |
| 9 | **Verificar en un teléfono real** que compartir abre el selector y entrega el `.md` y el `.txt`, y que el informe se escribe de verdad en Documentos. | Hay una captura o un registro del teléfono, no sólo pruebas de JVM. Todo lo cerrado en 1.13.1 está probado en código, no en dispositivo: hasta que no haya teléfono, eso es lo que se puede afirmar. |

## Límites que no se van a cruzar

- Nada de scripts de arranque en `/etc/init.d`: Android 13 no los lee. Ponerlos y decir que
  «quedan activos al encender» sería mentir.
- `dpm set-device-owner` no da permisos de sistema ni ejecuta `setprop` privilegiado.
- `setprop persist.sys.perfmode` no existe en Android 13; inventarlo es el mismo error que
  `cmd power get-fixed-performance-mode-supported`, que ya costó una versión.
