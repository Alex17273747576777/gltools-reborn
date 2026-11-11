# GLTools Reborn 1.13.1

**Arreglado: «Compartir el último informe» no compartía nada en Android 10 o más.**

La 1.12.2 añadió el botón y la 1.13.0 lo heredó tal cual. En Android 10 o más
`Informes.guardar` devuelve una etiqueta para enseñar —`Documentos/GLTools Reborn/reborn-….md`—
y no una ruta: la ruta real de MediaStore no es accesible para la app. `Compartir.para` filtraba
esas cadenas con `File(ruta).isFile`, que da falso, y concluía «no hay ningún informe en disco
que compartir» justo después de haberlo escrito. El informe estaba; el botón mentía.

Ahora compartir no se fía de la lista que le llega: de cada ruta o etiqueta saca el nombre, lo
resuelve dentro de la carpeta de la app —que es lo único que el proveedor sirve— y, si ahí no
está, mira qué informes hay y coge la tanda más reciente, su `.md` y su `.txt`. El botón sale
además activo al abrir la app si queda un informe de una sesión anterior: antes dependía de una
lista que vive en memoria y se pierde cuando muere el proceso.

## Lo demás que se tocó

- **Fuera la rama `Uri.fromFile`.** Estaba muerta —los nombres generados siempre cumplen el
  filtro— pero era un cierre de app esperando a ocurrir: con `targetSdk 35`, un `file://` en un
  `ACTION_SEND` lanza `FileUriExposedException` en Android 7 o más. Si un fichero no se puede
  servir por el proveedor, se descarta.
- **Las copias internas ya no se acumulan para siempre.** Se conservan las cinco últimas tandas
  y el resto se borra. Son informes con datos del teléfono: borrarlos es privacidad, no espacio.
- **Borrados dos `.pyc` que estaban versionados** en `.github/scripts/__pycache__/`, añadido
  `__pycache__/`, `*.pyc` y `*.pyo` a `.gitignore`, `-B` y `PYTHONDONTWRITEBYTECODE=1` al paso
  que los generó, y una comprobación nueva en CI que falla si vuelve a entrar bytecode o un
  keystore en git.
- **Borrado `res/xml/rutas_informes.xml`.** No lo leía nadie —el proveedor es propio, no
  `FileProvider`— y su comentario describía una clase que no existe. Un fichero de seguridad que
  no hace nada es peor que no tenerlo: invita a confiar en una protección imaginaria.
- **La versión ya no está escrita en dos sitios.** `RegistroFallos.VERSION_REBORN` se ponía a
  mano y `build.gradle` iba por su cuenta; ahora el CI los compara y falla si se separan.

## Pruebas

- `pruebasNucleo` (JVM, sin emulador) reproduce el fallo con ficheros reales en una carpeta
  temporal: que una etiqueta de MediaStore no es un fichero, que de ella se saca el nombre
  servible, que la tanda más reciente es su `.md` y su `.txt`, que la limpieza borra tandas
  viejas y nunca un fichero que no es informe, y que la fuente ya no construye `file://`.
- El APK sale firmado con la clave original `1ff4c6ee…c6c9e803`, comprobada con `apksigner`
  antes de publicar. Sin esa huella exacta no se publica.

## Lo que NO se ha hecho

- **Ninguna prueba en un teléfono.** Ni arranque, ni compartir de verdad contra Gmail o
  WhatsApp, ni comportamiento del Power HAL. Lo que se afirma aquí sale de leer y probar el
  código en una JVM, no de un dispositivo.
- El modo de penetración sigue con sondas que no conservan el estado previo: ver
  `docs/PENDIENTES.md`.
