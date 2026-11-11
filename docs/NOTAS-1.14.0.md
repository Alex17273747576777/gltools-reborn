# GLTools Reborn 1.14.0

**Tres vicios del mismo género: cosas que prometen una cosa y hacen otra.**

## 1 · El diagnóstico ya no llama «carga» a lo que no es carga

La prueba de CPU se llamaba **«Carga de CPU»** y mide operaciones de punto flotante por segundo en
**un solo hilo**. No mide carga: no mira cuántos núcleos están ocupados ni cuánto trabaja el
sistema. Es el mismo vicio que llamar «Congelar apps» a un `pm list packages -d`, arreglado en la
1.13.2. Ahora se llama «Velocidad de CPU (un hilo)» y el detalle dice que no es la carga ni una
puntuación comparable con AnTuTu.

**Nuevo: estado térmico y margen térmico.** `PowerManager.getCurrentThermalStatus()` (API 29) y
`getThermalHeadroom()` (API 30) son lo único que Android publica sobre calor sin root. Se enseñan
como lo que son: un **nivel declarado por el sistema**, con las descripciones literales de la
documentación oficial, y una estimación en el caso del margen. No son grados. La temperatura de la
batería sí son grados y ya se leía; la de la CPU sigue sin tener API pública y se sigue declarando
«no medido» en vez de suponerla.

Los valores numéricos de `THERMAL_STATUS_*` **no** aparecen en la documentación, así que el código
compara contra las constantes simbólicas y pasa etiquetas a la regla pura: la escala 0-6 que se
enseña es de la app y se dice en el detalle. No se inventan números de la plataforma.

**La escritura en almacenamiento ahora hace `fsync`.** Sin él se medía sobre todo la caché de
páginas: el número que salía no era lo que tardó el almacenamiento, era lo que tardó la RAM en
fingir que escribía. El detalle dice si hubo fsync o no, porque cambia lo que significa la cifra.

## 2 · El perfil de diagnóstico ahora hace lo que decía

`PerfilDiagnostico` llevaba escrito desde el principio: *«Pruebas a las que el usuario dijo que sí.
**Las que no estén, no se ejecutan.**»* Era mentira. `Autodiagnostico.ejecutar` no recibía la lista
de pruebas, así que se ejecutaban **todas** siempre: un perfil «Rápido» que enseña «3 pruebas» hacía
las nueve. Y el selector de duración de la CPU (150/400/1000 ms) tampoco llegaba a ningún sitio: el
código usaba `if (profundo) 400 else 150`, así que elegir 1000 ms no cambiaba nada.

Ahora `ejecutar` recibe `pruebas` y `msCpu`, y decide con `ReglasDiagnostico.seleccion`: **la misma
función** que gobierna en el teléfono y la que se comprueba en las pruebas, para que no pueda haber
dos versiones de la verdad. Los dos sitios que llamaban (`MainActivity` e `Informes.alCerrar`) pasan
el perfil completo.

**Cambio de comportamiento a propósito:** un perfil guardado con pocas pruebas marcadas medirá menos
que antes, porque antes medía de más. Un perfil antiguo que decía «todas» se migra a las nueve
claves nuevas y se avisa: la prueba térmica no existía cuando se guardó, así que el usuario no la
desmarcó.

## 3 · Los datos de terceros dicen de dónde vienen, con hash y fecha

El APK lleva 54,434 fichas del catálogo oficial de Google Play, 1.386 de mobolist, 101 SoC y 78 GPU de
Wikipedia, y las 14 ediciones de TOP500. Hasta aquí `manifiesto.json` declaraba fuente y URL, pero
**no había ni un hash ni una fecha de captura por dato**.

Ahora lleva un bloque `verificacion` con el **sha256 y el tamaño reales de cada fichero de datos**,
calculados sobre lo versionado, y el CI los recalcula en cada push: si un dato cambia y su hash
declarado no, la compilación falla. La pantalla de Datos lo enseña.

Y lo más importante, porque es lo que casi nadie hace: **la diferencia con el origen se declara en
vez de maquillarse.** El CSV oficial de Google descargado hoy (2026-11-11) pesa 4,817,618 bytes en
UTF-16LE con 54,452 fichas, sha256 `634203f9a5fbbb2f42da1ce2020643718f440308a4cc4333b22df58a9aa027e8`. La copia incluida pesa 1,976,584 bytes en UTF-8
con 54,434 fichas, sha256 `7300f8599f2f4353096fe940dbe8756411c367c18bc57586d215ea06a761b7d3`. **No coinciden**: el origen tiene 18 fichas más y va en
otra codificación. La copia es una foto anterior transcodificada, no el fichero de origen byte a
byte, y eso está escrito dentro del propio APK.

**La fecha de captura original no se registró nunca** y no se va a inventar: el campo va en `null`
con el motivo, y lo más antiguo verificable es `manifiesto.generado` (2026-11-09) y la entrada en git
del CSV (commit `5c262d9c`, 2026-11-10T02:00:37Z).

## Pruebas

`pruebasNucleo` (JVM, sin emulador): **41 comprobaciones nuevas** sobre los nombres, los niveles
térmicos, el respeto del perfil y la procedencia —leyendo el `manifiesto.json` real del APK, e
incluidas las que exigen que la deriva con el origen y la ausencia de fecha de captura estén
declaradas. CI nuevo que recalcula los hashes de los 7 ficheros de datos y los
29 de `top500/`, y falla si alguno viaja sin procedencia.

El APK sale firmado con la clave original `1ff4c6ee…c6c9e803`, comprobada con `apksigner` antes de
publicar.

## Lo que NO se ha hecho

- **Ninguna prueba en teléfono.** Las reglas se prueban en una JVM; que `getCurrentThermalStatus` o
  `getThermalHeadroom` devuelvan algo útil en un aparato concreto, y que el `fsync` no haga la prueba
  demasiado lenta en almacenamiento lento, **no está comprobado en un dispositivo**.
- No se reimportó el catálogo a las 54,452 fichas de hoy: se declaró la diferencia. Reimportar sin poder
  probar el resultado en un teléfono es cambiar 2,5 MB de datos a ciegas por 18 fichas.
- No hay casillas en la interfaz para marcar pruebas individuales: sólo los perfiles de fábrica y los
  importados pueden tener menos de nueve. Añadir ese control es el pendiente 10.
- versus.com sigue sin rastrearse: su `robots.txt` plantea un desafío anti-bot, y eso no se sortea.
  Ya estaba declarado en `fuentes_no_usadas` y sigue estándolo.
