# GLTools Reborn 1.15.0

**Un ajuste que existe en el código y no existe en la pantalla no es un ajuste.**

## Las pruebas ya se pueden marcar

La 1.14.0 hizo que el perfil se respetara de verdad: `Autodiagnostico.ejecutar` por fin recibe la
lista de pruebas y la duración de la CPU. Pero no había **ninguna manera de marcarlas**: la pantalla
sólo dejaba elegir duración y umbrales, así que con menos de nueve pruebas únicamente se llegaba por
el perfil de fábrica «Rápido» o importando un JSON a mano.

Ahora la pantalla de Auto-diagnóstico tiene una sección **«Qué pruebas se ejecutan»** con las nueve,
en filas con su hilo como el resto de la pantalla (nada de una caja por dato). Cada fila dice el
nombre de la prueba y **lo que mide de verdad**, y se toca para marcarla o desmarcarla.

Las descripciones están en `ReglasDiagnostico.ETIQUETAS`, que es pura y se prueba, precisamente para
que no digan más de lo que la prueba hace: «Red» dice que es **la estimación del sistema sobre el
enlace y que no se mide velocidad real**; «Velocidad de CPU» dice que son operaciones por segundo en
un solo hilo y **no la carga**; «Estado y margen térmico» dice que **no son grados**; y «Velocidad de
memoria» y «Escritura en almacenamiento» dicen que **sólo corren con el perfil en completo**, porque
es verdad.

La pantalla distingue **«marcada»** de **«se ejecuta»**: con el perfil en rápido, la RAM y la
escritura están marcadas pero no corren, y eso se ve en vez de esconderse. Debajo va el recuento
real, que sale de la misma `ReglasDiagnostico.seleccion` que decide en el teléfono: una sola regla,
dos sitios.

No se puede quitar la última prueba. `validar` ya convertiría un perfil vacío en uno de sólo memoria,
pero hacer eso ahí sería cambiarle el perfil al usuario sin decírselo: se niega el control y se
explica por qué en la pantalla.

## El hueco que dejó la 1.14.0, cerrado

«Guardar informe ahora» llamaba a `Informes.alCerrar` **sin pasarle el perfil**. En 1.14.0 arreglé la
llamada de `onStop` (el informe al cerrar la app) y ésta se quedó fuera, así que ese botón ejecutaba
las nueve pruebas a 400 ms por mucho que el usuario hubiera elegido otra cosa. Es el mismo vicio que
la 1.14.0 vino a cerrar, en el otro sitio donde se genera el informe.

Y ahora hay una prueba que lo vigila: recorre `MainActivity.kt` y **falla si queda alguna llamada a
`alCerrar` sin `pruebas`**, para que no vuelva a aparecer un tercer sitio.

## Pruebas

`pruebasNucleo` (JVM, sin emulador): **19 comprobaciones nuevas** —que las nueve claves tengan
etiqueta y ninguna se repita, que las descripciones no prometan de más, marcar, desmarcar, clave
inexistente, no poder dejar el perfil vacío, que lo marcado sea exactamente lo que `seleccion`
devuelve, y que ningún `alCerrar` se llame sin el perfil.

El APK sale firmado con la clave original `1ff4c6ee…c6c9e803`, comprobada con `apksigner` antes de
publicar.

## Lo que NO se ha hecho

- **Ninguna prueba en teléfono.** Las reglas se prueban en una JVM. Que las filas se toquen bien con
  el dedo, que el aviso de «no se puede quitar la última» se lea, y que el perfil editado sobreviva a
  cerrar y reabrir la app **no está comprobado en un dispositivo**.
- La persistencia sí está en el código (`onPerfilDiagnostico` valida y llama a
  `PerfilesDiagnosticoJson.guardar`, como ya hacían los umbrales y la duración de CPU), pero eso es lo
  que dice el código, no lo que hace un teléfono.
