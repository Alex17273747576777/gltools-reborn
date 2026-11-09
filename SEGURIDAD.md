# Modo seguro, modo experto y el filtro que puede decir NO

Esta app escribe propiedades del sistema de tu teléfono. Eso puede dejarlo raro si se
hace mal. Este documento dice exactamente qué se comprueba antes de cada escritura y
qué pasa si algo sale mal. No hay nada escondido: las listas están en el código
(`core/Seguridad.kt`) y también en la pantalla de Diagnóstico.

## Las tres capas

| Capa | Qué hace | Se puede desactivar |
|---|---|---|
| **Identidad** | Bloquea IMEI, número de serie, MAC, Android ID, `ro.serialno`, `gsm.sim.*`… | **Nunca.** Ni en modo experto. |
| **Arranque** | Bloquea `persist.*`, `ro.boot.*`, `ro.secure`, `ro.debuggable`, `init.*`, `sys.*` | **Nunca.** |
| **Alcance** | Según el modo: sólo modelo/GPU, o también API/kernel/build | Sí: es el interruptor |

Las dos primeras son la respuesta a «un sistema de auto corrección de datos para evitar
daño al dispositivo». No son avisos que se puedan saltar con un «acepto»: son una
comprobación que devuelve `false` y detiene la escritura.

### Por qué esas dos son intocables

- **Identidad**: cambiar el IMEI o el número de serie no simula otro teléfono, falsifica
  el identificador único de un aparato. Eso es lo que se usa para fraude, y en la mayoría
  de países es delito. Esta app no lo hace ni con el modo experto encendido, y lo dice en
  voz alta en lugar de callarlo.
- **Arranque**: `persist.*` se relee en el siguiente arranque y `ro.secure` decide si el
  sistema confía en sí mismo. Un error ahí es un teléfono que no pasa del logo. No merece
  la pena ni con el modo más permisivo.

## Los dos modos

### Seguro (por defecto)

Permite: marca, fabricante, modelo, device, board, hardware, versión de OpenGL ES, densidad.

- **Auto corrección**: después de aplicar, se relee cada propiedad. Si alguna no cuadró
  —el fabricante la bloqueó, o el sistema la rechazó— **se deshace todo solo** y te dice
  «tu teléfono está como estaba». No se queda a medias.

### Experto

Añade: versión de Android, número de API, huella de compilación, kernel, ABI.

- Cada escritura **se anota antes de hacerse**, con el valor que había. Si la app muere a
  mitad (el sistema la mata, se acaba la batería), al volver a abrirla ve que quedó a
  medias y **devuelve sola** los valores originales.
- Por qué importa: cambiar `ro.build.version.sdk` puede hacer que apps dejen de
  instalarse o que el sistema se comporte distinto. Es reversible, pero el orden
  «anotar antes de escribir» es lo que hace que sea reversible de verdad.

## Lo que el filtro rechaza siempre

- **Inyección de shell**: comillas, punto y coma, backticks, `$`, saltos de línea, nulos.
  Un valor como `Pixel; reboot` no se escribe: se rechaza.
- **Valores de más de 92 caracteres**. No es una manía: `PROP_VALUE_MAX` es 92 en bionic.
  Un valor más largo se trunca o se rechaza, así que escribirlo garantiza dejarlo mal.
- **Valor vacío**: vaciar una propiedad no es lo mismo que no tocarla. Se omite.

## Lo que NO se puede hacer, ni con el modo experto

**No existe un «acceso total sin root».** Shizuku da lo que da el shell de Android por
ADB: leer y escribir propiedades. No da acceso al kernel, ni a `/dev`, ni a SELinux, ni a
particiones. Con `su` sí hay más alcance, pero esta app no lo usa para nada que no sea
`setprop`, porque lo demás no hace falta para lo que hace.

Por eso **no hay** un botón de «acceso total». Habría sido fácil ponerlo y que no hiciera
nada; sería mentirte en la pantalla más importante de la app.

## Archivos

- `core/Seguridad.kt` — el filtro y las cuatro listas.
- `core/Registro.kt` — el cuaderno de escrituras y el deshacer.
- `core/Escritor.kt` — el orden: filtrar → anotar → escribir → releer → deshacer si falla.
