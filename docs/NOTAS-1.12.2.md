# GLTools Reborn 1.12.2

**El informe ahora se puede sacar del teléfono.** Hasta la 1.12.1 se generaba un `.md` y un
`.txt` al cerrar, pero en Android 8 y 9 no había forma de mandarlos: quedaban dentro de la app.
Se añade un proveedor de contenido cerrado —sólo entrega ficheros llamados `reborn-*.md` y
`reborn-*.txt`, no una carpeta— y un botón «Compartir el último informe» en Ayuda → Diagnóstico.
Además, el informe se copia siempre a la carpeta de la app, así que existe aunque Documentos
falle en el teléfono.

## Lo que se arregló por el camino

- El secreto de firma `GLTOOLS` del repositorio estaba mal formado y bloqueaba la publicación:
  la 1.12.1 se pudo publicar con la clave original `1ff4c6ee…c6c9e803` después de rehacerlo.
- El verificador del APK no entendía el formato de huella de `apksigner` 35 y rechazaba APK
  correctamente firmados.
- El verificador tenía la versión y el `versionCode` escritos a mano; ahora los lee de
  `build.gradle`, que es de donde los toma Gradle. Este cambio sube el `versionCode` a 26, que
  es lo que hace que Android instale esta versión **encima** de la anterior.

## Límites

Compilación y pruebas del núcleo verificadas en el CI. No hay prueba física en un teléfono real
desde aquí: el TECNO BG7, Android 13, es el equipo del usuario. El modo de rendimiento fijo sigue
dependiendo del fabricante y se dice cuando lo ignora. Sin root real no hay privilegios que esta
app pueda fabricar.
