# Qué se copió de qué app, y por qué

Encargo: entrar en `appteka.store/apps`, mirar las categorías **Tools** y **Root & System**, y
aprender del código y del diseño de cada app. Esto es lo que se hizo, qué se sacó de cada una y
qué se aplicó en la 1.9.0. **Nada de esto es una impresión general: cada app se descargó, se
desmontó y se miró por dentro.**

## Cómo se hizo

- `appteka.store` sirve las fichas y los APK sin sesión. Se listaron las etiquetas `tools`,
  `system` y `root` (96 apps cada una) y de ahí se eligieron las que representan algo.
- Cada APK se descargó, se verificó que fuera un APK de verdad y se desmontó con `aapt2`
  (recursos, manifiesto) y con **jadx** (código).
- Las capturas se descargaron de las fichas y se miraron una a una. **Lo que se copia es la
  estructura, no los recursos**: ni iconos, ni fuentes, ni marca ajena.

## App por app

| App | Categoría | Qué es | Qué se aprendió |
|---|---|---|---|
| **InstallerX** `com.rosan.installer.x` 1.7 | Tools | Instalador con Shizuku/root | Arquitectura de tres capas: UI → servicio (`InstallerService`) → capa de privilegio (`com.rosan.dhizuku`, un cliente propio sobre Shizuku). El shell vive en un **servicio**, no en la Activity. Ver *Lo que se copió* abajo. |
| **Axeron Core** `com.rezone.axcore` 1.0.6 | Tools | Servicio de shell alternativo a Shizuku | Empaqueta `rikka.shizuku.*` **dentro de su APK** en vez de depender del de fuera, y habla con `moe.shizuku.api`. Eso explica por qué hay apps que funcionan sin la app de Shizuku instalada. **No se copia**: es una dependencia de terceros. |
| **LSPosed** `org.lsposed.manager` 2.2.0 | Root & System | Gestor del framework | Su pantalla principal resuelve la jerarquía sin una sola tarjeta: **estado crítico arriba** (rojo, con texto grande), luego **un único bloque de superficie** con pares etiqueta-arriba/valor-abajo, y por último **una fila pulsable** («Informazioni ›»). Tres niveles, cero ruido. |
| **Hardware Monitor** `com.hardware.monitor` 1.1.0 | Tools | Datos del dispositivo | **El patrón que faltaba aquí**: un carrusel de píldoras («Sistema», «Batería», «Térmico», «CPU») con la elegida encendida en el color de marca, y un solo panel debajo que cambia. La píldora mide una línea: entra la elegida y se asoma la siguiente, así que se entiende que hay más sin flechas. Su paleta es violeta de plantilla y no se copia. |
| **SceneF** `com.zte.gamemode`… (Scene) | Tools | Monitor y ajustes de rendimiento | Separa los grupos de ajustes en **bloques con título** y deja respirar entre ellos, en vez de una lista interminable. Interruptores a la derecha, texto a la izquierda, subtítulo en gris más pequeño. |
| **Androoster** `msl.tool.androoster` | Root | Caja de herramientas | **Rejilla de baldosas con icono grande** para las acciones principales: se pulsan sin leer. Y su bloque de datos técnicos va en monoespaciado, no en texto normal. |
| **King Patcher pro** | Tools | Parcheo de apps | Lista de apps **con icono real de cada app** a la izquierda y estado a la derecha. Sin icono, una lista de paquetes es ilegible. |
| **Install with Options** | Root | Instalador avanzado | Agrupa opciones por **qué hacen**, no por orden alfabético: primero el paquete, luego permisos, luego comportamiento. Buen ejemplo de agrupación cuando hay 10+ interruptores. |
| **Database Extra** `by4a.setedit.more` | Tools | Editor de bases de datos | Prueba de que un APK de 1,8 MB con 28 clases puede ser una herramienta útil. El tamaño no es la calidad. |
| **Slice VPN**, **GT mode**, **Kitsune Mask**, **ReZukiSU** | varias | — | Rescursos de Material por defecto (`color/accent_material_dark`, `dimen/mtrl_*`): confirman que **el diseño de esas apps vive en Compose, no en `res/`**. Mirar `res/` no dice nada del diseño; hay que mirar capturas y el código. |

## Lo que se aplicó a GLTools Reborn en la 1.9.0

1. **Carrusel de píldoras con estado** (de Hardware Monitor) → `PildorasMedidor` en la Home.
   Cinco grupos («Identidad», «Sonda», «Arquitectura», «Sistema», «Apps») en el sitio donde
   antes había una lista interminable que se desplazaba. Sólo con datos reales detrás: ninguno
   de los cinco es un hueco.
2. **Rejilla de baldosas con icono dibujado** (de Androoster) → `Baldosa`. Cuatro acciones con
   el icono en un cuadrado teñido, el título y una línea. Se pulsan sin leer. El color vive en
   el icono, no en el fondo: cuatro fondos de color seguidos son una plantilla.
3. **Jerarquía de tres niveles sin cajas** (de LSPosed) → el orden de la Home: estado arriba,
   identidad en 26 sp sola, y después pares etiqueta/valor separados por una línea de 1 dp.
4. **Bloques con título que respiran** (de Scene) → los rótulos en versalitas de 9 sp y 26 dp
   entre secciones, en vez de un `spacedBy(12.dp)` uniforme.
5. **Datos técnicos en monoespaciado** (de Androoster) → `FilaDato` ya lo hacía; se mantiene.
6. **El shell en un servicio, no en la Activity** (de InstallerX) → ya era así en
   `ServicioSuperposicion`; queda confirmado como el patrón correcto.

## Lo que NO se copió, y por qué

- **Iconos, fuentes, colores y marca** de ninguna: se copia estructura, no recursos.
- **El violeta de Hardware Monitor**: es exactamente la huella de plantilla que la ley prohíbe.
- **`rikka.shizuku.*` empaquetado dentro del APK** (como Axeron): es una dependencia de
  terceros; aquí se habla con Shizuku si está instalado, y si no, se dice.
- **Nada de root**: LSPosed, Androoster, Kitsune Mask y ReZukiSU necesitan root. Aquí sólo se
  replican sus **patrones de interfaz**, no sus funciones.

## Deuda confesada

- Este documento describe **código y capturas**, no uso. La app no se ha podido ejecutar en un
  teléfono desde aquí (no hay emulador con KVM), así que los patrones están aplicados y
  compilados, y las pruebas los vigilan, pero **cómo se sienten al dedo no está probado**.
- Las capturas de Appteka son de los autores de cada app, no mías. Las conclusiones son sobre
  lo que se ve en ellas.

## Segunda tanda · las 42 categorías con filtro de código abierto (1.9.1)

Se recorrió el catálogo **entero** con la API interna del sitio: **42 categorías, 36.540 apps de
código abierto, todas con repositorio de origen declarado**. El volcado está en el repositorio
de normas (`censo/censo-2026-11-08.json.gz`). De ahí se desmontaron ocho más:

| App | Categoría | Qué se aprendió |
|---|---|---|
| **Kotatsu-Redo** `io.github.kotatsuredo.kotatsu` | Comics | **Nueve temas completos**, cada uno con 92 colores propios (`asuka_`, `miku_`, `sakura_`, `totoro_`…). Un tema no se hace con un color de acento: se hace con una paleta entera por rol. Y no usa el tema por defecto de Material: define el suyo. |
| **Saber** `com.adilhanney.saber` | Productivity | Trae **cinco familias tipográficas** empaquetadas (Atkinson Hyperlegible Next, Dekko, Fira Mono…). Es la prueba de que cambiar la letra es lo que más cambia la sensación de una app, y de que se puede hacer sin pedir nada a internet. De aquí salió la fuente de la 1.9.1. |
| **NewPipe** `org.schabi.newpipe` | Entertainment | Sus colores están nombrados por **función y por variante**: `black_settings_accent_color`, `black_separator_color`, `black_card_item_contrast_color`. Nombrar el color por lo que hace, no por cómo se ve. |
| **Aurora Store** `com.aurora.store` | Tools | Usa Compose con el tema de Material tal cual (`mtrl_*`, `design_*`): confirma que **el diseño no está en `res/`**. Dos apps de la misma categoría pueden tener recursos idénticos y verse completamente distintas. |
| **Rethink** `com.celzero.bravedns` | Tools (Security) | Una app de 31 MB con un panel de estado que ocupa la pantalla entera: el estado de la VPN *es* el producto. Buen ejemplo de «el dato principal manda». |
| **Kreate** `me.knighthat.kreate` | Music & Audio | Reimplementación completa de una app de música: mismo patrón de barra inferior que ya usamos, pero con el reproductor como hoja persistente. Apuntado para cuando haya reproductor. |
| **OmniShizuku** `com.krishna.omnishizuku` | Tools | Empaqueta el gestor de Shizuku entero (`moe.shizuku.manager.MainActivity`) dentro de un APK de 57 MB. **No se copia**: es una dependencia de terceros empaquetada. |
| **VpnHood! CONNECT** `com.vpnhood.connect.android` | Tools | .NET MAUI (134 MB, `crc64...MainActivity`). Prueba de que el tamaño no tiene nada que ver con el diseño: 134 MB y una pantalla que se podría hacer en 3. |

**Lo que se aplicó de esta tanda:** sólo una cosa, y la más importante — **familia tipográfica
propia** (Atkinson Hyperlegible Next, SIL OFL 1.1, cuatro pesos en `res/font/`, licencia dentro
del APK). Las demás quedan como referencia anotada, no como trabajo hecho.

**Lo que no se copió:** los nueve temas de Kotatsu (son su marca), el .NET de VpnHood, y el
Shizuku empaquetado de OmniShizuku.
