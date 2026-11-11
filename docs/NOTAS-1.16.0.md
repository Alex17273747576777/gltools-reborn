# GLTools Reborn 1.16.0

**Caza de promesas rotas: cinco frases de la interfaz decían algo que el código no hace.**

Se extrajeron de `ui/` y `MainActivity.kt` todas las cadenas visibles que **afirmaban un
comportamiento** —78 frases con verbos como *guarda*, *abre*, *borra*, *avisa*, *siempre*, *nunca*,
*requiere*— y se contrastó cada una con el código que la respalda. Cinco no se sostenían. Las cinco
tenían el mismo origen: la frase estaba escrita a mano en la pantalla, lejos de la condición que la
hace cierta o falsa.

## Las cinco que mentían

| La frase | Lo que hace el código |
|---|---|
| «Dale permiso de dibujar encima de otras apps. **Es el único permiso que necesita**» | Falso en Android 13+: la misma pantalla promete una notificación fija, el servicio va en primer plano (`startForeground`) y `MainActivity.pedirNotificaciones()` pide `POST_NOTIFICATIONS`. Sin ese permiso la capa dibuja igual, pero **el aviso no se ve**. |
| «**Solo Shizuku** puede concederlo; **sin él la app no puede hacer nada**» | Falso dos veces: con root se escribe sin Shizuku (lo dicen `Arquitectura.banner` y `Director.descripcion`), y sin ninguna vía leer y buscar siguen funcionando —lo dice esa misma pantalla, unos renglones más arriba. |
| «Al cerrar la app **se genera el informe igual**, con lo que haya en ese momento» | Falso si el perfil tiene `alCerrar = false`. La misma pantalla enseña más arriba «Cerrar la app genera informe: **no**». Se contradecía a sí misma en un palmo de scroll. |
| «**Se guarda en Documentos/GLTools Reborn**» | Sólo en Android 10+. En Android 8 y 9 (`minSdk 26`) `Informes.guardar` escribe en `getExternalFilesDir` y devuelve la ruta exacta. El KDoc del código ya lo decía con honestidad; la etiqueta no. |
| «si la app muere a mitad **se recupera al abrirla**» | Existe de verdad, pero sólo si al abrir sigue en modo experto **y** hay vía. Sin Shizuku en marcha ni root no se puede escribir, tampoco para deshacer, y el cuaderno se queda guardado. |

## El arreglo no es reescribir las frases

Eso es exactamente como se rompieron. Ahora las seis frases salen de `core/ReglasPromesas.kt`, un
objeto **puro** que recibe los hechos de los que depende cada promesa —nivel de API, ajuste del
perfil, si hay vía— y devuelve la frase que corresponde. Dos consecuencias:

1. Se pueden **probar en JVM**, que es lo que hace `pruebasNucleo`. Una frase escrita a mano no se
   puede probar; una que sale de un hecho, sí.
2. La pantalla ya no puede separarse de la condición. Si mañana cambia el umbral de Android, cambia
   en un sitio y cambia el texto.

Y hay una comprobación que **escanea el fuente de `ui/`**: si cualquiera de las cinco frases falsas
vuelve a aparecer escrita a mano, la prueba falla.

## Las que se comprobaron y resultaron VERDADERAS

No se tocan, y quedan anotadas en el KDoc de `ReglasPromesas` para que nadie las «arregle» sin mirar:

- **«El comando de vuelta se lanza aparte: funciona aunque la app se cierre.»** Cierto:
  `sh -c 'sleep N; wm size reset; wm density reset' >/dev/null 2>&1 &`, desacoplado de verdad.
- **«Cada escritura se anota antes de hacerla.»** Cierto: `Escritor.escribirConRegistro` llama a
  `registro.apuntar(…)` **antes** de `escribirPropiedad(…)`, y lo dice en un comentario.
- **«Al pulsar «Seguir», el parte se borra.»** Cierto: `onContinuar` llama a `RegistroFallos.borrar`.
- **«Arrástrala: se queda donde la dejes.»** Cierto: `AjustesSuperposicion.guardarPosicion` persiste
  x/y en `SharedPreferences`.
- El modo experto **persiste** entre arranques (`Director.cargarModo`), que es lo que hace posible la
  auto-reparación; y `Registro.deshacerTodo` **vacía el cuaderno** al terminar, así que la reparación
  no se repite en cada arranque.

## Pruebas

`pruebasNucleo` (JVM, sin emulador): **26 comprobaciones nuevas** —los dos umbrales de API (33
para notificaciones, 29 para Documentos), que cada promesa cambie con el hecho del que depende, que
las variantes falsas dejen de afirmarse, y el escaneo del fuente de `ui/` contra las cinco frases.

El APK sale firmado con la clave original `1ff4c6ee…c6c9e803`, comprobada con `apksigner` antes de
publicar.

## Lo que NO se ha hecho

- **Ninguna de las cinco se encontró en un teléfono.** Se encontraron leyendo, que es lo que se puede
  hacer desde aquí. La única evidencia de dispositivo de la serie sigue siendo el aviso que destapó
  la 1.15.1, y **ése tampoco está confirmado en pantalla**: hace falta que quien lo reportó instale
  una versión posterior y lo mire.
- Cambiar seis textos no cambia ningún comportamiento del teléfono. Lo que cambia es que la interfaz
  ya no afirma lo que no hace.
- Quedan frases por contrastar: se revisaron sólo las que **afirman** algo. Las que únicamente
  describen (nombres de pruebas, unidades, títulos, ayudas) no se auditaron en esta pasada, y queda
  anotado como pendiente 11.
